CREATE TABLE IF NOT EXISTS galaxy_quest_definition (
    quest_id UUID NOT NULL,
    definition_version INTEGER NOT NULL CHECK (definition_version > 0),
    lifecycle VARCHAR(16) NOT NULL CHECK (lifecycle IN ('DRAFT', 'PUBLISHED', 'RETIRED')),
    content_json JSONB NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (quest_id, definition_version)
);

CREATE TABLE IF NOT EXISTS galaxy_quest_chapter_definition (
    chapter_id UUID NOT NULL,
    definition_version INTEGER NOT NULL CHECK (definition_version > 0),
    lifecycle VARCHAR(16) NOT NULL CHECK (lifecycle IN ('DRAFT', 'PUBLISHED', 'RETIRED')),
    content_json JSONB NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (chapter_id, definition_version)
);

-- Catalog order belongs to the logical chapter identity, not to an immutable definition version or canvas coordinates.
CREATE TABLE IF NOT EXISTS galaxy_quest_chapter_catalog (
    chapter_id UUID PRIMARY KEY,
    sort_order BIGINT NOT NULL CHECK (sort_order >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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

-- Safe for a fresh schema and for a reviewed future migration of existing definitions. Existing catalog entries stay untouched.
INSERT INTO galaxy_quest_chapter_catalog(chapter_id,sort_order)
SELECT chapter_id,ROW_NUMBER() OVER (ORDER BY created_at,chapter_id)-1
FROM (
    SELECT DISTINCT ON (chapter_id) chapter_id,created_at
    FROM galaxy_quest_chapter_definition
    ORDER BY chapter_id,definition_version DESC
) existing
ON CONFLICT (chapter_id) DO NOTHING;

CREATE TABLE IF NOT EXISTS galaxy_quest_fact (
    source_server VARCHAR(64) NOT NULL,
    event_id VARCHAR(160) NOT NULL,
    player_id UUID NOT NULL,
    fact_type VARCHAR(128) NOT NULL,
    occurred_at BIGINT NOT NULL,
    attributes_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (source_server, event_id)
);

CREATE TABLE IF NOT EXISTS galaxy_quest_participant (
    participant_type VARCHAR(16) NOT NULL CHECK (participant_type IN ('PARTY', 'TEAM', 'PUBLIC')),
    participant_id UUID NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    lifecycle VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (lifecycle IN ('ACTIVE', 'ARCHIVED')),
    revision BIGINT NOT NULL DEFAULT 0 CHECK (revision >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (participant_type, participant_id)
);

CREATE TABLE IF NOT EXISTS galaxy_quest_participant_membership (
    participant_type VARCHAR(16) NOT NULL CHECK (participant_type IN ('PARTY', 'TEAM', 'PUBLIC')),
    participant_id UUID NOT NULL,
    player_id UUID NOT NULL,
    member_role VARCHAR(16) NOT NULL DEFAULT 'MEMBER' CHECK (member_role IN ('OWNER', 'ADMIN', 'MEMBER')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMPTZ,
    membership_version BIGINT NOT NULL DEFAULT 0 CHECK (membership_version >= 0),
    CHECK (left_at IS NULL OR left_at >= joined_at),
    FOREIGN KEY (participant_type, participant_id)
        REFERENCES galaxy_quest_participant (participant_type, participant_id),
    PRIMARY KEY (participant_type, participant_id, player_id, joined_at)
);

CREATE UNIQUE INDEX IF NOT EXISTS galaxy_quest_one_active_scoped_membership_idx
    ON galaxy_quest_participant_membership (participant_type, player_id)
    WHERE left_at IS NULL;

CREATE INDEX IF NOT EXISTS galaxy_quest_participant_active_members_idx
    ON galaxy_quest_participant_membership (participant_type, participant_id, joined_at, player_id)
    WHERE left_at IS NULL;

CREATE TABLE IF NOT EXISTS galaxy_quest_progress (
    participant_type VARCHAR(16) NOT NULL CHECK (participant_type IN ('PLAYER', 'PARTY', 'TEAM', 'PUBLIC')),
    participant_id UUID NOT NULL,
    quest_id UUID NOT NULL,
    definition_version INTEGER NOT NULL CHECK (definition_version > 0),
    cycle INTEGER NOT NULL DEFAULT 0 CHECK (cycle >= 0),
    status VARCHAR(16) NOT NULL CHECK (status IN ('LOCKED', 'AVAILABLE', 'IN_PROGRESS', 'COMPLETED')),
    completed_at BIGINT NOT NULL DEFAULT 0,
    revision BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (participant_type, participant_id, quest_id, definition_version)
);

CREATE TABLE IF NOT EXISTS galaxy_quest_task_progress (
    participant_type VARCHAR(16) NOT NULL,
    participant_id UUID NOT NULL,
    quest_id UUID NOT NULL,
    definition_version INTEGER NOT NULL,
    task_key VARCHAR(128) NOT NULL,
    progress_values BIGINT[] NOT NULL,
    target_values BIGINT[] NOT NULL,
    complete BOOLEAN NOT NULL,
    PRIMARY KEY (participant_type, participant_id, quest_id, definition_version, task_key),
    FOREIGN KEY (participant_type, participant_id, quest_id, definition_version)
        REFERENCES galaxy_quest_progress (participant_type, participant_id, quest_id, definition_version)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS galaxy_quest_reward_entitlement (
    entitlement_key VARCHAR(512) PRIMARY KEY,
    participant_type VARCHAR(16) NOT NULL,
    participant_id UUID NOT NULL,
    recipient_player_id UUID NOT NULL,
    quest_id UUID NOT NULL,
    definition_version INTEGER NOT NULL,
    cycle INTEGER NOT NULL CHECK (cycle >= 0),
    reward_key VARCHAR(128) NOT NULL,
    reward_type VARCHAR(128) NOT NULL,
    reward_parameters_json JSONB NOT NULL,
    delivery_status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        CHECK (delivery_status IN ('CLAIMABLE', 'PENDING', 'DELIVERING', 'DELIVERED', 'FAILED', 'ABANDONED')),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    failure_count INTEGER NOT NULL DEFAULT 0 CHECK (failure_count >= 0),
    lease_owner VARCHAR(128),
    lease_until TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claimed_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    UNIQUE (participant_type, participant_id, recipient_player_id, quest_id, definition_version, cycle, reward_key)
);

CREATE TABLE IF NOT EXISTS galaxy_quest_reward_delivery_attempt (
    entitlement_key VARCHAR(512) NOT NULL REFERENCES galaxy_quest_reward_entitlement(entitlement_key) ON DELETE CASCADE,
    attempt_no INTEGER NOT NULL CHECK (attempt_no > 0),
    worker_id VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('STARTED', 'DELIVERED', 'FAILED', 'ABANDONED')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    error_text TEXT,
    PRIMARY KEY (entitlement_key, attempt_no)
);

CREATE TABLE IF NOT EXISTS galaxy_quest_reward_choice_selection (
    entitlement_key VARCHAR(512) PRIMARY KEY
        REFERENCES galaxy_quest_reward_entitlement(entitlement_key) ON DELETE CASCADE,
    choice_index INTEGER NOT NULL CHECK (choice_index >= 0),
    selected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS galaxy_quest_player_preference (
    player_id UUID NOT NULL,
    quest_id UUID NOT NULL,
    tracked BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (player_id, quest_id)
);

CREATE TABLE IF NOT EXISTS galaxy_quest_consumption_submission (
    submission_key VARCHAR(512) PRIMARY KEY,
    source_server VARCHAR(64) NOT NULL,
    player_id UUID NOT NULL,
    participant_type VARCHAR(16) NOT NULL CHECK (participant_type IN ('PLAYER','PARTY','TEAM','PUBLIC')),
    participant_id UUID NOT NULL,
    quest_id UUID NOT NULL,
    definition_version INTEGER NOT NULL CHECK (definition_version > 0),
    task_key VARCHAR(128) NOT NULL,
    resource_kind VARCHAR(16) NOT NULL CHECK (resource_kind IN ('item', 'fluid', 'xp')),
    amount_values BIGINT[] NOT NULL,
    applied_values BIGINT[],
    resource_parameters_json JSONB NOT NULL,
    submission_status VARCHAR(16) NOT NULL
        CHECK (submission_status IN ('PREPARED', 'APPLIED', 'CONFIRMED', 'REJECTED')),
    evidence TEXT,
    rejection_reason TEXT,
    created_at BIGINT NOT NULL,
    applied_at BIGINT,
    confirmed_at BIGINT,
    rejected_at BIGINT,
    FOREIGN KEY (quest_id, definition_version)
        REFERENCES galaxy_quest_definition (quest_id, definition_version)
);

CREATE INDEX IF NOT EXISTS galaxy_quest_fact_player_time_idx
    ON galaxy_quest_fact (player_id, occurred_at);
CREATE INDEX IF NOT EXISTS galaxy_quest_entitlement_pending_idx
    ON galaxy_quest_reward_entitlement (delivery_status, lease_until, created_at);
CREATE INDEX IF NOT EXISTS galaxy_quest_consumption_recovery_idx
    ON galaxy_quest_consumption_submission (submission_status, created_at);
CREATE INDEX IF NOT EXISTS galaxy_quest_player_tracked_idx
    ON galaxy_quest_player_preference (player_id, updated_at DESC) WHERE tracked;
