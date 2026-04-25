package com.jsirgalaxybase.terminal.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TerminalPageTest {

    @Test
    public void marketRoutesAreSeparatedFromMarketRoot() {
        assertTrue(TerminalPage.MARKET.isMarketPage());
        assertTrue(TerminalPage.MARKET_STANDARDIZED.isMarketPage());
        assertTrue(TerminalPage.MARKET_CUSTOM.isMarketPage());
        assertTrue(TerminalPage.MARKET_EXCHANGE.isMarketPage());
        assertFalse(TerminalPage.BANK.isMarketPage());
        assertFalse(TerminalPage.HOME.isMarketPage());
    }

    @Test
    public void marketRootAndSubRoutesKeepDistinctTitles() {
        assertEquals("市场总入口", TerminalPage.MARKET.title);
        assertEquals("标准商品市场", TerminalPage.MARKET_STANDARDIZED.title);
        assertEquals("定制商品市场", TerminalPage.MARKET_CUSTOM.title);
        assertEquals("汇率市场", TerminalPage.MARKET_EXCHANGE.title);
        assertEquals(TerminalPage.MARKET_EXCHANGE, TerminalPage.byIndex(TerminalPage.MARKET_EXCHANGE.index));
    }

    @Test
    public void marketSubRoutesStillResolveToMarketNavigationHost() {
        assertEquals("market", TerminalPage.MARKET.toTopLevelPageId());
        assertEquals("market", TerminalPage.MARKET_STANDARDIZED.toTopLevelPageId());
        assertEquals("market", TerminalPage.MARKET_CUSTOM.toTopLevelPageId());
        assertEquals("market", TerminalPage.MARKET_EXCHANGE.toTopLevelPageId());
    }
}
