"""
FCM Notification Task — sends push notifications for drift alerts.
Firebase Admin SDK is used for reliable Android push delivery.
"""
import asyncio
import logging
import uuid
from datetime import date, datetime, timedelta, timezone

from app.tasks.celery_app import celery_app

logger = logging.getLogger(__name__)


@celery_app.task(
    name="app.tasks.notification_task.send_drift_alerts",
    bind=True,
    max_retries=3,
    default_retry_delay=300,  # 5 minutes between retries
)
def send_drift_alerts(self):
    """
    Find users with unsent drift alert notifications and send FCM pushes.
    Triggered after nightly compute finishes.
    """
    asyncio.run(_send_all_alerts())


async def _send_all_alerts():
    """Find and send all pending drift alert notifications."""
    from app.core.database import AsyncSessionLocal
    from app.models.risk_record import RiskRecord
    from app.models.user import User
    from sqlalchemy import select

    today = date.today()

    async with AsyncSessionLocal() as db:
        # Find risk records from today that haven't been notified yet
        result = await db.execute(
            select(RiskRecord, User)
            .join(User, RiskRecord.user_id == User.id)
            .where(
                RiskRecord.record_date == today,
                RiskRecord.is_notification_sent == False,
                RiskRecord.risk_level >= 2,  # Only notify for Mild Concern and above
                User.is_active == True,
                User.notifications_enabled == True,
                User.fcm_token.isnot(None),
                User.drift_alert_threshold <= RiskRecord.composite_drift,
            )
        )
        rows = result.all()

        sent_count = 0
        for risk_rec, user in rows:
            if not user.fcm_token:
                continue
            success = await _send_fcm_notification(
                fcm_token=user.fcm_token,
                risk_level=risk_rec.risk_level,
                risk_label=risk_rec.risk_label or "Concern",
                wellness_score=max(0, 100 - int(risk_rec.composite_drift or 0)),
                composite_drift=risk_rec.composite_drift or 0.0,
            )
            if success:
                risk_rec.is_notification_sent = True
                sent_count += 1

        await db.commit()
        logger.info("Drift alert notifications sent: %d", sent_count)


async def _send_fcm_notification(
    fcm_token: str,
    risk_level: int,
    risk_label: str,
    wellness_score: int,
    composite_drift: float,
) -> bool:
    """
    Send a push notification via Firebase Cloud Messaging.
    Returns True if sent successfully, False otherwise.
    """
    from app.core.config import settings

    if not settings.FIREBASE_ENABLED:
        logger.debug("Firebase disabled — skipping FCM push")
        return False

    try:
        import firebase_admin
        from firebase_admin import credentials, messaging

        # Initialize Firebase Admin SDK (idempotent)
        if not firebase_admin._apps:
            cred = credentials.Certificate(settings.FIREBASE_CREDENTIALS_PATH)
            firebase_admin.initialize_app(cred)

        title = _build_notification_title(risk_level, risk_label)
        body = _build_notification_body(risk_level, wellness_score, composite_drift)

        message = messaging.Message(
            token=fcm_token,
            notification=messaging.Notification(title=title, body=body),
            data={
                "type": "drift_alert",
                "risk_level": str(risk_level),
                "risk_label": risk_label,
                "wellness_score": str(wellness_score),
                "composite_drift": f"{composite_drift:.1f}",
                "action": "open_dashboard",
            },
            android=messaging.AndroidConfig(
                priority="high",
                notification=messaging.AndroidNotification(
                    channel_id="driftiq_drift_alerts",
                    icon="ic_notification",
                    color="#6C63FF",
                ),
            ),
        )

        messaging.send(message)
        logger.info(
            "FCM push sent: risk_level=%d wellness=%d",
            risk_level, wellness_score,
        )
        return True

    except Exception as exc:
        logger.warning("FCM notification failed: %s", exc)
        return False


def _build_notification_title(risk_level: int, risk_label: str) -> str:
    """Build a non-alarmist, behavioral notification title."""
    titles = {
        0: "Your patterns are on track",
        1: "A small shift in your patterns",
        2: "Behavioral pattern change detected",
        3: "Notable change in your patterns",
        4: "Sustained pattern change",
        5: "Your patterns need attention",
    }
    return titles.get(risk_level, f"DriftIQ Alert — {risk_label}")


def _build_notification_body(
    risk_level: int, wellness_score: int, composite_drift: float
) -> str:
    """Build a non-alarming, non-medical notification body."""
    if risk_level <= 1:
        return f"Wellness score: {wellness_score}/100. Tap to view your daily report."
    elif risk_level == 2:
        return (
            f"Your behavioral drift score is {composite_drift:.0f}/100. "
            "Open DriftIQ to see which patterns have shifted."
        )
    elif risk_level == 3:
        return (
            f"Drift score: {composite_drift:.0f}/100. "
            "Your app usage patterns have shifted meaningfully. Tap to review."
        )
    else:
        return (
            f"Drift score: {composite_drift:.0f}/100. "
            "Sustained changes in your behavioral patterns detected. Tap to view insights."
        )
