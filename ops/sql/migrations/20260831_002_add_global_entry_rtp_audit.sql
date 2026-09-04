BEGIN;

ALTER TABLE player_rtp_record
    ADD COLUMN IF NOT EXISTS request_id VARCHAR(96),
    ADD COLUMN IF NOT EXISTS target_server_id VARCHAR(64);

UPDATE player_rtp_record
SET request_id = 'legacy-rtp-' || record_id::text
WHERE request_id IS NULL OR btrim(request_id) = '';

UPDATE player_rtp_record
SET target_server_id = source_server_id
WHERE target_server_id IS NULL OR btrim(target_server_id) = '';

ALTER TABLE player_rtp_record
    ALTER COLUMN request_id SET NOT NULL,
    ALTER COLUMN target_server_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_player_rtp_record_request_id
    ON player_rtp_record (request_id);

CREATE INDEX IF NOT EXISTS idx_player_rtp_record_target
    ON player_rtp_record (target_server_id, created_at DESC);

COMMIT;
