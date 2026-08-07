BEGIN;

CREATE TABLE IF NOT EXISTS standardized_market_catalog (
    product_key VARCHAR(128) PRIMARY KEY,
    registry_name VARCHAR(128) NOT NULL,
    meta INT NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    unit_label VARCHAR(64) NOT NULL DEFAULT '标准单位',
    stackable BOOLEAN NOT NULL DEFAULT TRUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 1000,
    catalog_version VARCHAR(64) NOT NULL DEFAULT 'standardized-market-catalog-db-v1',
    category_code VARCHAR(64) NOT NULL DEFAULT 'standardized',
    admission_basis VARCHAR(255) NOT NULL DEFAULT '管理员目录准入',
    source_entry_label VARCHAR(255) NOT NULL DEFAULT '管理员维护',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_standardized_market_catalog_meta_nonnegative CHECK (meta >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_standardized_market_catalog_item
    ON standardized_market_catalog (registry_name, meta);
CREATE INDEX IF NOT EXISTS idx_standardized_market_catalog_browse
    ON standardized_market_catalog (enabled, sort_order, display_name, product_key);

-- Preserve the formal availability of existing live market assets when upgrading a populated installation.
INSERT INTO standardized_market_catalog (
    product_key, registry_name, meta, display_name, unit_label, stackable, enabled, sort_order,
    catalog_version, category_code, admission_basis, source_entry_label)
SELECT product_key, registry_name, meta, product_key, '标准单位', bool_or(stackable), TRUE, 1000,
       'standardized-market-catalog-db-v1', 'legacy-market-data',
       '由既有标准市场记录迁移，待管理员复核', 'migration: market_order/custody/trade'
FROM (
    SELECT product_key, registry_name, meta, stackable FROM market_order
    UNION ALL
    SELECT product_key, registry_name, meta, stackable FROM market_custody_inventory
    UNION ALL
    SELECT product_key, registry_name, meta, stackable FROM market_trade_record
) legacy_product
GROUP BY product_key, registry_name, meta
ON CONFLICT (product_key) DO NOTHING;

COMMIT;
