package com.jsirgalaxybase.quest.core;

import java.util.List;

/** Selects the authoritative participant scope for a definition; avoids advancing every scope accidentally. */
public interface QuestAssignmentPolicy {
    List<ParticipantMembership> select(QuestDefinition definition, List<ParticipantMembership> memberships);

    /** Selects progress owners for one fact; legacy policies remain fact-agnostic by default. */
    default List<ParticipantMembership> select(QuestDefinition definition, List<ParticipantMembership> memberships,
        GameplayFact fact) {
        return select(definition, memberships);
    }
}
