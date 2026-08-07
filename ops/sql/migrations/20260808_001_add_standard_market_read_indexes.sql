BEGIN;

CREATE INDEX IF NOT EXISTS idx_market_trade_product_created
    ON market_trade_record (product_key, created_at DESC, trade_id DESC);

CREATE INDEX IF NOT EXISTS idx_market_order_product_status_side_price
    ON market_order (product_key, order_status, order_side, unit_price, created_at, order_id);

COMMIT;
