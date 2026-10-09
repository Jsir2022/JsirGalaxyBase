package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class QuestEvaluationRequest {
    private final QuestDefinition definition;
    private final ParticipantMembership membership;
    private final ParticipantMembership factMembership;
    private final Set<UUID> completedPrerequisites;

    public QuestEvaluationRequest(QuestDefinition definition, ParticipantMembership membership,
        Set<UUID> completedPrerequisites) {
        this(definition, membership, membership, completedPrerequisites);
    }

    /**
     * Creates a request whose fact was authorized by a wider server-resolved membership than its progress owner.
     * This is used only for BQ-style per-player party fan-out (for example Hunt and crafting events).
     */
    public QuestEvaluationRequest(QuestDefinition definition, ParticipantMembership membership,
        ParticipantMembership factMembership, Set<UUID> completedPrerequisites) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.membership = Objects.requireNonNull(membership, "membership");
        this.factMembership = Objects.requireNonNull(factMembership, "factMembership");
        this.completedPrerequisites = Collections.unmodifiableSet(new LinkedHashSet<UUID>(completedPrerequisites == null
            ? Collections.<UUID>emptySet() : completedPrerequisites));
        if (!sameMembership(membership, factMembership)
            && (factMembership.getParticipantId().getType() != ParticipantType.PARTY
                || membership.getParticipantId().getType() != ParticipantType.PLAYER
                || !factMembership.contains(membership.getParticipantId().getId()))) {
            throw new IllegalArgumentException("fact membership cannot authorize this progress owner");
        }
    }

    public QuestDefinition getDefinition() { return definition; }
    public ParticipantMembership getMembership() { return membership; }
    public ParticipantMembership getFactMembership() { return factMembership; }
    public Set<UUID> getCompletedPrerequisites() { return completedPrerequisites; }

    private static boolean sameMembership(ParticipantMembership left, ParticipantMembership right) {
        return left.getParticipantId().equals(right.getParticipantId())
            && left.getPlayerIds().equals(right.getPlayerIds());
    }
}
