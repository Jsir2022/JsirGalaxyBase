package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;

public class QuestDefinitionBatchRetirementServiceTest {
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2), C = new UUID(0, 3);
    private static final QuestEditorActor ADMIN = new QuestEditorActor(new UUID(0, 99), "admin");

    @Test public void retiresInternalDependencyGroupInOneTransaction() {
        Repository definitions = new Repository(); definitions.seed(quest(A)); definitions.seed(quest(B, A));
        CountingTransaction transaction = new CountingTransaction();
        QuestDefinitionBatchRetirementResult result = service(definitions, transaction).retire(ADMIN,
            request(target(A), target(B)));
        assertEquals(QuestDefinitionBatchRetirementResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getRetired().size()); assertEquals(1, transaction.calls);
        assertEquals(QuestDefinitionLifecycle.RETIRED, definitions.find(A, 1).get().getLifecycle());
        assertEquals(QuestDefinitionLifecycle.RETIRED, definitions.find(B, 1).get().getLifecycle());
    }

    @Test public void externalDependentPreventsEveryWrite() {
        Repository definitions = new Repository(); definitions.seed(quest(A)); definitions.seed(quest(B, A));
        definitions.seed(quest(C, B));
        QuestDefinitionBatchRetirementResult result = service(definitions, new CountingTransaction()).retire(ADMIN,
            request(target(A), target(B)));
        assertEquals(QuestDefinitionBatchRetirementResult.Status.UNSAFE, result.getStatus());
        assertEquals(C, result.getImpact().getExternalDependents().get(0).getQuestId());
        assertEquals(0, definitions.retireCalls);
    }

    @Test public void staleVersionFailsBeforeAnyWrite() {
        Repository definitions = new Repository(); definitions.seed(quest(A)); definitions.seed(quest(B));
        QuestDefinitionBatchRetirementResult result = service(definitions, new CountingTransaction()).retire(ADMIN,
            request(target(A), new QuestDefinitionBatchRetirementRequest.Target(B, 2)));
        assertEquals(QuestDefinitionBatchRetirementResult.Status.NOT_FOUND, result.getStatus());
        assertEquals(0, definitions.retireCalls);
    }

    @Test public void forbiddenActorDoesNotOpenTransactionOrReadCatalog() {
        Repository definitions = new Repository(); CountingTransaction transaction = new CountingTransaction();
        QuestDefinitionBatchRetirementService service = new QuestDefinitionBatchRetirementService(definitions,
            new Chapters(), transaction, actor -> false);
        assertEquals(QuestDefinitionBatchRetirementResult.Status.FORBIDDEN,
            service.retire(ADMIN, request(target(A))).getStatus());
        assertEquals(0, transaction.calls); assertEquals(0, definitions.catalogReads);
    }

    private static QuestDefinitionBatchRetirementService service(Repository definitions,
        CountingTransaction transaction) {
        return new QuestDefinitionBatchRetirementService(definitions, new Chapters(), transaction, actor -> true);
    }
    private static QuestDefinitionBatchRetirementRequest request(QuestDefinitionBatchRetirementRequest.Target... values) {
        return new QuestDefinitionBatchRetirementRequest(Arrays.asList(values));
    }
    private static QuestDefinitionBatchRetirementRequest.Target target(UUID id) {
        return new QuestDefinitionBatchRetirementRequest.Target(id, 1);
    }
    private static QuestDefinition quest(UUID id, UUID... prerequisites) {
        return new QuestDefinition(id, 1, "Quest " + id, "", QuestLogic.AND, QuestLogic.AND,
            new java.util.LinkedHashSet<UUID>(Arrays.asList(prerequisites)),
            Collections.<TaskDefinition>emptyList(), Collections.<RewardDefinition>emptyList());
    }
    private static final class CountingTransaction implements QuestTransaction {
        int calls; public <T>T inTransaction(Work<T> work){calls++;return work.execute();}
    }
    private static final class Repository implements QuestDefinitionRepository {
        final Map<String,StoredQuestDefinition> values=new LinkedHashMap<String,StoredQuestDefinition>();
        int retireCalls,catalogReads;
        void seed(QuestDefinition value){values.put(key(value.getId(),value.getVersion()),new StoredQuestDefinition(
            value,QuestDefinitionLifecycle.PUBLISHED,"hash",1L));}
        public Optional<StoredQuestDefinition> find(UUID id,int version){return Optional.ofNullable(values.get(key(id,version)));}
        public Optional<StoredQuestDefinition> findPublished(UUID id){for(StoredQuestDefinition value:values.values())if(value.getDefinition().getId().equals(id)&&value.getLifecycle()==QuestDefinitionLifecycle.PUBLISHED)return Optional.of(value);return Optional.empty();}
        public List<StoredQuestDefinition> findAllPublished(){catalogReads++;List<StoredQuestDefinition> result=new ArrayList<StoredQuestDefinition>();for(StoredQuestDefinition value:values.values())if(value.getLifecycle()==QuestDefinitionLifecycle.PUBLISHED)result.add(value);return result;}
        public boolean retire(UUID id,int version){retireCalls++;StoredQuestDefinition value=values.get(key(id,version));if(value==null||value.getLifecycle()!=QuestDefinitionLifecycle.PUBLISHED)return false;values.put(key(id,version),new StoredQuestDefinition(value.getDefinition(),QuestDefinitionLifecycle.RETIRED,value.getContentHash(),value.getPublishedAt()));return true;}
        public StoredQuestDefinition createDraft(QuestDefinition value){throw new UnsupportedOperationException();}
        public StoredQuestDefinition updateDraft(QuestDefinition value,String hash){throw new UnsupportedOperationException();}
        public boolean publish(UUID id,int version,String hash,long at){return false;}
        private static String key(UUID id,int version){return id+":"+version;}
    }
    private static final class Chapters implements QuestChapterRepository {
        public List<StoredQuestChapter> findAllPublished(){return Collections.emptyList();}
        public Optional<StoredQuestChapter> findPublished(UUID id){return Optional.empty();}
        public Optional<StoredQuestChapter> find(UUID id,int version){return Optional.empty();}
        public StoredQuestChapter createDraft(QuestChapterDefinition value){throw new UnsupportedOperationException();}
        public StoredQuestChapter updateDraft(QuestChapterDefinition value,String hash){throw new UnsupportedOperationException();}
        public boolean publish(UUID id,int version,String hash,long at){return false;}
        public boolean retire(UUID id,int version){return false;}
    }
}
