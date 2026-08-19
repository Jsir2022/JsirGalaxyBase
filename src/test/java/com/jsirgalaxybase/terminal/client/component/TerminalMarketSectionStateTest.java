package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;
import com.jsirgalaxybase.terminal.TerminalMarketActionPayload;

public class TerminalMarketSectionStateTest {

    @Test
    public void standardizedDraftCompletenessRequiresSelectedProductAndPositiveNumbers() {
        TerminalMarketSectionState state = new TerminalMarketSectionState();

        assertFalse(state.hasCompleteLimitBuyDraft());
        assertFalse(state.hasCompleteLimitSellDraft());
        assertFalse(state.hasCompleteInstantBuyDraft());
        assertFalse(state.hasCompleteInstantSellDraft());

        state.setSelectedProductKey("minecraft:stone:0");
        state.setLimitBuyPriceText("12");
        state.setLimitBuyQuantityText("16");
        state.setLimitSellPriceText("13");
        state.setLimitSellQuantityText("8");
        state.setInstantBuyQuantityText("5");
        state.setInstantSellQuantityText("6");

        assertTrue(state.hasCompleteLimitBuyDraft());
        assertTrue(state.hasCompleteLimitSellDraft());
        assertTrue(state.hasCompleteInstantBuyDraft());
        assertTrue(state.hasCompleteInstantSellDraft());
    }

    @Test
    public void standardizedActionHintsExplainMissingInputs() {
        TerminalMarketSectionModel model = marketModelWithCatalog();
        TerminalMarketSectionState state = new TerminalMarketSectionState();

        assertTrue(TerminalMarketSectionContent.limitBuyActionHint(model, state).contains("先在左侧选择商品"));

        state.setSelectedProductKey("minecraft:stone:0");
        assertTrue(TerminalMarketSectionContent.limitBuyActionHint(model, state).contains("填写价格与数量"));
        assertTrue(TerminalMarketSectionContent.limitSellActionHint(model, state).contains("填写价格与数量"));
        assertTrue(TerminalMarketSectionContent.instantBuyActionHint(model, state).contains("填写数量"));
        assertTrue(TerminalMarketSectionContent.instantSellActionHint(model, state).contains("填写数量"));
    }

    @Test
    public void browserQueryResetsPageAndFlowsIntoMarketPayload() {
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        state.setBrowserPage(3);
        state.setBrowserQuery("steel");

        assertEquals(0, state.getBrowserPage());
        assertEquals("steel", state.toPayload().getBrowserQuery());
        assertEquals(0, state.toPayload().getBrowserPage());
    }

    @Test
    public void localizedBrowserTextCanSubmitStableProductKeyWithoutChangingVisibleText() {
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        state.setBrowserQuery("钢锭");
        state.submitBrowserQuery("gregtech:steel_ingot:0");

        assertEquals("钢锭", state.getBrowserQuery());
        assertEquals("gregtech:steel_ingot:0", state.toPayload().getBrowserQuery());

        TerminalMarketSectionModel response = marketModelWithCatalog().withCatalogPage(
            marketModelWithCatalog().getCatalogProducts(), "gregtech:steel_ingot:0", 0, 16, 1, false, false);
        assertTrue(state.acceptsModel(response));
        state.applyModel(response);

        assertEquals("钢锭", state.getBrowserQuery());
        assertEquals("gregtech:steel_ingot:0", state.getBrowserRequestQuery());
    }

    @Test
    public void chartRangeFlowsIntoBrowseAndOrderPayloads() {
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        state.setSelectedChartRange("7d");

        assertEquals("7d", state.toBrowsePayload().getChartRange());
        assertEquals("7d", state.toUnifiedOrderPayload().getChartRange());

        state.setSelectedChartRange("unsupported");
        assertEquals("24h", state.getSelectedChartRange());
    }

    @Test
    public void returnFromDetailRetainsBrowserContext() {
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        state.setBrowserQuery("steel");
        state.setBrowserPage(2);
        state.setBrowserGridScrollOffset(74);
        state.requestDetailProduct("gregtech:steel_ingot:0");

        state.setSelectedProductKey("gregtech:steel_ingot:0");
        state.requestDetailProduct("gregtech:steel_ingot:0");
        TerminalMarketSectionModel selected = selectedModel();
        selected.withCatalogPage(selected.getCatalogProducts(), "steel", 2, 16, 48, true, false);
        state.applyModel(selected);
        assertTrue(state.isStandardizedDetailView());

        state.returnToStandardizedBrowse();
        assertFalse(state.isStandardizedDetailView());
        assertEquals("steel", state.getBrowserQuery());
        assertEquals(2, state.getBrowserPage());
        assertEquals(74, state.getBrowserGridScrollOffset());
    }

    @Test
    public void lateStandardizedSnapshotsCannotReplaceNewerBrowseOrDetailContext() {
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        state.setBrowserQuery("steel");
        state.setBrowserPage(2);

        TerminalMarketSectionModel staleBrowse = marketModelWithCatalog().withCatalogPage(
            marketModelWithCatalog().getCatalogProducts(), "iron", 1, 16, 48, true, true);
        TerminalMarketSectionModel currentBrowse = marketModelWithCatalog().withCatalogPage(
            marketModelWithCatalog().getCatalogProducts(), "steel", 2, 16, 48, true, true);
        assertFalse(state.acceptsModel(staleBrowse));
        assertTrue(state.acceptsModel(currentBrowse));

        state.requestDetailProduct("gregtech:steel_ingot:0");
        assertFalse(state.acceptsModel(selectedModelFor("gregtech:iron_ingot:0")));
        TerminalMarketSectionModel steel = selectedModelFor("gregtech:steel_ingot:0");
        assertTrue(state.acceptsModel(steel));
        state.applyModel(steel);
        assertTrue(state.isStandardizedDetailView());
        assertFalse(state.acceptsModel(selectedModelFor("gregtech:iron_ingot:0")));
    }

    @Test
    public void historyFiltersResetPagingAndReturnToCurrentProductDetail() {
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        state.setSelectedProductKey("gregtech:steel_ingot:0");
        state.openStandardizedHistory();

        assertTrue(state.isStandardizedHistoryView());
        assertEquals("商品: 全部", state.getHistoryProductScopeLabel());
        assertEquals("方向: 全部", state.getHistorySideLabel());
        assertEquals("状态: 全部", state.getHistoryStatusLabel());
        assertEquals("时间: 全部", state.getHistoryTimeLabel());

        state.setHistoryPage(4);
        state.toggleHistoryProductScope();
        assertEquals(0, state.getHistoryPage());
        assertEquals("商品: 本商品", state.getHistoryProductScopeLabel());

        state.setHistoryPage(2);
        state.cycleHistorySide();
        assertEquals(0, state.getHistoryPage());
        assertEquals("方向: 买入", state.getHistorySideLabel());

        state.returnToStandardizedDetail();
        assertTrue(state.isStandardizedDetailView());
        assertEquals("gregtech:steel_ingot:0", state.getSelectedProductKey());
    }

    @Test
    public void historyPayloadCarriesServerSideFiltersAndPaging() {
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        state.setSelectedProductKey("gregtech:steel_ingot:0");
        state.openStandardizedHistory();
        state.toggleHistoryProductScope();
        state.cycleHistorySide();
        state.cycleHistoryStatus();
        state.cycleHistoryTime();
        state.setHistoryQuery("steel");
        state.setHistoryPage(2);

        TerminalMarketActionPayload payload = state.toHistoryPayload();
        assertEquals("CURRENT", payload.getHistoryProductScope());
        assertEquals("BUY", payload.getHistorySide());
        assertEquals("OPEN", payload.getHistoryStatus());
        assertEquals("DAY", payload.getHistoryTime());
        assertEquals("steel", payload.getHistoryQuery());
        assertEquals(2, payload.getHistoryPage());
        assertEquals(TerminalMarketActionPayload.DEFAULT_HISTORY_PAGE_SIZE, payload.getHistoryPageSize());

        state.resetHistoryFilters();
        assertEquals("", state.getHistoryQuery());
        assertEquals("ALL", state.toHistoryPayload().getHistorySide());
        assertEquals(0, state.getHistoryPage());
    }

