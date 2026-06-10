"""add activity sessions

Revision ID: 20260609_activity_sessions
Revises: 51ef3d990b9b
Create Date: 2026-06-09
"""

from alembic import op
import sqlalchemy as sa


revision = "20260609_activity_sessions"
down_revision = "51ef3d990b9b"
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
        "activity_sessions",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("activity_type", sa.String(), nullable=False),
        sa.Column("uses_location", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("start_time", sa.DateTime(), nullable=False),
        sa.Column("end_time", sa.DateTime(), nullable=False),
        sa.Column("duration_seconds", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("total_distance", sa.Float(), nullable=False, server_default="0"),
        sa.Column("avg_speed", sa.Float(), nullable=False, server_default="0"),
        sa.Column("calories_burned", sa.Float(), nullable=False, server_default="0"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id")
    )
    op.create_index("ix_activity_sessions_id", "activity_sessions", ["id"], unique=False)
    op.create_index("ix_activity_sessions_user_id", "activity_sessions", ["user_id"], unique=False)

    op.create_table(
        "activity_location_points",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("session_id", sa.Integer(), nullable=False),
        sa.Column("latitude", sa.Float(), nullable=False),
        sa.Column("longitude", sa.Float(), nullable=False),
        sa.Column("altitude", sa.Float(), nullable=True),
        sa.Column("accuracy", sa.Float(), nullable=True),
        sa.Column("timestamp", sa.DateTime(), nullable=False),
        sa.ForeignKeyConstraint(["session_id"], ["activity_sessions.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id")
    )
    op.create_index("ix_activity_location_points_id", "activity_location_points", ["id"], unique=False)
    op.create_index("ix_activity_location_points_session_id", "activity_location_points", ["session_id"], unique=False)


def downgrade():
    op.drop_index("ix_activity_location_points_session_id", table_name="activity_location_points")
    op.drop_index("ix_activity_location_points_id", table_name="activity_location_points")
    op.drop_table("activity_location_points")

    op.drop_index("ix_activity_sessions_user_id", table_name="activity_sessions")
    op.drop_index("ix_activity_sessions_id", table_name="activity_sessions")
    op.drop_table("activity_sessions")
