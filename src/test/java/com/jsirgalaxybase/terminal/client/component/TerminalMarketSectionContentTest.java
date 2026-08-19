package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.viewmodel.TerminalCustomMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalExchangeMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;
import com.jsirgalaxybase.terminal.TerminalMarketAccountCenterRow;

public class TerminalMarketSectionContentTest {

    @Test
    public void accountCenterRowsUseStructuredItemAndOrderFieldsInsteadOfLegacyDisplayLines() {
        TerminalMarketSectionModel model = TerminalMarketSectionModel.placeholder("market_account_center")
            .withHistoryPage(Arrays.asList("not a parseable legacy row"), Arrays.asList("42"), Arrays.asList("1"),
                1, 0, 4)
            .withAccountCenter("OPEN_ORDERS", "100", "20", 1, 27, 1, 0, 0,
                Arrays.asList("OPEN_ORDER"), Arrays.asList("minecraft:iron_ingot@0"))
            .withAccountCenterRows(Arrays.asList(new TerminalMarketAccountCenterRow("42", "OPEN_ORDER",
                "minecraft:iron_ingot", 0, "BUY", "LIMIT", 12L, 10L, 3L, 7L,
                "PARTIALLY_FILLED", "2026-08-16T00:00:00Z", 42L, 36L, 0L, "", true, 100L, 84L)));

        TerminalMarketSectionContent.OrderEntry row =
            TerminalMarketSectionContent.buildAccountCenterEntries(model).get(0);
        assertEquals("minecraft:iron_ingot:0", row.getProductKey());
        assertEquals("3/10 30%", row.getFillProgressLabel());
        assertEquals(100L, row.getUpdatedAtEpochSeconds());
        assertTrue(row.isCancelable());
    }

    @Test
    public void accountCenterStructuredRowsKeepExactGroupedValuesAndSafeProgress() {
        TerminalMarketSectionModel model = TerminalMarketSectionModel.placeholder("market_account_center")
            .withHistoryPage(Arrays.asList("structured"), Arrays.asList("99"), Arrays.asList("1"), 1, 0, 4)
            .withAccountCenter("OPEN_ORDERS", "1840230", "175636", 1248, 4096, 1, 0, 0,
                Arrays.asList("OPEN_ORDER"), Arrays.asList("minecraft:iron_ingot@0"))
            .withAccountCenterRows(Arrays.asList(new TerminalMarketAccountCenterRow("99", "OPEN_ORDER",
                "minecraft:iron_ingot", 0, "BUY", "LIMIT", 1200L, Long.MAX_VALUE,
                Long.MAX_VALUE - 1L, 1L, "PARTIALLY_FILLED", "2026-08-19T00:00:00Z", 99L,
                0L, 0L, "", true, 100L, Long.MAX_VALUE)));

        TerminalMarketSectionContent.OrderEntry row =
            TerminalMarketSectionContent.buildAccountCenterEntries(model).get(0);
        assertEquals("1,200", row.getUnitPrice());
        assertEquals("9,223,372,036,854,775,807", row.getOriginalQuantity());
        assertEquals("9,223,372,036,854,775,806/9,223,372,036,854,775,807 99%",
            row.getFillProgressLabel());
        assertEquals("1", row.getRemainingQuantity());
        assertTrue(row.isCancelable());
    }

