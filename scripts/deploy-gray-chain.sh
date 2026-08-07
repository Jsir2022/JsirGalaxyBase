#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/deploy-common.sh
source "$SCRIPT_DIR/lib/deploy-common.sh"

DRY_RUN=0
SKIP_RESTART=0
SKIP_LOG_CHECK=0
SKIP_DB_MIGRATE=0
TARGETS_CSV="lobby,s2,client"
JAR_PATH=""
BACKUP_ROOT="$DEFAULT_BACKUP_ROOT"

usage() {
    cat <<EOF
Usage: $(basename "$0") --jar <path> [--targets lobby,s2,client] [--dry-run] [--skip-restart] [--skip-log-check] [--skip-db-migrate]

Deploys a built JsirGalaxyBase runtime jar to the gray chain and client mods directory.

Defaults:
  targets: ${TARGETS_CSV}
  backup root: ${BACKUP_ROOT}
  gray container: ${GRAY_CONTAINER}

Notes:
  - Only lobby/s2/client are supported.
  - S1 is intentionally excluded.
EOF
}

copy_with_backup() {
    local target="$1"
    local source_jar="$2"
    local backup_dir="$3"
    local target_dir target_path existing_jar
    target_dir="$(target_mod_dir "$target")"
    target_path="$target_dir/$(jar_basename "$source_jar")"

    [[ -d "$target_dir" ]] || fail "target mods directory not found for $target: $target_dir"
    mkdir -p "$backup_dir/$target"

    while IFS= read -r existing_jar; do
        [[ -n "$existing_jar" ]] || continue
        cp -f "$existing_jar" "$backup_dir/$target/"
    done < <(find "$target_dir" -maxdepth 1 -type f -name 'jsirgalaxybase*.jar' | sort)

    if (( DRY_RUN )); then
        log "Dry run would copy $source_jar -> $target_path"
        return 0
    fi

    find "$target_dir" -maxdepth 1 -type f -name 'jsirgalaxybase*.jar' -delete
    cp -f "$source_jar" "$target_path"
}

verify_hash_match() {
    local source_jar="$1"
    local target="$2"
    local target_path
    target_path="$(target_mod_dir "$target")/$(jar_basename "$source_jar")"
    [[ -f "$target_path" ]] || fail "deployed jar missing for $target: $target_path"
    local source_sha target_sha
    source_sha="$(jar_sha256 "$source_jar")"
    target_sha="$(jar_sha256 "$target_path")"
    [[ "$source_sha" == "$target_sha" ]] || fail "hash mismatch for $target: $target_path"
}

main() {
    ensure_project_root
    require_command cp
    require_command docker
    require_command sha256sum

    while (( $# > 0 )); do
        case "$1" in
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
            --backup-root)
                shift
                [[ $# -gt 0 ]] || fail "--backup-root requires a path"
                BACKUP_ROOT="$1"
                ;;
            --skip-restart)
                SKIP_RESTART=1
                ;;
            --skip-log-check)
                SKIP_LOG_CHECK=1
                ;;
            --skip-db-migrate)
                SKIP_DB_MIGRATE=1
                ;;
            --dry-run)
                DRY_RUN=1
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

    [[ -n "$JAR_PATH" ]] || fail "--jar is required"
    if (( ! DRY_RUN )); then
        [[ -f "$JAR_PATH" ]] || fail "jar not found: $JAR_PATH"
    fi

    mapfile -t targets < <(split_targets_csv "$TARGETS_CSV")
    [[ ${#targets[@]} -gt 0 ]] || fail "no deploy targets resolved from: $TARGETS_CSV"
    validate_targets "${targets[@]}"

    local needs_server_restart=0
    local target
    for target in "${targets[@]}"; do
        [[ "$target" == "lobby" || "$target" == "s2" ]] && needs_server_restart=1
    done
    if (( needs_server_restart )); then
        ensure_gray_container_running
    fi

    local -a services_to_restart=()
    if (( ! SKIP_RESTART )); then
        for target in "${targets[@]}"; do
            case "$target" in
                lobby|s2) services_to_restart+=("$target") ;;
            esac
        done
    fi

    if (( ${#services_to_restart[@]} > 0 )) && (( ! DRY_RUN )); then
        log "Stopping gray services before schema migration: ${services_to_restart[*]}"
        stop_gray_services "${services_to_restart[@]}"
        for target in "${services_to_restart[@]}"; do
            wait_for_supervisor_stopped "$target"
        done
        if (( ! SKIP_DB_MIGRATE )); then
            apply_database_migrations
        fi
    fi

    local timestamp backup_dir
    timestamp="$(timestamp_utc)"
    backup_dir="$BACKUP_ROOT/$timestamp"
    mkdir -p "$backup_dir"

    log "Deploying jar: $JAR_PATH"
    log "Targets: ${targets[*]}"
    log "Backup directory: $backup_dir"

    for target in "${targets[@]}"; do
        copy_with_backup "$target" "$JAR_PATH" "$backup_dir"
    done

    if (( DRY_RUN )); then
        exit 0
    fi

    for target in "${targets[@]}"; do
        verify_hash_match "$JAR_PATH" "$target"
    done

    if (( ${#services_to_restart[@]} > 0 )); then
        log "Restarting gray services: ${services_to_restart[*]}"
        restart_gray_services "${services_to_restart[@]}"
        for target in "${services_to_restart[@]}"; do
            wait_for_supervisor_running "$target" >/dev/null
        done
        if (( ! SKIP_LOG_CHECK )); then
            for target in "${services_to_restart[@]}"; do
                wait_for_log_done "$target"
            done
        fi
    fi

    local -a deployed_paths=()
    for target in "${targets[@]}"; do
        deployed_paths+=("$(target_mod_dir "$target")/$(jar_basename "$JAR_PATH")")
    done
    log "Hash verification:"
    print_hash_report "$JAR_PATH" "${deployed_paths[@]}"
}

main "$@"
