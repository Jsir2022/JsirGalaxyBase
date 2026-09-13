package com.jsirgalaxybase.ui2.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.debug.UiDebugSnapshot;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.render.DrawList;

public class TerminalVisualScenarioTest {
    @Test public void v1ProductionDocumentsAreCleanAtAllSupportedSizes() {
        int[][] sizes = {{350, 193}, {427, 240}, {620, 340}};
        assertEquals(6, TerminalVisualScenario.v1Defaults().size());
        for (TerminalVisualScenario scenario : TerminalVisualScenario.v1Defaults()) {
            for (int[] size : sizes) {
                UiRuntime runtime = new UiRuntime(scenario.createDocument(), new UiContext(
                    scenario.getTheme(), Locale.CHINA, 1F, new FixedUiClock(0, true)));
                runtime.setViewport(size[0], size[1]);
                DrawList drawList = runtime.frame();
                String snapshot = UiDebugSnapshot.capture(runtime.getRoot(), drawList, size[0], size[1]);
                assertTrue(scenario.getId() + " " + size[0] + "x" + size[1] + "\n" + snapshot,
                    snapshot.contains("SUMMARY|issues=0"));
                runtime.close();
            }
        }
    }

    @Test public void questCenterStatesAreCleanAtAllSupportedSizesAndThemes() {
        int[][] sizes = {{350, 193}, {427, 240}, {620, 340}};
        int scenarios=0;
        for (TerminalVisualScenario scenario : TerminalVisualScenario.defaults()) {
            if (!scenario.getId().startsWith("terminal-quests-")) continue;
            scenarios++;
            for (int[] size : sizes) {
                UiRuntime runtime = new UiRuntime(scenario.createDocument(), new UiContext(
                    scenario.getTheme(), Locale.CHINA, 1F, new FixedUiClock(0, true)));
                runtime.setViewport(size[0], size[1]);
                DrawList drawList = runtime.frame();
                String snapshot = UiDebugSnapshot.capture(runtime.getRoot(), drawList, size[0], size[1]);
                assertTrue(scenario.getId() + " " + size[0] + "x" + size[1] + "\n" + snapshot,
                    snapshot.contains("SUMMARY|issues=0"));
                assertTrue(snapshot.contains("quest-"));
                runtime.close();
            }
        }
        assertEquals(12, scenarios);
    }

    @Test public void questManagementStatesAreCleanAtAllSupportedSizesAndThemes(){int[][] sizes={{350,193},{427,240},{620,340}};int scenarios=0;for(TerminalVisualScenario scenario:TerminalVisualScenario.defaults()){if(!scenario.getId().startsWith("terminal-quest-management-"))continue;scenarios++;for(int[] size:sizes){UiRuntime runtime=new UiRuntime(scenario.createDocument(),new UiContext(scenario.getTheme(),Locale.CHINA,1F,new FixedUiClock(0,true)));runtime.setViewport(size[0],size[1]);DrawList drawList=runtime.frame();String snapshot=UiDebugSnapshot.capture(runtime.getRoot(),drawList,size[0],size[1]);assertTrue(scenario.getId()+" "+size[0]+"x"+size[1]+"\n"+snapshot,snapshot.contains("SUMMARY|issues=0"));assertTrue(snapshot.contains("quest-admin")||snapshot.contains("quest-prerequisite")||snapshot.contains("quest-behavior"));if(scenario.getId().startsWith("terminal-quest-management-detail-"))assertTrue(snapshot,snapshot.contains("quest-admin-detail-impact"));runtime.close();}}assertEquals(30,scenarios);}

    @Test public void questChapterManagementStatesAreCleanAtAllSupportedSizesAndThemes(){int[][] sizes={{350,193},{427,240},{620,340}};int scenarios=0;for(TerminalVisualScenario scenario:TerminalVisualScenario.defaults()){if(!scenario.getId().startsWith("terminal-quest-chapters-"))continue;scenarios++;for(int[] size:sizes){UiRuntime runtime=new UiRuntime(scenario.createDocument(),new UiContext(scenario.getTheme(),Locale.CHINA,1F,new FixedUiClock(0,true)));runtime.setViewport(size[0],size[1]);DrawList drawList=runtime.frame();String snapshot=UiDebugSnapshot.capture(runtime.getRoot(),drawList,size[0],size[1]);assertTrue(scenario.getId()+" "+size[0]+"x"+size[1]+"\n"+snapshot,snapshot.contains("SUMMARY|issues=0"));assertTrue(snapshot.contains("chapter-admin")||snapshot.contains("chapter-layout-canvas")||snapshot.contains("chapter-create")||snapshot.contains("chapter-editor"));runtime.close();}}assertEquals(18,scenarios);}
}
