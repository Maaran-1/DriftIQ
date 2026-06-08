"""Auth dependency — extracts and validates Bearer JWT from requests."""
import logging

from fastapi import Depends, HTTPException, Request, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import decode_access_token
from app.models.user import User
from app.services.auth_service import get_current_user

logger = logging.getLogger(__name__)

_bearer = HTTPBearer(auto_error=True)


async def get_current_user_dep(
    request: Request,
    credentials: HTTPAuthorizationCredentials = Depends(_bearer),
    db: AsyncSession = Depends(get_db),
) -> User:
    """
    FastAPI dependency that authenticates the current request.

    - Extracts Bearer token from Authorization header
    - Decodes and validates the JWT
    - Loads the active user from DB
    - Raises 401 on any failure

    Structured logging ensures auth failures are traceable without
    exposing sensitive details.
    """
    token = credentials.credentials
    payload = decode_access_token(token)

    if not payload:
        client_host = getattr(request.client, "host", "unknown")
        logger.warning(
            "Auth failed: invalid/expired token from %s path=%s",
            client_host,
            request.url.path,
        )
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    try:
        user = await get_current_user(db, payload["sub"])
    except Exception as exc:
        logger.warning(
            "Auth failed: user load error sub=%s path=%s error=%s",
            payload.get("sub"),
            request.url.path,
            exc,
        )
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found or deactivated",
            headers={"WWW-Authenticate": "Bearer"},
        )

    return user
