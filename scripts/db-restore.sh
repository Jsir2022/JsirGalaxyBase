#!/usr/bin/env bash

set -euo pipefail

PGHOST="${PGHOST:-127.0.0.1}"
PGPORT="${PGPORT:-5432}"
PGDATABASE="${PGDATABASE:-jsirgalaxybase}"
PGUSER="${PGUSER:-jsirgalaxybase_app}"
BACKUP_DIR="${BACKUP_DIR:-$HOME/db-backups/jsirgalaxybase}"
PGADMIN_DATABASE="${PGADMIN_DATABASE:-postgres}"
PGADMINUSER="${PGADMINUSER:-}"
PGADMINPASSWORD="${PGADMINPASSWORD:-}"

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
    - When using --recreate-db, either provide an admin role via PGADMINUSER/PGADMINPASSWORD or run with local sudo access to the postgres OS user.
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

admin_psql() {
    if [[ -n "$PGADMINUSER" ]]; then
        local admin_env=("PGHOST=$PGHOST" "PGPORT=$PGPORT" "PGUSER=$PGADMINUSER")
        if [[ -n "$PGADMINPASSWORD" ]]; then
            admin_env+=("PGPASSWORD=$PGADMINPASSWORD")
        fi
        env "${admin_env[@]}" psql --dbname="$PGADMIN_DATABASE" "$@"
        return
    fi

    if [[ "$(id -u)" -eq 0 ]]; then
        sudo -u postgres psql --dbname="$PGADMIN_DATABASE" "$@"
        return
    fi

    if command -v sudo >/dev/null 2>&1; then
        sudo -u postgres psql --dbname="$PGADMIN_DATABASE" "$@"
        return
    fi

    echo "--recreate-db requires either PGADMINUSER/PGADMINPASSWORD or local sudo access to the postgres user." >&2
    exit 1
}

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
    db_name_literal="$(printf "%s" "$PGDATABASE" | sed "s/'/''/g")"
    admin_psql --set=ON_ERROR_STOP=1 \
        --command="SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$db_name_literal' AND pid <> pg_backend_pid();"
    admin_psql --set=ON_ERROR_STOP=1 \
        --command="DROP DATABASE IF EXISTS \"$PGDATABASE\";"
    admin_psql --set=ON_ERROR_STOP=1 \
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
SELECT 'coin_exchange_record=' || count(*) FROM coin_exchange_record;
SELECT 'managed_accounts=' || count(*)
FROM bank_account
WHERE owner_ref IN ('SYSTEM_OPERATIONS', 'EXCHANGE_RESERVE');
EOF

echo "Restore complete"
echo "  source: $backup_file"
echo "  target: postgresql://$PGUSER@$PGHOST:$PGPORT/$PGDATABASE"