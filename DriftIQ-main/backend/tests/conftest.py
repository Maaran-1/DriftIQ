"""
pytest configuration and shared fixtures.

Uses SQLite (via aiosqlite) for unit tests — avoids requiring a live Postgres.
PostgreSQL-specific types (JSONB, UUID) are handled via type override or
tested against the full Docker stack in integration tests.
"""
import asyncio
import uuid
from datetime import datetime, timezone

import pytest
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from sqlalchemy import event, text
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine, async_sessionmaker

from app.core.database import Base, get_db
from app.main import app

# SQLite for unit tests — fast, no external dependencies
TEST_DATABASE_URL = "sqlite+aiosqlite:///./test.db"


@pytest.fixture(scope="session")
def event_loop():
    """Create a single event loop for the test session."""
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


@pytest_asyncio.fixture(scope="function")
async def db_engine():
    """Create a fresh in-memory SQLite DB for each test function."""
    engine = create_async_engine(
        TEST_DATABASE_URL,
        echo=False,
        connect_args={"check_same_thread": False},
    )

    # SQLite compatibility patches for PostgreSQL-specific types
    @event.listens_for(engine.sync_engine, "connect")
    def set_sqlite_pragma(dbapi_connection, connection_record):
        cursor = dbapi_connection.cursor()
        cursor.execute("PRAGMA foreign_keys=ON")
        cursor.close()

    async with engine.begin() as conn:
        # Patch: SQLite doesn't support JSONB — use JSON text
        await conn.run_sync(Base.metadata.create_all)

    yield engine

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
    await engine.dispose()


@pytest_asyncio.fixture(scope="function")
async def db_session(db_engine):
    """Provide a transaction-scoped test session that rolls back after each test."""
    SessionLocal = async_sessionmaker(
        bind=db_engine,
        expire_on_commit=False,
        class_=AsyncSession,
    )
    async with SessionLocal() as session:
        yield session
        await session.rollback()


@pytest_asyncio.fixture(scope="function")
async def client(db_session: AsyncSession):
    """HTTP test client with DB session override."""
    async def override_get_db():
        yield db_session

    app.dependency_overrides[get_db] = override_get_db
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as c:
        yield c
    app.dependency_overrides.clear()


# ── Test data factories ──────────────────────────────────────────

async def create_test_user(db: AsyncSession, email: str = "test@example.com", password: str = "TestPass123!"):
    """Create a test user and return (user, access_token, refresh_token)."""
    from app.services.auth_service import register_user
    user, access_token, refresh_token = await register_user(db, email, password)
    await db.commit()
    return user, access_token, refresh_token


@pytest.fixture
def user_headers():
    """Return auth headers for a test token (does not hit DB)."""
    from app.core.security import create_access_token
    token = create_access_token(str(uuid.uuid4()), "test@example.com")
    return {"Authorization": f"Bearer {token}"}
