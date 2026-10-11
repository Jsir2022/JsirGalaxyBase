package com.jsirgalaxybase.quest.postgres;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuestTestDatabaseUrlTest {
    @Test public void requiresExplicitOptInAndTestDatabaseName() {
        assertTrue(QuestTestDatabaseUrl.isExplicitlyIsolated(
            "jdbc:postgresql://127.0.0.1:55432/jsirgalaxybase_test", "true"));
        assertFalse(QuestTestDatabaseUrl.isExplicitlyIsolated(
            "jdbc:postgresql://galaxy-base:5432/jsirgalaxybase", "true"));
        assertFalse(QuestTestDatabaseUrl.isExplicitlyIsolated(
            "jdbc:postgresql://127.0.0.1:5432/jsirgalaxybase", "true"));
        assertFalse(QuestTestDatabaseUrl.isExplicitlyIsolated(
            "jdbc:postgresql://127.0.0.1:55432/jsirgalaxybase_test", null));
    }
}
