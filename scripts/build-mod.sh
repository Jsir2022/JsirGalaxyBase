#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/deploy-common.sh
source "$SCRIPT_DIR/lib/deploy-common.sh"

DRY_RUN=0
PRINT_PATH=0
PRINT_SHA=0
GRADLE_TASK="${GRADLE_TASK:-assemble}"

usage() {
    cat <<EOF
Usage: $(basename "$0") [--dry-run] [--print-path] [--print-sha] [--task <gradle-task>]

Builds JsirGalaxyBase with the validated Docker Gradle path.

Defaults:
  task: ${GRADLE_TASK}
  compose file: ${COMPOSE_FILE}
  gradle service: ${GRADLE_SERVICE}
  gradle user home: ${GRADLE_USER_HOME_DIR}
  gradle project cache: ${GRADLE_PROJECT_CACHE_DIR}
EOF
}

run_build() {
    mkdir -p "$GRADLE_USER_HOME_DIR" "$GRADLE_PROJECT_CACHE_DIR"
    local cmd=(
        docker compose -f "$COMPOSE_FILE" run --rm
        -v "$GRADLE_USER_HOME_DIR:/codex-gradle-home"
        -v "$GRADLE_PROJECT_CACHE_DIR:/codex-project-cache"
        -e GRADLE_USER_HOME=/codex-gradle-home
        "$GRADLE_SERVICE"
        ./gradlew "$GRADLE_TASK"
        --project-cache-dir /codex-project-cache
        --no-daemon
        --no-configuration-cache
        -PforceToolchainVersion=17
    )
    if (( DRY_RUN )); then
        printf '[deploy] DRY RUN:'
        printf ' %q' "${cmd[@]}"
        printf '\n'
        return 0
    fi
    (cd "$PROJECT_ROOT" && "${cmd[@]}")
}

main() {
    ensure_project_root
    require_command docker

    while (( $# > 0 )); do
        case "$1" in
            --dry-run)
                DRY_RUN=1
                ;;
            --print-path)
                PRINT_PATH=1
                ;;
            --print-sha)
                PRINT_SHA=1
                ;;
            --task)
                shift
                [[ $# -gt 0 ]] || fail "--task requires a value"
                GRADLE_TASK="$1"
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                fail "unknown argument: $1"
                ;;
        esac
        shift
    done

    log "Building JsirGalaxyBase with Docker Gradle task: $GRADLE_TASK"
    run_build
    if (( DRY_RUN )); then
        exit 0
    fi

    local jar sha
    jar="$(find_latest_runtime_jar)"
    sha="$(jar_sha256 "$jar")"
    log "Build complete"
    log "Runtime jar: $jar"
    log "SHA256: $sha"

    if (( PRINT_PATH )); then
        printf '%s\n' "$jar"
    fi
    if (( PRINT_SHA )); then
        printf '%s\n' "$sha"
    fi
}

main "$@"
