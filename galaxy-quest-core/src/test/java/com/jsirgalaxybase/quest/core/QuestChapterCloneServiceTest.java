package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

public class QuestChapterCloneServiceTest {
    @Test public void atomicallyCreatesCopiesAndPlacesThemWithSourceRelativeOffsets() {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        UUID firstCopy = UUID.randomUUID(), secondCopy = UUID.randomUUID();
        Definitions definitions = new Definitions(); definitions.seed(quest(first, Collections.<UUID>emptySet()));
        definitions.seed(quest(second, Collections.singleton(first)));
        Chapters chapters = new Chapters(chapter(first, second)); CountingTransaction transaction = new CountingTransaction();
        QuestChapterCloneService service = service(chapters, definitions, transaction, true, sequence(firstCopy, secondCopy));

        QuestChapterCloneResult result = service.cloneIntoChapter(actor(), new QuestChapterCloneRequest(
            chapters.value.getDefinition().getId(), 1, chapters.value.getContentHash(), sources(first, second), 100, 200));

        assertTrue(result.isSuccess()); assertEquals(1, transaction.calls); assertEquals(2, definitions.created);
        assertEquals(4, result.getChapter().getDefinition().getEntries().size());
        QuestChapterEntry copyA = entry(result.getChapter().getDefinition(), firstCopy);
        QuestChapterEntry copyB = entry(result.getChapter().getDefinition(), secondCopy);
        assertEquals(100, copyA.getX()); assertEquals(200, copyA.getY());
        assertEquals(124, copyB.getX()); assertEquals(212, copyB.getY());
        assertEquals(Collections.singleton(firstCopy), definitions.find(secondCopy, 1).get().getDefinition().getPrerequisites());
    }

    @Test public void rejectsUnplacedOrMissingSourcesBeforeTransactionOrWrites() {
        UUID placed = UUID.randomUUID(), unplaced = UUID.randomUUID();
        Definitions definitions = new Definitions(); definitions.seed(quest(placed, Collections.<UUID>emptySet()));
        definitions.seed(quest(unplaced, Collections.<UUID>emptySet())); Chapters chapters = new Chapters(chapter(placed));
        CountingTransaction transaction = new CountingTransaction();
        QuestChapterCloneService service = service(chapters, definitions, transaction, true, sequence(UUID.randomUUID()));
        QuestChapterCloneResult result = service.cloneIntoChapter(actor(), new QuestChapterCloneRequest(
            chapters.value.getDefinition().getId(), 1, chapters.value.getContentHash(), sources(unplaced), 10, 10));
        assertEquals(QuestChapterCloneResult.Status.INVALID, result.getStatus()); assertEquals(0, transaction.calls);
        assertEquals(0, definitions.created); assertEquals(0, chapters.updated);
    }

    @Test public void authorizationIsCheckedBeforeChapterLookupOrMutation() {
        UUID source = UUID.randomUUID(); Definitions definitions = new Definitions(); definitions.seed(quest(source,
            Collections.<UUID>emptySet())); Chapters chapters = new Chapters(chapter(source)); CountingTransaction transaction = new CountingTransaction();
        QuestChapterCloneService service = service(chapters, definitions, transaction, false, sequence(UUID.randomUUID()));
        QuestChapterCloneResult result = service.cloneIntoChapter(actor(), new QuestChapterCloneRequest(
            chapters.value.getDefinition().getId(), 1, chapters.value.getContentHash(), sources(source), 10, 10));
        assertEquals(QuestChapterCloneResult.Status.FORBIDDEN, result.getStatus()); assertEquals(0, transaction.calls);
        assertEquals(0, definitions.created); assertEquals(0, chapters.updated);
    }

    private static QuestChapterCloneService service(Chapters chapters, Definitions definitions,
        CountingTransaction transaction, final boolean allowed, QuestDefinitionBatchCloner.IdSource ids) {
        QuestElementTypeDescriptor type = new QuestElementTypeDescriptor("bq_standard:checkbox", QuestElementKind.TASK,
            "Checkbox", Collections.<QuestEditorFieldDescriptor>emptyList(), null);
        return new QuestChapterCloneService(chapters, definitions, transaction, new QuestEditorAuthorization() {
            @Override public boolean canManageDefinitions(QuestEditorActor actor) { return allowed; }
        }, new QuestEditorTypeRegistry(Collections.singletonList(type)), ids);
    }

