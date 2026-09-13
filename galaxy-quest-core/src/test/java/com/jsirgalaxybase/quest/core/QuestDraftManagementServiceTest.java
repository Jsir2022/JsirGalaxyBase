package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

public class QuestDraftManagementServiceTest {
    private final QuestEditorActor actor = new QuestEditorActor(UUID.randomUUID(), "operator");

    @Test
    public void rejectsUnauthenticatedOrUnauthorizedActorsBeforeMutation() {
        FakeRepository repository = new FakeRepository();
        QuestDraftManagementService service = service(repository, false);

        assertEquals(QuestDraftManagementResult.Status.FORBIDDEN,
            service.createDraft(actor, definition("test:task")).getStatus());
        assertEquals(0, repository.mutations);
    }

    @Test
    public void validatesElementTypesAndParametersBeforeMutation() {
        FakeRepository repository = new FakeRepository();
        QuestDraftManagementService service = service(repository, true);

        QuestDraftManagementResult result = service.createDraft(actor, definition("unknown:task"));

        assertEquals(QuestDraftManagementResult.Status.INVALID, result.getStatus());
        assertEquals("tasks.task.type", result.getIssues().get(0).getFieldKey());
        assertEquals(0, repository.mutations);
    }

    @Test
    public void createsUpdatesAndPublishesThroughTransactions() {
        FakeRepository repository = new FakeRepository();
        CountingTransaction transaction = new CountingTransaction();
        QuestDraftManagementService service = service(repository, transaction, true);
        QuestDefinition original = definition("test:task");

        QuestDraftManagementResult created = service.createDraft(actor, original);
        QuestDefinition edited = QuestDraftEditor.edit(original).name("Edited").build();
        QuestDraftManagementResult updated = service.updateDraft(actor, edited, created.getDefinition().getContentHash());
        QuestDraftManagementResult published = service.publish(actor, original.getId(), original.getVersion(),
            updated.getDefinition().getContentHash(), 1234L);
        QuestDraftManagementResult retired = service.retire(actor, original.getId(), original.getVersion());

        assertTrue(created.isSuccess());
        assertTrue(updated.isSuccess());
        assertTrue(published.isSuccess());
        assertEquals(QuestDefinitionLifecycle.PUBLISHED, published.getDefinition().getLifecycle());
        assertTrue(retired.isSuccess());
        assertEquals(QuestDefinitionLifecycle.RETIRED, retired.getDefinition().getLifecycle());
        assertEquals(4, transaction.calls);
    }

    @Test
    public void mapsStaleUpdateAndPublishHashesToConflict() {
        FakeRepository repository = new FakeRepository();
        QuestDraftManagementService service = service(repository, true);
        QuestDefinition definition = definition("test:task");
        service.createDraft(actor, definition);

        assertEquals(QuestDraftManagementResult.Status.CONFLICT,
            service.updateDraft(actor, definition, "stale").getStatus());
        assertEquals(QuestDraftManagementResult.Status.CONFLICT,
            service.publish(actor, definition.getId(), 1, "stale", 10L).getStatus());
    }

    @Test
    public void reportsMissingPublishTarget() {
        QuestDraftManagementService service = service(new FakeRepository(), true);
        assertEquals(QuestDraftManagementResult.Status.NOT_FOUND,
            service.publish(actor, UUID.randomUUID(), 1, "hash", 10L).getStatus());
    }

    @Test public void createsServerOwnedTemplateWithoutClientChosenIdentity(){FakeRepository repository=new FakeRepository();QuestDraftManagementService service=new QuestDraftManagementService(repository,new CountingTransaction(),new QuestEditorAuthorization(){public boolean canManageDefinitions(QuestEditorActor ignored){return true;}},StandardQuestEditorTypeRegistry.create());QuestDraftManagementResult result=service.createFromTemplate(actor,QuestDraftTemplate.MANUAL_CHECK,"公共确认");assertTrue(result.isSuccess());assertEquals("公共确认",result.getDefinition().getDefinition().getName());assertEquals("bq_standard:checkbox",result.getDefinition().getDefinition().getTasks().get(0).getTypeId());}

