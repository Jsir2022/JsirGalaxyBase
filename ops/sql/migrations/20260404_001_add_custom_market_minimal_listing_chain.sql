BEGIN;

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
    CONSTRAINT ck_custom_market_item_snapshot_stack_positive CHECK (stack_size > 0)
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
    CONSTRAINT ck_custom_market_audit_type CHECK (audit_type IN ('LISTING_PUBLISH', 'LISTING_PURCHASE', 'LISTING_CANCEL'))
);

CREATE INDEX IF NOT EXISTS idx_custom_market_audit_listing
    ON custom_market_audit_log (listing_id, created_at DESC);

COMMIT;