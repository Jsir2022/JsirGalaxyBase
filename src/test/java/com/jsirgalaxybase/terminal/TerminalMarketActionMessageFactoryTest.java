package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.component.TerminalMarketSectionState;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalCustomMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalExchangeMarketSectionModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalMarketSectionModel;
import com.jsirgalaxybase.terminal.network.TerminalActionMessage;
import com.jsirgalaxybase.terminal.ui.TerminalPage;

public class TerminalMarketActionMessageFactoryTest {

    @Test
    public void confirmLimitBuyBuildsMarketActionMessageOnNewActionSnapshotChain() {
        TerminalHomeScreenModel screenModel = createMarketScreenModel();
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        state.applyModel(screenModel.getSelectedPageSnapshot().getMarketSectionModel());

        TerminalActionMessage message = TerminalMarketActionMessageFactory.createConfirmLimitBuyMessage(
            screenModel,
            screenModel.getSelectedPageSnapshot().getMarketSectionModel(),
            state);

        assertNotNull(message);
        assertEquals("session-market-test", message.getSessionToken());
        assertEquals("market_standardized", message.getPageId());
        assertEquals(TerminalActionType.MARKET_CONFIRM_LIMIT_BUY.getId(), message.getActionType());
        assertTrue(message.getPayload().contains("bWluZWNyYWZ0OnN0b25lOjA="));
    }

    @Test
    public void claimDoesNotBuildMessageWhenNoCustodyIsSelected() {
        TerminalHomeScreenModel screenModel = createMarketScreenModel();
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        state.applyModel(screenModel.getSelectedPageSnapshot().getMarketSectionModel());
        state.setPendingClaimCustodyId("");

        TerminalActionMessage message = TerminalMarketActionMessageFactory.createClaimMessage(
            screenModel,
            screenModel.getSelectedPageSnapshot().getMarketSectionModel(),
            state);

        assertNull(message);
    }

    @Test
    public void customBuyBuildsExplicitCustomActionMessage() {
        TerminalHomeScreenModel screenModel = createCustomMarketScreenModel();
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        state.getCustomState().applyModel(screenModel.getSelectedPageSnapshot().getCustomMarketSectionModel());

        TerminalActionMessage message = TerminalMarketActionMessageFactory.createCustomListingActionMessage(
            screenModel,
            state,
            TerminalActionType.MARKET_CUSTOM_BUY_LISTING);

        assertNotNull(message);
        assertEquals("market_custom", message.getPageId());
        assertEquals(TerminalActionType.MARKET_CUSTOM_BUY_LISTING.getId(), message.getActionType());
        assertEquals(42L, TerminalCustomMarketActionPayload.decode(message.getPayload()).parseSelectedListingId());
    }

    @Test
    public void exchangeConfirmBuildsExplicitExchangeActionMessage() {
        TerminalHomeScreenModel screenModel = createExchangeMarketScreenModel();
        TerminalMarketSectionState state = new TerminalMarketSectionState();
        state.getExchangeState().applyModel(screenModel.getSelectedPageSnapshot().getExchangeMarketSectionModel());

        TerminalActionMessage message = TerminalMarketActionMessageFactory.createExchangeConfirmMessage(screenModel, state);

        assertNotNull(message);
        assertEquals("market_exchange", message.getPageId());
        assertEquals(TerminalActionType.MARKET_EXCHANGE_CONFIRM.getId(), message.getActionType());
        assertEquals(TerminalExchangeMarketActionPayload.TARGET_TASK_COIN,
            TerminalExchangeMarketActionPayload.decode(message.getPayload()).getSelectedTargetCode());
    }

    private static TerminalHomeScreenModel createMarketScreenModel() {
        TerminalHomeScreenModel.PageSnapshotModel home = TerminalHomeScreenModel.PageSnapshotModel.placeholder(TerminalPage.HOME);
        TerminalHomeScreenModel.PageSnapshotModel career = TerminalHomeScreenModel.PageSnapshotModel.placeholder(TerminalPage.CAREER);
        TerminalHomeScreenModel.PageSnapshotModel publicService = TerminalHomeScreenModel.PageSnapshotModel.placeholder(TerminalPage.PUBLIC_SERVICE);
        TerminalHomeScreenModel.PageSnapshotModel market = new TerminalHomeScreenModel.PageSnapshotModel(
            "market",
            "标准商品市场",
            "标准商品市场完整业务页",
            Arrays.asList(
                new TerminalHomeScreenModel.SectionModel("market_standardized_runtime", "标准商品运行态", "市场服务在线", "当前标准市场已迁入新壳。")),
            null,
            new TerminalMarketSectionModel(
                "market_standardized",
                "市场服务在线",
                "请选择左侧商品进入交易详情。",
                Arrays.asList("minecraft:stone:0"),
                Arrays.asList("石头 | minecraft:stone:0"),
                "minecraft:stone:0",
                "石头",
                "组",
                "12 STARCOIN",
                "11 STARCOIN",
                "13 STARCOIN",
                "64",
                "768 STARCOIN",
                "32",
                "8",
                "4",
                "120 STARCOIN",
                "最新成交价仅用于行情展示。",
                Arrays.asList("13 x 16", "14 x 32"),
                Arrays.asList("11 x 12", "10 x 48"),
                Arrays.asList("orderId=7 | BUY | OPEN | 16 @ 11"),
                Arrays.asList("custodyId=31 | 4 单位待提取"),
                Arrays.asList("31"),
                Arrays.asList("即时成交按真实盘口撮合。", "CLAIMABLE 资产可直接提取。"),
                new TerminalMarketSectionModel.LimitBuyDraftModel("minecraft:stone:0", "12", "16", true),
                new TerminalMarketSectionModel.ActionFeedbackModel("市场动作反馈", "等待确认提交。", "INFO")));
        TerminalHomeScreenModel.PageSnapshotModel bank = TerminalHomeScreenModel.PageSnapshotModel.placeholder(TerminalPage.BANK);

        return new TerminalHomeScreenModel(
            "market_standardized",
            "银河终端 / Test",
            "phase 6 标准商品市场",
            new TerminalHomeScreenModel.StatusBandModel("当前页", "标准商品市场", "标准市场完整业务页已迁入新壳。", "贡献", "0"),
            Arrays.asList(
                new TerminalHomeScreenModel.NavItemModel("home", "首页", "总览", true, false),
                new TerminalHomeScreenModel.NavItemModel("career", "职业", "职业页", true, false),
                new TerminalHomeScreenModel.NavItemModel("public_service", "公共", "公共页", true, false),
                new TerminalHomeScreenModel.NavItemModel("market", "市场", "总入口", true, true),
                new TerminalHomeScreenModel.NavItemModel("bank", "银行", "银行页", true, false)),
            Arrays.asList(home, career, publicService, market, bank),
            Arrays.asList(new TerminalHomeScreenModel.NotificationModel("市场页已迁入新壳", "当前可以直接走市场确认 popup。", "INFO")),
            "session-market-test");
    }

