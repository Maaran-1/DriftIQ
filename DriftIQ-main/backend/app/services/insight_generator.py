"""
Insight Generator
=================
Generates behavioral insights via LLM (OpenAI) with rule-based fallback.
All generated content is behavioral only — no medical, clinical, or diagnostic language.
"""
import json
import logging
from datetime import date
from pathlib import Path
from typing import Optional

from app.core.config import settings

logger = logging.getLogger(__name__)

_PROMPT_DIR = Path(__file__).parent.parent / "prompts"

# Forbidden words that must never appear in generated content
_FORBIDDEN_WORDS = [
    "diagnos", "symptom", "disorder", "depress", "anxi", "burnout",
    "mental health", "clinical", "suicid", "bipolar", "schizo", "psychos",
]


def _load_prompt(name: str) -> str:
    path = _PROMPT_DIR / name
    try:
        return path.read_text(encoding="utf-8")
    except FileNotFoundError:
        logger.debug("Prompt file not found: %s — using context string", path)
        return ""


def _sanitize_content(content: str) -> str:
    """
    Final safety check — scan for forbidden clinical/medical terms.
    Replaces any offending content with the rule-based fallback.
    This is a defense-in-depth layer; the LLM system prompt is the primary guard.
    """
    lower = content.lower()
    for word in _FORBIDDEN_WORDS:
        if word in lower:
            logger.warning(
                "LLM output contained forbidden word '%s' — replacing with fallback",
                word,
            )
            return None  # Caller should use rule-based fallback
    return content


def _rule_based_daily(data: dict) -> str:
    drift = data.get("composite_drift", 0)
    level = data.get("risk_level", 0)
    contributors = data.get("contributors", [])
    top = contributors[0].replace("_", " ") if contributors else "screen time"
    if drift < 10:
        prefix = "Your behavioral patterns are well within your usual range."
    elif drift < 30:
        prefix = "Your behavioral patterns are within your usual range."
    elif drift < 60:
        prefix = f"A shift in your {top} pattern has been detected today."
    else:
        prefix = f"A notable shift in your {top} usage pattern has been detected today."
    return (
        f"{prefix} "
        f"Your drift score is {drift:.0f}/100 and current status is {RISK_LABELS_INLINE.get(level, 'level ' + str(level))}. "
        f"Continue monitoring your routine for patterns."
    )


def _rule_based_weekly(data: dict) -> str:
    avg_drift = data.get("avg_composite_drift", 0)
    days_tracked = data.get("days_tracked", 0)
    return (
        f"This week your average behavioral drift was {avg_drift:.0f}/100 "
        f"over {days_tracked} tracked days. "
        "Review each dimension below for detailed trend information. "
        "Consistent routines in sleep and productivity tend to correlate with lower drift scores."
    )


def _rule_based_monthly(data: dict) -> str:
    avg_drift = data.get("avg_composite_drift", 0)
    max_drift = data.get("max_composite_drift", 0)
    days_tracked = data.get("days_tracked", 0)
    return (
        f"This month's behavioral data shows an average drift of {avg_drift:.0f}/100 "
        f"with a peak of {max_drift:.0f}/100 across {days_tracked} tracked days. "
        "Review your trend charts for patterns in sleep, screen time, and app usage categories. "
        "Maintaining consistent daily routines typically results in lower drift scores over time."
    )


RISK_LABELS_INLINE = {
    0: "Healthy",
    1: "Observation",
    2: "Mild Concern",
    3: "Moderate Concern",
    4: "High Concern",
    5: "Critical",
}

_LLM_SYSTEM_PROMPT = (
    "You are DriftIQ's behavioral insight engine. "
    "Your role is to generate non-medical, non-clinical behavioral observations only. "
    "DO NOT use words like: diagnose, symptom, disorder, depression, anxiety, burnout, "
    "mental health, clinical, suicide, bipolar, or any other clinical/medical terminology. "
    "Focus only on observable behavioral patterns in app usage, screen time, and sleep duration. "
    "Write in second person, clear and supportive language. "
    "Limit response to 2-3 sentences."
)


