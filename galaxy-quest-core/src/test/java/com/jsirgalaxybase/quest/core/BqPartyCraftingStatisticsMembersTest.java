package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

public final class BqPartyCraftingStatisticsMembersTest {
    @Test
    public void selectsOnlyLocallyOnlineMembersAndKeepsTheirIdentitiesSeparate() {
        UUID source = id(1L);
        UUID onlinePeer = id(2L);
        UUID offlinePeer = id(3L);
        ParticipantMembership party = new ParticipantMembership(ParticipantId.party(id(4L)),
            new LinkedHashSet<UUID>(Arrays.asList(source, onlinePeer, offlinePeer)));
        Set<UUID> online = new LinkedHashSet<UUID>(Arrays.asList(source, onlinePeer));

        List<UUID> selected = BqPartyCraftingStatisticsMembers.localOnlineMembers(source,
            Arrays.asList(ParticipantMembership.player(source), party), online::contains);

        assertEquals(Arrays.asList(source, onlinePeer), selected);
        assertTrue("the offline Party member's statistic must not be fabricated or aggregated",
            !selected.contains(offlinePeer));
    }

    @Test
    public void soloCraftingSamplesOnlyTheOnlineSourcePlayer() {
        UUID source = id(11L);
        assertEquals(Collections.singletonList(source), BqPartyCraftingStatisticsMembers.localOnlineMembers(source,
            Collections.singletonList(ParticipantMembership.player(source)), player -> source.equals(player)));
        assertTrue(BqPartyCraftingStatisticsMembers.localOnlineMembers(source,
            Collections.singletonList(ParticipantMembership.player(source)), player -> false).isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAmbiguousCurrentPartyMembershipInsteadOfSamplingBothParties() {
        UUID source = id(21L);
        ParticipantMembership first = new ParticipantMembership(ParticipantId.party(id(22L)),
            Collections.singleton(source));
        ParticipantMembership second = new ParticipantMembership(ParticipantId.party(id(23L)),
            Collections.singleton(source));
        BqPartyCraftingStatisticsMembers.localOnlineMembers(source, Arrays.asList(first, second), player -> true);
    }

    private static UUID id(long value) { return new UUID(0L, value); }
}
