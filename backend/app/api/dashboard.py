from datetime import date
from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user_dep
from app.core.database import get_db
from app.models.ai_insight import AiInsight
from app.models.digital_twin import DigitalTwin
from app.models.drift_score import DriftScore
from app.models.risk_record import RiskRecord
from app.models.user import User
from app.schemas.dashboard import DashboardSummaryResponse, HighlightOut
from app.services.twin_manager import get_or_create_twin

router = APIRouter(prefix="/dashboard", tags=["dashboard"])


@router.get("/summary", response_model=DashboardSummaryResponse)
async def get_dashboard_summary(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user_dep),
):
    today = date.today()
    twin = await get_or_create_twin(db, current_user.id)

    # Get today's drift
    drift_result = await db.execute(
        select(DriftScore).where(DriftScore.user_id == current_user.id, DriftScore.score_date == today)
    )
    drift = drift_result.scalar_one_or_none()

    # Get today's risk
    risk_result = await db.execute(
        select(RiskRecord).where(RiskRecord.user_id == current_user.id, RiskRecord.record_date == today)
    )
    risk = risk_result.scalar_one_or_none()

    # Get today's insight for wellness score
    insight_result = await db.execute(
        select(AiInsight).where(
            AiInsight.user_id == current_user.id,
            AiInsight.insight_type == "daily",
            AiInsight.period_start == today,
        )
    )
    insight = insight_result.scalar_one_or_none()

    composite_drift = drift.composite_drift if drift else 0.0
    risk_level = risk.risk_level if risk else 0
    risk_label = risk.risk_label if risk else "Healthy"
    wellness_score = insight.wellness_score if insight else max(0, 100 - int(composite_drift))

    calibration_progress = min(twin.baseline_days / 14.0, 1.0)

    # Build highlights from top drift contributors
    highlights = []
    if drift and drift.top_contributors:
        for contributor in drift.top_contributors[:3]:
            z_scores = drift.dimension_z_scores or {}
            z = z_scores.get(contributor, 0.0)
            direction = "above" if z > 0 else "below"
            magnitude = abs(z)
            name = contributor.replace("_", " ").title()
            highlights.append(HighlightOut(
                type=contributor,
                message=f"{name} is {magnitude:.1f}σ {direction} your baseline",
            ))

    if not highlights and not twin.is_active:
        days_remaining = max(0, 7 - twin.baseline_days)
        highlights.append(HighlightOut(
            type="calibration",
            message=f"Building your baseline — {days_remaining} days remaining",
        ))

    return DashboardSummaryResponse(
        wellness_score=wellness_score,
        drift_score=composite_drift,
        risk_level=risk_level,
        risk_label=risk_label,
        baseline_active=twin.is_active,
        calibration_progress=calibration_progress,
        baseline_days=twin.baseline_days,
        highlights=highlights,
        last_updated=drift.computed_at if drift else None,
    )
