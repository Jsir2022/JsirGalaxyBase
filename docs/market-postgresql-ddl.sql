-- JsirGalaxyBase 市场系统第二阶段 PostgreSQL DDL 骨架
-- 日期：2026-04-01
--
-- 说明：
-- 1. 这份 DDL 现在同时覆盖“标准化现货市场最小骨架”和“定制商品市场最小挂牌链 v1”。
-- 2. 标准商品市场继续保留订单簿 / 托管 / 操作日志 / 成交记录四张主表。
-- 3. 定制商品市场使用自己独立的挂牌、快照、成交、审计表，不复用标准商品订单簿主表。
-- 4. 当前 v1 明确只支持单件手持挂牌，购买后必须通过 claim 动作把交付状态完结到 COMPLETED。

BEGIN;

CREATE TABLE IF NOT EXISTS market_order (
    order_id BIGSERIAL PRIMARY KEY,
    order_side VARCHAR(8) NOT NULL,
    order_status VARCHAR(24) NOT NULL,
    owner_player_ref VARCHAR(64) NOT NULL,
    product_key VARCHAR(128) NOT NULL,
    registry_name VARCHAR(128) NOT NULL,
    meta INT NOT NULL,
    stackable BOOLEAN NOT NULL,
    unit_price BIGINT NOT NULL,
    original_quantity BIGINT NOT NULL,
    open_quantity BIGINT NOT NULL,
    filled_quantity BIGINT NOT NULL DEFAULT 0,
    reserved_funds BIGINT NOT NULL DEFAULT 0,
    custody_id BIGINT,
    source_server_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_market_order_side CHECK (order_side IN ('BUY', 'SELL')),
    CONSTRAINT ck_market_order_status CHECK (order_status IN ('OPEN', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'EXCEPTION')),
    CONSTRAINT ck_market_order_unit_price_positive CHECK (unit_price > 0),
    CONSTRAINT ck_market_order_original_quantity_positive CHECK (original_quantity > 0),
    CONSTRAINT ck_market_order_open_quantity_nonnegative CHECK (open_quantity >= 0),
    CONSTRAINT ck_market_order_filled_quantity_nonnegative CHECK (filled_quantity >= 0),
    CONSTRAINT ck_market_order_reserved_funds_nonnegative CHECK (reserved_funds >= 0),
    CONSTRAINT ck_market_order_meta_nonnegative CHECK (meta >= 0)
);

CREATE INDEX IF NOT EXISTS idx_market_order_book_lookup
    ON market_order (product_key, order_side, order_status, unit_price, created_at);

CREATE INDEX IF NOT EXISTS idx_market_order_owner
    ON market_order (owner_player_ref, created_at DESC);

CREATE TABLE IF NOT EXISTS market_custody_inventory (
    custody_id BIGSERIAL PRIMARY KEY,
    owner_player_ref VARCHAR(64) NOT NULL,
    product_key VARCHAR(128) NOT NULL,
    registry_name VARCHAR(128) NOT NULL,
    meta INT NOT NULL,
    stackable BOOLEAN NOT NULL,
    quantity BIGINT NOT NULL,
    custody_status VARCHAR(24) NOT NULL,
    related_order_id BIGINT,
    related_operation_id BIGINT,
    source_server_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_market_custody_status CHECK (custody_status IN ('AVAILABLE', 'ESCROW_SELL', 'SETTLED', 'CLAIMABLE', 'CLAIMING', 'CLAIMED', 'EXCEPTION')),
    CONSTRAINT ck_market_custody_quantity_nonnegative CHECK (quantity >= 0),
    CONSTRAINT ck_market_custody_meta_nonnegative CHECK (meta >= 0)
);

