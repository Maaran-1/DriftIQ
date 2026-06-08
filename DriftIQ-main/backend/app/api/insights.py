from datetime import date, timedelta
from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user_dep
from app.core.database import get_db
from app.models.ai_insight import AiInsight
from app.models.user import User
from app.schemas.insights import DailyInsightResponse, MonthlyInsightResponse, WeeklyInsightResponse

router = APIRouter(prefix="/insights", tags=["insights"])


@router.get("/daily", response_model=DailyInsightResponse)
async def get_daily_insight(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user_dep),
):
    today = date.today()
    result = await db.execute(
        select(AiInsight).where(
            AiInsight.user_id == current_user.id,
            AiInsight.insight_type == "daily",
            AiInsight.period_start == today,
        )
    )
    insight = result.scalar_one_or_none()
    if not insight:
        return DailyInsightResponse(
            insight_id="pending",
            date=today,
            content="Your daily insight is being generated. Check back after your baseline is established.",
            wellness_score=50,
            risk_level=0,
        )
    return DailyInsightResponse(
        insight_id=str(insight.id),
        date=insight.period_start,
        content=insight.content,
        wellness_score=insight.wellness_score or 50,
        risk_level=insight.risk_level or 0,
    )


@router.get("/weekly", response_model=WeeklyInsightResponse)
async def get_weekly_insight(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user_dep),
):
    today = date.today()
    week_start = today - timedelta(days=7)
    result = await db.execute(
        select(AiInsight).where(
            AiInsight.user_id == current_user.id,
            AiInsight.insight_type == "weekly",
            AiInsight.period_start >= week_start,
        ).order_by(AiInsight.generated_at.desc()).limit(1)
    )
    insight = result.scalar_one_or_none()
    if not insight:
        return WeeklyInsightResponse(
            insight_id="pending",
            period_start=week_start,
            period_end=today,
            content="Weekly insight will be ready after 7 days of data collection.",
            wellness_score=50,
        )
    return WeeklyInsightResponse(
        insight_id=str(insight.id),
        period_start=insight.period_start,
        period_end=insight.period_end,
        content=insight.content,
        wellness_score=insight.wellness_score or 50,
    )


@router.get("/monthly", response_model=MonthlyInsightResponse)
async def get_monthly_insight(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user_dep),
):
    today = date.today()
    month_start = today - timedelta(days=30)
    result = await db.execute(
        select(AiInsight).where(
            AiInsight.user_id == current_user.id,
            AiInsight.insight_type == "monthly",
            AiInsight.period_start >= month_start,
        ).order_by(AiInsight.generated_at.desc()).limit(1)
    )
    insight = result.scalar_one_or_none()
    if not insight:
        return MonthlyInsightResponse(
            insight_id="pending",
            period_start=month_start,
            period_end=today,
            content="Monthly insight will be ready after 30 days of data collection.",
            wellness_score=50,
        )
    return MonthlyInsightResponse(
        insight_id=str(insight.id),
        period_start=insight.period_start,
        period_end=insight.period_end,
        content=insight.content,
        wellness_score=insight.wellness_score or 50,
    )
