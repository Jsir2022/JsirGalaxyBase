package com.jsirgalaxybase.client.gui.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.client.gui.theme.GuiTheme;
import com.jsirgalaxybase.client.gui.theme.TerminalThemeRegistry;

public class CanvasSceneRuntimeTest {

    @Test public void modalInputTakesPriorityOverRootAndEscapeCanCloseIt() {
        CanvasSceneRuntime runtime = new CanvasSceneRuntime(TerminalThemeRegistry.getDefaultTheme());
        CountingPanel rootChild = new CountingPanel();
        PanelContainer root = new PanelContainer(); root.addChild(rootChild);
        TestScene scene = new TestScene(runtime);
        runtime.initialize(scene, root);
        CountingPanel modal = new CountingPanel();
        runtime.openPopup(scene, modal);

        assertTrue(runtime.hasOpenPopup());
        runtime.mouseClicked(scene, 1, 1, 0);
        assertEquals(1, modal.clicks);
        assertEquals(0, rootChild.clicks);
        runtime.closePopup();
        assertFalse(runtime.hasOpenPopup());
        runtime.mouseClicked(scene, 1, 1, 0);
        assertEquals(1, rootChild.clicks);
    }

    @Test public void reinitializeDropsRouteSpecificHoverState() {
        CanvasSceneRuntime runtime = new CanvasSceneRuntime(TerminalThemeRegistry.getDefaultTheme());
        TestScene scene = new TestScene(runtime);
        runtime.initialize(scene, new PanelContainer());
        runtime.openHoverOverlay(scene, new CountingPanel());
        assertTrue(runtime.hasHoverOverlay());
        runtime.initialize(scene, new PanelContainer());
        assertFalse(runtime.hasHoverOverlay());
    }

    private static final class CountingPanel extends AbstractGuiPanel {
        private int clicks;
        @Override public void draw(GuiScene scene, int mouseX, int mouseY, float partialTicks) { }
        @Override public boolean mouseClicked(GuiScene scene, int mouseX, int mouseY, int mouseButton) {
            clicks++; return true;
        }
    }

    private static final class TestScene implements GuiScene {
        private final CanvasSceneRuntime runtime;
        private TestScene(CanvasSceneRuntime runtime) { this.runtime = runtime; }
        @Override public GuiTheme getTheme() { return runtime.getTheme(); }
        @Override public void openPopup(GuiPanel panel) { runtime.openPopup(this, panel); }
        @Override public void closePopup() { runtime.closePopup(); }
        @Override public void openHoverOverlay(GuiPanel panel) { runtime.openHoverOverlay(this, panel); }
        @Override public void closeHoverOverlay() { runtime.closeHoverOverlay(); }
    }
}
