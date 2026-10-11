package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class RevisionAwarePublishedQuestCatalogTest {
    @Test
    public void decodesOncePerRevisionAndPublishesImmutableSnapshots() {
        AtomicLong revision = new AtomicLong(7L);
        AtomicInteger revisionReads = new AtomicInteger();
        AtomicInteger catalogReads = new AtomicInteger();
        List<StoredQuestDefinition> first = Collections.singletonList(stored("First"));
        List<StoredQuestDefinition> second = Collections.singletonList(stored("Second"));
        PublishedQuestCatalog source = () -> catalogReads.incrementAndGet() == 1 ? first : second;
        PublishedQuestCatalogRevisionProvider revisions = () -> {
            revisionReads.incrementAndGet();
            return revision.get();
        };
        RevisionAwarePublishedQuestCatalog catalog = new RevisionAwarePublishedQuestCatalog(source, revisions);

        List<StoredQuestDefinition> initial = catalog.findAllPublished();
        assertSame(initial, catalog.findAllPublished());
        assertEquals("First", initial.get(0).getDefinition().getName());
        assertEquals(1, catalogReads.get());
        assertEquals(3, revisionReads.get());
        try {
            initial.clear();
            fail("cached catalog must be immutable");
        } catch (UnsupportedOperationException expected) { }

        revision.incrementAndGet();
        List<StoredQuestDefinition> refreshed = catalog.findAllPublished();
        assertNotSame(initial, refreshed);
        assertEquals("Second", refreshed.get(0).getDefinition().getName());
        assertEquals(2, catalogReads.get());
    }

    @Test
    public void revisionReadFailureDoesNotReturnStaleDefinitionsOrPoisonNextRefresh() {
        AtomicLong revision = new AtomicLong(1L);
        AtomicInteger catalogReads = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        RevisionAwarePublishedQuestCatalog catalog = new RevisionAwarePublishedQuestCatalog(
            () -> {
                catalogReads.incrementAndGet();
                return Collections.singletonList(stored("Current"));
            },
            () -> {
                if (failures.getAndIncrement() == 2) throw new IllegalStateException("database unavailable");
                return revision.get();
            });

        catalog.findAllPublished();
        revision.incrementAndGet();
        try {
            catalog.findAllPublished();
            fail("revision read failure must propagate rather than serve a stale plan");
        } catch (IllegalStateException expected) {
            assertEquals("database unavailable", expected.getMessage());
        }
        assertEquals(1, catalogReads.get());
        assertEquals("Current", catalog.findAllPublished().get(0).getDefinition().getName());
        assertEquals(2, catalogReads.get());
    }

    private static StoredQuestDefinition stored(String name) {
        QuestDefinition definition = new QuestDefinition(UUID.randomUUID(), 1, name, "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.<TaskDefinition>emptyList(),
            Collections.<RewardDefinition>emptyList());
        return new StoredQuestDefinition(definition, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L);
    }
}
