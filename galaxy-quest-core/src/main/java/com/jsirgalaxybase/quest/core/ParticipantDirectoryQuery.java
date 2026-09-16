package com.jsirgalaxybase.quest.core;

import java.util.List;
import java.util.UUID;

/** Identity-scoped directory; implementations must never accept an owner supplied by a client payload. */
public interface ParticipantDirectoryQuery {
    List<ParticipantDirectoryEntry> findActiveForPlayer(UUID authenticatedPlayerId);
    List<ParticipantMemberEntry> findMembers(UUID authenticatedPlayerId, ParticipantId participantId);
}
