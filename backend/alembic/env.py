"""
Alembic migration environment — async (asyncpg) version.

Uses SQLAlchemy's async engine so no psycopg2 installation is required.
The DATABASE_URL is read from the app's Settings, which reads from .env /
environment variables. The '+asyncpg' suffix is KEPT (not stripped) because
we're using the async engine pathway.
"""
import asyncio
import os
import sys
from logging.config import fileConfig

from sqlalchemy.ext.asyncio import create_async_engine
from sqlalchemy.pool import NullPool
from alembic import context

# ── path setup so alembic can import the app ───────────────────────────────
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from app.core.config import settings
from app.core.database import Base
import app.models  # noqa — registers all ORM models with Base.metadata

# ── alembic config ─────────────────────────────────────────────────────────
config = context.config

if config.config_file_name is not None:
    fileConfig(config.config_file_name)

target_metadata = Base.metadata

# Override the sqlalchemy.url from settings (env-driven, no hardcoding)
# Keep the +asyncpg driver — we use the async engine below.
DATABASE_URL = settings.DATABASE_URL


def run_migrations_offline() -> None:
    """
    Run migrations in 'offline' mode (generate SQL without a live connection).
    Strip +asyncpg for the offline URL since it's only used for SQL generation.
    """
    url = DATABASE_URL.replace("+asyncpg", "")
    context.configure(
        url=url,
        target_metadata=target_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
    )
    with context.begin_transaction():
        context.run_migrations()


def do_run_migrations(connection):
    context.configure(connection=connection, target_metadata=target_metadata)
    with context.begin_transaction():
        context.run_migrations()


async def run_migrations_online() -> None:
    """
    Run migrations in 'online' mode using an async connection (asyncpg).
    This avoids any dependency on psycopg2.
    """
    connectable = create_async_engine(DATABASE_URL, poolclass=NullPool)
    async with connectable.connect() as connection:
        await connection.run_sync(do_run_migrations)
    await connectable.dispose()


if context.is_offline_mode():
    run_migrations_offline()
else:
    asyncio.run(run_migrations_online())
