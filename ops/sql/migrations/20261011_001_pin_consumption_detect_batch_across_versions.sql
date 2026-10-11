-- Keep one active Detect operation per participant/Quest even after a newer definition is published.
-- Fail rather than discard or merge ambiguous in-flight operations from a pre-upgrade server.
BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM galaxy_quest_consumption_detect_batch
        WHERE batch_status='ACTIVE'
        GROUP BY participant_type,participant_id,quest_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'multiple active Quest Detect batches exist for one participant/Quest; resolve them before migration';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS galaxy_quest_consumption_detect_batch_active_subject_uq
    ON galaxy_quest_consumption_detect_batch (participant_type,participant_id,quest_id)
    WHERE batch_status='ACTIVE';

COMMIT;
