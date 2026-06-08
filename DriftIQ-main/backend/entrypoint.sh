#!/bin/sh
# DriftIQ API entrypoint script.
# Runs as root, copies key files to a writable /run location with correct
# permissions, then drops to the driftiq user via gosu before exec'ing uvicorn.
#
# Problem solved:
#   Docker Desktop on Windows (WSL2 backend) bind-mounts Windows NTFS files
#   into the Linux container as root:root with mode 600, making them unreadable
#   by the non-root driftiq user (UID 1001). This entrypoint copies the files
#   as root and re-owns them before dropping privileges.

set -e

KEYS_SRC="/app/keys"
KEYS_DEST="/run/driftiq/keys"

mkdir -p "$KEYS_DEST"
chmod 700 "$KEYS_DEST"

if [ -f "$KEYS_SRC/private.pem" ]; then
    cp "$KEYS_SRC/private.pem" "$KEYS_DEST/private.pem"
    chmod 600 "$KEYS_DEST/private.pem"
    chown driftiq:driftiq "$KEYS_DEST/private.pem"
    echo "[entrypoint] RSA private key copied — RS256 will be used"
else
    echo "[entrypoint] No RSA private key at $KEYS_SRC/private.pem — HS256 fallback active"
fi

if [ -f "$KEYS_SRC/public.pem" ]; then
    cp "$KEYS_SRC/public.pem" "$KEYS_DEST/public.pem"
    chmod 644 "$KEYS_DEST/public.pem"
    chown driftiq:driftiq "$KEYS_DEST/public.pem"
fi

export JWT_PRIVATE_KEY_PATH="$KEYS_DEST/private.pem"
export JWT_PUBLIC_KEY_PATH="$KEYS_DEST/public.pem"

exec gosu driftiq "$@"
