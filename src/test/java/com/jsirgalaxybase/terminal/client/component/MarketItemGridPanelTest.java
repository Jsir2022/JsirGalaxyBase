package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.jsirgalaxybase.client.gui.framework.GuiRect;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;

public class MarketItemGridPanelTest {

    @Test
    public void keepsFourColumnsAndConfinesScrollingToGridContent() {
        MarketItemGridPanel panel = new MarketItemGridPanel(Arrays.asList(
            item("iron"), item("steel"), item("copper"), item("gold"), item("tin"), item("lead"), item("silver"), item("nickel")), null);
        panel.setBounds(new GuiRect(20, 30, 260, 70));

        assertEquals(4, MarketItemGridPanel.getColumns());
        assertTrue(panel.getMaxScrollOffset() > 0);
        panel.setScrollOffset(Integer.MAX_VALUE);
        assertEquals(panel.getMaxScrollOffset(), panel.getScrollOffset());
    }

    @Test
    public void emptyGridHasNoScrollableOverflow() {
        MarketItemGridPanel panel = new MarketItemGridPanel(Collections.<MarketBrowseItemModel>emptyList(), null);
        panel.setBounds(new GuiRect(0, 0, 160, 100));
        assertEquals(0, panel.getMaxScrollOffset());
    }

    @Test
    public void compactMarketTextKeepsCurrencyOutOfDenseCards() {
        MarketBrowseItemModel item = new MarketBrowseItemModel("steel", "minecraft:iron_ingot", "Steel Ingot",
            "102 STARCOIN", "可交易", "101 STARCOIN", "103 STARCOIN", "1240", "320", "64", "8", "-1.6%",
            Collections.<TerminalMarketSectionModel.PricePointModel>emptyList());

        assertEquals("102", item.getCompactLatestPrice());
        assertEquals("101", item.getCompactBestBid());
        assertEquals("103", item.getCompactBestAsk());
        assertEquals("-1.6%", item.getDayChange());
        assertTrue(item.hasDayChange());
    }

    private static MarketBrowseItemModel item(String key) {
        return new MarketBrowseItemModel(key, "minecraft:iron_ingot", key, "100", "在线", "--", "--", "--", "0",
            Collections.<TerminalMarketSectionModel.PricePointModel>emptyList());
    }
}