    @Test
    public void personalHistoryEntriesParseFilterAndExposeExactCancellationTarget() {
        String now = java.time.Instant.now().toString();
        TerminalMarketSectionContent.OrderEntry open = new TerminalMarketSectionContent.OrderEntry(
            "42", "#42 | gregtech:steel_ingot:0 | SELL | 价 65 | 总 10 | 成 3 | 剩 7 | PARTIALLY_FILLED | "
                + now + " | 钢锭",
            true);
        TerminalMarketSectionContent.OrderEntry oldFilled = new TerminalMarketSectionContent.OrderEntry(
            "41", "#41 | gregtech:iron_ingot:0 | BUY | 价 63 | 总 8 | 成 8 | 剩 0 | FILLED | 2020-01-01T00:00:00Z",
            false);
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        state.setSelectedProductKey("gregtech:steel_ingot:0");
        state.openStandardizedHistory();

        assertEquals("gregtech:steel_ingot:0", open.getProductKey());
        assertEquals("SELL", open.getSide());
        assertEquals("PARTIALLY_FILLED", open.getStatus());
        assertEquals("钢锭", open.getDisplayName());
        assertEquals("65", open.getUnitPrice());
        assertEquals("3", open.getFilledQuantity());
        assertEquals("10", open.getOriginalQuantity());
        assertEquals("7", open.getRemainingQuantity());
        assertEquals("部分成交", open.getStatusLabel());
        assertEquals("进行中·部分成交", open.getStatusMarkerLabel());
        assertEquals("3/10 30%", open.getFillProgressLabel());
        assertTrue(open.isCancelable());
        assertFalse(oldFilled.isCancelable());
        assertEquals("完成·已成交", oldFilled.getStatusMarkerLabel());
        assertTrue(open.matches(state, state.getSelectedProductKey()));
        assertTrue(oldFilled.matches(state, state.getSelectedProductKey()));

        state.toggleHistoryProductScope();
        assertTrue(open.matches(state, state.getSelectedProductKey()));
        assertFalse(oldFilled.matches(state, state.getSelectedProductKey()));

        state.cycleHistorySide();
        state.cycleHistorySide();
        assertTrue(open.matches(state, state.getSelectedProductKey()));

        state.cycleHistoryTime();
        assertTrue(open.matches(state, state.getSelectedProductKey()));
        assertFalse(oldFilled.matches(state, "gregtech:iron_ingot:0"));
    }

    @Test
    public void productClaimRuleAndBookBuildersKeepFullDatasets() {
        TerminalMarketSectionModel model = new TerminalMarketSectionModel(
            "market_standardized",
            "市场服务在线",
            "请选择商品",
            Arrays.asList("p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9"),
            Arrays.asList("商品1", "商品2", "商品3", "商品4", "商品5", "商品6", "商品7", "商品8", "商品9"),
            "p3",
            "商品3",
            "组",
            "12 STARCOIN",
            "11 STARCOIN",
            "13 STARCOIN",
            "12",
            "16",
            "64",
            "768 STARCOIN",
            "32",
            "8",
            "5",
            "144 STARCOIN",
            "完整数据测试",
            "目录版本=default | 来源=runtime | 卖出来源=统一仓储 AVAILABLE",
            "当前 AVAILABLE=32，可直接卖出。",
            "冻结预计 204 STARCOIN。",
            "将锁定 AVAILABLE 数量。",
            "预计按当前卖盘成交。",
            "预计按当前买盘成交。",
            Arrays.asList("13 x 1", "14 x 2", "15 x 3", "16 x 4", "17 x 5", "18 x 6"),
            Arrays.asList("11 x 1", "10 x 2", "9 x 3", "8 x 4", "7 x 5", "6 x 6", "5 x 7"),
            Arrays.asList("订单1", "订单2", "订单3", "订单4"),
            Arrays.asList("11", "12", "13", "14"),
            Arrays.asList("1", "1", "0", "1"),
            Arrays.asList("claim1", "claim2", "claim3", "claim4", "claim5"),
            Arrays.asList("1", "2", "3", "4", "5"),
            Arrays.asList("规则1", "规则2", "规则3", "规则4", "规则5", "规则6"),
            true,
            new TerminalMarketSectionModel.LimitBuyDraftModel("p3", "12", "16", true),
            new TerminalMarketSectionModel.LimitSellDraftModel("p3", "13", "8", true),
            new TerminalMarketSectionModel.InstantDraftModel("p3", "5", true),
            new TerminalMarketSectionModel.InstantDraftModel("p3", "6", true),
            new TerminalMarketSectionModel.ActionFeedbackModel("反馈", "等待提交", "INFO"));

        assertEquals(9, TerminalMarketSectionContent.buildProductEntries(model).size());
        assertEquals(5, TerminalMarketSectionContent.buildClaimEntries(model).size());
        assertEquals(4, TerminalMarketSectionContent.buildOrderEntries(model).size());
        assertEquals(6, TerminalMarketSectionContent.buildRuleLines(model).size());
        assertEquals(11, TerminalMarketSectionContent.buildBookLines(model).size());
        assertTrue(TerminalMarketSectionContent.buildBookLines(model).get(10).contains("订单4"));
    }

