package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class GameplayFact {
    private final String sourceServer;
    private final String eventId;
    private final UUID playerId;
    private final String typeId;
    private final long occurredAt;
    private final Map<String, String> attributes;

    public GameplayFact(String sourceServer, String eventId, UUID playerId, String typeId, long occurredAt,
        Map<String, String> attributes) {
        this.sourceServer = requireText(sourceServer, "sourceServer");
        this.eventId = requireText(eventId, "eventId");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.typeId = requireText(typeId, "typeId");
        this.occurredAt = occurredAt;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<String, String>(attributes == null
            ? Collections.<String, String>emptyMap() : attributes));
    }

    public String getFactKey() { return sourceServer + ":" + eventId; }
    public String getSourceServer() { return sourceServer; }
    public String getEventId() { return eventId; }
    public UUID getPlayerId() { return playerId; }
    public String getTypeId() { return typeId; }
    public long getOccurredAt() { return occurredAt; }
    public Map<String, String> getAttributes() { return attributes; }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
