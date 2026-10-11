package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

public class AuthenticatedQuestConsumptionDetectServiceTest {
    @Test
    public void questDetectProcessesEveryConsumingTaskInOrderAndSettlesOnlyAfterTheBatch() {
        Fixture fixture = new Fixture();
        fixture.port.failOnCall = 2;

        try {
            fixture.detect.detect(fixture.player, fixture.quest.getId(), 100L);
            fail("the injected second inventory operation should interrupt the batch");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("injected"));
        }

        assertTrue("unfinished batch remains durable after a task failure", fixture.state.activeBatch != null);
        assertEquals(1, fixture.state.activeBatch.getNextTaskIndex());
        assertEquals("OR completion must remain deferred while the batch is active", QuestStatus.IN_PROGRESS,
            fixture.state.progress.getStatus());
        assertEquals(0, fixture.state.entitlements.size());

        ConsumptionDetectBatchResult resumed = fixture.detect.detect(fixture.player, fixture.quest.getId(), 101L);

        assertEquals(ConsumptionDetectBatchResult.Status.COMPLETED, resumed.getStatus());
        assertEquals(3, fixture.port.taskKeys.size());
        assertEquals(Arrays.asList("retrieve-a", "retrieve-b", "retrieve-b"), fixture.port.taskKeys);
        assertEquals(fixture.port.submissionKeys.get(1), fixture.port.submissionKeys.get(2));
        assertFalse("batch fence is removed only with final Quest settlement", fixture.state.hasActiveBatch());
        assertEquals(QuestStatus.COMPLETED, fixture.state.progress.getStatus());
        assertTrue(fixture.state.progress.getTasks().get("retrieve-a").isComplete());
        assertTrue(fixture.state.progress.getTasks().get("retrieve-b").isComplete());
        assertEquals(1, fixture.state.entitlements.size());
    }

    @Test
    public void activeBatchCanOnlyResumeOnItsOriginatingServer() {
        Fixture fixture = new Fixture();
        fixture.state.activeBatch = new ConsumptionDetectBatch(UUID.randomUUID().toString(), "lobby",
            fixture.player, ParticipantId.player(fixture.player), fixture.quest.getId(), 1, 0,
            Arrays.asList("retrieve-a", "retrieve-b"), 1, 90L);

        ConsumptionDetectBatchResult result = fixture.detect.detect(fixture.player, fixture.quest.getId(), 100L);

        assertEquals(ConsumptionDetectBatchResult.Status.WAITING_FOR_SOURCE_SERVER, result.getStatus());
        assertEquals("lobby", result.getSourceServer());
        assertEquals(0, fixture.port.calls);
        assertEquals(1, fixture.state.activeBatch.getNextTaskIndex());
    }

    @Test
    public void publishingNewDefinitionDoesNotRewriteAnActiveDetectBatchOrItsReward() {
        Fixture fixture = new Fixture();
        fixture.port.failOnCall = 2;
        String originalBatchId;
        try {
            fixture.detect.detect(fixture.player, fixture.quest.getId(), 100L);
            fail("the injected second inventory operation should interrupt the batch");
            return;
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("injected"));
            originalBatchId = fixture.state.activeBatch.getBatchId();
        }

        QuestDefinition replacement = new QuestDefinition(fixture.quest.getId(), 2, "Updated Detect", "",
            QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(),
            Collections.singletonList(task("new-retrieval")),
            Collections.singletonList(new RewardDefinition("replacement-reward", "item",
                Collections.<String, String>emptyMap())), RepeatPolicy.never());
        fixture.catalog.publish(replacement);

        ConsumptionDetectBatchResult resumed = fixture.detect.detect(fixture.player, fixture.quest.getId(), 101L);

        assertEquals(ConsumptionDetectBatchResult.Status.COMPLETED, resumed.getStatus());
        assertEquals("the original durable operation remains pinned", originalBatchId, resumed.getBatchId());
        assertEquals(Arrays.asList("retrieve-a", "retrieve-b", "retrieve-b"), fixture.port.taskKeys);
        assertEquals(Arrays.asList(1, 1, 1), fixture.port.definitionVersions);
        assertEquals("the old version's progress and reward contract settle", 1,
            fixture.state.progress.getDefinitionVersion());
        assertEquals("reward", fixture.state.entitlements.iterator().next().getReward().getKey());
    }

    @Test
    public void questDetectDoesNotStartRepeatCycleBeforeRewardClaimCooldown() {
        UUID player = UUID.randomUUID();
        TaskDefinition consuming = task("retrieve");
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Repeatable Detect", "",
            QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(consuming),
            Collections.<RewardDefinition>emptyList(), RepeatPolicy.after(100L));
        State state = new State();
        state.progress = new QuestProgressSnapshot(ParticipantId.player(player), quest.getId(), quest.getVersion(),
            QuestStatus.COMPLETED, Collections.singletonMap("retrieve", new TaskProgress("retrieve", 1L, 1L, true)),
            100L, 0);
        state.claimableReward = true;
        ConsumptionRequestAuthorizer authorizer = new ConsumptionRequestAuthorizer(
            () -> Collections.singletonList(new StoredQuestDefinition(quest, QuestDefinitionLifecycle.PUBLISHED,
                "hash", 1L)), state, participant -> Collections.<UUID>emptySet(), new QuestEngine());

        try {
            authorizer.planQuestDetect(player, quest.getId(), 10_000L);
            fail("a repeat cycle must remain frozen while the previous reward is claimable");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("not accepting consumption"));
        }
    }

    private static final class Fixture {
        private final UUID player = UUID.randomUUID();
        private final TaskDefinition first = task("retrieve-a");
        private final TaskDefinition second = task("retrieve-b");
        private final QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Consuming Detect", "",
            QuestLogic.AND, QuestLogic.OR, Collections.<UUID>emptySet(), Arrays.asList(first, second),
            Collections.singletonList(new RewardDefinition("reward", "item", Collections.<String, String>emptyMap())));
        private final State state = new State();
        private final MutableCatalog catalog = new MutableCatalog();
        private final Transaction transaction = new Transaction();
        private final SubmissionStore submissionStore = new SubmissionStore();
        private final RecordingPort port = new RecordingPort();
        private final QuestRuntimeService runtime;
        private final ConsumptionRequestAuthorizer authorizer;
        private final AuthenticatedQuestConsumptionDetectService detect;

        private Fixture() {
            catalog.publish(quest);
            TaskEvaluatorRegistry evaluators = new TaskEvaluatorRegistry();
            evaluators.register(new ConsumptionAppliedTaskEvaluator("bq_standard:retrieval", "item"));
            QuestCompletionQuery completions = participant -> Collections.<UUID>emptySet();
            ParticipantMembershipResolver memberships = playerId -> Collections.singletonList(
                ParticipantMembership.player(playerId));
            QuestAssignmentPolicy assignments = new ScopedQuestAssignmentPolicy();
            QuestEvaluationRequestProvider requests = new QuestEvaluationPlanner(catalog, completions,
                memberships, assignments);
            runtime = new QuestRuntimeService(state, transaction, new QuestFactProjector(evaluators),
                new QuestEngine());
            authorizer = new ConsumptionRequestAuthorizer(catalog, state, completions, new QuestEngine(),
                memberships, assignments);
            ConsumptionSubmissionService submissions = new ConsumptionSubmissionService(submissionStore, port,
                runtime, requests, transaction);
            detect = new AuthenticatedQuestConsumptionDetectService("s2", state, authorizer, submissions, runtime,
                requests, transaction);
        }

        private QuestEvaluationRequest evaluationRequest() {
            return new QuestEvaluationRequest(quest, ParticipantMembership.player(player),
                Collections.<UUID>emptySet());
        }
    }

    private static TaskDefinition task(String key) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("progressMode", "item-vector");
        parameters.put("item.count", "1");
        parameters.put("item.0.registryName", "minecraft:iron_ingot");
        parameters.put("item.0.meta", "0");
        parameters.put("item.0.amount", "1");
        parameters.put("target", "1");
        parameters.put("consume", "true");
        parameters.put("autoConsume", "false");
        return new TaskDefinition(key, "bq_standard:retrieval", false, parameters);
    }

    private static final class RecordingPort implements ConsumptionPort {
        private int calls;
        private int failOnCall = -1;
        private final List<String> taskKeys = new ArrayList<String>();
        private final List<String> submissionKeys = new ArrayList<String>();
        private final List<Integer> definitionVersions = new ArrayList<Integer>();
        @Override public ConsumptionApplyResult apply(ConsumptionRequest request) {
            calls++;
            taskKeys.add(request.getTaskKey());
            submissionKeys.add(request.getSubmissionKey());
            definitionVersions.add(request.getDefinitionVersion());
            if (calls == failOnCall) throw new IllegalStateException("injected inventory-side interruption");
            return ConsumptionApplyResult.applied("applied:" + request.getSubmissionKey());
        }
    }

    private static final class SubmissionStore implements ConsumptionSubmissionRepository {
        private final Map<String, ConsumptionSubmission> values = new HashMap<String, ConsumptionSubmission>();
        @Override public ConsumptionSubmission prepareIfAbsent(ConsumptionRequest request) {
            ConsumptionSubmission existing = values.get(request.getSubmissionKey());
            if (existing != null) return existing;
            ConsumptionSubmission created = new ConsumptionSubmission(request, ConsumptionSubmissionStatus.PREPARED, "");
            values.put(request.getSubmissionKey(), created);
            return created;
        }
        @Override public Optional<ConsumptionSubmission> find(String key) {
            return Optional.ofNullable(values.get(key));
        }
        @Override public List<ConsumptionSubmission> findPendingForPlayer(UUID playerId, String sourceServer,
            int limit) { return Collections.emptyList(); }
        @Override public ConsumptionSubmission markApplied(String key, String evidence, List<Long> appliedAmounts,
            long now) {
            ConsumptionSubmission current = values.get(key);
            ConsumptionSubmission updated = new ConsumptionSubmission(current.getRequest(),
                ConsumptionSubmissionStatus.APPLIED, evidence, appliedAmounts);
            values.put(key, updated);
            return updated;
        }
        @Override public ConsumptionSubmission markConfirmed(String key, long now) {
            ConsumptionSubmission current = values.get(key);
            ConsumptionSubmission updated = new ConsumptionSubmission(current.getRequest(),
                ConsumptionSubmissionStatus.CONFIRMED, current.getEvidence(), current.getAppliedAmounts());
            values.put(key, updated);
            return updated;
        }
        @Override public ConsumptionSubmission markRejected(String key, String reason, long now) {
            ConsumptionSubmission current = values.get(key);
            ConsumptionSubmission updated = new ConsumptionSubmission(current.getRequest(),
                ConsumptionSubmissionStatus.REJECTED, reason);
            values.put(key, updated);
            return updated;
        }
    }

    private static final class State implements QuestRuntimeRepository, ConsumptionDetectBatchRepository {
        private QuestProgressSnapshot progress;
        private final Set<String> facts = new HashSet<String>();
        private final Set<RewardEntitlement> entitlements = new HashSet<RewardEntitlement>();
        private ConsumptionDetectBatch activeBatch;
        private boolean claimableReward;
        @Override public Optional<QuestProgressSnapshot> findProgress(ParticipantId participant, UUID questId,
            int version) { return Optional.ofNullable(progress); }
        @Override public boolean hasPendingConsumptionSubmission(ParticipantId participant, UUID questId,
            int version, int cycle) { return false; }
        @Override public boolean hasActiveConsumptionDetectBatch(ParticipantId participant, UUID questId,
            int version) { return hasActiveBatch(); }
        @Override public boolean hasClaimableRewardEntitlement(ParticipantId participant, UUID questId,
            int version, int cycle) { return claimableReward; }
        @Override public long findRepeatCycleAnchorMillis(ParticipantId participant, UUID questId, int version,
            int cycle, long fallbackCompletedAt) { return fallbackCompletedAt; }
        @Override public boolean recordFactIfAbsent(GameplayFact fact) { return facts.add(fact.getFactKey()); }
        @Override public void saveProgress(QuestProgressSnapshot value) { progress = value; }
        @Override public void insertEntitlementsIfAbsent(Set<RewardEntitlement> values) { entitlements.addAll(values); }
        @Override public Optional<ConsumptionDetectBatch> findActive(UUID playerId, UUID questId) {
            return activeBatch != null && activeBatch.getPlayerId().equals(playerId)
                && activeBatch.getQuestId().equals(questId)
                    ? Optional.of(activeBatch) : Optional.<ConsumptionDetectBatch>empty();
        }
        @Override public ConsumptionDetectBatch createIfAbsent(ConsumptionDetectBatch candidate) {
            if (activeBatch == null) activeBatch = candidate;
            return activeBatch;
        }
        @Override public ConsumptionDetectBatch advance(String batchId, int expectedTaskIndex, long updatedAt) {
            if (activeBatch == null || !activeBatch.getBatchId().equals(batchId)) throw new IllegalStateException(
                "missing active batch");
            if (activeBatch.getNextTaskIndex() <= expectedTaskIndex) activeBatch = new ConsumptionDetectBatch(
                activeBatch.getBatchId(), activeBatch.getSourceServer(), activeBatch.getPlayerId(),
                activeBatch.getParticipantId(), activeBatch.getQuestId(), activeBatch.getDefinitionVersion(),
                activeBatch.getCycle(), activeBatch.getTaskKeys(), expectedTaskIndex + 1,
                activeBatch.getCreatedAt());
            return activeBatch;
        }
        @Override public void deleteCompleted(String batchId, int expectedTaskCount, long updatedAt) {
            if (activeBatch == null || !activeBatch.getBatchId().equals(batchId) || !activeBatch.isComplete()) {
                throw new IllegalStateException("batch is not complete");
            }
            activeBatch = null;
        }
        private boolean hasActiveBatch() { return activeBatch != null; }
    }

    private static final class Transaction implements QuestTransaction {
        @Override public <T> T inTransaction(Work<T> work) { return work.execute(); }
    }

    private static final class MutableCatalog implements PublishedQuestCatalog {
        private final Map<String, StoredQuestDefinition> published = new LinkedHashMap<String, StoredQuestDefinition>();

        private void publish(QuestDefinition definition) {
            published.put(definition.getId() + ":" + definition.getVersion(), new StoredQuestDefinition(definition,
                QuestDefinitionLifecycle.PUBLISHED, "hash-" + definition.getVersion(), definition.getVersion()));
        }

        @Override public List<StoredQuestDefinition> findAllPublished() {
            Map<UUID, StoredQuestDefinition> latest = new LinkedHashMap<UUID, StoredQuestDefinition>();
            for (StoredQuestDefinition stored : published.values()) {
                UUID id = stored.getDefinition().getId();
                StoredQuestDefinition previous = latest.get(id);
                if (previous == null || previous.getDefinition().getVersion() < stored.getDefinition().getVersion()) {
                    latest.put(id, stored);
                }
            }
            return new ArrayList<StoredQuestDefinition>(latest.values());
        }

        @Override public Optional<StoredQuestDefinition> findPublished(UUID questId, int version) {
            return Optional.ofNullable(published.get(questId + ":" + version));
        }
    }
}