    private static TerminalHomeScreenModel createCustomMarketScreenModel() {
        return createScreenModelWithMarketSnapshot(
            "market_custom",
            new TerminalHomeScreenModel.PageSnapshotModel(
                "market",
                "定制商品市场",
                "listing-first",
                Arrays.asList(new TerminalHomeScreenModel.SectionModel("custom", "定制", "在线", "详情")),
                null,
                null,
                new TerminalCustomMarketSectionModel(
                    "定制商品市场在线",
                    "listing-first 浏览",
                    "全部挂牌",
                    Arrays.asList("#42 | 定制商品 | 99 STARCOIN | ACTIVE"),
                    Arrays.asList("42"),
                    Arrays.asList("你当前没有出售中的挂牌。"),
                    Arrays.asList(""),
                    Arrays.asList("你当前没有待领取成交物。"),
                    Arrays.asList(""),
                    "42",
                    "定制商品",
                    "99 STARCOIN",
                    "ACTIVE / ESCROW_HELD",
                    "卖家=seller",
                    "minecraft:stone @0",
                    "尚未成交",
                    "可购买",
                    true,
                    false,
                    false,
                    new TerminalCustomMarketSectionModel.ActionFeedbackModel("定制", "等待确认", "INFO")),
                null));
    }

    private static TerminalHomeScreenModel createExchangeMarketScreenModel() {
        return createScreenModelWithMarketSnapshot(
            "market_exchange",
            new TerminalHomeScreenModel.PageSnapshotModel(
                "market",
                "汇率市场",
                "quote-first",
                Arrays.asList(new TerminalHomeScreenModel.SectionModel("exchange", "汇率", "在线", "详情")),
                null,
                null,
                null,
                new TerminalExchangeMarketSectionModel(
                    "汇率市场在线",
                    "quote-first 浏览",
                    Arrays.asList(TerminalExchangeMarketActionPayload.TARGET_TASK_COIN),
                    Arrays.asList("任务书硬币 -> STARCOIN | formal quote"),
                    TerminalExchangeMarketActionPayload.TARGET_TASK_COIN,
                    "任务书硬币正式兑换",
                    "formal quote",
                    "手持 1 枚",
                    "dreamcraft:item.Coin",
                    "TASK_COIN_TO_STARCOIN",
                    "TASK_COIN",
                    "STARCOIN",
                    "task_coin_v1",
                    "ALLOWED",
                    "--",
                    "可执行",
                    "1",
                    "100",
                    "100",
                    "10",
                    "按面值执行",
                    "100 / 1",
                    "确认后兑换",
                    true,
                    new TerminalExchangeMarketSectionModel.ActionFeedbackModel("汇率", "等待确认", "INFO"))));
    }

    private static TerminalHomeScreenModel createScreenModelWithMarketSnapshot(String selectedPageId,
        TerminalHomeScreenModel.PageSnapshotModel market) {
        return new TerminalHomeScreenModel(
            selectedPageId,
            "银河终端 / Test",
            "phase 7 market",
            new TerminalHomeScreenModel.StatusBandModel("当前页", "市场", "market", "贡献", "0"),
            Arrays.asList(
                new TerminalHomeScreenModel.NavItemModel("home", "首页", "总览", true, false),
                new TerminalHomeScreenModel.NavItemModel("career", "职业", "职业页", true, false),
                new TerminalHomeScreenModel.NavItemModel("public_service", "公共", "公共页", true, false),
                new TerminalHomeScreenModel.NavItemModel("market", "市场", "总入口", true, true),
                new TerminalHomeScreenModel.NavItemModel("bank", "银行", "银行页", true, false)),
            Arrays.asList(
                TerminalHomeScreenModel.PageSnapshotModel.placeholder(TerminalPage.HOME),
                TerminalHomeScreenModel.PageSnapshotModel.placeholder(TerminalPage.CAREER),
                TerminalHomeScreenModel.PageSnapshotModel.placeholder(TerminalPage.PUBLIC_SERVICE),
                market,
                TerminalHomeScreenModel.PageSnapshotModel.placeholder(TerminalPage.BANK)),
            Arrays.asList(new TerminalHomeScreenModel.NotificationModel("市场", "phase7", "INFO")),
            "session-market-test");
    }
}
