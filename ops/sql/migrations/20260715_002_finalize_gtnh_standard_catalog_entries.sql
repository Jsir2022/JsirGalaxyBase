BEGIN;

-- Complete the legacy catalogue rows using the verified GTNH 2.8.4 item metadata.
-- The reference prices intentionally use the latest persisted trade for each item:
-- they are catalogue guidance, not an enforced transaction price.
UPDATE standardized_market_catalog
SET display_name = CASE product_key
        WHEN 'gregtech:gt.metaitem.01:11019' THEN 'Aluminium Ingot'
        WHEN 'gregtech:gt.metaitem.01:11035' THEN 'Copper Ingot'
        WHEN 'gregtech:gt.metaitem.01:11054' THEN 'Silver Ingot'
        WHEN 'gregtech:gt.metaitem.01:17032' THEN 'Iron Plate'
        WHEN 'gregtech:gt.metaitem.01:17305' THEN 'Steel Plate'
    END,
    unit_label = CASE product_key
        WHEN 'gregtech:gt.metaitem.01:17032' THEN 'plate'
        WHEN 'gregtech:gt.metaitem.01:17305' THEN 'plate'
        ELSE 'ingot'
    END,
    reference_price = CASE product_key
        WHEN 'gregtech:gt.metaitem.01:11019' THEN 88
        WHEN 'gregtech:gt.metaitem.01:11035' THEN 13
        WHEN 'gregtech:gt.metaitem.01:11054' THEN 512
        WHEN 'gregtech:gt.metaitem.01:17032' THEN 64
        WHEN 'gregtech:gt.metaitem.01:17305' THEN 256
    END,
    sort_order = CASE product_key
        WHEN 'gregtech:gt.metaitem.01:11019' THEN 130
        WHEN 'gregtech:gt.metaitem.01:11035' THEN 140
        WHEN 'gregtech:gt.metaitem.01:11054' THEN 150
        WHEN 'gregtech:gt.metaitem.01:17032' THEN 160
        WHEN 'gregtech:gt.metaitem.01:17305' THEN 170
    END,
    category_code = 'metal',
    admission_basis = '管理员目录准入；GTNH 2.8.4 元数据已核验',
    source_entry_label = 'curated: GTNH 2.8.4 item metadata',
    updated_at = now()
WHERE product_key IN (
    'gregtech:gt.metaitem.01:11019',
    'gregtech:gt.metaitem.01:11035',
    'gregtech:gt.metaitem.01:11054',
    'gregtech:gt.metaitem.01:17032',
    'gregtech:gt.metaitem.01:17305'
);

COMMIT;