    private static QuestDefinitionCloneRequest sources(UUID... ids) {
        java.util.ArrayList<QuestDefinitionCloneRequest.Source> result = new java.util.ArrayList<QuestDefinitionCloneRequest.Source>();
        for (UUID id : ids) result.add(new QuestDefinitionCloneRequest.Source(id, 1));
        return new QuestDefinitionCloneRequest(result);
    }
    private static QuestEditorActor actor() { return new QuestEditorActor(UUID.randomUUID(), "operator"); }
    private static QuestDefinition quest(UUID id, java.util.Set<UUID> prerequisites) {
        return new QuestDefinition(id, 1, "Quest", "", QuestLogic.AND, QuestLogic.AND, prerequisites,
            Collections.singletonList(new TaskDefinition("check", "bq_standard:checkbox", false,
                Collections.<String, String>emptyMap())), Collections.<RewardDefinition>emptyList());
    }
    private static QuestChapterDefinition chapter(UUID... ids) {
        java.util.ArrayList<QuestChapterEntry> entries = new java.util.ArrayList<QuestChapterEntry>();
        for (int i = 0; i < ids.length; i++) entries.add(new QuestChapterEntry(ids[i], 12 + 24 * i, 16 + 12 * i, 16, 12));
        return new QuestChapterDefinition(UUID.randomUUID(), 1, "Chapter", "", "", "", entries);
    }
    private static QuestChapterEntry entry(QuestChapterDefinition chapter, UUID id) {
        for (QuestChapterEntry entry : chapter.getEntries()) if (entry.getQuestId().equals(id)) return entry;
        throw new AssertionError("missing entry");
    }
    private static QuestDefinitionBatchCloner.IdSource sequence(final UUID... ids) {
        return new QuestDefinitionBatchCloner.IdSource() { int index;
            @Override public UUID next() { return ids[index++]; }
        };
    }
    private static final class CountingTransaction implements QuestTransaction { int calls;
        @Override public <T> T inTransaction(Work<T> work) { calls++; return work.execute(); }
    }
    private static final class Definitions implements QuestDefinitionRepository {
        private final Map<String, StoredQuestDefinition> values = new LinkedHashMap<String, StoredQuestDefinition>(); int created;
        private void seed(QuestDefinition value) { values.put(key(value.getId(), value.getVersion()),
            new StoredQuestDefinition(value, QuestDefinitionLifecycle.PUBLISHED, "h-" + value.getId(), 0L)); }
        @Override public StoredQuestDefinition createDraft(QuestDefinition value) { created++; StoredQuestDefinition result =
            new StoredQuestDefinition(value, QuestDefinitionLifecycle.DRAFT, "h-" + value.getId(), 0L); values.put(key(value.getId(), value.getVersion()), result); return result; }
        @Override public Optional<StoredQuestDefinition> find(UUID id, int version) { return Optional.ofNullable(values.get(key(id, version))); }
        @Override public Optional<StoredQuestDefinition> findPublished(UUID id) { Optional<StoredQuestDefinition> value = find(id, 1); return value.isPresent() && value.get().getLifecycle() == QuestDefinitionLifecycle.PUBLISHED ? value : Optional.<StoredQuestDefinition>empty(); }
        @Override public List<StoredQuestDefinition> findAllPublished() { return Collections.emptyList(); }
        @Override public StoredQuestDefinition updateDraft(QuestDefinition value, String hash) { throw new UnsupportedOperationException(); }
        @Override public boolean publish(UUID id, int version, String hash, long at) { return false; }
        @Override public boolean retire(UUID id, int version) { return false; }
        private static String key(UUID id, int version) { return id + ":" + version; }
    }
    private static final class Chapters implements QuestChapterRepository {
        private StoredQuestChapter value; int updated;
        private Chapters(QuestChapterDefinition definition) { value = new StoredQuestChapter(definition, QuestDefinitionLifecycle.DRAFT, "chapter-hash", 0L); }
        @Override public StoredQuestChapter updateDraft(QuestChapterDefinition definition, String hash) { updated++; if (!value.getContentHash().equals(hash)) throw new QuestDefinitionConflictException("stale"); value = new StoredQuestChapter(definition, QuestDefinitionLifecycle.DRAFT, "chapter-hash-2", 0L); return value; }
        @Override public Optional<StoredQuestChapter> find(UUID id, int version) { return value.getDefinition().getId().equals(id) && version == 1 ? Optional.of(value) : Optional.<StoredQuestChapter>empty(); }
        @Override public StoredQuestChapter createDraft(QuestChapterDefinition definition) { throw new UnsupportedOperationException(); }
        @Override public boolean publish(UUID id, int version, String hash, long at) { return false; }
        @Override public boolean retire(UUID id, int version) { return false; }
        @Override public Optional<StoredQuestChapter> findPublished(UUID id) { return Optional.empty(); }
        @Override public List<StoredQuestChapter> findAllPublished() { return Collections.emptyList(); }
    }
}
