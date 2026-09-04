-- Personal AE2 Warehouse Drive v1: ownership/audit truth only; Cell contents remain in AE2.

CREATE TABLE IF NOT EXISTS warehouse_drive (
    drive_id BIGSERIAL PRIMARY KEY,
    source_server_id VARCHAR(128) NOT NULL,
    dimension_id INTEGER NOT NULL,
    block_x INTEGER NOT NULL,
    block_y INTEGER NOT NULL,
    block_z INTEGER NOT NULL,
    owner_player_ref VARCHAR(128) NOT NULL,
    drive_status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT warehouse_drive_status_check CHECK (drive_status IN ('ACTIVE', 'REMOVED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS warehouse_drive_active_location_unique
    ON warehouse_drive (source_server_id, dimension_id, block_x, block_y, block_z)
    WHERE drive_status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS warehouse_drive_owner_active_idx
    ON warehouse_drive (owner_player_ref, source_server_id, drive_id)
    WHERE drive_status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS warehouse_drive_operation (
    operation_id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(160) NOT NULL UNIQUE,
    semantics_key VARCHAR(512) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    result_code VARCHAR(64) NOT NULL,
    operator_player_ref VARCHAR(128) NOT NULL,
    source_server_id VARCHAR(128) NOT NULL,
    dimension_id INTEGER NOT NULL,
    block_x INTEGER NOT NULL,
    block_y INTEGER NOT NULL,
    block_z INTEGER NOT NULL,
    before_drive_id BIGINT NULL,
    before_owner_player_ref VARCHAR(128) NULL,
    before_status VARCHAR(32) NULL,
    before_version BIGINT NULL,
    after_drive_id BIGINT NULL,
    after_owner_player_ref VARCHAR(128) NULL,
    after_status VARCHAR(32) NULL,
    after_version BIGINT NULL,
    detail VARCHAR(512) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT warehouse_drive_operation_type_check CHECK (operation_type IN ('REGISTER', 'INSTALL_CELL', 'REMOVE_CELL', 'BREAK', 'DENIED'))
);

CREATE INDEX IF NOT EXISTS warehouse_drive_operation_owner_created_idx
    ON warehouse_drive_operation (operator_player_ref, source_server_id, operation_id DESC);
