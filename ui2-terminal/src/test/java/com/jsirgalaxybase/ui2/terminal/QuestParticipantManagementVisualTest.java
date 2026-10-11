package com.jsirgalaxybase.ui2.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.junit.Test;

import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.debug.UiDebugSnapshot;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.input.UiKeyCode;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.theme.DarkIndustrialTheme;
import com.jsirgalaxybase.ui2.theme.GlassTerminalTheme;
import com.jsirgalaxybase.ui2.theme.HackerGreenTheme;
import com.jsirgalaxybase.ui2.theme.HighContrastTheme;
import com.jsirgalaxybase.ui2.theme.UiTheme;

public class QuestParticipantManagementVisualTest {
    @Test public void craftingStatisticsTaskDoesNotExposeManualDetectAction() {
        QuestCenterVisualModel model=craftingDetectionModel();
        assertTrue("fixture represents a legacy-detectable crafting-stat task",
            model.getDetail().getTasks().get(0).isDetectable());
        int[][] sizes={{350,193},{427,240},{620,340}};
        for(int[] size:sizes){
            RecordingActions actions=new RecordingActions();
            QuestCenterVisualDocument document=new QuestCenterVisualDocument(model,TerminalActionPort.NONE,actions,
                TerminalWindowProfile.STANDARD);
            UiRuntime runtime=new UiRuntime(document,new UiContext(GlassTerminalTheme.create(),Locale.CHINA,1F,
                new FixedUiClock(0,true)));
            runtime.setViewport(size[0],size[1]);DrawList drawList=runtime.frame();
            assertTrue("crafting statistics update from matching gameplay events, not a manual task button",
                find(runtime.getRoot(),"quest-task-detect-0")==null);
            assertTrue(find(runtime.getRoot(),"quest-task-confirm-0")==null);
            assertTrue(find(runtime.getRoot(),"quest-task-detect-items-0")==null);
            assertTrue(UiDebugSnapshot.capture(runtime.getRoot(),drawList,size[0],size[1])
                .contains("SUMMARY|issues=0"));
            runtime.close();
        }
    }

