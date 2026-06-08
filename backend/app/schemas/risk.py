from datetime import date
from typing import Optional
from pydantic import BaseModel


class RiskCurrentResponse(BaseModel):
    level: int
    label: str
    explanation: str
    trend: str
    sustained_days: int
    color: str


class RiskHistoryEntry(BaseModel):
    date: date
    level: int
    label: str


class RiskHistoryResponse(BaseModel):
    history: list[RiskHistoryEntry]
