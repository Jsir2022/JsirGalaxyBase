package com.jsirgalaxybase.quest.core;

import java.util.Objects;
import java.util.UUID;

/** Bounded member row visible only to an authenticated member of the same participant. */
public final class ParticipantMemberEntry {
    private final UUID playerId;
    private final ParticipantMemberRole role;
    private final long joinedAt;
    private final long membershipVersion;

    public ParticipantMemberEntry(UUID playerId, ParticipantMemberRole role, long joinedAt,
        long membershipVersion) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.role = Objects.requireNonNull(role, "role");
        if (joinedAt < 0L || membershipVersion < 0L) throw new IllegalArgumentException("invalid member metadata");
        this.joinedAt = joinedAt;
        this.membershipVersion = membershipVersion;
    }

    public UUID getPlayerId() { return playerId; }
    public ParticipantMemberRole getRole() { return role; }
    public long getJoinedAt() { return joinedAt; }
    public long getMembershipVersion() { return membershipVersion; }
}
