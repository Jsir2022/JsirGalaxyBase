package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.jsirgalaxybase.client.gui.framework.GuiPanel;
import com.jsirgalaxybase.client.gui.framework.GuiRect;

public class MarketOrderEntryPopupTest {

    @Test
    public void keepsOrderControlsInsideCompactPopup() {
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        MarketOrderEntryPopup popup = new MarketOrderEntryPopup(420, 260, state,
            TerminalMarketSectionState.OrderSide.BUY, TerminalMarketSectionState.OrderType.MARKET,
            "Steel Ingot", "102", "银行结算", new Runnable() {
                @Override
                public void run() {}
            }, new Runnable() {
                @Override
                public void run() {}
            });

        assertEquals("1", state.getInstantBuyQuantityText());
        for (GuiPanel child : popup.getChildren()) {
            if (child.isVisible()) {
                assertInside(popup.getBounds(), child.getBounds());
            }
        }
    }

    @Test
    public void seedsLimitPriceWithoutRenderingAnInlineDetailForm() {
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        MarketOrderEntryPopup popup = new MarketOrderEntryPopup(320, 220, state,
            TerminalMarketSectionState.OrderSide.SELL, TerminalMarketSectionState.OrderType.LIMIT,
            "Gold Ingot", "256", "可卖 32", new Runnable() {
                @Override
                public void run() {}
            }, new Runnable() {
                @Override
                public void run() {}
            });

        assertEquals("1", state.getLimitSellQuantityText());
        assertEquals("256", state.getLimitSellPriceText());
        assertEquals(7, popup.getChildren().size());
        for (GuiPanel child : popup.getChildren()) {
            assertTrue(!child.isVisible() || child.getBounds().getWidth() > 0);
        }
    }

    @Test
    public void computesMaximumFromBalanceOrSellableInventory() {
        TerminalMarketSectionState buyState = new TerminalMarketSectionState();
        MarketOrderEntryPopup buyPopup = new MarketOrderEntryPopup(420, 260, buyState,
            TerminalMarketSectionState.OrderSide.BUY, TerminalMarketSectionState.OrderType.MARKET,
            "Steel Ingot", "25", "银行 260", 260L, null, null);
        assertEquals(10L, buyPopup.maximumQuantity());
        buyPopup.applyMaximumQuantity();
        assertEquals("10", buyState.getInstantBuyQuantityText());

        TerminalMarketSectionState sellState = new TerminalMarketSectionState();
        MarketOrderEntryPopup sellPopup = new MarketOrderEntryPopup(420, 260, sellState,
            TerminalMarketSectionState.OrderSide.SELL, TerminalMarketSectionState.OrderType.LIMIT,
            "Steel Ingot", "25", "可卖 37", 37L, null, null);
        assertEquals(37L, sellPopup.maximumQuantity());
        sellPopup.applyMaximumQuantity();
        assertEquals("37", sellState.getLimitSellQuantityText());
    }

    @Test
    public void extractsOnlyThePriceFromVerboseOrderBookRows() {
        TerminalMarketSectionState buyState = new TerminalMarketSectionState();
        MarketOrderEntryPopup buyPopup = new MarketOrderEntryPopup(420, 260, buyState,
            TerminalMarketSectionState.OrderSide.BUY, TerminalMarketSectionState.OrderType.MARKET,
            "Iron Ingot", "卖价 65 | 剩余 48 |", "余额 320", 320L, null, null);
        assertEquals(4L, buyPopup.maximumQuantity());

        TerminalMarketSectionState sellState = new TerminalMarketSectionState();
        new MarketOrderEntryPopup(420, 260, sellState,
            TerminalMarketSectionState.OrderSide.SELL, TerminalMarketSectionState.OrderType.LIMIT,
            "Iron Ingot", "买价 63 | 剩余 51 |", "可卖 64", 64L, null, null);
        assertEquals("63", sellState.getLimitSellPriceText());
    }

    @Test
    public void completeSellTicketDispatchesSubmitAction() {
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        AtomicInteger submissions = new AtomicInteger();
        MarketOrderEntryPopup popup = new MarketOrderEntryPopup(420, 260, state,
            TerminalMarketSectionState.OrderSide.SELL, TerminalMarketSectionState.OrderType.MARKET,
            "Iron Ingot", "买价 63 | 剩余 51 |", "可卖 64", 64L,
            new Runnable() {
                @Override
                public void run() { submissions.incrementAndGet(); }
            }, null);

        popup.submit();
        assertEquals(1, submissions.get());
    }

    private static void assertInside(GuiRect parent, GuiRect child) {
        assertTrue(child.getX() >= parent.getX());
        assertTrue(child.getY() >= parent.getY());
        assertTrue(child.getRight() <= parent.getRight());
        assertTrue(child.getBottom() <= parent.getBottom());
    }
}
