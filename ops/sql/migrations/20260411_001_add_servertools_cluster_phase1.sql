BEGIN;

CREATE TABLE IF NOT EXISTS cluster_server_directory (
    server_id VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(128) NOT NULL,
    gateway_endpoint VARCHAR(255),
    local_server BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS cluster_transfer_ticket (
    ticket_id VARCHAR(64) PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL UNIQUE,
    player_uuid VARCHAR(64) NOT NULL,
    player_name VARCHAR(64) NOT NULL,
    teleport_kind VARCHAR(24) NOT NULL,
    source_server_id VARCHAR(64) NOT NULL,
    target_server_id VARCHAR(64) NOT NULL,
    target_dimension_id INT NOT NULL,
    target_x DOUBLE PRECISION NOT NULL,
    target_y DOUBLE PRECISION NOT NULL,
    target_z DOUBLE PRECISION NOT NULL,
    target_yaw REAL NOT NULL,
    target_pitch REAL NOT NULL,
    status VARCHAR(24) NOT NULL,
    status_message VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_cluster_transfer_ticket_status CHECK (status IN ('PENDING_GATEWAY', 'DISPATCHED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_cluster_transfer_ticket_player
    ON cluster_transfer_ticket (player_uuid, created_at DESC);

CREATE TABLE IF NOT EXISTS player_home (
    player_uuid VARCHAR(64) NOT NULL,
    home_name VARCHAR(64) NOT NULL,
    server_id VARCHAR(64) NOT NULL,
    dimension_id INT NOT NULL,
    target_x DOUBLE PRECISION NOT NULL,
    target_y DOUBLE PRECISION NOT NULL,
    target_z DOUBLE PRECISION NOT NULL,
    target_yaw REAL NOT NULL,
    target_pitch REAL NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (player_uuid, home_name)
);

CREATE TABLE IF NOT EXISTS player_back_record (
    player_uuid VARCHAR(64) PRIMARY KEY,
    teleport_kind VARCHAR(24) NOT NULL,
    server_id VARCHAR(64) NOT NULL,
    dimension_id INT NOT NULL,
    target_x DOUBLE PRECISION NOT NULL,
    target_y DOUBLE PRECISION NOT NULL,
    target_z DOUBLE PRECISION NOT NULL,
    target_yaw REAL NOT NULL,
    target_pitch REAL NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS server_warp (
    warp_name VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    server_id VARCHAR(64) NOT NULL,
    dimension_id INT NOT NULL,
    target_x DOUBLE PRECISION NOT NULL,
    target_y DOUBLE PRECISION NOT NULL,
    target_z DOUBLE PRECISION NOT NULL,
    target_yaw REAL NOT NULL,
    target_pitch REAL NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS player_tpa_request (
    request_id VARCHAR(64) PRIMARY KEY,
    requester_player_uuid VARCHAR(64) NOT NULL,
    requester_player_name VARCHAR(64) NOT NULL,
    requester_server_id VARCHAR(64) NOT NULL,
    requester_origin_server_id VARCHAR(64) NOT NULL,
    requester_origin_dimension_id INT NOT NULL,
    requester_origin_x DOUBLE PRECISION NOT NULL,
    requester_origin_y DOUBLE PRECISION NOT NULL,
    requester_origin_z DOUBLE PRECISION NOT NULL,
    requester_origin_yaw REAL NOT NULL,
    requester_origin_pitch REAL NOT NULL,
    target_player_name VARCHAR(64) NOT NULL,
    target_server_id VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_player_tpa_request_status CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_player_tpa_target_lookup
    ON player_tpa_request (target_server_id, target_player_name, requester_player_name, status, expires_at DESC);

CREATE TABLE IF NOT EXISTS player_rtp_record (
    record_id BIGSERIAL PRIMARY KEY,
    player_uuid VARCHAR(64) NOT NULL,
    source_server_id VARCHAR(64) NOT NULL,
    dimension_id INT NOT NULL,
    target_x DOUBLE PRECISION NOT NULL,
    target_y DOUBLE PRECISION NOT NULL,
    target_z DOUBLE PRECISION NOT NULL,
    target_yaw REAL NOT NULL,
    target_pitch REAL NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_player_rtp_record_player
    ON player_rtp_record (player_uuid, created_at DESC);

COMMIT;