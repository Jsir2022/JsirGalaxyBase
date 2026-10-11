package com.jsirgalaxybase.quest.postgres;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.junit.Assume;
import org.junit.Test;

/** Runs the source-progress migration only against the explicitly isolated Quest test database. */
public class QuestTaskSourceProgressMigrationIntegrationTest {
    private static final String MIGRATION = "20261010_001_add_quest_task_source_progress.sql";

    @Test
    public void migrationIsIdempotentAndPreservesLegacyTaskProgress() throws Exception {
        String url = System.getenv("QUEST_TEST_POSTGRES_URL");
        if (url == null || url.trim().isEmpty()) {
            Assume.assumeTrue("QUEST_TEST_POSTGRES_URL is required for migration integration", false);
        }
        if (!QuestTestDatabaseUrl.isExplicitlyIsolated(url, System.getenv("QUEST_TEST_POSTGRES_ISOLATED"))) {
            throw new IllegalStateException("Quest migration test requires an explicitly isolated *_test or *_it database");
        }
        String user = System.getenv("POSTGRES_USER");
        String password = System.getenv("POSTGRES_PASSWORD");
        if (user == null || password == null) throw new IllegalStateException(
            "POSTGRES_USER and POSTGRES_PASSWORD are required for migration integration");

        Class.forName("org.postgresql.Driver");
        String schema = "quest_source_progress_" + UUID.randomUUID().toString().replace("-", "");
        try {
            execute(url, user, password, "CREATE SCHEMA \"" + schema + "\"");
            String scopedUrl = withCurrentSchema(url, schema);
            execute(scopedUrl, user, password,
                "CREATE TABLE galaxy_quest_task_progress ("
                    + "participant_type VARCHAR(16) NOT NULL, participant_id UUID NOT NULL, quest_id UUID NOT NULL, "
                    + "definition_version INTEGER NOT NULL, task_key VARCHAR(128) NOT NULL, "
                    + "progress_values BIGINT[] NOT NULL, target_values BIGINT[] NOT NULL, complete BOOLEAN NOT NULL, "
                    + "PRIMARY KEY (participant_type, participant_id, quest_id, definition_version, task_key))");
            execute(scopedUrl, user, password,
                "INSERT INTO galaxy_quest_task_progress VALUES ('PLAYER', '00000000-0000-0000-0000-000000000001', "
                    + "'00000000-0000-0000-0000-000000000002', 1, 'task-0', ARRAY[5]::BIGINT[], "
                    + "ARRAY[10]::BIGINT[], FALSE)");

            String migration = readMigration().replace("\r\n", "\n");
            execute(scopedUrl, user, password, migration);
            execute(scopedUrl, user, password, migration);

            try (Connection connection = DriverManager.getConnection(scopedUrl, user, password);
                 Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(
                     "SELECT progress_values[1], target_values[1], complete, source_progress_json::text, "
                         + "(SELECT is_nullable FROM information_schema.columns WHERE table_schema=current_schema() "
                         + "AND table_name='galaxy_quest_task_progress' AND column_name='source_progress_json') "
                         + "FROM galaxy_quest_task_progress WHERE task_key='task-0'")) {
                assertTrue("legacy progress row must survive", rows.next());
                assertEquals(5L, rows.getLong(1));
                assertEquals(10L, rows.getLong(2));
                assertFalse(rows.getBoolean(3));
                assertEquals("{}", rows.getString(4));
                assertEquals("NO", rows.getString(5));
                assertFalse("migration must not duplicate the legacy progress row", rows.next());
            }
        } finally {
            execute(url, user, password, "DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE");
        }
    }

    private static void execute(String url, String user, String password, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String withCurrentSchema(String url, String schema) {
        return url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
    }

    private static String readMigration() throws IOException {
        Path migration = Paths.get("ops/sql/migrations", MIGRATION);
        if (!Files.exists(migration)) migration = Paths.get("../ops/sql/migrations", MIGRATION);
        if (!Files.exists(migration)) throw new IOException("migration file is missing: " + MIGRATION);
        byte[] bytes = Files.readAllBytes(migration);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
