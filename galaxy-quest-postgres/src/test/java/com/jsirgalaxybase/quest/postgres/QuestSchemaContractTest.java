package com.jsirgalaxybase.quest.postgres;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class QuestSchemaContractTest {
    @Test
    public void schemaContainsEveryAuthorityAndIdempotencyBoundary() throws Exception {
        String ddl = resource("/com/jsirgalaxybase/quest/postgres/quest-postgresql-ddl.sql");
        assertTrue(ddl.contains("PRIMARY KEY (source_server, event_id)"));
        assertTrue(ddl.contains("PRIMARY KEY (participant_type, participant_id, quest_id, definition_version)"));
        assertTrue(ddl.contains("progress_values BIGINT[] NOT NULL"));
        assertTrue(ddl.contains("entitlement_key VARCHAR(512) PRIMARY KEY"));
        assertTrue(ddl.contains("UNIQUE (participant_type, participant_id, recipient_player_id, quest_id, definition_version, cycle, reward_key)"));
        assertTrue(ddl.contains("galaxy_quest_participant_membership"));
        assertTrue(ddl.contains("galaxy_quest_one_active_scoped_membership_idx"));
        assertTrue(ddl.contains("recipient_player_id UUID NOT NULL"));
        assertTrue(ddl.contains("failure_count INTEGER NOT NULL DEFAULT 0"));
        assertTrue(ddl.contains("galaxy_quest_reward_delivery_attempt"));
        assertTrue(ddl.contains("PRIMARY KEY (entitlement_key, attempt_no)"));
        assertTrue(ddl.contains("galaxy_quest_reward_choice_selection"));
        assertTrue(ddl.contains("choice_index INTEGER NOT NULL CHECK (choice_index >= 0)"));
        assertTrue(ddl.contains("galaxy_quest_consumption_submission"));
        assertTrue(ddl.contains("applied_values BIGINT[]"));
        assertTrue(ddl.contains("'item', 'fluid', 'xp'"));
        assertTrue(ddl.contains("submission_status IN ('PREPARED', 'APPLIED', 'CONFIRMED', 'REJECTED')"));
        assertTrue(ddl.contains("participant_type VARCHAR(16) NOT NULL CHECK (participant_type IN ('PLAYER','PARTY','TEAM','PUBLIC'))"));
    }

    @Test public void reviewedMigrationUpgradesTheCommittedPlayerOnlySchema() throws Exception {
        Path migration=Paths.get("ops/sql/migrations/20260914_001_add_cross_server_quest_platform.sql");
        if(!Files.exists(migration))migration=Paths.get("../ops/sql/migrations/20260914_001_add_cross_server_quest_platform.sql");
        assertTrue("reviewed quest migration must exist",Files.exists(migration));
        String reviewed=new String(Files.readAllBytes(migration),StandardCharsets.UTF_8);
        assertTrue(reviewed.contains("BEGIN;"));
        assertTrue(reviewed.contains("CREATE TABLE IF NOT EXISTS galaxy_quest_participant"));
        assertTrue(reviewed.contains("ADD COLUMN IF NOT EXISTS recipient_player_id UUID"));
        assertTrue(reviewed.contains("SET recipient_player_id = participant_id"));
        assertTrue(reviewed.contains("ALTER COLUMN recipient_player_id SET NOT NULL"));
        assertTrue(reviewed.contains("ADD COLUMN IF NOT EXISTS failure_count INTEGER NOT NULL DEFAULT 0"));
        assertTrue(reviewed.contains("legacy non-PLAYER reward entitlement requires an explicit recipient mapping"));
        assertTrue(reviewed.contains("uq_galaxy_quest_entitlement_recipient_cycle_reward"));
        assertTrue(reviewed.contains("ck_galaxy_quest_consumption_participant_type"));
        assertTrue(reviewed.contains("COMMIT;"));
    }

    private static String resource(String path) throws Exception {
        InputStream input = QuestSchemaContractTest.class.getResourceAsStream(path);
        assertNotNull(input);
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) >= 0) output.write(buffer, 0, read);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
