package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class PlayerOnlyMembershipResolver implements ParticipantMembershipResolver {
    @Override
    public List<ParticipantMembership> resolve(UUID playerId) {
        return Collections.singletonList(ParticipantMembership.player(playerId));
    }
}
