"""
Insight Generation Tasks
========================
Celery tasks for weekly and monthly behavioral insight generation.
Triggered by Celery Beat on schedule.
"""
import asyncio
import logging
import uuid
from datetime import date, timedelta
from typing import Optional

from app.tasks.celery_app import celery_app

logger = logging.getLogger(__name__)


@celery_app.task(
    name="app.tasks.insight_task.generate_weekly_insights_for_all",
    bind=True,
    max_retries=3,
    default_retry_delay=600,
)
def generate_weekly_insights_for_all(self):
    """Generate weekly insights for all active users. Called by Celery Beat on Sunday."""
    asyncio.run(_run_weekly_for_all())


@celery_app.task(
    name="app.tasks.insight_task.generate_monthly_insights_for_all",
    bind=True,
    max_retries=3,
    default_retry_delay=600,
)
def generate_monthly_insights_for_all(self):
    """Generate monthly insights for all active users. Called by Celery Beat on 1st of month."""
    asyncio.run(_run_monthly_for_all())


@celery_app.task(
    name="app.tasks.insight_task.generate_weekly_insight_for_user",
    bind=True,
    max_retries=3,
)
def generate_weekly_insight_for_user(self, user_id: str):
    """Generate weekly insight for a single user."""
    asyncio.run(_run_weekly_for_user(uuid.UUID(user_id)))


@celery_app.task(
    name="app.tasks.insight_task.generate_monthly_insight_for_user",
    bind=True,
    max_retries=3,
)
def generate_monthly_insight_for_user(self, user_id: str):
    """Generate monthly insight for a single user."""
    asyncio.run(_run_monthly_for_user(uuid.UUID(user_id)))


async def _run_weekly_for_all():
    from app.core.database import AsyncSessionLocal
    from app.models.user import User
    from sqlalchemy import select

    async with AsyncSessionLocal() as db:
        result = await db.execute(select(User.id).where(User.is_active == True))
        user_ids = [row[0] for row in result.fetchall()]

    for user_id in user_ids:
        generate_weekly_insight_for_user.delay(str(user_id))
        logger.debug("Queued weekly insight for user=%s", user_id)

    logger.info("Weekly insight generation queued for %d users", len(user_ids))


async def _run_monthly_for_all():
    from app.core.database import AsyncSessionLocal
    from app.models.user import User
    from sqlalchemy import select

    async with AsyncSessionLocal() as db:
        result = await db.execute(select(User.id).where(User.is_active == True))
        user_ids = [row[0] for row in result.fetchall()]

    for user_id in user_ids:
        generate_monthly_insight_for_user.delay(str(user_id))
        logger.debug("Queued monthly insight for user=%s", user_id)

    logger.info("Monthly insight generation queued for %d users", len(user_ids))


async def _run_weekly_for_user(user_id: uuid.UUID):
    from app.core.database import AsyncSessionLocal
    from app.models.ai_insight import AiInsight
    from app.models.drift_score import DriftScore
    from app.services.insight_generator import generate_weekly_insight
    from sqlalchemy import select, func

    today = date.today()
    period_end = today
    period_start = today - timedelta(days=7)

    async with AsyncSessionLocal() as db:
        # Check if we already have a weekly insight for this period
        existing = await db.execute(
            select(AiInsight).where(
                AiInsight.user_id == user_id,
                AiInsight.insight_type == "weekly",
                AiInsight.period_start >= period_start,
                AiInsight.period_end <= period_end,
            )
        )
        if existing.scalar_one_or_none():
            logger.debug("Weekly insight already exists for user=%s", user_id)
            return

        # Aggregate weekly stats from drift scores
        stats_result = await db.execute(
            select(
                func.avg(DriftScore.composite_drift).label("avg_drift"),
                func.max(DriftScore.composite_drift).label("max_drift"),
                func.min(DriftScore.composite_drift).label("min_drift"),
                func.count().label("days_tracked"),
            )
            .where(
                DriftScore.user_id == user_id,
                DriftScore.score_date.between(period_start, period_end),
                DriftScore.composite_drift.isnot(None),
            )
        )
        row = stats_result.first()
        if not row or not row.avg_drift:
            logger.debug("No drift data for weekly insight: user=%s", user_id)
            return

        weekly_stats = {
            "avg_composite_drift": float(row.avg_drift or 0),
            "max_composite_drift": float(row.max_drift or 0),
            "min_composite_drift": float(row.min_drift or 0),
            "days_tracked": int(row.days_tracked or 0),
            "period_start": period_start.isoformat(),
            "period_end": period_end.isoformat(),
        }

        content = await generate_weekly_insight(period_start, period_end, weekly_stats)
        wellness_score = max(0, min(100, 100 - int(weekly_stats["avg_composite_drift"])))

        insight = AiInsight(
            user_id=user_id,
            insight_type="weekly",
            period_start=period_start,
            period_end=period_end,
            content=content,
            wellness_score=wellness_score,
        )
        db.add(insight)
        await db.commit()
        logger.info("Weekly insight generated: user=%s wellness=%d", user_id, wellness_score)


async def _run_monthly_for_user(user_id: uuid.UUID):
    from app.core.database import AsyncSessionLocal
    from app.models.ai_insight import AiInsight
    from app.models.drift_score import DriftScore
    from app.services.insight_generator import generate_monthly_insight
    from sqlalchemy import select, func

    today = date.today()
    period_end = today
    period_start = today - timedelta(days=30)

    async with AsyncSessionLocal() as db:
        # Check for existing monthly insight
        existing = await db.execute(
            select(AiInsight).where(
                AiInsight.user_id == user_id,
                AiInsight.insight_type == "monthly",
                AiInsight.period_start >= period_start - timedelta(days=3),
            )
        )
        if existing.scalar_one_or_none():
            logger.debug("Monthly insight already exists for user=%s", user_id)
            return

        stats_result = await db.execute(
            select(
                func.avg(DriftScore.composite_drift).label("avg_drift"),
                func.max(DriftScore.composite_drift).label("max_drift"),
                func.min(DriftScore.composite_drift).label("min_drift"),
                func.count().label("days_tracked"),
                func.avg(DriftScore.drift_velocity).label("avg_velocity"),
            )
            .where(
                DriftScore.user_id == user_id,
                DriftScore.score_date.between(period_start, period_end),
                DriftScore.composite_drift.isnot(None),
            )
        )
        row = stats_result.first()
        if not row or not row.avg_drift:
            logger.debug("No drift data for monthly insight: user=%s", user_id)
            return

        monthly_stats = {
            "avg_composite_drift": float(row.avg_drift or 0),
            "max_composite_drift": float(row.max_drift or 0),
            "min_composite_drift": float(row.min_drift or 0),
            "days_tracked": int(row.days_tracked or 0),
            "avg_velocity": float(row.avg_velocity or 0),
            "period_start": period_start.isoformat(),
            "period_end": period_end.isoformat(),
        }

        content = await generate_monthly_insight(period_start, period_end, monthly_stats)
        wellness_score = max(0, min(100, 100 - int(monthly_stats["avg_composite_drift"])))

        insight = AiInsight(
            user_id=user_id,
            insight_type="monthly",
            period_start=period_start,
            period_end=period_end,
            content=content,
            wellness_score=wellness_score,
        )
        db.add(insight)
        await db.commit()
        logger.info("Monthly insight generated: user=%s wellness=%d", user_id, wellness_score)
