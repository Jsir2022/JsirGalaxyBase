#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/deploy-common.sh
source "$SCRIPT_DIR/lib/deploy-common.sh"

DB_CONTAINER="${DB_CONTAINER:-galaxy-base}"
DB_USER="${DB_USER:-jsirgalaxybase_app}"
DB_NAME="${DB_NAME:-jsirgalaxybase}"
STRICT=false
PLAYER_REF=""

usage() {
    cat <<'EOF'
Usage: scripts/market-audit.sh [--strict] [--player <player-ref>]

Read-only operational audit for the standardized and custom markets. It reports:
  - non-formal enabled catalogue rows;
  - player-owned open orders without their expected custody/frozen-fund state;
  - legacy managed demo liquidity separately from player asset anomalies;
  - stale CLAIMING custody and exception custody;
  - incomplete or recovery-required market operations, while reporting safely
    failed preflight attempts separately as history.
  - custom listings without their escrow item snapshot;
  - custom purchase/claim state that is missing its matching trade record;
  - invalid custom listing/trade delivery-state combinations;
  - custom pending claims that require an operator or player follow-up.
  - custom delivery operations that are still processing or have an uncertain result.
  - exchange records whose transaction audit metadata is not marked as exchange.
  - interrupted exchange input operations that require a physical inventory reconciliation.
  - incomplete Base Vault operations, including their recorded slot-change count.
  - Base Vault accounts whose configured capacity differs from the account-type contract.
  - Base Vault slots whose index falls outside their owning account capacity.

--strict exits non-zero when any anomaly is found. --player narrows custody and
operation reports to one player UUID/reference without changing global checks.
EOF
}

psql_value() {
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -X -At -v ON_ERROR_STOP=1 -c "$1"
}

psql_table() {
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -X -v ON_ERROR_STOP=1 -c "$1"
}

require_command docker

