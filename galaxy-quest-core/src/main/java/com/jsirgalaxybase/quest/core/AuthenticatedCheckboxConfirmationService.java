package com.jsirgalaxybase.quest.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Turns an authenticated player's checkbox click into one stable, quest-bound fact per cycle. */
public final class AuthenticatedCheckboxConfirmationService {
    private static final String CHECKBOX = "bq_standard:checkbox";
    private static final String FACT_TYPE = "galaxy:task_confirmed";

    private final QuestDefinitionRepository definitions;
    private final QuestRuntimeRepository progress;
    private final QuestRuntimeService runtime;
    private final QuestEvaluationRequestProvider requests;
    private final String sourceServer;

    public AuthenticatedCheckboxConfirmationService(QuestDefinitionRepository definitions,
        QuestRuntimeRepository progress, QuestRuntimeService runtime, QuestEvaluationRequestProvider requests,
        String sourceServer) {
        if (definitions == null || progress == null || runtime == null || requests == null
            || sourceServer == null || sourceServer.trim().isEmpty()) {
            throw new IllegalArgumentException("checkbox dependencies and source server are required");
        }
        this.definitions = definitions;
        this.progress = progress;
        this.runtime = runtime;
        this.requests = requests;
        this.sourceServer = sourceServer.trim();
    }

    public FactProcessingResult confirm(UUID authenticatedPlayerId, UUID questId, String taskKey, long now) {
        if (authenticatedPlayerId == null || questId == null || taskKey == null || taskKey.trim().isEmpty()
            || now < 0L) throw new IllegalArgumentException("player, quest, task and time are required");
        Optional<StoredQuestDefinition> found = definitions.findPublished(questId);
        if (!found.isPresent()) return FactProcessingResult.ignored();
        QuestDefinition definition = found.get().getDefinition();
        // Group cycles are owned by the group, not the clicking player's progress row.
        if (definition.getBehavior().getParticipantScope() != QuestParticipantScope.PLAYER) {
            return FactProcessingResult.ignored();
        }
        TaskDefinition checkbox = null;
        for (TaskDefinition task : definition.getTasks()) {
            if (taskKey.equals(task.getKey()) && CHECKBOX.equals(task.getTypeId())
                && FACT_TYPE.equals(task.getParameters().get("factType"))) {
                checkbox = task;
                break;
            }
        }
        if (checkbox == null) return FactProcessingResult.ignored();
        Optional<QuestProgressSnapshot> current = progress.findProgress(ParticipantId.player(authenticatedPlayerId),
            questId, definition.getVersion());
        int cycle = 0;
        if (current.isPresent()) {
            QuestProgressSnapshot snapshot = current.get();
            cycle = snapshot.getCycle();
            if (snapshot.getStatus() == QuestStatus.COMPLETED) {
                if (!definition.getRepeatPolicy().isAvailable(snapshot.getCompletedAt(), now)) {
                    return FactProcessingResult.ignored();
                }
                if (cycle == Integer.MAX_VALUE) throw new IllegalStateException("checkbox cycle exhausted");
                cycle++;
            } else {
                TaskProgress taskProgress = snapshot.getTasks().get(taskKey);
                if (taskProgress != null && taskProgress.isComplete()) return FactProcessingResult.ignored();
            }
        }
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("questId", questId.toString());
        attributes.put("definitionVersion", Integer.toString(definition.getVersion()));
        attributes.put("taskKey", taskKey);
        String eventId = "checkbox:" + authenticatedPlayerId + ':' + questId + ':' + definition.getVersion()
            + ':' + cycle + ':' + taskKey;
        GameplayFact fact = new GameplayFact(sourceServer, eventId, authenticatedPlayerId, FACT_TYPE, now,
            attributes);
        return runtime.applyIf(fact, requests, values -> mayConfirm(values, authenticatedPlayerId, definition), now);
    }

    private static boolean mayConfirm(List<QuestEvaluationRequest> requests, UUID playerId,
        QuestDefinition definition) {
        for (QuestEvaluationRequest request : requests) {
            ParticipantId participant = request.getMembership().getParticipantId();
            if (!request.getDefinition().getId().equals(definition.getId())
                || request.getDefinition().getVersion() != definition.getVersion()
                || participant.getType() != ParticipantType.PLAYER || !participant.getId().equals(playerId)) continue;
            if (definition.getPrerequisites().isEmpty() || definition.getBehavior().isProgressWhileLocked()) return true;
            int complete = 0;
            for (UUID prerequisite : definition.getPrerequisites()) {
                if (request.getCompletedPrerequisites().contains(prerequisite)) complete++;
            }
            if (definition.getPrerequisiteLogic().satisfied(complete, definition.getPrerequisites().size())) return true;
        }
        return false;
    }
}
