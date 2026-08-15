package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.client.gui.framework.GuiRect;

public class ExchangeDetailLayoutTest {

    @Test
    public void keepsDedicatedQuoteWorkspaceInsideTerminalBounds() {
        GuiRect parent = new GuiRect(80, 42, 720, 390);
        ExchangeDetailLayout layout = ExchangeDetailLayout.within(parent);

        assertInside(parent, layout.hero);
        assertInside(parent, layout.source);
        assertInside(parent, layout.quote);
        assertInside(parent, layout.actions);
        assertTrue(layout.hero.getBottom() < layout.source.getY());
        assertTrue(layout.source.getRight() < layout.quote.getX());
        assertTrue(layout.source.getBottom() < layout.actions.getY());
        assertTrue(layout.quote.getBottom() < layout.actions.getY());
    }

    @Test
    public void remainsNonOverlappingAtCompactHeight() {
        GuiRect parent = new GuiRect(0, 0, 420, 180);
        ExchangeDetailLayout layout = ExchangeDetailLayout.within(parent);

        assertInside(parent, layout.hero);
        assertInside(parent, layout.source);
        assertInside(parent, layout.quote);
        assertInside(parent, layout.actions);
        assertTrue(layout.source.getWidth() > 0);
        assertTrue(layout.quote.getWidth() > 0);
        assertTrue(layout.actions.getHeight() > 0);
    }

    private static void assertInside(GuiRect parent, GuiRect child) {
        assertTrue(child.getX() >= parent.getX());
        assertTrue(child.getY() >= parent.getY());
        assertTrue(child.getRight() <= parent.getRight());
        assertTrue(child.getBottom() <= parent.getBottom());
    }
}
