"""Centralized exception hierarchy for DriftIQ."""
from typing import Any


class DriftIQException(Exception):
    """Base exception for all DriftIQ-specific errors."""
    status_code: int = 500
    detail: str = "An unexpected error occurred"

    def __init__(self, detail: str | None = None, **kwargs: Any) -> None:
        self.detail = detail or self.__class__.detail
        super().__init__(self.detail)


class BadRequestError(DriftIQException):
    """HTTP 400 — bad input."""
    status_code = 400
    detail = "Bad request"


class UnauthorizedError(DriftIQException):
    """HTTP 401 — authentication required or failed."""
    status_code = 401
    detail = "Unauthorized"


class ForbiddenError(DriftIQException):
    """HTTP 403 — authenticated but not authorized."""
    status_code = 403
    detail = "Forbidden"


class NotFoundError(DriftIQException):
    """HTTP 404 — resource not found."""
    status_code = 404
    detail = "Not found"


class ConflictError(DriftIQException):
    """HTTP 409 — resource already exists."""
    status_code = 409
    detail = "Conflict"


class UnprocessableError(DriftIQException):
    """HTTP 422 — validation failed."""
    status_code = 422
    detail = "Unprocessable entity"


class RateLimitError(DriftIQException):
    """HTTP 429 — too many requests."""
    status_code = 429
    detail = "Too many requests"


class InternalError(DriftIQException):
    """HTTP 500 — unexpected server error."""
    status_code = 500
    detail = "Internal server error"


class ServiceUnavailableError(DriftIQException):
    """HTTP 503 — downstream service unavailable."""
    status_code = 503
    detail = "Service temporarily unavailable"
