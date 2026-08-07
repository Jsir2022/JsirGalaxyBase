-- Base Vault Phase 0: finite, account-bound storage. Do not replace this with market custody.

CREATE TABLE IF NOT EXISTS warehouse_account (
    account_id BIGSERIAL PRIMARY KEY,
    account_type VARCHAR(32) NOT NULL,
    account_ref VARCHAR(128) NOT NULL,
    base_slot_count INTEGER NOT NULL CHECK (base_slot_count > 0),
    vault_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT warehouse_account_owner_unique UNIQUE (account_type, account_ref)
);

CREATE TABLE IF NOT EXISTS warehouse_slot (
    account_id BIGINT NOT NULL REFERENCES warehouse_account(account_id) ON DELETE CASCADE,
    slot_index INTEGER NOT NULL CHECK (slot_index >= 0),
    stack_nbt TEXT NULL,
    item_id VARCHAR(255) NULL,
    item_meta INTEGER NULL,
    stack_size INTEGER NULL,
    display_name VARCHAR(512) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_id, slot_index)
);

CREATE TABLE IF NOT EXISTS warehouse_operation_log (
    operation_id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(160) NOT NULL UNIQUE,
    account_id BIGINT NOT NULL REFERENCES warehouse_account(account_id),
    operation_type VARCHAR(64) NOT NULL,
    source_domain VARCHAR(64) NOT NULL,
    target_domain VARCHAR(64) NOT NULL,
    item_snapshot TEXT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    operation_status VARCHAR(32) NOT NULL,
    message TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS warehouse_operation_log_account_status_idx
    ON warehouse_operation_log (account_id, operation_status, updated_at DESC);
