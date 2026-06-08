"""Tests for risk scorer."""
import pytest
from app.ml.risk_scorer import compute_risk_level, generate_risk_explanation, get_risk_color, compute_risk_trend, RISK_LABELS


def test_level_0_healthy():
    level, label = compute_risk_level(5.0, 0.0, 0, 1.0)
    assert level == 0
    assert label == "Healthy"


def test_level_1_observation():
    level, label = compute_risk_level(30.0, 0.0, 0, 1.0)
    assert level == 1


def test_level_2_mild_concern():
    level, label = compute_risk_level(60.0, 0.5, 3, 1.0)
    assert level == 2


def test_level_5_critical():
    level, label = compute_risk_level(100.0, 5.0, 30, 1.0)
    assert level == 5


def test_low_confidence_reduces_level():
    level_full, _ = compute_risk_level(80.0, 2.0, 10, 1.0)
    level_low, _ = compute_risk_level(80.0, 2.0, 10, 0.1)
    assert level_low < level_full


def test_all_levels_have_labels():
    for i in range(6):
        assert i in RISK_LABELS


def test_risk_colors_all_defined():
    from app.ml.risk_scorer import RISK_COLORS
    for i in range(6):
        color = get_risk_color(i)
        assert color.startswith("#")


def test_explanation_no_medical_language():
    # Simulate dimension results dict
    class FakeDim:
        z_score = 2.5
        direction = "increase"

    dim_results = {"social_minutes": FakeDim(), "sleep_hours": FakeDim(), "late_night_usage": FakeDim()}
    explanation = generate_risk_explanation(dim_results, 2)
    forbidden = ["diagnos", "symptom", "disorder", "depress", "anxi", "burnout", "mental health", "clinical"]
    for word in forbidden:
        assert word not in explanation.lower(), f"Found forbidden word '{word}' in explanation"


def test_trend_improving():
    assert compute_risk_trend([3, 2, 1]) == "improving"


def test_trend_worsening():
    assert compute_risk_trend([1, 2, 3]) == "worsening"


def test_trend_stable():
    assert compute_risk_trend([2, 2, 2]) == "stable"


def test_trend_single_value():
    assert compute_risk_trend([2]) == "stable"
