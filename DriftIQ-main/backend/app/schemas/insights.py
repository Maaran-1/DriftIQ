from datetime import date
from pydantic import BaseModel


class DailyInsightResponse(BaseModel):
    insight_id: str
    type: str = "daily"
    date: date
    content: str
    wellness_score: int
    risk_level: int


class WeeklyInsightResponse(BaseModel):
    insight_id: str
    period_start: date
    period_end: date
    content: str
    wellness_score: int


class MonthlyInsightResponse(BaseModel):
    insight_id: str
    period_start: date
    period_end: date
    content: str
    wellness_score: int
