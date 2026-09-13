package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.util.Arrays;import java.util.Collections;import java.util.UUID;
import org.junit.Test;
import com.jsirgalaxybase.quest.core.*;

public class TerminalQuestDraftEditPayloadTest {
    @Test public void roundTripsBoundedSemanticOperations(){UUID id=UUID.randomUUID();
        QuestDraftEditRequest request=new QuestDraftEditRequest(id,3,"hash-3",Arrays.asList(
            QuestDraftEditOperation.name("新任务"),QuestDraftEditOperation.taskLogic(QuestLogic.OR),
            QuestDraftEditOperation.replaceTask("old",new TaskDefinition("new","bq_standard:retrieval",true,Collections.singletonMap("item","minecraft:iron_ingot"))),
            QuestDraftEditOperation.addReward(new RewardDefinition("xp","bq_standard:xp",Collections.singletonMap("amount","50"))),
            QuestDraftEditOperation.option("behavior.autoClaim","true")));
        TerminalQuestDraftEditPayload decoded=TerminalQuestDraftEditPayload.decode(new TerminalQuestDraftEditPayload(request,"steel","DRAFT",7).encode());
        assertEquals(id,decoded.getRequest().getQuestId());assertEquals(3,decoded.getRequest().getVersion());assertEquals("hash-3",decoded.getRequest().getExpectedContentHash());
        assertEquals(5,decoded.getRequest().getOperations().size());assertEquals("minecraft:iron_ingot",decoded.getRequest().getOperations().get(2).getTask().getParameters().get("item"));assertEquals("behavior.autoClaim",decoded.getRequest().getOperations().get(4).getKey());assertEquals("steel",decoded.getQuery());assertEquals(7,decoded.getPage());}
    @Test public void rejectsMalformedOversizedAndTrailingPayloads(){reject("broken");StringBuilder huge=new StringBuilder();while(huge.length()<196609)huge.append('a');reject(huge.toString());String valid=new TerminalQuestDraftEditPayload(new QuestDraftEditRequest(UUID.randomUUID(),1,"hash",Collections.singletonList(QuestDraftEditOperation.name("x"))),"","",0).encode();reject(valid+"AA");}
    private static void reject(String value){try{TerminalQuestDraftEditPayload.decode(value);fail("expected rejection");}catch(IllegalArgumentException expected){}}
}
