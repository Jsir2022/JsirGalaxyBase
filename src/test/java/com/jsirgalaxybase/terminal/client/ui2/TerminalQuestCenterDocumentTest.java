package com.jsirgalaxybase.terminal.client.ui2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Locale;

import org.junit.Test;

import com.jsirgalaxybase.terminal.TerminalActionType;
import com.jsirgalaxybase.terminal.TerminalQuestActionPayload;
import com.jsirgalaxybase.terminal.TerminalQuestCenterSectionSnapshot;
import com.jsirgalaxybase.terminal.TerminalQuestBatchRetirePayload;
import com.jsirgalaxybase.terminal.TerminalQuestChapterAlignmentPayload;
import com.jsirgalaxybase.terminal.TerminalQuestChapterClonePayload;
import com.jsirgalaxybase.terminal.TerminalQuestChapterEditPayload;
import com.jsirgalaxybase.terminal.TerminalQuestChapterOrderPayload;
import com.jsirgalaxybase.terminal.TerminalQuestDraftEditPayload;
import com.jsirgalaxybase.terminal.TerminalQuestParticipantActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiElement;
import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.theme.GlassTerminalTheme;

public class TerminalQuestCenterDocumentTest {
    @Test public void detailBackCarriesBrowseChapterQueryFilterAndBothPageIndexes(){
        TerminalQuestCenterSectionSnapshot.Quest quest=new TerminalQuestCenterSectionSnapshot.Quest(
            "quest:alpha","chapter:steel","Alpha Quest","","IN_PROGRESS",1,3,"",0,"");
        TerminalQuestCenterSectionSnapshot.Detail detail=new TerminalQuestCenterSectionSnapshot.Detail(
            quest,"长说明","前置任务","不可重复",Collections.<TerminalQuestCenterSectionSnapshot.Task>emptyList(),
            Collections.<TerminalQuestCenterSectionSnapshot.Reward>emptyList());
        TerminalQuestCenterSectionSnapshot section=new TerminalQuestCenterSectionSnapshot("READY","",
            TerminalQuestCenterSectionSnapshot.View.DETAIL,"chapter:steel","quest:alpha","active","steel blast",
            Collections.singletonList(new TerminalQuestCenterSectionSnapshot.Chapter("chapter:steel","钢铁",2,8)),
            3,6,42,Collections.singletonList(quest),5,7,39,detail);
        TerminalHomeScreenModel.PageSnapshotModel career=new TerminalHomeScreenModel.PageSnapshotModel("career",
            "职业","",Collections.<TerminalHomeScreenModel.SectionModel>emptyList(),null,null,null,null,null,null,
            null,section);
        TerminalHomeScreenModel model=new TerminalHomeScreenModel("career","银河终端","",
            new TerminalHomeScreenModel.StatusBandModel("任务","个人进度","","","ready"),
            Collections.singletonList(new TerminalHomeScreenModel.NavItemModel("career","职业","",true,true)),
            Collections.singletonList(career),Collections.<TerminalHomeScreenModel.NotificationModel>emptyList(),
            "test-session");
        assertEquals(TerminalQuestCenterSectionSnapshot.View.DETAIL,
            model.getPageSnapshot("career").getQuestCenterModel().getView());
        RecordingActions actions=new RecordingActions();
        TerminalQuestCenterDocument document=new TerminalQuestCenterDocument(model,new TerminalAppShell.Actions(){
            public void navigate(String id){} public void refresh(){} public void help(){} public void back(){} public void close(){}
        },actions);
        UiRuntime runtime=new UiRuntime(document,new UiContext(GlassTerminalTheme.create(),Locale.CHINA,1F,
            new FixedUiClock(0,true)));
        UiContext context=new UiContext(GlassTerminalTheme.create(),Locale.CHINA,1F,new FixedUiClock(0,true));
        assertTrue(find(document.build(context),"quest-detail-back")!=null);
        runtime.setViewport(620,340);runtime.frame();
        UiNode back=find(runtime.getRoot(),"quest-detail-back");assertTrue(back!=null);
        UiRect bounds=back.getBounds();assertTrue(bounds.getWidth()>0&&bounds.getHeight()>0);
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN,
            bounds.getX()+Math.max(1,bounds.getWidth()/2),bounds.getY()+Math.max(1,bounds.getHeight()/2),0)));
        assertEquals(TerminalActionType.QUEST_BACK,actions.action);
        assertEquals("chapter:steel",actions.payload.getChapterId());
        assertEquals("active",actions.payload.getFilter());assertEquals("steel blast",actions.payload.getQuery());
        assertEquals(3,actions.payload.getChapterPage());assertEquals(5,actions.payload.getQuestPage());
        runtime.close();
    }

    private static UiNode find(UiNode node,String key){
        if(node==null)return null;
        if(node.getElement().getKey()!=null&&key.equals(node.getElement().getKey().getValue()))return node;
        for(UiNode child:node.getChildren()){UiNode found=find(child,key);if(found!=null)return found;}
        return null;
    }

    private static UiElement find(UiElement element,String key){
        if(element==null)return null;
        if(element.getKey()!=null&&key.equals(element.getKey().getValue()))return element;
        for(UiElement child:element.getChildren()){UiElement found=find(child,key);if(found!=null)return found;}
        return null;
    }

    private static final class RecordingActions implements TerminalQuestCenterDocument.Actions{
        private TerminalActionType action;private TerminalQuestActionPayload payload;
        public void sendQuest(TerminalActionType value,TerminalQuestActionPayload intent){action=value;payload=intent;}
        public void sendQuestParticipant(TerminalActionType type,TerminalQuestParticipantActionPayload payload){}
        public void sendQuestEdit(TerminalQuestDraftEditPayload payload){}
        public void sendQuestBatchRetire(TerminalQuestBatchRetirePayload payload){}
        public void sendQuestChapterEdit(TerminalQuestChapterEditPayload payload){}
        public void sendQuestChapterClone(TerminalQuestChapterClonePayload payload){}
        public void sendQuestChapterAlignment(TerminalQuestChapterAlignmentPayload payload){}
        public void sendQuestChapterOrder(TerminalQuestChapterOrderPayload payload){}
    }
}
