#!/usr/bin/env bash

set -euo pipefail

DEPLOY_COMMON_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$DEPLOY_COMMON_DIR/../.." && pwd)"
GTNH_ROOT="${GTNH_ROOT:-/media/u24/data/gtnh}"
COMPOSE_FILE="${COMPOSE_FILE:-$GTNH_ROOT/docker/projects/docker-compose.yml}"
GRAY_CONTAINER="${GRAY_CONTAINER:-galaxy-gtnh}"
DB_CONTAINER="${DB_CONTAINER:-galaxy-base}"
DB_USER="${DB_USER:-jsirgalaxybase_app}"
DB_NAME="${DB_NAME:-jsirgalaxybase}"
GRADLE_SERVICE="${GRADLE_SERVICE:-galaxy-dev}"
GRADLE_USER_HOME_DIR="${GRADLE_USER_HOME_DIR:-$GTNH_ROOT/.codex-gradle-home-jgb}"
GRADLE_PROJECT_CACHE_DIR="${GRADLE_PROJECT_CACHE_DIR:-$GTNH_ROOT/.codex-gradle-project-cache-jgb}"
CLIENT_INSTANCE_DIR="${CLIENT_INSTANCE_DIR:-$GTNH_ROOT/client-tools/prism/instances/GT New Horizons 2.8.4/.minecraft}"
CLIENT_JAR_DIR="$CLIENT_INSTANCE_DIR/mods"
LOBBY_ROOT="${LOBBY_ROOT:-$GTNH_ROOT/data/Galaxy_GTNH_Lobby}"
S2_ROOT="${S2_ROOT:-$GTNH_ROOT/data/Galaxy_GTNH284_S2}"
LOBBY_JAR_DIR="$LOBBY_ROOT/mods"
S2_JAR_DIR="$S2_ROOT/mods"
DEFAULT_BACKUP_ROOT="${DEFAULT_BACKUP_ROOT:-$PROJECT_ROOT/run/deploy-backups}"
SUPERVISOR_SOCKET="${SUPERVISOR_SOCKET:-unix:///tmp/supervisor.sock}"
PRISM_APPIMAGE="${PRISM_APPIMAGE:-$GTNH_ROOT/client-tools/downloads/PrismLauncher-Linux-x86_64.AppImage}"
PRISM_DATA_DIR="${PRISM_DATA_DIR:-$GTNH_ROOT/client-tools/prism}"
PRISM_INSTANCE_NAME="${PRISM_INSTANCE_NAME:-GT New Horizons 2.8.4}"
PRISM_ACCOUNT="${PRISM_ACCOUNT:-Jsir2022}"
PRISM_SERVER_ADDR="${PRISM_SERVER_ADDR:-127.0.0.1:25566}"

log() {
    printf '[deploy] %s\n' "$*" >&2
}

fail() {
    printf '[deploy] ERROR: %s\n' "$*" >&2
    exit 1
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || fail "missing required command: $1"
}

timestamp_utc() {
    date -u +%Y%m%d-%H%M%S
}

ensure_project_root() {
    [[ -x "$PROJECT_ROOT/gradlew" ]] || fail "gradlew not found under $PROJECT_ROOT"
}

ensure_gray_container_running() {
    docker ps --format '{{.Names}}' | grep -qx "$GRAY_CONTAINER" \
        || fail "gray container not running: $GRAY_CONTAINER"
}

runtime_jar_candidates() {
    find "$PROJECT_ROOT/build/libs" -maxdepth 1 -type f -name '*.jar' \
        ! -name '*-sources.jar' \
        ! -name '*-dev.jar' \
        ! -name '*-dev-preshadow.jar'
}

find_latest_runtime_jar() {
    local jar
    jar="$(runtime_jar_candidates | xargs -r ls -t 2>/dev/null | head -n 1 || true)"
    [[ -n "$jar" ]] || fail "no runtime jar found under $PROJECT_ROOT/build/libs"
    printf '%s\n' "$jar"
}

jar_sha256() {
    sha256sum "$1" | awk '{print $1}'
}

jar_basename() {
    basename "$1"
}

target_mod_dir() {
    case "$1" in
        lobby) printf '%s\n' "$LOBBY_JAR_DIR" ;;
        s2) printf '%s\n' "$S2_JAR_DIR" ;;
        client) printf '%s\n' "$CLIENT_JAR_DIR" ;;
        *) fail "unknown deploy target: $1" ;;
    esac
}

target_root_dir() {
    case "$1" in
        lobby) printf '%s\n' "$LOBBY_ROOT" ;;
        s2) printf '%s\n' "$S2_ROOT" ;;
        client) printf '%s\n' "$CLIENT_INSTANCE_DIR" ;;
        *) fail "unknown target root: $1" ;;
    esac
}

target_log_path() {
    case "$1" in
        lobby) printf '%s\n' "$LOBBY_ROOT/logs/latest.log" ;;
        s2) printf '%s\n' "$S2_ROOT/logs/latest.log" ;;
        *) fail "log path is only available for server targets" ;;
    esac
}

