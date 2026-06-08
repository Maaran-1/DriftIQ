"""
Nightly Compute Celery Tasks
============================
Entry point tasks for the nightly behavioral pipeline.
Uses a per-task event loop to avoid conflicts with existing loops.
"""
import logging
import uuid
from datetime import date, timedelta

from app.tasks.celery_app import celery_app

logger = logging.getLogger(__name__)


def _run_async(coro):
    """
    Run an async coroutine from a synchronous Celery task.

    Uses asyncio.run() which creates a new event loop per call — safe
    in Celery workers because each task runs in its own thread/process
    and asyncio.run() creates and tears down a fresh event loop.
    """
    import asyncio
    return asyncio.run(coro)


@celery_app.task(
    name="app.tasks.nightly_compute.run_nightly_for_all_users",
    bind=True,
    max_retries=3,
    default_retry_delay=300,
)
def run_nightly_for_all_users(self):
    """
    Fan-out: find all active users and enqueue per-user nightly tasks.
    Called by Celery Beat at the configured nightly time.
    """
    _run_async(_fan_out())


@celery_app.task(
    name="app.tasks.nightly_compute.run_nightly_for_user",
    bind=True,
    max_retries=3,
    default_retry_delay=120,
)
def run_nightly_for_user(self, user_id: str, target_date_str: str):
    """
    Run the full behavioral pipeline for one user on the given date.
    Re-queued with exponential backoff on failure.
    """
    try:
        _run_async(_run_user(uuid.UUID(user_id), date.fromisoformat(target_date_str)))
    except Exception as exc:
        logger.error(
            "Nightly task failed: user=%s date=%s error=%s — retrying",
            user_id, target_date_str, exc,
        )
        raise self.retry(exc=exc)


async def _fan_out():
    """Load active users and dispatch per-user tasks."""
    from app.core.database import AsyncSessionLocal
    from app.models.user import User
    from sqlalchemy import select

    yesterday = (date.today() - timedelta(days=1)).isoformat()

    async with AsyncSessionLocal() as db:
        result = await db.execute(select(User.id).where(User.is_active == True))
        user_ids = [str(row[0]) for row in result.fetchall()]

    logger.info("Nightly compute: dispatching %d user tasks for date=%s", len(user_ids), yesterday)

    for uid in user_ids:
        run_nightly_for_user.apply_async(
            args=[uid, yesterday],
            countdown=0,  # schedule immediately
        )


async def _run_user(user_id: uuid.UUID, target_date: date):
    """Execute the full pipeline for a single user and commit."""
    from app.core.database import AsyncSessionLocal
    from app.services.nightly_pipeline import run_nightly_pipeline

    async with AsyncSessionLocal() as db:
        results = await run_nightly_pipeline(db, user_id, target_date)
        await db.commit()

    status = results.get("status", "unknown")
    logger.info(
        "Nightly pipeline done: user=%s date=%s status=%s",
        user_id, target_date, status,
    )
    return results
