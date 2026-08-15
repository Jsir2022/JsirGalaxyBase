package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.client.gui.framework.GuiRect;

public class MarketDetailLayoutTest {

    @Test
    public void keepsEveryDetailBandInsideTheParent() {
        GuiRect parent = new GuiRect(100, 50, 620, 350);
        MarketDetailLayout layout = MarketDetailLayout.withinStandardSplit(parent);

        assertInside(parent, layout.hero);
        assertInside(parent, layout.chart);
        assertInside(parent, layout.orderBook);
        assertInside(parent, layout.ticket);
        assertInside(parent, layout.footer);
        assertTrue(layout.hero.getBottom() <= layout.orderBook.getY());
        assertTrue(layout.orderBook.getBottom() <= layout.ticket.getY());
        assertTrue(layout.hero.getRight() <= layout.chart.getX());
        assertTrue(layout.chart.getHeight() > layout.orderBook.getHeight());
        assertTrue(layout.chart.getBottom() == layout.ticket.getBottom());
        assertTrue(layout.hero.getHeight() <= 30);
        assertTrue(layout.ticket.getHeight() >= 76);
        assertTrue(layout.ticket.getHeight() <= 84);
        assertTrue(layout.footer.getWidth() == 0);
        assertTrue(layout.footer.getHeight() == 0);
    }

    @Test
    public void preservesAVisibleOrderBookAtCompactHeight() {
        MarketDetailLayout layout = MarketDetailLayout.withinStandardSplit(new GuiRect(0, 0, 480, 190));
        assertTrue(layout.orderBook.getHeight() >= 0);
        assertTrue(layout.actions.getHeight() >= 0);
        assertTrue(layout.hero.getWidth() < layout.chart.getWidth());
    }

    private static void assertInside(GuiRect parent, GuiRect child) {
        assertTrue(child.getX() >= parent.getX());
        assertTrue(child.getY() >= parent.getY());
        assertTrue(child.getRight() <= parent.getRight());
        assertTrue(child.getBottom() <= parent.getBottom());
    }
}
