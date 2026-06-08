import uuid
from datetime import datetime, date, timezone
from sqlalchemy import BigInteger, Date, DateTime, Float, ForeignKey, Integer, UniqueConstraint
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column
from app.core.database import Base


class DriftScore(Base):
    __tablename__ = "drift_scores"
    __table_args__ = (UniqueConstraint("user_id", "score_date", name="uq_drift_scores_user_date"),)

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    user_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    score_date: Mapped[date] = mapped_column(Date, nullable=False)
    composite_drift: Mapped[float | None] = mapped_column(Float, nullable=True)
    drift_velocity: Mapped[float | None] = mapped_column(Float, nullable=True)
    dimension_z_scores: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
    top_contributors: Mapped[list | None] = mapped_column(JSONB, nullable=True)
    sustained_days: Mapped[int] = mapped_column(Integer, default=0)
    computed_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )
