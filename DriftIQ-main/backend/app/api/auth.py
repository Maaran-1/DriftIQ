"""Auth routes — registration, login, token refresh, logout."""
import logging

from fastapi import APIRouter, Depends, Request
from slowapi import Limiter
from slowapi.util import get_remote_address
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.schemas.auth import (
    AccessTokenResponse, LoginRequest, LogoutRequest,
    RefreshRequest, RegisterRequest, TokenResponse,
)
from app.services.auth_service import (
    login_user, refresh_access_token,
    register_user, revoke_refresh_token,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/auth", tags=["auth"])
_limiter = Limiter(key_func=get_remote_address)


@router.post(
    "/register",
    response_model=TokenResponse,
    status_code=201,
    summary="Create a new account",
)
@_limiter.limit("5/minute")
async def register(
    request: Request,  # required by slowapi
    req: RegisterRequest,
    db: AsyncSession = Depends(get_db),
):
    """
    Register a new user account.
    Rate limited to 5 attempts per minute per IP.
    Returns JWT access + refresh token pair.
    """
    user, access_token, refresh_token = await register_user(db, req.email, req.password)
    logger.info("New user registered: %s", user.id)
    return TokenResponse(access_token=access_token, refresh_token=refresh_token)


@router.post(
    "/login",
    response_model=TokenResponse,
    summary="Authenticate and receive tokens",
)
@_limiter.limit("10/minute")
async def login(
    request: Request,
    req: LoginRequest,
    db: AsyncSession = Depends(get_db),
):
    """
    Authenticate with email/password.
    Rate limited to 10 attempts per minute per IP.
    Returns JWT access + refresh token pair.
    """
    user, access_token, refresh_token = await login_user(db, req.email, req.password)
    logger.info("User logged in: %s", user.id)
    return TokenResponse(access_token=access_token, refresh_token=refresh_token)


@router.post(
    "/refresh",
    response_model=TokenResponse,
    summary="Rotate refresh token and get new access token",
)
@_limiter.limit("30/minute")
async def refresh(
    request: Request,
    req: RefreshRequest,
    db: AsyncSession = Depends(get_db),
):
    """
    Exchange a valid refresh token for a new access + refresh token pair.
    The old refresh token is immediately revoked (rotation).
    Rate limited to 30 per minute per IP.
    """
    access_token, new_refresh_token = await refresh_access_token(db, req.refresh_token)
    return TokenResponse(access_token=access_token, refresh_token=new_refresh_token)


@router.post("/logout", status_code=200, summary="Revoke refresh token")
async def logout(
    req: LogoutRequest,
    db: AsyncSession = Depends(get_db),
):
    """Revoke the provided refresh token. Silent success even if token unknown."""
    await revoke_refresh_token(db, req.refresh_token)
    return {"message": "Logged out successfully"}
