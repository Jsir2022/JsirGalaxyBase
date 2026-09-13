package com.jsirgalaxybase.quest.postgres;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class QuestSchemaContractTest {
    @Test
    public void schemaContainsEveryAuthorityAndIdempotencyBoundary() throws Exception {
        String ddl = resource("/com/jsirgalaxybase/quest/postgres/quest-postgresql-ddl.sql");
        assertTrue(ddl.contains("PRIMARY KEY (source_server, event_id)"));
        assertTrue(ddl.contains("PRIMARY KEY (participant_type, participant_id, quest_id, definition_version)"));
        assertTrue(ddl.contains("progress_values BIGINT[] NOT NULL"));
        assertTrue(ddl.contains("entitlement_key VARCHAR(512) PRIMARY KEY"));
        assertTrue(ddl.contains("UNIQUE (participant_type, participant_id, quest_id, definition_version, cycle, reward_key)"));
        assertTrue(ddl.contains("galaxy_quest_reward_delivery_attempt"));
        assertTrue(ddl.contains("PRIMARY KEY (entitlement_key, attempt_no)"));
        assertTrue(ddl.contains("galaxy_quest_reward_choice_selection"));
        assertTrue(ddl.contains("choice_index INTEGER NOT NULL CHECK (choice_index >= 0)"));
        assertTrue(ddl.contains("galaxy_quest_consumption_submission"));
        assertTrue(ddl.contains("applied_values BIGINT[]"));
        assertTrue(ddl.contains("'item', 'fluid', 'xp'"));
        assertTrue(ddl.contains("submission_status IN ('PREPARED', 'APPLIED', 'CONFIRMED', 'REJECTED')"));
        assertTrue(ddl.contains("participant_id UUID NOT NULL CHECK (participant_id = player_id)"));
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
