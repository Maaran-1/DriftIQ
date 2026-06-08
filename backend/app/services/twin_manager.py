"""Twin Manager — manages DigitalTwin lifecycle."""
import uuid
from datetime import date, datetime, timezone
from typing import Optional

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.ml.baseline_model import (
    build_dimensions_from_dict, compute_confidence, dimensions_to_dict,
    get_total_baseline_days, update_dimension, DIMENSIONS,
)
from app.models.digital_twin import DigitalTwin, TwinSnapshot

MIN_ACTIVE_DAYS = 7
SNAPSHOT_RETENTION = 12


async def get_or_create_twin(db: AsyncSession, user_id: uuid.UUID) -> DigitalTwin:
    result = await db.execute(select(DigitalTwin).where(DigitalTwin.user_id == user_id))
    twin = result.scalar_one_or_none()
    if not twin:
        twin = DigitalTwin(user_id=user_id, dimensions={})
        db.add(twin)
        await db.flush()
    return twin


async def update_twin_baseline(
    db: AsyncSession,
    user_id: uuid.UUID,
    feature_values: dict,
    is_weekend: bool,
) -> DigitalTwin:
    """Apply EWMA update to all dimensions and persist."""
    twin = await get_or_create_twin(db, user_id)
    dims = build_dimensions_from_dict(twin.dimensions or {})

    dim_feature_map = {
        "sleep_hours": feature_values.get("sleep_estimate_hours", 0.0),
        "total_screen_time": feature_values.get("total_screen_time_minutes", 0.0),
        "unlock_count": float(feature_values.get("unlock_count", 0)),
        "social_minutes": feature_values.get("social_minutes", 0.0),
        "productivity_minutes": feature_values.get("productivity_minutes", 0.0),
        "entertainment_minutes": feature_values.get("entertainment_minutes", 0.0),
        "learning_minutes": feature_values.get("learning_minutes", 0.0),
        "late_night_usage": feature_values.get("late_night_minutes", 0.0),
        "session_count": float(feature_values.get("session_count", 0)),
        "usage_entropy": feature_values.get("usage_entropy", 0.0),
    }

    for dim_name in DIMENSIONS:
        if dim_name in dim_feature_map:
            dims[dim_name] = update_dimension(dims[dim_name], dim_feature_map[dim_name], is_weekend)

    twin.dimensions = dimensions_to_dict(dims)
    twin.confidence_score = compute_confidence(dims)
    twin.baseline_days = get_total_baseline_days(dims)
    twin.is_active = twin.baseline_days >= MIN_ACTIVE_DAYS
    twin.version += 1
    twin.updated_at = datetime.now(timezone.utc)
    return twin


async def reset_twin(db: AsyncSession, user_id: uuid.UUID, reason: str = "user_request") -> None:
    """Snapshot current state then reset to blank."""
    twin = await get_or_create_twin(db, user_id)

    if twin.dimensions:
        snapshot = TwinSnapshot(
            twin_id=twin.id,
            snapshot_date=date.today(),
            reason=reason,
            dimensions_snapshot=twin.dimensions,
        )
        db.add(snapshot)

    twin.dimensions = {}
    twin.confidence_score = 0.0
    twin.baseline_days = 0
    twin.is_active = False
    twin.version += 1


async def maybe_snapshot(db: AsyncSession, twin: DigitalTwin, reason: str) -> None:
    """Take a snapshot if warranted (risk level change ≥2, 30-day auto)."""
    if not twin.dimensions:
        return
    snap = TwinSnapshot(
        twin_id=twin.id,
        snapshot_date=date.today(),
        reason=reason,
        dimensions_snapshot=twin.dimensions,
    )
    db.add(snap)
