package com.jsirgalaxybase.quest.bqimport;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuestImporterTestDatabaseUrlTest {
    @Test public void requiresExplicitOptInAndTestDatabaseName() {
        assertTrue(QuestImporterTestDatabaseUrl.isExplicitlyIsolated(
            "jdbc:postgresql://127.0.0.1:55432/jsirgalaxybase_it", "true"));
        assertFalse(QuestImporterTestDatabaseUrl.isExplicitlyIsolated(
            "jdbc:postgresql://galaxy-base:5432/jsirgalaxybase_test", "true"));
        assertFalse(QuestImporterTestDatabaseUrl.isExplicitlyIsolated(
            "jdbc:postgresql://127.0.0.1:5432/jsirgalaxybase", "true"));
        assertFalse(QuestImporterTestDatabaseUrl.isExplicitlyIsolated(
            "jdbc:postgresql://127.0.0.1:55432/jsirgalaxybase_test", null));
    }
}
