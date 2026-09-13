package com.jsirgalaxybase.ui2.terminal;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

public class QuestCenterVisualModelTest {
    @Test public void snapshotsAreImmutableAndClampImpossibleProgress() {
        java.util.List<QuestCenterVisualModel.Chapter> chapters=new ArrayList<QuestCenterVisualModel.Chapter>();
        chapters.add(new QuestCenterVisualModel.Chapter("chapter","Chapter",5,2));
        QuestCenterVisualModel model=new QuestCenterVisualModel(shell(),QuestCenterVisualModel.View.BROWSE,
            QuestCenterVisualModel.LoadState.READY,"chapter","","all","",chapters,
            Collections.<QuestCenterVisualModel.QuestSummary>emptyList(),null);
        chapters.clear();
        assertEquals(1,model.getChapters().size());
        assertEquals(5,model.getChapters().get(0).getTotal());
        try { model.getChapters().clear(); fail("expected immutable chapters"); }
        catch (UnsupportedOperationException expected) { assertTrue(true); }
    }

    @Test public void paginationClampsIndicesAndReportsNavigation() {
        java.util.List<QuestCenterVisualModel.Chapter> chapters=new ArrayList<QuestCenterVisualModel.Chapter>();
        chapters.add(new QuestCenterVisualModel.Chapter("chapter","Chapter",0,1));
        QuestCenterVisualModel model=new QuestCenterVisualModel(shell(),QuestCenterVisualModel.View.BROWSE,
            QuestCenterVisualModel.LoadState.READY,"chapter","","all","needle","",chapters,99,6,46,
            Collections.<QuestCenterVisualModel.QuestSummary>emptyList(),2,7,30,null);
        assertEquals("needle",model.getQuery());
        assertEquals(7,model.getChapterPageIndex());
        assertEquals(8,model.getChapterTotalPages());
        assertTrue(model.hasPreviousChapterPage());
        assertFalse(model.hasNextChapterPage());
        assertEquals(2,model.getQuestPageIndex());
        assertEquals(5,model.getQuestTotalPages());
        assertTrue(model.hasPreviousQuestPage());
        assertTrue(model.hasNextQuestPage());
    }

    private static TerminalVisualModel shell(){return new TerminalVisualModel("Terminal","career",
        Collections.singletonList(new TerminalVisualModel.NavItem("career","Career",true,true)));}
}
