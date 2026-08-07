-- Server-authoritative Base Vault sort evidence. One row per changed Vault slot.
CREATE TABLE IF NOT EXISTS warehouse_operation_slot_change (
    operation_id BIGINT NOT NULL REFERENCES warehouse_operation_log(operation_id) ON DELETE CASCADE,
    slot_index INTEGER NOT NULL CHECK (slot_index >= 0),
    before_snapshot TEXT NULL,
    after_snapshot TEXT NULL,
    before_version BIGINT NOT NULL,
    after_version BIGINT NOT NULL,
    PRIMARY KEY (operation_id, slot_index)
);

CREATE INDEX IF NOT EXISTS warehouse_operation_slot_change_operation_idx
    ON warehouse_operation_slot_change (operation_id, slot_index);
