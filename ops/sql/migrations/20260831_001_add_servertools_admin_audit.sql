BEGIN;

CREATE TABLE IF NOT EXISTS servertools_admin_operation (
    operation_id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL UNIQUE,
    actor_player_uuid VARCHAR(64) NOT NULL,
    actor_player_name VARCHAR(64) NOT NULL,
    source_server_id VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    target_key VARCHAR(64) NOT NULL,
    before_snapshot TEXT NOT NULL,
    after_snapshot TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_servertools_admin_operation_action CHECK (
        action IN ('UPSERT_WARP', 'DELETE_WARP', 'SET_WARP_ENABLED', 'SET_SERVER_ENABLED')
    ),
    CONSTRAINT ck_servertools_admin_operation_target CHECK (target_type IN ('WARP', 'SERVER'))
);

CREATE INDEX IF NOT EXISTS idx_servertools_admin_operation_actor_time
    ON servertools_admin_operation (actor_player_uuid, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_servertools_admin_operation_target_time
    ON servertools_admin_operation (target_type, target_key, created_at DESC);

COMMIT;