async def generate_daily_insight(
    target_date: date,
    drift_data: dict,
    twin_data: dict,
) -> str:
    """
    Generate a daily behavioral insight via LLM or rule-based fallback.
    Always returns a safe, behavioral-only string.
    """
    composite_drift = drift_data.get("composite_drift", 0)
    risk_level = drift_data.get("risk_level", 0)
    risk_label = drift_data.get("risk_label", "Healthy")
    top_contributors = drift_data.get("top_contributors", [])

    context = {
        "composite_drift": composite_drift,
        "risk_level": risk_level,
        "risk_label": risk_label,
        "contributors": top_contributors,
    }

    if not settings.OPENAI_API_KEY or settings.LLM_FALLBACK_ENABLED is False:
        return _rule_based_daily(context)

    try:
        from openai import AsyncOpenAI
        client = AsyncOpenAI(
            api_key=settings.OPENAI_API_KEY,
            timeout=settings.LLM_TIMEOUT_SECONDS,
        )

        prompt_template = _load_prompt("daily_insight.txt")
        if prompt_template:
            prompt = prompt_template.format(
                date=target_date.isoformat(),
                composite_drift=composite_drift,
                sleep_hours=drift_data.get("sleep_hours", "N/A"),
                baseline_sleep=twin_data.get("sleep_hours_baseline", "N/A"),
                screen_time=drift_data.get("screen_time", "N/A"),
                baseline_screen=twin_data.get("total_screen_time_baseline", "N/A"),
                social_min=drift_data.get("social_minutes", "N/A"),
                baseline_social=twin_data.get("social_minutes_baseline", "N/A"),
                prod_min=drift_data.get("productivity_minutes", "N/A"),
                baseline_prod=twin_data.get("productivity_minutes_baseline", "N/A"),
                risk_level=risk_level,
                risk_label=risk_label,
                contributors=", ".join(top_contributors),
            )
        else:
            prompt = (
                f"Date: {target_date}. Drift score: {composite_drift:.0f}/100. "
                f"Risk: {risk_label} (level {risk_level}). "
                f"Top contributors: {', '.join(top_contributors) or 'none'}. "
                "Generate a 2-sentence behavioral observation."
            )

        response = await client.chat.completions.create(
            model=settings.LLM_MODEL,
            messages=[
                {"role": "system", "content": _LLM_SYSTEM_PROMPT},
                {"role": "user", "content": prompt},
            ],
            max_tokens=200,
            temperature=0.7,
        )
        content = response.choices[0].message.content.strip()
        sanitized = _sanitize_content(content)
        return sanitized if sanitized is not None else _rule_based_daily(context)

    except Exception as exc:
        logger.warning(
            "LLM daily insight generation failed (date=%s): %s — using rule-based fallback",
            target_date, exc,
        )
        return _rule_based_daily(context)


async def generate_weekly_insight(
    period_start: date,
    period_end: date,
    weekly_stats: dict,
) -> str:
    """Generate a weekly behavioral insight via LLM or rule-based fallback."""
    if not settings.OPENAI_API_KEY:
        return _rule_based_weekly(weekly_stats)

    try:
        from openai import AsyncOpenAI
        client = AsyncOpenAI(
            api_key=settings.OPENAI_API_KEY,
            timeout=settings.LLM_TIMEOUT_SECONDS,
        )
        prompt_template = _load_prompt("weekly_insight.txt")
        if prompt_template:
            prompt = prompt_template.format(
                end_date=period_end.isoformat(),
                weekly_stats_json=json.dumps(weekly_stats, default=str),
            )
        else:
            prompt = (
                f"Weekly behavioral summary ({period_start} to {period_end}): "
                f"{json.dumps(weekly_stats, default=str)}. "
                "Write a 3-sentence behavioral trend summary. No medical language."
            )

        response = await client.chat.completions.create(
            model=settings.LLM_MODEL,
            messages=[
                {"role": "system", "content": _LLM_SYSTEM_PROMPT},
                {"role": "user", "content": prompt},
            ],
            max_tokens=350,
            temperature=0.7,
        )
        content = response.choices[0].message.content.strip()
        sanitized = _sanitize_content(content)
        return sanitized if sanitized is not None else _rule_based_weekly(weekly_stats)

    except Exception as exc:
        logger.warning(
            "LLM weekly insight failed (period=%s–%s): %s",
            period_start, period_end, exc,
        )
        return _rule_based_weekly(weekly_stats)


async def generate_monthly_insight(
    period_start: date,
    period_end: date,
    monthly_stats: dict,
) -> str:
    """Generate a monthly behavioral insight via LLM or rule-based fallback."""
    if not settings.OPENAI_API_KEY:
        return _rule_based_monthly(monthly_stats)

    try:
        from openai import AsyncOpenAI
        client = AsyncOpenAI(
            api_key=settings.OPENAI_API_KEY,
            timeout=settings.LLM_TIMEOUT_SECONDS,
        )
        prompt = (
            f"Monthly behavioral summary for {period_start} to {period_end}:\n"
            f"{json.dumps(monthly_stats, default=str)}\n\n"
            "Write a 4-sentence behavioral trend summary. "
            "Focus only on app usage patterns, screen time, and routines. "
            "No medical language whatsoever."
        )
        response = await client.chat.completions.create(
            model=settings.LLM_MODEL,
            messages=[
                {"role": "system", "content": _LLM_SYSTEM_PROMPT},
                {"role": "user", "content": prompt},
            ],
            max_tokens=400,
            temperature=0.7,
        )
        content = response.choices[0].message.content.strip()
        sanitized = _sanitize_content(content)
        return sanitized if sanitized is not None else _rule_based_monthly(monthly_stats)

    except Exception as exc:
        logger.warning(
            "LLM monthly insight failed (period=%s–%s): %s",
            period_start, period_end, exc,
        )
        return _rule_based_monthly(monthly_stats)