    @Test
    public void overviewSummariesStayWorkflowFirst() {
        TerminalMarketSectionModel model = new TerminalMarketSectionModel(
            "market",
            "市场服务在线",
            "市场根页只做入口",
            Arrays.asList("p1", "p2", "p3"),
            Arrays.asList("商品1", "商品2", "商品3"),
            "",
            "未选中商品",
            "组",
            "12 STARCOIN",
            "11 STARCOIN",
            "13 STARCOIN",
            "12",
            "16",
            "64",
            "768 STARCOIN",
            "32",
            "8",
            "5",
            "144 STARCOIN",
            "完整数据测试",
            "目录版本=default | 来源=runtime | 卖出来源=统一仓储 AVAILABLE",
            "当前 AVAILABLE=32，可直接卖出。",
            "冻结预计 204 STARCOIN。",
            "将锁定 AVAILABLE 数量。",
            "预计按当前卖盘成交。",
            "预计按当前买盘成交。",
            Arrays.asList("13 x 1"),
            Arrays.asList("11 x 1"),
            Arrays.asList("订单1"),
            Arrays.asList("11"),
            Arrays.asList("1"),
            Arrays.asList("claim1"),
            Arrays.asList("1"),
            Arrays.asList("规则1"),
            true,
            new TerminalMarketSectionModel.LimitBuyDraftModel("p3", "12", "16", true),
            new TerminalMarketSectionModel.LimitSellDraftModel("p3", "13", "8", true),
            new TerminalMarketSectionModel.InstantDraftModel("p3", "5", true),
            new TerminalMarketSectionModel.InstantDraftModel("p3", "6", true),
            new TerminalMarketSectionModel.ActionFeedbackModel("反馈", "等待提交", "INFO"));

        assertEquals("3", TerminalMarketSectionContent.countActiveProducts(model));
        assertTrue(TerminalMarketSectionContent.buildOverviewSummaryLines(model).get(0).contains("3"));
        assertEquals("进入标准市场",
            TerminalMarketSectionContent.buildStandardizedOverviewEntry(model).getActionLabel());
        assertTrue(TerminalMarketSectionContent.buildCustomOverviewEntry().getSummary().contains("挂牌"));
        assertTrue(TerminalMarketSectionContent.buildExchangeOverviewEntry().getSummary().contains("报价"));
        assertTrue(TerminalMarketSectionContent.buildBrowserStatusLines(model).get(0).contains("服务"));
        assertTrue(TerminalMarketSectionContent.buildMarketSnapshotLines(model).get(0).contains("最新成交价"));
        assertTrue(TerminalMarketSectionContent.buildInventoryStatusLines(model).get(0).contains("可售"));
    }

