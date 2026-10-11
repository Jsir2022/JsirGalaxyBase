BEGIN;

CREATE TABLE IF NOT EXISTS galaxy_quest_definition_catalog_revision (
    singleton BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (singleton),
    revision BIGINT NOT NULL DEFAULT 0 CHECK (revision >= 0)
);

INSERT INTO galaxy_quest_definition_catalog_revision(singleton, revision)
VALUES (TRUE, 0) ON CONFLICT (singleton) DO NOTHING;

CREATE OR REPLACE FUNCTION galaxy_quest_bump_definition_catalog_revision() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    changed_rows INTEGER;
BEGIN
    IF (TG_OP = 'INSERT' AND NEW.lifecycle = 'PUBLISHED')
        OR (TG_OP = 'DELETE' AND OLD.lifecycle = 'PUBLISHED')
        OR (TG_OP = 'UPDATE'
            AND (OLD.lifecycle = 'PUBLISHED' OR NEW.lifecycle = 'PUBLISHED')
            AND (OLD.lifecycle IS DISTINCT FROM NEW.lifecycle
                OR OLD.quest_id IS DISTINCT FROM NEW.quest_id
                OR OLD.definition_version IS DISTINCT FROM NEW.definition_version
                OR OLD.content_json IS DISTINCT FROM NEW.content_json
                OR OLD.content_hash IS DISTINCT FROM NEW.content_hash
                OR OLD.published_at IS DISTINCT FROM NEW.published_at)) THEN
        EXECUTE format(
            'UPDATE %I.galaxy_quest_definition_catalog_revision '
                || 'SET revision = revision + 1 WHERE singleton = TRUE',
            TG_TABLE_SCHEMA);
        GET DIAGNOSTICS changed_rows = ROW_COUNT;
        IF changed_rows <> 1 THEN
            RAISE EXCEPTION 'quest definition catalog revision row is missing in schema %', TG_TABLE_SCHEMA;
        END IF;
    END IF;
    RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS galaxy_quest_definition_catalog_revision_trg ON galaxy_quest_definition;
CREATE TRIGGER galaxy_quest_definition_catalog_revision_trg
AFTER INSERT OR UPDATE OR DELETE ON galaxy_quest_definition
FOR EACH ROW EXECUTE PROCEDURE galaxy_quest_bump_definition_catalog_revision();

COMMIT;
