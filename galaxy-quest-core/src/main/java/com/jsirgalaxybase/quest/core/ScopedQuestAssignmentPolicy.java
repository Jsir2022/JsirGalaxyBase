package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.List;

/** Selects exactly one membership matching the definition scope; ambiguous memberships fail closed. */
public final class ScopedQuestAssignmentPolicy implements QuestAssignmentPolicy {
    @Override
    public List<ParticipantMembership> select(QuestDefinition definition, List<ParticipantMembership> memberships) {
        ParticipantType required = ParticipantType.valueOf(definition.getBehavior().getParticipantScope().name());
        ParticipantMembership selected = null;
        for (ParticipantMembership membership : memberships) {
            if (membership.getParticipantId().getType() != required) continue;
            if (selected != null) throw new IllegalArgumentException("multiple active memberships for scope " + required);
            selected = membership;
        }
        return selected == null ? Collections.<ParticipantMembership>emptyList()
            : Collections.singletonList(selected);
    }
}
