"""Tests for drift detection engine."""
import pytest
from app.ml.drift_engine import (
    compute_dimension_drift, compute_composite_drift, compute_drift_velocity,
    identify_top_contributors, DIMENSION_WEIGHTS,
)
from app.ml.baseline_model import BaselineDimension


def make_dim(mean: float, std: float) -> BaselineDimension:
    dim = BaselineDimension(dimension_name="test")
    dim.weekday_mean = mean
    dim.weekday_std = std
    dim.n_weekday_samples = 20
    return dim


def test_zero_drift_at_baseline():
    dim = make_dim(7.0, 1.0)
    z, direction = compute_dimension_drift(7.0, dim, is_weekend=False)
    assert z == 0.0
    assert direction == "stable"


def test_positive_drift_increase():
    dim = make_dim(7.0, 1.0)
    z, direction = compute_dimension_drift(10.0, dim, is_weekend=False)
    assert z == pytest.approx(3.0, abs=0.01)
    assert direction == "increase"


def test_negative_drift_decrease():
    dim = make_dim(7.0, 1.0)
    z, direction = compute_dimension_drift(4.0, dim, is_weekend=False)
    assert z == pytest.approx(-3.0, abs=0.01)
    assert direction == "decrease"


def test_prevents_division_by_zero():
    dim = make_dim(7.0, 0.0)
    z, _ = compute_dimension_drift(8.0, dim, is_weekend=False)
    assert abs(z) < 200  # capped by min std 0.01


def test_composite_drift_zero_at_baseline():
    z_scores = {k: 0.0 for k in DIMENSION_WEIGHTS}
    assert compute_composite_drift(z_scores) == 0.0


def test_composite_drift_max_at_z3():
    z_scores = {k: 3.0 for k in DIMENSION_WEIGHTS}
    score = compute_composite_drift(z_scores)
    assert score == pytest.approx(100.0, abs=0.01)


def test_composite_drift_capped_at_100():
    z_scores = {k: 10.0 for k in DIMENSION_WEIGHTS}
    score = compute_composite_drift(z_scores)
    assert score == 100.0


def test_drift_velocity_increasing():
    velocity = compute_drift_velocity([10.0, 30.0, 50.0])
    assert velocity > 0


def test_drift_velocity_decreasing():
    velocity = compute_drift_velocity([50.0, 30.0, 10.0])
    assert velocity < 0


def test_drift_velocity_stable():
    velocity = compute_drift_velocity([30.0, 30.0, 30.0])
    assert abs(velocity) < 0.1


def test_top_contributors_ordered():
    z_scores = {
        "sleep_hours": 3.0,
        "social_minutes": 1.0,
        "total_screen_time": 2.0,
        "unlock_count": 0.5,
    }
    top = identify_top_contributors(z_scores, 2)
    assert top[0] == "sleep_hours"
    assert top[1] == "total_screen_time"


def test_dimension_weights_sum_to_one():
    total = sum(DIMENSION_WEIGHTS.values())
    assert abs(total - 1.0) < 0.001