    @Test
    public void historyPaginationUsesAllFourVisibleRowsWithoutDroppingEntries() {
        TerminalMarketSectionModel model = selectedModel().withHistoryPage(
            Arrays.asList("order-1", "order-2", "order-3", "order-4"),
            Arrays.asList("1", "2", "3", "4"),
            Arrays.asList("false", "true", "false", "true"),
            11,
            0,
            TerminalMarketActionPayload.DEFAULT_HISTORY_PAGE_SIZE);

        assertEquals(4, model.getHistoryPageSize());
        assertEquals(3, model.getHistoryTotalPages());
        assertFalse(model.hasHistoryPreviousPage());
        assertTrue(model.hasHistoryNextPage());

        model.withHistoryPage(
            Arrays.asList("order-9", "order-10", "order-11"),
            Arrays.asList("9", "10", "11"),
            Arrays.asList("false", "false", "false"),
            11,
            2,
            TerminalMarketActionPayload.DEFAULT_HISTORY_PAGE_SIZE);
        assertTrue(model.hasHistoryPreviousPage());
        assertFalse(model.hasHistoryNextPage());
    }

    private static TerminalMarketSectionModel selectedModel() {
        return selectedModelFor("gregtech:steel_ingot:0");
    }

    private static TerminalMarketSectionModel selectedModelFor(String productKey) {
        return new TerminalMarketSectionModel(
            "market_standardized", "市场服务在线", "", Arrays.asList(productKey),
            Arrays.asList("Selected Ingot"), productKey, "Selected Ingot", "可堆叠单位", "102", "100",
            "104", "64", "64", "448", "45696 STARCOIN", "0", "0", "0", "0 STARCOIN", "", "", "",
            "", "", "", "", Arrays.asList("104 x64"), Arrays.asList("100 x64"), Arrays.asList(""),
            Arrays.asList(""), Arrays.asList(""), Arrays.asList(""), Arrays.asList(""), Arrays.asList(""), false,
            TerminalMarketSectionModel.LimitBuyDraftModel.placeholder(),
            TerminalMarketSectionModel.LimitSellDraftModel.placeholder(),
            TerminalMarketSectionModel.InstantDraftModel.placeholder(),
            TerminalMarketSectionModel.InstantDraftModel.placeholder(),
            TerminalMarketSectionModel.ActionFeedbackModel.placeholder());
    }

    private static TerminalMarketSectionModel marketModelWithCatalog() {
        return new TerminalMarketSectionModel(
            "market_standardized",
            "市场服务在线",
            "请选择商品",
            Arrays.asList("minecraft:stone:0"),
            Arrays.asList("Stone"),
            "",
            "未选中商品",
            "标准化单位",
            "--",
            "--",
            "--",
            "0",
            "0",
            "0",
            "0 STARCOIN",
            "0",
            "0",
            "0",
            "0 STARCOIN",
            "摘要",
            "来源",
            "仓储",
            "填写价格与数量后，将显示冻结资金摘要。",
            "填写价格与数量后，将显示 AVAILABLE 仓储卖出摘要。",
            "填写数量后，将按当前卖盘测深。",
            "填写数量后，将按当前买盘测深。",
            Arrays.asList("当前没有卖盘深度。"),
            Arrays.asList("当前没有买盘深度。"),
            Arrays.asList("当前没有个人订单。"),
            Arrays.asList(""),
            Arrays.asList("0"),
            Arrays.asList("当前没有待提取的 CLAIMABLE 资产。"),
            Arrays.asList(""),
            Arrays.asList("当前没有规则提示。"),
            false,
            TerminalMarketSectionModel.LimitBuyDraftModel.placeholder(),
            TerminalMarketSectionModel.LimitSellDraftModel.placeholder(),
            TerminalMarketSectionModel.InstantDraftModel.placeholder(),
            TerminalMarketSectionModel.InstantDraftModel.placeholder(),
            TerminalMarketSectionModel.ActionFeedbackModel.placeholder());
    }
}
