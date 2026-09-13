package com.jsirgalaxybase.terminal.client.ui2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.theme.GlassTerminalTheme;

public class TerminalHomeDocumentTest {
    @Test public void homeUsesCenteredSharedWindowAtProductionSizes() {
        for (int[] size : new int[][] {{350,193},{427,240},{620,340},{900,500}}) {
            TerminalHomeDocument document = new TerminalHomeDocument(TerminalHomeScreenModel.placeholder(), actions());
            UiRuntime runtime = new UiRuntime(document, context());
            runtime.setViewport(size[0], size[1]); runtime.frame();
            UiRect actual = find(runtime.getRoot(), "terminal-window").getBounds();
            assertEquals(TerminalWindowMetrics.compute(new com.jsirgalaxybase.ui2.geometry.UiSize(size[0],size[1])).getBounds(), actual);
            assertTrue(actual.getWidth() <= size[0]); assertTrue(actual.getHeight() <= size[1]);
        }
    }

    @Test public void helpIsModalAndEscapeClosesIt() {
        TerminalHomeDocument document = new TerminalHomeDocument(TerminalHomeScreenModel.placeholder(), actions());
        UiRuntime runtime = new UiRuntime(document, context()); runtime.setViewport(427,240); runtime.frame();
        document.openHelp(); runtime.invalidate(); runtime.frame(); assertTrue(runtime.hasModal());
        assertTrue(runtime.dispatch(com.jsirgalaxybase.ui2.input.UiEvent.key(com.jsirgalaxybase.ui2.input.UiKeyCode.ESCAPE)));
        runtime.frame(); assertTrue(!runtime.hasModal());
    }

    @Test public void compactHomeTextIsClippedAndCardsDoNotOverlap() {
        TerminalHomeDocument document = new TerminalHomeDocument(TerminalHomeScreenModel.placeholder(), actions());
        UiRuntime runtime = new UiRuntime(document, context()); runtime.setViewport(350,193);
        com.jsirgalaxybase.ui2.render.DrawList drawList=runtime.frame();
        UiRect window=find(runtime.getRoot(),"terminal-window").getBounds();
        UiRect hero=find(runtime.getRoot(),"home-hero").getBounds();
        UiRect first=find(runtime.getRoot(),"home-section-0").getBounds();
        assertTrue(hero.getBottom()<=first.getY());
        for(DrawCommand command:drawList.ordered()){
            if(command.getKind()!=DrawCommand.Kind.TEXT)continue;
            UiRect bounds=command.getBounds();
            assertTrue("text left window: "+command.snapshot(),bounds.getX()>=window.getX());
            assertTrue("text right window: "+command.snapshot(),bounds.getRight()<=window.getRight());
            assertTrue("text above window: "+command.snapshot(),bounds.getY()>=window.getY());
            assertTrue("text below window: "+command.snapshot(),bounds.getBottom()<=window.getBottom());
        }
    }

    private static UiContext context() { return new UiContext(GlassTerminalTheme.create(),Locale.CHINA,1F,new FixedUiClock(0,false)); }
    private static TerminalAppShell.Actions actions() { return new TerminalAppShell.Actions() {
        public void navigate(String pageId) {} public void refresh() {} public void help() {} public void back() {} public void close() {}
    }; }
    private static com.jsirgalaxybase.ui2.core.UiNode find(com.jsirgalaxybase.ui2.core.UiNode node, String key) {
        if (node.getElement().getKey() != null && key.equals(node.getElement().getKey().getValue())) return node;
        for (com.jsirgalaxybase.ui2.core.UiNode child : node.getChildren()) {
            com.jsirgalaxybase.ui2.core.UiNode match = findOrNull(child, key);
            if (match != null) return match;
        }
        throw new AssertionError("missing node " + key);
    }
    private static com.jsirgalaxybase.ui2.core.UiNode findOrNull(com.jsirgalaxybase.ui2.core.UiNode node, String key) {
        if (node.getElement().getKey() != null && key.equals(node.getElement().getKey().getValue())) return node;
        for (com.jsirgalaxybase.ui2.core.UiNode child : node.getChildren()) {
            com.jsirgalaxybase.ui2.core.UiNode match = findOrNull(child, key);
            if (match != null) return match;
        }
        return null;
    }
}
