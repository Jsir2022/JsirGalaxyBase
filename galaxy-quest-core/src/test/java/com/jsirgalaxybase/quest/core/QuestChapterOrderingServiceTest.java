package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class QuestChapterOrderingServiceTest {
    @Test public void atomicallyReplacesOnlyACompleteDistinctCatalogOrder() {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), third = UUID.randomUUID();
        Catalog catalog = new Catalog(first, second, third);
        QuestChapterOrderingService service = new QuestChapterOrderingService(catalog, new DirectTransaction(), auth(true));
        QuestChapterOrderingResult result = service.reorder(actor(), Arrays.asList(third, first, second));
        assertEquals(QuestChapterOrderingResult.Status.SUCCESS, result.getStatus());
        assertEquals(Arrays.asList(third, first, second), ids(result.getOrder()));
        assertEquals(1, catalog.replacements);
        assertEquals(Arrays.asList(first, third, second), ids(service.moveBefore(actor(), first, third).getOrder()));
        assertEquals(2, catalog.replacements);
        assertEquals(QuestChapterOrderingResult.Status.INVALID, service.reorder(actor(), Arrays.asList(first, first, second)).getStatus());
        assertEquals(2, catalog.replacements);
        assertEquals(QuestChapterOrderingResult.Status.INVALID, service.reorder(actor(), Arrays.asList(first, second)).getStatus());
        assertEquals(2, catalog.replacements);
    }
    @Test public void deniesBeforeReadingCatalog() {
        Catalog catalog = new Catalog(UUID.randomUUID());
        QuestChapterOrderingService service = new QuestChapterOrderingService(catalog, new DirectTransaction(), auth(false));
        assertEquals(QuestChapterOrderingResult.Status.FORBIDDEN, service.reorder(actor(), Collections.singletonList(UUID.randomUUID())).getStatus());
        assertEquals(0, catalog.reads);
    }
    private static List<UUID> ids(List<QuestChapterCatalogEntry> values) { List<UUID> result = new ArrayList<UUID>(); for (QuestChapterCatalogEntry value : values) result.add(value.getChapterId()); return result; }
    private static QuestEditorActor actor() { return new QuestEditorActor(UUID.randomUUID(), "operator"); }
    private static QuestEditorAuthorization auth(final boolean allowed) { return new QuestEditorAuthorization() { @Override public boolean canManageDefinitions(QuestEditorActor actor) { return allowed; } }; }
    private static final class DirectTransaction implements QuestTransaction { @Override public <T> T inTransaction(Work<T> work) { return work.execute(); } }
    private static final class Catalog implements QuestChapterCatalogRepository { final List<UUID> order = new ArrayList<UUID>(); int reads, replacements;
        Catalog(UUID... values) { order.addAll(Arrays.asList(values)); }
        @Override public void ensureCatalogEntry(UUID chapterId) { if (!order.contains(chapterId)) order.add(chapterId); }
        @Override public List<QuestChapterCatalogEntry> listOrdered() { reads++; List<QuestChapterCatalogEntry> result = new ArrayList<QuestChapterCatalogEntry>(); for (int i = 0; i < order.size(); i++) result.add(new QuestChapterCatalogEntry(order.get(i), i)); return result; }
        @Override public void replaceOrder(List<UUID> values) { replacements++; order.clear(); order.addAll(values); }
    }
}
