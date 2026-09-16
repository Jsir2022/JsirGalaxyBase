package com.jsirgalaxybase.quest.core;

import java.util.List;
import java.util.UUID;

public interface ParticipantMembershipResolver {
    List<ParticipantMembership> resolve(UUID playerId);

    /** Resolves current recipients for an already-authorized progress subject. */
    default ParticipantMembership resolveParticipant(ParticipantId participantId) {
        if (participantId.getType() == ParticipantType.PLAYER) return ParticipantMembership.player(participantId.getId());
        return null;
    }
}
