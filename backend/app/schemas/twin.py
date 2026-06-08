from typing import Dict, Optional
from pydantic import BaseModel


class BaselineDimensionOut(BaseModel):
    weekday_mean: float
    weekday_std: float
    weekend_mean: float
    weekend_std: float
    confidence: float
    n_samples: int


class TwinStatusResponse(BaseModel):
    is_active: bool
    baseline_days: int
    confidence_score: float
    dimensions: Dict[str, BaselineDimensionOut]
    calibration_progress: float


class TwinResetResponse(BaseModel):
    message: str
