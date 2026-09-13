package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

public class QuestCenterSnapshotTest {
    @Test public void joinsDefinitionProgressAndRewardStateWithoutExposingMutableLists(){
        UUID player=UUID.randomUUID(),questId=UUID.randomUUID();
        QuestDefinition definition=new QuestDefinition(questId,1,"Quest","",QuestLogic.AND,QuestLogic.AND,
            Collections.<UUID>emptySet(),Collections.<TaskDefinition>emptyList(),Collections.<RewardDefinition>emptyList());
        QuestProgressSnapshot progress=new QuestProgressSnapshot(ParticipantId.player(player),questId,1,
            QuestStatus.COMPLETED,Collections.<String,TaskProgress>emptyMap(),10L,0);
        QuestRewardSummary rewards=new QuestRewardSummary(1,1,0,0,0,0);
        QuestCenterEntry entry=new QuestCenterEntry(definition,progress,rewards);
        QuestCenterSnapshot snapshot=new QuestCenterSnapshot(ParticipantId.player(player),
            Collections.<QuestChapterDefinition>emptyList(),Collections.singletonList(entry));
        assertEquals(QuestStatus.COMPLETED,snapshot.getQuests().get(0).getStatus());
        assertTrue(snapshot.getQuests().get(0).getRewards().hasClaimable());
        try{snapshot.getQuests().clear();fail("expected immutable list");}catch(UnsupportedOperationException expected){assertTrue(true);}
    }
}
