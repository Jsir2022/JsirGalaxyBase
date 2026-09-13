package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

public class ConsumptionSubmissionServiceTest {
    @Test
    public void retriesUseStableExternalKeyAndConfirmProgressOnce() {
        UUID player = UUID.randomUUID();
        Map<String, String> parameters = map("progressMode", "item-vector", "item.count", "1",
            "item.0.amount", "3", "target", "3", "consume", "true");
        TaskDefinition task = new TaskDefinition("submit", "bq_standard:retrieval", false, parameters);
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Submit", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task),
            Collections.<RewardDefinition>emptyList());
        RuntimeRepository runtimeRepository = new RuntimeRepository();
        ReentrantTransaction transaction = new ReentrantTransaction();
        TaskEvaluatorRegistry evaluators = new TaskEvaluatorRegistry();
        evaluators.register(new ConsumptionAppliedTaskEvaluator("bq_standard:retrieval", "item"));
        QuestRuntimeService runtime = new QuestRuntimeService(runtimeRepository, transaction,
            new QuestFactProjector(evaluators), new QuestEngine());
        SubmissionRepository submissions = new SubmissionRepository();
        CountingPort port = new CountingPort();
        QuestEvaluationRequestProvider requests = fact -> Collections.singletonList(new QuestEvaluationRequest(quest,
            ParticipantMembership.player(player), Collections.<UUID>emptySet()));
        ConsumptionSubmissionService service = new ConsumptionSubmissionService(submissions, port, runtime,
            requests, transaction);
        ConsumptionRequestAuthorizer authorizer = new ConsumptionRequestAuthorizer(
            () -> Collections.singletonList(new StoredQuestDefinition(quest, QuestDefinitionLifecycle.PUBLISHED,
                "hash", 1L)), runtimeRepository, participant -> Collections.<UUID>emptySet(), new QuestEngine());
        ConsumptionIntent intent = new ConsumptionIntent("stable-1", "s2", player, quest.getId(), "submit", 10L);

