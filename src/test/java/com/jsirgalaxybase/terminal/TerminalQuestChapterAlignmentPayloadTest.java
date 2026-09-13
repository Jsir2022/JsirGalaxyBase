package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Test;
import com.jsirgalaxybase.quest.core.QuestChapterAlignment;
import com.jsirgalaxybase.quest.core.QuestChapterAlignmentRequest;

public class TerminalQuestChapterAlignmentPayloadTest {
    @Test public void roundTripsOnlyBoundedServerDerivableIntent(){UUID chapter=UUID.randomUUID(),first=UUID.randomUUID(),second=UUID.randomUUID();TerminalQuestChapterAlignmentPayload value=new TerminalQuestChapterAlignmentPayload(new QuestChapterAlignmentRequest(chapter,3,"hash",Arrays.asList(first,second),QuestChapterAlignment.CENTER_Y),"needle","DRAFT",2);TerminalQuestChapterAlignmentPayload copy=TerminalQuestChapterAlignmentPayload.decode(value.encode());assertEquals(chapter,copy.getRequest().getChapterId());assertEquals(QuestChapterAlignment.CENTER_Y,copy.getRequest().getAlignment());assertEquals(Arrays.asList(first,second),copy.getRequest().getQuestIds());}
}
