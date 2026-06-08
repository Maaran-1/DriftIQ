"""
Events API — batch ingestion of app usage events from Android client.
"""
import logging
from datetime import datetime, timezone

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user_dep
from app.core.database import get_db
from app.ml.feature_extractor import get_category
from app.models.usage_event import AppUsageEvent
from app.models.user import User
from app.schemas.events import BatchEventsRequest, BatchEventsResponse

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/events", tags=["events"])


@router.post("/batch", response_model=BatchEventsResponse, status_code=202)
async def ingest_batch(
    req: BatchEventsRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user_dep),
):
    """
    Ingest a batch of app usage events from the mobile client.

    - Rejects events with future timestamps (>5 min ahead)
    - Skips duplicate events (same user + package + session_start)
    - Maps package names to behavioral categories
    - Accepts up to 500 events per request
    """
    accepted = 0
    duplicates = 0
    invalid = 0
    now = datetime.now(timezone.utc)

    if len(req.events) > 500:
        # Silently truncate to prevent abuse — accepted count will reflect this
        events_to_process = req.events[:500]
        invalid += len(req.events) - 500
    else:
        events_to_process = req.events

    for ev in events_to_process:
        # ── Normalize to UTC-aware datetime ───────────────────────
        session_start = ev.session_start
        session_end = ev.session_end

        # Make timezone-aware if naive (assume UTC)
        if session_start.tzinfo is None:
            session_start = session_start.replace(tzinfo=timezone.utc)
        if session_end.tzinfo is None:
            session_end = session_end.replace(tzinfo=timezone.utc)

        # ── Reject future events (>5 min tolerance) ──────────────
        tolerance = datetime(now.year, now.month, now.day, now.hour, now.minute + 5,
                             tzinfo=timezone.utc)
        if session_start > tolerance:
            invalid += 1
            continue

        # ── Reject zero or negative duration ─────────────────────
        if ev.duration_seconds <= 0:
            invalid += 1
            continue

        # ── Reject if end before start ────────────────────────────
        if session_end < session_start:
            invalid += 1
            continue

        event_date = session_start.astimezone(timezone.utc).date()

        # ── Deduplication: same user + package + session_start ────
        existing = await db.execute(
            select(AppUsageEvent).where(
                AppUsageEvent.user_id == current_user.id,
                AppUsageEvent.package_name == ev.package_name,
                AppUsageEvent.session_start == session_start,
            )
        )
        if existing.scalar_one_or_none():
            duplicates += 1
            continue

        category = get_category(ev.package_name)
        db_event = AppUsageEvent(
            user_id=current_user.id,
            package_name=ev.package_name,
            app_category=category,
            session_start=session_start,
            session_end=session_end,
            duration_seconds=ev.duration_seconds,
            event_date=event_date,
            device_id=req.device_id,
        )
        db.add(db_event)
        accepted += 1

    # Flush to ensure events are persisted before returning
    if accepted > 0:
        await db.flush()

    logger.info(
        "Events batch user=%s: accepted=%d duplicates=%d invalid=%d",
        current_user.id, accepted, duplicates, invalid,
    )

    return BatchEventsResponse(
        accepted=accepted,
        duplicate_skipped=duplicates,
        invalid_skipped=invalid,
    )
