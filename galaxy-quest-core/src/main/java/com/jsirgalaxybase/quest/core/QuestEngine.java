package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class QuestEngine {
    public QuestEvaluation evaluate(UUID playerId, QuestDefinition definition, QuestProgressSnapshot previous,
        Map<String, TaskProgress> observations, Set<UUID> completedPrerequisites, long now) {
        return evaluate(ParticipantId.player(playerId), definition, previous, observations, completedPrerequisites, now);
    }

    public QuestEvaluation evaluate(ParticipantId participantId, QuestDefinition definition,
        QuestProgressSnapshot previous, Map<String, TaskProgress> observations, Set<UUID> completedPrerequisites,
        long now) {
        if (previous != null && !previous.getParticipantId().equals(participantId)) {
            throw new IllegalArgumentException("progress participant does not match evaluation participant");
        }
        if (previous != null && (!previous.getQuestId().equals(definition.getId())
            || previous.getDefinitionVersion() != definition.getVersion())) {
            throw new IllegalArgumentException("progress definition identity does not match evaluated definition");
        }
        boolean unlocked = definition.getPrerequisites().isEmpty() || definition.getPrerequisiteLogic().satisfied(
            intersectionSize(definition.getPrerequisites(), completedPrerequisites), definition.getPrerequisites().size());
        boolean acceptsProgress = unlocked || definition.getBehavior().isProgressWhileLocked();

        boolean beginNextCycle = previous != null && previous.getStatus() == QuestStatus.COMPLETED
            && definition.getRepeatPolicy().isAvailable(previous.getCompletedAt(), now);
        QuestProgressSnapshot progressBase = beginNextCycle ? null : previous;
        int cycle = previous == null ? 0 : previous.getCycle() + (beginNextCycle ? 1 : 0);

        Map<String, TaskProgress> merged = new LinkedHashMap<String, TaskProgress>();
        int requiredCount = 0;
        int completeCount = 0;
        for (TaskDefinition task : definition.getTasks()) {
            TaskProgress old = progressBase == null ? null : progressBase.getTasks().get(task.getKey());
            TaskProgress observed = observations == null ? null : observations.get(task.getKey());
            TaskProgress value = old == null ? TaskProgress.empty(task) : old;
            if (acceptsProgress && observed != null) value = value.merge(observed);
            merged.put(task.getKey(), value);
            if (!task.isOptional()) {
                requiredCount++;
                if (value.isComplete()) completeCount++;
            }
        }

        QuestStatus priorStatus = progressBase == null ? null : progressBase.getStatus();
        boolean alreadyComplete = priorStatus == QuestStatus.COMPLETED;
        boolean completed = acceptsProgress
            && (alreadyComplete || definition.getTaskLogic().satisfied(completeCount, requiredCount));
        if (!completed && acceptsProgress && definition.getBehavior().isSimultaneous() && completeCount > 0) {
            merged.clear();
            for (TaskDefinition task : definition.getTasks()) merged.put(task.getKey(), TaskProgress.empty(task));
        }
        QuestStatus status;
        if (completed) status = QuestStatus.COMPLETED;
        else if (!unlocked) status = QuestStatus.LOCKED;
        else status = hasProgress(merged) ? QuestStatus.IN_PROGRESS : QuestStatus.AVAILABLE;
        long completedAt = alreadyComplete ? progressBase.getCompletedAt() : completed ? now : 0L;

        QuestProgressSnapshot next = new QuestProgressSnapshot(participantId, definition.getId(),
            definition.getVersion(), status, merged, completedAt, cycle);
        List<RewardEntitlement> entitlements = new ArrayList<RewardEntitlement>();
        if (completed && !alreadyComplete) {
            for (RewardDefinition reward : definition.getRewards()) {
                entitlements.add(new RewardEntitlement(participantId, definition, reward, cycle,
                    definition.getBehavior().isAutoClaim()));
            }
        }
        return new QuestEvaluation(next, entitlements);
    }

    private static int intersectionSize(Set<UUID> required, Set<UUID> completed) {
        if (completed == null || completed.isEmpty()) return 0;
        int count = 0;
        for (UUID id : required) if (completed.contains(id)) count++;
        return count;
    }

    private static boolean hasProgress(Map<String, TaskProgress> tasks) {
        for (TaskProgress task : tasks.values()) if (task.getValue() > 0 || task.isComplete()) return true;
        return false;
    }

}
