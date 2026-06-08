# DriftIQ FastAPI Backend
# Multi-stage build — no RSA keys baked into image layers.
# Keys are mounted at runtime via the 'keys' volume (see docker-compose.yml).
# The entrypoint copies keys to a writable location and chmods them for the
# non-root driftiq user, solving Docker Desktop/Windows bind-mount permission issues.
FROM python:3.11-slim AS base

WORKDIR /app

# System deps:
#   - gcc, libpq-dev: required for asyncpg C extension compilation
#   - curl: used by the HEALTHCHECK
#   - gosu: privilege dropping in the entrypoint on Debian-based images
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc libpq-dev curl gosu \
    && rm -rf /var/lib/apt/lists/*

# Install Python deps in a separate layer for better cache utilization
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r requirements.txt

# Copy the backend build context.
COPY . .

# Create app user and keys directory.
# The entrypoint.sh runs as root, copies key files with correct permissions,
# then drops to driftiq user via gosu before starting uvicorn.
RUN useradd -m -u 1001 driftiq \
    && mkdir -p /app/keys /run/driftiq/keys \
    && chown -R driftiq:driftiq /app /run/driftiq/keys

# Copy and make the entrypoint executable.
# This file is inside the ./backend build context used by docker-compose.yml.
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=5 \
    CMD curl -f http://localhost:8000/health/live || exit 1

# Entrypoint runs as root to handle key permissions, then drops to driftiq
ENTRYPOINT ["/entrypoint.sh"]

# 2 workers is appropriate for a 2-core container
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", \
     "--workers", "2", "--access-log", "--log-level", "info"]
