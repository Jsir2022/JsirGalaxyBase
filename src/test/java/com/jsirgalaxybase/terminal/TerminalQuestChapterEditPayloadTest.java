package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.QuestChapterEditOperation;
import com.jsirgalaxybase.quest.core.QuestChapterEditRequest;
import com.jsirgalaxybase.quest.core.QuestVisibility;

public class TerminalQuestChapterEditPayloadTest {
    @Test
    public void roundTripsEveryAllowListedChapterOperation() {
        UUID chapter = UUID.randomUUID();
        UUID quest = UUID.randomUUID();
        TerminalQuestChapterEditPayload source = new TerminalQuestChapterEditPayload(new QuestChapterEditRequest(
            chapter, 3, "hash", Arrays.asList(QuestChapterEditOperation.name("Chapter"),
                QuestChapterEditOperation.description("Description"), QuestChapterEditOperation.icon("item:1"),
                QuestChapterEditOperation.background("texture"), QuestChapterEditOperation.backgroundSize(512),
                QuestChapterEditOperation.visibility(QuestVisibility.SECRET),
                QuestChapterEditOperation.place(quest, -20, 30, 24, 32),
                QuestChapterEditOperation.move(quest, 10, 11), QuestChapterEditOperation.resize(quest, 40, 41),
                QuestChapterEditOperation.remove(quest))), "needle", "DRAFT", 2);
        TerminalQuestChapterEditPayload decoded = TerminalQuestChapterEditPayload.decode(source.encode());
        assertEquals(chapter, decoded.getRequest().getChapterId());
        assertEquals(10, decoded.getRequest().getOperations().size());
        assertEquals(QuestVisibility.SECRET.name(), decoded.getRequest().getOperations().get(5).getText());
        assertEquals(-20, decoded.getRequest().getOperations().get(6).getFirst());
        assertEquals("needle", decoded.getQuery());
        assertEquals(2, decoded.getPage());
    }

    @Test
    public void malformedAndTrailingPayloadsFailClosed() {
        assertInvalid("not-base64");
        TerminalQuestChapterEditPayload source = new TerminalQuestChapterEditPayload(new QuestChapterEditRequest(
            UUID.randomUUID(), 1, "hash", Arrays.asList(QuestChapterEditOperation.name("Name"))), "", "", 0);
        assertInvalid(source.encode() + "AA");
    }

    private static void assertInvalid(String value) {
        try {
            TerminalQuestChapterEditPayload.decode(value);
            fail("expected invalid payload");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
