#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEMPLATE_DIR="$REPO_ROOT/ops/systemd"
SERVICE_NAME="jsirgalaxybase-db-backup"
UNIT_DIR="/etc/systemd/system"
ENV_DIR="/etc/jsirgalaxybase"
ENV_FILE="$ENV_DIR/db-backup.env"
BACKUP_RUN_AS_USER="${BACKUP_RUN_AS_USER:-${SUDO_USER:-$USER}}"

require_root() {
    if [[ "$(id -u)" -ne 0 ]]; then
        echo "This installer must run as root. Example: sudo BACKUP_RUN_AS_USER=$BACKUP_RUN_AS_USER $0" >&2
        exit 1
    fi
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "Missing required command: $1" >&2
        exit 1
    }
}

render_template() {
    local source_file="$1"
    local target_file="$2"

    sed \
        -e "s|@REPO_ROOT@|$REPO_ROOT|g" \
        -e "s|@BACKUP_RUN_AS_USER@|$BACKUP_RUN_AS_USER|g" \
        -e "s|@ENV_FILE@|$ENV_FILE|g" \
        -e "s|@BACKUP_DIR@|$BACKUP_DIR|g" \
        "$source_file" > "$target_file"
}

require_root
require_command systemctl
require_command install
require_command sed
require_command getent

BACKUP_USER_HOME="$(getent passwd "$BACKUP_RUN_AS_USER" | cut -d: -f6)"
if [[ -z "$BACKUP_USER_HOME" ]]; then
    echo "Unable to resolve home directory for user: $BACKUP_RUN_AS_USER" >&2
    exit 1
fi

BACKUP_DIR="${BACKUP_DIR:-$BACKUP_USER_HOME/db-backups/jsirgalaxybase}"

install -d -m 0755 "$UNIT_DIR" "$ENV_DIR"

tmp_service="$(mktemp)"
tmp_timer="$(mktemp)"
tmp_env="$(mktemp)"
trap 'rm -f "$tmp_service" "$tmp_timer" "$tmp_env"' EXIT

render_template "$TEMPLATE_DIR/$SERVICE_NAME.service" "$tmp_service"
render_template "$TEMPLATE_DIR/$SERVICE_NAME.timer" "$tmp_timer"
render_template "$TEMPLATE_DIR/$SERVICE_NAME.env.example" "$tmp_env"

install -m 0644 "$tmp_service" "$UNIT_DIR/$SERVICE_NAME.service"
install -m 0644 "$tmp_timer" "$UNIT_DIR/$SERVICE_NAME.timer"

if [[ ! -f "$ENV_FILE" ]]; then
    install -m 0600 "$tmp_env" "$ENV_FILE"
    echo "Created environment file template: $ENV_FILE"
    echo "Edit PGPASSWORD before first timer run."
else
    echo "Keeping existing environment file: $ENV_FILE"
fi

systemctl daemon-reload
systemctl enable --now "$SERVICE_NAME.timer"
systemctl status --no-pager "$SERVICE_NAME.timer" || true

echo
echo "Installed $SERVICE_NAME.timer"
echo "Service file: $UNIT_DIR/$SERVICE_NAME.service"
echo "Timer file:   $UNIT_DIR/$SERVICE_NAME.timer"
echo "Env file:     $ENV_FILE"
echo "Backup dir:   $BACKUP_DIR"
