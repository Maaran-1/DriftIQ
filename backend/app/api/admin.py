"""
Admin API — internal/dev-only endpoints.

IMPORTANT: These endpoints are only registered in non-production environments.
See main.py where APP_ENV guards the router include.
"""
import logging
import uuid
from datetime import date, timedelta

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user_dep
from app.core.database import get_db
from app.models.user import User
from app.services.nightly_pipeline import run_nightly_pipeline

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/admin", tags=["admin"])


@router.post("/run-pipeline")
async def trigger_pipeline(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user_dep),
):
    """
    Manually trigger the nightly behavioral pipeline for the current user.
    Runs synchronously for the previous day's data.

    Use this during development/testing to populate daily_features,
    drift_scores, risk_records, and ai_insights without waiting for
    Celery Beat to fire at 2:00 AM UTC.
    """
    target_date = date.today() - timedelta(days=1)
    results = await run_nightly_pipeline(db, current_user.id, target_date)
    await db.commit()
    logger.info("Manual pipeline trigger: user=%s date=%s status=%s",
                current_user.id, target_date, results.get("status"))
    return results


@router.post("/run-pipeline-today")
async def trigger_pipeline_today(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user_dep),
):
    """
    Run the pipeline for today's data (useful when testing same-day events).
    """
    target_date = date.today()
    results = await run_nightly_pipeline(db, current_user.id, target_date)
    await db.commit()
    logger.info("Manual pipeline trigger (today): user=%s date=%s status=%s",
                current_user.id, target_date, results.get("status"))
    return results
