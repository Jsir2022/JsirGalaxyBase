package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

public class QuestCenterPaginatorTest {
    @Test public void defaultRequestUsesCompactTerminalPageSizes(){
        QuestCenterPageRequest request=QuestCenterPageRequest.initial();
        assertEquals(4,request.getChapterPageSize());assertEquals(4,request.getQuestPageSize());
    }

    @Test public void boundsLargeCatalogAndFiltersBeforePaging(){
        UUID chapterId=UUID.randomUUID();List<QuestCenterEntry> quests=new ArrayList<QuestCenterEntry>();
        List<QuestChapterEntry> placements=new ArrayList<QuestChapterEntry>();
        for(int i=0;i<30;i++){UUID id=UUID.randomUUID();QuestDefinition definition=quest(id,"Machine "+i);
            QuestStatus status=i%2==0?QuestStatus.IN_PROGRESS:QuestStatus.COMPLETED;
            quests.add(new QuestCenterEntry(definition,null,QuestRewardSummary.EMPTY,status));
            placements.add(new QuestChapterEntry(id,i,0,1,1));}
        QuestChapterDefinition chapter=new QuestChapterDefinition(chapterId,1,"Machines","","","",placements);
        QuestCenterSnapshot snapshot=new QuestCenterSnapshot(ParticipantId.player(UUID.randomUUID()),Collections.singletonList(chapter),quests);
        QuestCenterPage page=QuestCenterPaginator.page(snapshot,new QuestCenterPageRequest(chapterId.toString(),"active","Machine",0,6,1,7));
        assertEquals(15,page.getQuestTotalEntries());assertEquals(7,page.getQuests().size());assertEquals(1,page.getQuestPageIndex());
        for(QuestCenterEntry entry:page.getQuests())assertEquals(QuestStatus.IN_PROGRESS,entry.getStatus());
    }

    @Test public void rejectsUnboundedSizesAndClampsPages(){
        QuestCenterPageRequest request=new QuestCenterPageRequest("","unknown","",99,999,99,999);
        assertEquals("all",request.getFilter());assertEquals(12,request.getChapterPageSize());assertEquals(24,request.getQuestPageSize());
        QuestCenterPage page=QuestCenterPaginator.page(new QuestCenterSnapshot(ParticipantId.player(UUID.randomUUID()),
            Collections.<QuestChapterDefinition>emptyList(),Collections.<QuestCenterEntry>emptyList()),request);
        assertEquals(0,page.getQuestPageIndex());assertTrue(page.getQuests().isEmpty());
    }

    @Test public void resolvesSelectedVisibleQuestAndOnlyVisiblePrerequisiteNames(){
        UUID player=UUID.randomUUID(),visibleParentId=UUID.randomUUID(),hiddenParentId=UUID.randomUUID();
        UUID selectedId=UUID.randomUUID();
        QuestCenterEntry visibleParent=new QuestCenterEntry(quest(visibleParentId,"Visible parent"),null,
            QuestRewardSummary.EMPTY,QuestStatus.COMPLETED);
        QuestDefinition selectedDefinition=new QuestDefinition(selectedId,1,"Selected quest","",QuestLogic.AND,
            QuestLogic.AND,new java.util.LinkedHashSet<UUID>(java.util.Arrays.asList(visibleParentId,hiddenParentId)),
            Collections.<TaskDefinition>emptyList(),Collections.<RewardDefinition>emptyList());
        QuestCenterEntry selected=new QuestCenterEntry(selectedDefinition,null,QuestRewardSummary.EMPTY,
            QuestStatus.AVAILABLE);
        QuestCenterSnapshot snapshot=new QuestCenterSnapshot(ParticipantId.player(player),
            Collections.<QuestChapterDefinition>emptyList(),java.util.Arrays.asList(visibleParent,selected));

        QuestCenterPage page=QuestCenterPaginator.page(snapshot,new QuestCenterPageRequest("",selectedId.toString(),
            "claimable","",0,6,99,7));

        assertTrue("detail selection must not depend on the current page/filter",page.getQuests().isEmpty());
        assertEquals(selectedId,page.getSelectedQuest().getDefinition().getId());
        assertEquals(1,page.getVisiblePrerequisites().size());
        assertEquals(visibleParentId,page.getVisiblePrerequisites().get(0).getDefinition().getId());
        assertNotEquals(hiddenParentId,page.getVisiblePrerequisites().get(0).getDefinition().getId());
    }

    private static QuestDefinition quest(UUID id,String name){TaskDefinition task=new TaskDefinition("check","bq_standard:checkbox",false,Collections.singletonMap("target","1"));return new QuestDefinition(id,1,name,"Description",QuestLogic.AND,QuestLogic.AND,Collections.<UUID>emptySet(),Collections.singletonList(task),Collections.<RewardDefinition>emptyList(),RepeatPolicy.never());}
}
