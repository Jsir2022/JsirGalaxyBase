#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/deploy-common.sh
source "$SCRIPT_DIR/lib/deploy-common.sh"

DB_CONTAINER="${DB_CONTAINER:-galaxy-base}"
DB_USER="${DB_USER:-jsirgalaxybase_app}"
DB_NAME="${DB_NAME:-jsirgalaxybase}"
APPLY=false

usage() {
    cat <<'EOF'
Usage: scripts/market-demo-fixture.sh --apply

Creates explicitly managed, non-player test liquidity for every enabled formal
standardized-market catalog product. It first quarantines legacy
`demo_market_seed` orders that do not have the required escrow or bank-account
state, then creates escrow-backed sell orders, frozen-funds-backed buy orders,
and recent completed trades for every formal product.

The fixture is idempotent by product and fixture version. It does not modify
player accounts, player custody, or player orders.
Do not run this against a production economy without an administrator's
explicit decision to retain test liquidity.
EOF
}

while (( $# > 0 )); do
    case "$1" in
        --apply) APPLY=true ;;
        -h|--help) usage; exit 0 ;;
        *) fail "unknown argument: $1" ;;
    esac
    shift
done

[[ "$APPLY" == true ]] || { usage >&2; exit 2; }
require_command docker
docker ps --format '{{.Names}}' | grep -qx "$DB_CONTAINER" || fail "database container is not running: $DB_CONTAINER"

cat <<'SQL' | docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -X -v ON_ERROR_STOP=1
BEGIN;

-- Older visual-only demo orders never reserved custody or created maker bank
-- accounts. Preserve them as history, but never expose them to matching.
UPDATE market_order
SET order_status = 'EXCEPTION', open_quantity = 0, reserved_funds = 0, updated_at = now()
WHERE source_server_id = 'demo_market_seed'
  AND order_status IN ('OPEN', 'PARTIALLY_FILLED');

DO $$
DECLARE
    fixture_account_id BIGINT;
    sell_custody_id BIGINT;
    sell_order_id BIGINT;
    catalog_record RECORD;
    unit_quantity BIGINT;
    bid_price BIGINT;
    ask_price BIGINT;
    trade_price BIGINT;
    total_reserved BIGINT;
