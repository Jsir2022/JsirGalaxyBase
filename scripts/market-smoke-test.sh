#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/deploy-common.sh
source "$SCRIPT_DIR/lib/deploy-common.sh"

TARGET="lobby"
DB_CONTAINER="${DB_CONTAINER:-galaxy-base}"
DB_USER="${DB_USER:-jsirgalaxybase_app}"
DB_NAME="${DB_NAME:-jsirgalaxybase}"

usage() {
    cat <<EOF
Usage: $(basename "$0") [--target lobby|s2]

Checks the standardized market data path without restarting the Minecraft client:
  1. PostgreSQL has enabled formal catalog products.
  2. Lobby/S2 log reached Done.
  3. Recent server log has a [market-smoke] runtime-catalog admission diagnostic.

If the script says runtime diagnostics are not observed yet, deploy the latest
jar and restart the target server before trusting the terminal UI data path.
EOF
}

psql_value() {
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -X -At -v ON_ERROR_STOP=1 -c "$1"
}

psql_table() {
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -X -v ON_ERROR_STOP=1 -c "$1"
}

print_section() {
    printf '\n== %s ==\n' "$1"
}

server_logs() {
    local latest alt
    latest="$(target_log_path "$TARGET")"
    alt="$(target_alt_log_path "$TARGET")"
    [[ -f "$latest" ]] && tail -n 5000 "$latest"
    [[ -f "$alt" ]] && tail -n 5000 "$alt"
}

while (( $# > 0 )); do
    case "$1" in
        --target)
            shift
            [[ $# -gt 0 ]] || fail "--target requires a value"
            TARGET="$1"
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

case "$TARGET" in
    lobby|s2) ;;
    *) fail "unsupported target: $TARGET" ;;
esac

require_command docker
docker ps --format '{{.Names}}' | grep -qx "$DB_CONTAINER" || fail "database container is not running: $DB_CONTAINER"

print_section "formal standardized catalog"
catalog_count="$(psql_value "SELECT count(*) FROM standardized_market_catalog WHERE enabled = TRUE;")"
technical_display_count="$(psql_value "SELECT count(*) FROM standardized_market_catalog WHERE enabled = TRUE AND display_name = product_key;")"
printf 'enabled_catalog_products=%s\n' "$catalog_count"
printf 'enabled_catalog_technical_display_names=%s\n' "$technical_display_count"
psql_table "
SELECT product_key, display_name, registry_name, meta, unit_label, enabled, sort_order, catalog_version
FROM standardized_market_catalog
WHERE enabled = TRUE
ORDER BY sort_order, display_name, product_key
LIMIT 12;
"

print_section "recent trades"
psql_table "
SELECT product_key, count(*) AS trades, max(created_at) AS last_trade
FROM market_trade_record
GROUP BY product_key
ORDER BY max(created_at) DESC NULLS LAST, product_key
LIMIT 12;
"

print_section "player custody"
psql_table "
SELECT owner_player_ref, product_key, custody_status, sum(quantity) AS quantity
FROM market_custody_inventory
WHERE custody_status IN ('AVAILABLE', 'ESCROW_SELL', 'CLAIMABLE')
GROUP BY owner_player_ref, product_key, custody_status
ORDER BY max(updated_at) DESC, owner_player_ref, product_key
LIMIT 12;
"

print_section "server log state"
target_latest_log="$(target_log_path "$TARGET")"
target_fml_log="$(target_alt_log_path "$TARGET")"
if { [[ -f "$target_latest_log" ]] && grep -q 'Done (' "$target_latest_log"; } \
    || { [[ -f "$target_fml_log" ]] && grep -q 'Done (' "$target_fml_log"; }; then
    printf '%s reached Done\n' "$TARGET"
else
    printf '%s has not reached Done in latest logs\n' "$TARGET"
fi

print_section "runtime catalog diagnostics"
diagnostic_lines="$(server_logs 2>/dev/null | grep -F '[market-smoke]' | tail -20 || true)"
if [[ -n "$diagnostic_lines" ]]; then
    printf '%s\n' "$diagnostic_lines"
else
    printf 'No runtime catalog diagnostic lines found. Deploy the latest jar and restart %s, then rerun.\n' "$TARGET"
fi

if [[ "$catalog_count" -le 0 ]]; then
    printf '\nFAIL: database has no enabled formal standardized catalog product.\n' >&2
    exit 2
fi

if [[ "$technical_display_count" -gt 0 ]]; then
    printf '\nFAIL: enabled standardized catalogue still contains technical product keys as display names.\n' >&2
    exit 6
fi

if [[ -z "$diagnostic_lines" ]]; then
    printf '\nFAIL: database has product keys, but no live runtime catalog smoke result was observed.\n' >&2
    exit 4
fi

if printf '%s\n' "$diagnostic_lines" | grep -Eq 'runtime catalog admitted none|startup smoke rejected all'; then
    printf '\nFAIL: database has product keys, but the live runtime catalog rejected all candidates.\n' >&2
    exit 3
fi

if ! printf '%s\n' "$diagnostic_lines" | grep -Eq 'startup smoke admitted|terminal .* admitted'; then
    printf '\nFAIL: runtime smoke logs exist, but no catalog admission line was observed.\n' >&2
    exit 5
fi

printf '\nPASS: database market data is present and the live runtime catalog admitted at least one product key.\n'
