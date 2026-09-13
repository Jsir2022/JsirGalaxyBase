package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

public class QuestRuntimeServiceTest {
    @Test
    public void appliesFactProgressAndEntitlementOnceInsideTransaction() {
        UUID player = UUID.randomUUID();
        Map<String, String> parameters = map("target", "1", "factType", "minecraft:kill", "subject", "Zombie");
        TaskDefinition task = new TaskDefinition("hunt", "count", false, parameters);
        RewardDefinition reward = new RewardDefinition("reward", "item", Collections.<String, String>emptyMap());
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Hunt", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task),
            Collections.singletonList(reward), RepeatPolicy.never());
        TaskEvaluatorRegistry registry = new TaskEvaluatorRegistry();
        registry.register(new FactCountTaskEvaluator("count"));
        FakeRepository repository = new FakeRepository();
        CountingTransaction transaction = new CountingTransaction();
        QuestRuntimeService service = new QuestRuntimeService(repository, transaction,
            new QuestFactProjector(registry), new QuestEngine());
        GameplayFact fact = new GameplayFact("s2", "kill-1", player, "minecraft:kill", 10L,
            map("subject", "Zombie", "amount", "1"));
        List<QuestEvaluationRequest> requests = Collections.singletonList(new QuestEvaluationRequest(quest,
            ParticipantMembership.player(player), Collections.<UUID>emptySet()));

        FactProcessingResult first = service.apply(fact, requests, 10L);
        FactProcessingResult duplicate = service.apply(fact, requests, 11L);

        assertTrue(first.isAccepted());
        assertEquals(QuestStatus.COMPLETED, first.getEvaluations().get(0).getProgress().getStatus());
        assertEquals(1, repository.entitlements.size());
        assertFalse(duplicate.isAccepted());
        assertTrue(duplicate.getEvaluations().isEmpty());
        assertEquals(2, transaction.calls);
    }

    @Test
    public void resolvesPlanInsideTransactionAndIgnoresIrrelevantFactWithoutPersistingIt() {
        FakeRepository repository = new FakeRepository();
        CountingTransaction transaction = new CountingTransaction();
        TaskEvaluatorRegistry registry = new TaskEvaluatorRegistry();
        registry.register(new FactCountTaskEvaluator("count"));
        QuestRuntimeService service = new QuestRuntimeService(repository, transaction,
            new QuestFactProjector(registry), new QuestEngine());
        GameplayFact fact = new GameplayFact("s2", "irrelevant", UUID.randomUUID(), "other", 1L,
            Collections.<String, String>emptyMap());

        FactProcessingResult result = service.apply(fact, value -> {
            assertTrue(transaction.active);
            return Collections.emptyList();
        }, 1L);

        assertEquals(FactProcessingStatus.IGNORED, result.getStatus());
        assertTrue(repository.facts.isEmpty());
    }

    private static Map<String, String> map(String... values) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int i = 0; i < values.length; i += 2) result.put(values[i], values[i + 1]);
        return result;
    }

    private static final class CountingTransaction implements QuestTransaction {
        private int calls;
        private boolean active;
        @Override public <T> T inTransaction(Work<T> work) {
            calls++;
            active = true;
            try { return work.execute(); } finally { active = false; }
        }
    }

    private static final class FakeRepository implements QuestRuntimeRepository {
        private final Set<String> facts = new HashSet<String>();
        private final Map<String, QuestProgressSnapshot> progress = new LinkedHashMap<String, QuestProgressSnapshot>();
        private final List<RewardEntitlement> entitlements = new ArrayList<RewardEntitlement>();

        @Override public Optional<QuestProgressSnapshot> findProgress(ParticipantId participant, UUID questId,
            int version) {
            return Optional.ofNullable(progress.get(participant + ":" + questId + ":" + version));
        }
        @Override public boolean recordFactIfAbsent(GameplayFact fact) { return facts.add(fact.getFactKey()); }
        @Override public void saveProgress(QuestProgressSnapshot value) {
            progress.put(value.getParticipantId() + ":" + value.getQuestId() + ":" + value.getDefinitionVersion(), value);
        }
        @Override public void insertEntitlementsIfAbsent(Set<RewardEntitlement> values) { entitlements.addAll(values); }
    }
}
