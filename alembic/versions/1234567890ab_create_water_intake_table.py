"""Create water_intake table

Revision ID: 1234567890ab
Revises: [previous_revision_id]
Create Date: 2025-05-08 00:00:00.000000
"""

from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '1234567890ab'
down_revision = '091879aa5f25'
branch_labels = None
depends_on = None

def upgrade():
    op.create_table(
        'water_intake',
        sa.Column('intake_id', sa.Integer(), nullable=False),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('summary_id', sa.Integer(), nullable=False),
        sa.Column('intake_ml', sa.Integer(), nullable=False),
        sa.Column('intake_time', sa.BigInteger(), nullable=False),
        sa.Column('is_synced', sa.Integer(), nullable=False, server_default='0'),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['summary_id'], ['daily_water_summary.summary_id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('intake_id')
    )
    op.create_index('idx_water_intake_user_id', 'water_intake', ['user_id'], unique=False)

def downgrade():
    op.drop_index('idx_water_intake_user_id', table_name='water_intake')
    op.drop_table('water_intake')