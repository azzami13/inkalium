from logging.config import fileConfig
from sqlalchemy import engine_from_config, pool
from alembic import context
import os
import sys
from dotenv import load_dotenv

# Tambahkan path project agar bisa mengimpor modul
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

# Load variabel dari .env
load_dotenv()

# Ambil DATABASE_URL dari environment
DATABASE_URL = os.getenv("DATABASE_URL")

# Alembic Config object
alembic_config = context.config

# Set up logging
if alembic_config.config_file_name is not None:
    fileConfig(alembic_config.config_file_name)

# Set database URL ke Alembic
alembic_config.set_main_option("sqlalchemy.url", DATABASE_URL)

# Import metadata dari models
from database.database import Base
import models  # pastikan semua model sudah ter-load

target_metadata = Base.metadata

def run_migrations_offline():
    url = alembic_config.get_main_option("sqlalchemy.url")
    context.configure(
        url=url,
        target_metadata=target_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
    )
    with context.begin_transaction():
        context.run_migrations()

def run_migrations_online():
    connectable = engine_from_config(
        alembic_config.get_section(alembic_config.config_ini_section),
        prefix="sqlalchemy.",
        poolclass=pool.NullPool,
    )

    with connectable.connect() as connection:
        context.configure(
            connection=connection,
            target_metadata=target_metadata,
            compare_type=True
        )
        with context.begin_transaction():
            context.run_migrations()

if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
