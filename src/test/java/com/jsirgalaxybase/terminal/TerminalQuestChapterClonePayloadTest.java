package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.QuestChapterCloneRequest;
import com.jsirgalaxybase.quest.core.QuestDefinitionCloneRequest;

public class TerminalQuestChapterClonePayloadTest {
    @Test
    public void roundTripsBoundedChapterAnchorAndOrderedSources() {
        UUID chapter = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        TerminalQuestChapterClonePayload source = new TerminalQuestChapterClonePayload(new QuestChapterCloneRequest(
            chapter, 4, "chapter-hash", new QuestDefinitionCloneRequest(Arrays.asList(
                new QuestDefinitionCloneRequest.Source(first, 2), new QuestDefinitionCloneRequest.Source(second, 7))),
            -100, 327), "steel", "DRAFT", 3);

        TerminalQuestChapterClonePayload decoded = TerminalQuestChapterClonePayload.decode(source.encode());
        assertEquals(chapter, decoded.getRequest().getChapterId());
        assertEquals(4, decoded.getRequest().getChapterVersion());
        assertEquals(-100, decoded.getRequest().getAnchorX());
        assertEquals(327, decoded.getRequest().getAnchorY());
        assertEquals(Arrays.asList(first, second), Arrays.asList(decoded.getRequest().getSources().getSources().get(0).getQuestId(),
            decoded.getRequest().getSources().getSources().get(1).getQuestId()));
        assertEquals("steel", decoded.getQuery());
        assertEquals("DRAFT", decoded.getLifecycle());
        assertEquals(3, decoded.getPage());
    }

    @Test
    public void malformedAndTrailingPayloadsFailClosed() {
        assertInvalid("not-base64");
        TerminalQuestChapterClonePayload source = new TerminalQuestChapterClonePayload(new QuestChapterCloneRequest(
            UUID.randomUUID(), 1, "hash", new QuestDefinitionCloneRequest(Arrays.asList(
                new QuestDefinitionCloneRequest.Source(UUID.randomUUID(), 1))), 0, 0), "", "", 0);
        assertInvalid(source.encode() + "AA");
    }

    private static void assertInvalid(String value) {
        try { TerminalQuestChapterClonePayload.decode(value); fail("expected invalid payload"); }
        catch (IllegalArgumentException expected) { /* expected */ }
    }
}
