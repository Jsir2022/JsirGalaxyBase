package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

public class AuthenticatedQuestChapterDependencyQueryTest {
    @Test public void authorizesBeforeStorageAndClassifiesPublishedOffCanvasPrerequisites() {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), external = UUID.randomUUID(), missing = UUID.randomUUID();
        Chapters chapters = new Chapters(chapter(first, second)); Definitions definitions = new Definitions();
        definitions.add(quest(first, Collections.<UUID>emptySet()));
        definitions.add(quest(second, new java.util.LinkedHashSet<UUID>(Arrays.asList(first, external, missing))));
        definitions.add(quest(external, Collections.<UUID>emptySet()));
        AuthenticatedQuestChapterDependencyQuery query = new AuthenticatedQuestChapterDependencyQuery(chapters, definitions, auth(true));

        AuthenticatedQuestChapterDependencyQuery.Result result = query.load(actor(), chapters.value.getDefinition().getId(), 1);
        assertEquals(AuthenticatedQuestChapterDependencyQuery.Status.SUCCESS, result.getStatus());
        assertEquals(3, result.getGraph().getEdges().size());
        assertEquals(QuestChapterDependencyGraph.EdgeKind.INTERNAL, result.getGraph().getEdges().get(0).getKind());
        assertEquals(QuestChapterDependencyGraph.EdgeKind.EXTERNAL, result.getGraph().getEdges().get(1).getKind());
        assertEquals(QuestChapterDependencyGraph.EdgeKind.MISSING, result.getGraph().getEdges().get(2).getKind());

        AuthenticatedQuestChapterDependencyQuery denied = new AuthenticatedQuestChapterDependencyQuery(chapters, definitions, auth(false));
        assertEquals(AuthenticatedQuestChapterDependencyQuery.Status.FORBIDDEN, denied.load(actor(), UUID.randomUUID(), 1).getStatus());
        assertNull(denied.load(actor(), UUID.randomUUID(), 1).getGraph());
        assertEquals(1, chapters.findCalls);
    }

    private static QuestEditorActor actor() { return new QuestEditorActor(UUID.randomUUID(), "operator"); }
    private static QuestEditorAuthorization auth(final boolean allowed) { return new QuestEditorAuthorization() {
        @Override public boolean canManageDefinitions(QuestEditorActor actor) { return allowed; }
    }; }
    private static QuestDefinition quest(UUID id, java.util.Set<UUID> prerequisites) { return new QuestDefinition(id, 1, id.toString(), "",
        QuestLogic.AND, QuestLogic.AND, prerequisites, Collections.<TaskDefinition>emptyList(), Collections.<RewardDefinition>emptyList()); }
    private static QuestChapterDefinition chapter(UUID... ids) { java.util.List<QuestChapterEntry> entries = new java.util.ArrayList<QuestChapterEntry>();
        for (int i = 0; i < ids.length; i++) entries.add(new QuestChapterEntry(ids[i], i * 24, 0, 16, 16));
        return new QuestChapterDefinition(UUID.randomUUID(), 1, "Chapter", "", "", "", entries); }
    private static final class Chapters implements QuestChapterRepository { final StoredQuestChapter value; int findCalls;
        Chapters(QuestChapterDefinition value) { this.value = new StoredQuestChapter(value, QuestDefinitionLifecycle.DRAFT, "hash", 0L); }
        @Override public Optional<StoredQuestChapter> find(UUID id, int version) { findCalls++; return value.getDefinition().getId().equals(id) && version == 1 ? Optional.of(value) : Optional.<StoredQuestChapter>empty(); }
        @Override public StoredQuestChapter createDraft(QuestChapterDefinition value) { throw new UnsupportedOperationException(); }
        @Override public StoredQuestChapter updateDraft(QuestChapterDefinition value, String hash) { throw new UnsupportedOperationException(); }
        @Override public boolean publish(UUID id, int version, String hash, long at) { return false; }
        @Override public boolean retire(UUID id, int version) { return false; }
        @Override public Optional<StoredQuestChapter> findPublished(UUID id) { return Optional.empty(); }
        @Override public List<StoredQuestChapter> findAllPublished() { return Collections.emptyList(); }
    }
    private static final class Definitions implements QuestDefinitionRepository { final Map<UUID, StoredQuestDefinition> values = new LinkedHashMap<UUID, StoredQuestDefinition>();
        void add(QuestDefinition definition) { values.put(definition.getId(), new StoredQuestDefinition(definition, QuestDefinitionLifecycle.PUBLISHED, "hash", 0L)); }
        @Override public Optional<StoredQuestDefinition> findPublished(UUID id) { return Optional.ofNullable(values.get(id)); }
        @Override public Optional<StoredQuestDefinition> find(UUID id, int version) { return findPublished(id); }
        @Override public List<StoredQuestDefinition> findAllPublished() { return Collections.emptyList(); }
        @Override public StoredQuestDefinition createDraft(QuestDefinition value) { throw new UnsupportedOperationException(); }
        @Override public StoredQuestDefinition updateDraft(QuestDefinition value, String hash) { throw new UnsupportedOperationException(); }
        @Override public boolean publish(UUID id, int version, String hash, long at) { return false; }
        @Override public boolean retire(UUID id, int version) { return false; }
    }
}
