package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TerminalQuestParticipantActionPayloadTest {
    @Test public void roundTripsBoundedIntentWithoutActorIdentity() {
        TerminalQuestParticipantActionPayload decoded=TerminalQuestParticipantActionPayload.decode(
            new TerminalQuestParticipantActionPayload("TEAM","550e8400-e29b-41d4-a716-446655440000",
                "银河工业组","Jsir2022","ADMIN",42L).encode());
        assertEquals("TEAM",decoded.getParticipantType());
        assertEquals("550e8400-e29b-41d4-a716-446655440000",decoded.getParticipantId());
        assertEquals("银河工业组",decoded.getDisplayName());
        assertEquals("Jsir2022",decoded.getTargetPlayer());
        assertEquals("ADMIN",decoded.getTargetRole());
        assertEquals(42L,decoded.getExpectedRevision());
    }

    @Test public void malformedIntentBecomesEmptyAndNegativeRevisionIsClamped() {
        assertEquals("",TerminalQuestParticipantActionPayload.decode("broken").getParticipantType());
        assertEquals(0L,new TerminalQuestParticipantActionPayload("PARTY","","","","",-3L)
            .getExpectedRevision());
    }
}
