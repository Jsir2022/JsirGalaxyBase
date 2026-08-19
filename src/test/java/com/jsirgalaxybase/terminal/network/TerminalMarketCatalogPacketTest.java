package com.jsirgalaxybase.terminal.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.jsirgalaxybase.terminal.TerminalMarketBrowseEntry;
import com.jsirgalaxybase.terminal.TerminalMarketAccountCenterRow;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalCustomMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalExchangeMarketSectionModel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TerminalMarketCatalogPacketTest {

    @Test
    public void terminalActionSequenceRoundTripsAndLegacyPacketDefaultsToZero() {
        TerminalActionMessage source = new TerminalActionMessage("session", "market", "refresh", "{}", 42L);
        ByteBuf current = Unpooled.buffer();
        source.toBytes(current);
        TerminalActionMessage decoded = new TerminalActionMessage();
        decoded.fromBytes(current.copy());
        assertEquals(42L, decoded.getRequestSequence());

        ByteBuf legacy = current.copy(0, current.writerIndex() - 8);
        TerminalActionMessage legacyDecoded = new TerminalActionMessage();
        legacyDecoded.fromBytes(legacy);
        assertEquals(0L, legacyDecoded.getRequestSequence());
    }

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
                    Arrays.asList(new TerminalMarketSectionModel.PricePointModel(98L, 103L, 97L, 99L,
                            8L, 792L, 100L, "CARRY_FORWARD"),
                        new TerminalMarketSectionModel.PricePointModel(102L, 4L, 101L))))), "steel", 1, 8, 17, true, true)
            .withHistoryPage(Arrays.asList("#42 | Steel Ingot | SELL | OPEN"), Arrays.asList("42"),
                Arrays.asList("1"), 19, 2, 7)
            .withAccountCenter("OPEN_ORDERS", "11638", "2158", 1248, 4096, 8, 3, 1,
                Arrays.asList("OPEN_ORDER"), Arrays.asList("minecraft:iron_ingot@0"))
            .withAccountCenterRows(Arrays.asList(new TerminalMarketAccountCenterRow("42", "OPEN_ORDER",
                "minecraft:iron_ingot", 0, "BUY", "LIMIT", 1232L, 4500L, 1500L, 3000L,
                "PARTIALLY_FILLED", "2026-08-16T07:28:41Z", 42L, 1848000L, 0L, "", true,
                1776497321L, 3696000L)));

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
        TerminalMarketSectionModel.PricePointModel candle = row.getMarketSummary().getPricePoints().get(0);
        assertEquals(98L, candle.getOpen());
        assertEquals(103L, candle.getHigh());
        assertEquals(97L, candle.getLow());
        assertEquals(99L, candle.getPrice());
        assertEquals(8L, candle.getQuantity());
        assertEquals(792L, candle.getTurnover());
        assertEquals("CARRY_FORWARD", candle.getSource());
        assertEquals(19, decoded.getHistoryTotalEntries());
        assertEquals(2, decoded.getHistoryPageIndex());
        assertEquals(7, decoded.getHistoryPageSize());
        assertEquals("42", decoded.getMyOrderIds().get(0));
        assertEquals("1", decoded.getMyOrderCancelableFlags().get(0));
        assertEquals("OPEN_ORDERS", decoded.getAccountCenterTab());
        assertEquals("11638", decoded.getCenterBankAvailable());
        assertEquals(1248, decoded.getCenterVaultUsedSlots());
        assertEquals("OPEN_ORDER", decoded.getCenterRowKinds().get(0));
        assertEquals("minecraft:iron_ingot", decoded.getAccountCenterRows().get(0).getRegistryName());
        assertEquals(3000L, decoded.getAccountCenterRows().get(0).getRemainingQuantity());
    }

    @Test
    public void customAndExchangeBrowseRowsSurviveTerminalPacketRoundTrip() {
        TerminalMarketBrowseEntry entry = new TerminalMarketBrowseEntry("dreamcraft:item.CoinChemistII",
            "dreamcraft:item.CoinChemistII", "化学家币 $100", "Chemist / II", "100", "个人仓兑换");

        ByteBuf customBuffer = Unpooled.buffer();
        OpenTerminalApprovedMessage.writeCustomMarketSection(customBuffer,
            TerminalCustomMarketSectionModel.placeholder().withBrowsePage(Arrays.asList(entry), "chem", 1, 12, 13,
                true, true));
        TerminalCustomMarketSectionModel custom = OpenTerminalApprovedMessage.readCustomMarketSection(customBuffer);
        assertEquals("chem", custom.getBrowseQuery());
        assertEquals(1, custom.getBrowsePageIndex());
        assertEquals(13, custom.getBrowseTotalEntries());
        assertEquals("化学家币 $100", custom.getBrowseEntries().get(0).getTitle());

        ByteBuf exchangeBuffer = Unpooled.buffer();
        OpenTerminalApprovedMessage.writeExchangeMarketSection(exchangeBuffer,
            exchangeModel("dreamcraft:item.CoinChemistII").withBrowsePage(Arrays.asList(entry), "chemist", 2, 12,
                75, true, true));
        TerminalExchangeMarketSectionModel exchange = OpenTerminalApprovedMessage.readExchangeMarketSection(exchangeBuffer);
        assertEquals("chemist", exchange.getBrowseQuery());
        assertEquals(2, exchange.getBrowsePageIndex());
        assertEquals(75, exchange.getBrowseTotalEntries());
        assertEquals("dreamcraft:item.CoinChemistII", exchange.getSelectedCoinCode());
        assertEquals("dreamcraft:item.CoinChemistII", exchange.getBrowseEntries().get(0).getKey());
    }

    private static TerminalExchangeMarketSectionModel exchangeModel(String selectedCoinCode) {
        return new TerminalExchangeMarketSectionModel("在线", "", Arrays.asList("TASK_COIN"),
            Arrays.asList("任务书硬币"), "TASK_COIN", selectedCoinCode, "任务书硬币", "", "", "", "--", "--",
            "--", "UNAVAILABLE", "--", "", "0", "0", "0", "0", "", "--", "", "", false, null);
    }
}
