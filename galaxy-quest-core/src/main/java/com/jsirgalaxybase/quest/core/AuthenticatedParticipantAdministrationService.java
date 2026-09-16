package com.jsirgalaxybase.quest.core;

import java.util.UUID;

/** Validates bounded commands while identity always comes from the authenticated server session. */
public final class AuthenticatedParticipantAdministrationService {
    private final ParticipantAdministrationRepository repository;
    private final QuestTransaction transaction;

    public AuthenticatedParticipantAdministrationService(ParticipantAdministrationRepository repository,
        QuestTransaction transaction) {
        if (repository == null || transaction == null) throw new IllegalArgumentException("dependencies are required");
        this.repository = repository;
        this.transaction = transaction;
    }

    public ParticipantMutationStatus create(final ParticipantId participant, final String displayName,
        final UUID authenticatedOwner, final long changedAt) {
        validate(participant, authenticatedOwner, changedAt);
        final String name = displayName == null ? "" : displayName.trim();
        if (name.isEmpty() || name.length() > 128) return ParticipantMutationStatus.INVALID;
        return transaction.inTransaction(() -> repository.create(participant, name, authenticatedOwner, changedAt));
    }

    public ParticipantMutationStatus addMember(final ParticipantId participant, final UUID authenticatedActor,
        final UUID playerId, final ParticipantMemberRole role, final long expectedRevision, final long changedAt) {
        validate(participant, authenticatedActor, changedAt);
        if (playerId == null || role == null || role == ParticipantMemberRole.OWNER || expectedRevision < 0L) {
            return ParticipantMutationStatus.INVALID;
        }
        return transaction.inTransaction(() -> repository.addMember(participant, authenticatedActor, playerId, role,
            expectedRevision, changedAt));
    }

    public ParticipantMutationStatus removeMember(final ParticipantId participant, final UUID authenticatedActor,
        final UUID playerId, final long expectedRevision, final long changedAt) {
        validate(participant, authenticatedActor, changedAt);
        if (playerId == null || expectedRevision < 0L) return ParticipantMutationStatus.INVALID;
        return transaction.inTransaction(() -> repository.removeMember(participant, authenticatedActor, playerId,
            expectedRevision, changedAt));
    }

    public ParticipantMutationStatus transferOwnership(final ParticipantId participant,
        final UUID authenticatedOwner, final UUID newOwner, final long expectedRevision, final long changedAt) {
        validate(participant, authenticatedOwner, changedAt);
        if (newOwner == null || newOwner.equals(authenticatedOwner) || expectedRevision < 0L) {
            return ParticipantMutationStatus.INVALID;
        }
        return transaction.inTransaction(() -> repository.transferOwnership(participant, authenticatedOwner,
            newOwner, expectedRevision, changedAt));
    }

    private static void validate(ParticipantId participant, UUID actor, long changedAt) {
        if (participant == null || actor == null || changedAt < 0L || participant.getType() == ParticipantType.PLAYER) {
            throw new IllegalArgumentException("group participant, authenticated actor, and time are required");
        }
    }
}
