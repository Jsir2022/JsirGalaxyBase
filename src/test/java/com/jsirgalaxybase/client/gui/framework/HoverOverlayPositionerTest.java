package com.jsirgalaxybase.client.gui.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HoverOverlayPositionerTest {

    @Test
    public void flipsAndClampsAtEveryTerminalEdge() {
        GuiRect owner = new GuiRect(100, 50, 300, 200);
        assertInside(owner, HoverOverlayPositioner.place(owner, 105, 55, 120, 80));
        assertInside(owner, HoverOverlayPositioner.place(owner, 395, 55, 120, 80));
        assertInside(owner, HoverOverlayPositioner.place(owner, 105, 245, 120, 80));
        GuiRect bottomRight = HoverOverlayPositioner.place(owner, 395, 245, 120, 80);
        assertInside(owner, bottomRight);
        assertTrue(bottomRight.getX() < 395);
        assertTrue(bottomRight.getY() < 245);
    }

    @Test
    public void limitsOversizedTooltipToOwner() {
        GuiRect owner = new GuiRect(10, 20, 80, 40);
        GuiRect tooltip = HoverOverlayPositioner.place(owner, 50, 35, 400, 200);
        assertEquals(owner.getWidth(), tooltip.getWidth());
        assertEquals(owner.getHeight(), tooltip.getHeight());
        assertInside(owner, tooltip);
    }

    private static void assertInside(GuiRect owner, GuiRect child) {
        assertTrue(child.getX() >= owner.getX());
        assertTrue(child.getY() >= owner.getY());
        assertTrue(child.getRight() <= owner.getRight());
        assertTrue(child.getBottom() <= owner.getBottom());
    }
}
