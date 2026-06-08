import uuid
from datetime import datetime, date, timezone
from sqlalchemy import BigInteger, Boolean, Date, DateTime, Float, ForeignKey, Integer, String, Text, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column
from app.core.database import Base


class RiskRecord(Base):
    __tablename__ = "risk_records"
    __table_args__ = (UniqueConstraint("user_id", "record_date", name="uq_risk_records_user_date"),)

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    user_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    record_date: Mapped[date] = mapped_column(Date, nullable=False)
    risk_level: Mapped[int] = mapped_column(Integer, nullable=False)
    risk_label: Mapped[str | None] = mapped_column(String(50), nullable=True)
    explanation: Mapped[str | None] = mapped_column(Text, nullable=True)
    composite_drift: Mapped[float | None] = mapped_column(Float, nullable=True)
    drift_velocity: Mapped[float | None] = mapped_column(Float, nullable=True)
    is_notification_sent: Mapped[bool] = mapped_column(Boolean, default=False)
    computed_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )
