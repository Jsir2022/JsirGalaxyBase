package com.jsirgalaxybase.quest.core;

import java.util.UUID;

/** PostgreSQL-backed authority for cross-server participant membership changes. */
public interface ParticipantAdministrationRepository {
    ParticipantMutationStatus create(ParticipantId participant, String displayName, UUID authenticatedOwner,
        long changedAt);
    ParticipantMutationStatus addMember(ParticipantId participant, UUID authenticatedActor, UUID playerId,
        ParticipantMemberRole role, long expectedRevision, long changedAt);
    ParticipantMutationStatus removeMember(ParticipantId participant, UUID authenticatedActor, UUID playerId,
        long expectedRevision, long changedAt);
    ParticipantMutationStatus transferOwnership(ParticipantId participant, UUID authenticatedOwner,
        UUID newOwner, long expectedRevision, long changedAt);
}
