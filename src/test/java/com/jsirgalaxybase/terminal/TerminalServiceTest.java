package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.cluster.domain.ServerDescriptor;
import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicket;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicketStatus;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;
import com.jsirgalaxybase.modules.servertools.domain.TeleportKind;
import com.jsirgalaxybase.modules.servertools.domain.ServerWarp;
import com.jsirgalaxybase.terminal.client.TerminalClientScreenController;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.terminal.network.TerminalSnapshotMessage;
import com.jsirgalaxybase.terminal.ui.TerminalActionFeedback;
import com.jsirgalaxybase.terminal.ui.TerminalBankSnapshot;
import com.jsirgalaxybase.terminal.ui.TerminalBankingService;
import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;
import com.jsirgalaxybase.terminal.ui.TerminalPage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

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
        assertEquals("分区快照已刷新", approval.getNotifications().get(0).getTitle());
        assertTrue(approval.getNotifications().get(0).getBody().contains("已完成刷新"));
    }

    @Test
    public void refreshActionRoundTripsThroughSnapshotMessageToScreenModel() {
        TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
            null,
            "bank",
            "session-3",
            TerminalActionType.BANK_REFRESH,
            "");

        TerminalSnapshotMessage encoded = new TerminalSnapshotMessage(approval);
        ByteBuf byteBuf = Unpooled.buffer();
        encoded.toBytes(byteBuf);
        TerminalSnapshotMessage decoded = new TerminalSnapshotMessage();
        decoded.fromBytes(byteBuf);
        TerminalHomeScreenModel model = decoded.toScreenModel();

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
                new TerminalMarketActionPayload("minecraft:stone:0", "", "", "", "", "", "", "", "").encode());

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
                new TerminalMarketActionPayload("minecraft:stone:0", "12", "16", "", "", "", "", "", "").encode());

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
    public void marketActionWritesBackLatestBankSnapshotAfterFundsMutate() {
        MutableBankPageFacade bankFacade = new MutableBankPageFacade(openedSnapshot());
        ReactiveMarketPageFacade marketFacade = new ReactiveMarketPageFacade(new Runnable() {
            @Override
            public void run() {
                bankFacade.setSnapshot(transferedSnapshot());
            }
        });
        TerminalService.setBankPageFacadeForTest(bankFacade);
        TerminalService.setMarketPageFacadeForTest(marketFacade);

        try {
            TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
                null,
                "market_standardized",
                "session-market-bank-refresh",
                TerminalActionType.MARKET_CONFIRM_LIMIT_BUY,
                new TerminalMarketActionPayload("minecraft:stone:0", "12", "16", "", "", "", "", "", "").encode());

            assertTrue(marketFacade.limitBuyCalled);
            assertEquals("750 / STARCOIN", approval.getPageSnapshots().get(4).getBankSectionSnapshot()
                .getBalanceSummary().getPlayerBalance());
        } finally {
            TerminalService.resetBankPageFacadeForTest();
            TerminalService.resetMarketPageFacadeForTest();
        }
    }

    @Test
    public void marketDepositActionTriggersServiceHandlingAndWritesBackUpdatedSnapshot() {
        StubMarketPageFacade facade = new StubMarketPageFacade();
        TerminalService.setMarketPageFacadeForTest(facade);

        try {
            TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
                null,
                "market_standardized",
                "session-market-deposit",
                TerminalActionType.MARKET_CONFIRM_DEPOSIT_HELD,
                new TerminalMarketActionPayload("minecraft:stone:0", "", "", "", "", "", "", "", "").encode());

            assertTrue(facade.depositCalled);
            assertEquals("minecraft:stone:0", facade.lastPayload.getSelectedProductKey());
            assertTrue(approval.getPageSnapshots().get(3).getMarketSectionSnapshot().isDepositEnabled());
            assertTrue(approval.getPageSnapshots().get(3).getMarketSectionSnapshot().getActionFeedback().getBody()
                .contains("已存入仓储"));
        } finally {
            TerminalService.resetMarketPageFacadeForTest();
        }
    }

    @Test
    public void marketLimitSellAndCancelActionsWriteBackUpdatedSnapshot() {
        StubMarketPageFacade facade = new StubMarketPageFacade();
        TerminalService.setMarketPageFacadeForTest(facade);

        try {
            TerminalOpenApproval sellApproval = TerminalService.buildTerminalSnapshot(
                null,
                "market_standardized",
                "session-market-sell",
                TerminalActionType.MARKET_CONFIRM_LIMIT_SELL,
                new TerminalMarketActionPayload("minecraft:stone:0", "", "", "", "", "13", "8", "", "").encode());

            assertTrue(facade.limitSellCalled);
            assertEquals(13L, facade.lastPayload.parseLimitSellPrice());
            assertEquals("", sellApproval.getPageSnapshots().get(3).getMarketSectionSnapshot().getLimitSellDraft().getQuantityText());

            TerminalOpenApproval cancelApproval = TerminalService.buildTerminalSnapshot(
                null,
                "market_standardized",
                "session-market-cancel",
                TerminalActionType.MARKET_CANCEL_ORDER,
                new TerminalMarketActionPayload("minecraft:stone:0", "", "", "", "7", "", "", "", "").encode());

            assertTrue(facade.cancelOrderCalled);
            assertEquals(7L, facade.lastPayload.parseOrderId());
            assertEquals("", cancelApproval.getPageSnapshots().get(3).getMarketSectionSnapshot().getMyOrderIds().get(0));
        } finally {
            TerminalService.resetMarketPageFacadeForTest();
        }
    }

    @Test
    public void marketInstantActionsTriggerServiceHandlingAndClearDrafts() {
        StubMarketPageFacade facade = new StubMarketPageFacade();
        TerminalService.setMarketPageFacadeForTest(facade);

        try {
            TerminalOpenApproval buyApproval = TerminalService.buildTerminalSnapshot(
                null,
                "market_standardized",
                "session-market-instant-buy",
                TerminalActionType.MARKET_CONFIRM_INSTANT_BUY,
                new TerminalMarketActionPayload("minecraft:stone:0", "", "", "", "", "", "", "5", "").encode());

            assertTrue(facade.instantBuyCalled);
            assertEquals("", buyApproval.getPageSnapshots().get(3).getMarketSectionSnapshot().getInstantBuyDraft().getQuantityText());

            TerminalOpenApproval sellApproval = TerminalService.buildTerminalSnapshot(
                null,
                "market_standardized",
                "session-market-instant-sell",
                TerminalActionType.MARKET_CONFIRM_INSTANT_SELL,
                new TerminalMarketActionPayload("minecraft:stone:0", "", "", "", "", "", "", "", "6").encode());

            assertTrue(facade.instantSellCalled);
            assertEquals("", sellApproval.getPageSnapshots().get(3).getMarketSectionSnapshot().getInstantSellDraft().getQuantityText());
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
                new TerminalMarketActionPayload("minecraft:stone:0", "", "", "31", "", "", "", "", "").encode());

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
    public void customMarketPublishActionRoutesPricePayloadAndWritesBackSnapshot() {
        StubMarketPageFacade facade = new StubMarketPageFacade();
        TerminalService.setMarketPageFacadeForTest(facade);

        try {
            TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
                null,
                "market_custom",
                "session-custom-publish",
                TerminalActionType.MARKET_CUSTOM_PUBLISH_HELD,
                new TerminalCustomMarketActionPayload("active", "", "1250").encode());

            assertTrue(facade.customPublishCalled);
            assertEquals(1250L, facade.lastCustomPayload.parsePublishPrice());
            assertNotNull(approval.getPageSnapshots().get(3).getCustomMarketSectionSnapshot());
            assertTrue(approval.getNotifications().get(0).getBody().contains("单件挂牌已发布"));
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

    @Test
    public void serverToolsRefreshAndSelectWarpWriteBackSectionSnapshot() {
        StubServerToolsPageFacade facade = new StubServerToolsPageFacade();
        TerminalService.setServerToolsPageFacadeForTest(facade);

        try {
            TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
                null,
                "server_tools",
                "session-server-tools",
                TerminalActionType.SERVER_TOOLS_SELECT_WARP,
                TerminalServerToolsActionPayload.forWarp("s2test").encode());

            TerminalHomeScreenModel model = new TerminalSnapshotMessage(approval).toScreenModel();

            assertEquals("server_tools", model.getSelectedPageId());
            assertEquals("server_tools", model.getSelectedSectionPageId());
            assertEquals("s2test", model.getSelectedPageSnapshot().getServerToolsSectionModel().getSelectedWarpName());
            assertEquals(2, model.getSelectedPageSnapshot().getServerToolsSectionModel().getWarpNames().size());
            assertTrue(model.getSelectedPageSnapshot().getServerToolsSectionModel().getRecentTransferLines().get(0)
                .contains("DISPATCHED"));
            assertFalse(facade.confirmCalled);
        } finally {
            TerminalService.resetServerToolsPageFacadeForTest();
        }
    }

    @Test
    public void serverToolsConfirmWarpRejectsNonServerPlayerBeforeFacadeExecution() {
        StubServerToolsPageFacade facade = new StubServerToolsPageFacade();
        TerminalService.setServerToolsPageFacadeForTest(facade);

        try {
            TerminalOpenApproval approval = TerminalService.buildTerminalSnapshot(
                null,
                "server_tools",
                "session-server-tools-confirm",
                TerminalActionType.SERVER_TOOLS_CONFIRM_WARP,
                TerminalServerToolsActionPayload.forWarp("s2test").encode());

            assertFalse(facade.confirmCalled);
            assertTrue(approval.getPageSnapshots().get(5).getServerToolsSectionSnapshot().getActionFeedback().getBody()
                .contains("服务端在线玩家"));
        } finally {
            TerminalService.resetServerToolsPageFacadeForTest();
        }
    }

    @Test
    public void serverToolsSnapshotRoundTripsThroughTerminalSnapshotMessage() {
        TerminalOpenApproval approval = new TerminalOpenApproval(
            "server_tools",
            "银河终端",
            "server-tools",
            TerminalOpenApproval.StatusBand.placeholder(),
            Collections.singletonList(new TerminalOpenApproval.NavItem("server_tools", "传送", "群组服", true, true)),
            Collections.singletonList(new TerminalOpenApproval.PageSnapshot(
                "server_tools",
                "群组服传送",
                "展示服务器目录、系统 warp 与最近传送反馈",
                Collections.singletonList(TerminalOpenApproval.Section.placeholder()),
                null,
                null,
                null,
                null,
                new TerminalServerToolsSectionSnapshot(
                    "ServerTools warp runtime online",
                    "lobby",
                    Arrays.asList("lobby | Lobby | 当前", "s2 | S2"),
                    Arrays.asList("lobby", "s2"),
                    Arrays.asList("[可用] s2test", "[可用] lobbytest"),
                    Arrays.asList("s2test", "lobbytest"),
                    Arrays.asList("前往 S2 测试节点", "返回 Lobby 中枢"),
                    Arrays.asList("可用", "可用"),
                    Arrays.asList("05-18 10:00 | lobby -> s2 | COMPLETED | restore completed"),
                    "s2test",
                    "s2test",
                    "target=s2",
                    "s2",
                    "dim 0 / 0, 80, 0",
                    "前往 S2 测试节点",
                    true,
                    "lobby",
                    "s2",
                    "COMPLETED",
                    "05-18 10:00",
                    "restore completed",
                    new TerminalServerToolsSectionSnapshot.ActionFeedback("已选择 warp", "当前选中: s2test", "INFO")))),
            Collections.<TerminalOpenApproval.NotificationEntry>emptyList(),
            "session-server-tools-roundtrip");

        TerminalHomeScreenModel model = new TerminalSnapshotMessage(approval).toScreenModel();

        assertEquals("server_tools", model.getSelectedPageSnapshot().getPageId());
        assertEquals("s2test", model.getSelectedPageSnapshot().getServerToolsSectionModel().getSelectedWarpName());
        assertEquals("lobbytest", model.getSelectedPageSnapshot().getServerToolsSectionModel().getWarpNames().get(1));
        assertEquals("前往 S2 测试节点", model.getSelectedPageSnapshot().getServerToolsSectionModel().getWarpSubtitles().get(0));
        assertEquals("s2", model.getSelectedPageSnapshot().getServerToolsSectionModel().getSelectedTargetServerId());
        assertEquals("05-18 10:00 | lobby -> s2 | COMPLETED | restore completed",
            model.getSelectedPageSnapshot().getServerToolsSectionModel().getRecentTransferLines().get(0));
        assertEquals("COMPLETED", model.getSelectedPageSnapshot().getServerToolsSectionModel().getRecentTransferStatus());
    }

    @Test
    public void defaultServerToolsFacadeUsesRuntimeBridgeForRemoteDispatch() {
        TerminalService.setServerToolsRuntimeProviderForTest(new FixedServerToolsRuntimeProvider(
            new RecordingServerToolsRuntimeBridge(
                GatewayDispatchResult.pendingRemote(
                    "proxy dispatch requested",
                    ticket("req-terminal-remote", TransferTicketStatus.DISPATCHED, "proxy dispatch requested")))));
        TerminalService.resetServerToolsPageFacadeForTest();

        try {
            TerminalServerToolsSectionSnapshot.ActionFeedback feedback =
                TerminalService.serverToolsPageFacade.confirmWarp(null, "s2test");

            assertEquals("跨服传送已提交", feedback.getTitle());
            assertTrue(feedback.getBody().contains("proxy dispatch requested"));
            RecordingServerToolsRuntimeBridge bridge =
                (RecordingServerToolsRuntimeBridge) TerminalService.serverToolsRuntimeProvider.resolve();
            assertTrue(bridge.prepareCalled);
            assertTrue(bridge.dispatchCalled);
            assertEquals("s2test", bridge.lastWarpName);
            assertEquals("server-alpha", bridge.lastDispatchPlan.getSourceServerId());
            assertEquals("server-beta", bridge.lastDispatchPlan.getTarget().getServerId());
        } finally {
            TerminalService.resetServerToolsRuntimeProviderForTest();
            TerminalService.resetServerToolsPageFacadeForTest();
        }
    }

    @Test
    public void defaultServerToolsFacadeUsesRuntimeBridgeForLocalCompletion() {
        TerminalService.setServerToolsRuntimeProviderForTest(new FixedServerToolsRuntimeProvider(
            new RecordingServerToolsRuntimeBridge(GatewayDispatchResult.completedLocal("local warp completed"))));
        TerminalService.resetServerToolsPageFacadeForTest();

        try {
            TerminalServerToolsSectionSnapshot.ActionFeedback feedback =
                TerminalService.serverToolsPageFacade.confirmWarp(null, "lobbytest");

            assertEquals("本服传送完成", feedback.getTitle());
            assertTrue(feedback.getBody().contains("lobbytest"));
        } finally {
            TerminalService.resetServerToolsRuntimeProviderForTest();
            TerminalService.resetServerToolsPageFacadeForTest();
        }
    }

    @Test
    public void defaultServerToolsFacadeReportsUnavailableRuntime() {
        TerminalService.setServerToolsRuntimeProviderForTest(new FixedServerToolsRuntimeProvider(
            new UnavailableServerToolsRuntimeBridge()));
        TerminalService.resetServerToolsPageFacadeForTest();

        try {
            TerminalServerToolsSectionSnapshot.ActionFeedback feedback =
                TerminalService.serverToolsPageFacade.confirmWarp(null, "s2test");

            assertEquals("传送失败", feedback.getTitle());
            assertTrue(feedback.getBody().contains("runtime"));
        } finally {
            TerminalService.resetServerToolsRuntimeProviderForTest();
            TerminalService.resetServerToolsPageFacadeForTest();
        }
    }

    @Test
    public void defaultServerToolsFacadeReportsBackendException() {
        TerminalService.setServerToolsRuntimeProviderForTest(new FixedServerToolsRuntimeProvider(
            new ThrowingServerToolsRuntimeBridge()));
        TerminalService.resetServerToolsPageFacadeForTest();

        try {
            TerminalServerToolsSectionSnapshot.ActionFeedback feedback =
                TerminalService.serverToolsPageFacade.confirmWarp(null, "s2test");

            assertEquals("传送失败", feedback.getTitle());
            assertTrue(feedback.getBody().contains("missing warp"));
        } finally {
            TerminalService.resetServerToolsRuntimeProviderForTest();
            TerminalService.resetServerToolsPageFacadeForTest();
        }
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

    private static final class StubServerToolsPageFacade implements TerminalService.ServerToolsPageFacade {

        private boolean confirmCalled;

        @Override
        public TerminalServerToolsSectionSnapshot createSnapshot(net.minecraft.entity.player.EntityPlayer player,
            TerminalServerToolsActionPayload payload,
            TerminalServerToolsSectionSnapshot.ActionFeedback actionFeedback) {
            TerminalServerToolsActionPayload effectivePayload = payload == null ? TerminalServerToolsActionPayload.empty() : payload;
            String selected = effectivePayload.hasWarpName() ? effectivePayload.getWarpName() : "s2test";
            return new TerminalServerToolsSectionSnapshot(
                "ServerTools warp runtime online",
                "lobby",
                Arrays.asList("lobby | Lobby | 当前", "s2 | S2"),
                Arrays.asList("lobby", "s2"),
                Arrays.asList("[可用] s2test", "[可用] lobbytest"),
                Arrays.asList("s2test", "lobbytest"),
                Arrays.asList("前往 S2 测试节点", "返回 Lobby 中枢"),
                Arrays.asList("可用", "可用"),
                Arrays.asList("05-18 10:00 | lobby -> s2 | DISPATCHED | proxy dispatch requested"),
                selected,
                selected,
                "target=" + ("lobbytest".equals(selected) ? "lobby" : "s2"),
                "lobbytest".equals(selected) ? "lobby" : "s2",
                "dim 0 / 0, 80, 0",
                "lobbytest".equals(selected) ? "返回 Lobby 中枢" : "前往 S2 测试节点",
                true,
                "lobby",
                "s2",
                "DISPATCHED",
                "05-18 10:00",
                "proxy dispatch requested",
                actionFeedback == null ? TerminalServerToolsSectionSnapshot.ActionFeedback.placeholder() : actionFeedback);
        }

        @Override
        public TerminalServerToolsSectionSnapshot.ActionFeedback confirmWarp(net.minecraft.entity.player.EntityPlayerMP player,
            String warpName) {
            confirmCalled = true;
            return new TerminalServerToolsSectionSnapshot.ActionFeedback(
                "跨服传送已提交",
                "Transfer ticket created / pending remote: " + warpName,
                TerminalNotificationSeverity.SUCCESS.name());
        }
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

    private static final class MutableBankPageFacade implements TerminalService.BankPageFacade {

        private TerminalBankSnapshot snapshot;

        private MutableBankPageFacade(TerminalBankSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public TerminalBankSnapshot createSnapshot(net.minecraft.entity.player.EntityPlayer player) {
            return snapshot;
        }

        @Override
        public TerminalBankingService.ActionResult openOwnAccount(net.minecraft.entity.player.EntityPlayer player) {
            return TerminalBankingService.ActionResult.info("unused");
        }

        @Override
        public TerminalBankingService.ActionResult transferToPlayer(net.minecraft.entity.player.EntityPlayer player,
            String targetPlayerName, long amount, String comment) {
            return TerminalBankingService.ActionResult.info("unused");
        }

        private void setSnapshot(TerminalBankSnapshot snapshot) {
            this.snapshot = snapshot;
        }
    }

    private static class StubMarketPageFacade implements TerminalService.MarketPageFacade {

        boolean depositCalled;
        boolean limitBuyCalled;
        boolean limitSellCalled;
        boolean instantBuyCalled;
        boolean instantSellCalled;
        boolean cancelOrderCalled;
        boolean claimCalled;
        boolean customBuyCalled;
        boolean customPublishCalled;
        boolean customCancelCalled;
        boolean customClaimCalled;
        boolean exchangeRefreshCalled;
        boolean exchangeSubmitCalled;
        TerminalMarketActionPayload lastPayload;
        TerminalCustomMarketActionPayload lastCustomPayload;

        @Override
        public TerminalMarketSectionSnapshot createSnapshot(net.minecraft.entity.player.EntityPlayer player,
            TerminalPage selectedPage, TerminalMarketActionPayload payload, TerminalActionFeedback actionFeedback) {
            TerminalMarketActionPayload effectivePayload = payload == null ? TerminalMarketActionPayload.empty() : payload;
            String claimId = claimCalled ? "" : effectivePayload.getCustodyIdText();
            String limitBuyQuantityText = limitBuyCalled ? "" : effectivePayload.getLimitBuyQuantityText();
            String limitSellQuantityText = limitSellCalled ? "" : effectivePayload.getLimitSellQuantityText();
            String instantBuyQuantityText = instantBuyCalled ? "" : effectivePayload.getInstantBuyQuantityText();
            String instantSellQuantityText = instantSellCalled ? "" : effectivePayload.getInstantSellQuantityText();
            String orderId = cancelOrderCalled ? "" : effectivePayload.getOrderIdText();
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
                "12",
                "16",
                "64",
                "768 STARCOIN",
                "32",
                "8",
                "4",
                "120 STARCOIN",
                "标准商品市场摘要说明。",
                "目录版本=default | 来源=runtime | 卖出来源=统一仓储 AVAILABLE",
                depositCalled ? "已存入最新手持。" : "当前 AVAILABLE 为 32，可直接卖出。",
                "冻结预计 204 STARCOIN。",
                "将锁定 AVAILABLE 数量并等待成交。",
                "预计按当前卖盘成交。",
                "预计按当前买盘成交。",
                Arrays.asList("13 x 16", "14 x 32"),
                Arrays.asList("11 x 12", "10 x 48"),
                Arrays.asList(orderId.isEmpty() ? "" : "orderId=" + orderId + " | BUY | OPEN | 16 @ 11"),
                Arrays.asList(orderId),
                Arrays.asList(orderId.isEmpty() ? "0" : "1"),
                Arrays.asList(claimId.isEmpty() ? "" : "custodyId=" + claimId + " | 4 单位待提取"),
                Arrays.asList(claimId),
                Arrays.asList("CLAIMABLE 资产可直接提取。", "即时成交按真实盘口撮合。"),
                true,
                new TerminalMarketSectionSnapshot.LimitBuyDraft(
                    effectivePayload.getSelectedProductKey(),
                    effectivePayload.getLimitBuyPriceText(),
                    limitBuyQuantityText,
                    true),
                new TerminalMarketSectionSnapshot.LimitSellDraft(
                    effectivePayload.getSelectedProductKey(),
                    effectivePayload.getLimitSellPriceText(),
                    limitSellQuantityText,
                    true),
                new TerminalMarketSectionSnapshot.InstantDraft(
                    effectivePayload.getSelectedProductKey(),
                    instantBuyQuantityText,
                    true),
                new TerminalMarketSectionSnapshot.InstantDraft(
                    effectivePayload.getSelectedProductKey(),
                    instantSellQuantityText,
                    true),
                new TerminalMarketSectionSnapshot.ActionFeedback(
                    actionFeedback == null ? "市场动作反馈" : actionFeedback.getTitle(),
                    actionFeedback == null ? "当前没有市场动作反馈。" : actionFeedback.getBody(),
                    actionFeedback == null ? TerminalNotificationSeverity.INFO.name() : actionFeedback.getSeverity().name()));
        }

        @Override
        public TerminalActionFeedback submitDepositHeld(net.minecraft.entity.player.EntityPlayer player,
            TerminalMarketActionPayload payload) {
            depositCalled = true;
            lastPayload = payload;
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "已存入仓储",
                "已存入仓储: product=" + payload.getSelectedProductKey(),
                3200L);
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
        public TerminalActionFeedback submitLimitSell(net.minecraft.entity.player.EntityPlayer player,
            TerminalMarketActionPayload payload) {
            limitSellCalled = true;
            lastPayload = payload;
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "卖单已提交",
                "卖单已提交: orderId=8",
                3200L);
        }

        @Override
        public TerminalActionFeedback submitInstantBuy(net.minecraft.entity.player.EntityPlayer player,
            TerminalMarketActionPayload payload) {
            instantBuyCalled = true;
            lastPayload = payload;
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "即时买入完成",
                "即时买入完成: quantity=" + payload.parseInstantBuyQuantity(),
                3200L);
        }

        @Override
        public TerminalActionFeedback submitInstantSell(net.minecraft.entity.player.EntityPlayer player,
            TerminalMarketActionPayload payload) {
            instantSellCalled = true;
            lastPayload = payload;
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "即时卖出完成",
                "即时卖出完成: quantity=" + payload.parseInstantSellQuantity(),
                3200L);
        }

        @Override
        public TerminalActionFeedback cancelOrder(net.minecraft.entity.player.EntityPlayer player,
            TerminalMarketActionPayload payload) {
            cancelOrderCalled = true;
            lastPayload = payload;
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "买单已撤销",
                "买单已撤销: orderId=" + payload.parseOrderId(),
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
        public TerminalActionFeedback publishCustomListing(net.minecraft.entity.player.EntityPlayer player,
            TerminalCustomMarketActionPayload payload) {
            customPublishCalled = true;
            lastCustomPayload = payload;
            return TerminalActionFeedback.of(TerminalNotificationSeverity.SUCCESS, "单件挂牌已发布",
                "单件挂牌已发布: price=" + payload.parsePublishPrice(), 3200L);
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
        public TerminalActionFeedback submitExchange(net.minecraft.entity.player.EntityPlayer player,
            TerminalExchangeMarketActionPayload payload) {
            exchangeSubmitCalled = true;
            return TerminalActionFeedback.of(TerminalNotificationSeverity.SUCCESS, "汇率兑换完成", "兑换已完成: 300 STARCOIN", 3200L);
        }
    }

    private static final class ReactiveMarketPageFacade extends StubMarketPageFacade {

        private final Runnable afterStandardizedAction;

        private ReactiveMarketPageFacade(Runnable afterStandardizedAction) {
            this.afterStandardizedAction = afterStandardizedAction;
        }

        @Override
        public TerminalActionFeedback submitLimitBuy(net.minecraft.entity.player.EntityPlayer player,
            TerminalMarketActionPayload payload) {
            TerminalActionFeedback feedback = super.submitLimitBuy(player, payload);
            if (afterStandardizedAction != null) {
                afterStandardizedAction.run();
            }
            return feedback;
        }
    }

    private static TransferTicket ticket(String requestId, TransferTicketStatus status, String statusMessage) {
        return new TransferTicket(
            "ticket-" + requestId,
            requestId,
            "player-uuid",
            "PlayerA",
            "WARP",
            "server-alpha",
            new TeleportTarget("server-beta", 0, 10, 70, 10, 0.0F, 0.0F),
            status,
            statusMessage,
            Instant.now().minusSeconds(30),
            Instant.now().plusSeconds(300),
            Instant.now());
    }

    private static final class FixedServerToolsRuntimeProvider implements TerminalService.ServerToolsRuntimeProvider {

        private final TerminalService.ServerToolsRuntimeBridge runtimeBridge;

        private FixedServerToolsRuntimeProvider(TerminalService.ServerToolsRuntimeBridge runtimeBridge) {
            this.runtimeBridge = runtimeBridge;
        }

        @Override
        public TerminalService.ServerToolsRuntimeBridge resolve() {
            return runtimeBridge;
        }
    }

    private static class RecordingServerToolsRuntimeBridge implements TerminalService.ServerToolsRuntimeBridge {

        private final GatewayDispatchResult dispatchResult;
        private boolean prepareCalled;
        private boolean dispatchCalled;
        private String lastWarpName;
        private TeleportDispatchPlan lastDispatchPlan;

        private RecordingServerToolsRuntimeBridge(GatewayDispatchResult dispatchResult) {
            this.dispatchResult = dispatchResult;
        }

        @Override
        public boolean isRuntimeAvailable() {
            return true;
        }

        @Override
        public String getLocalServerId() {
            return "server-alpha";
        }

        @Override
        public List<ServerDescriptor> listServers() {
            return Arrays.asList(
                new ServerDescriptor("server-alpha", "Lobby", null, true, true, Instant.now(), Instant.now()),
                new ServerDescriptor("server-beta", "S2", null, false, true, Instant.now(), Instant.now()));
        }

        @Override
        public List<ServerWarp> listWarps() {
            return Arrays.asList(
                new ServerWarp("s2test", "S2 测试点", "server beta test", new TeleportTarget("server-beta", 0, 10, 70, 10, 0.0F, 0.0F), true, Instant.now(), Instant.now()),
                new ServerWarp("lobbytest", "Lobby 测试点", "server alpha test", new TeleportTarget("server-alpha", 0, 0, 70, 0, 0.0F, 0.0F), true, Instant.now(), Instant.now()));
        }

        @Override
        public List<TransferTicket> findRecentTickets(String playerUuid, int limit) {
            return Arrays.asList(ticket("req-recent", TransferTicketStatus.COMPLETED, "restore completed"));
        }

        @Override
        public TeleportDispatchPlan prepareWarpTeleport(net.minecraft.entity.player.EntityPlayerMP player, String warpName) {
            prepareCalled = true;
            lastWarpName = warpName;
            lastDispatchPlan = new TeleportDispatchPlan(
                "req-terminal-" + warpName,
                "player-uuid",
                "PlayerA",
                "server-alpha",
                TeleportKind.WARP,
                "lobbytest".equals(warpName)
                    ? new TeleportTarget("server-alpha", 0, 0, 70, 0, 0.0F, 0.0F)
                    : new TeleportTarget("server-beta", 0, 10, 70, 10, 0.0F, 0.0F));
            return lastDispatchPlan;
        }

        @Override
        public GatewayDispatchResult dispatchTeleport(net.minecraft.entity.player.EntityPlayerMP player,
            TeleportDispatchPlan dispatchPlan) {
            dispatchCalled = true;
            lastDispatchPlan = dispatchPlan;
            return dispatchResult;
        }

        @Override
        public net.minecraft.entity.player.EntityPlayerMP findOnlinePlayer(String playerName) {
            return null;
        }
    }

    private static final class UnavailableServerToolsRuntimeBridge implements TerminalService.ServerToolsRuntimeBridge {

        @Override
        public boolean isRuntimeAvailable() {
            return false;
        }

        @Override
        public String getLocalServerId() {
            return "server-alpha";
        }

        @Override
        public List<ServerDescriptor> listServers() {
            return Collections.emptyList();
        }

        @Override
        public List<ServerWarp> listWarps() {
            return Collections.emptyList();
        }

        @Override
        public List<TransferTicket> findRecentTickets(String playerUuid, int limit) {
            return Collections.emptyList();
        }

        @Override
        public TeleportDispatchPlan prepareWarpTeleport(net.minecraft.entity.player.EntityPlayerMP player, String warpName) {
            throw new IllegalStateException("should not be called");
        }

        @Override
        public GatewayDispatchResult dispatchTeleport(net.minecraft.entity.player.EntityPlayerMP player,
            TeleportDispatchPlan dispatchPlan) {
            throw new IllegalStateException("should not be called");
        }

        @Override
        public net.minecraft.entity.player.EntityPlayerMP findOnlinePlayer(String playerName) {
            return null;
        }
    }

    private static final class ThrowingServerToolsRuntimeBridge extends RecordingServerToolsRuntimeBridge {

        private ThrowingServerToolsRuntimeBridge() {
            super(GatewayDispatchResult.completedLocal("unused"));
        }

        @Override
        public TeleportDispatchPlan prepareWarpTeleport(net.minecraft.entity.player.EntityPlayerMP player, String warpName) {
            throw new IllegalStateException("missing warp route");
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
