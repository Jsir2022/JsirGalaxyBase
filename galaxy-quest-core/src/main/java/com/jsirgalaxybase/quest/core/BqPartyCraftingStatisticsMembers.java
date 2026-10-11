package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Resolves the local online Party members whose personal crafting counters BQ would inspect. */
public final class BqPartyCraftingStatisticsMembers {
    private BqPartyCraftingStatisticsMembers() { }

    public static List<UUID> localOnlineMembers(UUID sourcePlayerId,
        List<ParticipantMembership> memberships, QuestOnlinePlayerDirectory onlinePlayers) {
        if (sourcePlayerId == null || memberships == null || onlinePlayers == null) {
            throw new IllegalArgumentException("source player, memberships and online directory are required");
        }
        ParticipantMembership party = null;
        for (ParticipantMembership membership : memberships) {
            if (membership == null || membership.getParticipantId().getType() != ParticipantType.PARTY
                || !membership.contains(sourcePlayerId)) continue;
            if (party != null) throw new IllegalArgumentException(
                "multiple active PARTY memberships for crafting statistics source player");
            party = membership;
        }

        if (party == null) {
            return onlinePlayers.isOnline(sourcePlayerId) ? Collections.singletonList(sourcePlayerId)
                : Collections.<UUID>emptyList();
        }

        List<UUID> result = new ArrayList<UUID>();
        for (UUID member : party.getPlayerIds()) {
            if (member != null && onlinePlayers.isOnline(member)) result.add(member);
        }
        return Collections.unmodifiableList(result);
    }
}
