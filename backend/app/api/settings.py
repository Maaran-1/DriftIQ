from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user_dep
from app.core.database import get_db
from app.core.security import verify_password
from app.core.exceptions import UnauthorizedError
from app.models.user import User
from app.schemas.dashboard import (
    DeleteAccountRequest, ExportResponse, UpdateSettingsRequest, UserSettingsOut,
)
from datetime import datetime, timezone, timedelta

router = APIRouter(prefix="/settings", tags=["settings"])


@router.get("", response_model=UserSettingsOut)
async def get_settings(current_user: User = Depends(get_current_user_dep)):
    return UserSettingsOut(
        data_retention_days=current_user.data_retention_days,
        drift_alert_threshold=current_user.drift_alert_threshold,
        notifications_enabled=current_user.notifications_enabled,
    )


@router.patch("", response_model=UserSettingsOut)
async def update_settings(
    req: UpdateSettingsRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user_dep),
):
    if req.data_retention_days is not None:
        current_user.data_retention_days = req.data_retention_days
    if req.drift_alert_threshold is not None:
        current_user.drift_alert_threshold = req.drift_alert_threshold
    if req.notifications_enabled is not None:
        current_user.notifications_enabled = req.notifications_enabled
    if req.fcm_token is not None:
        current_user.fcm_token = req.fcm_token
    return UserSettingsOut(
        data_retention_days=current_user.data_retention_days,
        drift_alert_threshold=current_user.drift_alert_threshold,
        notifications_enabled=current_user.notifications_enabled,
    )


@router.post("/export", response_model=ExportResponse)
async def export_data(current_user: User = Depends(get_current_user_dep)):
    # In production, trigger async S3 export job; return pre-signed URL
    expires_at = datetime.now(timezone.utc) + timedelta(hours=24)
    return ExportResponse(
        download_url=f"https://exports.driftiq.app/export_{current_user.id}.json",
        expires_at=expires_at,
    )


@router.delete("/account", status_code=200)
async def delete_account(
    req: DeleteAccountRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user_dep),
):
    if not req.confirm:
        raise UnauthorizedError("Must confirm deletion")
    if not verify_password(req.password, current_user.password_hash):
        raise UnauthorizedError("Invalid password")
    # Soft delete — hard delete scheduled by purge job
    current_user.is_active = False
    return {"message": "Account and all data scheduled for deletion within 30 days"}
