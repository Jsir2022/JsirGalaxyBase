package com.jsirgalaxybase.modules.warehouse.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.terminal.client.screen.TerminalHomeLayout;

public class TerminalAssetCenterLayoutTest {
    @Test public void compactHighScaleScreenNeverExceedsAvailableBounds() {
        TerminalAssetCenterLayout layout = TerminalAssetCenterLayout.compute(350, 193);
        assertEquals(342, layout.getWidth());
        assertEquals(185, layout.getHeight());
        assertTrue(layout.fitsScreen(350, 193));
        assertTrue(layout.getHeaderBottom() < layout.getSummaryTop());
        assertTrue(layout.getTabsBottom() < layout.getBodyTop());
        assertTrue(layout.getBodyTop() < layout.getBodyBottom());
        assertTrue(layout.getBodyBottom() < layout.getHeight());
    }

    @Test public void standardScreenUsesBoundedLargeWorkspace() {
        TerminalAssetCenterLayout layout = TerminalAssetCenterLayout.compute(620, 340);
        assertEquals(TerminalAssetCenterLayout.MAX_WIDTH, layout.getWidth());
        assertEquals(TerminalAssetCenterLayout.MAX_HEIGHT, layout.getHeight());
        assertTrue(layout.fitsScreen(620, 340));
    }

    @Test public void compactTerminalBodyKeepsVaultAndBackpackLeftOfCellBrowser() {
        TerminalHomeLayout shell = TerminalHomeLayout.compute(350, 193);
        TerminalAssetCenterContentLayout content = TerminalAssetCenterContentLayout.compute(
            shell.getBodyBounds(), shell.getPanelBounds());
        assertTrue(content.isCompact());
        assertTrue(content.fits());
        assertTrue(content.getPlayerY() > content.getVaultY());
        assertTrue(content.getLeftPane().getRight() < content.getRightPane().getRight());
        assertTrue(content.getBaySlotX() + 18 < shell.getPanelBounds().getWidth());
        assertTrue(content.getCellPageSize() >= 1);
    }

    @Test public void wideTerminalBodyKeepsNativeInventoriesTogetherOnTheLeft() {
        GuiRect panel = new GuiRect(10, 10, 620, 300);
        GuiRect body = new GuiRect(100, 30, 500, 260);
        TerminalAssetCenterContentLayout content = TerminalAssetCenterContentLayout.compute(body, panel);
        assertTrue(!content.isCompact());
        assertEquals(content.getVaultX(), content.getPlayerX());
        assertTrue(content.getPlayerY() > content.getVaultY());
        assertTrue(content.getRightPane().getX() > content.getLeftPane().getX());
        assertTrue(content.getCellPageSize() <= 35);
    }
}
