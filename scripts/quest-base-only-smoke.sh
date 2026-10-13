#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
GTNH_ROOT="$(cd "$PROJECT_ROOT/.." && pwd)"
DEFAULT_SOURCE="$GTNH_ROOT/data/Galaxy_GTNH284_S2/config/betterquesting/DefaultQuests"
SOURCE_DIR="${BQ_TEST_DEFAULT_QUESTS:-$DEFAULT_SOURCE}"
GRADLE_USER_HOME_DIR="${GRADLE_USER_HOME_DIR:-$GTNH_ROOT/.codex-gradle-home-jgb}"
DEV_IMAGE="galaxy-dev:local"
POSTGRES_IMAGE="postgres:16-alpine"
PREVIEW_FONT="${UI2_PREVIEW_FONT:-/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc}"
QZ_LICENSE_SHA256="f7b20465c7ba7e58284504c6eb2acc16287a0540a06c3a1b5d31df10daa29580"
QZ_LICENSE_SOURCE="${UI2_QZ_LICENSE_FILE:-$PROJECT_ROOT/src/main/resources/META-INF/licenses/Qz-UILib-LGPL-3.0-or-later.txt}"
RUN_ID="$(date -u +%Y%m%d%H%M%S)-$$"
NETWORK_NAME="jgb-bq-smoke-net-$RUN_ID"
DB_CONTAINER="jgb-bq-smoke-db-$RUN_ID"
DEV_CONTAINER="jgb-bq-smoke-gradle-$RUN_ID"
RUN_ROOT="$PROJECT_ROOT/.gradle/codex-quest-base-only-smoke-$RUN_ID"
CONTAINER_PROJECT_ROOT="/workspace/JsirGalaxyBase"
CONTAINER_GRADLE_HOME="/codex-gradle-home"
CONTAINER_SOURCE="/readonly/DefaultQuests"
PG_DATABASE="quest_n92_smoke_it"
IMPORT_DATABASE="quest_n92_import_it"
PG_USER="quest_smoke"
PG_PASSWORD=""
DEFAULT_QUESTS_SHA256="9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc"
QUEST_SCHEMA="$PROJECT_ROOT/galaxy-quest-postgres/src/main/resources/com/jsirgalaxybase/quest/postgres/quest-postgresql-ddl.sql"
QUEST_MIGRATIONS=(
    "20260914_001_add_cross_server_quest_platform.sql"
    "20260917_001_add_quest_content_migration_audit.sql"
    "20260929_001_add_quest_reward_suppressed_status.sql"
    "20260930_001_add_bq_party_inventory_snapshot_leases.sql"
    "20260930_002_add_quest_lifetime_completion_latch.sql"
    "20261009_001_add_quest_definition_catalog_revision.sql"
    "20261010_001_add_quest_task_source_progress.sql"
    "20261010_002_add_quest_consumption_detect_batch.sql"
    "20261011_001_pin_consumption_detect_batch_across_versions.sql"
)
NETWORK_CREATED=0
DB_STARTED=0
DEV_STARTED=0
DRY_RUN=0

usage() {
    cat <<'EOF'
Usage: scripts/quest-base-only-smoke.sh [--dry-run]

Runs the Quest/BQ integration regression suite in a temporary internal-only
PostgreSQL network, then assembles and checks the self-contained Base JAR.
The pinned DefaultQuests definition directory is mounted read-only. No Minecraft
server is launched and no production Compose file, database, or service is used.

--dry-run  Validate local inputs and print the isolated test scope without
           creating Docker resources or running Gradle.
EOF
}

fail() {
    printf '[quest-smoke] ERROR: %s\n' "$*" >&2
    exit 1
}

