#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/deploy-common.sh
source "$SCRIPT_DIR/lib/deploy-common.sh"

SKIP_BUILD=0
DRY_RUN=0
LAUNCH_CLIENT=0
TARGETS_CSV="lobby,s2,client"
JAR_PATH=""
GRADLE_TASK="assemble"

usage() {
    cat <<EOF
Usage: $(basename "$0") [--skip-build] [--jar <path>] [--targets lobby,s2,client] [--launch-client] [--dry-run]

Orchestrates JsirGalaxyBase gray-chain deployment:
  1. Build runtime jar with Docker Gradle unless --skip-build is set
  2. Deploy to lobby/s2/client
  3. Restart gray services as needed
  4. Verify hashes and server readiness

Defaults:
  targets: ${TARGETS_CSV}
  prism account: ${PRISM_ACCOUNT}
  prism server: ${PRISM_SERVER_ADDR}
EOF
}

launch_client() {
    local cmd=(
        "$PRISM_APPIMAGE"
        -d "$PRISM_DATA_DIR"
        -l "$PRISM_INSTANCE_NAME"
        -a "$PRISM_ACCOUNT"
        -s "$PRISM_SERVER_ADDR"
        --show-window
    )
    [[ -x "$PRISM_APPIMAGE" ]] || fail "Prism AppImage not found or not executable: $PRISM_APPIMAGE"
    if (( DRY_RUN )); then
        printf '[deploy] DRY RUN:'
        printf ' %q' "${cmd[@]}"
        printf '\n'
        return 0
    fi
    pkill -f 'org.prismlauncher.EntryPoint|GT New Horizons 2.8.4|PrismLauncher-Linux-x86_64.AppImage' >/dev/null 2>&1 || true
    nohup "${cmd[@]}" >/tmp/jsirgalaxybase-prism-launch.log 2>&1 &
    log "Launched Prism client with account ${PRISM_ACCOUNT} -> ${PRISM_SERVER_ADDR}"
}

main() {
    ensure_project_root

    while (( $# > 0 )); do
        case "$1" in
            --skip-build)
                SKIP_BUILD=1
                ;;
            --jar)
                shift
                [[ $# -gt 0 ]] || fail "--jar requires a path"
                JAR_PATH="$1"
                ;;
            --targets)
                shift
                [[ $# -gt 0 ]] || fail "--targets requires a csv list"
                TARGETS_CSV="$1"
                ;;
            --launch-client)
                LAUNCH_CLIENT=1
                ;;
            --dry-run)
                DRY_RUN=1
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

    if (( SKIP_BUILD )); then
        if [[ -n "$JAR_PATH" ]]; then
            [[ -f "$JAR_PATH" ]] || fail "jar not found: $JAR_PATH"
        else
            JAR_PATH="$(find_latest_runtime_jar)"
        fi
        log "Skipping build; using jar: $JAR_PATH"
    else
        local -a build_cmd=("$SCRIPT_DIR/build-mod.sh" --task "$GRADLE_TASK")
        if (( DRY_RUN )); then
            build_cmd+=(--dry-run)
        fi
        "${build_cmd[@]}" >/dev/null
        if (( DRY_RUN )); then
            if [[ -z "$JAR_PATH" ]]; then
                if runtime_jar_candidates >/dev/null 2>&1 && [[ -n "$(runtime_jar_candidates | head -n 1)" ]]; then
                    JAR_PATH="$(find_latest_runtime_jar)"
                else
                    JAR_PATH="$PROJECT_ROOT/build/libs/<runtime-jar>.jar"
                fi
            fi
        else
            JAR_PATH="${JAR_PATH:-$(find_latest_runtime_jar)}"
        fi
    fi

    if (( DRY_RUN )); then
        "$SCRIPT_DIR/deploy-gray-chain.sh" --dry-run --jar "$JAR_PATH" --targets "$TARGETS_CSV"
    else
        "$SCRIPT_DIR/deploy-gray-chain.sh" --jar "$JAR_PATH" --targets "$TARGETS_CSV"
    fi

    if (( LAUNCH_CLIENT )); then
        launch_client
    fi

    log "Deployment flow finished"
}

main "$@"
