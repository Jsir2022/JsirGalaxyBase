package com.jsirgalaxybase.quest.core;

import java.util.Objects;
import java.util.UUID;

/** Minimal server-side intent. Resource definitions and quantities are deliberately absent. */
public final class ConsumptionIntent {
    private final String submissionKey;
    private final String sourceServer;
    private final UUID playerId;
    private final UUID questId;
    private final String taskKey;
    private final long createdAt;

    public ConsumptionIntent(String submissionKey, String sourceServer, UUID playerId, UUID questId, String taskKey,
        long createdAt) {
        this.submissionKey = text(submissionKey, "submissionKey");
        this.sourceServer = text(sourceServer, "sourceServer");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.questId = Objects.requireNonNull(questId, "questId");
        this.taskKey = text(taskKey, "taskKey");
        this.createdAt = createdAt;
    }

    public String getSubmissionKey() { return submissionKey; }
    public String getSourceServer() { return sourceServer; }
    public UUID getPlayerId() { return playerId; }
    public UUID getQuestId() { return questId; }
    public String getTaskKey() { return taskKey; }
    public long getCreatedAt() { return createdAt; }

    private static String text(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