    @Test
    public void appliesBoundedSemanticEditBatchWithOneOptimisticWrite() {
        FakeRepository repository=new FakeRepository();CountingTransaction transaction=new CountingTransaction();
        QuestDraftManagementService service=service(repository,transaction,true);QuestDefinition original=definition("test:task");
        QuestDraftManagementResult created=service.createDraft(actor,original);int before=repository.mutations;
        QuestDraftEditRequest request=new QuestDraftEditRequest(original.getId(),1,created.getDefinition().getContentHash(),
            Arrays.asList(QuestDraftEditOperation.name("Edited safely"),QuestDraftEditOperation.description("New description"),
                QuestDraftEditOperation.taskLogic(QuestLogic.OR),QuestDraftEditOperation.replaceTask("task",
                    new TaskDefinition("task","test:task",true,Collections.<String,String>emptyMap()))));

        QuestDraftManagementResult edited=service.applyEdit(actor,request);

        assertTrue(edited.isSuccess());assertEquals("Edited safely",edited.getDefinition().getDefinition().getName());
        assertEquals("New description",edited.getDefinition().getDefinition().getDescription());
        assertEquals(QuestLogic.OR,edited.getDefinition().getDefinition().getTaskLogic());
        assertTrue(edited.getDefinition().getDefinition().getTasks().get(0).isOptional());
        assertEquals(before+1,repository.mutations);assertEquals(2,transaction.calls);
    }

    @Test
    public void rejectsInvalidEditWithoutPartialPersistence() {
        FakeRepository repository=new FakeRepository();QuestDraftManagementService service=service(repository,true);
        QuestDefinition original=definition("test:task");QuestDraftManagementResult created=service.createDraft(actor,original);
        int before=repository.mutations;
        QuestDraftEditRequest request=new QuestDraftEditRequest(original.getId(),1,created.getDefinition().getContentHash(),
            Arrays.asList(QuestDraftEditOperation.name("Would otherwise change"),QuestDraftEditOperation.removeTask("missing")));

        QuestDraftManagementResult result=service.applyEdit(actor,request);

        assertEquals(QuestDraftManagementResult.Status.INVALID,result.getStatus());assertEquals(before,repository.mutations);
        assertEquals("Quest",repository.stored.getDefinition().getName());
    }

    @Test public void appliesRepeatAndBehaviorOptionsAsOneValidatedBatch(){FakeRepository repository=new FakeRepository();QuestDraftManagementService service=service(repository,true);QuestDefinition original=definition("test:task");QuestDraftManagementResult created=service.createDraft(actor,original);QuestDraftManagementResult result=service.applyEdit(actor,new QuestDraftEditRequest(original.getId(),1,created.getDefinition().getContentHash(),Arrays.asList(QuestDraftEditOperation.option("repeat.cooldownMillis","3600000"),QuestDraftEditOperation.option("repeat.relative","false"),QuestDraftEditOperation.option("behavior.visibility","SECRET"),QuestDraftEditOperation.option("behavior.autoClaim","true"),QuestDraftEditOperation.option("behavior.simultaneous","true"))));assertTrue(result.isSuccess());QuestDefinition value=result.getDefinition().getDefinition();assertEquals(3600000L,value.getRepeatPolicy().getCooldownMillis());assertTrue(!value.getRepeatPolicy().isRelative());assertEquals(QuestVisibility.SECRET,value.getBehavior().getVisibility());assertTrue(value.getBehavior().isAutoClaim());assertTrue(value.getBehavior().isSimultaneous());}

    @Test public void rejectsUnknownBehaviorOptionWithoutPartialWrite(){FakeRepository repository=new FakeRepository();QuestDraftManagementService service=service(repository,true);QuestDefinition original=definition("test:task");QuestDraftManagementResult created=service.createDraft(actor,original);int before=repository.mutations;QuestDraftManagementResult result=service.applyEdit(actor,new QuestDraftEditRequest(original.getId(),1,created.getDefinition().getContentHash(),Collections.singletonList(QuestDraftEditOperation.option("behavior.clientAuthority","true"))));assertEquals(QuestDraftManagementResult.Status.INVALID,result.getStatus());assertEquals(before,repository.mutations);}

    private static QuestDraftManagementService service(FakeRepository repository, boolean allowed) {
        return service(repository, new CountingTransaction(), allowed);
    }

