package com.jsirgalaxybase.quest.core;

import java.util.List;
import java.util.UUID;

public interface ParticipantMembershipResolver {
    List<ParticipantMembership> resolve(UUID playerId);
}
