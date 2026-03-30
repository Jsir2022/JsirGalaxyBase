#!/usr/bin/env bash

set -euo pipefail

PGHOST="${PGHOST:-127.0.0.1}"
PGPORT="${PGPORT:-5432}"
PGDATABASE="${PGDATABASE:-jsirgalaxybase}"
PGUSER="${PGUSER:-jsirgalaxybase_app}"
BACKUP_DIR="${BACKUP_DIR:-$HOME/db-backups/jsirgalaxybase}"

backup_file=""
use_latest="false"
recreate_db="false"

usage() {
    cat <<'EOF'
Usage:
  scripts/db-restore.sh --latest [--recreate-db]
  scripts/db-restore.sh <backup-file> [--recreate-db]

Notes:
  - Stop the Minecraft server before restoring.
  - Pass PGPASSWORD in the environment when authentication is required.
EOF
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "Missing required command: $1" >&2
        exit 1
    }
}

require_command pg_restore
require_command psql
require_command sha256sum

while [[ "$#" -gt 0 ]]; do
    case "$1" in
        --latest)
            use_latest="true"
            shift
            ;;
        --recreate-db)
            recreate_db="true"
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            if [[ -n "$backup_file" ]]; then
                echo "Unexpected extra argument: $1" >&2
                usage >&2
                exit 1
            fi
            backup_file="$1"
            shift
            ;;
    esac
done

if [[ "$use_latest" == "true" ]]; then
    backup_file="$(find "$BACKUP_DIR" -maxdepth 1 -type f -name "${PGDATABASE}-*.dump" | sort | tail -n 1)"
fi

if [[ -z "$backup_file" ]]; then
    echo "No backup file specified" >&2
    usage >&2
    exit 1
fi

if [[ ! -f "$backup_file" ]]; then
    echo "Backup file not found: $backup_file" >&2
    exit 1
fi

checksum_file="$backup_file.sha256"
if [[ -f "$checksum_file" ]]; then
    echo "Verifying checksum: $checksum_file"
    sha256sum --check "$checksum_file"
else
    echo "Warning: checksum file not found, skipping sha256 verification" >&2
fi

if [[ "$recreate_db" == "true" ]]; then
    echo "Recreating database: $PGDATABASE"
    PSQL_URI="postgresql://$PGUSER@${PGHOST}:${PGPORT}/postgres"
    PGHOST="$PGHOST" PGPORT="$PGPORT" PGUSER="$PGUSER" psql \
        --dbname="$PSQL_URI" \
        --set=target_db="$PGDATABASE" \
        --command="SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = :'target_db' AND pid <> pg_backend_pid();"
    PGHOST="$PGHOST" PGPORT="$PGPORT" PGUSER="$PGUSER" psql \
        --dbname="$PSQL_URI" \
        --set=target_db="$PGDATABASE" \
        --command="DROP DATABASE IF EXISTS \"$PGDATABASE\";"
    PGHOST="$PGHOST" PGPORT="$PGPORT" PGUSER="$PGUSER" psql \
        --dbname="$PSQL_URI" \
        --command="CREATE DATABASE \"$PGDATABASE\" OWNER \"$PGUSER\";"
fi

echo "Restoring backup: $backup_file"
PGHOST="$PGHOST" PGPORT="$PGPORT" PGDATABASE="$PGDATABASE" PGUSER="$PGUSER" \
    pg_restore --clean --if-exists --no-owner --no-privileges --dbname="$PGDATABASE" "$backup_file"

echo "Running post-restore health check"
PGHOST="$PGHOST" PGPORT="$PGPORT" PGDATABASE="$PGDATABASE" PGUSER="$PGUSER" psql --tuples-only --no-align <<'EOF'
SELECT 'bank_account=' || count(*) FROM bank_account;
SELECT 'bank_transaction=' || count(*) FROM bank_transaction;
SELECT 'ledger_entry=' || count(*) FROM ledger_entry;
SELECT 'managed_accounts=' || count(*)
FROM bank_account
WHERE owner_ref IN ('SYSTEM_OPERATIONS', 'EXCHANGE_RESERVE');
EOF

echo "Restore complete"
echo "  source: $backup_file"
echo "  target: postgresql://$PGUSER@$PGHOST:$PGPORT/$PGDATABASE"