cleanup() {
    if (( DEV_STARTED )); then
        docker rm -f "$DEV_CONTAINER" >/dev/null 2>&1 || true
    fi
    if (( DB_STARTED )); then
        docker rm -f "$DB_CONTAINER" >/dev/null 2>&1 || true
    fi
    if (( NETWORK_CREATED )); then
        docker network rm "$NETWORK_NAME" >/dev/null 2>&1 || true
    fi
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

parse_args() {
    while (( $# > 0 )); do
        case "$1" in
            --dry-run) DRY_RUN=1 ;;
            -h|--help) usage; exit 0 ;;
            *) fail "unknown argument: $1" ;;
        esac
        shift
    done
}

preflight() {
    command -v docker >/dev/null 2>&1 || fail "docker is required"
    command -v sha256sum >/dev/null 2>&1 || fail "sha256sum is required"
    command -v openssl >/dev/null 2>&1 || fail "openssl is required"
    [[ -x "$PROJECT_ROOT/gradlew" ]] || fail "Gradle wrapper is missing"
    [[ -r "$PREVIEW_FONT" ]] || fail "UI2 preview CJK font is required: $PREVIEW_FONT"
    [[ -r "$QZ_LICENSE_SOURCE" ]] || fail "the pinned Qz LGPL license file is missing: $QZ_LICENSE_SOURCE"
    [[ "$(sha256sum "$QZ_LICENSE_SOURCE" | awk '{print $1}')" == "$QZ_LICENSE_SHA256" ]] \
        || fail "the Qz LGPL license SHA-256 does not match the pinned upstream revision"
    [[ -d "$SOURCE_DIR/Quests" && -d "$SOURCE_DIR/QuestLines" \
        && -f "$SOURCE_DIR/QuestLinesOrder.txt" ]] || fail "pinned DefaultQuests directory is incomplete"
    [[ -r "$QUEST_SCHEMA" ]] || fail "Quest PostgreSQL schema resource is missing: $QUEST_SCHEMA"
    local migration
    for migration in "${QUEST_MIGRATIONS[@]}"; do
        [[ -r "$PROJECT_ROOT/ops/sql/migrations/$migration" ]] \
            || fail "required Quest migration is missing: $migration"
    done
    SOURCE_DIR="$(realpath -e "$SOURCE_DIR")"
    PREVIEW_FONT="$(realpath -e "$PREVIEW_FONT")"
    QZ_LICENSE_SOURCE="$(realpath -e "$QZ_LICENSE_SOURCE")"
    [[ "$SOURCE_DIR" == "$DEFAULT_SOURCE" ]] || fail "source must be the authorized fixed DefaultQuests directory"
    docker image inspect "$DEV_IMAGE" >/dev/null 2>&1 || fail "required local image is missing: $DEV_IMAGE"
    docker image inspect "$POSTGRES_IMAGE" >/dev/null 2>&1 || fail "required local image is missing: $POSTGRES_IMAGE"
    [[ ! -L "$RUN_ROOT" ]] || fail "isolated output root may not be a symlink"
}

