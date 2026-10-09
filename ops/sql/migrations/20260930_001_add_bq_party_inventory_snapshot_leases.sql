-- Add bounded, session-fenced snapshots for active BQ PARTY inventory detection.
-- This migration is additive and is never run by application startup.
BEGIN;

CREATE TABLE IF NOT EXISTS galaxy_quest_active_inventory_snapshot (
    player_id UUID PRIMARY KEY,
    source_server VARCHAR(64) NOT NULL,
    session_id UUID NOT NULL,
    attributes_json JSONB NOT NULL CHECK (jsonb_typeof(attributes_json) = 'object'),
    observed_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CHECK (expires_at > observed_at)
);

CREATE INDEX IF NOT EXISTS galaxy_quest_active_inventory_snapshot_expiry_idx
    ON galaxy_quest_active_inventory_snapshot (expires_at);

COMMIT;
