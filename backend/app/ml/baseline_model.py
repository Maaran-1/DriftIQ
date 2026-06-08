"""
Baseline Model
==============
Maintains a per-user, per-dimension EWMA (Exponential Weighted Moving Average)
baseline. Supports separate weekday/weekend sub-models.

Dimensions tracked (10):
  sleep_hours, total_screen_time, unlock_count, social_minutes,
  productivity_minutes, entertainment_minutes, learning_minutes,
  late_night_usage, session_count, usage_entropy
"""
import math
from dataclasses import dataclass, field, asdict
from datetime import datetime, timezone
from typing import Dict, Optional


DIMENSIONS = [
    "sleep_hours",
    "total_screen_time",
    "unlock_count",
    "social_minutes",
    "productivity_minutes",
    "entertainment_minutes",
    "learning_minutes",
    "late_night_usage",
    "session_count",
    "usage_entropy",
]

EWMA_ALPHA = 0.1  # slow adaptation — spec §10.2
MIN_SAMPLES_FOR_ACTIVE = 7
CONFIDENCE_DIVISOR = 30  # confidence = min(n/30, 1.0)


@dataclass
class BaselineDimension:
    dimension_name: str
    weekday_mean: float = 0.0
    weekday_std: float = 1.0
    weekend_mean: float = 0.0
    weekend_std: float = 1.0
    ema_alpha: float = EWMA_ALPHA
    n_weekday_samples: int = 0
    n_weekend_samples: int = 0
    last_updated: Optional[str] = None  # ISO string

    @property
    def n_samples(self) -> int:
        return self.n_weekday_samples + self.n_weekend_samples

    @property
    def confidence(self) -> float:
        return min(self.n_samples / CONFIDENCE_DIVISOR, 1.0)

    def to_dict(self) -> dict:
        return {
            "dimension_name": self.dimension_name,
            "weekday_mean": self.weekday_mean,
            "weekday_std": self.weekday_std,
            "weekend_mean": self.weekend_mean,
            "weekend_std": self.weekend_std,
            "ema_alpha": self.ema_alpha,
            "n_weekday_samples": self.n_weekday_samples,
            "n_weekend_samples": self.n_weekend_samples,
            "confidence": self.confidence,
            "n_samples": self.n_samples,
            "last_updated": self.last_updated,
        }

    @classmethod
    def from_dict(cls, d: dict) -> "BaselineDimension":
        return cls(
            dimension_name=d["dimension_name"],
            weekday_mean=d.get("weekday_mean", 0.0),
            weekday_std=d.get("weekday_std", 1.0),
            weekend_mean=d.get("weekend_mean", 0.0),
            weekend_std=d.get("weekend_std", 1.0),
            ema_alpha=d.get("ema_alpha", EWMA_ALPHA),
            n_weekday_samples=d.get("n_weekday_samples", 0),
            n_weekend_samples=d.get("n_weekend_samples", 0),
            last_updated=d.get("last_updated"),
        )


def _ewma_update(old_mean: float, old_variance: float, new_value: float, alpha: float) -> tuple[float, float]:
    """
    EWMA update rule from spec §10.2.
    Returns (new_mean, new_std).
    """
    new_mean = alpha * new_value + (1 - alpha) * old_mean
    new_variance = alpha * (new_value - new_mean) ** 2 + (1 - alpha) * old_variance
    new_std = max(math.sqrt(new_variance), 0.01)
    return new_mean, new_std


def update_dimension(dim: BaselineDimension, value: float, is_weekend: bool) -> BaselineDimension:
    """Update a single dimension baseline with a new observation."""
    now_iso = datetime.now(timezone.utc).isoformat()
    alpha = dim.ema_alpha

    if is_weekend:
        # Bootstrap: first samples
        if dim.n_weekend_samples == 0:
            dim.weekend_mean = value
            dim.weekend_std = 1.0
        else:
            old_var = dim.weekend_std ** 2
            dim.weekend_mean, dim.weekend_std = _ewma_update(dim.weekend_mean, old_var, value, alpha)
        dim.n_weekend_samples += 1
    else:
        if dim.n_weekday_samples == 0:
            dim.weekday_mean = value
            dim.weekday_std = 1.0
        else:
            old_var = dim.weekday_std ** 2
            dim.weekday_mean, dim.weekday_std = _ewma_update(dim.weekday_mean, old_var, value, alpha)
        dim.n_weekday_samples += 1

    dim.last_updated = now_iso
    return dim


def build_dimensions_from_dict(raw: dict) -> Dict[str, BaselineDimension]:
    """Reconstruct dimension objects from the JSONB blob stored in digital_twins."""
    result: Dict[str, BaselineDimension] = {}
    for name in DIMENSIONS:
        if name in raw:
            result[name] = BaselineDimension.from_dict(raw[name])
        else:
            result[name] = BaselineDimension(dimension_name=name)
    return result


def dimensions_to_dict(dims: Dict[str, BaselineDimension]) -> dict:
    """Serialize dimensions to JSONB-ready dict."""
    return {name: dim.to_dict() for name, dim in dims.items()}


def compute_confidence(dims: Dict[str, BaselineDimension]) -> float:
    """Average confidence across all dimensions."""
    if not dims:
        return 0.0
    return sum(d.confidence for d in dims.values()) / len(dims)


def get_total_baseline_days(dims: Dict[str, BaselineDimension]) -> int:
    """Estimate baseline days from max samples across dimensions."""
    if not dims:
        return 0
    return max(d.n_samples for d in dims.values())
