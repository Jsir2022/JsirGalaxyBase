BEGIN;

ALTER TABLE custom_market_audit_log
    DROP CONSTRAINT IF EXISTS ck_custom_market_audit_type;

ALTER TABLE custom_market_audit_log
    ADD CONSTRAINT ck_custom_market_audit_type CHECK (
        audit_type IN (
            'LISTING_PUBLISH',
            'LISTING_PURCHASE',
            'LISTING_CLAIM',
            'LISTING_CANCEL',
            'LISTING_DELIVERY'
        )
    );

COMMIT;
