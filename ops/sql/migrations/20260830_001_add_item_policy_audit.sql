CREATE TABLE IF NOT EXISTS item_policy_audit (
    audit_id BIGSERIAL PRIMARY KEY,
    source_server_id VARCHAR(96) NOT NULL,
    actor_player_ref VARCHAR(96) NOT NULL,
    policy_scope VARCHAR(48) NOT NULL,
    operation VARCHAR(96) NOT NULL,
    item_registry_name VARCHAR(255) NOT NULL,
    item_meta INTEGER NOT NULL,
    decision VARCHAR(24) NOT NULL,
    rule_id VARCHAR(96) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS item_policy_audit_actor_created_idx
    ON item_policy_audit (actor_player_ref, created_at DESC);

CREATE INDEX IF NOT EXISTS item_policy_audit_rule_created_idx
    ON item_policy_audit (rule_id, created_at DESC);
