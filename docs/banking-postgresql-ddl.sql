-- JsirGalaxyBase 银行系统一期 PostgreSQL DDL 草案
-- 日期：2026-03-30
--
-- 说明：
-- 1. 这份 SQL 对应 docs/banking-schema-design.md 中的一期核心表设计。
-- 2. 当前定位是“结构草案”，用于先固定字段、约束、索引和审计边界。
-- 3. 真正上线前，建议再补 migration 编号、权限策略和初始化账户脚本。

BEGIN;

CREATE OR REPLACE FUNCTION touch_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

CREATE TABLE IF NOT EXISTS bank_account (
    account_id BIGSERIAL PRIMARY KEY,
    account_no VARCHAR(32) NOT NULL,
    account_type VARCHAR(24) NOT NULL,
    owner_type VARCHAR(24) NOT NULL,
    owner_ref VARCHAR(64) NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    available_balance BIGINT NOT NULL DEFAULT 0,
    frozen_balance BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    display_name VARCHAR(128) NOT NULL,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_bank_account_account_no UNIQUE (account_no),
    CONSTRAINT uk_bank_account_owner_currency UNIQUE (owner_type, owner_ref, currency_code),
    CONSTRAINT ck_bank_account_available_balance_nonnegative CHECK (available_balance >= 0),
    CONSTRAINT ck_bank_account_frozen_balance_nonnegative CHECK (frozen_balance >= 0),
    CONSTRAINT ck_bank_account_version_nonnegative CHECK (version >= 0),
    CONSTRAINT ck_bank_account_currency_not_blank CHECK (btrim(currency_code) <> ''),
    CONSTRAINT ck_bank_account_type_not_blank CHECK (btrim(account_type) <> ''),
    CONSTRAINT ck_bank_account_status_not_blank CHECK (btrim(status) <> ''),
    CONSTRAINT ck_bank_account_owner_type_not_blank CHECK (btrim(owner_type) <> ''),
    CONSTRAINT ck_bank_account_owner_ref_not_blank CHECK (btrim(owner_ref) <> ''),
    CONSTRAINT ck_bank_account_display_name_not_blank CHECK (btrim(display_name) <> '')
);

CREATE INDEX IF NOT EXISTS idx_bank_account_owner
    ON bank_account (owner_type, owner_ref);

CREATE INDEX IF NOT EXISTS idx_bank_account_type
    ON bank_account (account_type);

CREATE INDEX IF NOT EXISTS idx_bank_account_status
    ON bank_account (status);

DROP TRIGGER IF EXISTS trg_touch_bank_account_updated_at ON bank_account;

CREATE TRIGGER trg_touch_bank_account_updated_at
BEFORE UPDATE ON bank_account
FOR EACH ROW
EXECUTE FUNCTION touch_updated_at();

CREATE TABLE IF NOT EXISTS bank_transaction (
    transaction_id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    business_ref VARCHAR(64),
    source_server_id VARCHAR(64) NOT NULL,
    operator_type VARCHAR(24) NOT NULL,
    operator_ref VARCHAR(64),
    player_ref VARCHAR(64),
    comment VARCHAR(255),
    extra_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_bank_transaction_request_id UNIQUE (request_id),
    CONSTRAINT ck_bank_transaction_request_id_not_blank CHECK (btrim(request_id) <> ''),
    CONSTRAINT ck_bank_transaction_type_not_blank CHECK (btrim(transaction_type) <> ''),
    CONSTRAINT ck_bank_transaction_business_type_not_blank CHECK (btrim(business_type) <> ''),
    CONSTRAINT ck_bank_transaction_source_server_not_blank CHECK (btrim(source_server_id) <> ''),
    CONSTRAINT ck_bank_transaction_operator_type_not_blank CHECK (btrim(operator_type) <> '')
);

CREATE INDEX IF NOT EXISTS idx_bank_transaction_business
    ON bank_transaction (business_type, business_ref);

CREATE INDEX IF NOT EXISTS idx_bank_transaction_player
    ON bank_transaction (player_ref);

CREATE INDEX IF NOT EXISTS idx_bank_transaction_created_at
    ON bank_transaction (created_at DESC);

