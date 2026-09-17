BEGIN;

CREATE TABLE IF NOT EXISTS galaxy_quest_content_migration_batch (
    batch_id VARCHAR(64) PRIMARY KEY,
    source_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PREVIEW', 'BLOCKED', 'APPLIED', 'ROLLED_BACK')),
    manifest_text TEXT NOT NULL,
    error_count INTEGER NOT NULL DEFAULT 0 CHECK (error_count >= 0),
    warning_count INTEGER NOT NULL DEFAULT 0 CHECK (warning_count >= 0),
    previous_chapter_order TEXT NOT NULL DEFAULT '',
    applied_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rolled_back_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS galaxy_quest_content_migration_item (
    batch_id VARCHAR(64) NOT NULL REFERENCES galaxy_quest_content_migration_batch(batch_id) ON DELETE RESTRICT,
    entity_kind VARCHAR(16) NOT NULL CHECK (entity_kind IN ('QUEST', 'CHAPTER')),
    entity_id UUID NOT NULL,
    definition_version INTEGER NOT NULL CHECK (definition_version > 0),
    action VARCHAR(16) NOT NULL CHECK (action IN ('CREATED', 'VERSIONED', 'UNCHANGED')),
    content_hash VARCHAR(64) NOT NULL,
    PRIMARY KEY (batch_id, entity_kind, entity_id)
);

COMMIT;
