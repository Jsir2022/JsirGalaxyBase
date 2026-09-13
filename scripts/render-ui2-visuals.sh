#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
GTNH_ROOT="$(cd "$PROJECT_ROOT/.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-$GTNH_ROOT/docker/projects/docker-compose.yml}"
GRADLE_USER_HOME_DIR="${GRADLE_USER_HOME_DIR:-$GTNH_ROOT/.codex-gradle-home-jgb}"
GRADLE_PROJECT_CACHE_DIR="${GRADLE_PROJECT_CACHE_DIR:-$GTNH_ROOT/.codex-gradle-project-cache-jgb}"
PREVIEW_FONT="${UI2_PREVIEW_FONT:-/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc}"

if [[ ! -f "$PREVIEW_FONT" ]]; then
    printf '[ui2-visual] ERROR: CJK preview font not found: %s\n' "$PREVIEW_FONT" >&2
    printf '[ui2-visual] Set UI2_PREVIEW_FONT to an external TTF file. It is mounted for tests and never packaged.\n' >&2
    exit 1
fi

mkdir -p "$GRADLE_USER_HOME_DIR" "$GRADLE_PROJECT_CACHE_DIR"
docker compose -f "$COMPOSE_FILE" run --rm \
    -v "$GRADLE_USER_HOME_DIR:/codex-gradle-home" \
    -v "$GRADLE_PROJECT_CACHE_DIR:/codex-project-cache" \
    -v "$PREVIEW_FONT:/ui2-preview-font.ttf:ro" \
    -e GRADLE_USER_HOME=/codex-gradle-home \
    -e UI2_PREVIEW_FONT=/ui2-preview-font.ttf \
    galaxy-dev ./gradlew :ui2-core:test :ui2-terminal:test :ui2-lab:test :ui2-demo:test :ui2-demo:verifyTerminalVisuals \
    --project-cache-dir /codex-project-cache --no-daemon --no-configuration-cache -PforceToolchainVersion=17

printf '[ui2-visual] Report: %s\n' "$PROJECT_ROOT/ui2-demo/build/ui-lab/index.html"