    @Test
    public void productEntriesAndActionHintsExposeWorkbenchSummaries() {
        TerminalMarketSectionModel model = new TerminalMarketSectionModel(
            "market_standardized",
            "市场服务在线",
            "请选择商品",
            Arrays.asList("minecraft:stone:0", "minecraft:iron_ingot:0"),
            Arrays.asList("石头 | runtime 商品目录", "铁锭"),
            "minecraft:stone:0",
            "石头",
            "组",
            "12 STARCOIN",
            "11 STARCOIN",
            "13 STARCOIN",
            "12",
            "16",
            "64",
            "768 STARCOIN",
            "32",
            "8",
            "5",
            "144 STARCOIN",
            "完整数据测试",
            "目录版本=default | 来源=runtime | 卖出来源=统一仓储 AVAILABLE",
            "当前 AVAILABLE=32，可直接卖出。",
            "冻结预计 204 STARCOIN。",
            "将锁定 AVAILABLE 数量。",
            "预计按当前卖盘成交。",
            "预计按当前买盘成交。",
            Arrays.asList("13 x 1"),
            Arrays.asList("11 x 1"),
            Arrays.asList("订单1"),
            Arrays.asList("11"),
            Arrays.asList("1"),
            Arrays.asList("claim1"),
            Arrays.asList("1"),
            Arrays.asList("规则1"),
            true,
            new TerminalMarketSectionModel.LimitBuyDraftModel("minecraft:stone:0", "12", "16", true),
            new TerminalMarketSectionModel.LimitSellDraftModel("minecraft:stone:0", "13", "8", true),
            new TerminalMarketSectionModel.InstantDraftModel("minecraft:stone:0", "5", true),
            new TerminalMarketSectionModel.InstantDraftModel("minecraft:stone:0", "6", true),
            new TerminalMarketSectionModel.ActionFeedbackModel("市场动作反馈", "等待确认提交。", "INFO"));
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        state.applyModel(model);

        TerminalMarketSectionContent.ProductEntry selected = TerminalMarketSectionContent.buildProductEntries(model).get(0);
        TerminalMarketSectionContent.ProductEntry fallback = TerminalMarketSectionContent.buildProductEntries(model).get(1);

        assertEquals("石头", selected.getTitle());
        assertEquals("runtime 商品目录", selected.getSubtitle());
        assertEquals("已选中", selected.getStateLabel());
        assertEquals("minecraft:iron_ingot:0", fallback.getSubtitle());
        assertEquals("可交易", fallback.getStateLabel());
        assertEquals("目录: 统一仓储", TerminalMarketSectionContent.buildBrowserStatusLines(model).get(1));
        assertEquals("个人仓: 可直接卖出", TerminalMarketSectionContent.buildBrowserStatusLines(model).get(2));
        assertEquals("INFO / 市场动作反馈", TerminalMarketSectionContent.latestFeedbackLine(model));
        assertTrue(TerminalMarketSectionContent.limitBuyActionHint(model, state).contains("冻结预计"));
        assertTrue(TerminalMarketSectionContent.instantSellActionHint(model, state).contains("当前买盘"));
    }

    @Test
    public void structuredCatalogRowsPreferFormalItemMetadataOverLegacyLabels() {
        TerminalMarketSectionModel model = TerminalMarketSectionModel.placeholder("market_standardized").withCatalogPage(
            Arrays.asList(
                new TerminalMarketSectionModel.CatalogProductModel("minecraft:iron_ingot:0", "minecraft:iron_ingot", 0,
                    "Iron Ingot", "64 / stack", 10, true, 64L, "可交易"),
                new TerminalMarketSectionModel.CatalogProductModel("minecraft:gold_ingot:0", "minecraft:gold_ingot", 0,
                    "Gold Ingot", "64 / stack", 20, false, 256L, "已停用")),
            "iron", 0, 8, 1, false, false);

        List<TerminalMarketSectionContent.ProductEntry> entries = TerminalMarketSectionContent.buildProductEntries(model);

        assertEquals(1, entries.size());
        assertEquals("Iron Ingot", entries.get(0).getTitle());
        assertEquals("minecraft:iron_ingot@0", entries.get(0).getIconRef());
        assertEquals("64 / stack", entries.get(0).getSubtitle());
        assertEquals(64L, entries.get(0).getReferencePrice());
    }

    @Test
    public void emptyStandardizedCatalogExposesExplicitEmptyAndDisabledReasons() {
        TerminalMarketSectionModel model = TerminalMarketSectionModel.placeholder("market_standardized");
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        state.applyModel(model);

        assertTrue(!TerminalMarketSectionContent.hasProductCatalog(model));
        assertEquals("暂无可浏览标准商品", TerminalMarketSectionContent.buildProductCatalogEmptyTitle(model));
        assertTrue(TerminalMarketSectionContent.buildProductCatalogEmptyReason(model).contains("当前没有市场运行态"));
        assertTrue(TerminalMarketSectionContent.limitBuyActionHint(model, state).contains("没有可交易标准商品"));
        assertTrue(TerminalMarketSectionContent.instantSellActionHint(model, state).contains("没有可交易标准商品"));
    }

