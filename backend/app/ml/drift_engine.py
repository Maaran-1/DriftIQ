"""
Drift Detection Engine
======================
Implements the spec §12 drift formulas:
 - Per-dimension z-score
 - Composite drift score (0–100)
 - Drift velocity (3-day rolling slope)
 - Drift direction tagging
 - Top contributor identification
"""
from dataclasses import dataclass
from typing import Dict, List, Optional, Sequence, Tuple

import numpy as np

from app.ml.baseline_model import BaselineDimension, DIMENSIONS

# Dimension weights per spec §12
DIMENSION_WEIGHTS: Dict[str, float] = {
    "sleep_hours": 0.20,
    "total_screen_time": 0.15,
    "unlock_count": 0.10,
    "social_minutes": 0.12,
    "productivity_minutes": 0.10,
    "entertainment_minutes": 0.08,
    "learning_minutes": 0.08,
    "late_night_usage": 0.10,
    "session_count": 0.05,
    "usage_entropy": 0.02,
}


@dataclass
class DimensionDriftResult:
    dimension: str
    z_score: float
    direction: str  # increase | decrease | stable
    value: float
    baseline_value: float


def compute_dimension_drift(
    value: float,
    dimension: BaselineDimension,
    is_weekend: bool,
) -> Tuple[float, str]:
    """
    Compute z-score for one dimension (spec §12).
    Returns (z_score, direction).
    """
    mean = dimension.weekend_mean if is_weekend else dimension.weekday_mean
    std = dimension.weekend_std if is_weekend else dimension.weekday_std
    if std < 0.01:
        std = 0.01
    z = (value - mean) / std
    if abs(z) < 0.5:
        direction = "stable"
    elif z > 0:
        direction = "increase"
    else:
        direction = "decrease"
    return round(z, 4), direction


def compute_composite_drift(z_scores: Dict[str, float]) -> float:
    """
    Weighted composite drift score, normalized to 0–100 (spec §12).
    A z-score of 3 maps to ~100.
    """
    weighted_sum = sum(
        abs(z_scores.get(k, 0.0)) * DIMENSION_WEIGHTS.get(k, 0.0)
        for k in DIMENSION_WEIGHTS
    )
    normalized = min((weighted_sum / 3.0) * 100, 100.0)
    return round(normalized, 2)


def compute_drift_velocity(drift_history: Sequence[float]) -> float:
    """
    3-day rolling slope of composite drift scores (spec §12).
    drift_history: [d-2, d-1, d_today] (most recent last)
    """
    if len(drift_history) < 2:
        return 0.0
    n = min(len(drift_history), 3)
    recent = list(drift_history[-n:])
    x = list(range(n))
    # Linear regression slope
    x_arr = np.array(x, dtype=float)
    y_arr = np.array(recent, dtype=float)
    if np.std(x_arr) == 0:
        return 0.0
    slope = float(np.polyfit(x_arr, y_arr, 1)[0])
    return round(slope, 4)


def identify_top_contributors(z_scores: Dict[str, float], n: int = 3) -> List[str]:
    """Return top-n dimensions by absolute z-score."""
    sorted_dims = sorted(z_scores.items(), key=lambda x: abs(x[1]), reverse=True)
    return [d for d, _ in sorted_dims[:n]]


def compute_daily_drift(
    feature_values: Dict[str, float],
    dimensions: Dict[str, BaselineDimension],
    is_weekend: bool,
) -> Tuple[Dict[str, DimensionDriftResult], float, List[str]]:
    """
    Full drift computation for one day.

    Returns:
        dimension_results: per-dimension drift results
        composite_drift: 0–100 composite score
        top_contributors: top 3 dimension names
    """
    z_scores: Dict[str, float] = {}
    dimension_results: Dict[str, DimensionDriftResult] = {}

    for dim_name in DIMENSIONS:
        if dim_name not in dimensions or dim_name not in feature_values:
            continue
        dim = dimensions[dim_name]
        if dim.n_samples < 3:  # not enough data
            continue
        value = feature_values[dim_name]
        z, direction = compute_dimension_drift(value, dim, is_weekend)
        z_scores[dim_name] = z
        mean = dim.weekend_mean if is_weekend else dim.weekday_mean
        dimension_results[dim_name] = DimensionDriftResult(
            dimension=dim_name,
            z_score=z,
            direction=direction,
            value=value,
            baseline_value=mean,
        )

    composite = compute_composite_drift(z_scores)
    top_contributors = identify_top_contributors(z_scores)
    return dimension_results, composite, top_contributors


def map_feature_vector_to_dimensions(fv_dict: dict) -> Dict[str, float]:
    """Map DailyFeatureVector fields to dimension names."""
    return {
        "sleep_hours": fv_dict.get("sleep_estimate_hours", 0.0),
        "total_screen_time": fv_dict.get("total_screen_time_minutes", 0.0),
        "unlock_count": float(fv_dict.get("unlock_count", 0)),
        "social_minutes": fv_dict.get("social_minutes", 0.0),
        "productivity_minutes": fv_dict.get("productivity_minutes", 0.0),
        "entertainment_minutes": fv_dict.get("entertainment_minutes", 0.0),
        "learning_minutes": fv_dict.get("learning_minutes", 0.0),
        "late_night_usage": fv_dict.get("late_night_minutes", 0.0),
        "session_count": float(fv_dict.get("session_count", 0)),
        "usage_entropy": fv_dict.get("usage_entropy", 0.0),
    }
