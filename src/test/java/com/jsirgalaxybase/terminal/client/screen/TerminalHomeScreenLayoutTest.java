package com.jsirgalaxybase.terminal.client.screen;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;

public class TerminalHomeScreenLayoutTest {

    @Test
    public void smallScreensKeepMainPanelInsideSafeMargins() {
        TerminalHomeLayout layout = TerminalHomeLayout.compute(360, 240);

        assertInside(layout.panelBounds, 360, 240);
        assertInside(layout.statusBandBounds, 360, 240);
        assertInside(layout.navigationBounds, 360, 240);
        assertInside(layout.bodyBounds, 360, 240);
        assertTrue(layout.panelBounds.getX() >= 6);
        assertTrue(layout.panelBounds.getY() >= 6);
        assertTrue(layout.panelBounds.getRight() <= 354);
        assertTrue(layout.panelBounds.getBottom() <= 234);
    }

    @Test
    public void largeScreensStillCapTerminalSurface() {
        TerminalHomeLayout layout = TerminalHomeLayout.compute(1920, 1080);

        assertTrue(layout.panelBounds.getWidth() <= 920);
        assertTrue(layout.panelBounds.getHeight() <= 400);
        assertInside(layout.panelBounds, 1920, 1080);
    }

    @Test
    public void terminalAnchorsHigherToKeepHotbarVisible() {
        TerminalHomeLayout layout = TerminalHomeLayout.compute(1280, 720, TerminalHomeScreenModel.placeholder());

        assertTrue(layout.panelBounds.getHeight() >= 240);
        assertTrue(layout.panelBounds.getHeight() <= 340);
        assertTrue(layout.panelBounds.getBottom() >= 632);
        assertTrue(layout.panelBounds.getBottom() <= 656);
        assertTrue(layout.panelBounds.getY() >= 280);
    }

    @Test
    public void navigationGeometryStaysStableWhileBodyRemainsExpanded() {
        TerminalHomeLayout homeLayout = TerminalHomeLayout.compute(1280, 720, TerminalHomeScreenModel.placeholder());
        TerminalHomeLayout marketLayout = TerminalHomeLayout.compute(
            1280,
            720,
            TerminalHomeScreenModel.placeholder().withSelectedPageId("market"));

        assertTrue(homeLayout.navigationVisible);
        assertTrue(marketLayout.navigationVisible);
        assertTrue(marketLayout.bodyBounds.getX() == homeLayout.bodyBounds.getX());
        assertTrue(marketLayout.bodyBounds.getWidth() == homeLayout.bodyBounds.getWidth());
        assertTrue(homeLayout.navigationBounds.getX() <= homeLayout.bodyBounds.getX());
        assertTrue(homeLayout.navigationBounds.getRight() <= homeLayout.bodyBounds.getRight());
    }

    @Test
    public void navigationRailKeepsEnoughWidthForIconAndShortLabels() {
        TerminalHomeLayout layout = TerminalHomeLayout.compute(1280, 720, TerminalHomeScreenModel.placeholder());

        assertTrue(layout.navigationBounds.getWidth() >= 86);
        assertTrue(layout.bodyBounds.getX() >= layout.navigationBounds.getRight());
        assertTrue(layout.bodyBounds.getWidth() >= 420);
    }

    private static void assertInside(GuiRect rect, int screenWidth, int screenHeight) {
        assertTrue(rect.getX() >= 0);
        assertTrue(rect.getY() >= 0);
        assertTrue(rect.getRight() <= screenWidth);
        assertTrue(rect.getBottom() <= screenHeight);
    }
}
