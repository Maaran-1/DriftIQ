"""Initial schema — all DriftIQ tables.

Revision ID: 0001
Revises:
Create Date: 2026-01-01 00:00:00
"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision = "0001"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Users
    op.create_table(
        "users",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("email", sa.String(255), nullable=False, unique=True),
        sa.Column("password_hash", sa.String(255), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default="true"),
        sa.Column("consent_given_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("data_retention_days", sa.Integer(), nullable=False, server_default="365"),
        sa.Column("drift_alert_threshold", sa.Float(), nullable=False, server_default="40.0"),
        sa.Column("notifications_enabled", sa.Boolean(), nullable=False, server_default="true"),
        sa.Column("fcm_token", sa.String(512), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
    )
    op.create_index("ix_users_email", "users", ["email"])

    # App Usage Events
    op.create_table(
        "app_usage_events",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("package_name", sa.String(255), nullable=False),
        sa.Column("app_category", sa.String(50), nullable=True),
        sa.Column("session_start", sa.DateTime(timezone=True), nullable=False),
        sa.Column("session_end", sa.DateTime(timezone=True), nullable=False),
        sa.Column("duration_seconds", sa.Integer(), nullable=False),
        sa.Column("event_date", sa.Date(), nullable=False),
        sa.Column("device_id", sa.String(255), nullable=True),
        sa.Column("synced_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
    )
    op.create_index("idx_usage_events_user_date", "app_usage_events", ["user_id", "event_date"])

    # Daily Features
    op.create_table(
        "daily_features",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("feature_date", sa.Date(), nullable=False),
        sa.Column("total_screen_time_minutes", sa.Float(), nullable=True),
        sa.Column("unlock_count", sa.Integer(), nullable=True),
        sa.Column("unique_apps_used", sa.Integer(), nullable=True),
        sa.Column("social_minutes", sa.Float(), nullable=True),
        sa.Column("productivity_minutes", sa.Float(), nullable=True),
        sa.Column("entertainment_minutes", sa.Float(), nullable=True),
        sa.Column("learning_minutes", sa.Float(), nullable=True),
        sa.Column("sleep_estimate_hours", sa.Float(), nullable=True),
        sa.Column("peak_usage_hour", sa.Integer(), nullable=True),
        sa.Column("usage_entropy", sa.Float(), nullable=True),
        sa.Column("late_night_minutes", sa.Float(), nullable=True),
        sa.Column("morning_minutes", sa.Float(), nullable=True),
        sa.Column("session_count", sa.Integer(), nullable=True),
        sa.Column("avg_session_duration", sa.Float(), nullable=True),
        sa.Column("notification_count", sa.Integer(), nullable=True),
        sa.Column("is_weekend", sa.Boolean(), nullable=False, server_default="false"),
        sa.Column("computed_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
        sa.UniqueConstraint("user_id", "feature_date", name="uq_daily_features_user_date"),
    )

    # Digital Twins
    op.create_table(
        "digital_twins",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False, unique=True),
        sa.Column("version", sa.Integer(), nullable=False, server_default="1"),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default="false"),
        sa.Column("baseline_days", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("confidence_score", sa.Float(), nullable=False, server_default="0.0"),
        sa.Column("dimensions", postgresql.JSONB(), nullable=False, server_default="{}"),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
    )

    # Twin Snapshots
    op.create_table(
        "twin_snapshots",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("twin_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("digital_twins.id", ondelete="CASCADE"), nullable=False),
        sa.Column("snapshot_date", sa.Date(), nullable=False),
        sa.Column("reason", sa.String(100), nullable=True),
        sa.Column("dimensions_snapshot", postgresql.JSONB(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
    )

    # Drift Scores
    op.create_table(
        "drift_scores",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("score_date", sa.Date(), nullable=False),
        sa.Column("composite_drift", sa.Float(), nullable=True),
        sa.Column("drift_velocity", sa.Float(), nullable=True),
        sa.Column("dimension_z_scores", postgresql.JSONB(), nullable=True),
        sa.Column("top_contributors", postgresql.JSONB(), nullable=True),
        sa.Column("sustained_days", sa.Integer(), server_default="0"),
        sa.Column("computed_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
        sa.UniqueConstraint("user_id", "score_date", name="uq_drift_scores_user_date"),
    )
    op.create_index("idx_drift_scores_user_date", "drift_scores", ["user_id", "score_date"])

    # Risk Records
    op.create_table(
        "risk_records",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("record_date", sa.Date(), nullable=False),
        sa.Column("risk_level", sa.Integer(), nullable=False),
        sa.Column("risk_label", sa.String(50), nullable=True),
        sa.Column("explanation", sa.Text(), nullable=True),
        sa.Column("composite_drift", sa.Float(), nullable=True),
        sa.Column("drift_velocity", sa.Float(), nullable=True),
        sa.Column("is_notification_sent", sa.Boolean(), server_default="false"),
        sa.Column("computed_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
        sa.UniqueConstraint("user_id", "record_date", name="uq_risk_records_user_date"),
    )

    # Drift Events
    op.create_table(
        "drift_events",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("event_date", sa.Date(), nullable=False),
        sa.Column("event_type", sa.String(50), nullable=True),
        sa.Column("dimension", sa.String(50), nullable=True),
        sa.Column("z_score", sa.Float(), nullable=True),
        sa.Column("composite_drift", sa.Float(), nullable=True),
        sa.Column("risk_level", sa.Integer(), nullable=True),
        sa.Column("is_read", sa.Boolean(), server_default="false"),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
    )

    # AI Insights
    op.create_table(
        "ai_insights",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("insight_type", sa.String(20), nullable=False),
        sa.Column("period_start", sa.Date(), nullable=False),
        sa.Column("period_end", sa.Date(), nullable=False),
        sa.Column("content", sa.Text(), nullable=False),
        sa.Column("wellness_score", sa.Integer(), nullable=True),
        sa.Column("risk_level", sa.Integer(), nullable=True),
        sa.Column("metadata", postgresql.JSONB(), nullable=True),
        sa.Column("generated_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
    )

    # Refresh Tokens
    op.create_table(
        "refresh_tokens",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, server_default=sa.text("gen_random_uuid()")),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("token_hash", sa.String(255), nullable=False, unique=True),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("revoked", sa.Boolean(), server_default="false"),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
    )


def downgrade() -> None:
    for table in [
        "refresh_tokens", "ai_insights", "drift_events", "risk_records",
        "drift_scores", "twin_snapshots", "digital_twins", "daily_features",
        "app_usage_events", "users",
    ]:
        op.drop_table(table)
