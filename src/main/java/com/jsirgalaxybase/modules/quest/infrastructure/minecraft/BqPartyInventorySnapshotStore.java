package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.postgres.QuestInventorySnapshotLease;

/** Persistence port used by the BQ party inventory observation pipeline. */
interface BqPartyInventorySnapshotStore {
    void claimSession(UUID playerId, String sourceServer, UUID sessionId, Map<String, String> attributes,
        long leaseMillis);

    boolean publish(UUID playerId, String sourceServer, UUID sessionId, Map<String, String> attributes,
        long leaseMillis);

    boolean lockActiveParty(ParticipantId party);

    Map<UUID, QuestInventorySnapshotLease> findActive(List<UUID> playerIds);

    int deleteSession(UUID playerId, String sourceServer, UUID sessionId);

    int deleteExpired();
}
