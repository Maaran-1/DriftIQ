"""
Risk Scorer
===========
Implements spec §13 risk formula and Level 0–5 classification.
All language is behavioral only — no clinical/medical terms.
"""
from typing import Dict, Tuple, List, Union

RISK_COLORS = {
    0: "#2ECC71",  # Healthy
    1: "#F1C40F",  # Observation
    2: "#E67E22",  # Mild Concern
    3: "#E74C3C",  # Moderate Concern
    4: "#C0392B",  # High Concern
    5: "#7B241C",  # Critical
}

RISK_LABELS = {
    0: "Healthy",
    1: "Observation",
    2: "Mild Concern",
    3: "Moderate Concern",
    4: "High Concern",
    5: "Critical",
}

# Behavioral explanation templates (no medical language, no clinical terms)
EXPLANATION_TEMPLATES: Dict[str, Dict[str, str]] = {
    "sleep_hours": {
        "increase": "Your estimated sleep duration has increased compared to your usual pattern.",
        "decrease": "Your estimated sleep duration has decreased compared to your usual pattern.",
        "stable": "Your sleep pattern is in line with your baseline.",
    },
    "total_screen_time": {
        "increase": "Your total device usage is higher than your typical daily pattern.",
        "decrease": "Your total device usage is lower than your typical daily pattern.",
        "stable": "Your screen time matches your baseline.",
    },
    "unlock_count": {
        "increase": "You are picking up your phone more frequently than usual.",
        "decrease": "You are picking up your phone less frequently than usual.",
        "stable": "Your unlock pattern is consistent with your baseline.",
    },
    "social_minutes": {
        "increase": "Your social app usage has increased significantly above your baseline.",
        "decrease": "Your social app usage has dropped below your usual pattern.",
        "stable": "Your social app usage is within your usual range.",
    },
    "productivity_minutes": {
        "increase": "Your time in productivity apps is above your baseline.",
        "decrease": "Your time in productivity apps has decreased from your usual pattern.",
        "stable": "Your productivity app usage is consistent.",
    },
    "entertainment_minutes": {
        "increase": "Your entertainment consumption has increased above your typical pattern.",
        "decrease": "Your entertainment consumption is below your usual level.",
        "stable": "Your entertainment usage is consistent.",
    },
    "learning_minutes": {
        "increase": "You are spending more time in learning activities than usual.",
        "decrease": "Your learning activity time has decreased from your baseline.",
        "stable": "Your learning activity is consistent.",
    },
    "late_night_usage": {
        "increase": "Your late-night device activity (11 PM–4 AM) has increased from your baseline.",
        "decrease": "Your late-night device activity has decreased from your baseline.",
        "stable": "Your late-night usage is consistent with your pattern.",
    },
    "session_count": {
        "increase": "You are starting more device sessions than your baseline average.",
        "decrease": "You are starting fewer device sessions than your baseline average.",
        "stable": "Your session count is consistent.",
    },
    "usage_entropy": {
        "increase": "Your app usage is more spread out across apps than usual.",
        "decrease": "Your app usage is more concentrated than your typical pattern.",
        "stable": "Your usage distribution is consistent.",
    },
}

# Risk level boundaries (spec §13)
_RISK_THRESHOLDS = [
    (80.0, 5),
    (65.0, 4),
    (45.0, 3),
    (25.0, 2),
    (10.0, 1),
    (0.0,  0),
]


def compute_risk_level(
    composite_drift: float,
    drift_velocity: float,
    sustained_days: int,
    confidence: float,
) -> Tuple[int, str]:
    """
    Risk formula from spec §13.

    Risk score = composite_drift × 0.6 + |velocity| × 10 × 0.2 + sustained_days × 2 × 0.2
    Adjusted by confidence (low confidence caps the final score to prevent false positives).

    Returns (risk_level 0–5, risk_label).
    """
    base_score = (
        composite_drift * 0.6
        + abs(drift_velocity) * 10 * 0.2
        + min(sustained_days, 30) * 2 * 0.2  # cap sustained days at 30
    )
    # Low confidence dampens the score — prevents false positives during baseline buildup
    adjusted = base_score * max(confidence, 0.1)

    level = 0
    for threshold, lvl in _RISK_THRESHOLDS:
        if adjusted >= threshold:
            level = lvl
            break

    return level, RISK_LABELS[level]


def generate_risk_explanation(
    dimension_results: dict,
    level: int,
) -> str:
    """
    Build a behavioral explanation from top-3 drift contributors.
    No medical language. Behavioral framing only.

    dimension_results: dict of {dim_name: DimensionDriftResult | dict}
    """
    if not dimension_results:
        return "Your behavioral patterns are within your normal range."

    # Sort by absolute z-score (handle both dataclass and dict)
    def get_z(item: object) -> float:
        if hasattr(item, "z_score"):
            return abs(item.z_score)
        if isinstance(item, dict):
            return abs(item.get("z_score", 0.0))
        return 0.0

    def get_direction(item: object) -> str:
        if hasattr(item, "direction"):
            return item.direction
        if isinstance(item, dict):
            return item.get("direction", "stable")
        return "stable"

    sorted_dims = sorted(dimension_results.items(), key=lambda x: get_z(x[1]), reverse=True)
    top_3 = sorted_dims[:3]

    sentences = []
    for dim_name, result in top_3:
        templates = EXPLANATION_TEMPLATES.get(dim_name, {})
        direction = get_direction(result)
        sentence = templates.get(
            direction,
            f"Your {dim_name.replace('_', ' ')} has shifted from your baseline.",
        )
        sentences.append(sentence)

    if not sentences:
        return "Your behavioral patterns are within your normal range."

    prefix = {
        0: "Your behavioral patterns remain stable. ",
        1: "A slight shift in your patterns has been observed. ",
        2: "A noticeable change in your behavioral patterns has been detected. ",
        3: "A meaningful shift in your behavioral patterns is ongoing. ",
        4: "A significant change in your behavioral patterns has been sustained. ",
        5: "A substantial and sustained shift in your behavioral patterns has been detected. ",
    }.get(level, "")

    return prefix + " ".join(sentences)


def get_risk_color(level: int) -> str:
    """Return the hex color for a given risk level."""
    return RISK_COLORS.get(level, "#2ECC71")


def compute_risk_trend(history: List[int]) -> str:
    """
    Given a list of recent risk levels [oldest, ..., latest],
    return 'improving' | 'stable' | 'worsening'.

    A trend is only declared if there is a clear directional change of ≥1 level.
    """
    if len(history) < 2:
        return "stable"
    recent = history[-3:] if len(history) >= 3 else history
    first = recent[0]
    last = recent[-1]

    # Bug fix: original had `last < first - 0` which is `last < first` but
    # the intent was a meaningful threshold. We use a delta of ≥1 level.
    if last < first:
        return "improving"
    elif last > first:
        return "worsening"
    return "stable"
