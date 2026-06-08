import uuid
from datetime import datetime, date, timezone
from sqlalchemy import BigInteger, Boolean, Date, DateTime, Float, ForeignKey, Integer, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column
from app.core.database import Base


class DailyFeature(Base):
    __tablename__ = "daily_features"
    __table_args__ = (UniqueConstraint("user_id", "feature_date", name="uq_daily_features_user_date"),)

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    user_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    feature_date: Mapped[date] = mapped_column(Date, nullable=False)

    # Feature vector (15 features)
    total_screen_time_minutes: Mapped[float | None] = mapped_column(Float, nullable=True)
    unlock_count: Mapped[int | None] = mapped_column(Integer, nullable=True)
    unique_apps_used: Mapped[int | None] = mapped_column(Integer, nullable=True)
    social_minutes: Mapped[float | None] = mapped_column(Float, nullable=True)
    productivity_minutes: Mapped[float | None] = mapped_column(Float, nullable=True)
    entertainment_minutes: Mapped[float | None] = mapped_column(Float, nullable=True)
    learning_minutes: Mapped[float | None] = mapped_column(Float, nullable=True)
    sleep_estimate_hours: Mapped[float | None] = mapped_column(Float, nullable=True)
    peak_usage_hour: Mapped[int | None] = mapped_column(Integer, nullable=True)
    usage_entropy: Mapped[float | None] = mapped_column(Float, nullable=True)
    late_night_minutes: Mapped[float | None] = mapped_column(Float, nullable=True)
    morning_minutes: Mapped[float | None] = mapped_column(Float, nullable=True)
    session_count: Mapped[int | None] = mapped_column(Integer, nullable=True)
    avg_session_duration: Mapped[float | None] = mapped_column(Float, nullable=True)
    notification_count: Mapped[int | None] = mapped_column(Integer, nullable=True)

    is_weekend: Mapped[bool] = mapped_column(Boolean, default=False)
    computed_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )
