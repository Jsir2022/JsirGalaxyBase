#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

SERVER_PORT="${SERVER_PORT:-25100}"
SERVER_HOST="${SERVER_HOST:-127.0.0.1}"
CLIENT_A_USERNAME="${CLIENT_A_USERNAME:-DevA}"
CLIENT_B_USERNAME="${CLIENT_B_USERNAME:-DevB}"
CLIENT_A_DIR_REL="${CLIENT_A_DIR_REL:-run/client}"
CLIENT_B_DIR_REL="${CLIENT_B_DIR_REL:-run/client_traderb}"
SERVER_DIR_REL="${SERVER_DIR_REL:-run/server}"
LOG_ROOT="${LOG_ROOT:-$PROJECT_ROOT/run/local-test-stack-logs}"
DEFAULT_JAVA_HOME="/usr/lib/jvm/java-25-openjdk-amd64"
DRY_RUN=0

declare -a STARTED_SESSION_PIDS=()

usage() {
    cat <<EOF
Usage: $(basename "$0") [--dry-run] [--help]

Starts the local JsirGalaxyBase manual-test stack:
  - client A in ${CLIENT_A_DIR_REL}
  - client B in ${CLIENT_B_DIR_REL}
    - dedicated server on ${SERVER_HOST}:${SERVER_PORT}

Behavior:
  1. Kills existing local JsirGalaxyBase client/server processes first
    2. Starts two clients with isolated game directories first
    3. Starts dedicated server last and keeps the current terminal attached to the server console
    4. When the server process exits, stops the clients started by this script

Useful overrides:
  CLIENT_A_USERNAME=TraderA CLIENT_B_USERNAME=TraderB $(basename "$0")
  JAVA_HOME=/path/to/jdk $(basename "$0")

Press Ctrl-C in this script terminal to stop the foreground server and both clients started by it.
EOF
}

log() {
    printf '[stack] %s\n' "$*"
}

fail() {
    printf '[stack] ERROR: %s\n' "$*" >&2
    exit 1
}

ensure_requirements() {
    [[ -x "$PROJECT_ROOT/gradlew" ]] || fail "gradlew not found under $PROJECT_ROOT"
    [[ -d "$PROJECT_ROOT/$CLIENT_A_DIR_REL" ]] || fail "missing client A directory: $PROJECT_ROOT/$CLIENT_A_DIR_REL"
    [[ -d "$PROJECT_ROOT/$CLIENT_B_DIR_REL" ]] || fail "missing client B directory: $PROJECT_ROOT/$CLIENT_B_DIR_REL"
    [[ -d "$PROJECT_ROOT/$SERVER_DIR_REL" ]] || fail "missing server directory: $PROJECT_ROOT/$SERVER_DIR_REL"

    if [[ -z "${JAVA_HOME:-}" && -d "$DEFAULT_JAVA_HOME" ]]; then
        export JAVA_HOME="$DEFAULT_JAVA_HOME"
    fi

    [[ -n "${JAVA_HOME:-}" ]] || fail "JAVA_HOME is not set, and default $DEFAULT_JAVA_HOME does not exist"
    if (( ! DRY_RUN )); then
        [[ -n "${DISPLAY:-}" ]] || fail "DISPLAY is not set; client windows cannot be opened from this shell"
    fi

    mkdir -p "$LOG_ROOT"
}

collect_existing_pids() {
    ps -eo pid=,args= | awk \
        -v root="$PROJECT_ROOT" \
        -v client_a="$PROJECT_ROOT/$CLIENT_A_DIR_REL" \
        -v client_b="$PROJECT_ROOT/$CLIENT_B_DIR_REL" \
        'index($0, root) && (index($0, "GradleStartServer") || index($0, client_a) || index($0, client_b) || index($0, " runServer") || index($0, " runClient")) { print $1 }' | sort -u
}

