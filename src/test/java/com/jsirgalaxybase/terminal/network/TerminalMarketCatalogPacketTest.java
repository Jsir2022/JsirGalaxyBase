package com.jsirgalaxybase.terminal.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TerminalMarketCatalogPacketTest {

    @Test
    public void formalCatalogRowsAndPaginationSurviveTerminalPacketRoundTrip() {
        TerminalMarketSectionModel source = TerminalMarketSectionModel.placeholder("market_standardized")
            .withCatalogPage(Arrays.asList(new TerminalMarketSectionModel.CatalogProductModel(
                "gregtech:gt.metaitem.01:11305",
                "gregtech:gt.metaitem.01",
                11305,
                "Steel Ingot",
                "ingot",
                120,
                true,
                102L,
                "可交易", new TerminalMarketSectionModel.CatalogMarketSummaryModel(
                    "102 STARCOIN", "101 STARCOIN", "103 STARCOIN", "64", "7", "2", "1", "+2.0%",
                    Arrays.asList(new TerminalMarketSectionModel.PricePointModel(99L, 8L, 100L),
                        new TerminalMarketSectionModel.PricePointModel(102L, 4L, 101L))))), "steel", 1, 8, 17, true, true);

        ByteBuf buffer = Unpooled.buffer();
        OpenTerminalApprovedMessage.writeMarketSection(buffer, source);
        TerminalMarketSectionModel decoded = OpenTerminalApprovedMessage.readMarketSection(buffer);

        assertEquals("steel", decoded.getCatalogQuery());
        assertEquals(1, decoded.getCatalogPageIndex());
        assertEquals(3, decoded.getCatalogTotalPages());
        assertTrue(decoded.hasCatalogPreviousPage());
        assertTrue(decoded.hasCatalogNextPage());
        assertEquals(1, decoded.getCatalogProducts().size());
        TerminalMarketSectionModel.CatalogProductModel row = decoded.getCatalogProducts().get(0);
        assertEquals("Steel Ingot", row.getDisplayName());
        assertEquals(11305, row.getMeta());
        assertEquals(102L, row.getReferencePrice());
        assertTrue(row.isEnabled());
        assertFalse(row.getProductKey().isEmpty());
        assertEquals("102 STARCOIN", row.getMarketSummary().getLatestTrade());
        assertEquals("+2.0%", row.getMarketSummary().getDayChange());
        assertEquals(2, row.getMarketSummary().getPricePoints().size());
    }
}
