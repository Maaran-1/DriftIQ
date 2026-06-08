import logging
import os
from functools import lru_cache
from pathlib import Path
from typing import List, Optional
from pydantic import field_validator, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

logger = logging.getLogger(__name__)


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # App
    APP_ENV: str = "development"
    APP_SECRET_KEY: str = "change-me-in-production-min-32-chars!!"
    BACKEND_CORS_ORIGINS: List[str] = ["http://localhost:3000", "http://localhost:8000"]

    # Database
    DATABASE_URL: str = "postgresql+asyncpg://driftiq:driftiq_secret@localhost:5432/driftiq"

    # Redis
    REDIS_URL: str = "redis://localhost:6379/0"
    CELERY_BROKER_URL: str = "redis://localhost:6379/0"
    CELERY_RESULT_BACKEND: str = "redis://localhost:6379/1"

    # JWT — RSA keys (RS256) or symmetric HS256 fallback.
    # Keys may be provided as file paths OR as inline PEM content via env vars.
    # Env-var inline content takes priority over file paths (avoids file permission issues).
    JWT_PRIVATE_KEY_PATH: str = "./keys/private.pem"
    JWT_PUBLIC_KEY_PATH: str = "./keys/public.pem"
    JWT_PRIVATE_KEY_CONTENT: str = ""   # Inline PEM — set in env to bypass file access
    JWT_PUBLIC_KEY_CONTENT: str = ""    # Inline PEM — set in env to bypass file access
    JWT_ALGORITHM: str = "RS256"
    JWT_ACCESS_TOKEN_EXPIRE_MINUTES: int = 60
    JWT_REFRESH_TOKEN_EXPIRE_DAYS: int = 30

    # LLM
    OPENAI_API_KEY: str = ""
    LLM_PROVIDER: str = "openai"
    LLM_MODEL: str = "gpt-4o"
    LLM_FALLBACK_ENABLED: bool = True
    LLM_TIMEOUT_SECONDS: int = 30

    # Firebase
    FIREBASE_CREDENTIALS_PATH: str = "./keys/firebase-service-account.json"
    FIREBASE_ENABLED: bool = False

    # AWS S3 (for data exports)
    AWS_ACCESS_KEY_ID: str = ""
    AWS_SECRET_ACCESS_KEY: str = ""
    AWS_S3_BUCKET: str = "driftiq-exports"
    AWS_REGION: str = "us-east-1"

    # Drift Thresholds (configurable per spec §12–13)
    DRIFT_ALERT_THRESHOLD: float = 40.0
    DRIFT_DIMENSION_SPIKE_Z: float = 2.5
    DRIFT_SUSTAINED_THRESHOLD: float = 25.0  # matches risk level 1 boundary

    # Nightly Compute Schedule (UTC)
    NIGHTLY_COMPUTE_HOUR: int = 2
    NIGHTLY_COMPUTE_MINUTE: int = 0

    # Data Retention
    DEFAULT_DATA_RETENTION_DAYS: int = 365

    # Logging
    LOG_LEVEL: str = "INFO"
    LOG_FORMAT: str = "json"  # json | text

    # Sentry (optional)
    SENTRY_DSN: str = ""

    @field_validator("BACKEND_CORS_ORIGINS", mode="before")
    @classmethod
    def parse_cors(cls, v: object) -> object:
        if isinstance(v, str):
            import json
            return json.loads(v)
        return v

    @model_validator(mode="after")
    def validate_production_secrets(self) -> "Settings":
        """Enforce strong secrets in production."""
        if self.APP_ENV == "production":
            if self.APP_SECRET_KEY == "change-me-in-production-min-32-chars!!":
                raise ValueError(
                    "APP_SECRET_KEY must be changed from the default in production"
                )
            if len(self.APP_SECRET_KEY) < 32:
                raise ValueError("APP_SECRET_KEY must be at least 32 characters in production")
        return self

    @property
    def is_production(self) -> bool:
        return self.APP_ENV == "production"

    @property
    def is_development(self) -> bool:
        return self.APP_ENV == "development"

    def _read_key_file(self, path: str) -> Optional[str]:
        """
        Safely read a key file. Returns None on any OS error.

        Catches OSError (which covers FileNotFoundError, PermissionError,
        and all other OS-level I/O failures). This is important because:
        - On Windows Docker Desktop, bind-mounted files may be owned by root
          with mode 600, making them unreadable by the non-root app user.
        - In CI environments, keys may legitimately be absent.
        """
        try:
            return Path(path).read_text()
        except OSError:
            return None

    @property
    def private_key(self) -> str:
        """
        Return the JWT private key.
        Priority: env-var inline content > file path > HS256 fallback (APP_SECRET_KEY).
        """
        # 1. Inline env var (highest priority — no filesystem dependency)
        if self.JWT_PRIVATE_KEY_CONTENT.strip():
            return self.JWT_PRIVATE_KEY_CONTENT

        # 2. File path
        content = self._read_key_file(self.JWT_PRIVATE_KEY_PATH)
        if content:
            return content

        # 3. HS256 fallback
        if self.is_production:
            logger.warning(
                "RSA private key unavailable (path=%s). "
                "Falling back to HS256. For production, set "
                "JWT_PRIVATE_KEY_CONTENT env var or fix key file permissions.",
                self.JWT_PRIVATE_KEY_PATH,
            )
        return self.APP_SECRET_KEY

    @property
    def public_key(self) -> str:
        """
        Return the JWT public key.
        Priority: env-var inline content > file path > HS256 fallback.
        """
        if self.JWT_PUBLIC_KEY_CONTENT.strip():
            return self.JWT_PUBLIC_KEY_CONTENT
        content = self._read_key_file(self.JWT_PUBLIC_KEY_PATH)
        if content:
            return content
        return self.APP_SECRET_KEY

    @property
    def effective_jwt_algorithm(self) -> str:
        """
        Returns the algorithm that will actually be used for JWT signing.

        RS256 if a readable private key is available (file or env var).
        HS256 otherwise (file absent, permission denied, or any other OSError).
        """
        if self.JWT_PRIVATE_KEY_CONTENT.strip():
            return self.JWT_ALGORITHM
        content = self._read_key_file(self.JWT_PRIVATE_KEY_PATH)
        if content:
            return self.JWT_ALGORITHM
        logger.warning(
            "JWT: RSA key unavailable at %s — using HS256 fallback.",
            self.JWT_PRIVATE_KEY_PATH,
        )
        return "HS256"


@lru_cache
def get_settings() -> Settings:
    return Settings()


# Module-level singleton — use `get_settings()` for dependency injection
settings = get_settings()
