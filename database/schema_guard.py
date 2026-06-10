from sqlalchemy import inspect, text
from sqlalchemy.engine import Engine


def ensure_runtime_schema(engine: Engine) -> None:
    inspector = inspect(engine)
    if "users" not in inspector.get_table_names():
        return

    existing_columns = {column["name"] for column in inspector.get_columns("users")}
    required_user_columns = {
        "role": "VARCHAR",
        "last_login": "TIMESTAMP",
        "login_count": "INTEGER DEFAULT 0",
        "username": "VARCHAR",
        "umur": "INTEGER",
        "jenis_kelamin": "VARCHAR",
        "berat_badan": "DOUBLE PRECISION",
        "tinggi_badan": "DOUBLE PRECISION",
        "dibuat_pada": "TIMESTAMP",
        "profile_picture": "VARCHAR",
    }

    with engine.begin() as connection:
        for column_name, column_type in required_user_columns.items():
            if column_name not in existing_columns:
                connection.execute(
                    text(f'ALTER TABLE users ADD COLUMN IF NOT EXISTS "{column_name}" {column_type}')
                )
