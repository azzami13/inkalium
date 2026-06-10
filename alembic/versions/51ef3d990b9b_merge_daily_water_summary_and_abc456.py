"""merge daily water summary and abc456

Revision ID: 51ef3d990b9b
Revises: 55703cea55b7, abc456
Create Date: 2025-05-19 13:20:22.821783

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '51ef3d990b9b'
down_revision: Union[str, None] = ('55703cea55b7', 'abc456')
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    pass


def downgrade() -> None:
    """Downgrade schema."""
    pass
