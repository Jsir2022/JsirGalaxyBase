package com.jsirgalaxybase.terminal.client.ui2;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.QuestCenterEntry;
import com.jsirgalaxybase.quest.core.QuestCenterSnapshot;
import com.jsirgalaxybase.quest.core.QuestCenterPage;
import com.jsirgalaxybase.quest.core.QuestCenterPageRequest;
import com.jsirgalaxybase.quest.core.QuestChapterDefinition;
import com.jsirgalaxybase.quest.core.QuestChapterEntry;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestLogic;
import com.jsirgalaxybase.quest.core.QuestRewardSummary;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.RepeatPolicy;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.ui2.terminal.QuestCenterVisualModel;
import com.jsirgalaxybase.ui2.terminal.TerminalVisualModel;

public class QuestCenterVisualModelAdapterTest {
    @Test public void mapsChaptersFiltersClaimableAndBuildsDetailWithoutMinecraftTypes(){
        UUID player=UUID.randomUUID(),questId=UUID.randomUUID(),chapterId=UUID.randomUUID();
        TaskDefinition task=new TaskDefinition("collect","bq_standard:checkbox",false,Collections.singletonMap("target","1"));
        RewardDefinition reward=new RewardDefinition("reward","bq_standard:item",Collections.singletonMap("item.count","0"));
        QuestDefinition definition=new QuestDefinition(questId,1,"跨服公共任务","说明",QuestLogic.AND,QuestLogic.AND,
            Collections.<UUID>emptySet(),Collections.singletonList(task),Collections.singletonList(reward),RepeatPolicy.never());
        QuestCenterEntry entry=new QuestCenterEntry(definition,null,new QuestRewardSummary(1,1,0,0,0,0),QuestStatus.COMPLETED);
        QuestChapterDefinition chapter=new QuestChapterDefinition(chapterId,1,"公共建设","","","",
            Collections.singletonList(new QuestChapterEntry(questId,0,0,24,24)));
        QuestCenterSnapshot snapshot=new QuestCenterSnapshot(ParticipantId.player(player),Collections.singletonList(chapter),Collections.singletonList(entry));
        TerminalVisualModel shell=new TerminalVisualModel("银河终端","career",Collections.singletonList(new TerminalVisualModel.NavItem("career","职业",true,true)));

        QuestCenterVisualModel browse=QuestCenterVisualModelAdapter.browse(shell,snapshot,chapterId.toString(),"claimable","");
        assertEquals(1,browse.getQuests().size());
        assertEquals(QuestCenterVisualModel.QuestState.CLAIMABLE,browse.getQuests().get(0).getState());
        QuestCenterVisualModel detail=QuestCenterVisualModelAdapter.detail(shell,snapshot,questId.toString(),chapterId.toString(),"all");
        assertEquals(QuestCenterVisualModel.View.DETAIL,detail.getView());
        assertEquals(1,detail.getDetail().getTasks().size());
        assertEquals(1,detail.getDetail().getRewards().size());
    }

    @Test public void browsePagesLargeCatalogWithoutSilentlyDroppingTotals() {
        UUID player=UUID.randomUUID(),chapterId=UUID.randomUUID();
        List<QuestCenterEntry> entries=new ArrayList<QuestCenterEntry>();
        List<QuestChapterEntry> placements=new ArrayList<QuestChapterEntry>();
        for(int i=0;i<15;i++){
            UUID questId=UUID.randomUUID();
            QuestDefinition definition=definition(questId,"Quest "+i);
            entries.add(new QuestCenterEntry(definition,null,new QuestRewardSummary(0,0,0,0,0,0),QuestStatus.AVAILABLE));
            placements.add(new QuestChapterEntry(questId,i,0,1,1));
        }
        QuestChapterDefinition chapter=chapter(chapterId,placements);
        QuestCenterSnapshot snapshot=new QuestCenterSnapshot(ParticipantId.player(player),Collections.singletonList(chapter),entries);
        QuestCenterVisualModel page=QuestCenterVisualModelAdapter.browse(shell(),snapshot,chapterId.toString(),"all","Quest",0,6,1,7);
        assertEquals(15,page.getQuestTotalEntries());
        assertEquals(3,page.getQuestTotalPages());
        assertEquals(1,page.getQuestPageIndex());
        assertEquals(7,page.getQuests().size());
        assertEquals("Quest 7",page.getQuests().get(0).getName());
        QuestCenterPage serverPage=com.jsirgalaxybase.quest.core.QuestCenterPaginator.page(snapshot,
            new QuestCenterPageRequest(chapterId.toString(),"all","Quest",0,6,1,7));
        QuestCenterVisualModel bounded=QuestCenterVisualModelAdapter.browse(shell(),serverPage,"all","Quest");
        assertEquals(15,bounded.getQuestTotalEntries());
        assertEquals(7,bounded.getQuests().size());
    }

    private static QuestDefinition definition(UUID id,String name) {
        TaskDefinition task=new TaskDefinition("check","bq_standard:checkbox",false,
            Collections.singletonMap("target","1"));
        return new QuestDefinition(id,1,name,"Description",QuestLogic.AND,QuestLogic.AND,
            Collections.<UUID>emptySet(),Collections.singletonList(task),
            Collections.<RewardDefinition>emptyList(),RepeatPolicy.never());
    }

    private static QuestChapterDefinition chapter(UUID id,List<QuestChapterEntry> entries) {
        return new QuestChapterDefinition(id,1,"Chapter","","","",entries);
    }

    private static TerminalVisualModel shell() {
        return new TerminalVisualModel("银河终端","career",
            Collections.singletonList(new TerminalVisualModel.NavItem("career","职业",true,true)));
    }
}
