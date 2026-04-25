package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.jsirgalaxybase.terminal.client.TerminalClientScreenController;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.network.TerminalSnapshotMessage;
import com.jsirgalaxybase.terminal.ui.TerminalActionFeedback;
import com.jsirgalaxybase.terminal.ui.TerminalBankSnapshot;
import com.jsirgalaxybase.terminal.ui.TerminalBankingService;
import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;
import com.jsirgalaxybase.terminal.ui.TerminalPage;

public class TerminalServiceTest {

    @Test
    public void buildSnapshotCarriesSelectedPageIntoShellHostSnapshot() {
        TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
            null,
            "market_custom",
            "session-1",
            TerminalActionType.SELECT_PAGE,
            "nav_click");

        assertEquals("market_custom", approval.getSelectedPageId());
        assertEquals("market", approval.getNavItems().get(3).getPageId());
        assertTrue(approval.getNavItems().get(3).isSelected());
        assertEquals("market", approval.getPageSnapshots().get(3).getPageId());
    }

    @Test
    public void refreshActionProducesRefreshNotificationAndStableSessionToken() {
        TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
            null,
            "career",
            "session-2",
            TerminalActionType.REFRESH_PAGE,
            "manual_refresh");

        assertEquals("session-2", approval.getSessionToken());
        assertFalse(approval.getNotifications().isEmpty());
        assertTrue(approval.getNotifications().get(0).getBody().contains("TerminalActionMessage -> TerminalSnapshotMessage"));
    }

    @Test
    public void refreshActionRoundTripsThroughSnapshotMessageToScreenModel() {
        TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
            null,
            "bank",
            "session-3",
            TerminalActionType.BANK_REFRESH,
            "");

        TerminalHomeScreenModel model = new TerminalSnapshotMessage(approval).toScreenModel();

        assertEquals("bank", model.getSelectedPageId());
        assertEquals("bank", model.getSelectedSectionPageId());
        assertEquals("session-3", model.getSessionToken());
        assertEquals("bank", model.getSelectedPageSnapshot().getPageId());
        assertNotNull(model.getSelectedPageSnapshot().getBankSectionModel());
        assertEquals("银行页摘要已刷新", model.getSelectedPageSnapshot().getBankSectionModel().getActionFeedback().getBody());
    }

    @Test
    public void snapshotHandlerQueuesRefreshedModelForClientScreenController() throws Exception {
        TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
            null,
            "market",
            "session-4",
            TerminalActionType.SELECT_PAGE,
            "nav_click");
        TerminalSnapshotMessage message = new TerminalSnapshotMessage(approval);
        TerminalSnapshotMessage.Handler handler = new TerminalSnapshotMessage.Handler();

        setPendingHomeScreen(null);
        handler.onMessage(message, null);

        TerminalHomeScreenModel queuedModel = getPendingHomeScreen();
        assertNotNull(queuedModel);
        assertEquals("market", queuedModel.getSelectedPageId());
        assertEquals("market", queuedModel.getSelectedSectionPageId());
        assertEquals("session-4", queuedModel.getSessionToken());

        setPendingHomeScreen(null);
        assertNull(getPendingHomeScreen());
    }

    @Test
    public void bankOpenAccountActionTriggersServiceHandlingAndWritesBackBankSnapshot() {
        StubBankPageFacade facade = new StubBankPageFacade(
            unopenedSnapshot(),
            openedSnapshot(),
            transferedSnapshot());
        TerminalService.setBankPageFacadeForTest(facade);

        try {
            TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
                null,
                "bank",
                "session-open",
                TerminalActionType.BANK_OPEN_ACCOUNT,
                "");

            assertTrue(facade.openCalled);
            assertEquals("session-open", approval.getSessionToken());
            assertTrue(approval.getPageSnapshots().get(4).getBankSectionSnapshot().getAccountStatus().isOpened());
            assertEquals("0 / STARCOIN", approval.getPageSnapshots().get(4).getBankSectionSnapshot().getBalanceSummary().getPlayerBalance());
            assertTrue(approval.getNotifications().get(0).getBody().contains("开户成功"));
        } finally {
            TerminalService.resetBankPageFacadeForTest();
        }
    }

    @Test
    public void bankTransferActionTriggersServiceHandlingAndWritesBackUpdatedSnapshot() {
        StubBankPageFacade facade = new StubBankPageFacade(
            openedSnapshot(),
            openedSnapshot(),
            transferedSnapshot());
        TerminalService.setBankPageFacadeForTest(facade);

        try {
            TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
                null,
                "bank",
                "session-transfer",
                TerminalActionType.BANK_CONFIRM_TRANSFER,
                new TerminalBankActionPayload("Receiver", "250", "phase5 test").encode());

            assertTrue(facade.transferCalled);
            assertEquals("Receiver", facade.lastTarget);
            assertEquals(250L, facade.lastAmount);
            assertEquals("phase5 test", facade.lastComment);
            assertEquals("750 / STARCOIN", approval.getPageSnapshots().get(4).getBankSectionSnapshot().getBalanceSummary().getPlayerBalance());
            assertEquals("Receiver", approval.getPageSnapshots().get(4).getBankSectionSnapshot().getTransferForm().getTargetPlayerName());
            assertEquals("", approval.getPageSnapshots().get(4).getBankSectionSnapshot().getTransferForm().getAmountText());
            assertTrue(approval.getPageSnapshots().get(4).getBankSectionSnapshot().getActionFeedback().getBody().contains("转账成功"));
        } finally {
            TerminalService.resetBankPageFacadeForTest();
        }
    }

    @Test
    public void bankActionsDoNotExecuteWhenCurrentPageIsNotBank() {
        StubBankPageFacade facade = new StubBankPageFacade(
            unopenedSnapshot(),
            openedSnapshot(),
            transferedSnapshot());
        TerminalService.setBankPageFacadeForTest(facade);

        try {
            TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
                null,
                "market",
                "session-non-bank",
                TerminalActionType.BANK_OPEN_ACCOUNT,
                "");

            assertFalse(facade.openCalled);
            assertFalse(facade.transferCalled);
            assertEquals("market", approval.getSelectedPageId());
            assertEquals("market", approval.getPageSnapshots().get(3).getPageId());
        } finally {
            TerminalService.resetBankPageFacadeForTest();
        }
    }

    @Test
    public void marketRefreshRoundTripsThroughSnapshotMessageToScreenModel() {
        StubMarketPageFacade facade = new StubMarketPageFacade();
        TerminalService.setMarketPageFacadeForTest(facade);

        try {
            TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
                null,
                "market_standardized",
                "session-market-refresh",
                TerminalActionType.MARKET_REFRESH,
                new TerminalMarketActionPayload("minecraft:stone:0", "", "", "").encode());

            TerminalHomeScreenModel model = new TerminalSnapshotMessage(approval).toScreenModel();

            assertEquals("market_standardized", model.getSelectedPageId());
            assertEquals("market", model.getSelectedSectionPageId());
            assertEquals("market_standardized", model.getSelectedPageSnapshot().getMarketSectionModel().getRoutePageId());
            assertEquals("石头", model.getSelectedPageSnapshot().getMarketSectionModel().getSelectedProductName());
            assertTrue(model.getSelectedPageSnapshot().getMarketSectionModel().getActionFeedback().getBody().contains("已刷新"));
        } finally {
            TerminalService.resetMarketPageFacadeForTest();
        }
    }

    @Test
    public void marketLimitBuyActionTriggersServiceHandlingAndWritesBackUpdatedSnapshot() {
        StubMarketPageFacade facade = new StubMarketPageFacade();
        TerminalService.setMarketPageFacadeForTest(facade);

        try {
            TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
                null,
                "market_standardized",
                "session-market-buy",
                TerminalActionType.MARKET_CONFIRM_LIMIT_BUY,
                new TerminalMarketActionPayload("minecraft:stone:0", "12", "16", "").encode());

            assertTrue(facade.limitBuyCalled);
            assertEquals("minecraft:stone:0", facade.lastPayload.getSelectedProductKey());
            assertEquals(12L, facade.lastPayload.parsePrice());
            assertEquals(16L, facade.lastPayload.parseQuantity());
            assertEquals("", approval.getPageSnapshots().get(3).getMarketSectionSnapshot().getLimitBuyDraft().getQuantityText());
            assertTrue(approval.getPageSnapshots().get(3).getMarketSectionSnapshot().getActionFeedback().getBody().contains("买单已提交"));
        } finally {
            TerminalService.resetMarketPageFacadeForTest();
        }
    }

    @Test
    public void marketClaimActionTriggersServiceHandlingAndClearsPendingClaimId() {
        StubMarketPageFacade facade = new StubMarketPageFacade();
        TerminalService.setMarketPageFacadeForTest(facade);

        try {
            TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
                null,
                "market_standardized",
                "session-market-claim",
                TerminalActionType.MARKET_CLAIM_ASSET,
                new TerminalMarketActionPayload("minecraft:stone:0", "", "", "31").encode());

            assertTrue(facade.claimCalled);
            assertEquals(31L, facade.lastPayload.parseCustodyId());
            assertEquals("", approval.getPageSnapshots().get(3).getMarketSectionSnapshot().getClaimIds().get(0));
            assertTrue(approval.getNotifications().get(0).getBody().contains("资产已提取"));
        } finally {
            TerminalService.resetMarketPageFacadeForTest();
        }
    }

    @Test
    public void customMarketBuyActionTriggersServiceHandlingAndWritesBackSnapshot() {
        StubMarketPageFacade facade = new StubMarketPageFacade();
        TerminalService.setMarketPageFacadeForTest(facade);

        try {
            TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
                null,
                "market_custom",
                "session-custom-buy",
                TerminalActionType.MARKET_CUSTOM_BUY_LISTING,
                new TerminalCustomMarketActionPayload("active", "42").encode());

            assertTrue(facade.customBuyCalled);
            assertEquals(42L, facade.lastCustomPayload.parseSelectedListingId());
            assertNotNull(approval.getPageSnapshots().get(3).getCustomMarketSectionSnapshot());
            assertEquals("定制商品", approval.getPageSnapshots().get(3).getCustomMarketSectionSnapshot().getSelectedTitle());
            assertTrue(approval.getPageSnapshots().get(3).getCustomMarketSectionSnapshot().getActionFeedback().getBody().contains("挂牌已买下"));
        } finally {
            TerminalService.resetMarketPageFacadeForTest();
        }
    }

    @Test
    public void exchangeMarketConfirmActionTriggersServiceHandlingAndWritesBackSnapshot() {
        StubMarketPageFacade facade = new StubMarketPageFacade();
        TerminalService.setMarketPageFacadeForTest(facade);

        try {
            TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
                null,
                "market_exchange",
                "session-exchange",
                TerminalActionType.MARKET_EXCHANGE_CONFIRM,
                new TerminalExchangeMarketActionPayload(TerminalExchangeMarketActionPayload.TARGET_TASK_COIN).encode());

            assertTrue(facade.exchangeSubmitCalled);
            assertNotNull(approval.getPageSnapshots().get(3).getExchangeMarketSectionSnapshot());
            assertEquals("任务书硬币正式兑换", approval.getPageSnapshots().get(3).getExchangeMarketSectionSnapshot().getSelectedTargetTitle());
            assertTrue(approval.getPageSnapshots().get(3).getExchangeMarketSectionSnapshot().getActionFeedback().getBody().contains("兑换已完成"));
        } finally {
            TerminalService.resetMarketPageFacadeForTest();
        }
    }

    @Test
    public void exchangeMarketConfirmDoesNotExecuteWithoutSelectedTargetPayload() {
        StubMarketPageFacade facade = new StubMarketPageFacade();
        TerminalService.setMarketPageFacadeForTest(facade);

        try {
            TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
                null,
                "market_exchange",
                "session-exchange-reject",
                TerminalActionType.MARKET_EXCHANGE_CONFIRM,
                TerminalExchangeMarketActionPayload.empty().encode());

            assertFalse(facade.exchangeSubmitCalled);
            assertNotNull(approval.getPageSnapshots().get(3).getExchangeMarketSectionSnapshot());
            assertTrue(approval.getPageSnapshots().get(3).getExchangeMarketSectionSnapshot().getActionFeedback().getBody()
                .contains("服务端拒绝"));
        } finally {
            TerminalService.resetMarketPageFacadeForTest();
        }
    }

    @Test
    public void customMarketSnapshotRoundTripsWithoutFixedListingTruncation() {
        List<String> listingLines = new ArrayList<String>();
        List<String> listingIds = new ArrayList<String>();
        for (int index = 1; index <= 12; index++) {
            listingLines.add("#" + index + " | 定制商品 " + index + " | ACTIVE");
            listingIds.add(String.valueOf(index));
        }
        TerminalOpenApproval approval = new TerminalOpenApproval(
            "market_custom",
            "银河终端",
            "phase7",
            TerminalOpenApproval.StatusBand.placeholder(),
            Collections.singletonList(new TerminalOpenApproval.NavItem("market", "市场", "总入口", true, true)),
            Collections.singletonList(new TerminalOpenApproval.PageSnapshot(
                "market",
                "定制商品市场",
                "listing-first",
                Collections.singletonList(TerminalOpenApproval.Section.placeholder()),
                null,
                null,
                new TerminalCustomMarketSectionSnapshot(
                    "定制商品市场在线",
                    "listing-first",
                    "全部挂牌",
                    listingLines,
                    listingIds,
                    Collections.singletonList("empty"),
                    Collections.<String>emptyList(),
                    Collections.singletonList("empty"),
                    Collections.<String>emptyList(),
                    "12",
                    "定制商品 12",
                    "120 STARCOIN",
                    "ACTIVE",
                    "卖家=seller",
                    "minecraft:stone @0",
                    "尚未成交",
                    "可购买",
                    true,
                    false,
                    false,
                    TerminalCustomMarketSectionSnapshot.ActionFeedback.placeholder()),
                null)),
            Collections.<TerminalOpenApproval.NotificationEntry>emptyList(),
            "session-custom-long");

        TerminalHomeScreenModel model = new TerminalSnapshotMessage(approval).toScreenModel();

        assertEquals(12, model.getSelectedPageSnapshot().getCustomMarketSectionModel().getActiveListingLines().size());
        assertEquals("12", model.getSelectedPageSnapshot().getCustomMarketSectionModel().getActiveListingIds().get(11));
    }

    private static TerminalBankSnapshot unopenedSnapshot() {
        return new TerminalBankSnapshot(
            "银行服务在线",
            "未开户",
            "未开户 / 按需开户 / 余额 0",
            "未分配",
            "无更新记录",
            "请先开户后再转账",
            new String[] { "尚未开户，暂无个人流水" },
            "99,999 / STARCOIN",
            "ACTIVE / 公开透明 / 兑换储备",
            "EX-001",
            "04-18 10:00",
            new String[] { "公开账本" });
    }

    private static TerminalBankSnapshot openedSnapshot() {
        return new TerminalBankSnapshot(
            "银行服务在线",
            "0 / STARCOIN",
            "ACTIVE / 按需开户 / 冻结 0",
            "ACC-001",
            "04-18 10:10",
            "可向已开户玩家转账",
            new String[] { "04-18 10:10 | 入账 +0 | 结余 0" },
            "99,999 / STARCOIN",
            "ACTIVE / 公开透明 / 兑换储备",
            "EX-001",
            "04-18 10:00",
            new String[] { "公开账本" });
    }

    private static TerminalBankSnapshot transferedSnapshot() {
        return new TerminalBankSnapshot(
            "银行服务在线",
            "750 / STARCOIN",
            "ACTIVE / 按需开户 / 冻结 0",
            "ACC-001",
            "04-18 10:20",
            "最近一次转账已完成",
            new String[] { "04-18 10:20 | 出账 -250 | 结余 750", "04-18 10:10 | 入账 +1,000 | 结余 1,000" },
            "99,999 / STARCOIN",
            "ACTIVE / 公开透明 / 兑换储备",
            "EX-001",
            "04-18 10:00",
            new String[] { "公开账本" });
    }

    private static final class StubBankPageFacade implements TerminalService.BankPageFacade {

        private final TerminalBankSnapshot initialSnapshot;
        private final TerminalBankSnapshot openSnapshot;
        private final TerminalBankSnapshot transferSnapshot;
        private TerminalBankSnapshot currentSnapshot;
        private boolean openCalled;
        private boolean transferCalled;
        private String lastTarget;
        private long lastAmount;
        private String lastComment;

        private StubBankPageFacade(TerminalBankSnapshot initialSnapshot, TerminalBankSnapshot openSnapshot,
            TerminalBankSnapshot transferSnapshot) {
            this.initialSnapshot = initialSnapshot;
            this.openSnapshot = openSnapshot;
            this.transferSnapshot = transferSnapshot;
            this.currentSnapshot = initialSnapshot;
        }

        @Override
        public TerminalBankSnapshot createSnapshot(net.minecraft.entity.player.EntityPlayer player) {
            return currentSnapshot == null ? initialSnapshot : currentSnapshot;
        }

        @Override
        public TerminalBankingService.ActionResult openOwnAccount(net.minecraft.entity.player.EntityPlayer player) {
            openCalled = true;
            currentSnapshot = openSnapshot;
            return TerminalBankingService.ActionResult.success("开户成功: ACC-001 | 余额 0 STARCOIN");
        }

        @Override
        public TerminalBankingService.ActionResult transferToPlayer(net.minecraft.entity.player.EntityPlayer player,
            String targetPlayerName, long amount, String comment) {
            transferCalled = true;
            lastTarget = targetPlayerName;
            lastAmount = amount;
            lastComment = comment;
            currentSnapshot = transferSnapshot;
            return TerminalBankingService.ActionResult.success("转账成功: 向 Receiver 支付 250 STARCOIN");
        }
    }

    private static final class StubMarketPageFacade implements TerminalService.MarketPageFacade {

        private boolean limitBuyCalled;
        private boolean claimCalled;
        private boolean customBuyCalled;
        private boolean customCancelCalled;
        private boolean customClaimCalled;
        private boolean exchangeRefreshCalled;
        private boolean exchangeSubmitCalled;
        private TerminalMarketActionPayload lastPayload;
        private TerminalCustomMarketActionPayload lastCustomPayload;

        @Override
        public TerminalMarketSectionSnapshot createSnapshot(net.minecraft.entity.player.EntityPlayer player,
            TerminalPage selectedPage, TerminalMarketActionPayload payload, TerminalActionFeedback actionFeedback) {
            TerminalMarketActionPayload effectivePayload = payload == null ? TerminalMarketActionPayload.empty() : payload;
            String claimId = claimCalled ? "" : effectivePayload.getCustodyIdText();
            String quantityText = limitBuyCalled ? "" : effectivePayload.getQuantityText();
            return new TerminalMarketSectionSnapshot(
                selectedPage == null ? "market" : selectedPage.getId(),
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
                "标准商品市场摘要说明。",
                Arrays.asList("13 x 16", "14 x 32"),
                Arrays.asList("11 x 12", "10 x 48"),
                Arrays.asList("orderId=7 | BUY | OPEN | 16 @ 11"),
                Arrays.asList(claimId.isEmpty() ? "" : "custodyId=" + claimId + " | 4 单位待提取"),
                Arrays.asList(claimId),
                Arrays.asList("CLAIMABLE 资产可直接提取。", "即时成交按真实盘口撮合。"),
                new TerminalMarketSectionSnapshot.LimitBuyDraft(
                    effectivePayload.getSelectedProductKey(),
                    effectivePayload.getPriceText(),
                    quantityText,
                    true),
                new TerminalMarketSectionSnapshot.ActionFeedback(
                    actionFeedback == null ? "市场动作反馈" : actionFeedback.getTitle(),
                    actionFeedback == null ? "当前没有市场动作反馈。" : actionFeedback.getBody(),
                    actionFeedback == null ? TerminalNotificationSeverity.INFO.name() : actionFeedback.getSeverity().name()));
        }

        @Override
        public TerminalActionFeedback submitLimitBuy(net.minecraft.entity.player.EntityPlayer player,
            TerminalMarketActionPayload payload) {
            limitBuyCalled = true;
            lastPayload = payload;
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "买单已提交",
                "买单已提交: orderId=7",
                3200L);
        }

        @Override
        public TerminalActionFeedback claimAsset(net.minecraft.entity.player.EntityPlayer player,
            TerminalMarketActionPayload payload) {
            claimCalled = true;
            lastPayload = payload;
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "资产已提取",
                "资产已提取: custodyId=" + payload.parseCustodyId(),
                3200L);
        }

        @Override
        public TerminalCustomMarketSectionSnapshot createCustomSnapshot(net.minecraft.entity.player.EntityPlayer player,
            TerminalCustomMarketActionPayload payload, TerminalActionFeedback actionFeedback) {
            TerminalCustomMarketActionPayload effectivePayload = payload == null ? TerminalCustomMarketActionPayload.empty() : payload;
            return new TerminalCustomMarketSectionSnapshot(
                "定制商品市场在线",
                "listing-first 浏览已刷新。",
                "全部挂牌",
                Arrays.asList("#42 | 定制商品 | 99 STARCOIN | ACTIVE", "#43 | 备用挂牌 | 10 STARCOIN | ACTIVE"),
                Arrays.asList("42", "43"),
                Arrays.asList("你当前没有出售中的挂牌。"),
                Arrays.asList(""),
                Arrays.asList("你当前没有待领取成交物。"),
                Arrays.asList(""),
                effectivePayload.getSelectedListingId().isEmpty() ? "42" : effectivePayload.getSelectedListingId(),
                "定制商品",
                "99 STARCOIN",
                customBuyCalled ? "SOLD / BUYER_PENDING_CLAIM" : "ACTIVE / ESCROW_HELD",
                "卖家=seller / 买家=暂无",
                "minecraft:stone @0",
                "当前尚未成交",
                "当前是他人 active listing，可执行 buy。",
                !customBuyCalled,
                false,
                customBuyCalled,
                new TerminalCustomMarketSectionSnapshot.ActionFeedback(
                    actionFeedback == null ? "定制市场动作反馈" : actionFeedback.getTitle(),
                    actionFeedback == null ? "当前没有定制市场动作反馈。" : actionFeedback.getBody(),
                    actionFeedback == null ? TerminalNotificationSeverity.INFO.name() : actionFeedback.getSeverity().name()));
        }

        @Override
        public TerminalActionFeedback purchaseCustomListing(net.minecraft.entity.player.EntityPlayer player,
            TerminalCustomMarketActionPayload payload) {
            customBuyCalled = true;
            lastCustomPayload = payload;
            return TerminalActionFeedback.of(TerminalNotificationSeverity.SUCCESS, "挂牌已买下", "挂牌已买下: listingId=42", 3200L);
        }

        @Override
        public TerminalActionFeedback cancelCustomListing(net.minecraft.entity.player.EntityPlayer player,
            TerminalCustomMarketActionPayload payload) {
            customCancelCalled = true;
            lastCustomPayload = payload;
            return TerminalActionFeedback.of(TerminalNotificationSeverity.SUCCESS, "挂牌已下架", "挂牌已下架: listingId=42", 3200L);
        }

        @Override
        public TerminalActionFeedback claimCustomListing(net.minecraft.entity.player.EntityPlayer player,
            TerminalCustomMarketActionPayload payload) {
            customClaimCalled = true;
            lastCustomPayload = payload;
            return TerminalActionFeedback.of(TerminalNotificationSeverity.SUCCESS, "成交物已完成提取", "成交物已完成提取: listingId=42", 3200L);
        }

        @Override
        public TerminalExchangeMarketSectionSnapshot createExchangeSnapshot(net.minecraft.entity.player.EntityPlayer player,
            TerminalExchangeMarketActionPayload payload, TerminalActionFeedback actionFeedback) {
            TerminalExchangeMarketActionPayload effectivePayload = payload == null ? TerminalExchangeMarketActionPayload.empty() : payload;
            return new TerminalExchangeMarketSectionSnapshot(
                "汇率市场在线",
                "quote-first 浏览已刷新。",
                Arrays.asList(TerminalExchangeMarketActionPayload.TARGET_TASK_COIN),
                Arrays.asList("任务书硬币 -> STARCOIN | formal quote"),
                effectivePayload.getSelectedTargetCode(),
                "任务书硬币正式兑换",
                "当前详情页聚焦 formal quote。",
                "手持 3 枚任务书硬币",
                "dreamcraft:item.Coin",
                "TASK_COIN_TO_STARCOIN",
                "TASK_COIN",
                "STARCOIN",
                "task_coin_v1",
                "ALLOWED",
                "--",
                "可执行",
                "3",
                "300",
                "300",
                "30",
                "按面值执行",
                "100 / 1",
                "确认后将把当前手持硬币兑换为 300 STARCOIN。",
                true,
                new TerminalExchangeMarketSectionSnapshot.ActionFeedback(
                    actionFeedback == null ? "汇率市场动作反馈" : actionFeedback.getTitle(),
                    actionFeedback == null ? "当前没有汇率市场动作反馈。" : actionFeedback.getBody(),
                    actionFeedback == null ? TerminalNotificationSeverity.INFO.name() : actionFeedback.getSeverity().name()));
        }

        @Override
        public TerminalActionFeedback refreshExchangeQuote(net.minecraft.entity.player.EntityPlayer player) {
            exchangeRefreshCalled = true;
            return TerminalActionFeedback.of(TerminalNotificationSeverity.INFO, "正式报价已刷新", "正式报价已刷新", 3200L);
        }

        @Override
        public TerminalActionFeedback submitExchangeHeld(net.minecraft.entity.player.EntityPlayer player) {
            exchangeSubmitCalled = true;
            return TerminalActionFeedback.of(TerminalNotificationSeverity.SUCCESS, "汇率兑换完成", "兑换已完成: 300 STARCOIN", 3200L);
        }
    }

    private static TerminalHomeScreenModel getPendingHomeScreen() throws Exception {
        Field field = TerminalClientScreenController.class.getDeclaredField("pendingHomeScreen");
        field.setAccessible(true);
        return (TerminalHomeScreenModel) field.get(TerminalClientScreenController.INSTANCE);
    }

    private static void setPendingHomeScreen(TerminalHomeScreenModel model) throws Exception {
        Field field = TerminalClientScreenController.class.getDeclaredField("pendingHomeScreen");
        field.setAccessible(true);
        field.set(TerminalClientScreenController.INSTANCE, model);
    }
}
