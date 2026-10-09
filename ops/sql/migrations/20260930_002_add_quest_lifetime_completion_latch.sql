-- Preserve BetterQuesting isComplete(UUID) across repeat-cycle progress resets.
ALTER TABLE galaxy_quest_progress
    ADD COLUMN IF NOT EXISTS ever_completed BOOLEAN NOT NULL DEFAULT FALSE;

-- The current persisted completed state is authoritative for legacy rows. Earlier completed
-- cycles overwritten before this migration cannot be reconstructed from the latest-progress table.
UPDATE galaxy_quest_progress
SET ever_completed = TRUE
WHERE status = 'COMPLETED' AND ever_completed = FALSE;
