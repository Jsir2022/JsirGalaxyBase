#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_ROOT="$(mktemp -d)"
trap 'rm -rf -- "$TEST_ROOT"' EXIT

export LOBBY_ROOT="$TEST_ROOT/lobby"
export S2_ROOT="$TEST_ROOT/s2"
mkdir -p "$LOBBY_ROOT/logs" "$S2_ROOT/logs"

# shellcheck source=lib/deploy-common.sh
source "$SCRIPT_DIR/lib/deploy-common.sh"

printf 'Done (old)!\n' >"$LOBBY_ROOT/logs/latest.log"
marker="$TEST_ROOT/restart-marker"
touch "$marker"

if ( wait_for_log_done lobby 1 0 "$marker" 2>/dev/null ); then
    printf 'stale Done marker was incorrectly accepted\n' >&2
    exit 1
fi

printf 'Done (new)!\n' >>"$LOBBY_ROOT/logs/latest.log"
wait_for_log_done lobby 1 0 "$marker"
printf 'deploy log readiness checks passed\n'
