#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

PGHOST="${PGHOST:-127.0.0.1}"
PGPORT="${PGPORT:-5432}"
PGDATABASE="${PGDATABASE:-jsirgalaxybase}"
PGUSER="${PGUSER:-jsirgalaxybase_app}"
MIGRATION_DIR="${MIGRATION_DIR:-$PROJECT_ROOT/ops/sql/migrations}"
HISTORY_TABLE="${HISTORY_TABLE:-schema_migration_history}"
LOCK_KEY="${LOCK_KEY:-2026040301}"
STATUS_ONLY="false"

usage() {
    cat <<'EOF'
Usage:
  scripts/db-migrate.sh
  scripts/db-migrate.sh --status

Notes:
  - Stop the Minecraft server before applying schema migrations.
  - Pass PGPASSWORD in the environment when authentication is required.
  - Migrations are applied in filename order and recorded in schema_migration_history.
EOF
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "Missing required command: $1" >&2
        exit 1
    }
}

db_psql() {
    PGHOST="$PGHOST" PGPORT="$PGPORT" PGDATABASE="$PGDATABASE" PGUSER="$PGUSER" \
        psql --no-psqlrc --set=ON_ERROR_STOP=1 "$@"
}

escape_sql_literal() {
    printf "%s" "$1" | sed "s/'/''/g"
}

ensure_history_table() {
    db_psql <<EOF
CREATE TABLE IF NOT EXISTS ${HISTORY_TABLE} (
    migration_id VARCHAR(160) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
EOF
}

list_migration_files() {
    find "$MIGRATION_DIR" -maxdepth 1 -type f -name '*.sql' | sort
}

applied_checksum() {
    local migration_id="$1"
    local escaped_id
    escaped_id="$(escape_sql_literal "$migration_id")"
    db_psql --tuples-only --no-align \
        --command="SELECT checksum_sha256 FROM ${HISTORY_TABLE} WHERE migration_id = '${escaped_id}'"
}

applied_at() {
    local migration_id="$1"
    local escaped_id
    escaped_id="$(escape_sql_literal "$migration_id")"
    db_psql --tuples-only --no-align \
        --command="SELECT to_char(applied_at, 'YYYY-MM-DD HH24:MI:SSOF') FROM ${HISTORY_TABLE} WHERE migration_id = '${escaped_id}'"
}

print_status() {
    ensure_history_table
    while IFS= read -r file; do
        local_id="$(basename "$file")"
        local_applied_at="$(applied_at "$local_id")"
        if [[ -n "$local_applied_at" ]]; then
            echo "applied  $local_id  $local_applied_at"
        else
            echo "pending  $local_id"
        fi
    done < <(list_migration_files)
}

apply_migration_file() {
    local file="$1"
    local migration_id checksum recorded_checksum escaped_id escaped_description escaped_checksum temp_sql

    migration_id="$(basename "$file")"
    checksum="$(sha256sum "$file" | awk '{print $1}')"
    recorded_checksum="$(applied_checksum "$migration_id")"

    if [[ -n "$recorded_checksum" ]]; then
        if [[ "$recorded_checksum" != "$checksum" ]]; then
            echo "Checksum mismatch for already applied migration: $migration_id" >&2
            exit 1
        fi
        echo "Skipping already applied migration: $migration_id"
        return
    fi

    escaped_id="$(escape_sql_literal "$migration_id")"
    escaped_description="$escaped_id"
    escaped_checksum="$(escape_sql_literal "$checksum")"
    temp_sql="$(mktemp)"

    cat > "$temp_sql" <<EOF
BEGIN;
SELECT pg_advisory_xact_lock(${LOCK_KEY});
CREATE TABLE IF NOT EXISTS ${HISTORY_TABLE} (
    migration_id VARCHAR(160) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
DO \$\$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM ${HISTORY_TABLE}
        WHERE migration_id = '${escaped_id}'
          AND checksum_sha256 <> '${escaped_checksum}'
    ) THEN
        RAISE EXCEPTION 'Checksum mismatch for already applied migration %', '${escaped_id}';
    END IF;
END
\$\$;
\\i ${file}
INSERT INTO ${HISTORY_TABLE} (migration_id, description, checksum_sha256)
VALUES ('${escaped_id}', '${escaped_description}', '${escaped_checksum}')
ON CONFLICT (migration_id) DO NOTHING;
COMMIT;
EOF

    echo "Applying migration: $migration_id"
    db_psql --file="$temp_sql"
    rm -f "$temp_sql"
}

require_command psql
require_command sha256sum

while [[ "$#" -gt 0 ]]; do
    case "$1" in
        --status)
            STATUS_ONLY="true"
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage >&2
            exit 1
            ;;
    esac
done

[[ -d "$MIGRATION_DIR" ]] || {
    echo "Migration directory not found: $MIGRATION_DIR" >&2
    exit 1
}

if [[ "$STATUS_ONLY" == "true" ]]; then
    print_status
    exit 0
fi

ensure_history_table
while IFS= read -r migration_file; do
    apply_migration_file "$migration_file"
done < <(list_migration_files)

echo "Migration complete"
echo "  target: postgresql://$PGUSER@$PGHOST:$PGPORT/$PGDATABASE"