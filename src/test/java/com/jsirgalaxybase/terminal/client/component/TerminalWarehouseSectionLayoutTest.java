package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.client.gui.framework.GuiRect;

public class TerminalWarehouseSectionLayoutTest {
    @Test public void compactOverviewUsesColumnsAndNeverDrawsBelowTerminal() {
        GuiRect parent = new GuiRect(67, 39, 350, 193);
        TerminalWarehouseSectionLayout layout = TerminalWarehouseSectionLayout.compute(parent);
        assertTrue(layout.fitsWithin(parent));
        assertEquals(layout.vaultCard.getY(), layout.bayCard.getY());
        assertEquals(layout.vaultButton.getY(), layout.bayButton.getY());
        assertTrue(layout.vaultCard.getRight() < layout.bayCard.getRight());
        assertTrue(layout.auditCard.getBottom() <= parent.getBottom());
    }

    @Test public void standardOverviewAlsoKeepsEveryRegionInside() {
        GuiRect parent = new GuiRect(100, 60, 620, 340);
        TerminalWarehouseSectionLayout layout = TerminalWarehouseSectionLayout.compute(parent);
        assertTrue(layout.fitsWithin(parent));
        assertTrue(layout.auditCard.getHeight() > 0);
    }
}
