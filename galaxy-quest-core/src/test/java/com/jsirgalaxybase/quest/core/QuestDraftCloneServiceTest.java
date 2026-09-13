package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

public class QuestDraftCloneServiceTest {
    @Test public void authorizedBatchReadsExactVersionsAndCreatesAllDraftsInOneTransaction() {
        Repository repository = new Repository();
        QuestDefinition first = definition(UUID.randomUUID(), Collections.<UUID>emptySet());
        QuestDefinition second = definition(UUID.randomUUID(), Collections.singleton(first.getId()));
        repository.seed(first, QuestDefinitionLifecycle.PUBLISHED);
        repository.seed(second, QuestDefinitionLifecycle.DRAFT);
        final UUID copyFirst = UUID.randomUUID(), copySecond = UUID.randomUUID();
        CountingTransaction transaction = new CountingTransaction();
        QuestDraftManagementService service = service(repository, transaction, true,
            new QuestDefinitionBatchCloner.IdSource() { int next;
                @Override public UUID next() { return next++ == 0 ? copyFirst : copySecond; }
            });

        QuestDefinitionCloneResult result = service.cloneDrafts(new QuestEditorActor(UUID.randomUUID(), "op"),
            new QuestDefinitionCloneRequest(Arrays.asList(new QuestDefinitionCloneRequest.Source(first.getId(), 1),
                new QuestDefinitionCloneRequest.Source(second.getId(), 1))));

        assertTrue(result.isSuccess()); assertEquals(1, transaction.calls); assertEquals(2, result.getDefinitions().size());
        assertEquals(Collections.singleton(copyFirst), result.getDefinitions().get(1).getDefinition().getPrerequisites());
        assertEquals(QuestDefinitionLifecycle.DRAFT, result.getDefinitions().get(0).getLifecycle());
    }

    @Test public void authorizationAndMissingVersionFailBeforeTransaction() {
        Repository repository = new Repository(); CountingTransaction deniedTx = new CountingTransaction();
        QuestDefinition source = definition(UUID.randomUUID(), Collections.<UUID>emptySet()); repository.seed(source,
            QuestDefinitionLifecycle.PUBLISHED);
        QuestDefinitionCloneRequest request = new QuestDefinitionCloneRequest(Collections.singletonList(
            new QuestDefinitionCloneRequest.Source(source.getId(), 2)));
        assertEquals(QuestDefinitionCloneResult.Status.FORBIDDEN, service(repository, deniedTx, false,
            fixed(UUID.randomUUID())).cloneDrafts(new QuestEditorActor(UUID.randomUUID(), "guest"), request).getStatus());
        assertEquals(0, deniedTx.calls);
        CountingTransaction missingTx = new CountingTransaction();
        assertEquals(QuestDefinitionCloneResult.Status.NOT_FOUND, service(repository, missingTx, true,
            fixed(UUID.randomUUID())).cloneDrafts(new QuestEditorActor(UUID.randomUUID(), "op"), request).getStatus());
        assertEquals(0, missingTx.calls);
    }

    private static QuestDraftManagementService service(Repository repository, CountingTransaction transaction,
        final boolean allowed, QuestDefinitionBatchCloner.IdSource ids) {
        QuestElementTypeDescriptor task = new QuestElementTypeDescriptor("bq_standard:checkbox",
            QuestElementKind.TASK, "Checkbox", Collections.<QuestEditorFieldDescriptor>emptyList(), null);
        return new QuestDraftManagementService(repository, transaction, new QuestEditorAuthorization() {
            @Override public boolean canManageDefinitions(QuestEditorActor actor) { return allowed; }
        }, new QuestEditorTypeRegistry(Collections.singletonList(task)), ids);
    }

    private static QuestDefinition definition(UUID id, java.util.Set<UUID> prerequisites) {
        return new QuestDefinition(id, 1, "Quest", "", QuestLogic.AND, QuestLogic.AND, prerequisites,
            Collections.singletonList(new TaskDefinition("check", "bq_standard:checkbox", false,
                Collections.<String, String>emptyMap())), Collections.<RewardDefinition>emptyList());
    }

    private static QuestDefinitionBatchCloner.IdSource fixed(final UUID id) {
        return new QuestDefinitionBatchCloner.IdSource() { @Override public UUID next() { return id; } };
    }

    private static final class CountingTransaction implements QuestTransaction {
        private int calls;
        @Override public <T> T inTransaction(Work<T> work) { calls++; return work.execute(); }
    }

    private static final class Repository implements QuestDefinitionRepository {
        private final Map<String, StoredQuestDefinition> values = new LinkedHashMap<String, StoredQuestDefinition>();
        private void seed(QuestDefinition definition, QuestDefinitionLifecycle lifecycle) {
            values.put(key(definition.getId(), definition.getVersion()), new StoredQuestDefinition(definition,
                lifecycle, "hash-" + definition.getId(), 0L));
        }
        @Override public StoredQuestDefinition createDraft(QuestDefinition definition) {
            String key = key(definition.getId(), definition.getVersion());
            if (values.containsKey(key)) throw new QuestDefinitionConflictException("duplicate");
            StoredQuestDefinition stored = new StoredQuestDefinition(definition, QuestDefinitionLifecycle.DRAFT,
                "hash-" + definition.getId(), 0L); values.put(key, stored); return stored;
        }
        @Override public Optional<StoredQuestDefinition> find(UUID id, int version) {
            return Optional.ofNullable(values.get(key(id, version)));
        }
        @Override public Optional<StoredQuestDefinition> findPublished(UUID id) {
            for (StoredQuestDefinition value : values.values()) if (value.getDefinition().getId().equals(id)
                && value.getLifecycle() == QuestDefinitionLifecycle.PUBLISHED) return Optional.of(value);
            return Optional.empty();
        }
        @Override public List<StoredQuestDefinition> findAllPublished() {
            List<StoredQuestDefinition> result = new ArrayList<StoredQuestDefinition>();
            for (StoredQuestDefinition value : values.values()) if (value.getLifecycle() == QuestDefinitionLifecycle.PUBLISHED)
                result.add(value); return result;
        }
        @Override public StoredQuestDefinition updateDraft(QuestDefinition d, String h) { throw new UnsupportedOperationException(); }
        @Override public boolean publish(UUID id, int v, String h, long at) { throw new UnsupportedOperationException(); }
        @Override public boolean retire(UUID id, int v) { throw new UnsupportedOperationException(); }
        private static String key(UUID id, int version) { return id + ":" + version; }
    }
}