    private static QuestDraftManagementService service(FakeRepository repository, CountingTransaction transaction,
        final boolean allowed) {
        QuestElementTypeDescriptor task = new QuestElementTypeDescriptor("test:task", QuestElementKind.TASK,
            "Task", Collections.<QuestEditorFieldDescriptor>emptyList(), null);
        QuestElementTypeDescriptor reward = new QuestElementTypeDescriptor("test:reward", QuestElementKind.REWARD,
            "Reward", Collections.<QuestEditorFieldDescriptor>emptyList(), null);
        return new QuestDraftManagementService(repository, transaction, new QuestEditorAuthorization() {
            @Override public boolean canManageDefinitions(QuestEditorActor ignored) { return allowed; }
        }, new QuestEditorTypeRegistry(Arrays.asList(task, reward)));
    }

    private static QuestDefinition definition(String taskType) {
        return new QuestDefinition(UUID.randomUUID(), 1, "Quest", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.singletonList(new TaskDefinition("task", taskType, false,
                new LinkedHashMap<String, String>())),
            Collections.singletonList(new RewardDefinition("reward", "test:reward",
                new LinkedHashMap<String, String>())), RepeatPolicy.never());
    }

    private static final class CountingTransaction implements QuestTransaction {
        int calls;
        @Override public <T> T inTransaction(Work<T> work) { calls++; return work.execute(); }
    }

    private static final class FakeRepository implements QuestDefinitionRepository {
        StoredQuestDefinition stored;
        int mutations;

        @Override public StoredQuestDefinition createDraft(QuestDefinition definition) {
            mutations++;
            if (stored != null) throw new QuestDefinitionConflictException("duplicate");
            stored = new StoredQuestDefinition(definition, QuestDefinitionLifecycle.DRAFT, hash(definition), 0L);
            return stored;
        }

        @Override public StoredQuestDefinition updateDraft(QuestDefinition definition, String expectedHash) {
            mutations++;
            if (stored == null || stored.getLifecycle() != QuestDefinitionLifecycle.DRAFT
                || !stored.getContentHash().equals(expectedHash)) throw new QuestDefinitionConflictException("stale");
            stored = new StoredQuestDefinition(definition, QuestDefinitionLifecycle.DRAFT, hash(definition), 0L);
            return stored;
        }

        @Override public boolean publish(UUID questId, int version, String expectedHash, long publishedAt) {
            mutations++;
            if (stored == null || !stored.getDefinition().getId().equals(questId)
                || stored.getDefinition().getVersion() != version || !stored.getContentHash().equals(expectedHash)
                || stored.getLifecycle() != QuestDefinitionLifecycle.DRAFT) return false;
            stored = new StoredQuestDefinition(stored.getDefinition(), QuestDefinitionLifecycle.PUBLISHED,
                stored.getContentHash(), publishedAt);
            return true;
        }

        @Override public boolean retire(UUID questId, int version) {
            mutations++;
            if (stored == null || stored.getLifecycle() != QuestDefinitionLifecycle.PUBLISHED
                || !stored.getDefinition().getId().equals(questId) || stored.getDefinition().getVersion() != version) {
                return false;
            }
            stored = new StoredQuestDefinition(stored.getDefinition(), QuestDefinitionLifecycle.RETIRED,
                stored.getContentHash(), stored.getPublishedAt());
            return true;
        }
        @Override public Optional<StoredQuestDefinition> find(UUID questId, int version) {
            return stored != null && stored.getDefinition().getId().equals(questId)
                && stored.getDefinition().getVersion() == version ? Optional.of(stored) : Optional.empty();
        }
        @Override public Optional<StoredQuestDefinition> findPublished(UUID questId) {
            return stored != null && stored.getDefinition().getId().equals(questId)
                && stored.getLifecycle() == QuestDefinitionLifecycle.PUBLISHED
                ? Optional.of(stored) : Optional.<StoredQuestDefinition>empty();
        }
        @Override public List<StoredQuestDefinition> findAllPublished() {
            return stored != null && stored.getLifecycle() == QuestDefinitionLifecycle.PUBLISHED
                ? Collections.singletonList(stored) : Collections.<StoredQuestDefinition>emptyList();
        }
        private static String hash(QuestDefinition definition) { return "hash-" + definition.getName(); }
    }
}
