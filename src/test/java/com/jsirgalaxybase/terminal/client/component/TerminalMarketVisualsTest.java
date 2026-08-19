package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel.PricePointModel;

public class TerminalMarketVisualsTest {

    @Test
    public void localizedNameFallsBackToCatalogTextWhenItemCannotResolve() {
        assertEquals("目录名称",
            TerminalMarketVisuals.resolveLocalizedItemName("missing:item:0", " 目录名称 "));
        assertEquals("--", TerminalMarketVisuals.resolveLocalizedItemName("missing:item:0", null));
    }

    @Test
    public void structuredItemReferenceKeepsRegistryNamespaceSeparateFromMetadata() {
        assertEquals("gregtech:gt.metaitem.01@11305",
            TerminalMarketVisuals.itemRef("gregtech:gt.metaitem.01", 11305));
        assertEquals("minecraft:iron_ingot@0", TerminalMarketVisuals.itemRef("minecraft:iron_ingot", 0));
    }

    @Test
    public void latestTradeColumnExcludesSyntheticChartPoints() {
        List<PricePointModel> points = Arrays.asList(
            new PricePointModel(63L, 2L, 100L),
            new PricePointModel(63L, 63L, 63L, 63L, 0L, 0L, 200L, "CARRY_FORWARD"),
            new PricePointModel(64L, 64L, 64L, 64L, 0L, 0L, 300L, "REFERENCE"),
            new PricePointModel(65L, 3L, 400L),
            new PricePointModel(0L, 0L, 0L, 0L, 0L, 0L, 500L, "EMPTY"));

        List<PricePointModel> trades = TerminalMarketSection.latestRealTrades(points, 5);

        assertEquals(2, trades.size());
        assertEquals(65L, trades.get(0).getPrice());
        assertEquals(3L, trades.get(0).getQuantity());
        assertEquals(63L, trades.get(1).getPrice());
        assertEquals(1, TerminalMarketSection.latestRealTrades(points, 1).size());
    }
}
