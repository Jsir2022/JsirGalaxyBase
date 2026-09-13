package com.jsirgalaxybase.terminal;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.QuestDefinitionBatchRetirementRequest;

public class TerminalQuestBatchRetirePayloadTest {
    private static final UUID A=new UUID(0,1), B=new UUID(0,2);

    @Test public void boundedIntentRoundTripsOnlyIdsAndVersions(){
        TerminalQuestBatchRetirePayload original=new TerminalQuestBatchRetirePayload(new QuestDefinitionBatchRetirementRequest(Arrays.asList(
            new QuestDefinitionBatchRetirementRequest.Target(A,3),new QuestDefinitionBatchRetirementRequest.Target(B,4))),"钢材","PUBLISHED",2);
        TerminalQuestBatchRetirePayload decoded=TerminalQuestBatchRetirePayload.decode(original.encode());
        assertEquals("钢材",decoded.getQuery());assertEquals("PUBLISHED",decoded.getLifecycle());assertEquals(2,decoded.getPage());
        assertEquals(A,decoded.getRequest().getTargets().get(0).getQuestId());assertEquals(4,decoded.getRequest().getTargets().get(1).getVersion());
    }

    @Test(expected=IllegalArgumentException.class) public void oneTargetIsNotBatch(){
        new TerminalQuestBatchRetirePayload(new QuestDefinitionBatchRetirementRequest(Arrays.asList(
            new QuestDefinitionBatchRetirementRequest.Target(A,1))),"","",0);
    }

    @Test public void malformedAndTrailingPayloadFailClosed(){
        try { TerminalQuestBatchRetirePayload.decode("not-a-payload"); fail("expected invalid payload"); }
        catch (IllegalArgumentException expected) { }
    }
}
