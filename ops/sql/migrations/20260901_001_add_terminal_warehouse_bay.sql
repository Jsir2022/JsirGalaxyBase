-- Terminal-hosted personal AE2 Cell Bay. It does not create an AE2 grid node.

CREATE TABLE IF NOT EXISTS warehouse_terminal_bay (
    source_server_id VARCHAR(128) NOT NULL,
    owner_player_ref VARCHAR(128) NOT NULL,
    cell_nbt TEXT NULL,
    version BIGINT NOT NULL CHECK (version >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (source_server_id, owner_player_ref)
);

CREATE TABLE IF NOT EXISTS warehouse_terminal_bay_operation (
    operation_id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(160) NOT NULL UNIQUE,
    semantics_key VARCHAR(1024) NOT NULL,
    operation_type VARCHAR(48) NOT NULL,
    result_code VARCHAR(64) NOT NULL,
    operator_player_ref VARCHAR(128) NOT NULL,
    source_server_id VARCHAR(128) NOT NULL,
    before_cell_nbt TEXT NULL,
    before_version BIGINT NOT NULL,
    after_cell_nbt TEXT NULL,
    after_version BIGINT NOT NULL,
    detail VARCHAR(512) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT warehouse_terminal_bay_operation_type_check CHECK (operation_type IN ('CONTAINER_MUTATION'))
);

CREATE INDEX IF NOT EXISTS warehouse_terminal_bay_operation_owner_created_idx
    ON warehouse_terminal_bay_operation (source_server_id, operator_player_ref, operation_id DESC);
