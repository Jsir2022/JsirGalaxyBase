BEGIN;

ALTER TABLE standardized_market_catalog
    ADD COLUMN IF NOT EXISTS reference_price BIGINT NOT NULL DEFAULT 0;

ALTER TABLE standardized_market_catalog
    DROP CONSTRAINT IF EXISTS ck_standardized_market_catalog_reference_price_nonnegative;

ALTER TABLE standardized_market_catalog
    ADD CONSTRAINT ck_standardized_market_catalog_reference_price_nonnegative CHECK (reference_price >= 0);

-- Curated names and deterministic browse order for the catalogue migrated from the first live market.
-- Unknown GregTech metadata intentionally retains its product key until an administrator assigns a verified name.
UPDATE standardized_market_catalog
SET display_name = CASE product_key
        WHEN 'minecraft:iron_ingot:0' THEN 'Iron Ingot'
        WHEN 'minecraft:gold_ingot:0' THEN 'Gold Ingot'
        WHEN 'gregtech:gt.metaitem.01:11305' THEN 'Steel Ingot'
        ELSE display_name
    END,
    unit_label = CASE product_key
        WHEN 'minecraft:iron_ingot:0' THEN 'ingot'
        WHEN 'minecraft:gold_ingot:0' THEN 'ingot'
        WHEN 'gregtech:gt.metaitem.01:11305' THEN 'ingot'
        ELSE unit_label
    END,
    sort_order = CASE product_key
        WHEN 'minecraft:iron_ingot:0' THEN 100
        WHEN 'minecraft:gold_ingot:0' THEN 110
        WHEN 'gregtech:gt.metaitem.01:11305' THEN 120
        ELSE sort_order
    END,
    reference_price = CASE product_key
        WHEN 'minecraft:iron_ingot:0' THEN 64
        WHEN 'minecraft:gold_ingot:0' THEN 256
        WHEN 'gregtech:gt.metaitem.01:11305' THEN 102
        ELSE reference_price
    END,
    updated_at = now()
WHERE product_key IN (
    'minecraft:iron_ingot:0',
    'minecraft:gold_ingot:0',
    'gregtech:gt.metaitem.01:11305'
);

COMMIT;
