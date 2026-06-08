"""Celery app configuration — broker, beat schedule, worker settings."""
from celery import Celery
from celery.schedules import crontab
from app.core.config import settings

celery_app = Celery(
    "driftiq",
    broker=settings.CELERY_BROKER_URL,
    backend=settings.CELERY_RESULT_BACKEND,
    include=[
        "app.tasks.nightly_compute",
        "app.tasks.insight_task",
        "app.tasks.notification_task",
    ],
)

celery_app.conf.update(
    # Serialization
    task_serializer="json",
    accept_content=["json"],
    result_serializer="json",
    # Time
    timezone="UTC",
    enable_utc=True,
    # Reliability
    task_track_started=True,
    task_acks_late=True,          # ACK only after task completes
    worker_prefetch_multiplier=1, # One task at a time per worker slot
    task_reject_on_worker_lost=True,  # Re-queue if worker crashes mid-task
    # Result expiry
    result_expires=86400,         # 24 hours
    # Retry defaults
    task_soft_time_limit=600,     # 10 minute soft limit
    task_time_limit=900,          # 15 minute hard limit
    # Beat schedule — all times UTC
    beat_schedule={
        # Nightly behavioral compute (default: 2:00 AM UTC)
        "nightly-compute": {
            "task": "app.tasks.nightly_compute.run_nightly_for_all_users",
            "schedule": crontab(
                hour=settings.NIGHTLY_COMPUTE_HOUR,
                minute=settings.NIGHTLY_COMPUTE_MINUTE,
            ),
        },
        # Drift alert notifications (30 min after nightly compute)
        "drift-notifications": {
            "task": "app.tasks.notification_task.send_drift_alerts",
            "schedule": crontab(
                hour=settings.NIGHTLY_COMPUTE_HOUR,
                minute=(settings.NIGHTLY_COMPUTE_MINUTE + 30) % 60,
            ),
        },
        # Weekly insights — Sunday evening UTC
        "weekly-insights": {
            "task": "app.tasks.insight_task.generate_weekly_insights_for_all",
            "schedule": crontab(hour=20, minute=0, day_of_week="sunday"),
        },
        # Monthly insights — 1st of each month
        "monthly-insights": {
            "task": "app.tasks.insight_task.generate_monthly_insights_for_all",
            "schedule": crontab(hour=21, minute=0, day_of_month="1"),
        },
    },
)
