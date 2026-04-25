package com.jsirgalaxybase.terminal.client.screen;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.client.gui.framework.GuiRect;

public class TerminalHomeScreenLayoutTest {

    @Test
    public void smallScreensKeepMainPanelInsideSafeMargins() {
        TerminalHomeLayout layout = TerminalHomeLayout.compute(360, 240);

        assertInside(layout.panelBounds, 360, 240);
        assertInside(layout.statusBandBounds, 360, 240);
        assertInside(layout.navigationBounds, 360, 240);
        assertInside(layout.bodyBounds, 360, 240);
        assertTrue(layout.panelBounds.getX() >= 8);
        assertTrue(layout.panelBounds.getY() >= 8);
        assertTrue(layout.panelBounds.getRight() <= 352);
        assertTrue(layout.panelBounds.getBottom() <= 232);
    }

    @Test
    public void largeScreensStillCapTerminalSurface() {
        TerminalHomeLayout layout = TerminalHomeLayout.compute(1920, 1080);

        assertTrue(layout.panelBounds.getWidth() <= 920);
        assertTrue(layout.panelBounds.getHeight() <= 680);
        assertInside(layout.panelBounds, 1920, 1080);
    }

    private static void assertInside(GuiRect rect, int screenWidth, int screenHeight) {
        assertTrue(rect.getX() >= 0);
        assertTrue(rect.getY() >= 0);
        assertTrue(rect.getRight() <= screenWidth);
        assertTrue(rect.getBottom() <= screenHeight);
    }
}
