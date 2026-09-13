package com.jsirgalaxybase.modules.quest.application;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.*;
import com.jsirgalaxybase.terminal.TerminalTrackedQuestSnapshot;

public class TrackedQuestSnapshotFactoryTest {
    @Test public void includesOnlyTrackedQuestsAndBoundsTasks(){UUID player=UUID.randomUUID(),trackedId=UUID.randomUUID(),otherId=UUID.randomUUID();QuestCenterEntry tracked=new QuestCenterEntry(definition(trackedId,"Tracked",4),null,QuestRewardSummary.EMPTY,QuestStatus.IN_PROGRESS,true);QuestCenterEntry other=new QuestCenterEntry(definition(otherId,"Other",1),null,QuestRewardSummary.EMPTY,QuestStatus.AVAILABLE,false);QuestCenterQuery query=new QuestCenterQuery(){public QuestCenterSnapshot load(ParticipantId id){return new QuestCenterSnapshot(id,Collections.emptyList(),Arrays.asList(other,tracked));}};TerminalTrackedQuestSnapshot result=new TrackedQuestSnapshotFactory(query).create(ParticipantId.player(player),9L);assertEquals(1,result.getQuests().size());assertEquals("Tracked",result.getQuests().get(0).getName());assertEquals(3,result.getQuests().get(0).getTasks().size());assertEquals(9L,result.getGeneratedAt());}
    private static QuestDefinition definition(UUID id,String name,int taskCount){java.util.List<TaskDefinition> tasks=new java.util.ArrayList<TaskDefinition>();for(int i=0;i<taskCount;i++)tasks.add(new TaskDefinition("task-"+i,"bq_standard:item",false,Collections.singletonMap("target",Integer.toString(i+2))));return new QuestDefinition(id,1,name,"",QuestLogic.AND,QuestLogic.AND,Collections.emptySet(),tasks,Collections.emptyList());}
}
