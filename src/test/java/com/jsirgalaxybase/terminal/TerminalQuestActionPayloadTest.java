package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class TerminalQuestActionPayloadTest {
    @Test
    public void roundTripsNavigationIntentWithoutPlayerIdentity() {
        TerminalQuestActionPayload original = new TerminalQuestActionPayload(
            "chapter:steel", "quest:blast_furnace", "claimable", "高炉 | blast", 3, 27);

        String encoded = original.encode();
        TerminalQuestActionPayload decoded = TerminalQuestActionPayload.decode(encoded);

        assertEquals("chapter:steel", decoded.getChapterId());
        assertEquals("quest:blast_furnace", decoded.getQuestId());
        assertEquals("claimable", decoded.getFilter());
        assertEquals("高炉 | blast", decoded.getQuery());
        assertEquals(3, decoded.getChapterPage());
        assertEquals(27, decoded.getQuestPage());
        assertFalse(encoded.toLowerCase().contains("player"));
        assertFalse(encoded.toLowerCase().contains("uuid"));
    }

    @Test
    public void roundTripsBoundedRewardChoiceWithoutAuthorityFields() {
        TerminalQuestActionPayload decoded=TerminalQuestActionPayload.decode(new TerminalQuestActionPayload(
            "chapter","550e8400-e29b-41d4-a716-446655440000","all","",0,0,"pick-material",2).encode());
        assertEquals("pick-material",decoded.getRewardKey());assertEquals(2,decoded.getChoiceIndex());
        TerminalQuestActionPayload bounded=new TerminalQuestActionPayload("","","all","",0,0,repeat('r',100),99);
        assertEquals(64,bounded.getRewardKey().length());assertEquals(31,bounded.getChoiceIndex());
    }

    @Test
    public void malformedAndOversizedIntentFailsClosedToBoundedValues() {
        TerminalQuestActionPayload malformed = TerminalQuestActionPayload.decode("not-a-valid-payload");
        assertEquals("", malformed.getChapterId());
        assertEquals("all", malformed.getFilter());

        TerminalQuestActionPayload bounded = new TerminalQuestActionPayload(
            repeat('c', 100), repeat('q', 100), repeat('f', 30), repeat('x', 140), -2, Integer.MAX_VALUE);
        assertEquals(64, bounded.getChapterId().length());
        assertEquals(64, bounded.getQuestId().length());
        assertEquals(16, bounded.getFilter().length());
        assertEquals(96, bounded.getQuery().length());
        assertEquals(0, bounded.getChapterPage());
        assertEquals(100000, bounded.getQuestPage());
    }

    @Test
    public void roundTripsBoundedManagementFilterWithoutAuthority() {
        TerminalQuestActionPayload decoded=TerminalQuestActionPayload.decode(new TerminalQuestActionPayload(
            "","550e8400-e29b-41d4-a716-446655440000","all","steel",0,0,"",-1,"DRAFT",42,7,"sha256-value").encode());
        assertEquals("DRAFT",decoded.getManagementLifecycle());assertEquals(42,decoded.getManagementPage());
        assertEquals(7,decoded.getManagementVersion());
        assertEquals("sha256-value",decoded.getManagementHash());
        assertFalse(decoded.encode().toLowerCase().contains("operator"));
    }

    @Test public void roundTripsDedicatedDraftTemplateWithoutUsingRewardFields(){TerminalQuestActionPayload decoded=TerminalQuestActionPayload.decode(new TerminalQuestActionPayload("","","all","新任务",0,0,"",-1,"DRAFT",0,0,"","MANUAL_CHECK").encode());assertEquals("MANUAL_CHECK",decoded.getManagementTemplate());assertEquals("",decoded.getRewardKey());assertEquals(-1,decoded.getChoiceIndex());}

    private static String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int index = 0; index < count; index++) result.append(value);
        return result.toString();
    }
}
