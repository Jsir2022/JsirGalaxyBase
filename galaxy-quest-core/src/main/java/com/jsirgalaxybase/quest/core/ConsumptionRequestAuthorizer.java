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
        QuestDefinition definition = published(intent);
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
        int activeCycle = stored.isPresent() ? stored.get().getCycle() : 0;
        boolean pendingActiveCycle = progress.hasPendingConsumptionSubmission(participant, definition.getId(),
            definition.getVersion(), activeCycle);
        QuestProgressSnapshot normalized = engine.evaluate(membership, definition, stored.orElse(null), null,
            completed, now, pendingActiveCycle).getProgress();
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
        QuestDefinition found = null;
        for (StoredQuestDefinition stored : definitions.findAllPublished()) {
            QuestDefinition candidate = stored.getDefinition();
            if (!candidate.getId().equals(intent.getQuestId())) continue;
            if (found != null) throw new IllegalStateException("published catalog contains duplicate quest versions");
            found = candidate;
        }
        if (found == null) throw new IllegalArgumentException("quest is not published");
        return found;
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
