-- Persistent cursor for the BQ-compatible Quest-level consuming Detect operation.
-- Apply only through the reviewed deployment migration process; this file has not been run on production.
BEGIN;

CREATE TABLE IF NOT EXISTS galaxy_quest_consumption_detect_batch (
    batch_id UUID PRIMARY KEY,
    source_server VARCHAR(64) NOT NULL,
    player_id UUID NOT NULL,
    participant_type VARCHAR(16) NOT NULL CHECK (participant_type IN ('PLAYER','PARTY','TEAM','PUBLIC')),
    participant_id UUID NOT NULL,
    quest_id UUID NOT NULL,
    definition_version INTEGER NOT NULL CHECK (definition_version > 0),
    quest_cycle INTEGER NOT NULL CHECK (quest_cycle >= 0),
    task_keys TEXT[] NOT NULL CHECK (cardinality(task_keys) > 0),
    next_task_index INTEGER NOT NULL DEFAULT 0 CHECK (next_task_index >= 0),
    batch_status VARCHAR(16) NOT NULL CHECK (batch_status = 'ACTIVE'),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    CHECK (next_task_index <= cardinality(task_keys)),
    FOREIGN KEY (quest_id, definition_version)
        REFERENCES galaxy_quest_definition (quest_id, definition_version)
);

CREATE UNIQUE INDEX IF NOT EXISTS galaxy_quest_consumption_detect_batch_active_uq
    ON galaxy_quest_consumption_detect_batch
        (participant_type, participant_id, quest_id, definition_version)
    WHERE batch_status='ACTIVE';
CREATE INDEX IF NOT EXISTS galaxy_quest_consumption_detect_batch_progress_idx
    ON galaxy_quest_consumption_detect_batch
        (participant_type, participant_id, quest_id, definition_version)
    WHERE batch_status='ACTIVE';

COMMIT;
