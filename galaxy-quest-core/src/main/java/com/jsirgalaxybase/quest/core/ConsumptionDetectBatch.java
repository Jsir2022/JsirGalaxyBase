package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Durable cursor for one authenticated Quest-level consuming Detect pass. */
public final class ConsumptionDetectBatch {
    private final String batchId;
    private final String sourceServer;
    private final UUID playerId;
    private final ParticipantId participantId;
    private final UUID questId;
    private final int definitionVersion;
    private final int cycle;
    private final List<String> taskKeys;
    private final int nextTaskIndex;
    private final long createdAt;

    public ConsumptionDetectBatch(String batchId, String sourceServer, UUID playerId, ParticipantId participantId,
        UUID questId, int definitionVersion, int cycle, List<String> taskKeys, int nextTaskIndex, long createdAt) {
        this.batchId = text(batchId, "batchId");
        this.sourceServer = text(sourceServer, "sourceServer");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.participantId = Objects.requireNonNull(participantId, "participantId");
        if (participantId.getType() == ParticipantType.PLAYER && !participantId.getId().equals(playerId)) {
            throw new IllegalArgumentException("PLAYER Detect batch must belong to its authenticated player");
        }
        this.questId = Objects.requireNonNull(questId, "questId");
        if (definitionVersion < 1 || cycle < 0) throw new IllegalArgumentException("invalid Detect batch version/cycle");
        this.definitionVersion = definitionVersion;
        this.cycle = cycle;
        if (taskKeys == null || taskKeys.isEmpty()) throw new IllegalArgumentException("taskKeys must not be empty");
        List<String> copy = new ArrayList<String>(taskKeys.size());
        for (String key : taskKeys) {
            String normalized = text(key, "taskKey");
            if (normalized.length() > 128 || copy.contains(normalized)) throw new IllegalArgumentException(
                "Detect batch task keys must be unique and at most 128 characters");
            copy.add(normalized);
        }
        if (nextTaskIndex < 0 || nextTaskIndex > copy.size()) throw new IllegalArgumentException(
            "nextTaskIndex is outside the batch");
        this.taskKeys = Collections.unmodifiableList(copy);
        this.nextTaskIndex = nextTaskIndex;
        this.createdAt = createdAt;
    }

    public String getBatchId() { return batchId; }
    public String getSourceServer() { return sourceServer; }
    public UUID getPlayerId() { return playerId; }
    public ParticipantId getParticipantId() { return participantId; }
    public UUID getQuestId() { return questId; }
    public int getDefinitionVersion() { return definitionVersion; }
    public int getCycle() { return cycle; }
    public List<String> getTaskKeys() { return taskKeys; }
    public int getNextTaskIndex() { return nextTaskIndex; }
    public long getCreatedAt() { return createdAt; }
    public boolean isComplete() { return nextTaskIndex >= taskKeys.size(); }
    public String getCurrentTaskKey() { return isComplete() ? "" : taskKeys.get(nextTaskIndex); }

    private static String text(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
