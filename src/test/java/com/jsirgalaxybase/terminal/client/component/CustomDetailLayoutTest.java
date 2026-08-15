package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.client.gui.framework.GuiRect;

public class CustomDetailLayoutTest {

    @Test
    public void keepsListingDeliveryAndActionsInsideTheTerminal() {
        GuiRect parent = new GuiRect(80, 42, 720, 390);
        CustomDetailLayout layout = CustomDetailLayout.within(parent);

        assertInside(parent, layout.hero);
        assertInside(parent, layout.listing);
        assertInside(parent, layout.delivery);
        assertInside(parent, layout.actions);
        assertTrue(layout.hero.getBottom() < layout.listing.getY());
        assertTrue(layout.listing.getRight() < layout.delivery.getX());
        assertTrue(layout.listing.getBottom() < layout.actions.getY());
        assertTrue(layout.delivery.getBottom() < layout.actions.getY());
    }

    @Test
    public void remainsUsableAtCompactTerminalHeight() {
        GuiRect parent = new GuiRect(0, 0, 420, 180);
        CustomDetailLayout layout = CustomDetailLayout.within(parent);

        assertInside(parent, layout.hero);
        assertInside(parent, layout.listing);
        assertInside(parent, layout.delivery);
        assertInside(parent, layout.actions);
        assertTrue(layout.listing.getHeight() > 0);
        assertTrue(layout.delivery.getHeight() > 0);
        assertTrue(layout.actions.getHeight() > 0);
    }

    private static void assertInside(GuiRect parent, GuiRect child) {
        assertTrue(child.getX() >= parent.getX());
        assertTrue(child.getY() >= parent.getY());
        assertTrue(child.getRight() <= parent.getRight());
        assertTrue(child.getBottom() <= parent.getBottom());
    }
}