kill_existing_stack() {
    mapfile -t existing_pids < <(collect_existing_pids)

    if (( ${#existing_pids[@]} == 0 )); then
        log "No existing local JsirGalaxyBase client/server processes found."
        return
    fi

    if (( DRY_RUN )); then
        log "Dry run would stop existing local stack PIDs: ${existing_pids[*]}"
        return
    fi

    log "Stopping existing local stack PIDs: ${existing_pids[*]}"
    kill "${existing_pids[@]}" 2>/dev/null || true
    sleep 3

    mapfile -t stubborn_pids < <(collect_existing_pids)
    if (( ${#stubborn_pids[@]} > 0 )); then
        log "Force killing stubborn PIDs: ${stubborn_pids[*]}"
        kill -KILL "${stubborn_pids[@]}" 2>/dev/null || true
        sleep 1
    fi
}

start_session() {
    local name="$1"
    local log_file="$2"
    shift 2

    log "Starting $name"
    if (( DRY_RUN )); then
        printf '[stack] DRY RUN:'
        printf ' %q' "$@"
        printf ' > %s\n' "$log_file"
        return 0
    fi

    setsid "$@" >"$log_file" 2>&1 &
    local session_pid=$!
    STARTED_SESSION_PIDS+=("$session_pid")
    log "$name session pid=$session_pid log=$log_file"
}

run_server_foreground() {
    local log_file="$1"
    shift

    log "Starting dedicated server in foreground console"
    log "Server address: ${SERVER_HOST}:${SERVER_PORT}"
    log "Client A: ${CLIENT_A_USERNAME} (${PROJECT_ROOT}/${CLIENT_A_DIR_REL})"
    log "Client B: ${CLIENT_B_USERNAME} (${PROJECT_ROOT}/${CLIENT_B_DIR_REL})"
    log "Gradle/Minecraft logs remain under ${PROJECT_ROOT}/${SERVER_DIR_REL}/logs"
    log "This terminal is now attached directly to runServer stdin/stdout; you can type server commands here."

    if (( DRY_RUN )); then
        printf '[stack] DRY RUN:'
        printf ' %q' "$@"
        printf '\n'
        return 0
    fi

    "$@"
}

cleanup_started_sessions() {
    local exit_code=$?
    trap - EXIT INT TERM

    if (( ${#STARTED_SESSION_PIDS[@]} > 0 )); then
        log "Stopping started sessions: ${STARTED_SESSION_PIDS[*]}"
        for pid in "${STARTED_SESSION_PIDS[@]}"; do
            kill -TERM -- "-$pid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null || true
        done
        sleep 3
        for pid in "${STARTED_SESSION_PIDS[@]}"; do
            kill -KILL -- "-$pid" 2>/dev/null || kill -KILL "$pid" 2>/dev/null || true
        done
    fi

    exit "$exit_code"
}

main() {
    while (( $# > 0 )); do
        case "$1" in
            --dry-run)
                DRY_RUN=1
                ;;
            --help|-h)
                usage
                exit 0
                ;;
            *)
                fail "unknown argument: $1"
                ;;
        esac
        shift
    done

    ensure_requirements

    local timestamp
    timestamp="$(date +%Y%m%d-%H%M%S)"
    local server_log="$LOG_ROOT/${timestamp}-server.log"
    local client_a_log="$LOG_ROOT/${timestamp}-client-a.log"
    local client_b_log="$LOG_ROOT/${timestamp}-client-b.log"

    trap cleanup_started_sessions EXIT INT TERM

    kill_existing_stack

    start_session \
        "client A" \
        "$client_a_log" \
        env JAVA_HOME="$JAVA_HOME" DISPLAY="${DISPLAY:-}" \
        "$PROJECT_ROOT/gradlew" -p "$PROJECT_ROOT" --no-daemon \
        -PrunClientWorkingDirectory="$CLIENT_A_DIR_REL" \
        runClient "--username=$CLIENT_A_USERNAME"

    sleep 3

    start_session \
        "client B" \
        "$client_b_log" \
        env JAVA_HOME="$JAVA_HOME" DISPLAY="${DISPLAY:-}" \
        "$PROJECT_ROOT/gradlew" -p "$PROJECT_ROOT" --no-daemon \
        -PrunClientWorkingDirectory="$CLIENT_B_DIR_REL" \
        runClient "--username=$CLIENT_B_USERNAME"

    if (( DRY_RUN )); then
        run_server_foreground \
            "$server_log" \
            env JAVA_HOME="$JAVA_HOME" \
            "$PROJECT_ROOT/gradlew" -p "$PROJECT_ROOT" --no-daemon \
            -PrunServerWorkingDirectory="$SERVER_DIR_REL" \
            runServer
        log "Dry run finished. No process was started."
        return 0
    fi

    run_server_foreground \
        "$server_log" \
        env JAVA_HOME="$JAVA_HOME" \
        "$PROJECT_ROOT/gradlew" -p "$PROJECT_ROOT" --no-daemon \
        -PrunServerWorkingDirectory="$SERVER_DIR_REL" \
        runServer
}

main "$@"