    @Test public void availableCheckboxTaskRendersAndDispatchesOnlyQuestAndTaskIntent() {
        int[][] sizes={{350,193},{427,240},{620,340}};
        for(int[] size:sizes){
            RecordingActions actions=new RecordingActions();
            QuestCenterVisualDocument document=new QuestCenterVisualDocument(checkboxModel(true),
                TerminalActionPort.NONE,actions,TerminalWindowProfile.STANDARD);
            UiRuntime runtime=new UiRuntime(document,new UiContext(GlassTerminalTheme.create(),Locale.CHINA,1F,
                new FixedUiClock(0,true)));
            runtime.setViewport(size[0],size[1]);runtime.frame();
            UiNode confirm=find(runtime.getRoot(),"quest-task-confirm-0");
            assertTrue("available checkbox task should expose a confirmation action",confirm!=null);
            UiRect bounds=confirm.getBounds();
            assertTrue("checkbox hit target must fit "+size[0]+"x"+size[1]+": "+bounds,
                new UiRect(0,0,size[0],size[1]).contains(bounds)&&bounds.getWidth()>0&&bounds.getHeight()>0);
            assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN,
                bounds.getX()+Math.max(1,bounds.getWidth()/2),bounds.getY()+Math.max(1,bounds.getHeight()/2),0)));
            assertEquals(1,actions.confirmCalls);
            assertEquals("quest-checkbox",actions.confirmedQuest);
            assertEquals("task-0",actions.confirmedTask);
            runtime.close();
        }

        QuestCenterVisualDocument hidden=new QuestCenterVisualDocument(checkboxModel(false),TerminalActionPort.NONE,
            new RecordingActions(),TerminalWindowProfile.STANDARD);
        UiRuntime runtime=new UiRuntime(hidden,new UiContext(GlassTerminalTheme.create(),Locale.CHINA,1F,
            new FixedUiClock(0,true)));
        runtime.setViewport(620,340);runtime.frame();
        assertTrue("locked/non-confirmable tasks must not expose a client confirmation action",
            find(runtime.getRoot(),"quest-task-confirm-0")==null);
        runtime.close();
    }

    @Test public void consumingTasksShareOneQuestLevelDetectAction() {
        int[][] sizes={{350,193},{427,240},{620,340}};
        for(int[] size:sizes){
            RecordingActions actions=new RecordingActions();
            QuestCenterVisualDocument document=new QuestCenterVisualDocument(consumingModel(),TerminalActionPort.NONE,
                actions,TerminalWindowProfile.STANDARD);
            UiRuntime runtime=new UiRuntime(document,new UiContext(GlassTerminalTheme.create(),Locale.CHINA,1F,
                new FixedUiClock(0,true)));
            runtime.setViewport(size[0],size[1]);
            DrawList drawList=runtime.frame();
            UiNode detect=find(runtime.getRoot(),"quest-detect-consumptions");
            assertTrue("Quest-level Detect should fit "+size[0]+"x"+size[1],detect!=null);
            assertTrue("task rows must not expose client-driven per-Task submission",
                find(runtime.getRoot(),"quest-task-item-actions-0")==null);
            UiRect bounds=detect.getBounds();
            assertTrue(new UiRect(0,0,size[0],size[1]).contains(bounds));
            assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN,
                bounds.getX()+Math.max(1,bounds.getWidth()/2),bounds.getY()+Math.max(1,bounds.getHeight()/2),0)));
            assertEquals(1,actions.detectConsumptionCalls);
            assertEquals("quest-consume",actions.detectedQuest);
            assertTrue(UiDebugSnapshot.capture(runtime.getRoot(),drawList,size[0],size[1])
                .contains("SUMMARY|issues=0"));
            runtime.close();
        }
    }

    @Test public void unresolvedChoiceOffersExplicitRandomClaimAndRoutesOnlyThatIntent() {
        int[][] sizes={{350,193},{427,240},{620,340}};
        for(int[] size:sizes){
            RecordingActions actions=new RecordingActions();
            QuestCenterVisualDocument document=new QuestCenterVisualDocument(claimableChoiceModel(false),
                TerminalActionPort.NONE,actions,TerminalWindowProfile.STANDARD);
            UiRuntime runtime=new UiRuntime(document,new UiContext(GlassTerminalTheme.create(),Locale.CHINA,1F,
                new FixedUiClock(0,true)));
            runtime.setViewport(size[0],size[1]);runtime.frame();
            UiNode button=find(runtime.getRoot(),"quest-claim-random-choice");
            assertTrue("unresolved choice should offer a BQ-compatible action",button!=null);
            UiRect bounds=button.getBounds();
            assertTrue("random-claim hit target must fit "+size[0]+"x"+size[1]+": "+bounds,
                new UiRect(0,0,size[0],size[1]).contains(bounds)&&bounds.getWidth()>0&&bounds.getHeight()>0);
            UiNode normalClaim=find(runtime.getRoot(),"quest-claim");
            UiRect normalBounds=normalClaim.getBounds();
            assertTrue("normal-claim hit target must fit "+size[0]+"x"+size[1]+": "+normalBounds,
                new UiRect(0,0,size[0],size[1]).contains(normalBounds)
                    &&normalBounds.getWidth()>0&&normalBounds.getHeight()>0);
            assertFalse("ordinary claim must wait for an explicit choice",
                runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN,
                    normalBounds.getX()+Math.max(1,normalBounds.getWidth()/2),
                    normalBounds.getY()+Math.max(1,normalBounds.getHeight()/2),0)));
            assertEquals(0,actions.claimCalls);
            int x=bounds.getX()+Math.max(1,bounds.getWidth()/2);
            int y=bounds.getY()+Math.max(1,bounds.getHeight()/2);
            assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN,x,y,0)));
            assertEquals(1,actions.randomClaimCalls);assertEquals(0,actions.claimCalls);
            runtime.close();
        }

        RecordingActions resolvedActions=new RecordingActions();
        QuestCenterVisualDocument resolved=new QuestCenterVisualDocument(claimableChoiceModel(true),
            TerminalActionPort.NONE,resolvedActions,TerminalWindowProfile.STANDARD);
        UiRuntime resolvedRuntime=new UiRuntime(resolved,new UiContext(GlassTerminalTheme.create(),Locale.CHINA,1F,
            new FixedUiClock(0,true)));
        resolvedRuntime.setViewport(620,340);resolvedRuntime.frame();
        assertTrue("a resolved choice should not offer random replacement",find(resolvedRuntime.getRoot(),
            "quest-claim-random-choice")==null);
        UiNode reselect=find(resolvedRuntime.getRoot(),"quest-choice-0-1");
        assertTrue("a choice reward remains selectable until it is claimed",reselect!=null);
        UiRect reselectBounds=reselect.getBounds();
        assertTrue(resolvedRuntime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN,
            reselectBounds.getX()+Math.max(1,reselectBounds.getWidth()/2),
            reselectBounds.getY()+Math.max(1,reselectBounds.getHeight()/2),0)));
        assertEquals(1,resolvedActions.choiceCalls);assertEquals(1,resolvedActions.selectedChoice);
        UiNode normalClaim=find(resolvedRuntime.getRoot(),"quest-claim");
        UiRect normalBounds=normalClaim.getBounds();
        assertTrue(resolvedRuntime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN,
            normalBounds.getX()+Math.max(1,normalBounds.getWidth()/2),
            normalBounds.getY()+Math.max(1,normalBounds.getHeight()/2),0)));
        assertEquals(1,resolvedActions.claimCalls);
        resolvedRuntime.close();

        RecordingActions lockedActions=new RecordingActions();
        QuestCenterVisualDocument locked=new QuestCenterVisualDocument(claimableChoiceModel(true,false),
            TerminalActionPort.NONE,lockedActions,TerminalWindowProfile.STANDARD);
        UiRuntime lockedRuntime=new UiRuntime(locked,new UiContext(GlassTerminalTheme.create(),Locale.CHINA,1F,
            new FixedUiClock(0,true)));
        lockedRuntime.setViewport(620,340);lockedRuntime.frame();
        UiNode lockedChoice=find(lockedRuntime.getRoot(),"quest-choice-0-1");UiRect lockedBounds=lockedChoice.getBounds();
        assertFalse("a claimed choice reward must not accept a new selection",lockedRuntime.dispatch(
            UiEvent.pointer(UiEvent.Type.POINTER_DOWN,lockedBounds.getX()+Math.max(1,lockedBounds.getWidth()/2),
                lockedBounds.getY()+Math.max(1,lockedBounds.getHeight()/2),0)));
        assertEquals(0,lockedActions.choiceCalls);lockedRuntime.close();
    }

    @Test public void participantConfirmationBlocksInputAndEscapeCancelsBeforeEnterExecutes() {
        RecordingActions actions=new RecordingActions();
        QuestCenterVisualDocument document=new QuestCenterVisualDocument(model(),TerminalActionPort.NONE,actions,
            TerminalWindowProfile.STANDARD);
        document.openParticipantManagementForPreview();
        assertTrue(document.requestLeaveForPreview());
        UiRuntime runtime=new UiRuntime(document,new UiContext(GlassTerminalTheme.create(),Locale.CHINA,1F,
            new FixedUiClock(0,true)));
        runtime.setViewport(427,240);runtime.frame();assertTrue(runtime.hasModal());
        assertTrue(runtime.dispatch(UiEvent.key(UiKeyCode.ESCAPE)));runtime.frame();assertFalse(runtime.hasModal());
        assertEquals(0,actions.leaveCalls);
        assertTrue(document.requestLeaveForPreview());runtime.invalidate();runtime.frame();
        assertTrue(runtime.dispatch(UiEvent.key(UiKeyCode.ENTER)));assertEquals(1,actions.leaveCalls);
        runtime.close();
    }

    @Test public void participantManagementIsStructurallyCleanAtEverySupportedViewportAndTheme() {
        int[][] sizes={{350,193},{427,240},{620,340}};
        UiTheme[] themes={GlassTerminalTheme.create(),HackerGreenTheme.create(),DarkIndustrialTheme.create(),
            HighContrastTheme.create()};
        for(UiTheme theme:themes)for(int[] size:sizes){
            QuestCenterVisualDocument document=new QuestCenterVisualDocument(model(),TerminalActionPort.NONE,
                new RecordingActions(),TerminalWindowProfile.STANDARD);
            document.openParticipantManagementForPreview();
            UiRuntime runtime=new UiRuntime(document,new UiContext(theme,Locale.CHINA,1F,new FixedUiClock(0,true)));
            runtime.setViewport(size[0],size[1]);DrawList drawList=runtime.frame();
            String snapshot=UiDebugSnapshot.capture(runtime.getRoot(),drawList,size[0],size[1]);
            assertTrue(theme+" "+size[0]+"x"+size[1]+"\n"+snapshot,
                snapshot.contains("SUMMARY|issues=0"));
            assertTrue(snapshot.contains("quest-participant"));runtime.close();
        }
    }

    private static QuestCenterVisualModel model(){
        TerminalVisualModel shell=new TerminalVisualModel("终端","career",Collections.singletonList(
            new TerminalVisualModel.NavItem("career","职业",true,true)));
        QuestCenterVisualModel base=new QuestCenterVisualModel(shell,QuestCenterVisualModel.View.BROWSE,
            QuestCenterVisualModel.LoadState.EMPTY,"","","all","",Collections.<QuestCenterVisualModel.Chapter>emptyList(),
            Collections.<QuestCenterVisualModel.QuestSummary>emptyList(),null);
        QuestCenterVisualModel.Member owner=new QuestCenterVisualModel.Member(
            "550e8400-e29b-41d4-a716-446655440000","OWNER",1L,1L);
        return base.withParticipants(Collections.singletonList(new QuestCenterVisualModel.Participant("TEAM",
            "550e8400-e29b-41d4-a716-446655440001","银河组","OWNER",3L,1,Arrays.asList(owner))));
    }

    private static QuestCenterVisualModel claimableChoiceModel(boolean selected){return claimableChoiceModel(selected,true);}
    private static QuestCenterVisualModel claimableChoiceModel(boolean selected,boolean claimable){
        TerminalVisualModel shell=new TerminalVisualModel("终端","career",Collections.singletonList(
            new TerminalVisualModel.NavItem("career","职业",true,true)));
        QuestCenterVisualModel.QuestSummary summary=new QuestCenterVisualModel.QuestSummary("quest-a","chapter-a",
            "奖励选择任务","",QuestCenterVisualModel.QuestState.CLAIMABLE,1,1,null);
        VisualItem optionItem=new VisualItem("minecraft:iron_ingot","minecraft:iron_ingot",0,"铁锭",4);
        QuestCenterVisualModel.ChoiceOption option=new QuestCenterVisualModel.ChoiceOption(optionItem,"铁锭");
        VisualItem secondOptionItem=new VisualItem("minecraft:gold_ingot","minecraft:gold_ingot",0,"金锭",2);
        QuestCenterVisualModel.ChoiceOption secondOption=new QuestCenterVisualModel.ChoiceOption(secondOptionItem,"金锭");
        QuestCenterVisualModel.RewardRow reward=new QuestCenterVisualModel.RewardRow("choice-a","任选奖励","",null,
            claimable,false,Arrays.asList(option,secondOption),selected?0:-1);
        QuestCenterVisualModel.QuestDetail detail=new QuestCenterVisualModel.QuestDetail(summary,"说明","","不重复",
            Collections.<QuestCenterVisualModel.TaskRow>emptyList(),Collections.singletonList(reward));
        return new QuestCenterVisualModel(shell,QuestCenterVisualModel.View.DETAIL,QuestCenterVisualModel.LoadState.READY,
            "chapter-a","quest-a","all","","",Collections.<QuestCenterVisualModel.Chapter>emptyList(),0,1,0,
            Collections.singletonList(summary),0,1,1,detail);
    }

    private static QuestCenterVisualModel checkboxModel(boolean confirmable){
        TerminalVisualModel shell=new TerminalVisualModel("终端","career",Collections.singletonList(
            new TerminalVisualModel.NavItem("career","职业",true,true)));
        QuestCenterVisualModel.QuestSummary summary=new QuestCenterVisualModel.QuestSummary("quest-checkbox",
            "chapter-a","手动确认任务","",QuestCenterVisualModel.QuestState.AVAILABLE,0,1,null);
        QuestCenterVisualModel.TaskRow task=new QuestCenterVisualModel.TaskRow("task-0","确认目标","玩家确认",
            0L,1L,confirmable);
        QuestCenterVisualModel.QuestDetail detail=new QuestCenterVisualModel.QuestDetail(summary,"说明","","不重复",
            Collections.singletonList(task),Collections.<QuestCenterVisualModel.RewardRow>emptyList());
        return new QuestCenterVisualModel(shell,QuestCenterVisualModel.View.DETAIL,QuestCenterVisualModel.LoadState.READY,
            "chapter-a","quest-checkbox","all","","",Collections.<QuestCenterVisualModel.Chapter>emptyList(),
            0,1,0,Collections.singletonList(summary),0,1,1,detail);
    }

    private static QuestCenterVisualModel craftingDetectionModel(){
        TerminalVisualModel shell=new TerminalVisualModel("终端","career",Collections.singletonList(
            new TerminalVisualModel.NavItem("career","职业",true,true)));
        QuestCenterVisualModel.QuestSummary summary=new QuestCenterVisualModel.QuestSummary("quest-crafting",
            "chapter-a","统计合成任务","",QuestCenterVisualModel.QuestState.IN_PROGRESS,0,1,null);
        QuestCenterVisualModel.TaskRow task=new QuestCenterVisualModel.TaskRow("crafting-0","制作目标",
            "由匹配的合成产物自动更新",0L,1L,false,false,true);
        QuestCenterVisualModel.QuestDetail detail=new QuestCenterVisualModel.QuestDetail(summary,"说明","",
            "不重复",Collections.singletonList(task),Collections.<QuestCenterVisualModel.RewardRow>emptyList());
        return new QuestCenterVisualModel(shell,QuestCenterVisualModel.View.DETAIL,QuestCenterVisualModel.LoadState.READY,
            "chapter-a","quest-crafting","all","","",Collections.<QuestCenterVisualModel.Chapter>emptyList(),
            0,1,0,Collections.singletonList(summary),0,1,1,detail);
    }

    private static QuestCenterVisualModel consumingModel(){
        TerminalVisualModel shell=new TerminalVisualModel("终端","career",Collections.singletonList(
            new TerminalVisualModel.NavItem("career","职业",true,true)));
        QuestCenterVisualModel.QuestSummary summary=new QuestCenterVisualModel.QuestSummary("quest-consume",
            "chapter-a","消耗物品任务","",QuestCenterVisualModel.QuestState.IN_PROGRESS,0,2,null);
        QuestCenterVisualModel.TaskRow first=new QuestCenterVisualModel.TaskRow("item-a","物品目标 A","提交目标物品",
            0L,4L,false,true,false);
        QuestCenterVisualModel.TaskRow second=new QuestCenterVisualModel.TaskRow("item-b","物品目标 B","提交另一种物品",
            1L,3L,false,true,false);
        QuestCenterVisualModel.QuestDetail detail=new QuestCenterVisualModel.QuestDetail(summary,"说明","",
            "不重复",Arrays.asList(first,second),Collections.<QuestCenterVisualModel.RewardRow>emptyList());
        return new QuestCenterVisualModel(shell,QuestCenterVisualModel.View.DETAIL,QuestCenterVisualModel.LoadState.READY,
            "chapter-a","quest-consume","all","","",Collections.<QuestCenterVisualModel.Chapter>emptyList(),
            0,1,0,Collections.singletonList(summary),0,1,1,detail);
    }

    private static UiNode find(UiNode node,String key){
        if(node==null)return null;
        if(node.getElement().getKey()!=null&&key.equals(node.getElement().getKey().getValue()))return node;
        for(UiNode child:node.getChildren()){UiNode match=find(child,key);if(match!=null)return match;}
        return null;
    }

    private static final class RecordingActions implements QuestCenterActionPort{
        private int leaveCalls,claimCalls,randomClaimCalls,choiceCalls,selectedChoice=-1,confirmCalls,
            detectConsumptionCalls;
        private String confirmedQuest="",confirmedTask="",detectedQuest="";
        public void selectChapter(String value){}public void selectQuest(String value){}public void changeFilter(String value){}
        public void changeQuery(String value){}public void changeChapterPage(int value){}public void changeQuestPage(int value){}
        public void backToBrowse(){}public void claim(String value){claimCalls++;}public void forceClaimWithRandomChoices(String value){randomClaimCalls++;}public void confirmTask(String quest,String task){confirmCalls++;confirmedQuest=quest;confirmedTask=task;}public void submitTask(String quest,String task){}public void submitHeldStack(String quest,String task){}public void selectRewardChoice(String quest,String reward,int choice){choiceCalls++;selectedChoice=choice;}
        public void detectConsumptions(String quest){detectConsumptionCalls++;detectedQuest=quest;}
        public void toggleTracking(String value){}public void openManagement(){}public void retry(){}
        public void createParticipant(String type,String name){}public void addParticipantMember(String type,String id,long revision,String player,String role){}
        public void removeParticipantMember(String type,String id,long revision,String player){}
        public void leaveParticipant(String type,String id,long revision){leaveCalls++;}
        public void transferParticipantOwner(String type,String id,long revision,String player){}
    }
}
