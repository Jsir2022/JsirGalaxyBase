package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class QuestRuntimeService {
    private final QuestRuntimeRepository repository;
    private final QuestTransaction transaction;
    private final QuestFactProjector projector;
    private final QuestEngine engine;
    private final RewardSuppressionPolicy rewardSuppressionPolicy;

    public QuestRuntimeService(QuestRuntimeRepository repository, QuestTransaction transaction,
        QuestFactProjector projector, QuestEngine engine) {
        this(repository, transaction, projector, engine, new RewardSuppressionPolicy(false));
    }

    public QuestRuntimeService(QuestRuntimeRepository repository, QuestTransaction transaction,
        QuestFactProjector projector, QuestEngine engine, RewardSuppressionPolicy rewardSuppressionPolicy) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.transaction = Objects.requireNonNull(transaction, "transaction");
        this.projector = Objects.requireNonNull(projector, "projector");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.rewardSuppressionPolicy = Objects.requireNonNull(rewardSuppressionPolicy, "rewardSuppressionPolicy");
    }

    public FactProcessingResult apply(final GameplayFact fact, final List<QuestEvaluationRequest> requests,
        final long now) {
        return apply(fact, requests, null, now);
    }

    /** Applies a server-captured event-time eligibility snapshot while retaining normal unlock reconciliation. */
    public FactProcessingResult apply(final GameplayFact fact, final List<QuestEvaluationRequest> requests,
        final QuestEvaluationRequestProvider provider, final long now) {
        Objects.requireNonNull(fact, "fact");
        if (requests == null || requests.isEmpty()) throw new IllegalArgumentException("requests must not be empty");
        return transaction.inTransaction(new QuestTransaction.Work<FactProcessingResult>() {
            @Override
            public FactProcessingResult execute() {
                List<QuestEvaluationRequest> snapshot = Collections.unmodifiableList(
                    new ArrayList<QuestEvaluationRequest>(requests));
                return QuestRuntimeService.this.execute(fact, snapshot, provider, now);
            }
        });
    }

    public FactProcessingResult apply(final GameplayFact fact, final QuestEvaluationRequestProvider provider,
        final long now) {
        return applyIf(fact, provider, requests -> true, now);
    }

    /**
     * Evaluates a server-side acceptance rule against the same request snapshot used for projection, before the
     * idempotency fact is persisted. This prevents rejected facts from consuming their stable event key.
     */
    public FactProcessingResult applyIf(final GameplayFact fact, final QuestEvaluationRequestProvider provider,
        final Predicate<List<QuestEvaluationRequest>> acceptance, final long now) {
        Objects.requireNonNull(fact, "fact");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(acceptance, "acceptance");
        return transaction.inTransaction(() -> {
            List<QuestEvaluationRequest> requests = provider.requestsFor(fact);
            if (requests == null || requests.isEmpty()) return FactProcessingResult.ignored();
            List<QuestEvaluationRequest> snapshot = Collections.unmodifiableList(
                new ArrayList<QuestEvaluationRequest>(requests));
            if (!acceptance.test(snapshot)) return FactProcessingResult.ignored();
            return execute(fact, snapshot, provider, now);
        });
    }

    /** Rechecks only persisted PLAYER locked-progress rows, matching BQ's periodic passive quest update. */
    public FactProcessingResult reconcileLockedProgress(final ParticipantId player,
        final QuestEvaluationRequestProvider provider, final long now) {
        Objects.requireNonNull(provider, "provider");
        if (player == null || player.getType() != ParticipantType.PLAYER) return FactProcessingResult.ignored();
        return transaction.inTransaction(() -> {
            List<QuestProgressSnapshot> locked = repository.findLockedProgress(player);
            if (locked == null || locked.isEmpty()) return FactProcessingResult.ignored();
            Set<UUID> lockedQuestIds = new LinkedHashSet<UUID>();
            Map<String, QuestProgressSnapshot> byIdentity = new HashMap<String, QuestProgressSnapshot>();
            for (QuestProgressSnapshot progress : locked) {
                if (!player.equals(progress.getParticipantId()) || progress.getStatus() != QuestStatus.LOCKED) continue;
                lockedQuestIds.add(progress.getQuestId());
                byIdentity.put(progressIdentity(progress.getQuestId(), progress.getDefinitionVersion()), progress);
            }
            if (lockedQuestIds.isEmpty()) return FactProcessingResult.ignored();
            List<QuestEvaluationRequest> requests = provider.requestsForLockedProgressReconciliation(player,
                Collections.unmodifiableSet(lockedQuestIds));
            if (requests == null || requests.isEmpty()) return FactProcessingResult.ignored();

            List<QuestEvaluation> evaluations = new ArrayList<QuestEvaluation>();
            Set<RewardEntitlement> entitlements = new LinkedHashSet<RewardEntitlement>();
            Deque<CompletedQuest> newlyCompleted = new ArrayDeque<CompletedQuest>();
            Set<String> visited = new LinkedHashSet<String>();
            for (QuestEvaluationRequest request : requests) {
                QuestDefinition definition = request.getDefinition();
                ParticipantId participant = request.getMembership().getParticipantId();
                if (!player.equals(participant) || participant.getType() != ParticipantType.PLAYER
                    || !definition.getBehavior().isProgressWhileLocked()) continue;
                String identity = progressIdentity(definition.getId(), definition.getVersion());
                QuestProgressSnapshot previous = byIdentity.get(identity);
                if (previous == null || !visited.add(identity)
                    || !prerequisitesSatisfied(definition, request.getCompletedPrerequisites())) continue;
                QuestEvaluation evaluation = engine.evaluate(request.getMembership(), definition, previous,
                    new LinkedHashMap<String, TaskProgress>(previous.getTasks()), request.getCompletedPrerequisites(),
                    now, false, false, previous.getCompletedAt());
                if (evaluation.getProgress().getStatus() == QuestStatus.LOCKED) continue;
                repository.saveProgress(evaluation.getProgress());
                evaluations.add(evaluation);
                addEntitlements(evaluation, entitlements);
                if (evaluation.getProgress().getStatus() == QuestStatus.COMPLETED) {
                    newlyCompleted.addLast(new CompletedQuest(player, definition.getId()));
                }
            }
            if (evaluations.isEmpty()) return FactProcessingResult.ignored();
            reconcileNewlyUnlockedLockedProgress(provider, newlyCompleted, evaluations, entitlements, now);
            repository.insertEntitlementsIfAbsent(entitlements);
            return FactProcessingResult.accepted(evaluations);
        });
    }

    /**
     * Settles one persisted Quest after its consuming Detect batch has ended. The active-batch fence and this
     * completion/reward write must share the caller's transaction so a crash cannot expose a half-finalized batch.
     */
    public FactProcessingResult reconcileAfterConsumptionDetectBatch(final QuestEvaluationRequest request,
        final QuestEvaluationRequestProvider provider, final long now) {
        return reconcileAfterConsumptionDetectBatch(request, provider, now, -1);
    }

    public FactProcessingResult reconcileAfterConsumptionDetectBatch(final QuestEvaluationRequest request,
        final QuestEvaluationRequestProvider provider, final long now, final int expectedCycle) {
        return reconcileAfterConsumptionDetectBatch(request, provider, now, expectedCycle,
            Collections.<String>emptyList());
    }

    public FactProcessingResult reconcileAfterConsumptionDetectBatch(final QuestEvaluationRequest request,
        final QuestEvaluationRequestProvider provider, final long now, final int expectedCycle,
        final List<String> detectTaskKeys) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(detectTaskKeys, "detectTaskKeys");
        return transaction.inTransaction(() -> {
            QuestDefinition definition = request.getDefinition();
            ParticipantId participant = request.getMembership().getParticipantId();
            if (repository.hasActiveConsumptionDetectBatch(participant, definition.getId(), definition.getVersion())) {
                throw new IllegalStateException("consumption Detect batch is still active");
            }
            QuestProgressSnapshot previous = repository.findProgress(participant, definition.getId(),
                definition.getVersion()).orElse(null);
            int cycle = previous == null ? 0 : previous.getCycle();
            boolean holdCompletedCycle = repository.hasPendingConsumptionSubmission(participant, definition.getId(),
                definition.getVersion(), cycle);
            long repeatAnchorMillis = previous == null ? 0L : previous.getCompletedAt();
            if (!holdCompletedCycle && previous != null && previous.getStatus() == QuestStatus.COMPLETED
                && definition.getRepeatPolicy().isRepeatable()) {
                holdCompletedCycle = repository.hasClaimableRewardEntitlement(participant, definition.getId(),
                    definition.getVersion(), cycle);
                if (!holdCompletedCycle) repeatAnchorMillis = repository.findRepeatCycleAnchorMillis(participant,
                    definition.getId(), definition.getVersion(), cycle, previous.getCompletedAt());
            }
            boolean startsExpectedNextCycle = expectedCycle == cycle + 1 && previous != null
                && previous.getStatus() == QuestStatus.COMPLETED
                && engine.beginsNextCycle(definition, previous, now, holdCompletedCycle, repeatAnchorMillis);
            if (expectedCycle >= 0 && cycle != expectedCycle && !startsExpectedNextCycle) {
                throw new IllegalStateException("Quest cycle changed before consumption Detect settlement");
            }
            boolean simultaneousDetectTaskCompleted = false;
            if (previous != null && !detectTaskKeys.isEmpty()) {
                for (TaskDefinition task : definition.getTasks()) {
                    if (!detectTaskKeys.contains(task.getKey())) continue;
                    TaskProgress taskProgress = previous.getTasks().get(task.getKey());
                    if (taskProgress != null && taskProgress.isComplete()) {
                        simultaneousDetectTaskCompleted = true;
                        break;
                    }
                }
            }
            QuestEvaluation evaluation = engine.evaluate(request.getMembership(), definition, previous, null,
                request.getCompletedPrerequisites(), now, holdCompletedCycle, false, repeatAnchorMillis, false,
                simultaneousDetectTaskCompleted);
            if (expectedCycle >= 0 && evaluation.getProgress().getCycle() != expectedCycle) throw new IllegalStateException(
                "Quest cycle did not reach the consumption Detect batch cycle");
            repository.saveProgress(evaluation.getProgress());
            List<QuestEvaluation> evaluations = new ArrayList<QuestEvaluation>();
            evaluations.add(evaluation);
            Set<RewardEntitlement> entitlements = new LinkedHashSet<RewardEntitlement>();
            addEntitlements(evaluation, entitlements);
            Deque<CompletedQuest> newlyCompleted = new ArrayDeque<CompletedQuest>();
            if ((previous == null || previous.getStatus() != QuestStatus.COMPLETED)
                && evaluation.getProgress().getStatus() == QuestStatus.COMPLETED
                && participant.getType() == ParticipantType.PLAYER) {
                newlyCompleted.addLast(new CompletedQuest(participant, definition.getId()));
            }
            reconcileNewlyUnlockedLockedProgress(provider, newlyCompleted, evaluations, entitlements, now);
            repository.insertEntitlementsIfAbsent(entitlements);
            return FactProcessingResult.accepted(evaluations);
        });
    }

    private FactProcessingResult execute(GameplayFact fact, List<QuestEvaluationRequest> requests,
        QuestEvaluationRequestProvider provider, long now) {
        if (!repository.recordFactIfAbsent(fact)) return FactProcessingResult.duplicate();
        List<PreparedEvaluation> prepared = new ArrayList<PreparedEvaluation>(requests.size());
        for (QuestEvaluationRequest request : requests) {
            ParticipantId participant = request.getMembership().getParticipantId();
            QuestDefinition definition = request.getDefinition();
            QuestProgressSnapshot previous = repository
                .findProgress(participant, definition.getId(), definition.getVersion()).orElse(null);
            int cycle = previous == null ? 0 : previous.getCycle();
            long repeatAnchorMillis = previous == null ? 0L : previous.getCompletedAt();
            boolean holdCompletedCycle = repository.hasPendingConsumptionSubmission(participant, definition.getId(),
                definition.getVersion(), cycle);
            boolean deferQuestCompletion = repository.hasActiveConsumptionDetectBatch(participant,
                definition.getId(), definition.getVersion());
            if (!holdCompletedCycle && previous != null && previous.getStatus() == QuestStatus.COMPLETED
                && definition.getRepeatPolicy().isRepeatable()) {
                holdCompletedCycle = repository.hasClaimableRewardEntitlement(participant, definition.getId(),
                    definition.getVersion(), cycle);
                if (!holdCompletedCycle) repeatAnchorMillis = repository.findRepeatCycleAnchorMillis(participant,
                    definition.getId(), definition.getVersion(), cycle, previous.getCompletedAt());
            }
            boolean beginsNextCycle = engine.beginsNextCycle(definition, previous, now, holdCompletedCycle,
                repeatAnchorMillis);
            QuestProgressSnapshot observationBase = previousOrEmpty(participant, definition,
                beginsNextCycle ? null : previous);
            Map<String, TaskProgress> observations = new LinkedHashMap<String, TaskProgress>(projector
                .project(definition, observationBase, request.getMembership(), request.getFactMembership(), fact));
            prepared.add(new PreparedEvaluation(request, previous, holdCompletedCycle, observations,
                isBqPartyProgressFanout(request, fact), repeatAnchorMillis, deferQuestCompletion));
        }
        fanOutBqRetrievalCompletion(prepared);

        List<QuestEvaluation> evaluations = new ArrayList<QuestEvaluation>();
        Set<RewardEntitlement> entitlements = new LinkedHashSet<RewardEntitlement>();
        Deque<CompletedQuest> newlyCompleted = new ArrayDeque<CompletedQuest>();
        for (PreparedEvaluation value : prepared) {
            QuestEvaluationRequest request = value.request;
            QuestDefinition definition = request.getDefinition();
            QuestEvaluation evaluation = engine.evaluate(request.getMembership(), definition, value.previous,
                value.observations, request.getCompletedPrerequisites(), now, value.holdCompletedCycle,
                value.bqPartyProgressFanout, value.repeatAnchorMillis, value.deferQuestCompletion);
            repository.saveProgress(evaluation.getProgress());
            addEntitlements(evaluation, entitlements);
            evaluations.add(evaluation);
            if ((value.previous == null || value.previous.getStatus() != QuestStatus.COMPLETED)
                && evaluation.getProgress().getStatus() == QuestStatus.COMPLETED
                && request.getMembership().getParticipantId().getType() == ParticipantType.PLAYER) {
                newlyCompleted.addLast(new CompletedQuest(request.getMembership().getParticipantId(),
                    definition.getId()));
            }
        }
        if (provider != null) {
            reconcileNewlyUnlockedLockedProgress(provider, newlyCompleted, evaluations, entitlements, now);
        }
        repository.insertEntitlementsIfAbsent(entitlements);
        return FactProcessingResult.accepted(evaluations);
    }

    private void reconcileNewlyUnlockedLockedProgress(QuestEvaluationRequestProvider provider,
        Deque<CompletedQuest> newlyCompleted, List<QuestEvaluation> evaluations,
        Set<RewardEntitlement> entitlements, long now) {
        Set<String> visited = new LinkedHashSet<String>();
        while (!newlyCompleted.isEmpty()) {
            CompletedQuest completed = newlyCompleted.removeFirst();
            List<QuestEvaluationRequest> dependents = provider.requestsForNewlyUnlockedLockedProgress(
                completed.participantId, completed.questId);
            if (dependents == null || dependents.isEmpty()) continue;
            for (QuestEvaluationRequest request : dependents) {
                QuestDefinition definition = request.getDefinition();
                ParticipantId participant = request.getMembership().getParticipantId();
                if (participant.getType() != ParticipantType.PLAYER
                    || !participant.equals(completed.participantId)
                    || !definition.getBehavior().isProgressWhileLocked()) continue;
                String key = participant + ":" + definition.getId() + ":" + definition.getVersion();
                if (!visited.add(key)) continue;
                QuestProgressSnapshot previous = repository.findProgress(participant, definition.getId(),
                    definition.getVersion()).orElse(null);
                if (previous == null || previous.getStatus() != QuestStatus.LOCKED) continue;
                if (!prerequisitesSatisfied(definition, request.getCompletedPrerequisites())) continue;

                QuestEvaluation evaluation = engine.evaluate(request.getMembership(), definition, previous,
                    new LinkedHashMap<String, TaskProgress>(previous.getTasks()),
                    request.getCompletedPrerequisites(), now, false, false, previous.getCompletedAt());
                if (evaluation.getProgress().getStatus() == QuestStatus.LOCKED) continue;
                repository.saveProgress(evaluation.getProgress());
                evaluations.add(evaluation);
                addEntitlements(evaluation, entitlements);
                if (evaluation.getProgress().getStatus() == QuestStatus.COMPLETED) {
                    newlyCompleted.addLast(new CompletedQuest(participant, definition.getId()));
                }
            }
        }
    }

    private boolean prerequisitesSatisfied(QuestDefinition definition, Set<UUID> completedPrerequisites) {
        int completed = 0;
        for (UUID prerequisite : definition.getPrerequisites()) {
            if (completedPrerequisites.contains(prerequisite)) completed++;
        }
        return definition.getPrerequisites().isEmpty()
            || definition.getPrerequisiteLogic().satisfied(completed, definition.getPrerequisites().size());
    }

    private void addEntitlements(QuestEvaluation evaluation, Set<RewardEntitlement> entitlements) {
        for (RewardEntitlement entitlement : evaluation.getEntitlements()) {
            if (entitlement.isDeliveryRequested() && rewardSuppressionPolicy.suppresses(entitlement.getReward())) {
                entitlements.add(entitlement.withSuppressedDelivery());
            } else {
                entitlements.add(entitlement);
            }
        }
    }

    private static String progressIdentity(UUID questId, int version) {
        return questId + ":" + version;
    }

    private static final class CompletedQuest {
        private final ParticipantId participantId;
        private final UUID questId;

        private CompletedQuest(ParticipantId participantId, UUID questId) {
            this.participantId = participantId;
            this.questId = questId;
        }
    }

    private static boolean isBqPartyProgressFanout(QuestEvaluationRequest request, GameplayFact fact) {
        if (BqPartyTaskProgressScope.authorizesPlayerRequest(request, fact)) return true;
        if (BqPartyCraftingStatisticsScope.authorizesLockedPlayerProgress(request, fact)) return true;
        if (!BqPartyInventorySnapshotScope.isPartySnapshot(fact)
            || request.getMembership().getParticipantId().getType() != ParticipantType.PLAYER
            || request.getFactMembership().getParticipantId().getType() != ParticipantType.PARTY
            || !BqPartyInventorySnapshotScope.matches(fact, request.getFactMembership())
            || !request.getFactMembership().contains(request.getMembership().getParticipantId().getId())) return false;
        for (TaskDefinition task : request.getDefinition().getTasks()) {
            if (("bq_standard:retrieval".equals(task.getTypeId())
                || "bq_standard:optional_retrieval".equals(task.getTypeId()))
                && "galaxy:inventory_observed".equals(task.getParameters().get("factType"))
                && !QuestBooleanParameter.parse(task.getParameters().get("consume"), false)) return true;
        }
        return false;
    }

    private static void fanOutBqRetrievalCompletion(List<PreparedEvaluation> prepared) {
        Map<String, Set<String>> completedTaskKeys = new HashMap<String, Set<String>>();
        for (PreparedEvaluation value : prepared) {
            if (!value.bqPartyProgressFanout) continue;
            QuestDefinition definition = value.request.getDefinition();
            String definitionKey = definitionKey(definition);
            for (TaskDefinition task : definition.getTasks()) {
                if (!isNonConsumingRetrieval(task)) continue;
                TaskProgress observation = value.observations.get(task.getKey());
                if (observation == null || !observation.isComplete()) continue;
                Set<String> keys = completedTaskKeys.get(definitionKey);
                if (keys == null) {
                    keys = new java.util.LinkedHashSet<String>();
                    completedTaskKeys.put(definitionKey, keys);
                }
                keys.add(task.getKey());
            }
        }
        if (completedTaskKeys.isEmpty()) return;
        for (PreparedEvaluation value : prepared) {
            if (!value.bqPartyProgressFanout) continue;
            Set<String> keys = completedTaskKeys.get(definitionKey(value.request.getDefinition()));
            if (keys == null) continue;
            for (String taskKey : keys) {
                TaskProgress observation = value.observations.get(taskKey);
                if (observation != null) value.observations.put(taskKey, observation.markComplete());
            }
        }
    }

    private static boolean isNonConsumingRetrieval(TaskDefinition task) {
        String type = task.getTypeId();
        return ("bq_standard:retrieval".equals(type) || "bq_standard:optional_retrieval".equals(type))
            && !QuestBooleanParameter.parse(task.getParameters().get("consume"), false);
    }

    private static String definitionKey(QuestDefinition definition) {
        return definition.getId().toString() + ":" + definition.getVersion();
    }

    private static QuestProgressSnapshot previousOrEmpty(ParticipantId participant, QuestDefinition definition,
        QuestProgressSnapshot previous) {
        if (previous != null) return previous;
        return new QuestProgressSnapshot(participant, definition.getId(), definition.getVersion(),
            QuestStatus.AVAILABLE, java.util.Collections.<String, TaskProgress>emptyMap(), 0L, 0);
    }

    private static final class PreparedEvaluation {
        private final QuestEvaluationRequest request;
        private final QuestProgressSnapshot previous;
        private final boolean holdCompletedCycle;
        private final Map<String, TaskProgress> observations;
        private final boolean bqPartyProgressFanout;
        private final long repeatAnchorMillis;
        private final boolean deferQuestCompletion;

        private PreparedEvaluation(QuestEvaluationRequest request, QuestProgressSnapshot previous,
            boolean holdCompletedCycle, Map<String, TaskProgress> observations, boolean bqPartyProgressFanout,
            long repeatAnchorMillis, boolean deferQuestCompletion) {
            this.request = request;
            this.previous = previous;
            this.holdCompletedCycle = holdCompletedCycle;
            this.observations = observations;
            this.bqPartyProgressFanout = bqPartyProgressFanout;
            this.repeatAnchorMillis = repeatAnchorMillis;
            this.deferQuestCompletion = deferQuestCompletion;
        }
    }
}
