from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = 'abc456'  # Ganti dengan ID revisi yang dihasilkan Alembic
down_revision = '1234567890ab'  # Bergantung pada water intake
branch_labels = None
depends_on = None

def upgrade():
    op.create_table(
        'calorie_expenditure',
        sa.Column('id', sa.Integer, primary_key=True, index=True),
        sa.Column('user_id', sa.Integer, sa.ForeignKey('users.id'), index=True),
        sa.Column('calories', sa.Float, nullable=False),
        sa.Column('date', sa.String, nullable=False)
    )

def downgrade():
    op.drop_table('calorie_expenditure')