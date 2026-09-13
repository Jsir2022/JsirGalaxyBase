package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ParticipantMembership {
    private final ParticipantId participantId;
    private final Set<UUID> playerIds;

    public ParticipantMembership(ParticipantId participantId, Set<UUID> playerIds) {
        this.participantId = Objects.requireNonNull(participantId, "participantId");
        this.playerIds = Collections.unmodifiableSet(new LinkedHashSet<UUID>(Objects.requireNonNull(playerIds,
            "playerIds")));
        if (this.playerIds.isEmpty()) throw new IllegalArgumentException("participant must have at least one player");
        if (participantId.getType() == ParticipantType.PLAYER && (!this.playerIds.contains(participantId.getId())
            || this.playerIds.size() != 1)) {
            throw new IllegalArgumentException("player participant membership must contain exactly its own UUID");
        }
    }

    public static ParticipantMembership player(UUID playerId) {
        return new ParticipantMembership(ParticipantId.player(playerId), Collections.singleton(playerId));
    }

    public ParticipantId getParticipantId() { return participantId; }
    public Set<UUID> getPlayerIds() { return playerIds; }
    public boolean contains(UUID playerId) { return playerIds.contains(playerId); }
}
