from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel


class HighlightOut(BaseModel):
    type: str
    message: str


class DashboardSummaryResponse(BaseModel):
    wellness_score: int
    drift_score: float
    risk_level: int
    risk_label: str
    baseline_active: bool
    calibration_progress: float
    baseline_days: int
    highlights: List[HighlightOut]
    last_updated: Optional[datetime] = None


class UpdateSettingsRequest(BaseModel):
    data_retention_days: Optional[int] = None
    drift_alert_threshold: Optional[float] = None
    notifications_enabled: Optional[bool] = None
    fcm_token: Optional[str] = None


class UserSettingsOut(BaseModel):
    data_retention_days: int
    drift_alert_threshold: float
    notifications_enabled: bool


class ExportResponse(BaseModel):
    download_url: str
    expires_at: datetime


class DeleteAccountRequest(BaseModel):
    password: str
    confirm: bool = False
