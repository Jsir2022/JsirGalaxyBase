package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public final class AuthenticatedQuestContentMigrationQueryTest {
    @Test public void unauthorizedHealthDoesNotAccessRepository() {
        final int[] reads = {0};
        QuestContentMigrationQuery repository = repository(reads);
        AuthenticatedQuestContentMigrationQuery query =
            new AuthenticatedQuestContentMigrationQuery(repository, actor -> false);
        assertFalse(query.health(new QuestEditorActor(UUID.randomUUID(), "player")).isPresent());
        assertFalse(query.health(null).isPresent());
        assertEquals(0, reads[0]);
    }

    @Test public void authorizedHealthReadsServerCounters() {
        final int[] reads = {0};
        AuthenticatedQuestContentMigrationQuery query =
            new AuthenticatedQuestContentMigrationQuery(repository(reads), actor -> true);
        QuestRuntimeHealthSummary health = query.health(new QuestEditorActor(UUID.randomUUID(), "admin")).get();
        assertEquals(3744L, health.getQuests());
        assertEquals(5620L, health.getTasks());
        assertEquals(5448L, health.getRewards());
        assertEquals(47L, health.getChapters());
        assertEquals(2L, health.getFailedRewards());
        assertEquals(3L, health.getPendingConsumption());
        assertEquals(1, reads[0]);
    }

    private static QuestContentMigrationQuery repository(final int[] reads) {
        return new QuestContentMigrationQuery() {
            public List<QuestContentMigrationSummary> listRecent(int limit) { return Collections.emptyList(); }
            public Optional<QuestRuntimeHealthSummary> health() {
                reads[0]++;
                return Optional.of(new QuestRuntimeHealthSummary(3744, 5620, 5448, 47, 9, 2, 3));
            }
        };
    }
}
