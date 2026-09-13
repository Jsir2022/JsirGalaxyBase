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

    public ConsumptionRequestAuthorizer(PublishedQuestCatalog definitions, QuestRuntimeRepository progress,
        QuestCompletionQuery completions, QuestEngine engine) {
        if (definitions == null || progress == null || completions == null || engine == null) {
            throw new IllegalArgumentException("authorizer dependencies must not be null");
        }
        this.definitions = definitions;
        this.progress = progress;
        this.completions = completions;
        this.engine = engine;
    }

    public ConsumptionRequest authorize(ConsumptionIntent intent, long now) {
        QuestDefinition definition = published(intent);
        TaskDefinition task = task(definition, intent.getTaskKey());
        String kind = kind(task);
        if (!Boolean.parseBoolean(task.getParameters().get("consume"))) {
            throw new IllegalArgumentException("task does not consume resources");
        }
        ParticipantId participant = ParticipantId.player(intent.getPlayerId());
        Optional<QuestProgressSnapshot> stored = progress.findProgress(participant, definition.getId(),
            definition.getVersion());
        Set<java.util.UUID> completed = completions.findCompletedQuestIds(participant);
        QuestProgressSnapshot normalized = engine.evaluate(participant, definition, stored.orElse(null), null,
            completed, now).getProgress();
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
        return new ConsumptionRequest(intent.getSubmissionKey(), intent.getSourceServer(), intent.getPlayerId(),
            participant, definition.getId(), definition.getVersion(), task.getKey(), kind, remaining,
            resourceParameters(task, kind), intent.getCreatedAt());
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
        if ("bq_standard:xp".equals(task.getTypeId())) return "xp";
        throw new IllegalArgumentException("task type is not a supported consumption task");
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
