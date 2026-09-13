package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class QuestProgressSnapshot {
    private final ParticipantId participantId;
    private final UUID questId;
    private final int definitionVersion;
    private final QuestStatus status;
    private final Map<String, TaskProgress> tasks;
    private final long completedAt;
    private final int cycle;

    public QuestProgressSnapshot(UUID playerId, UUID questId, int definitionVersion, QuestStatus status,
        Map<String, TaskProgress> tasks, long completedAt) {
        this(ParticipantId.player(playerId), questId, definitionVersion, status, tasks, completedAt, 0);
    }

    public QuestProgressSnapshot(ParticipantId participantId, UUID questId, int definitionVersion, QuestStatus status,
        Map<String, TaskProgress> tasks, long completedAt, int cycle) {
        this.participantId = Objects.requireNonNull(participantId, "participantId");
        this.questId = Objects.requireNonNull(questId, "questId");
        this.definitionVersion = definitionVersion;
        this.status = Objects.requireNonNull(status, "status");
        this.tasks = Collections.unmodifiableMap(new LinkedHashMap<String, TaskProgress>(tasks));
        this.completedAt = completedAt;
        if (cycle < 0) throw new IllegalArgumentException("cycle must not be negative");
        this.cycle = cycle;
    }

    public ParticipantId getParticipantId() { return participantId; }
    public UUID getPlayerId() { return participantId.getId(); }
    public UUID getQuestId() { return questId; }
    public int getDefinitionVersion() { return definitionVersion; }
    public QuestStatus getStatus() { return status; }
    public Map<String, TaskProgress> getTasks() { return tasks; }
    public long getCompletedAt() { return completedAt; }
    public int getCycle() { return cycle; }
}
