BEGIN;

-- Stores per-source Crafting event and absolute-stat counters. Existing rows remain readable;
-- the first source-aware update preserves their aggregate as a legacy baseline.
ALTER TABLE galaxy_quest_task_progress
    ADD COLUMN IF NOT EXISTS source_progress_json JSONB NOT NULL DEFAULT '{}'::jsonb;

COMMIT;
