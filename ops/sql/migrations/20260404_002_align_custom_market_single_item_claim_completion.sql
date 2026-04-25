BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM custom_market_item_snapshot
        WHERE stack_size <> 1
    ) THEN
        RAISE EXCEPTION 'custom_market_item_snapshot contains stacked listings; clean existing data before applying 20260404_002';
    END IF;
END $$;

ALTER TABLE custom_market_item_snapshot
    DROP CONSTRAINT IF EXISTS ck_custom_market_item_snapshot_stack_positive;

ALTER TABLE custom_market_item_snapshot
    DROP CONSTRAINT IF EXISTS ck_custom_market_item_snapshot_single_item;

ALTER TABLE custom_market_item_snapshot
    ADD CONSTRAINT ck_custom_market_item_snapshot_single_item CHECK (stack_size = 1);

ALTER TABLE custom_market_audit_log
    DROP CONSTRAINT IF EXISTS ck_custom_market_audit_type;

ALTER TABLE custom_market_audit_log
    ADD CONSTRAINT ck_custom_market_audit_type CHECK (
        audit_type IN ('LISTING_PUBLISH', 'LISTING_PURCHASE', 'LISTING_CLAIM', 'LISTING_CANCEL')
    );

COMMIT;