CREATE TABLE IF NOT EXISTS ledger_entry (
    entry_id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    entry_side VARCHAR(8) NOT NULL,
    amount BIGINT NOT NULL,
    balance_before BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    frozen_balance_before BIGINT NOT NULL DEFAULT 0,
    frozen_balance_after BIGINT NOT NULL DEFAULT 0,
    currency_code VARCHAR(16) NOT NULL,
    sequence_in_tx SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_ledger_entry_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES bank_transaction (transaction_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_ledger_entry_account
        FOREIGN KEY (account_id)
        REFERENCES bank_account (account_id)
        ON DELETE RESTRICT,
    CONSTRAINT uk_ledger_entry_transaction_sequence UNIQUE (transaction_id, sequence_in_tx),
    CONSTRAINT ck_ledger_entry_side CHECK (entry_side IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_ledger_entry_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_ledger_entry_balance_before_nonnegative CHECK (balance_before >= 0),
    CONSTRAINT ck_ledger_entry_balance_after_nonnegative CHECK (balance_after >= 0),
    CONSTRAINT ck_ledger_entry_frozen_balance_before_nonnegative CHECK (frozen_balance_before >= 0),
    CONSTRAINT ck_ledger_entry_frozen_balance_after_nonnegative CHECK (frozen_balance_after >= 0),
    CONSTRAINT ck_ledger_entry_currency_not_blank CHECK (btrim(currency_code) <> ''),
    CONSTRAINT ck_ledger_entry_sequence_positive CHECK (sequence_in_tx > 0)
);

CREATE INDEX IF NOT EXISTS idx_ledger_entry_account_id
    ON ledger_entry (account_id, entry_id DESC);

CREATE INDEX IF NOT EXISTS idx_ledger_entry_transaction_id
    ON ledger_entry (transaction_id);

CREATE INDEX IF NOT EXISTS idx_ledger_entry_created_at
    ON ledger_entry (created_at DESC);

CREATE TABLE IF NOT EXISTS coin_exchange_record (
    exchange_id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    player_ref VARCHAR(64) NOT NULL,
    coin_family VARCHAR(32) NOT NULL,
    coin_tier VARCHAR(16) NOT NULL,
    coin_face_value BIGINT NOT NULL,
    coin_quantity BIGINT NOT NULL,
    effective_exchange_value BIGINT NOT NULL,
    contribution_value BIGINT NOT NULL DEFAULT 0,
    exchange_rule_version VARCHAR(32) NOT NULL,
    source_server_id VARCHAR(64) NOT NULL,
    extra_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_coin_exchange_transaction UNIQUE (transaction_id),
    CONSTRAINT fk_coin_exchange_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES bank_transaction (transaction_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_coin_exchange_player_not_blank CHECK (btrim(player_ref) <> ''),
    CONSTRAINT ck_coin_exchange_family_not_blank CHECK (btrim(coin_family) <> ''),
    CONSTRAINT ck_coin_exchange_tier_not_blank CHECK (btrim(coin_tier) <> ''),
    CONSTRAINT ck_coin_exchange_face_value_positive CHECK (coin_face_value > 0),
    CONSTRAINT ck_coin_exchange_quantity_positive CHECK (coin_quantity > 0),
    CONSTRAINT ck_coin_exchange_effective_value_nonnegative CHECK (effective_exchange_value >= 0),
    CONSTRAINT ck_coin_exchange_contribution_nonnegative CHECK (contribution_value >= 0),
    CONSTRAINT ck_coin_exchange_rule_version_not_blank CHECK (btrim(exchange_rule_version) <> ''),
    CONSTRAINT ck_coin_exchange_source_server_not_blank CHECK (btrim(source_server_id) <> '')
);

CREATE INDEX IF NOT EXISTS idx_coin_exchange_player_created_at
    ON coin_exchange_record (player_ref, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_coin_exchange_source_server
    ON coin_exchange_record (source_server_id);

CREATE INDEX IF NOT EXISTS idx_coin_exchange_rule_version
    ON coin_exchange_record (exchange_rule_version);

CREATE TABLE IF NOT EXISTS bank_daily_snapshot (
    snapshot_id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    available_balance BIGINT NOT NULL,
    frozen_balance BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_bank_daily_snapshot_account
        FOREIGN KEY (account_id)
        REFERENCES bank_account (account_id)
        ON DELETE RESTRICT,
    CONSTRAINT uk_bank_daily_snapshot_account_date UNIQUE (account_id, snapshot_date),
    CONSTRAINT ck_bank_daily_snapshot_available_nonnegative CHECK (available_balance >= 0),
    CONSTRAINT ck_bank_daily_snapshot_frozen_nonnegative CHECK (frozen_balance >= 0),
    CONSTRAINT ck_bank_daily_snapshot_currency_not_blank CHECK (btrim(currency_code) <> '')
);

CREATE INDEX IF NOT EXISTS idx_bank_daily_snapshot_date
    ON bank_daily_snapshot (snapshot_date DESC);

COMMIT;