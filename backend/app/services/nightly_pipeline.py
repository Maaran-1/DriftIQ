"""
Nightly Compute Pipeline
========================
Runs all behavioral processing steps for a single user/day:
  1. Feature extraction
  2. Baseline update
  3. Drift computation
  4. Risk computation
  5. AI insight generation

Designed to be idempotent — safe to re-run for the same user/date.
"""
import logging
import uuid
from datetime import date, datetime, timedelta, timezone
from typing import Optional

from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.ml.drift_engine import (
    compute_daily_drift, compute_drift_velocity, map_feature_vector_to_dimensions,
)
from app.ml.baseline_model import build_dimensions_from_dict
from app.ml.feature_extractor import RawEvent, extract_features
from app.ml.risk_scorer import (
    compute_risk_level, compute_risk_trend, generate_risk_explanation, get_risk_color,
)
from app.models.ai_insight import AiInsight, DriftEvent
from app.models.daily_feature import DailyFeature
from app.models.drift_score import DriftScore
from app.models.risk_record import RiskRecord
from app.models.usage_event import AppUsageEvent
from app.services.insight_generator import generate_daily_insight
from app.services.twin_manager import get_or_create_twin, update_twin_baseline

logger = logging.getLogger(__name__)

# Minimum baseline days before drift computation is meaningful
MIN_BASELINE_FOR_DRIFT = 7


