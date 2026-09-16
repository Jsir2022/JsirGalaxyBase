-- Reviewed, additive migration from the committed PLAYER-only Quest schema.
-- Intentionally never executed by application startup; production execution requires separate approval.
BEGIN;

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

LOCK TABLE galaxy_quest_reward_entitlement IN ACCESS EXCLUSIVE MODE;

ALTER TABLE galaxy_quest_reward_entitlement
    ADD COLUMN IF NOT EXISTS recipient_player_id UUID,
    ADD COLUMN IF NOT EXISTS failure_count INTEGER NOT NULL DEFAULT 0;

-- A legacy group entitlement has no authoritative recipient and must be reviewed instead of guessed.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM galaxy_quest_reward_entitlement
        WHERE recipient_player_id IS NULL AND participant_type <> 'PLAYER'
    ) THEN
        RAISE EXCEPTION 'legacy non-PLAYER reward entitlement requires an explicit recipient mapping';
    END IF;
END $$;

UPDATE galaxy_quest_reward_entitlement
SET recipient_player_id = participant_id
WHERE recipient_player_id IS NULL AND participant_type = 'PLAYER';

ALTER TABLE galaxy_quest_reward_entitlement
    ALTER COLUMN recipient_player_id SET NOT NULL;

DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    FOR constraint_name IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'galaxy_quest_reward_entitlement'::regclass
          AND contype = 'u'
          AND pg_get_constraintdef(oid) =
              'UNIQUE (participant_type, participant_id, quest_id, definition_version, cycle, reward_key)'
    LOOP
        EXECUTE format('ALTER TABLE galaxy_quest_reward_entitlement DROP CONSTRAINT %I', constraint_name);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'galaxy_quest_reward_entitlement'::regclass
          AND conname = 'uq_galaxy_quest_entitlement_recipient_cycle_reward'
    ) THEN
        ALTER TABLE galaxy_quest_reward_entitlement
            ADD CONSTRAINT uq_galaxy_quest_entitlement_recipient_cycle_reward
            UNIQUE (participant_type, participant_id, recipient_player_id, quest_id,
                definition_version, cycle, reward_key);
    END IF;
END $$;

LOCK TABLE galaxy_quest_consumption_submission IN ACCESS EXCLUSIVE MODE;

-- Replace the original PLAYER-only checks with an explicit scoped-subject check. The actor remains player_id;
-- participant_id is the PostgreSQL progress owner selected by the server.
DO $$
DECLARE
    constraint_name TEXT;
    constraint_definition TEXT;
BEGIN
    FOR constraint_name, constraint_definition IN
        SELECT conname, pg_get_constraintdef(oid)
        FROM pg_constraint
        WHERE conrelid = 'galaxy_quest_consumption_submission'::regclass
          AND contype = 'c'
    LOOP
        IF constraint_definition LIKE '%participant_type%=%''PLAYER''%'
           OR constraint_definition LIKE '%participant_id%=%player_id%' THEN
            EXECUTE format('ALTER TABLE galaxy_quest_consumption_submission DROP CONSTRAINT %I', constraint_name);
        END IF;
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'galaxy_quest_consumption_submission'::regclass
          AND conname = 'ck_galaxy_quest_consumption_participant_type'
    ) THEN
        ALTER TABLE galaxy_quest_consumption_submission
            ADD CONSTRAINT ck_galaxy_quest_consumption_participant_type
            CHECK (participant_type IN ('PLAYER', 'PARTY', 'TEAM', 'PUBLIC'));
    END IF;
END $$;

COMMIT;
