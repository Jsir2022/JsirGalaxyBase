package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.jsirgalaxybase.client.gui.framework.GuiPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.client.gui.framework.PanelContainer;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalLandSectionModel;

public class TerminalLandSectionLayoutTest {

    @Test
    public void propertyUsesDedicatedBoundedSectionWithoutOuterScroll() {
        assertLandLayout(new GuiRect(10, 20, 620, 340));
    }

    @Test
    public void highGuiScaleEquivalentKeepsMapAsPrimaryWorkspace() {
        assertLandLayout(new GuiRect(4, 8, 350, 193));
    }

    @Test
    public void inspectorWidthStaysWithinResponsiveQuarter() {
        assertEquals(84, TerminalLandSection.inspectorWidth(350));
        assertEquals(149, TerminalLandSection.inspectorWidth(620));
        assertEquals(176, TerminalLandSection.inspectorWidth(900));
    }

    private static void assertLandLayout(GuiRect bounds) {
        List<String> cells = new ArrayList<String>();
        for (int i = 0; i < 225; i++) cells.add("UNCLAIMED");
        TerminalLandSectionModel land = new TerminalLandSectionModel("READY", "lobby", "SHADOW", 0,
            0, 0, 0, 0, 0, 4, "NEARBY", cells, Collections.<Long>emptyList(),
            Collections.<Integer>emptyList(), Collections.<Integer>emptyList(), Collections.<Long>emptyList(),
            0, 0, 0, 0L, 0L, "UNCLAIMED", true, false, "NONE");
        TerminalHomeScreenModel.PageSnapshotModel page = new TerminalHomeScreenModel.PageSnapshotModel("property",
            "个人地产", "附近地皮", Collections.<TerminalHomeScreenModel.SectionModel>emptyList(),
            null, null, null, null, null, land);
        TerminalHomeScreenModel model = new TerminalHomeScreenModel("property", "终端", "",
            new TerminalHomeScreenModel.StatusBandModel("", "", "", "", ""),
            Collections.singletonList(new TerminalHomeScreenModel.NavItemModel("property", "地产", "", true, true)),
            Collections.singletonList(page), Collections.<TerminalHomeScreenModel.NotificationModel>emptyList(), "s");
        PanelContainer body = TerminalShellPanels.createSectionBody(new TerminalPanelFactory(), bounds, model,
            null, null, null, null, null, null, null, null, new TerminalLandSectionState(), null);

        assertEquals(1, body.getChildren().size());
        GuiPanel section = body.getChildren().get(0);
        assertTrue(section instanceof TerminalLandSection);
        assertEquals(bounds.getX(), section.getBounds().getX());
        assertEquals(bounds.getY(), section.getBounds().getY());
        assertEquals(bounds.getWidth(), section.getBounds().getWidth());
        assertEquals(bounds.getHeight(), section.getBounds().getHeight());
        PanelContainer landSection = (PanelContainer) section;
        PanelContainer content = (PanelContainer) landSection.getChildren().get(4);
        assertEquals(2, content.getChildren().size());
        assertTrue(content.getChildren().get(0) instanceof TerminalLandMapPanel);
        GuiRect mapBounds = content.getChildren().get(0).getBounds();
        assertTrue(mapBounds.getWidth() > mapBounds.getHeight());
        assertTrue(mapBounds.getWidth() * 100 / content.getBounds().getWidth() >= 74);
        assertTrue(mapBounds.getBottom() <= bounds.getBottom());
        GuiRect inspectorBounds = content.getChildren().get(1).getBounds();
        assertTrue(inspectorBounds.getWidth() * 100 / content.getBounds().getWidth() <= 25);
        assertTrue(inspectorBounds.getWidth() >= 84);
        assertTrue(mapBounds.getRight() < inspectorBounds.getX());
        assertTrue(inspectorBounds.getBottom() <= bounds.getBottom());
        assertEquals(bounds.getY() + 32, content.getBounds().getY());
        assertEquals(bounds.getBottom(), content.getBounds().getBottom());
    }
}
