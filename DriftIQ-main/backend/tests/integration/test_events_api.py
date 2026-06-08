"""
Integration tests for Events ingestion API.
"""
import pytest
from datetime import datetime, timezone, timedelta
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_batch_events_accepted(client: AsyncClient):
    """POST /api/v1/events/batch returns 202 with accepted count."""
    # Register and get token
    reg = await client.post("/api/v1/auth/register", json={
        "email": "events@test.com",
        "password": "EventsPass1!",
    })
    token = reg.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    now = datetime.now(timezone.utc)
    events = [
        {
            "package_name": "com.instagram.android",
            "session_start": (now - timedelta(hours=2)).isoformat(),
            "session_end": (now - timedelta(hours=1, minutes=30)).isoformat(),
            "duration_seconds": 1800,
        },
        {
            "package_name": "com.netflix.mediaclient",
            "session_start": (now - timedelta(hours=1)).isoformat(),
            "session_end": (now - timedelta(minutes=30)).isoformat(),
            "duration_seconds": 1800,
        },
    ]

    resp = await client.post(
        "/api/v1/events/batch",
        json={"device_id": "test-device-001", "events": events},
        headers=headers,
    )
    assert resp.status_code == 202
    data = resp.json()
    assert data["accepted"] == 2
    assert data["duplicate_skipped"] == 0
    assert data["invalid_skipped"] == 0


@pytest.mark.asyncio
async def test_future_events_rejected(client: AsyncClient):
    """Events with future timestamps are rejected."""
    reg = await client.post("/api/v1/auth/register", json={
        "email": "future@test.com",
        "password": "FuturePass1!",
    })
    token = reg.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    future = datetime.now(timezone.utc) + timedelta(hours=2)
    resp = await client.post(
        "/api/v1/events/batch",
        json={
            "device_id": "test-device-002",
            "events": [{
                "package_name": "com.test.app",
                "session_start": future.isoformat(),
                "session_end": (future + timedelta(minutes=30)).isoformat(),
                "duration_seconds": 1800,
            }],
        },
        headers=headers,
    )
    assert resp.status_code == 202
    assert resp.json()["invalid_skipped"] == 1
    assert resp.json()["accepted"] == 0


@pytest.mark.asyncio
async def test_duplicate_events_skipped(client: AsyncClient):
    """Posting the same event twice skips the duplicate."""
    reg = await client.post("/api/v1/auth/register", json={
        "email": "dedup@test.com",
        "password": "DedupPass1!",
    })
    token = reg.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    now = datetime.now(timezone.utc)
    event_data = {
        "package_name": "com.test.dedup",
        "session_start": (now - timedelta(hours=1)).isoformat(),
        "session_end": (now - timedelta(minutes=45)).isoformat(),
        "duration_seconds": 900,
    }
    batch = {"device_id": "test-device-003", "events": [event_data]}

    r1 = await client.post("/api/v1/events/batch", json=batch, headers=headers)
    assert r1.json()["accepted"] == 1

    r2 = await client.post("/api/v1/events/batch", json=batch, headers=headers)
    assert r2.json()["duplicate_skipped"] == 1
    assert r2.json()["accepted"] == 0


@pytest.mark.asyncio
async def test_events_require_auth(client: AsyncClient):
    """Events endpoint rejects unauthenticated requests."""
    resp = await client.post(
        "/api/v1/events/batch",
        json={"device_id": "x", "events": []},
    )
    assert resp.status_code in (401, 403)
