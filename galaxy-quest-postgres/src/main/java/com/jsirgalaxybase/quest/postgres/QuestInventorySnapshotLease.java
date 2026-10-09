package com.jsirgalaxybase.quest.postgres;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Short-lived server-authored inventory snapshot for one active player session. */
public final class QuestInventorySnapshotLease {
    private final UUID playerId;
    private final String sourceServer;
    private final UUID sessionId;
    private final Map<String, String> attributes;

    public QuestInventorySnapshotLease(UUID playerId, String sourceServer, UUID sessionId,
        Map<String, String> attributes) {
        if (playerId == null || sessionId == null || sourceServer == null || sourceServer.trim().isEmpty()
            || attributes == null) throw new IllegalArgumentException("complete inventory lease is required");
        this.playerId = playerId;
        this.sourceServer = sourceServer.trim();
        this.sessionId = sessionId;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<String, String>(attributes));
    }

    public UUID getPlayerId() { return playerId; }
    public String getSourceServer() { return sourceServer; }
    public UUID getSessionId() { return sessionId; }
    public Map<String, String> getAttributes() { return attributes; }
}
