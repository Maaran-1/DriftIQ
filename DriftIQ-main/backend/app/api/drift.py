from datetime import date, timedelta
from typing import Optional
from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user_dep
from app.core.database import get_db
from app.ml.baseline_model import build_dimensions_from_dict
from app.ml.drift_engine import compute_daily_drift, compute_drift_velocity, map_feature_vector_to_dimensions
from app.ml.risk_scorer import generate_risk_explanation
from app.models.daily_feature import DailyFeature
from app.models.digital_twin import DigitalTwin
from app.models.drift_score import DriftScore
from app.models.risk_record import RiskRecord
from app.models.user import User
from app.schemas.drift import DimensionScoreOut, DriftHistoryEntry, DriftHistoryResponse, DriftTodayResponse
from app.services.twin_manager import get_or_create_twin

router = APIRouter(prefix="/drift", tags=["drift"])


@router.get("/today", response_model=DriftTodayResponse)
async def get_drift_today(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user_dep),
):
    today = date.today()
    result = await db.execute(
        select(DriftScore).where(DriftScore.user_id == current_user.id, DriftScore.score_date == today)
    )
    drift = result.scalar_one_or_none()

    if not drift:
        # Try to compute on-the-fly from stored features
        feat_result = await db.execute(
            select(DailyFeature).where(DailyFeature.user_id == current_user.id, DailyFeature.feature_date == today)
        )
        feat = feat_result.scalar_one_or_none()

        if not feat:
            return DriftTodayResponse(
                date=today, composite_drift=0.0, drift_velocity=0.0,
                dimension_scores={}, top_contributors=[],
                explanation="No data available yet. Keep using your device to build your baseline.",
            )

        twin = await get_or_create_twin(db, current_user.id)
        dims = build_dimensions_from_dict(twin.dimensions or {})
        fv_dict = {
            "sleep_estimate_hours": feat.sleep_estimate_hours or 0,
            "total_screen_time_minutes": feat.total_screen_time_minutes or 0,
            "unlock_count": feat.unlock_count or 0,
            "social_minutes": feat.social_minutes or 0,
            "productivity_minutes": feat.productivity_minutes or 0,
            "entertainment_minutes": feat.entertainment_minutes or 0,
            "learning_minutes": feat.learning_minutes or 0,
            "late_night_minutes": feat.late_night_minutes or 0,
            "session_count": feat.session_count or 0,
            "usage_entropy": feat.usage_entropy or 0,
        }
        dim_values = map_feature_vector_to_dimensions(fv_dict)
        dim_results, composite, top_3 = compute_daily_drift(dim_values, dims, feat.is_weekend)
        explanation = generate_risk_explanation(dim_results, 0)
        dimension_scores = {
            k: DimensionScoreOut(
                z_score=v.z_score, direction=v.direction,
                value=v.value, baseline=v.baseline_value,
            )
            for k, v in dim_results.items()
        }
        return DriftTodayResponse(
            date=today, composite_drift=composite, drift_velocity=0.0,
            dimension_scores=dimension_scores, top_contributors=top_3,
            explanation=explanation,
        )

    # Return from stored drift record
    risk_result = await db.execute(
        select(RiskRecord).where(RiskRecord.user_id == current_user.id, RiskRecord.record_date == today)
    )
    risk_rec = risk_result.scalar_one_or_none()
    explanation = risk_rec.explanation if risk_rec else "Behavioral patterns analyzed."

    dim_scores = {}
    if drift.dimension_z_scores:
        for k, z in drift.dimension_z_scores.items():
            dim_scores[k] = DimensionScoreOut(
                z_score=z,
                direction="increase" if z > 0.5 else "decrease" if z < -0.5 else "stable",
                value=0.0, baseline=0.0,
            )

    return DriftTodayResponse(
        date=today,
        composite_drift=drift.composite_drift or 0.0,
        drift_velocity=drift.drift_velocity or 0.0,
        dimension_scores=dim_scores,
        top_contributors=drift.top_contributors or [],
        explanation=explanation or "",
    )


@router.get("/history", response_model=DriftHistoryResponse)
async def get_drift_history(
    days: int = Query(default=30, le=365),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user_dep),
):
    since = date.today() - timedelta(days=days)
    result = await db.execute(
        select(DriftScore, RiskRecord)
        .outerjoin(RiskRecord, (RiskRecord.user_id == DriftScore.user_id) & (RiskRecord.record_date == DriftScore.score_date))
        .where(DriftScore.user_id == current_user.id, DriftScore.score_date >= since)
        .order_by(DriftScore.score_date.asc())
    )
    rows = result.all()
    history = [
        DriftHistoryEntry(
            date=ds.score_date,
            composite_drift=ds.composite_drift or 0.0,
            risk_level=rr.risk_level if rr else 0,
        )
        for ds, rr in rows
    ]
    return DriftHistoryResponse(history=history)
