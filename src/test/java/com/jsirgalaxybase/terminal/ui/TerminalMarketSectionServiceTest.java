package com.jsirgalaxybase.terminal.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.jsirgalaxybase.terminal.TerminalMarketActionPayload;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogEntry;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogPage;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;

public class TerminalMarketSectionServiceTest {

    private static final String PRODUCT_KEY = "gregtech:steel_ingot:0";
    private static final String[] EMPTY = new String[0];

    @Test
    public void standardizedDraftDefaultsFillBuyActionsFromVisibleOrderBook() {
        TerminalMarketActionPayload draft = TerminalMarketSectionService.applyStandardizedDraftDefaults(
            new TerminalMarketActionPayload(PRODUCT_KEY, "", "", "", "", "", "", "", ""),
            snapshot("0", new String[] { "104" }, new String[] { "100" }, "64", "32", "102 STARCOIN"));

        assertEquals(PRODUCT_KEY, draft.getSelectedProductKey());
        assertEquals("104", draft.getLimitBuyPriceText());
        assertEquals("1", draft.getLimitBuyQuantityText());
        assertEquals("1", draft.getInstantBuyQuantityText());
        assertEquals("100", draft.getLimitSellPriceText());
        assertEquals("", draft.getLimitSellQuantityText());
        assertEquals("", draft.getInstantSellQuantityText());
    }

    @Test
    public void standardizedDraftDefaultsFillSellActionsOnlyWhenPlayerHasAvailableStock() {
        TerminalMarketActionPayload draft = TerminalMarketSectionService.applyStandardizedDraftDefaults(
            new TerminalMarketActionPayload(PRODUCT_KEY, "", "", "", "", "", "", "", ""),
            snapshot("5", new String[] { "104" }, new String[] { "100" }, "64", "32", "102 STARCOIN"));

        assertEquals("100", draft.getLimitSellPriceText());
        assertEquals("1", draft.getLimitSellQuantityText());
        assertEquals("1", draft.getInstantSellQuantityText());
    }

    @Test
    public void standardizedDraftDefaultsPreserveExplicitPlayerInput() {
        TerminalMarketActionPayload draft = TerminalMarketSectionService.applyStandardizedDraftDefaults(
            new TerminalMarketActionPayload(PRODUCT_KEY, "77", "3", "", "", "88", "4", "5", "6"),
            snapshot("5", new String[] { "104" }, new String[] { "100" }, "64", "32", "102 STARCOIN"));

        assertEquals("77", draft.getLimitBuyPriceText());
        assertEquals("3", draft.getLimitBuyQuantityText());
        assertEquals("88", draft.getLimitSellPriceText());
        assertEquals("4", draft.getLimitSellQuantityText());
        assertEquals("5", draft.getInstantBuyQuantityText());
        assertEquals("6", draft.getInstantSellQuantityText());
    }

    @Test
    public void emptySelectionStillCarriesTheCurrentFormalCatalogPage() {
        StandardizedMarketCatalogEntry entry = new StandardizedMarketCatalogEntry(
            new StandardizedMarketProduct("minecraft:iron_ingot", 0), "metal", "iron", "iron-ingot");
        StandardizedMarketCatalogPage page = new StandardizedMarketCatalogPage("", 0, 16, 1,
            Arrays.asList(entry));

        TerminalMarketSnapshot snapshot = TerminalMarketService.attachCatalogBrowserData(
            snapshot("0", EMPTY, EMPTY, "0", "0", "--"), page, Collections.<String,
                TerminalMarketSnapshot.CatalogMarketSummary>emptyMap());

        assertEquals(1, snapshot.catalogPage.getTotalEntries());
        assertEquals(1, snapshot.catalogPage.getEntries().size());
        assertFalse(snapshot.catalogPage.getEntries().isEmpty());
    }

    private static TerminalMarketSnapshot snapshot(String sourceAvailable, String[] askPrices, String[] bidPrices,
        String bestBidQuantity, String bestAskQuantity, String latestTradePrice) {
        return new TerminalMarketSnapshot(
            "online",
            "hint",
            new String[] { PRODUCT_KEY },
            new String[] { "Steel Ingot" },
            PRODUCT_KEY,
            "Steel Ingot",
            "unit",
            latestTradePrice,
            "100 STARCOIN",
            "104 STARCOIN",
            bestBidQuantity,
            bestAskQuantity,
            "448",
            "44800 STARCOIN",
            "notice",
            EMPTY,
            askPrices,
            EMPTY,
            bidPrices,
            "",
            "",
            "",
            "",
            "catalog",
            sourceAvailable,
            "0",
            "0",
            "0 STARCOIN",
            "settlement",
            EMPTY,
            EMPTY,
            EMPTY,
            EMPTY,
            EMPTY,
            EMPTY,
            "exchange online",
            "held",
            "input",
            "pair",
            "inputAsset",
            "outputAsset",
            "v1",
            "OK",
            "",
            "notes",
            "1",
            "1",
            "1",
            "1",
            "ready",
            "rate",
            "hint",
            "0");
    }
}
