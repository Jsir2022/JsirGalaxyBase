-- Personal land Batch B: PostgreSQL title truth and idempotent operation receipts.

CREATE TABLE IF NOT EXISTS land_title (
    title_id BIGSERIAL PRIMARY KEY,
    source_server_id VARCHAR(128) NOT NULL,
    dimension_id INTEGER NOT NULL,
    chunk_x INTEGER NOT NULL,
    chunk_z INTEGER NOT NULL,
    owner_player_ref VARCHAR(128) NOT NULL,
    title_status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT land_title_status_check CHECK (title_status IN ('ACTIVE', 'LISTED', 'FROZEN', 'REVOKED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS land_title_active_chunk_unique
    ON land_title (source_server_id, dimension_id, chunk_x, chunk_z)
    WHERE title_status <> 'REVOKED';

CREATE INDEX IF NOT EXISTS land_title_owner_active_idx
    ON land_title (owner_player_ref, source_server_id, title_id)
    WHERE title_status <> 'REVOKED';

CREATE TABLE IF NOT EXISTS land_operation_log (
    operation_id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(160) NOT NULL UNIQUE,
    semantics_key VARCHAR(512) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    result_code VARCHAR(64) NOT NULL,
    operator_player_ref VARCHAR(128) NOT NULL,
    source_server_id VARCHAR(128) NOT NULL,
    dimension_id INTEGER NOT NULL,
    chunk_x INTEGER NOT NULL,
    chunk_z INTEGER NOT NULL,
    expected_version BIGINT NULL,
    before_title_id BIGINT NULL,
    before_owner_player_ref VARCHAR(128) NULL,
    before_title_status VARCHAR(32) NULL,
    before_version BIGINT NULL,
    after_title_id BIGINT NULL,
    after_owner_player_ref VARCHAR(128) NULL,
    after_title_status VARCHAR(32) NULL,
    after_version BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT land_operation_action_check CHECK (action_type IN ('CLAIM', 'UNCLAIM'))
);

CREATE INDEX IF NOT EXISTS land_operation_player_created_idx
    ON land_operation_log (operator_player_ref, created_at DESC, operation_id DESC);
