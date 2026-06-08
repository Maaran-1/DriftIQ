from datetime import date, timedelta
from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user_dep
from app.core.database import get_db
from app.ml.risk_scorer import compute_risk_trend, get_risk_color
from app.models.risk_record import RiskRecord
from app.models.user import User
from app.schemas.risk import RiskCurrentResponse, RiskHistoryEntry, RiskHistoryResponse

router = APIRouter(prefix="/risk", tags=["risk"])


@router.get("/current", response_model=RiskCurrentResponse)
async def get_current_risk(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user_dep),
):
    today = date.today()
    result = await db.execute(
        select(RiskRecord)
        .where(RiskRecord.user_id == current_user.id)
        .order_by(RiskRecord.record_date.desc())
        .limit(1)
    )
    risk = result.scalar_one_or_none()
    if not risk:
        return RiskCurrentResponse(
            level=0, label="Healthy",
            explanation="No data yet. Build your baseline to see risk analysis.",
            trend="stable", sustained_days=0, color="#2ECC71",
        )

    # Compute trend from last 7 days
    history_result = await db.execute(
        select(RiskRecord.risk_level)
        .where(RiskRecord.user_id == current_user.id, RiskRecord.record_date >= today - timedelta(days=7))
        .order_by(RiskRecord.record_date.asc())
    )
    recent_levels = [r[0] for r in history_result.fetchall()]
    trend = compute_risk_trend(recent_levels)

    return RiskCurrentResponse(
        level=risk.risk_level,
        label=risk.risk_label or "Healthy",
        explanation=risk.explanation or "",
        trend=trend,
        sustained_days=0,
        color=get_risk_color(risk.risk_level),
    )


@router.get("/history", response_model=RiskHistoryResponse)
async def get_risk_history(
    days: int = Query(default=30, le=365),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user_dep),
):
    since = date.today() - timedelta(days=days)
    result = await db.execute(
        select(RiskRecord)
        .where(RiskRecord.user_id == current_user.id, RiskRecord.record_date >= since)
        .order_by(RiskRecord.record_date.asc())
    )
    records = result.scalars().all()
    history = [RiskHistoryEntry(date=r.record_date, level=r.risk_level, label=r.risk_label or "") for r in records]
    return RiskHistoryResponse(history=history)
