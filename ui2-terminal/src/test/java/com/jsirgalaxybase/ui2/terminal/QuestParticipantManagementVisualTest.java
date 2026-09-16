package com.jsirgalaxybase.ui2.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.junit.Test;

import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.debug.UiDebugSnapshot;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.input.UiKeyCode;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.theme.DarkIndustrialTheme;
import com.jsirgalaxybase.ui2.theme.GlassTerminalTheme;
import com.jsirgalaxybase.ui2.theme.HackerGreenTheme;
import com.jsirgalaxybase.ui2.theme.HighContrastTheme;
import com.jsirgalaxybase.ui2.theme.UiTheme;

public class QuestParticipantManagementVisualTest {
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

    private static final class RecordingActions implements QuestCenterActionPort{
        private int leaveCalls;
        public void selectChapter(String value){}public void selectQuest(String value){}public void changeFilter(String value){}
        public void changeQuery(String value){}public void changeChapterPage(int value){}public void changeQuestPage(int value){}
        public void backToBrowse(){}public void claim(String value){}public void selectRewardChoice(String quest,String reward,int choice){}
        public void toggleTracking(String value){}public void openManagement(){}public void retry(){}
        public void createParticipant(String type,String name){}public void addParticipantMember(String type,String id,long revision,String player,String role){}
        public void removeParticipantMember(String type,String id,long revision,String player){}
        public void leaveParticipant(String type,String id,long revision){leaveCalls++;}
        public void transferParticipantOwner(String type,String id,long revision,String player){}
    }
}
