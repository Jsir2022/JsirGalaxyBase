#!/usr/bin/env bash

set -euo pipefail

APPIMAGE="${PRISM_APPIMAGE:-/media/u24/data/gtnh/client-tools/downloads/PrismLauncher-Linux-x86_64.AppImage}"
PRISM_DATA_DIR="${PRISM_DATA_DIR:-/media/u24/data/gtnh/client-tools/prism}"
INSTANCE_NAME="${PRISM_INSTANCE_NAME:-GT New Horizons 2.8.4}"
ACCOUNT_NAME="${PRISM_ACCOUNT:-Jsir2022}"
SERVER_ADDR="${PRISM_SERVER_ADDR:-127.0.0.1:25566}"

[[ -x "$APPIMAGE" ]] || {
    echo "Prism AppImage not found or not executable: $APPIMAGE" >&2
    exit 1
}

exec "$APPIMAGE" \
    -d "$PRISM_DATA_DIR" \
    -l "$INSTANCE_NAME" \
    -a "$ACCOUNT_NAME" \
    -s "$SERVER_ADDR" \
    --show-window