CREATE INDEX IF NOT EXISTS idx_market_custody_owner
    ON market_custody_inventory (owner_player_ref, custody_status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_market_custody_order
    ON market_custody_inventory (related_order_id);

CREATE TABLE IF NOT EXISTS market_operation_log (
    operation_id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    operation_status VARCHAR(24) NOT NULL,
    source_server_id VARCHAR(64) NOT NULL,
    player_ref VARCHAR(64) NOT NULL,
    request_semantics_key TEXT NOT NULL,
    recovery_metadata_key TEXT,
    related_order_id BIGINT,
    related_custody_id BIGINT,
    related_trade_id BIGINT,
    message VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_market_operation_request UNIQUE (request_id),
    CONSTRAINT ck_market_operation_type CHECK (operation_type IN ('INVENTORY_DEPOSIT', 'SELL_ORDER_CREATE', 'SELL_ORDER_CANCEL', 'BUY_ORDER_CREATE', 'BUY_ORDER_CANCEL', 'MATCH_EXECUTION', 'CLAIMABLE_ASSET_CLAIM')),
    CONSTRAINT ck_market_operation_status CHECK (operation_status IN ('CREATED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RECOVERY_REQUIRED'))
);

CREATE INDEX IF NOT EXISTS idx_market_operation_related_order
    ON market_operation_log (related_order_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_market_operation_status
    ON market_operation_log (operation_status, created_at DESC);

CREATE TABLE IF NOT EXISTS market_trade_record (
    trade_id BIGSERIAL PRIMARY KEY,
    buyer_player_ref VARCHAR(64) NOT NULL,
    seller_player_ref VARCHAR(64) NOT NULL,
    product_key VARCHAR(128) NOT NULL,
    registry_name VARCHAR(128) NOT NULL,
    meta INT NOT NULL,
    stackable BOOLEAN NOT NULL,
    unit_price BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    fee_amount BIGINT NOT NULL DEFAULT 0,
    buy_order_id BIGINT,
    sell_order_id BIGINT,
    operation_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_market_trade_unit_price_positive CHECK (unit_price > 0),
    CONSTRAINT ck_market_trade_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_market_trade_fee_amount_nonnegative CHECK (fee_amount >= 0),
    CONSTRAINT ck_market_trade_meta_nonnegative CHECK (meta >= 0)
);

CREATE INDEX IF NOT EXISTS idx_market_trade_sell_order
    ON market_trade_record (sell_order_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_market_trade_buy_order
    ON market_trade_record (buy_order_id, created_at DESC);

CREATE TABLE IF NOT EXISTS custom_market_listing (
    listing_id BIGSERIAL PRIMARY KEY,
    seller_player_ref VARCHAR(64) NOT NULL,
    buyer_player_ref VARCHAR(64),
    asking_price BIGINT NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    listing_status VARCHAR(24) NOT NULL,
    delivery_status VARCHAR(32) NOT NULL,
    source_server_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_custom_market_listing_price_positive CHECK (asking_price > 0),
    CONSTRAINT ck_custom_market_listing_status CHECK (listing_status IN ('ACTIVE', 'SOLD', 'CANCELLED', 'EXCEPTION')),
    CONSTRAINT ck_custom_market_delivery_status CHECK (delivery_status IN ('ESCROW_HELD', 'BUYER_PENDING_CLAIM', 'COMPLETED', 'CANCELLED', 'EXCEPTION'))
);

CREATE INDEX IF NOT EXISTS idx_custom_market_listing_status
    ON custom_market_listing (listing_status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_custom_market_listing_seller_delivery
    ON custom_market_listing (seller_player_ref, delivery_status, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_custom_market_listing_buyer_delivery
    ON custom_market_listing (buyer_player_ref, delivery_status, updated_at DESC);

CREATE TABLE IF NOT EXISTS custom_market_item_snapshot (
    snapshot_id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    item_id VARCHAR(128) NOT NULL,
    meta INT NOT NULL,
    stack_size INT NOT NULL,
    stackable BOOLEAN NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    nbt_snapshot TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_custom_market_item_snapshot_listing UNIQUE (listing_id),
    CONSTRAINT fk_custom_market_item_snapshot_listing FOREIGN KEY (listing_id)
        REFERENCES custom_market_listing (listing_id) ON DELETE CASCADE,
    CONSTRAINT ck_custom_market_item_snapshot_meta_nonnegative CHECK (meta >= 0),
    CONSTRAINT ck_custom_market_item_snapshot_single_item CHECK (stack_size = 1)
);

CREATE INDEX IF NOT EXISTS idx_custom_market_item_snapshot_item
    ON custom_market_item_snapshot (item_id, meta);

CREATE TABLE IF NOT EXISTS custom_market_trade_record (
    trade_id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    seller_player_ref VARCHAR(64) NOT NULL,
    buyer_player_ref VARCHAR(64) NOT NULL,
    settled_amount BIGINT NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    delivery_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_custom_market_trade_listing FOREIGN KEY (listing_id)
        REFERENCES custom_market_listing (listing_id) ON DELETE CASCADE,
    CONSTRAINT ck_custom_market_trade_amount_positive CHECK (settled_amount > 0),
    CONSTRAINT ck_custom_market_trade_delivery_status CHECK (delivery_status IN ('ESCROW_HELD', 'BUYER_PENDING_CLAIM', 'COMPLETED', 'CANCELLED', 'EXCEPTION'))
);

CREATE INDEX IF NOT EXISTS idx_custom_market_trade_listing
    ON custom_market_trade_record (listing_id, created_at DESC);

CREATE TABLE IF NOT EXISTS custom_market_audit_log (
    audit_id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL,
    audit_type VARCHAR(32) NOT NULL,
    player_ref VARCHAR(64) NOT NULL,
    request_semantics_key TEXT NOT NULL,
    listing_id BIGINT,
    trade_id BIGINT,
    source_server_id VARCHAR(64) NOT NULL,
    message VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_custom_market_audit_request UNIQUE (request_id),
    CONSTRAINT fk_custom_market_audit_listing FOREIGN KEY (listing_id)
        REFERENCES custom_market_listing (listing_id) ON DELETE SET NULL,
    CONSTRAINT fk_custom_market_audit_trade FOREIGN KEY (trade_id)
        REFERENCES custom_market_trade_record (trade_id) ON DELETE SET NULL,
    CONSTRAINT ck_custom_market_audit_type CHECK (audit_type IN ('LISTING_PUBLISH', 'LISTING_PURCHASE', 'LISTING_CLAIM', 'LISTING_CANCEL'))
);

CREATE INDEX IF NOT EXISTS idx_custom_market_audit_listing
    ON custom_market_audit_log (listing_id, created_at DESC);

COMMIT;