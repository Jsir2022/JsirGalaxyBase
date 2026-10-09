package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

/** Explicit, authenticated equivalent of BQ's quest Detect action for crafting-statistics tasks. */
public final class AuthenticatedCraftingStatisticsDetectionService {
    private static final String CRAFTING = "bq_standard:crafting";
    private static final String FACT_TYPE = "galaxy:item_produced";

    private final QuestDefinitionRepository definitions;
    private final QuestRuntimeRepository progress;
    private final QuestRuntimeService runtime;
    private final QuestEvaluationRequestProvider requests;
    private final CraftingStatisticReader statistics;
    private final String sourceServer;

    public AuthenticatedCraftingStatisticsDetectionService(QuestDefinitionRepository definitions,
        QuestRuntimeRepository progress, QuestRuntimeService runtime, QuestEvaluationRequestProvider requests,
        CraftingStatisticReader statistics, String sourceServer) {
        if (definitions == null || progress == null || runtime == null || requests == null || statistics == null
            || sourceServer == null || sourceServer.trim().isEmpty()) {
            throw new IllegalArgumentException("crafting detection dependencies and source server are required");
        }
        this.definitions = definitions;
        this.progress = progress;
        this.runtime = runtime;
        this.requests = requests;
        this.statistics = statistics;
        this.sourceServer = sourceServer.trim();
    }

    /**
     * Detects one explicitly selected task. Group/global quests are deliberately excluded until their BQ
     * participant UUID fan-out can be represented by authenticated cross-server statistics.
     */
    public List<FactProcessingResult> detect(UUID authenticatedPlayerId, UUID questId, String taskKey, long now) {
        if (authenticatedPlayerId == null || questId == null || taskKey == null || taskKey.trim().isEmpty()
            || now < 0L) throw new IllegalArgumentException("player, quest, task and time are required");
        Optional<StoredQuestDefinition> stored = definitions.findPublished(questId);
        if (!stored.isPresent()) return Collections.emptyList();
        QuestDefinition definition = stored.get().getDefinition();
        QuestBehavior behavior = definition.getBehavior();
        if (behavior.getParticipantScope() != QuestParticipantScope.PLAYER || behavior.isGlobal()) {
            return Collections.emptyList();
        }
        TaskDefinition selected = null;
        for (TaskDefinition task : definition.getTasks()) {
            if (taskKey.equals(task.getKey()) && CRAFTING.equals(task.getTypeId())
                && QuestBooleanParameter.parse(task.getParameters().get("allowCraftedFromStatistics"), false)) {
                selected = task;
                break;
            }
        }
        if (selected == null) return Collections.emptyList();

        ParticipantId participant = ParticipantId.player(authenticatedPlayerId);
        QuestProgressSnapshot previous = progress.findProgress(participant, questId, definition.getVersion())
            .orElse(null);
        int cycle = 0;
        if (previous != null) {
            if (previous.getStatus() == QuestStatus.LOCKED) return Collections.emptyList();
            if (previous.getStatus() == QuestStatus.COMPLETED) {
                if (!definition.getRepeatPolicy().isRepeatable()) return Collections.emptyList();
                if (previous.getCycle() == Integer.MAX_VALUE) throw new IllegalStateException(
                    "crafting detection cycle exhausted");
                cycle = previous.getCycle() + 1;
            } else {
                TaskProgress current = previous.getTasks().get(taskKey);
                if (current != null && current.isComplete()) return Collections.emptyList();
                cycle = previous.getCycle();
            }
        }

        List<FactProcessingResult> results = new ArrayList<FactProcessingResult>();
        for (String registryName : requiredItems(selected)) {
            OptionalLong value = statistics.craftedCount(authenticatedPlayerId, registryName);
            if (!value.isPresent() || value.getAsLong() <= 0L) continue;
            Map<String, String> attributes = new LinkedHashMap<String, String>();
            attributes.put("registryName", registryName);
            attributes.put("meta", "0");
            attributes.put("damageable", "false");
            attributes.put("amount", Long.toString(value.getAsLong()));
            attributes.put("channel", "craft-statistics");
            attributes.put("targetQuestId", questId.toString());
            attributes.put("targetQuestVersion", Integer.toString(definition.getVersion()));
            attributes.put("targetTaskKey", taskKey);
            String eventId = "craft-stat-detect:" + authenticatedPlayerId + ':' + questId + ':'
                + definition.getVersion() + ':' + cycle + ':' + taskKey + ':' + registryName + ':'
                + value.getAsLong();
            GameplayFact fact = new GameplayFact(sourceServer, eventId, authenticatedPlayerId, FACT_TYPE, now,
                attributes);
            final int expectedCycle = cycle;
            FactProcessingResult result = runtime.applyIf(fact, requests, planned -> unlockedTarget(definition, planned)
                && isDetectionEligible(authenticatedPlayerId, definition, taskKey, expectedCycle, now), now);
            if (result.getStatus() != FactProcessingStatus.IGNORED) results.add(result);
        }
        return Collections.unmodifiableList(results);
    }

