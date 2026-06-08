from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel


class AppUsageEventIn(BaseModel):
    package_name: str
    session_start: datetime
    session_end: datetime
    duration_seconds: int


class BatchEventsRequest(BaseModel):
    device_id: str
    events: List[AppUsageEventIn]


class BatchEventsResponse(BaseModel):
    accepted: int
    duplicate_skipped: int
    invalid_skipped: int
