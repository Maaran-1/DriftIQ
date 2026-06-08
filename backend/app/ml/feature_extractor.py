"""
Feature Extractor
=================
Converts raw AppUsageEvents for a single user/day into a DailyFeatureVector.

15 Features:
 1. total_screen_time_minutes
 2. unlock_count
 3. unique_apps_used
 4. social_minutes
 5. productivity_minutes
 6. entertainment_minutes
 7. learning_minutes
 8. sleep_estimate_hours  (gap from last prev-day event to first morning event)
 9. peak_usage_hour
10. usage_spread_entropy  (Shannon entropy of hourly distribution)
11. late_night_minutes    (23:00 – 04:00)
12. morning_minutes       (06:00 – 09:00)
13. session_count
14. avg_session_duration
15. notification_count    (placeholder; set from device if available)
"""
import json
import math
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import date, datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional, Sequence

_CATEGORY_FILE = Path(__file__).parent.parent / "data" / "app_categories.json"
_CATEGORY_MAP: Dict[str, str] = {}

try:
    with open(_CATEGORY_FILE) as f:
        _CATEGORY_MAP = json.load(f)
except FileNotFoundError:
    pass

CATEGORY_SOCIAL = "SOCIAL"
CATEGORY_PRODUCTIVITY = "PRODUCTIVITY"
CATEGORY_ENTERTAINMENT = "ENTERTAINMENT"
CATEGORY_LEARNING = "LEARNING"
CATEGORY_HEALTH = "HEALTH"
CATEGORY_UTILITY = "UTILITY"
CATEGORY_OTHER = "OTHER"

LATE_NIGHT_START = 23
LATE_NIGHT_END = 4  # inclusive wrap-around
MORNING_START = 6
MORNING_END = 9


def get_category(package_name: str) -> str:
    return _CATEGORY_MAP.get(package_name, CATEGORY_OTHER)


@dataclass
class RawEvent:
    package_name: str
    session_start: datetime
    session_end: datetime
    duration_seconds: int
    category: Optional[str] = None


@dataclass
class DailyFeatureVector:
    feature_date: date
    is_weekend: bool

    total_screen_time_minutes: float = 0.0
    unlock_count: int = 0
    unique_apps_used: int = 0
    social_minutes: float = 0.0
    productivity_minutes: float = 0.0
    entertainment_minutes: float = 0.0
    learning_minutes: float = 0.0
    sleep_estimate_hours: float = 0.0
    peak_usage_hour: int = 0
    usage_entropy: float = 0.0
    late_night_minutes: float = 0.0
    morning_minutes: float = 0.0
    session_count: int = 0
    avg_session_duration: float = 0.0
    notification_count: int = 0

    def to_dict(self) -> dict:
        return {
            "total_screen_time_minutes": self.total_screen_time_minutes,
            "unlock_count": self.unlock_count,
            "unique_apps_used": self.unique_apps_used,
            "social_minutes": self.social_minutes,
            "productivity_minutes": self.productivity_minutes,
            "entertainment_minutes": self.entertainment_minutes,
            "learning_minutes": self.learning_minutes,
            "sleep_estimate_hours": self.sleep_estimate_hours,
            "peak_usage_hour": self.peak_usage_hour,
            "usage_entropy": self.usage_entropy,
            "late_night_minutes": self.late_night_minutes,
            "morning_minutes": self.morning_minutes,
            "session_count": self.session_count,
            "avg_session_duration": self.avg_session_duration,
            "notification_count": self.notification_count,
            "is_weekend": self.is_weekend,
        }


def _shannon_entropy(counts: List[float]) -> float:
    total = sum(counts)
    if total == 0:
        return 0.0
    probs = [c / total for c in counts if c > 0]
    return -sum(p * math.log2(p) for p in probs)


def _is_late_night(hour: int) -> bool:
    return hour >= LATE_NIGHT_START or hour < LATE_NIGHT_END


def _is_morning(hour: int) -> bool:
    return MORNING_START <= hour < MORNING_END


def extract_features(
    events: Sequence[RawEvent],
    feature_date: date,
    prev_day_last_event: Optional[datetime] = None,
) -> DailyFeatureVector:
    """
    Compute a DailyFeatureVector from a list of RawEvents for feature_date.

    Args:
        events: All events for the target date (session_start falls on feature_date).
        feature_date: The date being computed.
        prev_day_last_event: The last event timestamp from the previous day (for sleep estimation).
    """
    is_weekend = feature_date.weekday() >= 5
    fv = DailyFeatureVector(feature_date=feature_date, is_weekend=is_weekend)

    if not events:
        return fv

    hourly_minutes: Dict[int, float] = defaultdict(float)
    category_minutes: Dict[str, float] = defaultdict(float)
    unique_packages: set = set()
    durations: List[float] = []

    for ev in events:
        dur_min = ev.duration_seconds / 60.0
        cat = ev.category or get_category(ev.package_name)

        fv.total_screen_time_minutes += dur_min
        category_minutes[cat] += dur_min
        unique_packages.add(ev.package_name)
        durations.append(dur_min)

        hour = ev.session_start.hour
        hourly_minutes[hour] += dur_min

        if _is_late_night(hour):
            fv.late_night_minutes += dur_min
        if _is_morning(hour):
            fv.morning_minutes += dur_min

    fv.unique_apps_used = len(unique_packages)
    fv.session_count = len(events)
    fv.avg_session_duration = (fv.total_screen_time_minutes / fv.session_count) if fv.session_count else 0.0
    fv.social_minutes = category_minutes.get(CATEGORY_SOCIAL, 0.0)
    fv.productivity_minutes = category_minutes.get(CATEGORY_PRODUCTIVITY, 0.0)
    fv.entertainment_minutes = category_minutes.get(CATEGORY_ENTERTAINMENT, 0.0)
    fv.learning_minutes = category_minutes.get(CATEGORY_LEARNING, 0.0)

    # Peak usage hour
    if hourly_minutes:
        fv.peak_usage_hour = max(hourly_minutes, key=hourly_minutes.get)

    # Shannon entropy across 24 hours
    all_counts = [hourly_minutes.get(h, 0.0) for h in range(24)]
    fv.usage_entropy = _shannon_entropy(all_counts)

    # Unlock count: approximate as number of sessions (each session = one phone pickup)
    fv.unlock_count = fv.session_count

    # Sleep estimation: gap from previous day's last event to first morning event today
    if prev_day_last_event and events:
        sorted_events = sorted(events, key=lambda e: e.session_start)
        first_event_today = sorted_events[0].session_start
        gap_hours = (first_event_today - prev_day_last_event).total_seconds() / 3600.0
        # Clamp to reasonable sleep range
        fv.sleep_estimate_hours = max(0.0, min(gap_hours, 14.0))

    return fv
