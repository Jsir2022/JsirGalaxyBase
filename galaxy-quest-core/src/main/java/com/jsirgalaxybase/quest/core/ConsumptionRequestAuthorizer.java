package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Builds the only authoritative deduction request from published definitions and locked progress. */
public final class ConsumptionRequestAuthorizer {
    private final PublishedQuestCatalog definitions;
    private final QuestRuntimeRepository progress;
    private final QuestCompletionQuery completions;
    private final QuestEngine engine;
    private final ParticipantMembershipResolver memberships;
    private final QuestAssignmentPolicy assignments;

    public ConsumptionRequestAuthorizer(PublishedQuestCatalog definitions, QuestRuntimeRepository progress,
        QuestCompletionQuery completions, QuestEngine engine) {
        this(definitions, progress, completions, engine, new PlayerOnlyMembershipResolver(),
            new ScopedQuestAssignmentPolicy());
    }

    public ConsumptionRequestAuthorizer(PublishedQuestCatalog definitions, QuestRuntimeRepository progress,
        QuestCompletionQuery completions, QuestEngine engine, ParticipantMembershipResolver memberships,
        QuestAssignmentPolicy assignments) {
        if (definitions == null || progress == null || completions == null || engine == null
            || memberships == null || assignments == null) {
            throw new IllegalArgumentException("authorizer dependencies must not be null");
        }
        this.definitions = definitions;
        this.progress = progress;
        this.completions = completions;
        this.engine = engine;
        this.memberships = memberships;
        this.assignments = assignments;
    }

    public ConsumptionRequest authorize(ConsumptionIntent intent, long now) {
        return authorize(intent, now, null, null);
    }

    /** Batch variant pins participant and repeat cycle before any player inventory side effect is applied. */
    public ConsumptionRequest authorize(ConsumptionIntent intent, long now, ParticipantId expectedParticipant,
        Integer expectedCycle) {
        return authorize(intent, now, expectedParticipant, expectedCycle, null);
    }

    /** Version-pinned authorization for a durable Detect batch; a newer publication cannot rewrite its tasks. */
    public ConsumptionRequest authorize(ConsumptionIntent intent, long now, ParticipantId expectedParticipant,
        Integer expectedCycle, Integer expectedDefinitionVersion) {
        QuestDefinition definition = expectedDefinitionVersion == null ? published(intent)
            : published(intent.getQuestId(), expectedDefinitionVersion.intValue());
        TaskDefinition task = task(definition, intent.getTaskKey());
        if (!supports(definition, task)) {
            throw new IllegalArgumentException("task is outside the verified manual item-consumption subset");
        }
        String kind = kind(task);
        List<ParticipantMembership> selected = assignments.select(definition, memberships.resolve(intent.getPlayerId()));
        if (selected.isEmpty()) throw new IllegalArgumentException("player has no active participant for quest scope");
        ParticipantMembership membership = selected.get(0);
        ParticipantId participant = membership.getParticipantId();
        Optional<QuestProgressSnapshot> stored = progress.findProgress(participant, definition.getId(),
            definition.getVersion());
        Set<java.util.UUID> completed = completions.findCompletedQuestIds(participant);
        if (expectedParticipant != null && !expectedParticipant.equals(participant)) throw new IllegalArgumentException(
            "quest membership changed during consuming Detect batch");
        QuestProgressSnapshot normalized = normalize(membership, definition, stored.orElse(null), completed, now);
        if (expectedCycle != null && expectedCycle.intValue() != normalized.getCycle()) throw new IllegalArgumentException(
            "quest repeat cycle changed during consuming Detect batch");
        if (progress.hasPendingConsumptionSubmission(participant, definition.getId(), definition.getVersion(),
            normalized.getCycle())) {
            throw new IllegalArgumentException("another item submission is pending for this quest cycle");
        }
        if (normalized.getStatus() == QuestStatus.LOCKED || normalized.getStatus() == QuestStatus.COMPLETED) {
            throw new IllegalArgumentException("quest is not accepting consumption in status " + normalized.getStatus());
        }
        TaskProgress taskProgress = normalized.getTasks().get(task.getKey());
        if (taskProgress == null || taskProgress.isComplete()) throw new IllegalArgumentException(
            "task has no remaining consumption");
        List<Long> remaining = new ArrayList<Long>(taskProgress.getValues().size());
        boolean any = false;
        for (int i = 0; i < taskProgress.getValues().size(); i++) {
            long value = Math.max(0L, taskProgress.getTargets().get(i) - taskProgress.getValues().get(i));
            remaining.add(value);
            any |= value > 0;
        }
        if (!any) throw new IllegalArgumentException("task has no remaining consumption");
        Map<String, String> parameters = resourceParameters(task, kind);
        parameters.put(ConsumptionRequest.QUEST_CYCLE_PARAMETER, Integer.toString(normalized.getCycle()));
        if (intent.getSelection() != null) {
            parameters.put(ConsumptionRequest.SELECTION_SLOT_PARAMETER,
                Integer.toString(intent.getSelection().getInventorySlot()));
            parameters.put(ConsumptionRequest.SELECTION_FINGERPRINT_PARAMETER,
                intent.getSelection().getStackFingerprint());
        }
        return new ConsumptionRequest(intent.getSubmissionKey(), intent.getSourceServer(), intent.getPlayerId(),
            participant, definition.getId(), definition.getVersion(), task.getKey(), kind, remaining,
            parameters, intent.getCreatedAt());
    }

