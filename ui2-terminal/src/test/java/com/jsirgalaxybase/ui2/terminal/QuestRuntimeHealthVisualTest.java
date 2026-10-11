package com.jsirgalaxybase.ui2.terminal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.*;
import com.jsirgalaxybase.ui2.core.*;
import com.jsirgalaxybase.ui2.debug.UiDebugSnapshot;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.theme.GlassTerminalTheme;

public final class QuestRuntimeHealthVisualTest {
    @Test public void healthAndFailureRemainAuditableAtAllWindowSizes() {
        TerminalVisualModel shell=new TerminalVisualModel("终端","career",Collections.singletonList(
            new TerminalVisualModel.NavItem("career","职业",true,true)));
        for(String status:Arrays.asList("目标 5620 · 奖励 5448 · 奖励失败 2 · 消费待恢复 3",
            "数据库健康查询失败，请稍后刷新；未改变任务或奖励。")) {
            QuestManagementVisualModel model=new QuestManagementVisualModel(shell,"","","",0,20,0,
                Collections.<QuestManagementVisualModel.Definition>emptyList()).withHealthRows(Arrays.asList(
                    "任务书 · Quest 3744 · 章节 47 · revision 3744",status));
            for(int[] size:new int[][]{{350,193},{427,240},{620,340}}) {
                UiRuntime runtime=new UiRuntime(new QuestManagementVisualDocument(model,TerminalActionPort.NONE,
                    QuestManagementActionPort.NONE,TerminalWindowProfile.STANDARD),new UiContext(
                        GlassTerminalTheme.create(),Locale.CHINA,1F,new FixedUiClock(0,true)));
                runtime.setViewport(size[0],size[1]);
                com.jsirgalaxybase.ui2.render.DrawList draw=runtime.frame();
                String snapshot=UiDebugSnapshot.capture(runtime.getRoot(),draw,size[0],size[1]);
                assertTrue(snapshot,snapshot.contains("quest-health-0"));
                assertTrue(snapshot,snapshot.contains("quest-health-1"));
                assertTrue(snapshot,snapshot.contains("SUMMARY|issues=0"));
                runtime.close();
            }
        }
    }
}