    /** Rechecks cycle and reward state inside the same transaction that records the stable fact key. */
    private boolean isDetectionEligible(UUID playerId, QuestDefinition definition, String taskKey, int expectedCycle,
        long now) {
        ParticipantId participant = ParticipantId.player(playerId);
        QuestProgressSnapshot current = progress.findProgress(participant, definition.getId(), definition.getVersion())
            .orElse(null);
        if (current == null) return expectedCycle == 0;
        if (current.getStatus() == QuestStatus.LOCKED) return false;
        if (current.getStatus() != QuestStatus.COMPLETED) {
            if (expectedCycle != current.getCycle()) return false;
            TaskProgress taskProgress = current.getTasks().get(taskKey);
            return taskProgress == null || !taskProgress.isComplete();
        }
        if (!definition.getRepeatPolicy().isRepeatable() || current.getCycle() == Integer.MAX_VALUE
            || expectedCycle != current.getCycle() + 1) return false;
        int completedCycle = current.getCycle();
        if (progress.hasPendingConsumptionSubmission(participant, definition.getId(), definition.getVersion(),
            completedCycle)) return false;
        if (progress.hasClaimableRewardEntitlement(participant, definition.getId(), definition.getVersion(),
            completedCycle)) return false;
        long repeatAnchor = progress.findRepeatCycleAnchorMillis(participant, definition.getId(),
            definition.getVersion(), completedCycle, current.getCompletedAt());
        return definition.getRepeatPolicy().isAvailable(repeatAnchor, now);
    }

    private static Set<String> requiredItems(TaskDefinition task) {
        int count = integer(task.getParameters().get("item.count"));
        Set<String> names = new LinkedHashSet<String>();
        for (int index = 0; index < count; index++) {
            String name = task.getParameters().get("item." + index + ".registryName");
            if (name != null && !name.trim().isEmpty()) names.add(name.trim());
        }
        return names;
    }

    private static boolean unlockedTarget(QuestDefinition target, List<QuestEvaluationRequest> planned) {
        if (planned == null) return false;
        for (QuestEvaluationRequest request : planned) {
            QuestDefinition definition = request.getDefinition();
            if (!target.getId().equals(definition.getId()) || target.getVersion() != definition.getVersion()) continue;
            Set<UUID> prerequisites = definition.getPrerequisites();
            if (prerequisites.isEmpty()) return true;
            int completed = 0;
            for (UUID prerequisite : prerequisites) {
                if (request.getCompletedPrerequisites().contains(prerequisite)) completed++;
            }
            if (definition.getPrerequisiteLogic().satisfied(completed, prerequisites.size())) return true;
        }
        return false;
    }

    private static int integer(String value) {
        if (value == null || value.isEmpty()) return 0;
        try { return Math.max(0, Integer.parseInt(value)); }
        catch (NumberFormatException invalid) { return 0; }
    }
}
