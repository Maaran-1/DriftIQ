"""Auth service — user creation, login validation, token management."""
import logging
import uuid
from datetime import datetime, timezone

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ConflictError, UnauthorizedError
from app.core.security import (
    create_access_token, create_refresh_token, hash_password,
    hash_refresh_token, refresh_token_expires_at, verify_password,
)
from app.models.ai_insight import RefreshToken
from app.models.user import User

logger = logging.getLogger(__name__)


async def register_user(db: AsyncSession, email: str, password: str) -> tuple[User, str, str]:
    """
    Create a new user account.
    Returns (user, access_token, raw_refresh_token).
    Raises ConflictError if email already registered.
    """
    email = email.lower().strip()

    existing = await db.execute(select(User).where(User.email == email))
    if existing.scalar_one_or_none():
        raise ConflictError("Email already registered")

    user = User(
        email=email,
        password_hash=hash_password(password),
        consent_given_at=datetime.now(timezone.utc),
    )
    db.add(user)
    await db.flush()  # get user.id before creating refresh token

    access_token = create_access_token(str(user.id), email)
    raw_refresh, hashed_refresh = create_refresh_token()

    rt = RefreshToken(
        user_id=user.id,
        token_hash=hashed_refresh,
        expires_at=refresh_token_expires_at(),
    )
    db.add(rt)
    await db.flush()

    logger.info("User registered: id=%s email=%s", user.id, email)
    return user, access_token, raw_refresh


async def login_user(db: AsyncSession, email: str, password: str) -> tuple[User, str, str]:
    """
    Validate credentials and issue token pair.
    Returns (user, access_token, raw_refresh_token).
    Raises UnauthorizedError on bad credentials.
    """
    email = email.lower().strip()

    result = await db.execute(
        select(User).where(User.email == email, User.is_active == True)
    )
    user = result.scalar_one_or_none()

    # Use constant-time check even if user not found to prevent timing attacks
    if user is None:
        verify_password(password, "$2b$12$placeholder_hash_for_timing_safety_!!")
        raise UnauthorizedError("Invalid email or password")

    if not verify_password(password, user.password_hash):
        logger.warning("Failed login attempt for email=%s", email)
        raise UnauthorizedError("Invalid email or password")

    access_token = create_access_token(str(user.id), user.email)
    raw_refresh, hashed_refresh = create_refresh_token()

    rt = RefreshToken(
        user_id=user.id,
        token_hash=hashed_refresh,
        expires_at=refresh_token_expires_at(),
    )
    db.add(rt)
    await db.flush()

    return user, access_token, raw_refresh


async def refresh_access_token(
    db: AsyncSession, raw_refresh_token: str
) -> tuple[str, str]:
    """
    Validate refresh token, rotate it, and return new token pair.

    Returns (new_access_token, new_raw_refresh_token).
    Raises UnauthorizedError if token is invalid, expired, or revoked.

    IMPORTANT: Returns both tokens so the client can update its stored refresh token.
    This is full token rotation — the old refresh token is immediately invalidated.
    """
    token_hash = hash_refresh_token(raw_refresh_token)
    result = await db.execute(
        select(RefreshToken).where(
            RefreshToken.token_hash == token_hash,
            RefreshToken.revoked == False,
            RefreshToken.expires_at > datetime.now(timezone.utc),
        )
    )
    rt = result.scalar_one_or_none()
    if not rt:
        logger.warning("Invalid or expired refresh token presented")
        raise UnauthorizedError("Invalid or expired refresh token")

    # Revoke the old token immediately (rotation)
    rt.revoked = True

    # Load and validate user
    user_result = await db.execute(
        select(User).where(User.id == rt.user_id, User.is_active == True)
    )
    user = user_result.scalar_one_or_none()
    if not user:
        raise UnauthorizedError("Account not found or deactivated")

    # Issue new token pair
    new_access_token = create_access_token(str(user.id), user.email)
    new_raw_refresh, new_hashed_refresh = create_refresh_token()

    new_rt = RefreshToken(
        user_id=user.id,
        token_hash=new_hashed_refresh,
        expires_at=refresh_token_expires_at(),
    )
    db.add(new_rt)
    await db.flush()

    return new_access_token, new_raw_refresh


async def revoke_refresh_token(db: AsyncSession, raw_refresh_token: str) -> None:
    """Revoke a refresh token. Silent no-op if not found."""
    token_hash = hash_refresh_token(raw_refresh_token)
    result = await db.execute(
        select(RefreshToken).where(RefreshToken.token_hash == token_hash)
    )
    rt = result.scalar_one_or_none()
    if rt:
        rt.revoked = True
        await db.flush()


async def get_current_user(db: AsyncSession, user_id: str) -> User:
    """Load active user by UUID string. Raises UnauthorizedError if not found."""
    try:
        uid = uuid.UUID(user_id)
    except ValueError:
        raise UnauthorizedError("Invalid user identifier")

    result = await db.execute(
        select(User).where(User.id == uid, User.is_active == True)
    )
    user = result.scalar_one_or_none()
    if not user:
        raise UnauthorizedError("User not found or deactivated")
    return user
