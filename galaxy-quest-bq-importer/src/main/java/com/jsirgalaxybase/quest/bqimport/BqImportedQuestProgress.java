package com.jsirgalaxybase.quest.bqimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class BqImportedQuestProgress {
    private final UUID questId;
    private final boolean completed;
    private final boolean claimed;
    private final long timestamp;
    private final List<BqImportedTaskProgress> tasks;
    private final String rawTasksJson;

    BqImportedQuestProgress(UUID questId, boolean completed, boolean claimed, long timestamp,
        List<BqImportedTaskProgress> tasks, String rawTasksJson) {
        this.questId = questId;
        this.completed = completed;
        this.claimed = claimed;
        this.timestamp = timestamp;
        this.tasks = Collections.unmodifiableList(new ArrayList<BqImportedTaskProgress>(tasks));
        this.rawTasksJson = rawTasksJson;
    }

    public UUID getQuestId() { return questId; }
    public boolean isCompleted() { return completed; }
    public boolean isClaimed() { return claimed; }
    public long getTimestamp() { return timestamp; }
    public List<BqImportedTaskProgress> getTasks() { return tasks; }
    public String getRawTasksJson() { return rawTasksJson; }
}