while (( $# > 0 )); do
    case "$1" in
        --strict)
            STRICT=true
            ;;
        --player)
            shift
            [[ $# -gt 0 ]] || { echo "--player requires a value" >&2; exit 2; }
            PLAYER_REF="$1"
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
    shift
done

docker ps --format '{{.Names}}' | grep -qx "$DB_CONTAINER" || {
    echo "Database container is not running: $DB_CONTAINER" >&2
    exit 2
}

player_filter=""
if [[ -n "$PLAYER_REF" ]]; then
    escaped_player="${PLAYER_REF//\'/\'\'}"
    player_filter=" AND owner_player_ref = '$escaped_player'"
fi

echo "== standardized market audit =="
echo "database=$DB_NAME player=${PLAYER_REF:-all} strict=$STRICT"

formal_count="$(psql_value "SELECT count(*) FROM standardized_market_catalog WHERE enabled = TRUE AND display_name = product_key;")"
sell_custody_gap="$(psql_value "SELECT count(*) FROM market_order o WHERE o.order_status = 'OPEN' AND o.order_side = 'SELL' AND o.owner_player_ref NOT LIKE 'demo-market-%' AND NOT EXISTS (SELECT 1 FROM market_custody_inventory c WHERE c.related_order_id = o.order_id AND c.custody_status = 'ESCROW_SELL' AND c.quantity >= o.open_quantity);")"
demo_sell_custody_gap="$(psql_value "SELECT count(*) FROM market_order o WHERE o.order_status = 'OPEN' AND o.order_side = 'SELL' AND o.owner_player_ref LIKE 'demo-market-%' AND NOT EXISTS (SELECT 1 FROM market_custody_inventory c WHERE c.related_order_id = o.order_id AND c.custody_status = 'ESCROW_SELL' AND c.quantity >= o.open_quantity);")"
buy_funds_gap="$(psql_value "SELECT count(*) FROM market_order o WHERE o.order_status IN ('OPEN', 'PARTIALLY_FILLED') AND o.order_side = 'BUY' AND o.open_quantity > 0 AND o.reserved_funds < (o.unit_price * o.open_quantity + (o.unit_price * o.open_quantity * 80 / 10000));")"
bank_buy_funds_gap="$(psql_value "SELECT count(*) FROM (SELECT o.owner_player_ref, sum(o.reserved_funds) AS order_reserved FROM market_order o WHERE o.order_status IN ('OPEN', 'PARTIALLY_FILLED') AND o.order_side = 'BUY' AND o.open_quantity > 0 GROUP BY o.owner_player_ref) r LEFT JOIN bank_account a ON a.owner_type = 'PLAYER_UUID' AND a.owner_ref = r.owner_player_ref AND a.currency_code = 'STARCOIN' WHERE a.account_id IS NULL OR a.frozen_balance < r.order_reserved;")"
stale_claiming="$(psql_value "SELECT count(*) FROM market_custody_inventory WHERE custody_status = 'CLAIMING' AND updated_at < now() - interval '10 minutes'$player_filter;")"
exception_custody="$(psql_value "SELECT count(*) FROM market_custody_inventory WHERE custody_status = 'EXCEPTION'$player_filter;")"
pending_operations="$(psql_value "SELECT count(*) FROM market_operation_log WHERE operation_status IN ('CREATED', 'PROCESSING', 'RECOVERY_REQUIRED')${PLAYER_REF:+ AND player_ref = '$escaped_player'};")"
failed_operations="$(psql_value "SELECT count(*) FROM market_operation_log WHERE operation_status = 'FAILED'${PLAYER_REF:+ AND player_ref = '$escaped_player'};")"

printf 'enabled_catalog_technical_display_names=%s\n' "$formal_count"
printf 'open_sell_orders_without_escrow=%s\n' "$sell_custody_gap"
printf 'legacy_demo_sell_orders_without_escrow=%s\n' "$demo_sell_custody_gap"
printf 'active_buy_orders_under_reserved=%s\n' "$buy_funds_gap"
printf 'buy_order_owners_without_bank_frozen_coverage=%s\n' "$bank_buy_funds_gap"
printf 'stale_claiming_custody=%s\n' "$stale_claiming"
printf 'exception_custody=%s\n' "$exception_custody"
printf 'incomplete_market_operations=%s\n' "$pending_operations"
printf 'failed_market_operations_history=%s\n' "$failed_operations"

echo
echo "== actionable records =="
psql_table "
SELECT order_id, owner_player_ref, product_key, unit_price, open_quantity, reserved_funds,
       unit_price * open_quantity + (unit_price * open_quantity * 80 / 10000) AS required_reserve,
       source_server_id, updated_at
FROM market_order o
WHERE o.order_status IN ('OPEN', 'PARTIALLY_FILLED')
  AND o.order_side = 'BUY'
  AND o.open_quantity > 0
  AND o.reserved_funds < (o.unit_price * o.open_quantity
      + (o.unit_price * o.open_quantity * 80 / 10000))
ORDER BY o.order_id
LIMIT 30;
"
psql_table "
SELECT order_id, owner_player_ref, product_key, open_quantity, source_server_id, created_at
FROM market_order o
WHERE o.order_status = 'OPEN'
  AND o.order_side = 'SELL'
  AND o.owner_player_ref LIKE 'demo-market-%'
  AND NOT EXISTS (
      SELECT 1 FROM market_custody_inventory c
      WHERE c.related_order_id = o.order_id
        AND c.custody_status = 'ESCROW_SELL'
        AND c.quantity >= o.open_quantity
  )
ORDER BY o.order_id
LIMIT 30;
"
psql_table "
SELECT operation_id, operation_type, operation_status, player_ref, related_order_id, related_custody_id, message, updated_at
FROM market_operation_log
WHERE operation_status IN ('CREATED', 'PROCESSING', 'RECOVERY_REQUIRED')${PLAYER_REF:+ AND player_ref = '$escaped_player'}
ORDER BY updated_at DESC, operation_id DESC
LIMIT 30;
"
psql_table "
SELECT operation_id, operation_type, player_ref, message, updated_at
FROM market_operation_log
WHERE operation_status = 'FAILED'${PLAYER_REF:+ AND player_ref = '$escaped_player'}
ORDER BY updated_at DESC, operation_id DESC
LIMIT 10;
"
psql_table "
SELECT custody_id, owner_player_ref, product_key, quantity, custody_status, related_order_id, updated_at
FROM market_custody_inventory
WHERE custody_status IN ('CLAIMING', 'EXCEPTION')$player_filter
ORDER BY updated_at DESC, custody_id DESC
LIMIT 30;
"

echo
echo "== custom market audit =="
custom_snapshot_gap="$(psql_value "SELECT count(*) FROM custom_market_listing l WHERE NOT EXISTS (SELECT 1 FROM custom_market_item_snapshot s WHERE s.listing_id = l.listing_id);")"
custom_sold_trade_gap="$(psql_value "SELECT count(*) FROM custom_market_listing l WHERE l.listing_status = 'SOLD' AND NOT EXISTS (SELECT 1 FROM custom_market_trade_record t WHERE t.listing_id = l.listing_id);")"
custom_listing_state_gap="$(psql_value "SELECT count(*) FROM custom_market_listing l WHERE (l.listing_status = 'ACTIVE' AND l.delivery_status <> 'ESCROW_HELD') OR (l.listing_status = 'SOLD' AND l.delivery_status NOT IN ('BUYER_PENDING_CLAIM', 'COMPLETED', 'EXCEPTION')) OR (l.listing_status = 'CANCELLED' AND l.delivery_status NOT IN ('CANCELLED', 'EXCEPTION'));")"
custom_trade_state_gap="$(psql_value "SELECT count(*) FROM custom_market_trade_record t JOIN custom_market_listing l ON l.listing_id = t.listing_id WHERE (l.delivery_status = 'BUYER_PENDING_CLAIM' AND t.delivery_status <> 'BUYER_PENDING_CLAIM') OR (l.delivery_status = 'COMPLETED' AND t.delivery_status <> 'COMPLETED') OR (l.delivery_status = 'EXCEPTION' AND t.delivery_status <> 'EXCEPTION');")"
custom_exception_deliveries="$(psql_value "SELECT count(*) FROM custom_market_listing WHERE delivery_status = 'EXCEPTION';")"
custom_pending_claims="$(psql_value "SELECT count(*) FROM custom_market_listing WHERE listing_status = 'SOLD' AND delivery_status = 'BUYER_PENDING_CLAIM';")"
custom_delivery_processing="$(psql_value "SELECT count(*) FROM custom_market_audit_log WHERE audit_type = 'LISTING_DELIVERY' AND message LIKE 'DELIVERY_PROCESSING%';")"
custom_delivery_unknown="$(psql_value "SELECT count(*) FROM custom_market_audit_log WHERE audit_type = 'LISTING_DELIVERY' AND message LIKE 'DELIVERY_UNKNOWN%';")"

printf 'custom_listings_without_snapshot=%s\n' "$custom_snapshot_gap"
printf 'custom_sold_listings_without_trade=%s\n' "$custom_sold_trade_gap"
printf 'custom_listing_delivery_state_mismatches=%s\n' "$custom_listing_state_gap"
printf 'custom_trade_delivery_state_mismatches=%s\n' "$custom_trade_state_gap"
printf 'custom_exception_deliveries=%s\n' "$custom_exception_deliveries"
printf 'custom_pending_buyer_claims=%s\n' "$custom_pending_claims"
printf 'custom_delivery_operations_processing=%s\n' "$custom_delivery_processing"
printf 'custom_delivery_operations_unknown=%s\n' "$custom_delivery_unknown"

echo
echo "== custom market actionable records =="
psql_table "
SELECT l.listing_id, l.listing_status, l.delivery_status, l.seller_player_ref, l.buyer_player_ref,
       l.asking_price, l.currency_code, l.updated_at
FROM custom_market_listing l
WHERE l.delivery_status IN ('BUYER_PENDING_CLAIM', 'EXCEPTION')
ORDER BY l.updated_at ASC, l.listing_id ASC
LIMIT 30;
"
psql_table "
SELECT l.listing_id, l.listing_status, l.delivery_status, l.created_at
FROM custom_market_listing l
WHERE NOT EXISTS (
    SELECT 1 FROM custom_market_item_snapshot s WHERE s.listing_id = l.listing_id
)
   OR (l.listing_status = 'SOLD' AND NOT EXISTS (
       SELECT 1 FROM custom_market_trade_record t WHERE t.listing_id = l.listing_id
   ))
   OR (l.listing_status = 'ACTIVE' AND l.delivery_status <> 'ESCROW_HELD')
   OR (l.listing_status = 'SOLD' AND l.delivery_status NOT IN ('BUYER_PENDING_CLAIM', 'COMPLETED', 'EXCEPTION'))
   OR (l.listing_status = 'CANCELLED' AND l.delivery_status NOT IN ('CANCELLED', 'EXCEPTION'))
ORDER BY l.updated_at DESC, l.listing_id DESC
LIMIT 30;
"
psql_table "
SELECT audit_id, request_id, listing_id, trade_id, player_ref, message, updated_at
FROM custom_market_audit_log
WHERE audit_type = 'LISTING_DELIVERY'
  AND (message LIKE 'DELIVERY_PROCESSING%' OR message LIKE 'DELIVERY_UNKNOWN%')
ORDER BY updated_at ASC, audit_id ASC
LIMIT 30;
"

echo
echo "== exchange market audit =="
exchange_metadata_gap="$(psql_value "SELECT count(*) FROM coin_exchange_record e JOIN bank_transaction t ON t.transaction_id = e.transaction_id WHERE e.extra_json ->> 'marketType' IS DISTINCT FROM 'exchange' OR t.extra_json ->> 'marketType' IS DISTINCT FROM 'exchange';")"
exchange_input_recovery_required="$(psql_value "SELECT count(*) FROM market_operation_log WHERE operation_type = 'EXCHANGE_EXECUTION' AND operation_status = 'RECOVERY_REQUIRED';")"
printf 'exchange_records_without_formal_market_audit_metadata=%s\n' "$exchange_metadata_gap"
printf 'exchange_input_operations_requiring_reconciliation=%s\n' "$exchange_input_recovery_required"

echo
echo "== exchange market actionable records =="
psql_table "
SELECT e.exchange_id, e.transaction_id, e.player_ref, e.coin_family, e.coin_tier,
       e.effective_exchange_value, e.exchange_rule_version, e.created_at
FROM coin_exchange_record e
JOIN bank_transaction t ON t.transaction_id = e.transaction_id
WHERE e.extra_json ->> 'marketType' IS DISTINCT FROM 'exchange'
   OR t.extra_json ->> 'marketType' IS DISTINCT FROM 'exchange'
ORDER BY e.exchange_id DESC
LIMIT 30;
"
psql_table "
SELECT operation_id, request_id, player_ref, recovery_metadata_key, message, updated_at
FROM market_operation_log
WHERE operation_type = 'EXCHANGE_EXECUTION'
  AND operation_status = 'RECOVERY_REQUIRED'
ORDER BY updated_at ASC, operation_id ASC
LIMIT 30;
"

echo
echo "== Base Vault audit =="
vault_account_filter=""
if [[ -n "$PLAYER_REF" ]]; then
    vault_account_filter=" AND a.account_ref = '$escaped_player'"
fi
vault_incomplete_operations="$(psql_value "SELECT count(*) FROM warehouse_operation_log o JOIN warehouse_account a ON a.account_id = o.account_id WHERE o.operation_status IN ('CREATED', 'PROCESSING', 'RECOVERY_REQUIRED')$vault_account_filter;")"
vault_stale_processing="$(psql_value "SELECT count(*) FROM warehouse_operation_log o JOIN warehouse_account a ON a.account_id = o.account_id WHERE o.operation_status = 'PROCESSING' AND o.updated_at < now() - interval '10 minutes'$vault_account_filter;")"
vault_capacity_mismatches="$(psql_value "SELECT count(*) FROM warehouse_account a WHERE ((a.account_type = 'PERSONAL' AND a.base_slot_count <> 27) OR (a.account_type IN ('ENTERPRISE', 'PUBLIC') AND a.base_slot_count <> 54) OR a.account_type NOT IN ('PERSONAL', 'ENTERPRISE', 'PUBLIC'))$vault_account_filter;")"
vault_out_of_range_slots="$(psql_value "SELECT count(*) FROM warehouse_slot s JOIN warehouse_account a ON a.account_id = s.account_id WHERE (s.slot_index < 0 OR s.slot_index >= a.base_slot_count)$vault_account_filter;")"
printf 'incomplete_vault_operations=%s\n' "$vault_incomplete_operations"
printf 'stale_vault_processing_operations=%s\n' "$vault_stale_processing"
printf 'vault_account_capacity_mismatches=%s\n' "$vault_capacity_mismatches"
printf 'vault_slots_outside_account_capacity=%s\n' "$vault_out_of_range_slots"

echo
echo "== Base Vault actionable records =="
psql_table "
SELECT o.operation_id, o.request_id, a.account_type, a.account_ref, o.operation_type,
       o.source_domain, o.target_domain, o.quantity, o.operation_status,
       count(c.slot_index) AS recorded_slot_changes, o.message, o.updated_at
FROM warehouse_operation_log o
JOIN warehouse_account a ON a.account_id = o.account_id
LEFT JOIN warehouse_operation_slot_change c ON c.operation_id = o.operation_id
WHERE o.operation_status IN ('CREATED', 'PROCESSING', 'RECOVERY_REQUIRED')$vault_account_filter
GROUP BY o.operation_id, a.account_type, a.account_ref
ORDER BY o.updated_at ASC, o.operation_id ASC
LIMIT 30;
"
psql_table "
SELECT a.account_id, a.account_type, a.account_ref, a.base_slot_count, a.vault_status, a.updated_at
FROM warehouse_account a
WHERE ((a.account_type = 'PERSONAL' AND a.base_slot_count <> 27)
    OR (a.account_type IN ('ENTERPRISE', 'PUBLIC') AND a.base_slot_count <> 54)
    OR a.account_type NOT IN ('PERSONAL', 'ENTERPRISE', 'PUBLIC'))$vault_account_filter
ORDER BY a.account_id
LIMIT 30;
"
psql_table "
SELECT a.account_type, a.account_ref, a.base_slot_count, s.slot_index,
       s.item_id, s.stack_size, s.version, s.updated_at
FROM warehouse_slot s
JOIN warehouse_account a ON a.account_id = s.account_id
WHERE (s.slot_index < 0 OR s.slot_index >= a.base_slot_count)$vault_account_filter
ORDER BY a.account_id, s.slot_index
LIMIT 30;
"

anomalies=$((formal_count + sell_custody_gap + buy_funds_gap + bank_buy_funds_gap + stale_claiming + exception_custody + pending_operations + custom_snapshot_gap + custom_sold_trade_gap + custom_listing_state_gap + custom_trade_state_gap + custom_exception_deliveries + custom_delivery_processing + custom_delivery_unknown + exchange_metadata_gap + vault_incomplete_operations + vault_capacity_mismatches + vault_out_of_range_slots))
if [[ "$STRICT" == true && "$anomalies" -gt 0 ]]; then
    echo "FAIL: market audit found $anomalies anomaly count(s)." >&2
    exit 3
fi

echo "PASS: audit completed; anomaly_count=$anomalies"
