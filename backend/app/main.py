import logging
import time
from contextlib import asynccontextmanager
from typing import Callable

from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded

from app.core.config import settings
from app.core.exceptions import DriftIQException
from app.api import auth, events, twin, drift, risk, insights, dashboard
from app.api import settings as settings_router
from app.api import admin as admin_router

logger = logging.getLogger(__name__)
limiter = Limiter(key_func=get_remote_address)

# Configure structured logging at startup
logging.basicConfig(
    level=getattr(logging, settings.LOG_LEVEL.upper(), logging.INFO),
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager — startup and shutdown hooks."""
    # ── Startup ─────────────────────────────────────────────────
    logger.info(
        "DriftIQ API starting up (env=%s, jwt=%s)",
        settings.APP_ENV,
        settings.effective_jwt_algorithm,
    )

    # Verify database connectivity
    try:
        from app.core.database import engine
        async with engine.begin() as conn:
            await conn.execute(__import__("sqlalchemy").text("SELECT 1"))
        logger.info("Database connection: OK")
    except Exception as exc:
        logger.critical("Database connection failed on startup: %s", exc)
        # Don't crash — let the healthcheck surface the issue

    # Verify Redis connectivity
    try:
        import redis.asyncio as aioredis
        r = aioredis.from_url(settings.REDIS_URL, socket_connect_timeout=3)
        await r.ping()
        await r.aclose()
        logger.info("Redis connection: OK")
    except Exception as exc:
        logger.warning("Redis connection unavailable on startup: %s", exc)

    # Optional: Sentry initialization
    if settings.SENTRY_DSN:
        try:
            import sentry_sdk
            sentry_sdk.init(
                dsn=settings.SENTRY_DSN,
                environment=settings.APP_ENV,
                traces_sample_rate=0.1,
            )
            logger.info("Sentry initialized")
        except ImportError:
            logger.warning("sentry-sdk not installed — Sentry disabled")

    yield

    # ── Shutdown ─────────────────────────────────────────────────
    logger.info("DriftIQ API shutting down")
    try:
        from app.core.database import engine
        await engine.dispose()
        logger.info("Database connections closed")
    except Exception as exc:
        logger.error("Error during database shutdown: %s", exc)


def create_app() -> FastAPI:
    app = FastAPI(
        title="DriftIQ API",
        description=(
            "Behavioral Intelligence Platform — "
            "Behavioral Digital Twin, Drift Detection & Risk Engine"
        ),
        version="1.0.0",
        docs_url="/docs" if not settings.is_production else None,
        redoc_url="/redoc" if not settings.is_production else None,
        openapi_url="/openapi.json" if not settings.is_production else None,
        lifespan=lifespan,
    )

    # ── Rate limiting ────────────────────────────────────────────
    app.state.limiter = limiter
    app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

    # ── CORS ─────────────────────────────────────────────────────
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.BACKEND_CORS_ORIGINS,
        allow_credentials=True,
        allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
        allow_headers=["Authorization", "Content-Type", "X-Request-ID"],
        expose_headers=["X-Request-ID"],
    )

    # ── Request logging middleware ────────────────────────────────
    @app.middleware("http")
    async def request_logging_middleware(request: Request, call_next: Callable):
        start = time.perf_counter()
        request_id = request.headers.get("X-Request-ID", "")
        response = await call_next(request)
        duration_ms = (time.perf_counter() - start) * 1000
        logger.info(
            "HTTP %s %s → %d (%.1fms) rid=%s",
            request.method,
            request.url.path,
            response.status_code,
            duration_ms,
            request_id,
        )
        response.headers["X-Request-ID"] = request_id
        return response

    # ── Custom exception handlers ────────────────────────────────
    @app.exception_handler(DriftIQException)
    async def driftiq_exception_handler(request: Request, exc: DriftIQException):
        return JSONResponse(
            status_code=exc.status_code,
            content={"detail": exc.detail, "type": type(exc).__name__},
        )

    @app.exception_handler(Exception)
    async def unhandled_exception_handler(request: Request, exc: Exception):
        logger.error("Unhandled exception: %s %s — %s", request.method, request.url.path, exc, exc_info=True)
        return JSONResponse(
            status_code=500,
            content={"detail": "An unexpected error occurred", "type": "InternalError"},
        )

    # ── Health & metrics endpoints ────────────────────────────────
    @app.get("/health", tags=["ops"])
    async def health(request: Request):
        """Lightweight health check — used by Docker HEALTHCHECK and load balancers."""
        db_ok = False
        try:
            from app.core.database import engine
            import sqlalchemy
            async with engine.begin() as conn:
                await conn.execute(sqlalchemy.text("SELECT 1"))
            db_ok = True
        except Exception:
            pass
        status_code = 200 if db_ok else 503
        return JSONResponse(
            status_code=status_code,
            content={
                "status": "ok" if db_ok else "degraded",
                "service": "driftiq-api",
                "version": "1.0.0",
                "db": "ok" if db_ok else "error",
                "env": settings.APP_ENV,
            },
        )

    @app.get("/health/live", tags=["ops"])
    async def liveness():
        """Kubernetes liveness probe — always returns 200 if process is alive."""
        return {"status": "alive"}

    # ── API v1 routers ────────────────────────────────────────────
    prefix = "/api/v1"
    app.include_router(auth.router, prefix=prefix)
    app.include_router(events.router, prefix=prefix)
    app.include_router(twin.router, prefix=prefix)
    app.include_router(drift.router, prefix=prefix)
    app.include_router(risk.router, prefix=prefix)
    app.include_router(insights.router, prefix=prefix)
    app.include_router(dashboard.router, prefix=prefix)
    app.include_router(settings_router.router, prefix=prefix)

    # Admin endpoints — dev/staging only, never in production
    if not settings.is_production:
        app.include_router(admin_router.router, prefix=prefix)

    return app


app = create_app()
