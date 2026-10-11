package com.jsirgalaxybase.quest.core;

import java.util.Objects;
import java.util.UUID;

/** Stable published-task identity used by automatic BQ crafting-statistic detection. */
public final class CraftingStatisticsDetectionTarget {
    private final UUID questId;
    private final int questVersion;
    private final String taskKey;

    public CraftingStatisticsDetectionTarget(UUID questId, int questVersion, String taskKey) {
        if (questId == null || questVersion < 1 || taskKey == null || taskKey.trim().isEmpty()) {
            throw new IllegalArgumentException("quest, positive version and task key are required");
        }
        this.questId = questId;
        this.questVersion = questVersion;
        this.taskKey = taskKey.trim();
    }

    public UUID getQuestId() { return questId; }
    public int getQuestVersion() { return questVersion; }
    public String getTaskKey() { return taskKey; }

    @Override public boolean equals(Object other) {
        if (!(other instanceof CraftingStatisticsDetectionTarget)) return false;
        CraftingStatisticsDetectionTarget value = (CraftingStatisticsDetectionTarget) other;
        return questVersion == value.questVersion && questId.equals(value.questId) && taskKey.equals(value.taskKey);
    }

    @Override public int hashCode() { return Objects.hash(questId, questVersion, taskKey); }
}
