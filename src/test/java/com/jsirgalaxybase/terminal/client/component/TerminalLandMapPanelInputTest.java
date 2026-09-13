package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.lwjgl.input.Keyboard;

import com.jsirgalaxybase.terminal.TerminalLandActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalLandSectionModel;
import com.jsirgalaxybase.ui2.geometry.UiRect;

public class TerminalLandMapPanelInputTest {

    @Test
    public void clickDragRightClickAndEscapeRemainDistinct() {
        RecordingHandler handler = new RecordingHandler();
        TerminalLandSectionState state = new TerminalLandSectionState();
        TerminalLandMapPanel panel = new TerminalLandMapPanel(model(), state, handler);
        panel.setBounds(new UiRect(10, 20, 450, 270));

        assertTrue(panel.mouseClicked(235, 155, 0));
        assertTrue(panel.mouseReleased(275, 155, 0));
        assertEquals(0, handler.selects);
        assertEquals(1, handler.viewports);

        assertTrue(panel.mouseClicked(235, 155, 1));
        assertTrue(panel.isContextOpen());
        assertTrue(panel.keyTyped('\0', Keyboard.KEY_ESCAPE));
        assertFalse(panel.isContextOpen());
    }

    @Test
    public void contextActionAndKeyboardShortcutsUseExistingHandlers() {
        RecordingHandler handler = new RecordingHandler();
        TerminalLandMapPanel panel = new TerminalLandMapPanel(model(), new TerminalLandSectionState(), handler);
        panel.setBounds(new UiRect(10, 20, 450, 270));

        panel.mouseClicked(235, 155, 1);
        assertTrue(panel.mouseClicked(245, 179, 0));
        assertEquals(1, handler.contextActions);
        assertFalse(handler.lastUnclaim);

        assertTrue(panel.keyTyped('f', Keyboard.KEY_F));
        assertTrue(panel.keyTyped('+', Keyboard.KEY_ADD));
        assertTrue(panel.keyTyped('-', Keyboard.KEY_MINUS));
        assertEquals(3, handler.viewports);
    }

    @Test
    public void ownershipLayerTogglePersistsInSectionState() {
        TerminalLandSectionState state = new TerminalLandSectionState();
        assertTrue(state.isOwnershipOverlayVisible());
        state.toggleOwnershipOverlay();
        assertFalse(state.isOwnershipOverlayVisible());
        state.toggleOwnershipOverlay();
        assertTrue(state.isOwnershipOverlayVisible());
    }

    @Test
    public void playerMarkerTracksCellSizeWithoutCoveringMultipleChunks() {
        assertEquals(20, TerminalLandMapPanel.playerMarkerSize(30D, TerminalLandActionPayload.Zoom.NEAR));
        assertEquals(16, TerminalLandMapPanel.playerMarkerSize(17.5D, TerminalLandActionPayload.Zoom.MEDIUM));
        assertEquals(6, TerminalLandMapPanel.playerMarkerSize(8D, TerminalLandActionPayload.Zoom.FAR));
        assertTrue(TerminalLandMapPanel.playerMarkerSize(17.5D, TerminalLandActionPayload.Zoom.MEDIUM) <= 17.5D);
    }

    @Test
    public void hoverCardUsesMapRelativeBoundsAtHighGuiScale() {
        assertEquals(110, TerminalLandMapPanel.tooltipWidth(262, 200));
        assertEquals(88, TerminalLandMapPanel.tooltipWidth(180, 200));
        assertEquals(132, TerminalLandMapPanel.tooltipWidth(470, 200));
    }

    private static TerminalLandSectionModel model() {
        List<String> grid = new ArrayList<String>();
        for (int index = 0; index < 225; index++) grid.add("UNCLAIMED");
        return new TerminalLandSectionModel("READY", "lobby", "SHADOW", 0,
            0, 0, 0, 0, 0, 4, "NEARBY", grid, Collections.<Long>emptyList(),
            Collections.<Integer>emptyList(), Collections.<Integer>emptyList(), Collections.<Long>emptyList(),
            0, 0, 0, 0L, 0L, "UNCLAIMED", true, false, "NONE");
    }

    private static final class RecordingHandler implements TerminalLandMapPanel.Handler {
        int selects;
        int viewports;
        int refreshes;
        int contextActions;
        boolean lastUnclaim;

        @Override public void select(int chunkX, int chunkZ, long version, boolean terrainLoaded) { selects++; }
        @Override public void viewport(int chunkX, int chunkZ, TerminalLandActionPayload.Zoom zoom) { viewports++; }
        @Override public void refresh() { refreshes++; }
        @Override public void contextAction(boolean unclaim) {
            contextActions++;
            lastUnclaim = unclaim;
        }
    }
}
