BEGIN;

ALTER TABLE galaxy_quest_reward_entitlement
    DROP CONSTRAINT IF EXISTS galaxy_quest_reward_entitlement_delivery_status_check;

ALTER TABLE galaxy_quest_reward_entitlement
    ADD CONSTRAINT galaxy_quest_reward_entitlement_delivery_status_check
    CHECK (delivery_status IN ('CLAIMABLE', 'PENDING', 'DELIVERING', 'DELIVERED', 'SUPPRESSED', 'FAILED', 'ABANDONED'));

COMMIT;