        assertEquals(ConsumptionSubmissionStatus.CONFIRMED, service.submit(intent, authorizer, 10L).getStatus());
        assertEquals(ConsumptionSubmissionStatus.CONFIRMED, service.resume("stable-1", 11L).getStatus());
        assertEquals(1, port.calls);
        assertEquals(Collections.singletonList(3L), port.lastRequest.getAmounts());
        assertEquals("1", port.lastRequest.getResourceParameters().get("item.count"));
        assertEquals(1, runtimeRepository.factCount);
        assertTrue(runtimeRepository.progress.getTasks().get("submit").isComplete());
    }

    @Test
    public void insufficientResourcesAreRejectedWithoutProgressFact() {
        UUID player = UUID.randomUUID();
        SubmissionRepository submissions = new SubmissionRepository();
        CountingPort port = new CountingPort();
        port.sufficient = false;
        ReentrantTransaction transaction = new ReentrantTransaction();
        RuntimeRepository runtimeRepository = new RuntimeRepository();
        TaskEvaluatorRegistry evaluators = new TaskEvaluatorRegistry();
        evaluators.register(new ConsumptionAppliedTaskEvaluator("bq_standard:retrieval", "item"));
        QuestRuntimeService runtime = new QuestRuntimeService(runtimeRepository, transaction,
            new QuestFactProjector(evaluators), new QuestEngine());
        ConsumptionSubmissionService service = new ConsumptionSubmissionService(submissions, port, runtime,
            fact -> Collections.<QuestEvaluationRequest>emptyList(), transaction);
        UUID questId = UUID.randomUUID();
        Map<String, String> parameters = map("progressMode", "item-vector", "item.count", "1",
            "item.0.amount", "1", "target", "1", "consume", "true");
        QuestDefinition quest = new QuestDefinition(questId, 1, "Submit", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.singletonList(new TaskDefinition("submit",
                "bq_standard:retrieval", false, parameters)), Collections.<RewardDefinition>emptyList());
        ConsumptionRequestAuthorizer authorizer = new ConsumptionRequestAuthorizer(
            () -> Collections.singletonList(new StoredQuestDefinition(quest, QuestDefinitionLifecycle.PUBLISHED,
                "hash", 1L)), runtimeRepository, participant -> Collections.<UUID>emptySet(), new QuestEngine());
        ConsumptionIntent intent = new ConsumptionIntent("stable-2", "s2", player, questId, "submit", 10L);

        assertEquals(ConsumptionSubmissionStatus.REJECTED, service.submit(intent, authorizer, 10L).getStatus());
        assertEquals(0, runtimeRepository.factCount);
    }

    @Test public void actualPartialAmountIsPersistedAndProjectedInsteadOfRequestedMaximum() {
        UUID player = UUID.randomUUID();
        TaskDefinition task = new TaskDefinition("xp", "bq_standard:xp", false,
            map("target", "100", "consume", "true", "xpUnit", "points"));
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "XP", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task),
            Collections.<RewardDefinition>emptyList());
        RuntimeRepository progress = new RuntimeRepository();
        ReentrantTransaction transaction = new ReentrantTransaction();
        TaskEvaluatorRegistry evaluators = new TaskEvaluatorRegistry();
        evaluators.register(new ConsumptionAppliedTaskEvaluator("bq_standard:xp", "xp"));
        QuestRuntimeService runtime = new QuestRuntimeService(progress, transaction,
            new QuestFactProjector(evaluators), new QuestEngine());
        SubmissionRepository submissions = new SubmissionRepository();
        CountingPort port = new CountingPort();
        port.appliedAmounts = Collections.singletonList(40L);
        ConsumptionSubmissionService service = new ConsumptionSubmissionService(submissions, port, runtime,
            fact -> Collections.singletonList(new QuestEvaluationRequest(quest, ParticipantMembership.player(player),
                Collections.<UUID>emptySet())), transaction);
        ConsumptionRequestAuthorizer authorizer = new ConsumptionRequestAuthorizer(
            () -> Collections.singletonList(new StoredQuestDefinition(quest, QuestDefinitionLifecycle.PUBLISHED,
                "hash", 1L)), progress, participant -> Collections.<UUID>emptySet(), new QuestEngine());

        ConsumptionSubmission result = service.submit(new ConsumptionIntent("xp-partial", "s2", player,
            quest.getId(), "xp", 10L), authorizer, 10L);

        assertEquals(ConsumptionSubmissionStatus.CONFIRMED, result.getStatus());
        assertEquals(Collections.singletonList(40L), result.getAppliedAmounts());
        assertEquals(40L, progress.progress.getTasks().get("xp").getValue());
    }

    private static final class CountingPort implements ConsumptionPort {
        private int calls;
        private boolean sufficient = true;
        private ConsumptionRequest lastRequest;
        private java.util.List<Long> appliedAmounts = Collections.emptyList();
        @Override public ConsumptionApplyResult apply(ConsumptionRequest request) {
            calls++;
            lastRequest = request;
            return sufficient ? (appliedAmounts.isEmpty()
                ? ConsumptionApplyResult.applied("player-nbt:" + request.getSubmissionKey())
                : ConsumptionApplyResult.applied("player-nbt:" + request.getSubmissionKey(), appliedAmounts))
                : ConsumptionApplyResult.insufficient("insufficient inventory");
        }
    }

    private static final class SubmissionRepository implements ConsumptionSubmissionRepository {
        private ConsumptionSubmission value;
        @Override public ConsumptionSubmission prepareIfAbsent(ConsumptionRequest request) {
            if (value == null) value = new ConsumptionSubmission(request, ConsumptionSubmissionStatus.PREPARED, "");
            return value;
        }
        @Override public Optional<ConsumptionSubmission> find(String key) { return Optional.ofNullable(value); }
        @Override public ConsumptionSubmission markApplied(String key, String evidence, java.util.List<Long> amounts, long now) {
            value = new ConsumptionSubmission(value.getRequest(), ConsumptionSubmissionStatus.APPLIED, evidence, amounts);
            return value;
        }
        @Override public ConsumptionSubmission markConfirmed(String key, long now) {
            value = new ConsumptionSubmission(value.getRequest(), ConsumptionSubmissionStatus.CONFIRMED,
                value.getEvidence(), value.getAppliedAmounts());
            return value;
        }
        @Override public ConsumptionSubmission markRejected(String key, String reason, long now) {
            value = new ConsumptionSubmission(value.getRequest(), ConsumptionSubmissionStatus.REJECTED, reason);
            return value;
        }
    }

    private static final class ReentrantTransaction implements QuestTransaction {
        @Override public <T> T inTransaction(Work<T> work) { return work.execute(); }
    }

    private static final class RuntimeRepository implements QuestRuntimeRepository {
        private QuestProgressSnapshot progress;
        private int factCount;
        @Override public Optional<QuestProgressSnapshot> findProgress(ParticipantId participant, UUID questId,
            int version) { return Optional.ofNullable(progress); }
        @Override public boolean recordFactIfAbsent(GameplayFact fact) {
            if (factCount > 0) return false;
            factCount++;
            return true;
        }
        @Override public void saveProgress(QuestProgressSnapshot value) { progress = value; }
        @Override public void insertEntitlementsIfAbsent(Set<RewardEntitlement> values) {}
    }

    private static Map<String, String> map(String... values) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int i = 0; i < values.length; i += 2) result.put(values[i], values[i + 1]);
        return result;
    }
}
