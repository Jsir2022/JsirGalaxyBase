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
        if (participantId.getType() != ParticipantType.PLAYER) throw new IllegalArgumentException(
            "group evaluation requires an explicit membership snapshot");
        return evaluate(new ParticipantMembership(participantId, java.util.Collections.singleton(participantId.getId())),
            definition, previous, observations, completedPrerequisites, now);
    }

    public QuestEvaluation evaluate(ParticipantMembership membership, QuestDefinition definition,
        QuestProgressSnapshot previous, Map<String, TaskProgress> observations, Set<UUID> completedPrerequisites,
        long now) {
        return evaluate(membership, definition, previous, observations, completedPrerequisites, now, false);
    }

    /** Holds an expired completed cycle open while its manual-consumption saga is being recovered. */
    public QuestEvaluation evaluate(ParticipantMembership membership, QuestDefinition definition,
        QuestProgressSnapshot previous, Map<String, TaskProgress> observations, Set<UUID> completedPrerequisites,
        long now, boolean holdCompletedCycle) {
        return evaluate(membership, definition, previous, observations, completedPrerequisites, now,
            holdCompletedCycle, false);
    }

    /**
     * Allows authenticated BQ party task facts to update every member's Task progress, while still deferring
     * locked members' quest completion and reward entitlement until they unlock.
     */
    public QuestEvaluation evaluate(ParticipantMembership membership, QuestDefinition definition,
        QuestProgressSnapshot previous, Map<String, TaskProgress> observations, Set<UUID> completedPrerequisites,
        long now, boolean holdCompletedCycle, boolean bqPartyProgressFanout) {
        long repeatAnchorMillis = previous == null ? 0L : previous.getCompletedAt();
        return evaluate(membership, definition, previous, observations, completedPrerequisites, now,
            holdCompletedCycle, bqPartyProgressFanout, repeatAnchorMillis);
    }

    public QuestEvaluation evaluate(ParticipantMembership membership, QuestDefinition definition,
        QuestProgressSnapshot previous, Map<String, TaskProgress> observations, Set<UUID> completedPrerequisites,
        long now, boolean holdCompletedCycle, boolean bqPartyProgressFanout, long repeatAnchorMillis) {
        return evaluate(membership, definition, previous, observations, completedPrerequisites, now,
            holdCompletedCycle, bqPartyProgressFanout, repeatAnchorMillis, false);
    }

    /**
     * Applies Task progress during a quest-level Detect batch without settling Quest completion or rewards until
     * the batch has reached its final task.
     */
    public QuestEvaluation evaluate(ParticipantMembership membership, QuestDefinition definition,
        QuestProgressSnapshot previous, Map<String, TaskProgress> observations, Set<UUID> completedPrerequisites,
        long now, boolean holdCompletedCycle, boolean bqPartyProgressFanout, long repeatAnchorMillis,
        boolean deferQuestCompletion) {
        return evaluate(membership, definition, previous, observations, completedPrerequisites, now,
            holdCompletedCycle, bqPartyProgressFanout, repeatAnchorMillis, deferQuestCompletion, false);
    }

    /**
     * The final Detect-batch settlement supplies whether a task selected as incomplete at batch start has since
     * completed. Its progress was intentionally applied while Quest completion was deferred, so it is no longer a
     * transition in the settlement snapshot itself.
     */
    public QuestEvaluation evaluate(ParticipantMembership membership, QuestDefinition definition,
        QuestProgressSnapshot previous, Map<String, TaskProgress> observations, Set<UUID> completedPrerequisites,
        long now, boolean holdCompletedCycle, boolean bqPartyProgressFanout, long repeatAnchorMillis,
        boolean deferQuestCompletion, boolean simultaneousDetectTaskCompleted) {
        ParticipantId participantId = membership.getParticipantId();
        if (previous != null && !previous.getParticipantId().equals(participantId)) {
            throw new IllegalArgumentException("progress participant does not match evaluation participant");
        }
        if (previous != null && (!previous.getQuestId().equals(definition.getId())
            || previous.getDefinitionVersion() != definition.getVersion())) {
            throw new IllegalArgumentException("progress definition identity does not match evaluated definition");
        }
        boolean unlocked = definition.getPrerequisites().isEmpty() || definition.getPrerequisiteLogic().satisfied(
            intersectionSize(definition.getPrerequisites(), completedPrerequisites), definition.getPrerequisites().size());
        boolean acceptsProgress = unlocked || definition.getBehavior().isProgressWhileLocked()
            || bqPartyProgressFanout;
        boolean acceptsQuestCompletion = unlocked || definition.getBehavior().isProgressWhileLocked();

        boolean beginNextCycle = beginsNextCycle(definition, previous, now, holdCompletedCycle, repeatAnchorMillis);
        boolean frozenCompletedCycle = previous != null && previous.getStatus() == QuestStatus.COMPLETED
            && !beginNextCycle;
        QuestProgressSnapshot progressBase = beginNextCycle ? null : previous;
        int cycle = previous == null ? 0 : previous.getCycle() + (beginNextCycle ? 1 : 0);

        Map<String, TaskProgress> merged = new LinkedHashMap<String, TaskProgress>();
        int requiredCount = 0;
        int completeCount = 0;
        boolean taskCompletedDuringEvaluation = false;
        for (TaskDefinition task : definition.getTasks()) {
            TaskProgress old = progressBase == null ? null : progressBase.getTasks().get(task.getKey());
            TaskProgress observed = observations == null ? null : observations.get(task.getKey());
            TaskProgress value = old == null ? TaskProgress.empty(task) : old;
            boolean wasComplete = value.isComplete();
            if (acceptsProgress && observed != null && !frozenCompletedCycle) {
                if (isNonConsumingSnapshotTask(task)) {
                    // BQ groupDetect resets every component before a party-wide item/fluid scan. Without
                    // it, individually satisfied requirements remain latched while partial requirements
                    // are recomputed from the current inventory. Task completion itself is always latched.
                    boolean groupDetect = QuestBooleanParameter.parse(
                        task.getParameters().get("groupDetect"), false);
                    value = groupDetect
                        ? value.replaceIncomplete(observed.getValues())
                        : value.replaceIncompleteComponents(observed.getValues());
                    if (observed.isComplete()) value = value.markComplete();
                } else {
                    value = value.merge(observed);
                }
            }
            if (!wasComplete && value.isComplete()) taskCompletedDuringEvaluation = true;
            merged.put(task.getKey(), value);
            if (!task.isOptional()) {
                requiredCount++;
                if (value.isComplete()) completeCount++;
            }
        }

        QuestStatus priorStatus = progressBase == null ? null : progressBase.getStatus();
        boolean alreadyComplete = priorStatus == QuestStatus.COMPLETED;
        boolean completed = alreadyComplete || !deferQuestCompletion && acceptsQuestCompletion
            && definition.getTaskLogic().satisfied(completeCount, requiredCount);
        if (!completed && !deferQuestCompletion && acceptsQuestCompletion
            && definition.getBehavior().isSimultaneous()
            && (taskCompletedDuringEvaluation || simultaneousDetectTaskCompleted)) {
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
            for (UUID recipient : membership.getPlayerIds()) {
                for (RewardDefinition reward : definition.getRewards()) {
                    entitlements.add(new RewardEntitlement(participantId, recipient, definition, reward, cycle,
                        definition.getBehavior().isAutoClaim()));
                }
            }
        }
        return new QuestEvaluation(next, entitlements);
    }

    boolean beginsNextCycle(QuestDefinition definition, QuestProgressSnapshot previous, long now,
        boolean holdCompletedCycle) {
        return beginsNextCycle(definition, previous, now, holdCompletedCycle,
            previous == null ? 0L : previous.getCompletedAt());
    }

    boolean beginsNextCycle(QuestDefinition definition, QuestProgressSnapshot previous, long now,
        boolean holdCompletedCycle, long repeatAnchorMillis) {
        return !holdCompletedCycle && previous != null && previous.getStatus() == QuestStatus.COMPLETED
            && definition.getRepeatPolicy().isAvailable(repeatAnchorMillis, now);
    }

    private static int intersectionSize(Set<UUID> required, Set<UUID> completed) {
        if (completed == null || completed.isEmpty()) return 0;
        int count = 0;
        for (UUID id : required) if (completed.contains(id)) count++;
        return count;
    }

    private static boolean isNonConsumingSnapshotTask(TaskDefinition task) {
        String type = task.getTypeId();
        boolean inventorySnapshot = "bq_standard:retrieval".equals(type)
            || "bq_standard:optional_retrieval".equals(type);
        boolean fluidSnapshot = "bq_standard:fluid".equals(type)
            && "galaxy:fluid_inventory_observed".equals(task.getParameters().get("factType"));
        return (inventorySnapshot || fluidSnapshot)
            && !QuestBooleanParameter.parse(task.getParameters().get("consume"), false);
    }

    private static boolean hasProgress(Map<String, TaskProgress> tasks) {
        for (TaskProgress task : tasks.values()) if (task.getValue() > 0 || task.isComplete()) return true;
        return false;
    }

}
