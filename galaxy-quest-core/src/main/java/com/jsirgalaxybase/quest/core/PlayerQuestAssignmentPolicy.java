package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.List;

public final class PlayerQuestAssignmentPolicy implements QuestAssignmentPolicy {
    @Override
    public List<ParticipantMembership> select(QuestDefinition definition, List<ParticipantMembership> memberships) {
        for (ParticipantMembership membership : memberships) {
            if (membership.getParticipantId().getType() == ParticipantType.PLAYER) return Collections.singletonList(membership);
        }
        return Collections.emptyList();
    }
}
