"""
Security utilities — JWT, bcrypt, refresh tokens.
Algorithm detection is cached to avoid repeated filesystem hits.
"""
import hashlib
import logging
import secrets
from datetime import datetime, timedelta, timezone
from functools import lru_cache
from typing import Optional

from jose import jwt, JWTError
from passlib.context import CryptContext
from app.core.config import settings

logger = logging.getLogger(__name__)

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


@lru_cache(maxsize=1)
def get_jwt_algorithm() -> str:
    """
    Detect and cache the JWT algorithm to use.
    Priority: inline env-var key > file path > HS256 fallback.

    Catches OSError (superset of FileNotFoundError and PermissionError).
    On Windows Docker Desktop, bind-mounted files may have root:600 permissions
    that the non-root app user cannot read — PermissionError must be handled.
    """
    # Inline env-var key takes priority (no filesystem access needed)
    if settings.JWT_PRIVATE_KEY_CONTENT.strip():
        logger.info("JWT: Using RS256 (inline key from JWT_PRIVATE_KEY_CONTENT)")
        return settings.JWT_ALGORITHM

    # Try reading the key file
    try:
        with open(settings.JWT_PRIVATE_KEY_PATH):
            pass
        logger.info("JWT: Using RS256 (RSA keypair file found at %s)", settings.JWT_PRIVATE_KEY_PATH)
        return settings.JWT_ALGORITHM  # RS256
    except OSError as exc:
        logger.warning(
            "JWT: RSA private key unavailable at %s (%s). "
            "Falling back to HS256. Set JWT_PRIVATE_KEY_CONTENT env var or "
            "fix key file permissions for production RS256.",
            settings.JWT_PRIVATE_KEY_PATH,
            exc,
        )
        return "HS256"


def hash_password(password: str) -> str:
    return pwd_context.hash(password)


def verify_password(plain: str, hashed: str) -> bool:
    return pwd_context.verify(plain, hashed)


def create_access_token(user_id: str, email: str) -> str:
    algo = get_jwt_algorithm()
    key = settings.private_key
    now = datetime.now(timezone.utc)
    payload = {
        "sub": user_id,
        "email": email,
        "type": "access",
        "iat": now,
        "exp": now + timedelta(minutes=settings.JWT_ACCESS_TOKEN_EXPIRE_MINUTES),
        "jti": secrets.token_hex(16),  # unique token ID for future revocation
    }
    return jwt.encode(payload, key, algorithm=algo)


def create_refresh_token() -> tuple[str, str]:
    """
    Generate a cryptographically secure refresh token.
    Returns (raw_token, hashed_token).
    raw_token: sent to client (never stored server-side)
    hashed_token: stored in DB
    """
    raw = secrets.token_urlsafe(64)
    hashed = hashlib.sha256(raw.encode()).hexdigest()
    return raw, hashed


def decode_access_token(token: str) -> Optional[dict]:
    """
    Decode and validate an access token.
    Returns the payload dict or None if invalid/expired.
    """
    algo = get_jwt_algorithm()
    key = settings.public_key
    try:
        payload = jwt.decode(
            token,
            key,
            algorithms=[algo],
            options={"verify_exp": True},
        )
        if payload.get("type") != "access":
            logger.warning("JWT: Token type mismatch — expected 'access'")
            return None
        return payload
    except JWTError as e:
        logger.debug("JWT decode failed: %s", e)
        return None


def hash_refresh_token(raw_token: str) -> str:
    """Hash a raw refresh token for storage/lookup."""
    return hashlib.sha256(raw_token.encode()).hexdigest()


def refresh_token_expires_at() -> datetime:
    return datetime.now(timezone.utc) + timedelta(days=settings.JWT_REFRESH_TOKEN_EXPIRE_DAYS)
