BEGIN;

ALTER TABLE cluster_transfer_ticket
    DROP CONSTRAINT IF EXISTS ck_cluster_transfer_ticket_status;

ALTER TABLE cluster_transfer_ticket
    ADD CONSTRAINT ck_cluster_transfer_ticket_status
        CHECK (status IN ('PENDING_GATEWAY', 'DISPATCHED', 'COMPLETED', 'EXPIRED', 'FAILED'));

CREATE INDEX IF NOT EXISTS idx_cluster_transfer_ticket_target_restore
    ON cluster_transfer_ticket (target_server_id, player_uuid, status, expires_at DESC);

COMMIT;