    public int publishedVersion(java.util.UUID questId) {
        if (questId == null) throw new IllegalArgumentException("questId is required");
        return published(questId).getVersion();
    }

    /** Current server-side evaluation context used only for final Quest settlement after a Detect batch. */
    public QuestEvaluationRequest evaluationRequest(java.util.UUID playerId, java.util.UUID questId) {
        if (playerId == null || questId == null) throw new IllegalArgumentException("player and quest are required");
        QuestDefinition definition = published(questId);
        return evaluationRequest(playerId, definition, null);
    }

    /** Uses the pinned historical definition and participant to settle an already-started Detect batch. */
    public QuestEvaluationRequest evaluationRequest(java.util.UUID playerId, java.util.UUID questId,
        int definitionVersion, ParticipantId expectedParticipant) {
        if (playerId == null || questId == null || definitionVersion < 1 || expectedParticipant == null) {
            throw new IllegalArgumentException("player, pinned quest version and participant are required");
        }
        if (expectedParticipant.getType() != ParticipantType.PLAYER
            || !playerId.equals(expectedParticipant.getId())) throw new IllegalArgumentException(
                "consuming Detect settlement is only supported for the authenticated PLAYER participant");
        return evaluationRequest(playerId, published(questId, definitionVersion), expectedParticipant);
    }

    private QuestEvaluationRequest evaluationRequest(java.util.UUID playerId, QuestDefinition definition,
        ParticipantId expectedParticipant) {
        if (definition.getBehavior().getParticipantScope() != QuestParticipantScope.PLAYER) {
            throw new IllegalArgumentException("consuming Detect is not enabled for this quest participant scope");
        }
        ParticipantMembership membership;
        if (expectedParticipant != null) {
            membership = ParticipantMembership.player(playerId);
        } else {
            List<ParticipantMembership> selected = assignments.select(definition, memberships.resolve(playerId));
            if (selected.isEmpty()) throw new IllegalArgumentException("player has no active participant for quest scope");
            membership = selected.get(0);
        }
        return new QuestEvaluationRequest(definition, membership, completions.findCompletedQuestIds(
            membership.getParticipantId()));
    }

    /** Builds the ordered server-side task list for a single Quest-level consuming Detect action. */
    public QuestConsumptionDetectPlan planQuestDetect(java.util.UUID playerId, java.util.UUID questId, long now) {
        if (playerId == null || questId == null) throw new IllegalArgumentException("player and quest are required");
        QuestEvaluationRequest request = evaluationRequest(playerId, questId);
        QuestDefinition definition = request.getDefinition();
        ParticipantMembership membership = request.getMembership();
        ParticipantId participant = membership.getParticipantId();
        Optional<QuestProgressSnapshot> stored = progress.findProgress(participant, definition.getId(),
            definition.getVersion());
        Set<java.util.UUID> completed = request.getCompletedPrerequisites();
        QuestProgressSnapshot normalized = normalize(membership, definition, stored.orElse(null), completed, now);
        if (progress.hasPendingConsumptionSubmission(participant, definition.getId(), definition.getVersion(),
            normalized.getCycle())) throw new IllegalArgumentException(
                "another consumption operation is still pending for this quest cycle");
        if (normalized.getStatus() == QuestStatus.LOCKED || normalized.getStatus() == QuestStatus.COMPLETED) {
            throw new IllegalArgumentException("quest is not accepting consumption in status " + normalized.getStatus());
        }
        List<String> taskKeys = new ArrayList<String>();
        for (TaskDefinition task : definition.getTasks()) {
            if (!supports(definition, task)) continue;
            TaskProgress taskProgress = normalized.getTasks().get(task.getKey());
            if (taskProgress == null || taskProgress.isComplete()) continue;
            boolean remaining = false;
            for (int i = 0; i < taskProgress.getValues().size(); i++) {
                if (taskProgress.getTargets().get(i) > taskProgress.getValues().get(i)) {
                    remaining = true;
                    break;
                }
            }
            if (remaining) taskKeys.add(task.getKey());
        }
        return new QuestConsumptionDetectPlan(request, normalized.getCycle(), taskKeys);
    }

