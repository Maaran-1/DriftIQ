"""Tests for the feature extractor."""
from datetime import date, datetime, timezone
import pytest
from app.ml.feature_extractor import DailyFeatureVector, RawEvent, extract_features, _shannon_entropy


def make_event(package: str, hour_start: int, duration_min: int, category: str | None = None) -> RawEvent:
    start = datetime(2024, 1, 15, hour_start, 0, tzinfo=timezone.utc)
    end = datetime(2024, 1, 15, hour_start, duration_min, tzinfo=timezone.utc)
    return RawEvent(
        package_name=package,
        session_start=start,
        session_end=end,
        duration_seconds=duration_min * 60,
        category=category,
    )


def test_empty_events_returns_zero_vector():
    fv = extract_features([], date(2024, 1, 15))
    assert fv.total_screen_time_minutes == 0.0
    assert fv.session_count == 0
    assert fv.unique_apps_used == 0


def test_social_minutes_counted():
    events = [
        make_event("com.instagram.android", 14, 30, "SOCIAL"),
        make_event("com.instagram.android", 16, 15, "SOCIAL"),
    ]
    fv = extract_features(events, date(2024, 1, 15))
    assert fv.social_minutes == 45.0
    assert fv.session_count == 2
    assert fv.total_screen_time_minutes == 45.0


def test_late_night_usage_detected():
    events = [
        make_event("com.netflix.mediaclient", 23, 60, "ENTERTAINMENT"),
        make_event("com.netflix.mediaclient", 1, 30, "ENTERTAINMENT"),
    ]
    fv = extract_features(events, date(2024, 1, 15))
    assert fv.late_night_minutes == 90.0


def test_morning_usage_detected():
    events = [make_event("com.google.android.gm", 7, 20, "PRODUCTIVITY")]
    fv = extract_features(events, date(2024, 1, 15))
    assert fv.morning_minutes == 20.0


def test_sleep_estimate_from_gap():
    prev_last = datetime(2024, 1, 14, 23, 0, tzinfo=timezone.utc)
    events = [make_event("com.google.android.dialer", 7, 5)]
    fv = extract_features(events, date(2024, 1, 15), prev_day_last_event=prev_last)
    # Gap is 8 hours
    assert abs(fv.sleep_estimate_hours - 8.0) < 0.1


def test_shannon_entropy_uniform():
    # Uniform distribution → max entropy
    counts = [10.0] * 24
    entropy = _shannon_entropy(counts)
    assert entropy > 4.0  # log2(24) ≈ 4.58


def test_shannon_entropy_all_zero():
    assert _shannon_entropy([0.0] * 24) == 0.0


def test_weekend_detection():
    # 2024-01-13 is Saturday
    fv = extract_features([], date(2024, 1, 13))
    assert fv.is_weekend is True


def test_weekday_detection():
    # 2024-01-15 is Monday
    fv = extract_features([], date(2024, 1, 15))
    assert fv.is_weekend is False


def test_unique_apps_counted():
    events = [
        make_event("com.instagram.android", 10, 10, "SOCIAL"),
        make_event("com.twitter.android", 11, 5, "SOCIAL"),
        make_event("com.instagram.android", 12, 15, "SOCIAL"),
    ]
    fv = extract_features(events, date(2024, 1, 15))
    assert fv.unique_apps_used == 2
