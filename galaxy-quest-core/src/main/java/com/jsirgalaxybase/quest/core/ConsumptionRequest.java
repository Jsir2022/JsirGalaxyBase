package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Immutable intent to consume player-owned resources for one task. */
public final class ConsumptionRequest {
    private final String submissionKey;
    private final String sourceServer;
    private final UUID playerId;
    private final ParticipantId participantId;
    private final UUID questId;
    private final int definitionVersion;
    private final String taskKey;
    private final String kind;
    private final List<Long> amounts;
    private final Map<String, String> resourceParameters;
    private final long createdAt;

    public ConsumptionRequest(String submissionKey, String sourceServer, UUID playerId, ParticipantId participantId,
        UUID questId, int definitionVersion, String taskKey, String kind, List<Long> amounts,
        Map<String, String> resourceParameters, long createdAt) {
        this.submissionKey = text(submissionKey, "submissionKey");
        this.sourceServer = text(sourceServer, "sourceServer");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.participantId = Objects.requireNonNull(participantId, "participantId");
        if (participantId.getType() == ParticipantType.PLAYER && !participantId.getId().equals(playerId)) {
            throw new IllegalArgumentException("player progress must belong to the submitting player");
        }
        this.questId = Objects.requireNonNull(questId, "questId");
        if (definitionVersion < 1) throw new IllegalArgumentException("definitionVersion must be positive");
        this.definitionVersion = definitionVersion;
        this.taskKey = text(taskKey, "taskKey");
        this.kind = text(kind, "kind");
        if (amounts == null || amounts.isEmpty()) throw new IllegalArgumentException("amounts must not be empty");
        List<Long> amountCopy = new ArrayList<Long>(amounts.size());
        for (Long amount : amounts) {
            long value = Objects.requireNonNull(amount, "amount");
            if (value < 0) throw new IllegalArgumentException("amount must not be negative");
            amountCopy.add(value);
        }
        this.amounts = Collections.unmodifiableList(amountCopy);
        this.resourceParameters = Collections.unmodifiableMap(new LinkedHashMap<String, String>(
            resourceParameters == null ? Collections.<String, String>emptyMap() : resourceParameters));
        this.createdAt = createdAt;
    }

    public String getSubmissionKey() { return submissionKey; }
    public String getSourceServer() { return sourceServer; }
    public UUID getPlayerId() { return playerId; }
    public ParticipantId getParticipantId() { return participantId; }
    public UUID getQuestId() { return questId; }
    public int getDefinitionVersion() { return definitionVersion; }
    public String getTaskKey() { return taskKey; }
    public String getKind() { return kind; }
    public List<Long> getAmounts() { return amounts; }
    public Map<String, String> getResourceParameters() { return resourceParameters; }
    public long getCreatedAt() { return createdAt; }

    public GameplayFact confirmedFact() {
        return confirmedFact(amounts);
    }

    public GameplayFact confirmedFact(List<Long> appliedAmounts) {
        if (appliedAmounts == null || appliedAmounts.size() != amounts.size()) throw new IllegalArgumentException(
            "applied consumption vector width mismatch");
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("taskKey", taskKey);
        attributes.put("kind", kind);
        attributes.put("questId", questId.toString());
        attributes.put("definitionVersion", Integer.toString(definitionVersion));
        attributes.put("progressParticipantType", participantId.getType().name());
        attributes.put("progressParticipantId", participantId.getId().toString());
        attributes.put("value.count", Integer.toString(appliedAmounts.size()));
        for (int i = 0; i < appliedAmounts.size(); i++) {
            long value = appliedAmounts.get(i);
            if (value < 0 || value > amounts.get(i)) throw new IllegalArgumentException("invalid applied consumption amount");
            attributes.put("value." + i, Long.toString(value));
        }
        return new GameplayFact(sourceServer, "consumption:" + submissionKey, playerId,
            "galaxy:consumption_applied", createdAt, attributes);
    }

    private static String text(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
