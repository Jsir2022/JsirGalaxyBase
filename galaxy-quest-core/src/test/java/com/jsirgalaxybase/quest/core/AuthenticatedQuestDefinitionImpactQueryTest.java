package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;

public class AuthenticatedQuestDefinitionImpactQueryTest {
    @Test public void rejectsBeforeReadingCatalogsAndLoadsOnlyForAnAuthorizedActor() {
        Definitions definitions = new Definitions(); Chapters chapters = new Chapters();
        QuestEditorActor actor = new QuestEditorActor(UUID.randomUUID(), "operator");
        AuthenticatedQuestDefinitionImpactQuery denied = new AuthenticatedQuestDefinitionImpactQuery(definitions,
            chapters, authorization(false));
        AuthenticatedQuestDefinitionImpactQuery.Result rejected = denied.load(actor, UUID.randomUUID());
        assertEquals(AuthenticatedQuestDefinitionImpactQuery.Status.FORBIDDEN, rejected.getStatus());
        assertNull(rejected.getReport());
        assertEquals(0, definitions.reads); assertEquals(0, chapters.reads);

        AuthenticatedQuestDefinitionImpactQuery allowed = new AuthenticatedQuestDefinitionImpactQuery(definitions,
            chapters, authorization(true));
        AuthenticatedQuestDefinitionImpactQuery.Result loaded = allowed.load(actor, UUID.randomUUID());
        assertEquals(AuthenticatedQuestDefinitionImpactQuery.Status.SUCCESS, loaded.getStatus());
        assertEquals(1, definitions.reads); assertEquals(1, chapters.reads);
    }

    private static QuestEditorAuthorization authorization(final boolean value) { return new QuestEditorAuthorization() {
        @Override public boolean canManageDefinitions(QuestEditorActor actor) { return value; }
    }; }
    private static final class Definitions implements QuestDefinitionRepository {
        int reads;
        @Override public List<StoredQuestDefinition> findAllPublished() { reads++; return Collections.emptyList(); }
        @Override public StoredQuestDefinition createDraft(QuestDefinition value) { throw new UnsupportedOperationException(); }
        @Override public StoredQuestDefinition updateDraft(QuestDefinition value, String hash) { throw new UnsupportedOperationException(); }
        @Override public boolean publish(UUID id, int version, String hash, long at) { return false; }
        @Override public boolean retire(UUID id, int version) { return false; }
        @Override public Optional<StoredQuestDefinition> find(UUID id, int version) { return Optional.empty(); }
        @Override public Optional<StoredQuestDefinition> findPublished(UUID id) { return Optional.empty(); }
    }
    private static final class Chapters implements QuestChapterRepository {
        int reads;
        @Override public List<StoredQuestChapter> findAllPublished() { reads++; return Collections.emptyList(); }
        @Override public StoredQuestChapter createDraft(QuestChapterDefinition value) { throw new UnsupportedOperationException(); }
        @Override public StoredQuestChapter updateDraft(QuestChapterDefinition value, String hash) { throw new UnsupportedOperationException(); }
        @Override public boolean publish(UUID id, int version, String hash, long at) { return false; }
        @Override public boolean retire(UUID id, int version) { return false; }
        @Override public Optional<StoredQuestChapter> find(UUID id, int version) { return Optional.empty(); }
        @Override public Optional<StoredQuestChapter> findPublished(UUID id) { return Optional.empty(); }
    }
}