run_smoke() {
    mkdir -p "$GRADLE_USER_HOME_DIR" "$RUN_ROOT"
    PG_PASSWORD="$(openssl rand -hex 24)"
    docker network create --internal "$NETWORK_NAME" >/dev/null
    NETWORK_CREATED=1

    docker run --detach --rm \
        --name "$DB_CONTAINER" \
        --network "$NETWORK_NAME" \
        --memory 1g --cpus 2 \
        --tmpfs /var/lib/postgresql/data:rw,size=512m \
        --env "POSTGRES_DB=$PG_DATABASE" \
        --env "POSTGRES_USER=$PG_USER" \
        --env "POSTGRES_PASSWORD=$PG_PASSWORD" \
        --health-cmd "pg_isready -U $PG_USER -d $PG_DATABASE" \
        --health-interval 1s --health-timeout 2s --health-retries 45 \
        "$POSTGRES_IMAGE" >/dev/null
    DB_STARTED=1

    local attempt health
    for attempt in $(seq 1 60); do
        health="$(docker inspect -f '{{.State.Health.Status}}' "$DB_CONTAINER" 2>/dev/null || true)"
        if [[ "$health" == "healthy" ]]; then break; fi
        if [[ "$health" == "unhealthy" ]]; then fail "isolated PostgreSQL failed its health check"; fi
        sleep 1
    done
    [[ "$health" == "healthy" ]] || fail "timed out waiting for isolated PostgreSQL"

    DEV_STARTED=1
    run_dev_gradle() {
        docker run --rm \
            --name "$DEV_CONTAINER" \
            --network "container:$DB_CONTAINER" \
            --memory 6g --cpus 4 \
            --workdir "$CONTAINER_PROJECT_ROOT" \
            --volume "$PROJECT_ROOT:$CONTAINER_PROJECT_ROOT:rw" \
            --volume "$GRADLE_USER_HOME_DIR:$CONTAINER_GRADLE_HOME:rw" \
            --volume "$SOURCE_DIR:$CONTAINER_SOURCE:ro" \
            --volume "$PREVIEW_FONT:/fonts/NotoSansCJK-Regular.ttc:ro" \
            --env "GRADLE_USER_HOME=$CONTAINER_GRADLE_HOME" \
            --env "UI2_PREVIEW_FONT=/fonts/NotoSansCJK-Regular.ttc" \
            --env "QUEST_BASE_ONLY_SMOKE_RUN_ID=$RUN_ID" \
            --env "QUEST_TEST_POSTGRES_URL=jdbc:postgresql://127.0.0.1:5432/$PG_DATABASE" \
            --env "QUEST_TEST_POSTGRES_ISOLATED=true" \
            --env "POSTGRES_USER=$PG_USER" \
            --env "POSTGRES_PASSWORD=$PG_PASSWORD" \
            --env "BQ_MIGRATION_JDBC_URL=jdbc:postgresql://127.0.0.1:5432/$IMPORT_DATABASE" \
            --env "BQ_MIGRATION_DB_USER=$PG_USER" \
            --env "BQ_MIGRATION_DB_PASSWORD=$PG_PASSWORD" \
            --env "BQ_MIGRATION_WRITE_CONFIRM=ISOLATED_ONLY" \
            --env "BQ_PREFLIGHT_JDBC_URL=jdbc:postgresql://127.0.0.1:5432/$IMPORT_DATABASE" \
            --env "BQ_PREFLIGHT_DB_USER=$PG_USER" \
            --env "BQ_PREFLIGHT_DB_PASSWORD=$PG_PASSWORD" \
            --env "BQ_PREFLIGHT_ISOLATED_CONFIRM=ISOLATED_ONLY" \
            --env "BQ_TEST_DEFAULT_QUESTS=$CONTAINER_SOURCE" \
            --env "GIT_CONFIG_COUNT=1" \
            --env "GIT_CONFIG_KEY_0=safe.directory" \
            --env "GIT_CONFIG_VALUE_0=$CONTAINER_PROJECT_ROOT" \
            "$DEV_IMAGE" \
            ./gradlew \
            --init-script "$CONTAINER_PROJECT_ROOT/scripts/quest-base-only-smoke.init.gradle" \
            --project-cache-dir "$CONTAINER_PROJECT_ROOT/.gradle/codex-quest-base-only-smoke-$RUN_ID/project-cache" \
            --offline --no-daemon --no-configuration-cache --rerun-tasks \
            -PforceToolchainVersion=17 \
            -Pgtnh.modules.codeStyle=false \
            -Pgtnh.modules.updater=false \
            -Pgtnh.modules.gitVersion=false \
            "$@"
    }

    # RFG's project-JAR transform resolves during graph calculation, so warm
    # isolated project archives before the root shadowJar graph is configured.
    run_dev_gradle \
        :galaxy-quest-core:jar \
        :galaxy-quest-postgres:jar \
        :ui2-core:jar \
        :ui2-lab:jar \
        :ui2-terminal:jar

    run_dev_gradle \
        :galaxy-quest-core:test \
        :galaxy-quest-postgres:test \
        :galaxy-quest-bq-importer:test \
        :ui2-core:test \
        :ui2-terminal:test \
        test assemble verifyUi2SelfContained

    docker exec "$DB_CONTAINER" createdb -U "$PG_USER" "$IMPORT_DATABASE"
    docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$PG_USER" -d "$IMPORT_DATABASE" \
        < "$QUEST_SCHEMA"
    local migration
    for migration in "${QUEST_MIGRATIONS[@]}"; do
        docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$PG_USER" -d "$IMPORT_DATABASE" \
            < "$PROJECT_ROOT/ops/sql/migrations/$migration"
    done

    local import_log="$RUN_ROOT/logs/defaultquests-apply.log"
    mkdir -p "$RUN_ROOT/logs"
    if run_dev_gradle :galaxy-quest-bq-importer:run "--args=apply $CONTAINER_SOURCE" \
        > "$import_log" 2>&1; then
        rg '^(batchId|sourceHash|quests|chapters|applicable|status)=' "$import_log" || true
        printf '[quest-smoke] Import warnings: %s; full report: %s\n' \
            "$(grep -c '^WARNING|' "$import_log" || true)" "$import_log"
    else
        local import_status=$?
        tail -n 160 "$import_log" >&2
        return "$import_status"
    fi

    local preflight_jar_host preflight_jar_container preflight_jar_sha256
    preflight_jar_host="$RUN_ROOT/build/root/libs/jsirgalaxybase-0.0.0-quest-base-only-smoke.jar"
    [[ -f "$preflight_jar_host" ]] || fail "isolated runtime JAR is missing before preflight"
    preflight_jar_container="$CONTAINER_PROJECT_ROOT/.gradle/codex-quest-base-only-smoke-$RUN_ID/build/root/libs/jsirgalaxybase-0.0.0-quest-base-only-smoke.jar"
    preflight_jar_sha256="$(sha256sum "$preflight_jar_host" | awk '{print $1}')"
    run_dev_gradle :galaxy-quest-bq-importer:baseOnlyPreflight \
        "-PbqDefaultQuests=$CONTAINER_SOURCE" \
        "-PbqBaseJar=$preflight_jar_container" \
        "-PbqExpectedSourceSha256=$DEFAULT_QUESTS_SHA256" \
        "-PbqExpectedJarSha256=$preflight_jar_sha256"

    local runtime_jar
    runtime_jar="$(find "$RUN_ROOT/build/root/libs" -maxdepth 1 -type f \
        -name 'jsirgalaxybase-*-quest-base-only-smoke.jar' ! -name '*-sources.jar' -print -quit)"
    [[ -n "$runtime_jar" && -f "$runtime_jar" ]] || fail "isolated runtime JAR was not produced"
    printf '[quest-smoke] Runtime JAR: %s\n' "$runtime_jar"
    printf '[quest-smoke] SHA-256: %s\n' "$(sha256sum "$runtime_jar" | awk '{print $1}')"
    printf '[quest-smoke] Build and test reports: %s\n' "$RUN_ROOT/build"
}

main() {
    parse_args "$@"
    preflight
    if (( DRY_RUN )); then
        printf '[quest-smoke] Source: %s (mounted read-only)\n' "$SOURCE_DIR"
        printf '[quest-smoke] UI2 preview font: %s (mounted read-only; not packaged)\n' "$PREVIEW_FONT"
        printf '[quest-smoke] Pinned Qz LGPL license SHA-256 verified from tracked source.\n'
        printf '[quest-smoke] Database: %s on a new internal-only tmpfs network\n' "$PG_DATABASE"
        printf '[quest-smoke] Import DB: %s; Quest schema/migrations, fixed DefaultQuests apply, and read-only Base JAR preflight\n' "$IMPORT_DATABASE"
        printf '[quest-smoke] Gradle scope: Quest Core/Postgres/Importer, UI2 Core/Terminal, root tests, assemble, self-contained verifier\n'
        printf '[quest-smoke] No server startup, production Compose, published database port, or persistent volume.\n'
        return
    fi
    run_smoke
}

main "$@"
