package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TerminalMarketActionPayloadTest {

    @Test
    public void keepsCatalogBrowseContextAcrossWireEncoding() {
        TerminalMarketActionPayload payload = new TerminalMarketActionPayload(
            "gregtech:steel_ingot:0", "102", "4", "", "", "99", "2", "1", "1",
            "steel ingot", "3", "enabled");

        TerminalMarketActionPayload decoded = TerminalMarketActionPayload.decode(payload.encode());

        assertEquals("steel ingot", decoded.getBrowserQuery());
        assertEquals(3, decoded.getBrowserPage());
        assertEquals("enabled", decoded.getBrowserFilter());
        assertEquals("gregtech:steel_ingot:0", decoded.getSelectedProductKey());
    }

    @Test
    public void readsExistingNineFieldPayloadWithoutLosingTradeDrafts() {
        TerminalMarketActionPayload oldPayload = new TerminalMarketActionPayload(
            "gregtech:steel_ingot:0", "102", "4", "", "", "99", "2", "1", "1");

        TerminalMarketActionPayload decoded = TerminalMarketActionPayload.decode(oldPayload.encode());

        assertEquals("102", decoded.getLimitBuyPriceText());
        assertEquals("4", decoded.getLimitBuyQuantityText());
        assertEquals(0, decoded.getBrowserPage());
    }

    @Test
    public void keepsExplicitVaultDepositQuantityAcrossWireEncoding() {
        TerminalMarketActionPayload payload = new TerminalMarketActionPayload(
            "gregtech:steel_ingot:0", "", "", "", "", "", "", "", "",
            "steel", "1", "enabled", "48");

        TerminalMarketActionPayload decoded = TerminalMarketActionPayload.decode(payload.encode());

        assertEquals("48", decoded.getVaultDepositQuantityText());
        assertEquals(48L, decoded.parseVaultDepositQuantity());
        assertEquals("steel", decoded.getBrowserQuery());
    }

    @Test
    public void unifiedOrderEncodingPreservesTicketAndLegacyBrowseContext() {
        TerminalMarketActionPayload payload = new TerminalMarketActionPayload(
            "gregtech:steel_ingot:0", "", "", "", "", "", "", "", "",
            "steel", "2", "book", "").withOrderTicket("SELL", "LIMIT", "48", "103", "24h", "volume");

        TerminalMarketActionPayload decoded = TerminalMarketActionPayload.decode(payload.encodeUnifiedOrder());

        assertEquals("SELL", decoded.getOrderSide());
        assertEquals("LIMIT", decoded.getOrderType());
        assertEquals(48L, decoded.parseOrderQuantity());
        assertEquals(103L, decoded.parseOrderLimitPrice());
        assertEquals("24h", decoded.getChartRange());
        assertEquals("volume", decoded.getBrowserSort());
        assertEquals("steel", decoded.getBrowserQuery());
    }

    @Test
    public void historyEncodingPreservesSearchAndFilters() {
        TerminalMarketActionPayload payload = new TerminalMarketActionPayload(
            "gregtech:steel_ingot:0", "", "", "", "", "", "", "", "")
                .withHistory("ALL", "SELL", "OPEN", "WEEK", "steel ingot", 2, 7);

        TerminalMarketActionPayload decoded = TerminalMarketActionPayload.decode(payload.encodeHistory());

        assertEquals("steel ingot", decoded.getHistoryQuery());
        assertEquals("SELL", decoded.getHistorySide());
        assertEquals("OPEN", decoded.getHistoryStatus());
        assertEquals("WEEK", decoded.getHistoryTime());
        assertEquals(2, decoded.getHistoryPage());
        assertEquals(7, decoded.getHistoryPageSize());
    }
}
