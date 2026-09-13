package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;

public class AuthenticatedQuestDefinitionBatchImpactQueryTest {
    @Test public void forbiddenActorDoesNotReadCatalogs() {
        CountingDefinitions definitions = new CountingDefinitions();
        CountingChapters chapters = new CountingChapters();
        AuthenticatedQuestDefinitionBatchImpactQuery query = new AuthenticatedQuestDefinitionBatchImpactQuery(
            definitions, chapters, actor -> false);
        assertEquals(AuthenticatedQuestDefinitionBatchImpactQuery.Status.FORBIDDEN,
            query.load(new QuestEditorActor(UUID.randomUUID(), "viewer"), Collections.singletonList(UUID.randomUUID()))
                .getStatus());
        assertEquals(0, definitions.reads); assertEquals(0, chapters.reads);
    }

    private static final class CountingDefinitions implements QuestDefinitionRepository {
        int reads;
        public List<StoredQuestDefinition> findAllPublished(){reads++;return Collections.emptyList();}
        public Optional<StoredQuestDefinition> findPublished(UUID id){return Optional.empty();}
        public Optional<StoredQuestDefinition> find(UUID id,int version){return Optional.empty();}
        public StoredQuestDefinition createDraft(QuestDefinition value){throw new UnsupportedOperationException();}
        public StoredQuestDefinition updateDraft(QuestDefinition value,String hash){throw new UnsupportedOperationException();}
        public boolean publish(UUID id,int version,String hash,long at){return false;}
        public boolean retire(UUID id,int version){return false;}
    }
    private static final class CountingChapters implements QuestChapterRepository {
        int reads;
        public List<StoredQuestChapter> findAllPublished(){reads++;return Collections.emptyList();}
        public Optional<StoredQuestChapter> findPublished(UUID id){return Optional.empty();}
        public Optional<StoredQuestChapter> find(UUID id,int version){return Optional.empty();}
        public StoredQuestChapter createDraft(QuestChapterDefinition value){throw new UnsupportedOperationException();}
        public StoredQuestChapter updateDraft(QuestChapterDefinition value,String hash){throw new UnsupportedOperationException();}
        public boolean publish(UUID id,int version,String hash,long at){return false;}
        public boolean retire(UUID id,int version){return false;}
    }
}
