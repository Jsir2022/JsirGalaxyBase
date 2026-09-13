package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.UUID;
import org.junit.Test;

public class TerminalQuestChapterOrderPayloadTest {
    @Test public void roundTripsMoveBeforeAndAppendIntent() {
        UUID source = UUID.randomUUID(), target = UUID.randomUUID();
        TerminalQuestChapterOrderPayload moved = TerminalQuestChapterOrderPayload.decode(new TerminalQuestChapterOrderPayload(source, target, "iron", "DRAFT", 3).encode());
        assertEquals(source, moved.getChapterId()); assertEquals(target, moved.getBeforeChapterId()); assertEquals("iron", moved.getQuery());
        assertNull(TerminalQuestChapterOrderPayload.decode(new TerminalQuestChapterOrderPayload(source, null, "", "", 0).encode()).getBeforeChapterId());
    }
}