target_alt_log_path() {
    case "$1" in
        lobby) printf '%s\n' "$LOBBY_ROOT/logs/fml-server-latest.log" ;;
        s2) printf '%s\n' "$S2_ROOT/logs/fml-server-latest.log" ;;
        *) fail "alt log path is only available for server targets" ;;
    esac
}

validate_targets() {
    local target
    for target in "$@"; do
        case "$target" in
            lobby|s2|client) ;;
            *) fail "unsupported target: $target" ;;
        esac
    done
}

split_targets_csv() {
    local csv="$1"
    local item
    IFS=',' read -r -a _split_targets <<<"$csv"
    for item in "${_split_targets[@]}"; do
        item="${item//[[:space:]]/}"
        [[ -n "$item" ]] && printf '%s\n' "$item"
    done
}

wait_for_supervisor_running() {
    local service="$1"
    local attempts="${2:-60}"
    local delay_seconds="${3:-2}"
    local output
    local i
    for ((i = 1; i <= attempts; i++)); do
        output="$(docker exec "$GRAY_CONTAINER" sh -lc "supervisorctl -s $SUPERVISOR_SOCKET status $service" 2>/dev/null || true)"
        if printf '%s\n' "$output" | grep -q "$service[[:space:]].*RUNNING"; then
            printf '%s\n' "$output"
            return 0
        fi
        sleep "$delay_seconds"
    done
    fail "service did not reach RUNNING: $service"
}

wait_for_log_done() {
    local target="$1"
    local attempts="${2:-90}"
    local delay_seconds="${3:-2}"
    local log_path alt_log_path
    local i
    log_path="$(target_log_path "$target")"
    alt_log_path="$(target_alt_log_path "$target")"
    for ((i = 1; i <= attempts; i++)); do
        if { [[ -f "$log_path" ]] && grep -q 'Done (' "$log_path"; } \
            || { [[ -f "$alt_log_path" ]] && grep -q 'Done (' "$alt_log_path"; }; then
            return 0
        fi
        sleep "$delay_seconds"
    done
    fail "server log did not reach Done state for target: $target"
}

restart_gray_services() {
    local services=("$@")
    [[ ${#services[@]} -gt 0 ]] || return 0
    docker exec "$GRAY_CONTAINER" sh -lc \
        "supervisorctl -s $SUPERVISOR_SOCKET restart ${services[*]}" >/dev/null
}

stop_gray_services() {
    local services=("$@")
    [[ ${#services[@]} -gt 0 ]] || return 0
    docker exec "$GRAY_CONTAINER" sh -lc \
        "supervisorctl -s $SUPERVISOR_SOCKET stop ${services[*]}" >/dev/null
}

wait_for_supervisor_stopped() {
    local service="$1"
    local attempts="${2:-30}"
    local delay_seconds="${3:-1}"
    local output i
    for ((i = 1; i <= attempts; i++)); do
        output="$(docker exec "$GRAY_CONTAINER" sh -lc "supervisorctl -s $SUPERVISOR_SOCKET status $service" 2>/dev/null || true)"
        if printf '%s\n' "$output" | grep -Eq "$service[[:space:]].*(STOPPED|EXITED|FATAL)"; then
            return 0
        fi
        sleep "$delay_seconds"
    done
    fail "service did not stop: $service"
}

apply_database_migrations() {
    docker ps --format '{{.Names}}' | grep -qx "$DB_CONTAINER" \
        || fail "database container is not running: $DB_CONTAINER"
    docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 <<EOF
CREATE TABLE IF NOT EXISTS schema_migration_history (
    migration_id VARCHAR(160) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
EOF
    local migration file checksum already
    while IFS= read -r file; do
        migration="$(basename "$file")"
        checksum="$(sha256sum "$file" | awk '{print $1}')"
        already="$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -X -At -v ON_ERROR_STOP=1 \
            -c "SELECT checksum_sha256 FROM schema_migration_history WHERE migration_id = '$migration'" || true)"
        if [[ -n "$already" ]]; then
            [[ "$already" == "$checksum" ]] || fail "migration checksum mismatch: $migration"
            continue
        fi
        log "Applying database migration: $migration"
        {
            printf 'BEGIN;\nSELECT pg_advisory_xact_lock(2026040301);\n'
            sed -e '/^[[:space:]]*BEGIN;[[:space:]]*$/Id' -e '/^[[:space:]]*COMMIT;[[:space:]]*$/Id' "$file"
            printf "INSERT INTO schema_migration_history (migration_id, description, checksum_sha256) VALUES ('%s', '%s', '%s');\n" \
                "$migration" "$migration" "$checksum"
            printf 'COMMIT;\n'
        } | docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1
    done < <(find "$PROJECT_ROOT/ops/sql/migrations" -maxdepth 1 -type f -name '*.sql' | sort)
}

print_hash_report() {
    local source_jar="$1"
    shift
    sha256sum "$source_jar" "$@"
}