    @Test
    public void customAndExchangeModelsCompactNetworkPaddingAndExposeDisabledReasons() {
        TerminalCustomMarketSectionModel custom = new TerminalCustomMarketSectionModel(
            "定制商品市场在线",
            "当前没有 active custom listings。",
            "全部挂牌",
            Arrays.asList("当前范围下没有可显示挂牌。", "", ""),
            Arrays.asList("", "", ""),
            Arrays.asList("", ""),
            Arrays.asList("", ""),
            Arrays.asList("", ""),
            Arrays.asList("", ""),
            "",
            "未选中挂牌",
            "--",
            "--",
            "请先选择挂牌",
            "--",
            "--",
            "先从列表选择一条挂牌。",
            false,
            false,
            false,
            TerminalCustomMarketSectionModel.ActionFeedbackModel.placeholder());
        TerminalExchangeMarketSectionModel exchange = new TerminalExchangeMarketSectionModel(
            "汇率市场在线",
            "先选择兑换标的。",
            Arrays.asList("task-coin-formal", "", ""),
            Arrays.asList("任务书硬币 -> STARCOIN | formal quote", "", ""),
            "",
            "未选择兑换标的",
            "请选择标的后查看报价。",
            "当前未检测到手持物品",
            "--",
            "--",
            "--",
            "--",
            "--",
            "UNAVAILABLE",
            "--",
            "请先把任务书硬币拿在手上，再刷新报价。",
            "0",
            "0",
            "0",
            "0",
            "当前暂无可执行报价",
            "--",
            "当前不能继续执行兑换。",
            false,
            TerminalExchangeMarketSectionModel.ActionFeedbackModel.placeholder());

        assertEquals(1, custom.getActiveListingLines().size());
        assertEquals(0, custom.getActiveListingIds().size());
        assertTrue(!custom.hasAnyListing());
        assertTrue(custom.getDisabledReason().contains("没有挂牌数据"));
        assertEquals(1, exchange.getTargetCodes().size());
        assertEquals(1, exchange.getTargetLabels().size());
        assertTrue(exchange.getDisabledReason().contains("选择兑换标的"));
    }

    @Test
    public void customAndExchangeModelsOnlyTrimTrailingNetworkPadding() {
        TerminalCustomMarketSectionModel custom = new TerminalCustomMarketSectionModel(
            "定制商品市场在线",
            "listing-first",
            "全部挂牌",
            Arrays.asList("", "#42 | 定制商品 | 99 STARCOIN | ACTIVE", ""),
            Arrays.asList("", "42", ""),
            Arrays.asList("", ""),
            Arrays.asList("", ""),
            Arrays.asList("", ""),
            Arrays.asList("", ""),
            "",
            "未选中挂牌",
            "--",
            "--",
            "请先选择挂牌",
            "--",
            "--",
            "先从列表选择一条挂牌。",
            false,
            false,
            false,
            TerminalCustomMarketSectionModel.ActionFeedbackModel.placeholder());
        TerminalExchangeMarketSectionModel exchange = new TerminalExchangeMarketSectionModel(
            "汇率市场在线",
            "quote-first",
            Arrays.asList("", "task-coin-formal", ""),
            Arrays.asList("", "任务书硬币 -> STARCOIN | formal quote", ""),
            "",
            "未选择兑换标的",
            "请选择标的后查看报价。",
            "当前未检测到手持物品",
            "--",
            "--",
            "--",
            "--",
            "--",
            "UNAVAILABLE",
            "--",
            "请先把任务书硬币拿在手上，再刷新报价。",
            "0",
            "0",
            "0",
            "0",
            "当前暂无可执行报价",
            "--",
            "当前不能继续执行兑换。",
            false,
            TerminalExchangeMarketSectionModel.ActionFeedbackModel.placeholder());

        assertEquals(2, custom.getActiveListingLines().size());
        assertEquals("", custom.getActiveListingLines().get(0));
        assertEquals("42", custom.getActiveListingIds().get(1));
        assertEquals(2, exchange.getTargetCodes().size());
        assertEquals("", exchange.getTargetCodes().get(0));
        assertEquals("task-coin-formal", exchange.getTargetCodes().get(1));
    }
}
