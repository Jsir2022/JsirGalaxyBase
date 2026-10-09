package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

public class AuthenticatedCheckboxConfirmationServiceTest {
    @Test
    public void authenticatedClickCompletesOnceAndRejectsOtherTasks() {
        Fixture fixture = new Fixture(RepeatPolicy.never());
        assertEquals(FactProcessingStatus.IGNORED, fixture.confirm("other", 100L).getStatus());
        assertEquals(FactProcessingStatus.ACCEPTED, fixture.confirm("check", 100L).getStatus());
        assertEquals(FactProcessingStatus.IGNORED, fixture.confirm("check", 101L).getStatus());
        assertEquals(1, fixture.repository.facts.size());
        assertEquals(1, fixture.repository.entitlements.size());
        assertEquals(QuestStatus.COMPLETED, fixture.repository.current.getStatus());
    }

    @Test
    public void repeatableCheckboxUsesNewStableFactOnlyAfterCooldown() {
        Fixture fixture = new Fixture(RepeatPolicy.after(10L));
        assertEquals(FactProcessingStatus.ACCEPTED, fixture.confirm("check", 100L).getStatus());
        assertEquals(FactProcessingStatus.IGNORED, fixture.confirm("check", 109L).getStatus());
        assertEquals(FactProcessingStatus.ACCEPTED, fixture.confirm("check", 110L).getStatus());
        assertEquals(FactProcessingStatus.IGNORED, fixture.confirm("check", 111L).getStatus());
        assertEquals(2, fixture.repository.facts.size());
        assertEquals(2, fixture.repository.entitlements.size());
        assertEquals(1, fixture.repository.current.getCycle());
    }

    @Test
    public void lockedCheckboxDoesNotConsumeFactKeyAndCanBeConfirmedAfterUnlock() {
        Fixture fixture = new Fixture(RepeatPolicy.never(), true);
        assertEquals(FactProcessingStatus.IGNORED, fixture.confirm("check", 100L).getStatus());
        assertTrue(fixture.repository.facts.isEmpty());

        fixture.prerequisitesSatisfied = true;
        assertEquals(FactProcessingStatus.ACCEPTED, fixture.confirm("check", 101L).getStatus());
        assertEquals(1, fixture.repository.facts.size());
        assertEquals(QuestStatus.COMPLETED, fixture.repository.current.getStatus());
    }

    @Test
    public void explicitProgressWhileLockedDefinitionCanConfirm() {
        Fixture fixture = new Fixture(RepeatPolicy.never(), true,
            QuestBehavior.builder().progressWhileLocked(true).build());
        assertEquals(FactProcessingStatus.ACCEPTED, fixture.confirm("check", 100L).getStatus());
        assertEquals(1, fixture.repository.facts.size());
        assertEquals(QuestStatus.COMPLETED, fixture.repository.current.getStatus());
    }

    private static final class Fixture {
        private final UUID player = UUID.randomUUID();
        private final QuestDefinition definition;
        private final MemoryRuntimeRepository repository = new MemoryRuntimeRepository();
        private final AuthenticatedCheckboxConfirmationService service;
        private final UUID prerequisite = UUID.randomUUID();
        private boolean prerequisitesSatisfied;

        private Fixture(RepeatPolicy repeat) {
            this(repeat, false, QuestBehavior.DEFAULT);
        }

        private Fixture(RepeatPolicy repeat, boolean locked) {
            this(repeat, locked, QuestBehavior.DEFAULT);
        }

        private Fixture(RepeatPolicy repeat, boolean locked, QuestBehavior behavior) {
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("target", "1");
            parameters.put("factType", "galaxy:task_confirmed");
            TaskDefinition task = new TaskDefinition("check", "bq_standard:checkbox", false, parameters);
            RewardDefinition reward = new RewardDefinition("reward", "bq_standard:item",
                Collections.<String, String>emptyMap());
            definition = new QuestDefinition(UUID.randomUUID(), 1, "Check", "", QuestLogic.AND, QuestLogic.AND,
                locked ? Collections.singleton(prerequisite) : Collections.<UUID>emptySet(),
                Collections.singletonList(task), Collections.singletonList(reward), repeat, behavior);
            QuestDefinitionRepository definitions = new ReadOnlyDefinitionRepository(definition);
            TaskEvaluatorRegistry evaluators = new TaskEvaluatorRegistry();
            evaluators.register(new ConfirmationTaskEvaluator("bq_standard:checkbox", "galaxy:task_confirmed"));
            QuestRuntimeService runtime = new QuestRuntimeService(repository, new DirectTransaction(),
                new QuestFactProjector(evaluators), new QuestEngine());
            QuestEvaluationRequestProvider requests = fact -> Collections.singletonList(
                new QuestEvaluationRequest(definition, ParticipantMembership.player(fact.getPlayerId()),
                    locked && prerequisitesSatisfied ? Collections.singleton(prerequisite)
                        : Collections.<UUID>emptySet()));
            service = new AuthenticatedCheckboxConfirmationService(definitions, repository, runtime, requests, "s2");
        }

        private FactProcessingResult confirm(String key, long now) {
            return service.confirm(player, definition.getId(), key, now);
        }
    }

    private static final class MemoryRuntimeRepository implements QuestRuntimeRepository {
        private QuestProgressSnapshot current;
        private final Set<String> facts = new HashSet<String>();
        private final List<RewardEntitlement> entitlements = new ArrayList<RewardEntitlement>();
        @Override public Optional<QuestProgressSnapshot> findProgress(ParticipantId participant, UUID questId,
            int version) { return current == null ? Optional.empty() : Optional.of(current); }
        @Override public boolean hasPendingConsumptionSubmission(ParticipantId participant, UUID questId,
            int version, int cycle) { return false; }
        @Override public boolean hasClaimableRewardEntitlement(ParticipantId participant, UUID questId,
            int version, int cycle) { return false; }
        @Override public long findRepeatCycleAnchorMillis(ParticipantId participant, UUID questId, int version,
            int cycle, long fallbackCompletedAt) { return fallbackCompletedAt; }
        @Override public boolean recordFactIfAbsent(GameplayFact fact) { return facts.add(fact.getFactKey()); }
        @Override public void saveProgress(QuestProgressSnapshot value) { current = value; }
        @Override public void insertEntitlementsIfAbsent(Set<RewardEntitlement> values) { entitlements.addAll(values); }
    }

    private static final class DirectTransaction implements QuestTransaction {
        @Override public <T> T inTransaction(Work<T> work) { return work.execute(); }
    }

    private static final class ReadOnlyDefinitionRepository implements QuestDefinitionRepository {
        private final StoredQuestDefinition value;
        private ReadOnlyDefinitionRepository(QuestDefinition definition) {
            value = new StoredQuestDefinition(definition, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L);
        }
        @Override public StoredQuestDefinition createDraft(QuestDefinition definition) { throw new UnsupportedOperationException(); }
        @Override public StoredQuestDefinition updateDraft(QuestDefinition definition, String hash) { throw new UnsupportedOperationException(); }
        @Override public boolean publish(UUID id, int version, String hash, long at) { throw new UnsupportedOperationException(); }
        @Override public boolean retire(UUID id, int version) { throw new UnsupportedOperationException(); }
        @Override public Optional<StoredQuestDefinition> find(UUID id, int version) { return Optional.empty(); }
        @Override public Optional<StoredQuestDefinition> findPublished(UUID id) {
            return value.getDefinition().getId().equals(id) ? Optional.of(value) : Optional.empty();
        }
        @Override public List<StoredQuestDefinition> findAllPublished() { return Collections.singletonList(value); }
    }
}