    private QuestProgressSnapshot normalize(ParticipantMembership membership, QuestDefinition definition,
        QuestProgressSnapshot previous, Set<java.util.UUID> completed, long now) {
        ParticipantId participant = membership.getParticipantId();
        int cycle = previous == null ? 0 : previous.getCycle();
        boolean holdCompletedCycle = progress.hasPendingConsumptionSubmission(participant, definition.getId(),
            definition.getVersion(), cycle);
        long repeatAnchorMillis = previous == null ? 0L : previous.getCompletedAt();
        if (!holdCompletedCycle && previous != null && previous.getStatus() == QuestStatus.COMPLETED
            && definition.getRepeatPolicy().isRepeatable()) {
            holdCompletedCycle = progress.hasClaimableRewardEntitlement(participant, definition.getId(),
                definition.getVersion(), cycle);
            if (!holdCompletedCycle) repeatAnchorMillis = progress.findRepeatCycleAnchorMillis(participant,
                definition.getId(), definition.getVersion(), cycle, previous.getCompletedAt());
        }
        boolean activeDetectBatch = progress.hasActiveConsumptionDetectBatch(participant, definition.getId(),
            definition.getVersion());
        return engine.evaluate(membership, definition, previous, null, completed, now, holdCompletedCycle, false,
            repeatAnchorMillis, activeDetectBatch).getProgress();
    }

    /** Manual item retrieval is supported for player-owned quests; other consumption types remain gated. */
    public static boolean supports(QuestDefinition definition, TaskDefinition task) {
        if (definition == null || task == null
            || definition.getBehavior().getParticipantScope() != QuestParticipantScope.PLAYER) return false;
        String type = task.getTypeId();
        if (!"bq_standard:retrieval".equals(type) && !"bq_standard:optional_retrieval".equals(type)
            && !"bq_standard:fluid".equals(type)) return false;
        return flag(task.getParameters().get("consume")) && !flag(task.getParameters().get("autoConsume"));
    }

    private QuestDefinition published(ConsumptionIntent intent) {
        return published(intent.getQuestId());
    }

    private QuestDefinition published(java.util.UUID questId) {
        QuestDefinition found = null;
        for (StoredQuestDefinition stored : definitions.findAllPublished()) {
            QuestDefinition candidate = stored.getDefinition();
            if (!candidate.getId().equals(questId)) continue;
            if (found != null) throw new IllegalStateException("published catalog contains duplicate quest versions");
            found = candidate;
        }
        if (found == null) throw new IllegalArgumentException("quest is not published");
        return found;
    }

    private QuestDefinition published(java.util.UUID questId, int version) {
        Optional<StoredQuestDefinition> stored = definitions.findPublished(questId, version);
        if (!stored.isPresent()) throw new IllegalArgumentException(
            "pinned quest definition is no longer published: " + questId + " v" + version);
        QuestDefinition definition = stored.get().getDefinition();
        if (!questId.equals(definition.getId()) || version != definition.getVersion()) throw new IllegalStateException(
            "published quest catalog returned a different pinned definition");
        return definition;
    }

    private TaskDefinition task(QuestDefinition definition, String key) {
        for (TaskDefinition task : definition.getTasks()) if (task.getKey().equals(key)) return task;
        throw new IllegalArgumentException("task does not belong to published quest");
    }

    private String kind(TaskDefinition task) {
        if ("bq_standard:retrieval".equals(task.getTypeId())
            || "bq_standard:optional_retrieval".equals(task.getTypeId())) return "item";
        if ("bq_standard:fluid".equals(task.getTypeId())) return "fluid";
        throw new IllegalArgumentException("task type is not a supported consumption task");
    }

    private static boolean flag(String value) {
        return QuestBooleanParameter.parse(value, false);
    }

    private Map<String, String> resourceParameters(TaskDefinition task, String kind) {
        String prefix = "item".equals(kind) ? "item." : "fluid".equals(kind) ? "fluid." : "xp";
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : task.getParameters().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(prefix) || "partialMatch".equals(key) || "ignoreNBT".equals(key)) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }
}
