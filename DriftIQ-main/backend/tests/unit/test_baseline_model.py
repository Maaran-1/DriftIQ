"""Tests for baseline model EWMA and confidence scoring."""
import pytest
from app.ml.baseline_model import (
    BaselineDimension, update_dimension, build_dimensions_from_dict,
    dimensions_to_dict, compute_confidence, EWMA_ALPHA, DIMENSIONS,
)


def test_initial_dimension_confidence_zero():
    dim = BaselineDimension(dimension_name="sleep_hours")
    assert dim.confidence == 0.0
    assert dim.n_samples == 0


def test_first_sample_bootstraps_mean():
    dim = BaselineDimension(dimension_name="sleep_hours")
    dim = update_dimension(dim, 7.5, is_weekend=False)
    assert dim.weekday_mean == 7.5
    assert dim.n_weekday_samples == 1


def test_ewma_update_converges():
    dim = BaselineDimension(dimension_name="sleep_hours")
    for _ in range(50):
        dim = update_dimension(dim, 7.0, is_weekend=False)
    # After 50 samples of 7.0, mean should be ~7.0
    assert abs(dim.weekday_mean - 7.0) < 0.01


def test_weekend_weekday_separate():
    dim = BaselineDimension(dimension_name="sleep_hours")
    for _ in range(10):
        dim = update_dimension(dim, 7.0, is_weekend=False)
        dim = update_dimension(dim, 9.0, is_weekend=True)
    assert dim.weekday_mean < 8.0
    assert dim.weekend_mean > 8.0


def test_confidence_grows_with_samples():
    dim = BaselineDimension(dimension_name="sleep_hours")
    for i in range(30):
        dim = update_dimension(dim, 7.0, is_weekend=i % 2 == 0)
    assert dim.confidence == 1.0


def test_build_dimensions_from_empty_dict():
    dims = build_dimensions_from_dict({})
    assert len(dims) == len(DIMENSIONS)
    for name in DIMENSIONS:
        assert name in dims


def test_serialization_roundtrip():
    dims = build_dimensions_from_dict({})
    for _ in range(5):
        for name in DIMENSIONS:
            dims[name] = update_dimension(dims[name], 5.0, False)
    serialized = dimensions_to_dict(dims)
    restored = build_dimensions_from_dict(serialized)
    for name in DIMENSIONS:
        assert abs(restored[name].weekday_mean - dims[name].weekday_mean) < 1e-6


def test_compute_confidence_average():
    dims = build_dimensions_from_dict({})
    # All zero confidence
    assert compute_confidence(dims) == 0.0