async def run_nightly_pipeline(
    db: AsyncSession,
    user_id: uuid.UUID,
    target_date: date,
) -> dict:
    """
    Full nightly pipeline for one user on one date.

    Returns a results dict summarizing what was computed.
    Always safe to call — uses upsert patterns throughout.
    """
    results: dict = {"user_id": str(user_id), "date": target_date.isoformat()}

    try:
        # ── Step 1: Feature Extraction ────────────────────────────
        events_result = await db.execute(
            select(AppUsageEvent).where(
                AppUsageEvent.user_id == user_id,
                AppUsageEvent.event_date == target_date,
            )
        )
        raw_db_events = events_result.scalars().all()

        raw_events = [
            RawEvent(
                package_name=e.package_name,
                session_start=e.session_start,
                session_end=e.session_end,
                duration_seconds=e.duration_seconds,
                category=e.app_category,
            )
            for e in raw_db_events
        ]

        # Previous day's last event for sleep estimation
        prev_date = target_date - timedelta(days=1)
        prev_result = await db.execute(
            select(AppUsageEvent)
            .where(AppUsageEvent.user_id == user_id, AppUsageEvent.event_date == prev_date)
            .order_by(AppUsageEvent.session_end.desc())
            .limit(1)
        )
        prev_event = prev_result.scalar_one_or_none()
        prev_last: Optional[datetime] = prev_event.session_end if prev_event else None

        fv = extract_features(raw_events, target_date, prev_last)
        fv_dict = fv.to_dict()

        # Upsert DailyFeature
        existing_feat = await db.execute(
            select(DailyFeature).where(
                DailyFeature.user_id == user_id,
                DailyFeature.feature_date == target_date,
            )
        )
        df = existing_feat.scalar_one_or_none()
        if not df:
            df = DailyFeature(
                user_id=user_id,
                feature_date=target_date,
                is_weekend=fv.is_weekend,
                **{k: v for k, v in fv_dict.items() if k != "is_weekend" and hasattr(DailyFeature, k)},
            )
            db.add(df)
        else:
            for k, v in fv_dict.items():
                if hasattr(df, k):
                    setattr(df, k, v)

        results["features"] = {
            "session_count": fv.session_count,
            "total_screen_time_minutes": round(fv.total_screen_time_minutes, 1),
        }

        # ── Step 2: Baseline Update ───────────────────────────────
        twin = await update_twin_baseline(db, user_id, fv_dict, fv.is_weekend)
        await db.flush()

        results["baseline"] = {
            "days": twin.baseline_days,
            "confidence": round(twin.confidence_score, 3),
            "is_active": twin.is_active,
        }

        # Only compute drift if baseline is established
        if twin.baseline_days < MIN_BASELINE_FOR_DRIFT:
            logger.info(
                "Pipeline user=%s date=%s: baseline insufficient (%d days), skip drift",
                user_id, target_date, twin.baseline_days,
            )
            results["status"] = "baseline_building"
            return results

        # ── Step 3: Drift Computation ─────────────────────────────
        dims = build_dimensions_from_dict(twin.dimensions or {})
        dim_values = map_feature_vector_to_dimensions(fv_dict)

        dim_results, composite_drift, top_contributors = compute_daily_drift(
            dim_values, dims, fv.is_weekend
        )

        # Velocity from last 2 stored drift scores + today
        recent_drifts_result = await db.execute(
            select(DriftScore.composite_drift)
            .where(
                DriftScore.user_id == user_id,
                DriftScore.score_date < target_date,
                DriftScore.composite_drift.isnot(None),
            )
            .order_by(DriftScore.score_date.desc())
            .limit(2)
        )
        recent_vals = [r[0] for r in recent_drifts_result.fetchall()]
        # Build time-ordered list: [oldest, ..., today]
        drift_velocity = compute_drift_velocity([*reversed(recent_vals), composite_drift])

        # Sustained days: days above the risk-level-1 threshold (25.0 per spec §13)
        # Using configurable threshold from settings
        sustained_threshold = settings.DRIFT_SUSTAINED_THRESHOLD
        sustained_result = await db.execute(
            select(func.count()).where(
                DriftScore.user_id == user_id,
                DriftScore.composite_drift >= sustained_threshold,
                DriftScore.score_date >= target_date - timedelta(days=30),
                DriftScore.score_date < target_date,
            )
        )
        sustained_days = sustained_result.scalar() or 0

        z_scores_dict = {k: v.z_score for k, v in dim_results.items()}

        # Upsert DriftScore
        existing_drift = await db.execute(
            select(DriftScore).where(
                DriftScore.user_id == user_id,
                DriftScore.score_date == target_date,
            )
        )
        drift_rec = existing_drift.scalar_one_or_none()
        if not drift_rec:
            drift_rec = DriftScore(
                user_id=user_id,
                score_date=target_date,
                composite_drift=composite_drift,
                drift_velocity=drift_velocity,
                dimension_z_scores=z_scores_dict,
                top_contributors=top_contributors,
                sustained_days=sustained_days,
            )
            db.add(drift_rec)
        else:
            drift_rec.composite_drift = composite_drift
            drift_rec.drift_velocity = drift_velocity
            drift_rec.dimension_z_scores = z_scores_dict
            drift_rec.top_contributors = top_contributors
            drift_rec.sustained_days = sustained_days
            drift_rec.computed_at = datetime.now(timezone.utc)

        results["drift"] = {
            "composite": round(composite_drift, 2),
            "velocity": round(drift_velocity, 4),
            "top_contributors": top_contributors[:3],
        }

        # ── Step 4: Risk Computation ──────────────────────────────
        risk_level, risk_label = compute_risk_level(
            composite_drift, drift_velocity, sustained_days, twin.confidence_score
        )
        explanation = generate_risk_explanation(dim_results, risk_level)

        # Upsert RiskRecord
        existing_risk = await db.execute(
            select(RiskRecord).where(
                RiskRecord.user_id == user_id,
                RiskRecord.record_date == target_date,
            )
        )
        risk_rec = existing_risk.scalar_one_or_none()
        if not risk_rec:
            risk_rec = RiskRecord(
                user_id=user_id,
                record_date=target_date,
                risk_level=risk_level,
                risk_label=risk_label,
                explanation=explanation,
                composite_drift=composite_drift,
                drift_velocity=drift_velocity,
            )
            db.add(risk_rec)
        else:
            risk_rec.risk_level = risk_level
            risk_rec.risk_label = risk_label
            risk_rec.explanation = explanation
            risk_rec.composite_drift = composite_drift
            risk_rec.drift_velocity = drift_velocity
            risk_rec.computed_at = datetime.now(timezone.utc)

        results["risk"] = {"level": risk_level, "label": risk_label}

        # Log drift events for significant threshold crossings
        if composite_drift >= settings.DRIFT_ALERT_THRESHOLD:
            db.add(DriftEvent(
                user_id=user_id,
                event_date=target_date,
                event_type="composite_threshold",
                composite_drift=composite_drift,
                risk_level=risk_level,
            ))

        # ── Step 5: Insight Generation ────────────────────────────
        wellness_score = max(0, min(100, 100 - int(composite_drift)))
        drift_data = {
            "composite_drift": composite_drift,
            "risk_level": risk_level,
            "risk_label": risk_label,
            "top_contributors": top_contributors,
            "sleep_hours": fv.sleep_estimate_hours,
            "screen_time": fv.total_screen_time_minutes,
            "social_minutes": fv.social_minutes,
            "productivity_minutes": fv.productivity_minutes,
        }
        twin_data = {
            f"{dim_name}_baseline": (dim.weekend_mean if fv.is_weekend else dim.weekday_mean)
            for dim_name, dim in build_dimensions_from_dict(twin.dimensions or {}).items()
        }

        content = await generate_daily_insight(target_date, drift_data, twin_data)

        # Upsert AiInsight
        existing_insight = await db.execute(
            select(AiInsight).where(
                AiInsight.user_id == user_id,
                AiInsight.insight_type == "daily",
                AiInsight.period_start == target_date,
            )
        )
        insight = existing_insight.scalar_one_or_none()
        if not insight:
            insight = AiInsight(
                user_id=user_id,
                insight_type="daily",
                period_start=target_date,
                period_end=target_date,
                content=content,
                wellness_score=wellness_score,
                risk_level=risk_level,
            )
            db.add(insight)
        else:
            insight.content = content
            insight.wellness_score = wellness_score
            insight.risk_level = risk_level
            insight.generated_at = datetime.now(timezone.utc)

        results["insight"] = {"wellness_score": wellness_score}
        results["status"] = "complete"

        logger.info(
            "Pipeline complete: user=%s date=%s drift=%.1f risk=%d wellness=%d",
            user_id, target_date, composite_drift, risk_level, wellness_score,
        )

    except Exception as exc:
        logger.error(
            "Pipeline error: user=%s date=%s error=%s",
            user_id, target_date, exc,
            exc_info=True,
        )
        results["status"] = "error"
        results["error"] = str(exc)

    return results
