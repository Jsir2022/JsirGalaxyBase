#!/usr/bin/env bash

set -euo pipefail

PGHOST="${PGHOST:-127.0.0.1}"
PGPORT="${PGPORT:-5432}"
PGDATABASE="${PGDATABASE:-jsirgalaxybase}"
PGUSER="${PGUSER:-jsirgalaxybase_app}"
BACKUP_DIR="${BACKUP_DIR:-$HOME/db-backups/jsirgalaxybase}"
RETAIN_COUNT="${RETAIN_COUNT:-0}"

require_command() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "Missing required command: $1" >&2
        exit 1
    }
}

require_command pg_dump
require_command pg_restore
require_command sha256sum

mkdir -p "$BACKUP_DIR"

timestamp="$(date +%F-%H%M%S)"
base_name="${PGDATABASE}-${timestamp}"
backup_file="$BACKUP_DIR/$base_name.dump"
checksum_file="$backup_file.sha256"

echo "Creating backup: $backup_file"
PGHOST="$PGHOST" PGPORT="$PGPORT" PGDATABASE="$PGDATABASE" PGUSER="$PGUSER" \
    pg_dump --format=custom --compress=9 --file="$backup_file"

sha256sum "$backup_file" > "$checksum_file"
pg_restore --list "$backup_file" >/dev/null

echo "Backup complete"
echo "  file: $backup_file"
echo "  sha256: $checksum_file"

if [[ "$RETAIN_COUNT" =~ ^[0-9]+$ ]] && [[ "$RETAIN_COUNT" -gt 0 ]]; then
    mapfile -t backup_files < <(find "$BACKUP_DIR" -maxdepth 1 -type f -name "${PGDATABASE}-*.dump" | sort)
    if [[ "${#backup_files[@]}" -gt "$RETAIN_COUNT" ]]; then
        delete_count=$(( ${#backup_files[@]} - RETAIN_COUNT ))
        for old_file in "${backup_files[@]:0:delete_count}"; do
            echo "Pruning old backup: $old_file"
            rm -f "$old_file" "$old_file.sha256"
        done
    fi
fi