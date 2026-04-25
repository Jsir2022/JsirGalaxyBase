ALTER TABLE ledger_entry
    ADD COLUMN IF NOT EXISTS frozen_balance_before BIGINT NOT NULL DEFAULT 0;

ALTER TABLE ledger_entry
    ADD COLUMN IF NOT EXISTS frozen_balance_after BIGINT NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'ledger_entry'::regclass
          AND conname = 'ck_ledger_entry_frozen_balance_before_nonnegative'
    ) THEN
        ALTER TABLE ledger_entry
            ADD CONSTRAINT ck_ledger_entry_frozen_balance_before_nonnegative
            CHECK (frozen_balance_before >= 0);
    END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'ledger_entry'::regclass
          AND conname = 'ck_ledger_entry_frozen_balance_after_nonnegative'
    ) THEN
        ALTER TABLE ledger_entry
            ADD CONSTRAINT ck_ledger_entry_frozen_balance_after_nonnegative
            CHECK (frozen_balance_after >= 0);
    END IF;
END;
$$;