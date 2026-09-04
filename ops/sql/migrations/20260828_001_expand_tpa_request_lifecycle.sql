BEGIN;

ALTER TABLE player_tpa_request
    ADD COLUMN IF NOT EXISTS accepted_target_dimension_id INT,
    ADD COLUMN IF NOT EXISTS accepted_target_x DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS accepted_target_y DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS accepted_target_z DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS accepted_target_yaw REAL,
    ADD COLUMN IF NOT EXISTS accepted_target_pitch REAL;

ALTER TABLE player_tpa_request
    DROP CONSTRAINT IF EXISTS ck_player_tpa_request_status;

ALTER TABLE player_tpa_request
    DROP CONSTRAINT IF EXISTS ck_player_tpa_request_accepted_target;

ALTER TABLE player_tpa_request
    ADD CONSTRAINT ck_player_tpa_request_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED', 'EXPIRED'));

ALTER TABLE player_tpa_request
    ADD CONSTRAINT ck_player_tpa_request_accepted_target
        CHECK (
            status <> 'ACCEPTED'
            OR (
                accepted_target_dimension_id IS NOT NULL
                AND accepted_target_x IS NOT NULL
                AND accepted_target_y IS NOT NULL
                AND accepted_target_z IS NOT NULL
                AND accepted_target_yaw IS NOT NULL
                AND accepted_target_pitch IS NOT NULL
            )
        );

CREATE INDEX IF NOT EXISTS idx_player_tpa_requester_accepted_dispatch
    ON player_tpa_request (requester_server_id, requester_player_uuid, expires_at DESC)
    WHERE status = 'ACCEPTED';

CREATE INDEX IF NOT EXISTS idx_player_tpa_requester_recent
    ON player_tpa_request (requester_server_id, requester_player_uuid, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_player_tpa_target_recent
    ON player_tpa_request (target_server_id, target_player_name, updated_at DESC);

COMMIT;