BEGIN
    INSERT INTO bank_account (
        account_no, account_type, owner_type, owner_ref, currency_code,
        available_balance, frozen_balance, status, version, display_name, metadata_json)
    VALUES (
        'MARKET-DEMO-MAKER-STC', 'PLAYER', 'PLAYER_UUID', 'MARKET_DEMO_MAKER', 'STARCOIN',
        1000000, 0, 'ACTIVE', 0, '受管市场测试做市账户',
        '{"fixture":"market-demo-fixture-v2","purpose":"standardized-market-visual-smoke"}'::jsonb)
    ON CONFLICT (owner_type, owner_ref, currency_code) DO UPDATE
        SET available_balance = EXCLUDED.available_balance,
            frozen_balance = EXCLUDED.frozen_balance,
            status = 'ACTIVE',
            metadata_json = EXCLUDED.metadata_json,
            version = bank_account.version + 1
    RETURNING account_id INTO fixture_account_id;

    FOR catalog_record IN
        SELECT product_key, registry_name, meta, stackable, reference_price
        FROM standardized_market_catalog
        WHERE enabled = TRUE
        ORDER BY sort_order, product_key
    LOOP
        unit_quantity := CASE WHEN catalog_record.stackable THEN 64 ELSE 1 END;
        bid_price := GREATEST(1, catalog_record.reference_price - GREATEST(1, catalog_record.reference_price / 50));
        ask_price := catalog_record.reference_price + GREATEST(1, catalog_record.reference_price / 50);

        IF NOT EXISTS (
            SELECT 1 FROM market_order
            WHERE source_server_id = 'market-demo-fixture-v2'
              AND product_key = catalog_record.product_key
              AND order_status IN ('OPEN', 'PARTIALLY_FILLED')
        ) THEN
            INSERT INTO market_custody_inventory (
                owner_player_ref, product_key, registry_name, meta, stackable, quantity,
                custody_status, related_order_id, related_operation_id, source_server_id)
            VALUES (
                'MARKET_DEMO_MAKER', catalog_record.product_key, catalog_record.registry_name,
                catalog_record.meta, catalog_record.stackable, unit_quantity,
                'ESCROW_SELL', 0, 0, 'market-demo-fixture-v2')
            RETURNING custody_id INTO sell_custody_id;

            INSERT INTO market_order (
                order_side, order_status, owner_player_ref, product_key, registry_name, meta, stackable,
                unit_price, original_quantity, open_quantity, filled_quantity, reserved_funds, custody_id, source_server_id)
            VALUES (
                'SELL', 'OPEN', 'MARKET_DEMO_MAKER', catalog_record.product_key, catalog_record.registry_name,
                catalog_record.meta, catalog_record.stackable, ask_price, unit_quantity, unit_quantity, 0, 0,
                sell_custody_id, 'market-demo-fixture-v2')
            RETURNING order_id INTO sell_order_id;

            UPDATE market_custody_inventory
            SET related_order_id = sell_order_id, updated_at = now()
            WHERE custody_id = sell_custody_id;

            INSERT INTO market_order (
                order_side, order_status, owner_player_ref, product_key, registry_name, meta, stackable,
                unit_price, original_quantity, open_quantity, filled_quantity, reserved_funds, custody_id, source_server_id)
            VALUES (
                'BUY', 'OPEN', 'MARKET_DEMO_MAKER', catalog_record.product_key, catalog_record.registry_name,
                catalog_record.meta, catalog_record.stackable, bid_price, unit_quantity, unit_quantity, 0,
                bid_price * unit_quantity, 0, 'market-demo-fixture-v2');
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM market_trade_record
            WHERE seller_player_ref = 'MARKET_DEMO_SELLER_V2'
              AND product_key = catalog_record.product_key
              AND created_at >= now() - interval '24 hours'
        ) THEN
            FOR trade_price IN
                SELECT GREATEST(1, catalog_record.reference_price
                    + ((series_index % 5) - 2) * GREATEST(1, catalog_record.reference_price / 50))
                FROM generate_series(1, 10) AS series_index
            LOOP
                INSERT INTO market_trade_record (
                    buyer_player_ref, seller_player_ref, product_key, registry_name, meta, stackable,
                    unit_price, quantity, fee_amount, buy_order_id, sell_order_id, operation_id, created_at)
                VALUES (
                    'MARKET_DEMO_BUYER_V2', 'MARKET_DEMO_SELLER_V2', catalog_record.product_key,
                    catalog_record.registry_name, catalog_record.meta, catalog_record.stackable,
                    trade_price, GREATEST(1, unit_quantity / 2), 0, 0, 0, 0,
                    now() - ((10 - trade_price % 10) * interval '45 minutes'));
            END LOOP;
        END IF;
    END LOOP;

    SELECT COALESCE(sum(reserved_funds), 0) INTO total_reserved
    FROM market_order
    WHERE owner_player_ref = 'MARKET_DEMO_MAKER'
      AND order_side = 'BUY'
      AND order_status IN ('OPEN', 'PARTIALLY_FILLED');
    UPDATE bank_account
    SET frozen_balance = total_reserved,
        available_balance = GREATEST(0, 1000000 - total_reserved),
        version = version + 1,
        updated_at = now()
    WHERE account_id = fixture_account_id;

    RAISE NOTICE 'created or verified market-demo-fixture-v2 account=% reserved=%',
        fixture_account_id, total_reserved;
END $$;

COMMIT;

SELECT order_id, order_side, order_status, unit_price, open_quantity, reserved_funds, custody_id
FROM market_order
WHERE source_server_id IN ('market-demo-fixture-v1', 'market-demo-fixture-v2')
ORDER BY order_id;

SELECT custody_id, product_key, quantity, custody_status, related_order_id
FROM market_custody_inventory
WHERE source_server_id IN ('market-demo-fixture-v1', 'market-demo-fixture-v2')
ORDER BY custody_id;

SELECT owner_ref, available_balance, frozen_balance, status
FROM bank_account
WHERE owner_ref = 'MARKET_DEMO_MAKER';
SQL

"$SCRIPT_DIR/market-audit.sh" --strict
