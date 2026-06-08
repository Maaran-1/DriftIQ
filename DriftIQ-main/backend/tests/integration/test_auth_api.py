"""
Integration tests for Auth API endpoints.
"""
import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_register_success(client: AsyncClient):
    """POST /api/v1/auth/register returns 201 with access + refresh tokens."""
    resp = await client.post("/api/v1/auth/register", json={
        "email": "newuser@test.com",
        "password": "SecurePass123!",
    })
    assert resp.status_code == 201
    data = resp.json()
    assert "access_token" in data
    assert "refresh_token" in data
    assert data["token_type"] == "bearer"


@pytest.mark.asyncio
async def test_register_duplicate_email(client: AsyncClient):
    """Registering with an existing email returns 409 Conflict."""
    payload = {"email": "dup@test.com", "password": "SecurePass123!"}
    resp1 = await client.post("/api/v1/auth/register", json=payload)
    assert resp1.status_code == 201
    resp2 = await client.post("/api/v1/auth/register", json=payload)
    assert resp2.status_code == 409


@pytest.mark.asyncio
async def test_login_success(client: AsyncClient):
    """POST /api/v1/auth/login returns tokens for valid credentials."""
    # Register first
    await client.post("/api/v1/auth/register", json={
        "email": "login@test.com",
        "password": "TestPass99!",
    })
    resp = await client.post("/api/v1/auth/login", json={
        "email": "login@test.com",
        "password": "TestPass99!",
    })
    assert resp.status_code == 200
    assert "access_token" in resp.json()


@pytest.mark.asyncio
async def test_login_wrong_password(client: AsyncClient):
    """POST /api/v1/auth/login with wrong password returns 401."""
    await client.post("/api/v1/auth/register", json={
        "email": "wrongpw@test.com",
        "password": "CorrectPass1!",
    })
    resp = await client.post("/api/v1/auth/login", json={
        "email": "wrongpw@test.com",
        "password": "WrongPassword",
    })
    assert resp.status_code == 401


@pytest.mark.asyncio
async def test_refresh_token_rotation(client: AsyncClient):
    """POST /api/v1/auth/refresh returns new access + refresh tokens."""
    reg = await client.post("/api/v1/auth/register", json={
        "email": "refresh@test.com",
        "password": "RefreshMe1!",
    })
    old_refresh = reg.json()["refresh_token"]
    old_access = reg.json()["access_token"]

    resp = await client.post("/api/v1/auth/refresh", json={"refresh_token": old_refresh})
    assert resp.status_code == 200
    data = resp.json()
    assert "access_token" in data
    assert "refresh_token" in data
    # New tokens should differ from old ones
    assert data["access_token"] != old_access
    assert data["refresh_token"] != old_refresh


@pytest.mark.asyncio
async def test_refresh_token_reuse_rejected(client: AsyncClient):
    """Reusing a refresh token after rotation returns 401."""
    reg = await client.post("/api/v1/auth/register", json={
        "email": "reuse@test.com",
        "password": "ReuseMe1!",
    })
    old_refresh = reg.json()["refresh_token"]

    # Use the refresh token once
    await client.post("/api/v1/auth/refresh", json={"refresh_token": old_refresh})

    # Second use of old token must be rejected
    resp = await client.post("/api/v1/auth/refresh", json={"refresh_token": old_refresh})
    assert resp.status_code == 401


@pytest.mark.asyncio
async def test_protected_endpoint_without_token(client: AsyncClient):
    """GET /api/v1/dashboard/summary without auth returns 403."""
    resp = await client.get("/api/v1/dashboard/summary")
    assert resp.status_code in (401, 403)


@pytest.mark.asyncio
async def test_health_endpoint(client: AsyncClient):
    """GET /health returns 200 or 503 (200 when DB available)."""
    resp = await client.get("/health")
    assert resp.status_code in (200, 503)
    data = resp.json()
    assert "status" in data
    assert data["service"] == "driftiq-api"
