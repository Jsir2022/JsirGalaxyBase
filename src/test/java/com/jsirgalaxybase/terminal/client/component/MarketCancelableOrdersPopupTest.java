package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;

public class MarketCancelableOrdersPopupTest {

    @Test
    public void popupIsCenteredInsideTerminalBoundsAndUsesContentDrivenHeight() {
        GuiRect terminal = new GuiRect(40, 70, 760, 360);
        MarketCancelableOrdersPopup popup = new MarketCancelableOrdersPopup(terminal,
            TerminalMarketSectionModel.placeholder("market_standardized"), null);

        GuiRect bounds = popup.getBounds();
        assertTrue(bounds.getX() >= terminal.getX());
        assertTrue(bounds.getY() >= terminal.getY());
        assertTrue(bounds.getRight() <= terminal.getRight());
        assertTrue(bounds.getBottom() <= terminal.getBottom());
        assertTrue(bounds.getHeight() <= 154);
    }
}
