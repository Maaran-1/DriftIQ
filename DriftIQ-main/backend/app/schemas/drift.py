from datetime import date
from typing import Dict, List, Optional
from pydantic import BaseModel


class DimensionScoreOut(BaseModel):
    z_score: float
    direction: str
    value: float
    baseline: float


class DriftTodayResponse(BaseModel):
    date: date
    composite_drift: float
    drift_velocity: float
    dimension_scores: Dict[str, DimensionScoreOut]
    top_contributors: List[str]
    explanation: str


class DriftHistoryEntry(BaseModel):
    date: date
    composite_drift: float
    risk_level: int


class DriftHistoryResponse(BaseModel):
    history: List[DriftHistoryEntry]
