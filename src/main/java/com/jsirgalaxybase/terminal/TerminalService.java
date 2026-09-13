package com.jsirgalaxybase.terminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.cluster.domain.ServerDescriptor;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicket;
import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.vault.application.BaseVaultService;
import com.jsirgalaxybase.modules.core.vault.domain.VaultSlot;
import com.jsirgalaxybase.modules.core.vault.infrastructure.minecraft.BaseVaultGuiHandler;
import com.jsirgalaxybase.modules.itempolicy.ItemPolicyModule;
import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyAuditRecord;
import com.jsirgalaxybase.modules.itempolicy.domain.ItemPolicyRule;
import com.jsirgalaxybase.modules.land.LandModule;
import com.jsirgalaxybase.modules.warehouse.WarehouseModule;
import com.jsirgalaxybase.modules.warehouse.application.TerminalWarehouseBayService;
import com.jsirgalaxybase.modules.warehouse.application.TerminalWarehouseCellInspector;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBay;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBayReceipt;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveHealth;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveReceipt;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveRecord;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.WarehouseDriveHealthResolver;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.TerminalWarehouseBayGuiHandler;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.TerminalAssetCenterGuiHandler;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.TerminalAssetCenterTab;
import com.jsirgalaxybase.modules.cluster.infrastructure.ClusterInfrastructure;
import com.jsirgalaxybase.modules.servertools.ServerToolsModule;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.domain.PlayerHome;
import com.jsirgalaxybase.modules.servertools.domain.ServerWarp;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;
import com.jsirgalaxybase.modules.servertools.domain.TpaRequest;
import com.jsirgalaxybase.terminal.ui.TerminalBankSnapshot;
import com.jsirgalaxybase.terminal.ui.TerminalBankSnapshotProvider;
import com.jsirgalaxybase.terminal.ui.TerminalBankingService;
import com.jsirgalaxybase.terminal.ui.TerminalActionFeedback;
import com.jsirgalaxybase.terminal.ui.TerminalHomeSnapshot;
import com.jsirgalaxybase.terminal.ui.TerminalHomeSnapshotProvider;
import com.jsirgalaxybase.terminal.ui.TerminalMarketSectionService;
import com.jsirgalaxybase.terminal.ui.TerminalLandPageService;
import com.jsirgalaxybase.terminal.ui.TerminalNotification;
import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;
import com.jsirgalaxybase.terminal.ui.TerminalPage;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestClaimService;
import com.jsirgalaxybase.quest.core.AuthenticatedRewardChoiceService;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestTrackingService;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestDefinitionManagementQuery;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestChapterManagementQuery;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestChapterDependencyQuery;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestDefinitionImpactQuery;
import com.jsirgalaxybase.quest.core.QuestDefinitionImpactReport;
import com.jsirgalaxybase.quest.core.QuestDefinitionBatchImpactReport;
import com.jsirgalaxybase.quest.core.QuestDefinitionBatchRetirementResult;
import com.jsirgalaxybase.quest.core.QuestChapterDependencyGraph;
import com.jsirgalaxybase.quest.core.QuestDefinitionManagementPage;
import com.jsirgalaxybase.quest.core.QuestDefinitionManagementRequest;
import com.jsirgalaxybase.quest.core.QuestDefinitionLifecycle;
import com.jsirgalaxybase.quest.core.QuestDraftManagementResult;
import com.jsirgalaxybase.quest.core.QuestDraftManagementService;
import com.jsirgalaxybase.quest.core.QuestChapterManagementService;
import com.jsirgalaxybase.quest.core.QuestChapterManagementResult;
import com.jsirgalaxybase.quest.core.QuestChapterManagementPage;
import com.jsirgalaxybase.quest.core.QuestChapterCloneService;
import com.jsirgalaxybase.quest.core.QuestChapterAlignmentService;
import com.jsirgalaxybase.quest.core.QuestChapterCloneResult;
import com.jsirgalaxybase.quest.core.QuestChapterOrderingService;
import com.jsirgalaxybase.quest.core.QuestChapterOrderingResult;
import com.jsirgalaxybase.quest.core.StoredQuestChapter;
import com.jsirgalaxybase.quest.core.QuestChapterEntry;
import com.jsirgalaxybase.quest.core.QuestDraftTemplate;
import com.jsirgalaxybase.quest.core.QuestDraftEditRequest;
import com.jsirgalaxybase.quest.core.QuestEditorActor;
import com.jsirgalaxybase.quest.core.QuestEditorValidationIssue;
import com.jsirgalaxybase.quest.core.QuestElementKind;
import com.jsirgalaxybase.quest.core.QuestElementTypeDescriptor;
import com.jsirgalaxybase.quest.core.QuestEditorFieldDescriptor;
import com.jsirgalaxybase.quest.core.StoredQuestDefinition;
import com.jsirgalaxybase.quest.core.RewardChoiceSelectionStatus;
import com.jsirgalaxybase.quest.core.QuestCenterPage;
import com.jsirgalaxybase.quest.core.QuestCenterPageRequest;
import com.jsirgalaxybase.quest.core.QuestCenterQuery;
import com.jsirgalaxybase.quest.core.RewardClaimStatus;
import com.jsirgalaxybase.modules.quest.application.TerminalQuestCenterSnapshotMapper;

public final class TerminalService {

    private static final DateTimeFormatter SERVER_TOOLS_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.ROOT).withZone(ZoneId.systemDefault());

    static BankPageFacade bankPageFacade = new DefaultBankPageFacade();
    static MarketPageFacade marketPageFacade = new DefaultMarketPageFacade();
    static ServerToolsPageFacade serverToolsPageFacade = new DefaultServerToolsPageFacade();
    static ServerToolsRuntimeProvider serverToolsRuntimeProvider = new DefaultServerToolsRuntimeProvider();
    static final TerminalExchangeQuoteConfirmationGate exchangeQuoteConfirmationGate =
        new TerminalExchangeQuoteConfirmationGate();
    static final TerminalLandPageService landPageService = new TerminalLandPageService();
    static final TerminalPlayerNotificationCenter notificationCenter = new TerminalPlayerNotificationCenter();
    private static volatile QuestCenterQuery questCenterQuery;
    private static volatile AuthenticatedQuestClaimService questClaimService;
    private static volatile AuthenticatedRewardChoiceService rewardChoiceService;
    private static volatile AuthenticatedQuestTrackingService questTrackingService;
    private static volatile AuthenticatedQuestDefinitionManagementQuery questDefinitionManagementQuery;
    private static volatile QuestDraftManagementService questDraftManagementService;
    private static volatile com.jsirgalaxybase.quest.core.QuestDefinitionBatchRetirementService questBatchRetirementService;
    private static volatile AuthenticatedQuestChapterManagementQuery questChapterManagementQuery;
    private static volatile AuthenticatedQuestChapterDependencyQuery questChapterDependencyQuery;
    private static volatile AuthenticatedQuestDefinitionImpactQuery questDefinitionImpactQuery;
    private static volatile QuestChapterManagementService questChapterManagementService;
    private static volatile QuestChapterCloneService questChapterCloneService;
    private static volatile QuestChapterAlignmentService questChapterAlignmentService;
    private static volatile QuestChapterOrderingService questChapterOrderingService;
    private static final TerminalQuestCenterSnapshotMapper questCenterMapper = new TerminalQuestCenterSnapshotMapper();

    private TerminalService() {}

    public static TerminalOpenApproval approveTerminalClientScreen(EntityPlayerMP player) {
        if (!canOpenTerminal(player)) {
            return null;
        }
        return buildTerminalSnapshot(player, TerminalPage.HOME.getId(), UUID.randomUUID().toString(),
            TerminalActionType.OPEN_SHELL, "");
    }

    public static TerminalOpenApproval handleClientAction(EntityPlayerMP player, String sessionToken, String pageId,
        String actionType, String payload) {
        if (!canOpenTerminal(player)) {
            return null;
        }
        if (TerminalActionType.fromId(actionType) == TerminalActionType.VAULT_OPEN
            && (TerminalPage.fromId(pageId) == TerminalPage.VAULT || TerminalPage.fromId(pageId) == TerminalPage.WAREHOUSE)) {
            if (!TerminalAssetCenterGuiHandler.open(player, TerminalAssetCenterTab.STORAGE)) {
                GalaxyBase.LOG.warn("Base Vault GUI was requested before its server runtime was ready for {}",
                    player.getCommandSenderName());
            }
            return null;
        }
        if (TerminalActionType.fromId(actionType) == TerminalActionType.WAREHOUSE_OPEN_BAY
            && TerminalPage.fromId(pageId) == TerminalPage.WAREHOUSE) {
            if (!TerminalAssetCenterGuiHandler.open(player, TerminalAssetCenterTab.STORAGE)) {
                GalaxyBase.LOG.warn("Terminal Warehouse Bay GUI was requested before its server runtime was ready for {}",
                    player.getCommandSenderName());
            }
            return null;
        }
        return buildTerminalSnapshot(player, pageId, sessionToken, TerminalActionType.fromId(actionType), payload);
    }

    static TerminalOpenApproval buildTerminalSnapshot(EntityPlayer player, String pageId, String sessionToken,
        TerminalActionType actionType, String payload) {
        TerminalPage selectedPage = TerminalPage.fromId(pageId);
        String normalizedPageId = selectedPage.getId();
        String normalizedSessionToken = normalize(sessionToken, "terminal-session");
        String playerName = player == null ? "访客" : player.getCommandSenderName();
        TerminalHomeSnapshot snapshot = TerminalHomeSnapshotProvider.INSTANCE.create(player);
        MarketActionContext marketContext = buildMarketActionContext(player, normalizedSessionToken, selectedPage, actionType,
            payload);
        BankActionContext bankContext = buildBankActionContext(player, selectedPage, actionType, payload);
        ServerToolsActionContext serverToolsContext = buildServerToolsActionContext(player, selectedPage, actionType, payload);
        LandActionContext landContext = buildLandActionContext(player, selectedPage, actionType, payload);
        if (player == null) notificationCenter.clearAnonymousForTestOrPreview();
        List<TerminalOpenApproval.NotificationEntry> notifications = notificationCenter.recordAndPage(player,
            createNotifications(player, selectedPage, actionType, bankContext, marketContext, serverToolsContext));
        return new TerminalOpenApproval(
            normalizedPageId,
            "银河终端 / " + (playerName == null || playerName.trim().isEmpty() ? "访客" : playerName),
            "服务端授权已通过，当前为 phase 9 新终端壳正式入口主链",
            new TerminalOpenApproval.StatusBand(
                "当前页",
                selectedPage.getTitle(),
                buildStatusDetail(snapshot, selectedPage, actionType, bankContext, marketContext, serverToolsContext),
                "贡献",
                String.valueOf(snapshot == null ? 0 : snapshot.getContribution())),
            createTopLevelNavItems(normalizedPageId),
            createPageSnapshots(player, snapshot, bankContext, marketContext, serverToolsContext, landContext,
                selectedPage, actionType, payload),
            notifications,
            normalizedSessionToken);
    }

    /** Installs Base's own quest read model. The caller owns lifecycle; null disables the terminal task center. */
    public static void installQuestCenterQuery(QuestCenterQuery query) { questCenterQuery = query; }
    public static void installQuestClaimService(AuthenticatedQuestClaimService service) { questClaimService = service; }
    public static void installRewardChoiceService(AuthenticatedRewardChoiceService service) { rewardChoiceService = service; }
    public static void installQuestTrackingService(AuthenticatedQuestTrackingService service) { questTrackingService = service; }
    public static void installQuestDefinitionManagementQuery(AuthenticatedQuestDefinitionManagementQuery query) { questDefinitionManagementQuery = query; }
    public static void installQuestDraftManagementService(QuestDraftManagementService service) { questDraftManagementService = service; }
    public static void installQuestBatchRetirementService(com.jsirgalaxybase.quest.core.QuestDefinitionBatchRetirementService service) { questBatchRetirementService = service; }
    public static void installQuestChapterManagementQuery(AuthenticatedQuestChapterManagementQuery query) { questChapterManagementQuery = query; }
    public static void installQuestChapterDependencyQuery(AuthenticatedQuestChapterDependencyQuery query) { questChapterDependencyQuery = query; }
    public static void installQuestDefinitionImpactQuery(AuthenticatedQuestDefinitionImpactQuery query) { questDefinitionImpactQuery = query; }
    public static void installQuestChapterManagementService(QuestChapterManagementService service) { questChapterManagementService = service; }
    /** Installs the Base-owned atomic chapter copy command; null leaves definitions unchanged. */
    public static void installQuestChapterCloneService(QuestChapterCloneService service) { questChapterCloneService = service; }
    public static void installQuestChapterAlignmentService(QuestChapterAlignmentService service) { questChapterAlignmentService = service; }
    public static void installQuestChapterOrderingService(QuestChapterOrderingService service) { questChapterOrderingService = service; }

    static void setBankPageFacadeForTest(BankPageFacade facade) {
        bankPageFacade = facade == null ? new DefaultBankPageFacade() : facade;
    }

    static void resetBankPageFacadeForTest() {
        bankPageFacade = new DefaultBankPageFacade();
    }

    static void setMarketPageFacadeForTest(MarketPageFacade facade) {
        marketPageFacade = facade == null ? new DefaultMarketPageFacade() : facade;
    }

    static void resetMarketPageFacadeForTest() {
        marketPageFacade = new DefaultMarketPageFacade();
    }

    static void setServerToolsPageFacadeForTest(ServerToolsPageFacade facade) {
        serverToolsPageFacade = facade == null ? new DefaultServerToolsPageFacade() : facade;
    }

    static void resetServerToolsPageFacadeForTest() {
        serverToolsPageFacade = new DefaultServerToolsPageFacade();
    }

    static void setServerToolsRuntimeProviderForTest(ServerToolsRuntimeProvider provider) {
        serverToolsRuntimeProvider = provider == null ? new DefaultServerToolsRuntimeProvider() : provider;
    }

    static void resetServerToolsRuntimeProviderForTest() {
        serverToolsRuntimeProvider = new DefaultServerToolsRuntimeProvider();
    }

    private static BankActionContext buildBankActionContext(EntityPlayer player, TerminalPage selectedPage,
        TerminalActionType actionType, String payload) {
        TerminalBankActionPayload bankPayload = TerminalBankActionPayload.decode(payload);
        TerminalBankingService.ActionResult actionResult = null;

        if (selectedPage.isBankPage() && actionType == TerminalActionType.BANK_OPEN_ACCOUNT) {
            actionResult = bankPageFacade.openOwnAccount(player);
        } else if (selectedPage.isBankPage() && actionType == TerminalActionType.BANK_CONFIRM_TRANSFER) {
            actionResult = bankPageFacade.transferToPlayer(
                player,
                bankPayload.getTargetPlayerName(),
                bankPayload.parseAmount(),
                bankPayload.getComment());
            if (actionResult != null && actionResult.isSuccess()) {
                bankPayload = bankPayload.clearedAfterTransferSuccess();
            }
        } else if (selectedPage.isBankPage()
            && (actionType == TerminalActionType.BANK_REFRESH || actionType == TerminalActionType.REFRESH_PAGE)) {
            actionResult = TerminalBankingService.ActionResult.info("银行页摘要已刷新");
        }

        TerminalBankSnapshot latestSnapshot = bankPageFacade.createSnapshot(player);
        return new BankActionContext(latestSnapshot, bankPayload, actionResult);
    }

    private static MarketActionContext buildMarketActionContext(EntityPlayer player, String sessionToken,
        TerminalPage selectedPage, TerminalActionType actionType, String payload) {
        TerminalMarketActionPayload marketPayload = TerminalMarketActionPayload.decode(payload);
        TerminalCustomMarketActionPayload customPayload = TerminalCustomMarketActionPayload.decode(payload);
        TerminalExchangeMarketActionPayload exchangePayload = TerminalExchangeMarketActionPayload.decode(payload);
        TerminalActionFeedback actionResult = null;

        if (selectedPage == TerminalPage.MARKET_STANDARDIZED
            && actionType == TerminalActionType.MARKET_CONFIRM_DEPOSIT_HELD) {
            actionResult = marketPageFacade.submitDepositHeld(player, marketPayload);
        } else if (selectedPage == TerminalPage.MARKET_STANDARDIZED
            && actionType == TerminalActionType.MARKET_CONFIRM_ORDER) {
            actionResult = submitUnifiedMarketOrder(player, marketPayload);
        } else if (selectedPage == TerminalPage.MARKET_STANDARDIZED
            && actionType == TerminalActionType.MARKET_CONFIRM_LIMIT_BUY) {
            actionResult = marketPageFacade.submitLimitBuy(player, marketPayload);
            if (actionResult != null && actionResult.getSeverity() == TerminalNotificationSeverity.SUCCESS) {
                marketPayload = marketPayload.clearedAfterLimitBuySuccess();
            }
        } else if (selectedPage == TerminalPage.MARKET_STANDARDIZED
            && actionType == TerminalActionType.MARKET_CONFIRM_LIMIT_SELL) {
            actionResult = marketPageFacade.submitLimitSell(player, marketPayload);
            if (actionResult != null && actionResult.getSeverity() == TerminalNotificationSeverity.SUCCESS) {
                marketPayload = marketPayload.clearedAfterLimitSellSuccess();
            }
        } else if (selectedPage == TerminalPage.MARKET_STANDARDIZED
            && actionType == TerminalActionType.MARKET_CONFIRM_INSTANT_BUY) {
            actionResult = marketPageFacade.submitInstantBuy(player, marketPayload);
            if (actionResult != null && actionResult.getSeverity() == TerminalNotificationSeverity.SUCCESS) {
                marketPayload = marketPayload.clearedAfterInstantBuySuccess();
            }
        } else if (selectedPage == TerminalPage.MARKET_STANDARDIZED
            && actionType == TerminalActionType.MARKET_CONFIRM_INSTANT_SELL) {
            actionResult = marketPageFacade.submitInstantSell(player, marketPayload);
            if (actionResult != null && actionResult.getSeverity() == TerminalNotificationSeverity.SUCCESS) {
                marketPayload = marketPayload.clearedAfterInstantSellSuccess();
            }
        } else if ((selectedPage == TerminalPage.MARKET_STANDARDIZED || selectedPage == TerminalPage.MARKET_ACCOUNT_CENTER)
            && actionType == TerminalActionType.MARKET_CANCEL_ORDER) {
            actionResult = marketPageFacade.cancelOrder(player, marketPayload);
            if (actionResult != null && actionResult.getSeverity() == TerminalNotificationSeverity.SUCCESS) {
                marketPayload = marketPayload.clearedAfterCancelSuccess();
            }
        } else if (selectedPage == TerminalPage.MARKET_STANDARDIZED
            && actionType == TerminalActionType.MARKET_CLAIM_ASSET) {
            actionResult = marketPageFacade.claimAsset(player, marketPayload);
            if (actionResult != null && actionResult.getSeverity() == TerminalNotificationSeverity.SUCCESS) {
                marketPayload = marketPayload.clearedAfterClaimSuccess();
            }
        } else if ((selectedPage == TerminalPage.MARKET_STANDARDIZED || selectedPage == TerminalPage.MARKET_ACCOUNT_CENTER)
            && actionType == TerminalActionType.MARKET_REFRESH_HISTORY) {
            actionResult = TerminalActionFeedback.info(
                "个人市场历史已刷新", "订单、成交与撤单状态已按当前筛选重新加载。", 2400L);
        } else if (selectedPage == TerminalPage.MARKET_STANDARDIZED
            && (actionType == TerminalActionType.MARKET_REFRESH || actionType == TerminalActionType.REFRESH_PAGE)) {
            actionResult = TerminalActionFeedback.info("标准商品市场已刷新", "当前商品详情、盘口和待收货摘要已刷新。", 3200L);
        } else if (selectedPage == TerminalPage.MARKET
            && (actionType == TerminalActionType.MARKET_REFRESH || actionType == TerminalActionType.REFRESH_PAGE)) {
            actionResult = TerminalActionFeedback.info("市场总入口已刷新", "市场共享摘要与入口卡已刷新。", 3200L);
        } else if (selectedPage == TerminalPage.MARKET_CUSTOM
            && (actionType == TerminalActionType.MARKET_CUSTOM_REFRESH || actionType == TerminalActionType.MARKET_REFRESH
                || actionType == TerminalActionType.REFRESH_PAGE || actionType == TerminalActionType.MARKET_CUSTOM_SELECT_LISTING)) {
            actionResult = TerminalActionFeedback.info("定制商品市场已刷新", "挂牌目录、商品详情与个人资产摘要已刷新。", 3200L);
        } else if (selectedPage == TerminalPage.MARKET_CUSTOM
            && actionType == TerminalActionType.MARKET_CUSTOM_PUBLISH_HELD) {
            actionResult = marketPageFacade.publishCustomListing(player, customPayload);
        } else if (selectedPage == TerminalPage.MARKET_CUSTOM
            && actionType == TerminalActionType.MARKET_CUSTOM_BUY_LISTING) {
            actionResult = marketPageFacade.purchaseCustomListing(player, customPayload);
        } else if (selectedPage == TerminalPage.MARKET_CUSTOM
            && actionType == TerminalActionType.MARKET_CUSTOM_CANCEL_LISTING) {
            actionResult = marketPageFacade.cancelCustomListing(player, customPayload);
        } else if (selectedPage == TerminalPage.MARKET_CUSTOM
            && actionType == TerminalActionType.MARKET_CUSTOM_CLAIM_LISTING) {
            actionResult = marketPageFacade.claimCustomListing(player, customPayload);
        } else if (selectedPage == TerminalPage.MARKET_EXCHANGE
            && (actionType == TerminalActionType.MARKET_EXCHANGE_SELECT_TARGET
                || actionType == TerminalActionType.MARKET_EXCHANGE_REFRESH_QUOTE
                || actionType == TerminalActionType.MARKET_REFRESH || actionType == TerminalActionType.REFRESH_PAGE)) {
            actionResult = actionType == TerminalActionType.MARKET_EXCHANGE_REFRESH_QUOTE
                ? marketPageFacade.refreshExchangeQuote(player)
                : TerminalActionFeedback.info("汇率市场已刷新", "兑换标的、报价规则、限额与执行条件已刷新。", 3200L);
        } else if (selectedPage == TerminalPage.MARKET_EXCHANGE
            && actionType == TerminalActionType.MARKET_EXCHANGE_CONFIRM) {
            actionResult = confirmExchangeHeld(player, sessionToken, exchangePayload);
        }

        // Custom and exchange retain their own business snapshots. The standardized snapshot is
        // additionally carried as the shared, read-only personal Vault picker source.
        TerminalMarketSectionSnapshot latestSnapshot = selectedPage == TerminalPage.MARKET_CUSTOM
            || selectedPage == TerminalPage.MARKET_EXCHANGE
                ? marketPageFacade.createSnapshot(player, TerminalPage.MARKET_STANDARDIZED,
                    TerminalMarketActionPayload.empty(), null)
                : marketPageFacade.createSnapshot(player, selectedPage, marketPayload, actionResult);
        TerminalCustomMarketSectionSnapshot customSnapshot = selectedPage == TerminalPage.MARKET_CUSTOM
            ? marketPageFacade.createCustomSnapshot(player, customPayload, actionResult)
            : null;
        TerminalExchangeMarketSectionSnapshot exchangeSnapshot = selectedPage == TerminalPage.MARKET_EXCHANGE
            ? marketPageFacade.createExchangeSnapshot(player, exchangePayload, actionResult)
            : null;
        registerExchangeQuoteConfirmation(player, sessionToken, exchangePayload, exchangeSnapshot);
        return new MarketActionContext(latestSnapshot, customSnapshot, exchangeSnapshot, actionResult);
    }

    private static TerminalActionFeedback submitUnifiedMarketOrder(EntityPlayer player,
        TerminalMarketActionPayload payload) {
        if (payload == null || !payload.hasUnifiedOrderTicket()) {
            return TerminalActionFeedback.of(TerminalNotificationSeverity.ERROR, "订单票据无效",
                "请选择买卖方向、订单类型并填写有效数量与价格。", 4800L);
        }
        String productKey = payload.getSelectedProductKey();
        String query = payload.getBrowserQuery();
        String page = String.valueOf(payload.getBrowserPage());
        String filter = payload.getBrowserFilter();
        String quantity = String.valueOf(payload.parseOrderQuantity());
        if ("BUY".equals(payload.getOrderSide())) {
            if ("MARKET".equals(payload.getOrderType())) {
                return marketPageFacade.submitInstantBuy(player, new TerminalMarketActionPayload(productKey,
                    "", "", "", "", "", "", quantity, "", query, page, filter, ""));
            }
            return marketPageFacade.submitLimitBuy(player, new TerminalMarketActionPayload(productKey,
                String.valueOf(payload.parseOrderLimitPrice()), quantity, "", "", "", "", "", "",
                query, page, filter, ""));
        }
        if ("MARKET".equals(payload.getOrderType())) {
            return marketPageFacade.submitInstantSell(player, new TerminalMarketActionPayload(productKey,
                "", "", "", "", "", "", "", quantity, query, page, filter, ""));
        }
        return marketPageFacade.submitLimitSell(player, new TerminalMarketActionPayload(productKey,
            "", "", "", "", String.valueOf(payload.parseOrderLimitPrice()), quantity, "", "",
            query, page, filter, ""));
    }

    private static TerminalActionFeedback confirmExchangeHeld(EntityPlayer player, String sessionToken,
        TerminalExchangeMarketActionPayload exchangePayload) {
        if (!exchangePayload.hasSelectedTarget()) {
            return TerminalActionFeedback.of(TerminalNotificationSeverity.ERROR, "汇率兑换已拒绝",
                "服务端拒绝未选择正式兑换标的的确认请求。", 3600L);
        }
        if (!(player instanceof EntityPlayerMP)) {
            return marketPageFacade.submitExchange(player, exchangePayload);
        }
        TerminalExchangeMarketSectionSnapshot currentQuote = marketPageFacade.createExchangeSnapshot(player,
            exchangePayload, null);
        if (!exchangeQuoteConfirmationGate.consumeIfCurrent(player.getUniqueID().toString(), sessionToken,
            exchangePayload, currentQuote)) {
            return TerminalActionFeedback.of(TerminalNotificationSeverity.WARNING, "正式报价需要重新确认",
                "Base Vault 选中资产、报价规则、限额或终端会话已变化。请刷新正式报价后再次确认兑换。", 4200L);
        }
        return marketPageFacade.submitExchange(player, exchangePayload);
    }

    private static void registerExchangeQuoteConfirmation(EntityPlayer player, String sessionToken,
        TerminalExchangeMarketActionPayload exchangePayload, TerminalExchangeMarketSectionSnapshot exchangeSnapshot) {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }
        exchangeQuoteConfirmationGate.register(player.getUniqueID().toString(), sessionToken, exchangePayload,
            exchangeSnapshot);
    }

    private static ServerToolsActionContext buildServerToolsActionContext(EntityPlayer player, TerminalPage selectedPage,
        TerminalActionType actionType, String payload) {
        TerminalServerToolsActionPayload serverToolsPayload = TerminalServerToolsActionPayload.decode(payload);
        TerminalServerToolsSectionSnapshot.ActionFeedback actionFeedback = null;

        if (selectedPage == TerminalPage.SERVER_TOOLS) {
            if (actionType == TerminalActionType.SERVER_TOOLS_REFRESH || actionType == TerminalActionType.REFRESH_PAGE) {
                actionFeedback = new TerminalServerToolsSectionSnapshot.ActionFeedback(
                    "传送页已刷新",
                    "服务器目录、系统 warp 与最近反馈已刷新。",
                    TerminalNotificationSeverity.INFO.name());
            } else if (actionType == TerminalActionType.SERVER_TOOLS_SELECT_WARP) {
                actionFeedback = new TerminalServerToolsSectionSnapshot.ActionFeedback(
                    "已选择 warp",
                    serverToolsPayload.hasWarpName() ? "当前选中: " + serverToolsPayload.getWarpName() : "当前未选择 warp。",
                    TerminalNotificationSeverity.INFO.name());
            } else if (actionType == TerminalActionType.SERVER_TOOLS_CONFIRM_WARP) {
                if (!(player instanceof EntityPlayerMP)) {
                    actionFeedback = new TerminalServerToolsSectionSnapshot.ActionFeedback(
                        "传送已拒绝",
                        "只有服务端在线玩家可以从终端确认传送。",
                        TerminalNotificationSeverity.ERROR.name());
                } else if (!serverToolsPayload.hasWarpName()) {
                    actionFeedback = new TerminalServerToolsSectionSnapshot.ActionFeedback(
                        "传送已拒绝",
                        "请先选择一个系统 warp。",
                        TerminalNotificationSeverity.ERROR.name());
                } else {
                    actionFeedback = serverToolsPageFacade.confirmWarp((EntityPlayerMP) player,
                        serverToolsPayload.getWarpName());
                }
            } else if (actionType == TerminalActionType.SERVER_TOOLS_CONFIRM_QUICK) {
                if (!(player instanceof EntityPlayerMP)) {
                    actionFeedback = new TerminalServerToolsSectionSnapshot.ActionFeedback(
                        "传送已拒绝",
                        "只有服务端在线玩家可以从终端确认传送。",
                        TerminalNotificationSeverity.ERROR.name());
                } else if (!serverToolsPayload.hasQuickAction()) {
                    actionFeedback = new TerminalServerToolsSectionSnapshot.ActionFeedback(
                        "传送已拒绝",
                        "请选择一个快捷传送动作。",
                        TerminalNotificationSeverity.ERROR.name());
                } else {
                    actionFeedback = serverToolsPageFacade.confirmQuickAction((EntityPlayerMP) player,
                        serverToolsPayload.getQuickAction());
                }
            } else if (actionType == TerminalActionType.SERVER_TOOLS_SELECT_HOME) {
                actionFeedback = new TerminalServerToolsSectionSnapshot.ActionFeedback(
                    "已选择 Home", serverToolsPayload.hasHomeName()
                        ? "当前选中: " + serverToolsPayload.getHomeName() : "当前未选择 Home。",
                    TerminalNotificationSeverity.INFO.name());
            } else if (actionType == TerminalActionType.SERVER_TOOLS_CONFIRM_HOME
                || actionType == TerminalActionType.SERVER_TOOLS_SET_HOME
                || actionType == TerminalActionType.SERVER_TOOLS_DELETE_HOME) {
                if (!(player instanceof EntityPlayerMP)) {
                    actionFeedback = new TerminalServerToolsSectionSnapshot.ActionFeedback(
                        "Home 操作已拒绝", "只有服务端在线玩家可以管理个人 Home。",
                        TerminalNotificationSeverity.ERROR.name());
                } else if (!serverToolsPayload.hasHomeName()) {
                    actionFeedback = new TerminalServerToolsSectionSnapshot.ActionFeedback(
                        "Home 操作已拒绝", "请先输入或选择一个 Home 名称。",
                        TerminalNotificationSeverity.ERROR.name());
                } else if (actionType == TerminalActionType.SERVER_TOOLS_CONFIRM_HOME) {
                    actionFeedback = serverToolsPageFacade.confirmHome((EntityPlayerMP) player,
                        serverToolsPayload.getHomeName());
                } else if (actionType == TerminalActionType.SERVER_TOOLS_SET_HOME) {
                    actionFeedback = serverToolsPageFacade.setHome((EntityPlayerMP) player,
                        serverToolsPayload.getHomeName());
                } else {
                    actionFeedback = serverToolsPageFacade.deleteHome((EntityPlayerMP) player,
                        serverToolsPayload.getHomeName());
                }
            } else if (actionType == TerminalActionType.SERVER_TOOLS_TPA_REQUEST
                || actionType == TerminalActionType.SERVER_TOOLS_TPA_ACCEPT
                || actionType == TerminalActionType.SERVER_TOOLS_TPA_DENY
                || actionType == TerminalActionType.SERVER_TOOLS_TPA_CANCEL) {
                if (!(player instanceof EntityPlayerMP)) {
                    actionFeedback = new TerminalServerToolsSectionSnapshot.ActionFeedback(
                        "TPA 操作已拒绝", "只有服务端在线玩家可以管理 TPA 请求。",
                        TerminalNotificationSeverity.ERROR.name());
                } else if (!serverToolsPayload.hasTpaPlayerName()) {
                    actionFeedback = new TerminalServerToolsSectionSnapshot.ActionFeedback(
                        "TPA 操作已拒绝", "请先输入或选择另一位玩家。", TerminalNotificationSeverity.ERROR.name());
                } else if (actionType == TerminalActionType.SERVER_TOOLS_TPA_REQUEST
                    && !serverToolsPayload.hasTpaTargetServerId()) {
                    actionFeedback = new TerminalServerToolsSectionSnapshot.ActionFeedback(
                        "TPA 操作已拒绝", "请指定目标所在服务器。", TerminalNotificationSeverity.ERROR.name());
                } else if (actionType == TerminalActionType.SERVER_TOOLS_TPA_REQUEST) {
                    actionFeedback = serverToolsPageFacade.createTpa((EntityPlayerMP) player,
                        serverToolsPayload.getTpaPlayerName(), serverToolsPayload.getTpaTargetServerId());
                } else if (actionType == TerminalActionType.SERVER_TOOLS_TPA_ACCEPT) {
                    actionFeedback = serverToolsPageFacade.acceptTpa((EntityPlayerMP) player,
                        serverToolsPayload.getTpaPlayerName());
                } else if (actionType == TerminalActionType.SERVER_TOOLS_TPA_DENY) {
                    actionFeedback = serverToolsPageFacade.denyTpa((EntityPlayerMP) player,
                        serverToolsPayload.getTpaPlayerName());
                } else {
                    actionFeedback = serverToolsPageFacade.cancelTpa((EntityPlayerMP) player,
                        serverToolsPayload.getTpaPlayerName(), serverToolsPayload.getTpaTargetServerId());
                }
            }
        }

        TerminalServerToolsActionPayload snapshotPayload = serverToolsPayload;
        if (actionType == TerminalActionType.SERVER_TOOLS_DELETE_HOME
            && actionFeedback != null
            && TerminalNotificationSeverity.SUCCESS.name().equals(actionFeedback.getSeverityName())) {
            // Do not leave the client focused on a title that has just been removed.
            snapshotPayload = TerminalServerToolsActionPayload.empty();
        }
        TerminalServerToolsSectionSnapshot latestSnapshot =
            serverToolsPageFacade.createSnapshot(player, snapshotPayload, actionFeedback);
        return new ServerToolsActionContext(latestSnapshot, snapshotPayload, actionFeedback);
    }

    private static LandActionContext buildLandActionContext(EntityPlayer player, TerminalPage selectedPage,
        TerminalActionType actionType, String payload) {
        TerminalLandActionPayload landPayload = TerminalLandActionPayload.decode(payload);
        if (selectedPage != TerminalPage.PROPERTY) {
            return new LandActionContext(TerminalLandSectionSnapshot.unavailable(), landPayload);
        }
        LandModule module = resolveLandModule();
        if (module == null || !module.isRuntimeAvailable() || player == null || player.worldObj == null) {
            return new LandActionContext(TerminalLandSectionSnapshot.unavailable(), landPayload);
        }
        String playerRef = player.getUniqueID() == null ? "" : player.getUniqueID().toString();
        int chunkX = ((int) Math.floor(player.posX)) >> 4;
        int chunkZ = ((int) Math.floor(player.posZ)) >> 4;
        TerminalLandSectionSnapshot snapshot = landPageService.createSnapshot(module.getPersonalLandService(),
            module.getLocalServerId(), module.getProtectionRuntime().getMode().name(), module.getMaxClaimsPerPlayer(),
            playerRef, player.worldObj.provider.dimensionId, chunkX, chunkZ, actionType, landPayload);
        return new LandActionContext(snapshot, landPayload);
    }

    private static LandModule resolveLandModule() {
        if (GalaxyBase.proxy == null || GalaxyBase.proxy.getModuleManager() == null) return null;
        return GalaxyBase.proxy.getModuleManager().findModule(LandModule.class);
    }

    private static ItemPolicyModule resolveItemPolicyModule() {
        if (GalaxyBase.proxy == null || GalaxyBase.proxy.getModuleManager() == null) return null;
        return GalaxyBase.proxy.getModuleManager().findModule(ItemPolicyModule.class);
    }

    private static WarehouseModule resolveWarehouseModule() {
        if (GalaxyBase.proxy == null || GalaxyBase.proxy.getModuleManager() == null) return null;
        return GalaxyBase.proxy.getModuleManager().findModule(WarehouseModule.class);
    }

    private static String resolveInstitutionServerId() {
        if (GalaxyBase.proxy == null || GalaxyBase.proxy.getModuleManager() == null) return "";
        InstitutionCoreModule module = GalaxyBase.proxy.getModuleManager().findModule(InstitutionCoreModule.class);
        return module == null ? "" : safeText(module.getBankingSourceServerId(), "");
    }

    private static boolean canOpenTerminal(EntityPlayerMP player) {
        return player != null && player.playerNetServerHandler != null && !player.isDead;
    }

    private static BaseVaultService resolveBaseVaultService() {
        if (GalaxyBase.proxy == null || GalaxyBase.proxy.getModuleManager() == null) {
            return null;
        }
        InstitutionCoreModule module = GalaxyBase.proxy.getModuleManager().findModule(InstitutionCoreModule.class);
        return module == null ? null : module.getBaseVaultService();
    }

    private static String playerRef(EntityPlayer player) {
        return player instanceof EntityPlayerMP ? ((EntityPlayerMP) player).getUniqueID().toString() : "";
    }

    private static String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static List<TerminalOpenApproval.NavItem> createTopLevelNavItems(String selectedPageId) {
        List<TerminalOpenApproval.NavItem> items = new ArrayList<TerminalOpenApproval.NavItem>();
        TerminalPage[] pages = new TerminalPage[] {
            TerminalPage.HOME,
            TerminalPage.CAREER,
            TerminalPage.PUBLIC_SERVICE,
            TerminalPage.MARKET,
            TerminalPage.PROPERTY,
            TerminalPage.SERVER_TOOLS,
            TerminalPage.BANK,
            TerminalPage.NOTIFICATIONS,
            TerminalPage.ITEM_POLICY,
            TerminalPage.WAREHOUSE };
        for (TerminalPage page : pages) {
            items.add(new TerminalOpenApproval.NavItem(
                page.getId(),
                page.getLabel(),
                page.getSubtitle(),
                true,
                page.getId().equalsIgnoreCase(TerminalPage.fromId(selectedPageId).toTopLevelPageId())));
        }
        return items;
    }

    private static List<TerminalOpenApproval.PageSnapshot> createPageSnapshots(EntityPlayer player,
        TerminalHomeSnapshot snapshot, BankActionContext bankContext, MarketActionContext marketContext,
        ServerToolsActionContext serverToolsContext, LandActionContext landContext, TerminalPage selectedPage,
        TerminalActionType actionType, String payload) {
        List<TerminalOpenApproval.PageSnapshot> pageSnapshots = new ArrayList<TerminalOpenApproval.PageSnapshot>();
        pageSnapshots.add(createHomePageSnapshot(snapshot));
        pageSnapshots.add(createCareerPageSnapshot(player, selectedPage, actionType, payload));
        pageSnapshots.add(createPublicServicePageSnapshot(player));
        pageSnapshots.add(createMarketPageSnapshot(selectedPage, marketContext));
        pageSnapshots.add(createBankPageSnapshot(bankContext));
        pageSnapshots.add(createServerToolsPageSnapshot(serverToolsContext));
        pageSnapshots.add(createLandPageSnapshot(landContext));
        pageSnapshots.add(createVaultPageSnapshot(player));
        pageSnapshots.add(createNotificationCenterPageSnapshot(player));
        pageSnapshots.add(createItemPolicyPageSnapshot(player));
        pageSnapshots.add(createWarehousePageSnapshot(player));
        return pageSnapshots;
    }

    private static TerminalOpenApproval.PageSnapshot createNotificationCenterPageSnapshot(EntityPlayer player) {
        List<TerminalOpenApproval.Section> sections = new ArrayList<TerminalOpenApproval.Section>();
        TerminalNotificationCenterSnapshot centerSnapshot = notificationCenter.snapshot(player);
        sections.add(new TerminalOpenApproval.Section("notification_center_runtime", "通知中心",
            centerSnapshot.getServiceState(), "可按来源和等级筛选；点击有定位目标的记录可进入对应工作台。"));
        return new TerminalOpenApproval.PageSnapshot(TerminalPage.NOTIFICATIONS.getId(),
            TerminalPage.NOTIFICATIONS.getTitle(), TerminalPage.NOTIFICATIONS.getLead(), sections,
            null, null, null, null, null, null, centerSnapshot);
    }

    /**
     * A read-only, actor-scoped diagnosis surface. It deliberately never
     * scans inventories, changes items, or exposes another player's records.
     */
    private static TerminalOpenApproval.PageSnapshot createItemPolicyPageSnapshot(EntityPlayer player) {
        List<TerminalOpenApproval.Section> sections = new ArrayList<TerminalOpenApproval.Section>();
        ItemPolicyModule module = resolveItemPolicyModule();
        if (module == null || !module.isRuntimeAvailable()) {
            sections.add(new TerminalOpenApproval.Section("item_policy_disabled", "物品准入策略未启用",
                "当前服务器未启用物品准入策略。", "不会扫描、删除或修改你的既有物品。"));
        } else {
            List<ItemPolicyRule> rules = module.getConfiguredRules();
            sections.add(new TerminalOpenApproval.Section("item_policy_runtime", "当前规则",
                "已启用 " + rules.size() + " 条拒绝规则。", "规则只在正式资产入口执行。"));
            for (ItemPolicyRule rule : rules) {
                String meta = rule.getMeta() == null ? "任意 meta" : "meta " + rule.getMeta();
                sections.add(new TerminalOpenApproval.Section("item_policy_rule_" + rule.getRuleId(),
                    "规则：" + rule.getRuleId(), rule.getRegistryName() + " / " + meta,
                    "适用范围：" + rule.getScopes().toString()));
            }
            String actor = playerRef(player);
            if (actor.isEmpty()) {
                sections.add(new TerminalOpenApproval.Section("item_policy_audit_unavailable", "最近拒绝记录",
                    "当前没有服务端玩家会话。", "请在服务器内打开终端后查看自己的记录。"));
            } else {
                try {
                    List<ItemPolicyAuditRecord> records = module.getAuditQuery().listRecentForActor(
                        resolveInstitutionServerId(), actor, 12);
                    if (records.isEmpty()) {
                        sections.add(new TerminalOpenApproval.Section("item_policy_audit_empty", "最近拒绝记录",
                            "没有你的拒绝记录。", "系统不会显示其他玩家、UUID 或其物品信息。"));
                    } else {
                        for (ItemPolicyAuditRecord record : records) {
                            sections.add(new TerminalOpenApproval.Section("item_policy_denied_" + record.getCreatedAt().toEpochMilli(),
                                "已拒绝：" + record.getRegistryName() + "@" + record.getMeta(),
                                record.getScope() + " / " + record.getOperation(),
                                "原因规则：" + record.getRuleId() + "；时间：" + SERVER_TOOLS_TIME_FORMATTER.format(record.getCreatedAt())));
                        }
                    }
                } catch (RuntimeException exception) {
                    GalaxyBase.LOG.warn("Unable to query item-policy terminal diagnostics", exception);
                    sections.add(new TerminalOpenApproval.Section("item_policy_audit_runtime", "最近拒绝记录暂不可用",
                        "审计数据库当前不可读取。", "策略仍按服务器配置执行；请稍后刷新或联系维护者检查数据库。"));
                }
            }
        }
        return new TerminalOpenApproval.PageSnapshot(TerminalPage.ITEM_POLICY.getId(),
            TerminalPage.ITEM_POLICY.getTitle(), TerminalPage.ITEM_POLICY.getLead(), sections);
    }

    private static TerminalOpenApproval.PageSnapshot createWarehousePageSnapshot(EntityPlayer player) {
        List<TerminalOpenApproval.Section> sections = new ArrayList<TerminalOpenApproval.Section>();
        WarehouseModule module = resolveWarehouseModule();
        if (!(player instanceof EntityPlayerMP)) {
            sections.add(new TerminalOpenApproval.Section("warehouse_unavailable", "银河仓储", "当前没有服务端仓储会话。",
                "请在已连接服务器内刷新；本页不连接外部 AE 网络。"));
        } else if (module == null || !module.isRuntimeAvailable() || module.getTerminalBayService() == null) {
            sections.add(new TerminalOpenApproval.Section("warehouse_runtime_unavailable", "银河仓储未启用",
                "个人存储单元 Bay 当前未就绪。", "管理员启用 warehouseEnabled 后必须先应用对应 PostgreSQL migration；Base Vault 保持独立可用。"));
        } else {
            String owner = playerRef(player);
            BaseVaultService vaultService = resolveBaseVaultService();
            if (vaultService != null) {
                BaseVaultService.VaultView view = vaultService.viewPersonalVault(owner); int occupied = 0; long items = 0L;
                for (VaultSlot slot : view.getSlots()) if (slot != null && slot.getStack() != null && slot.getStack().stackSize > 0) { occupied++; items += slot.getStack().stackSize; }
                sections.add(new TerminalOpenApproval.Section("warehouse_base_vault", "Base Vault", "占用 " + occupied + " / " + view.getAccount().getSlotCount() + " 格 | 实体 " + items,
                    "独立的跨服资产账本；不会暴露为 AE2 存储，也不会被 Cell 复制。"));
            }
            TerminalWarehouseBayService bayService = module.getTerminalBayService(); TerminalWarehouseBay bay = bayService.view(owner);
            if (!bay.hasCell()) {
                sections.add(new TerminalOpenApproval.Section("warehouse_bay", "AE2 存储单元", "Bay 为空 · 0 / 1",
                    "点击“插入存储单元”后从背包放入一枚真实 AE2 Cell；不需要放置任何方块。"));
            } else {
                TerminalWarehouseCellInspector.CellFacts facts = TerminalWarehouseCellInspector.inspect(bay.getCell());
                String name = bay.getCell().getDisplayName();
                sections.add(new TerminalOpenApproval.Section("warehouse_bay", "AE2 存储单元 · " + name,
                    "已插入 · " + facts.getUsedBytes() + " / " + facts.getTotalBytes() + " Bytes | 物品 " + facts.getStoredItems(),
                    "类型 " + facts.getStoredTypes() + " / " + facts.getTotalTypes() + " | Bay 版本 " + bay.getVersion()
                        + "。容量与内容由 Cell 决定；无频道、供电或外部 ME 网络状态。"));
            }
            List<TerminalWarehouseBayReceipt> recent = bayService.listRecent(owner, 6);
            if (recent.isEmpty()) {
                sections.add(new TerminalOpenApproval.Section("warehouse_audit_empty", "最近操作", "暂无个人存储单元操作审计。",
                    "插入、取出与版本冲突都会由服务器记录；Cell 内容不复制到操作日志。"));
            } else {
                for (TerminalWarehouseBayReceipt receipt : recent) {
                    sections.add(new TerminalOpenApproval.Section("warehouse_audit_" + receipt.getRequestId(), "审计：存储单元变更",
                        receipt.getResult().name() + " | 版本 " + receipt.getBefore().getVersion() + " → " + receipt.getAfter().getVersion(), receipt.getDetail()));
                }
            }
        }
        return new TerminalOpenApproval.PageSnapshot(TerminalPage.WAREHOUSE.getId(), TerminalPage.WAREHOUSE.getTitle(),
            TerminalPage.WAREHOUSE.getLead(), sections);
    }

    private static TerminalOpenApproval.PageSnapshot createLandPageSnapshot(LandActionContext context) {
        TerminalLandSectionSnapshot snapshot = context == null || context.snapshot == null
            ? TerminalLandSectionSnapshot.unavailable() : context.snapshot;
        List<TerminalOpenApproval.Section> sections = new ArrayList<TerminalOpenApproval.Section>();
        sections.add(new TerminalOpenApproval.Section("property_runtime", "个人地产",
            snapshot.getServiceState(), snapshot.getServerId() + " / " + snapshot.getProtectionMode()));
        return new TerminalOpenApproval.PageSnapshot(TerminalPage.PROPERTY.getId(), TerminalPage.PROPERTY.getTitle(),
            TerminalPage.PROPERTY.getLead(), sections, null, null, null, null, null, snapshot);
    }

    private static TerminalOpenApproval.PageSnapshot createVaultPageSnapshot(EntityPlayer player) {
        List<TerminalOpenApproval.Section> sections = new ArrayList<TerminalOpenApproval.Section>();
        if (!(player instanceof EntityPlayerMP)) {
            sections.add(new TerminalOpenApproval.Section("vault_unavailable", "Base Vault", "当前没有服务端仓库会话。",
                "请在已连接的服务器内打开终端。"));
        } else {
            BaseVaultService vaultService = resolveBaseVaultService();
            if (vaultService == null) {
                sections.add(new TerminalOpenApproval.Section("vault_runtime_unavailable", "Base Vault 未就绪",
                    "仓储 PostgreSQL 运行时尚未启动。", "管理员需先应用仓储迁移并检查服务端启动日志。"));
            } else {
                try {
                    BaseVaultService.VaultView view = vaultService.viewPersonalVault(playerRef(player));
                    int occupied = 0;
                    int itemCount = 0;
                    for (VaultSlot slot : view.getSlots()) {
                        if (slot != null && slot.getStack() != null && slot.getStack().stackSize > 0) {
                            occupied++;
                            itemCount += slot.getStack().stackSize;
                        }
                    }
                    sections.add(new TerminalOpenApproval.Section("vault_personal_capacity", "个人 Base Vault",
                        "占用 " + occupied + " / " + view.getAccount().getSlotCount() + " 格 | 实体 " + itemCount,
                        "市场领取、定制市场领取和标准商品托管都以此账户为实体来源或目标。"));
                    sections.add(new TerminalOpenApproval.Section("vault_transfer_boundary", "跨服资产边界",
                        "个人仓为跨服持久资产；市场托管、订单冻结与银行余额保持独立。",
                        "背包存取仅会由专用 Vault 格位交互发起，并记录可恢复 request id 操作。"));
                } catch (RuntimeException exception) {
                    sections.add(new TerminalOpenApproval.Section("vault_runtime_error", "Base Vault 不可用",
                        "无法读取个人保险箱。", "原因：" + safeText(exception.getMessage(), "仓储运行时异常")));
                }
            }
        }
        return new TerminalOpenApproval.PageSnapshot(TerminalPage.VAULT.getId(), TerminalPage.VAULT.getTitle(),
            TerminalPage.VAULT.getLead(), sections);
    }

    private static TerminalOpenApproval.PageSnapshot createHomePageSnapshot(TerminalHomeSnapshot snapshot) {
        List<TerminalOpenApproval.Section> sections = new ArrayList<TerminalOpenApproval.Section>();
        sections.add(new TerminalOpenApproval.Section(
            "career_status",
            "职业与声望",
            TerminalOpenSummaryFormatter.buildCareerSectionSummary(snapshot),
            "下一阶段会把职业等级、资格和制度权限接到同一首页壳的 section 宿主上。"));
        sections.add(new TerminalOpenApproval.Section(
            "public_service",
            "公共任务",
            TerminalOpenSummaryFormatter.buildPublicServiceSectionSummary(snapshot),
            "公共任务、福利和公共服务入口仍保留为只读摘要，后续直接挂到这套主内容区。"));
        sections.add(new TerminalOpenApproval.Section(
            "market_overview",
            "市场总览",
            TerminalOpenSummaryFormatter.buildMarketSectionSummary(snapshot),
            "MARKET 总入口、标准商品、定制商品与汇率市场都已迁入新壳。"));
        sections.add(new TerminalOpenApproval.Section(
            "bank_migration_state",
            "终端迁移状态",
            "BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE 都已作为正式业务页迁入新壳。",
            "银行与三类市场均已接入终端操作、确认和状态刷新流程。"));
        return new TerminalOpenApproval.PageSnapshot(
            TerminalPage.HOME.getId(),
            TerminalPage.HOME.getTitle(),
            TerminalPage.HOME.getLead(),
            sections);
    }

    private static TerminalOpenApproval.PageSnapshot createCareerPageSnapshot(EntityPlayer player,
        TerminalPage selectedPage,TerminalActionType actionType,String payload) {
        TerminalQuestCenterSectionSnapshot questSnapshot=TerminalQuestCenterSectionSnapshot.unavailable(
            "跨服任务运行时尚未启用；现有 BetterQuesting 不受影响。");
        if(selectedPage==TerminalPage.CAREER&&player instanceof EntityPlayerMP&&questCenterQuery!=null){
            try{
                if(actionType==TerminalActionType.QUEST_ADMIN_EDIT)questSnapshot=buildAuthenticatedQuestDraftEdit(
                    ((EntityPlayerMP)player).getUniqueID(),player.getCommandSenderName(),TerminalQuestDraftEditPayload.decode(payload));
                else if(actionType==TerminalActionType.QUEST_ADMIN_BATCH_RETIRE)questSnapshot=buildAuthenticatedQuestBatchRetire(
                    ((EntityPlayerMP)player).getUniqueID(),player.getCommandSenderName(),TerminalQuestBatchRetirePayload.decode(payload));
                else if(actionType==TerminalActionType.QUEST_CHAPTER_ADMIN_EDIT)questSnapshot=buildAuthenticatedQuestChapterEdit(
                    ((EntityPlayerMP)player).getUniqueID(),player.getCommandSenderName(),TerminalQuestChapterEditPayload.decode(payload));
                else if(actionType==TerminalActionType.QUEST_CHAPTER_ADMIN_CLONE)questSnapshot=buildAuthenticatedQuestChapterClone(
                    ((EntityPlayerMP)player).getUniqueID(),player.getCommandSenderName(),TerminalQuestChapterClonePayload.decode(payload));
                else if(actionType==TerminalActionType.QUEST_CHAPTER_ADMIN_ALIGN)questSnapshot=buildAuthenticatedQuestChapterAlign(
                    ((EntityPlayerMP)player).getUniqueID(),player.getCommandSenderName(),TerminalQuestChapterAlignmentPayload.decode(payload));
                else if(actionType==TerminalActionType.QUEST_CHAPTER_ADMIN_MOVE_BEFORE)questSnapshot=buildAuthenticatedQuestChapterMoveBefore(
                    ((EntityPlayerMP)player).getUniqueID(),player.getCommandSenderName(),TerminalQuestChapterOrderPayload.decode(payload));
                else questSnapshot=buildAuthenticatedQuestCenterSnapshot(((EntityPlayerMP)player).getUniqueID(),
                    player.getCommandSenderName(),actionType,TerminalQuestActionPayload.decode(payload));
            }catch(RuntimeException failure){
                GalaxyBase.LOG.error("Unable to build authenticated quest-center snapshot for {}",
                    player.getCommandSenderName(),failure);
                questSnapshot=TerminalQuestCenterSectionSnapshot.unavailable("跨服任务暂时不可读取，请稍后刷新。");
            }
        }
        List<TerminalOpenApproval.Section> sections=new ArrayList<TerminalOpenApproval.Section>();
        sections.add(new TerminalOpenApproval.Section("quest_center_runtime","跨服任务中心",
            questSnapshot.getServiceState(),questSnapshot.getMessage()));
        return new TerminalOpenApproval.PageSnapshot(TerminalPage.CAREER.getId(),TerminalPage.CAREER.getTitle(),
            TerminalPage.CAREER.getLead(),sections,null,null,null,null,null,null,null,questSnapshot);
    }

    static TerminalQuestCenterSectionSnapshot buildAuthenticatedQuestDraftEdit(UUID playerId,String playerName,
        TerminalQuestDraftEditPayload payload){
        if(playerId==null||payload==null)return TerminalQuestCenterSectionSnapshot.unavailable("任务编辑请求无效。");
        QuestDraftEditRequest request=payload.getRequest();QuestEditorActor actor=new QuestEditorActor(playerId,playerName);
        QuestDraftManagementResult result=questDraftManagementService==null
            ?QuestDraftManagementResult.status(QuestDraftManagementResult.Status.NOT_FOUND)
            :questDraftManagementService.applyEdit(actor,request);
        String message=questDraftManagementService==null?"任务定义写入运行时尚未启用，草稿没有改变。"
            :managementMutationMessage(TerminalActionType.QUEST_ADMIN_EDIT,result);
        String hash=result.isSuccess()?result.getDefinition().getContentHash():request.getExpectedContentHash();
        TerminalQuestActionPayload intent=new TerminalQuestActionPayload("",request.getQuestId().toString(),"all",
            payload.getQuery(),0,0,"",-1,payload.getLifecycle(),payload.getPage(),request.getVersion(),hash);
        if(questDefinitionManagementQuery==null)return TerminalQuestCenterSectionSnapshot.unavailable(message);
        return buildQuestManagementSnapshot(actor,intent,null,message);
    }

    static TerminalQuestCenterSectionSnapshot buildAuthenticatedQuestBatchRetire(UUID playerId,String playerName,
        TerminalQuestBatchRetirePayload payload){
        if(playerId==null||payload==null)return TerminalQuestCenterSectionSnapshot.unavailable("批量退役请求无效。");
        QuestDefinitionBatchRetirementResult result=questBatchRetirementService==null
            ?QuestDefinitionBatchRetirementResult.status(QuestDefinitionBatchRetirementResult.Status.NOT_FOUND)
            :questBatchRetirementService.retire(new QuestEditorActor(playerId,playerName),payload.getRequest());
        String message=batchRetirementMessage(result);
        return buildQuestManagementSnapshot(new QuestEditorActor(playerId,playerName),new TerminalQuestActionPayload(
            "","","",payload.getQuery(),0,0,"",-1,payload.getLifecycle(),payload.getPage()),null,message);
    }

    private static String batchRetirementMessage(QuestDefinitionBatchRetirementResult result){
        if(result==null)return "批量退役没有返回结果。";
        if(result.getStatus()==QuestDefinitionBatchRetirementResult.Status.SUCCESS)return "已原子退役 "+result.getRetired().size()+" 个任务版本。";
        if(result.getStatus()==QuestDefinitionBatchRetirementResult.Status.FORBIDDEN)return "当前玩家没有管理任务定义的权限。";
        if(result.getStatus()==QuestDefinitionBatchRetirementResult.Status.UNSAFE){QuestDefinitionBatchImpactReport impact=result.getImpact();return "批量退役被阻止：外部依赖 "+impact.getExternalDependents().size()+"，章节放置 "+impact.getPlacements().size()+"。";}
        if(result.getStatus()==QuestDefinitionBatchRetirementResult.Status.NOT_FOUND)return "所选任务版本不存在或不是已发布版本。";
        if(result.getStatus()==QuestDefinitionBatchRetirementResult.Status.CONFLICT)return "任务版本已变化，批量退役已回滚，请刷新后重试。";
        return "批量退役请求无效，状态没有改变。";
    }

    static TerminalQuestCenterSectionSnapshot buildAuthenticatedQuestChapterEdit(UUID playerId,String playerName,
        TerminalQuestChapterEditPayload payload){
        if(playerId==null||payload==null)return TerminalQuestCenterSectionSnapshot.unavailable("章节编辑请求无效。");
        QuestEditorActor actor=new QuestEditorActor(playerId,playerName);
        QuestChapterManagementResult result=questChapterManagementService==null
            ?QuestChapterManagementResult.status(QuestChapterManagementResult.Status.NOT_FOUND)
            :questChapterManagementService.apply(actor,payload.getRequest());
        String message=questChapterManagementService==null?"章节写入运行时尚未启用，草稿没有改变。"
            :chapterMutationMessage(TerminalActionType.QUEST_CHAPTER_ADMIN_EDIT,result);
        String hash=result.isSuccess()?result.getChapter().getContentHash():payload.getRequest().getExpectedHash();
        TerminalQuestActionPayload intent=new TerminalQuestActionPayload(payload.getRequest().getChapterId().toString(),
            "","all",payload.getQuery(),0,0,"",-1,payload.getLifecycle(),payload.getPage(),
            payload.getRequest().getVersion(),hash);
        return buildQuestChapterManagementSnapshot(actor,intent,null,message);
    }

    static TerminalQuestCenterSectionSnapshot buildAuthenticatedQuestChapterClone(UUID playerId,String playerName,
        TerminalQuestChapterClonePayload payload){
        if(playerId==null||payload==null)return TerminalQuestCenterSectionSnapshot.unavailable("任务复制请求无效。");
        QuestEditorActor actor=new QuestEditorActor(playerId,playerName);
        QuestChapterCloneResult result=questChapterCloneService==null
            ?QuestChapterCloneResult.status(QuestChapterCloneResult.Status.NOT_FOUND)
            :questChapterCloneService.cloneIntoChapter(actor,payload.getRequest());
        String message=questChapterCloneService==null?"任务复制运行时尚未启用，章节没有改变。"
            :chapterCloneMessage(result);
        String hash=result.isSuccess()?result.getChapter().getContentHash():payload.getRequest().getExpectedChapterHash();
        TerminalQuestActionPayload intent=new TerminalQuestActionPayload(payload.getRequest().getChapterId().toString(),
            "","all",payload.getQuery(),0,0,"",-1,payload.getLifecycle(),payload.getPage(),
            payload.getRequest().getChapterVersion(),hash);
        return buildQuestChapterManagementSnapshot(actor,intent,null,message);
    }

    static TerminalQuestCenterSectionSnapshot buildAuthenticatedQuestChapterMoveBefore(UUID playerId,String playerName,
        TerminalQuestChapterOrderPayload payload){
        if(playerId==null||payload==null)return TerminalQuestCenterSectionSnapshot.unavailable("章节排序请求无效。");
        QuestChapterOrderingResult result=questChapterOrderingService==null
            ?QuestChapterOrderingResult.status(QuestChapterOrderingResult.Status.INVALID,"runtime unavailable")
            :questChapterOrderingService.moveBefore(new QuestEditorActor(playerId,playerName),payload.getChapterId(),payload.getBeforeChapterId());
        String message=result.getStatus()==QuestChapterOrderingResult.Status.SUCCESS?"章节目录顺序已更新。"
            :result.getStatus()==QuestChapterOrderingResult.Status.FORBIDDEN?"当前玩家没有排序章节的权限。"
            :"章节顺序已变化或请求不完整，请刷新后重试。";
        return buildQuestChapterManagementSnapshot(new QuestEditorActor(playerId,playerName),new TerminalQuestActionPayload("","","all",
            payload.getQuery(),0,0,"",-1,payload.getLifecycle(),payload.getPage()),null,message);
    }

    static TerminalQuestCenterSectionSnapshot buildAuthenticatedQuestChapterAlign(UUID playerId,String playerName,
        TerminalQuestChapterAlignmentPayload payload){
        if(playerId==null||payload==null)return TerminalQuestCenterSectionSnapshot.unavailable("章节对齐请求无效。");
        QuestChapterManagementResult result=questChapterAlignmentService==null
            ?QuestChapterManagementResult.status(QuestChapterManagementResult.Status.NOT_FOUND)
            :questChapterAlignmentService.align(new QuestEditorActor(playerId,playerName),payload.getRequest());
        String message=questChapterAlignmentService==null?"章节对齐运行时尚未启用，草稿没有改变。"
            :chapterMutationMessage(TerminalActionType.QUEST_CHAPTER_ADMIN_ALIGN,result);
        String hash=result.isSuccess()?result.getChapter().getContentHash():payload.getRequest().getExpectedHash();
        return buildQuestChapterManagementSnapshot(new QuestEditorActor(playerId,playerName),new TerminalQuestActionPayload(
            payload.getRequest().getChapterId().toString(),"","all",payload.getQuery(),0,0,"",-1,payload.getLifecycle(),
            payload.getPage(),payload.getRequest().getVersion(),hash),null,message);
    }

    private static boolean isChapterManagementAction(TerminalActionType action){return action==TerminalActionType.QUEST_CHAPTER_ADMIN_OPEN
        ||action==TerminalActionType.QUEST_CHAPTER_ADMIN_FILTER||action==TerminalActionType.QUEST_CHAPTER_ADMIN_PAGE
        ||action==TerminalActionType.QUEST_CHAPTER_ADMIN_SELECT||action==TerminalActionType.QUEST_CHAPTER_ADMIN_CREATE
        ||action==TerminalActionType.QUEST_CHAPTER_ADMIN_PUBLISH||action==TerminalActionType.QUEST_CHAPTER_ADMIN_RETIRE;}

    private static TerminalQuestCenterSectionSnapshot buildQuestChapterManagementSnapshot(QuestEditorActor actor,
        TerminalQuestActionPayload intent,TerminalActionType actionType,String initialMessage){
        if(questChapterManagementQuery==null)return TerminalQuestCenterSectionSnapshot.unavailable(
            "章节编辑管理运行时尚未启用。");
        String message=initialMessage;
        if(actionType==TerminalActionType.QUEST_CHAPTER_ADMIN_CREATE){
            QuestChapterManagementResult result=questChapterManagementService==null
                ?QuestChapterManagementResult.status(QuestChapterManagementResult.Status.NOT_FOUND)
                :questChapterManagementService.create(actor,intent.getQuery());
            message=chapterMutationMessage(actionType,result);
            intent=new TerminalQuestActionPayload("","","all","",0,0,"",-1,"DRAFT",0,0,"");
        }else if(actionType==TerminalActionType.QUEST_CHAPTER_ADMIN_PUBLISH
            ||actionType==TerminalActionType.QUEST_CHAPTER_ADMIN_RETIRE){
            try{
                UUID id=UUID.fromString(intent.getChapterId());
                QuestChapterManagementResult result=questChapterManagementService==null
                    ?QuestChapterManagementResult.status(QuestChapterManagementResult.Status.NOT_FOUND)
                    :actionType==TerminalActionType.QUEST_CHAPTER_ADMIN_PUBLISH
                        ?questChapterManagementService.publish(actor,id,intent.getManagementVersion(),
                            intent.getManagementHash(),System.currentTimeMillis())
                        :questChapterManagementService.retire(actor,id,intent.getManagementVersion());
                message=chapterMutationMessage(actionType,result);
            }catch(IllegalArgumentException invalid){message="章节标识无效，状态没有改变。";}
        }
        QuestDefinitionLifecycle lifecycle=null;
        if(!intent.getManagementLifecycle().isEmpty())try{lifecycle=QuestDefinitionLifecycle.valueOf(
            intent.getManagementLifecycle().toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ignored){lifecycle=null;}
        com.jsirgalaxybase.quest.core.AuthenticatedQuestChapterManagementQuery.Result queried=
            questChapterManagementQuery.load(actor,new QuestDefinitionManagementRequest(intent.getQuery(),lifecycle,
                intent.getManagementPage(),20));
        if(queried.getStatus()!=com.jsirgalaxybase.quest.core.AuthenticatedQuestChapterManagementQuery.Status.SUCCESS)
            return TerminalQuestCenterSectionSnapshot.unavailable("只有服务端授权的任务管理员可以管理章节。");
        QuestChapterManagementPage page=queried.getPage();
        List<TerminalQuestCenterSectionSnapshot.ManagedChapter> rows=new ArrayList<TerminalQuestCenterSectionSnapshot.ManagedChapter>();
        for(StoredQuestChapter stored:page.getChapters())rows.add(managedChapter(stored));
        TerminalQuestCenterSectionSnapshot.ChapterDetail detail=null;
        if(!intent.getChapterId().isEmpty()&&intent.getManagementVersion()>0)try{
            com.jsirgalaxybase.quest.core.AuthenticatedQuestChapterManagementQuery.Result found=
                questChapterManagementQuery.find(actor,UUID.fromString(intent.getChapterId()),intent.getManagementVersion());
            if(found.getStatus()==com.jsirgalaxybase.quest.core.AuthenticatedQuestChapterManagementQuery.Status.SUCCESS)
                detail=chapterDetail(actor,found.getChapter());
            else if(found.getStatus()==com.jsirgalaxybase.quest.core.AuthenticatedQuestChapterManagementQuery.Status.NOT_FOUND)
                message="所选章节版本不存在。";
        }catch(IllegalArgumentException invalid){message="所选章节标识无效。";}
        TerminalQuestCenterSectionSnapshot.ChapterManagement management=
            new TerminalQuestCenterSectionSnapshot.ChapterManagement(intent.getQuery(),lifecycle==null?"":lifecycle.name(),
                page.getPage(),page.getPageSize(),page.getTotal(),rows,detail);
        return new TerminalQuestCenterSectionSnapshot("READY",message,TerminalQuestCenterSectionSnapshot.View.ADMIN,
            "","","all",intent.getQuery(),java.util.Collections.<TerminalQuestCenterSectionSnapshot.Chapter>emptyList(),
            0,6,0,java.util.Collections.<TerminalQuestCenterSectionSnapshot.Quest>emptyList(),0,7,0,null,null,management);
    }

    private static TerminalQuestCenterSectionSnapshot.ManagedChapter managedChapter(StoredQuestChapter stored){
        com.jsirgalaxybase.quest.core.QuestChapterDefinition value=stored.getDefinition();
        return new TerminalQuestCenterSectionSnapshot.ManagedChapter(value.getId().toString(),value.getVersion(),
            value.getName(),stored.getLifecycle().name(),stored.getContentHash(),stored.getPublishedAt(),value.getEntries().size());
    }

    private static TerminalQuestCenterSectionSnapshot.ChapterDetail chapterDetail(QuestEditorActor actor,
        StoredQuestChapter stored){
        java.util.Map<String,String> names=new java.util.LinkedHashMap<String,String>();
        List<TerminalQuestCenterSectionSnapshot.ChapterCandidate> candidates=new ArrayList<TerminalQuestCenterSectionSnapshot.ChapterCandidate>();
        if(questDefinitionManagementQuery!=null){AuthenticatedQuestDefinitionManagementQuery.Result definitions=
            questDefinitionManagementQuery.load(actor,new QuestDefinitionManagementRequest("",
                QuestDefinitionLifecycle.PUBLISHED,0,50));
            if(definitions.getStatus()==AuthenticatedQuestDefinitionManagementQuery.Status.SUCCESS)
                for(StoredQuestDefinition value:definitions.getPage().getDefinitions()){
                    String id=value.getDefinition().getId().toString();names.put(id,value.getDefinition().getName());
                    candidates.add(new TerminalQuestCenterSectionSnapshot.ChapterCandidate(id,value.getDefinition().getName()));
                }
        }
        List<TerminalQuestCenterSectionSnapshot.ChapterPlacement> placements=new ArrayList<TerminalQuestCenterSectionSnapshot.ChapterPlacement>();
        for(QuestChapterEntry entry:stored.getDefinition().getEntries()){
            String id=entry.getQuestId().toString();String name=names.get(id);
            placements.add(new TerminalQuestCenterSectionSnapshot.ChapterPlacement(id,name==null?id:name,entry.getX(),
                entry.getY(),entry.getWidth(),entry.getHeight()));
            for(int i=candidates.size()-1;i>=0;i--)if(candidates.get(i).getQuestId().equals(id))candidates.remove(i);
        }
        com.jsirgalaxybase.quest.core.QuestChapterDefinition value=stored.getDefinition();
        List<TerminalQuestCenterSectionSnapshot.ChapterDependency> dependencies=new ArrayList<TerminalQuestCenterSectionSnapshot.ChapterDependency>();
        if(questChapterDependencyQuery!=null){AuthenticatedQuestChapterDependencyQuery.Result projection=
            questChapterDependencyQuery.load(actor,value.getId(),value.getVersion());
            if(projection.getStatus()==AuthenticatedQuestChapterDependencyQuery.Status.SUCCESS)
                for(QuestChapterDependencyGraph.Edge edge:projection.getGraph().getEdges())dependencies.add(
                    new TerminalQuestCenterSectionSnapshot.ChapterDependency(edge.getPrerequisiteId().toString(),
                        edge.getQuestId().toString(),edge.getKind().name()));
        }
        return new TerminalQuestCenterSectionSnapshot.ChapterDetail(managedChapter(stored),value.getDescription(),
            value.getIconReference(),value.getBackgroundReference(),value.getBackgroundSize(),value.getVisibility().name(),
            placements,candidates,dependencies);
    }

    private static String chapterMutationMessage(TerminalActionType action,QuestChapterManagementResult result){
        if(result==null)return "章节操作没有返回结果。";
        if(result.getStatus()==QuestChapterManagementResult.Status.SUCCESS)
            return action==TerminalActionType.QUEST_CHAPTER_ADMIN_CREATE?"章节草稿已创建。"
                :action==TerminalActionType.QUEST_CHAPTER_ADMIN_PUBLISH?"章节版本已发布。"
                :action==TerminalActionType.QUEST_CHAPTER_ADMIN_RETIRE?"章节版本已退役。":"章节草稿已保存。";
        if(result.getStatus()==QuestChapterManagementResult.Status.FORBIDDEN)return "当前玩家没有管理章节的权限。";
        if(result.getStatus()==QuestChapterManagementResult.Status.NOT_FOUND)return "所选章节版本不存在。";
        if(result.getStatus()==QuestChapterManagementResult.Status.CONFLICT)return "章节版本已经变化，请刷新后重试。";
        return result.getMessage().isEmpty()?"章节校验失败，状态没有改变。":safeBounded(result.getMessage(),256);
    }

    private static String chapterCloneMessage(QuestChapterCloneResult result){
        if(result.getStatus()==QuestChapterCloneResult.Status.SUCCESS)return "已复制 "+result.getRemappedIds().size()+" 个任务草稿。";
        if(result.getStatus()==QuestChapterCloneResult.Status.FORBIDDEN)return "当前玩家没有复制章节任务的权限。";
        if(result.getStatus()==QuestChapterCloneResult.Status.NOT_FOUND)return "章节或待复制任务版本不存在。";
        if(result.getStatus()==QuestChapterCloneResult.Status.CONFLICT)return "章节版本已经变化，请刷新后重试。";
        return "任务复制数据无效，章节没有改变。";
    }

    static TerminalQuestCenterSectionSnapshot buildAuthenticatedQuestCenterSnapshot(UUID authenticatedPlayerId,
        TerminalActionType actionType,TerminalQuestActionPayload intent) {
        return buildAuthenticatedQuestCenterSnapshot(authenticatedPlayerId,"",actionType,intent);
    }

    static TerminalQuestCenterSectionSnapshot buildAuthenticatedQuestCenterSnapshot(UUID authenticatedPlayerId,
        String authenticatedPlayerName,TerminalActionType actionType,TerminalQuestActionPayload intent) {
        if(authenticatedPlayerId==null||questCenterQuery==null)return TerminalQuestCenterSectionSnapshot.unavailable(
            "跨服任务运行时尚未启用；现有 BetterQuesting 不受影响。");
        TerminalQuestActionPayload safeIntent=intent==null?TerminalQuestActionPayload.empty():intent;
        if(isChapterManagementAction(actionType))return buildQuestChapterManagementSnapshot(
            new QuestEditorActor(authenticatedPlayerId,authenticatedPlayerName),safeIntent,actionType,"章节由 PostgreSQL 跨服真源管理。");
        if(actionType==TerminalActionType.QUEST_ADMIN_OPEN||actionType==TerminalActionType.QUEST_ADMIN_FILTER
            ||actionType==TerminalActionType.QUEST_ADMIN_PAGE||actionType==TerminalActionType.QUEST_ADMIN_SELECT||actionType==TerminalActionType.QUEST_ADMIN_CREATE
            ||actionType==TerminalActionType.QUEST_ADMIN_PUBLISH||actionType==TerminalActionType.QUEST_ADMIN_RETIRE
            ||actionType==TerminalActionType.QUEST_ADMIN_BATCH_RETIRE)return buildQuestManagementSnapshot(authenticatedPlayerId,
                authenticatedPlayerName,safeIntent,actionType);
        QuestCenterPageRequest request=new QuestCenterPageRequest(safeIntent.getChapterId(),safeIntent.getFilter(),
            safeIntent.getQuery(),safeIntent.getChapterPage(),6,safeIntent.getQuestPage(),7);
        ParticipantId participant=ParticipantId.player(authenticatedPlayerId);
        String message="";
        if(actionType==TerminalActionType.QUEST_CLAIM){
            if(safeIntent.getQuestId().isEmpty())message="未选择要领取奖励的任务。";
            else if(questClaimService==null)message="奖励领取运行时尚未启用，任务状态没有改变。";
            else try{message=claimMessage(questClaimService.claim(authenticatedPlayerId,
                UUID.fromString(safeIntent.getQuestId()),System.currentTimeMillis()));}
            catch(IllegalArgumentException invalid){message="任务标识无效，奖励状态没有改变。";}
        }else if(actionType==TerminalActionType.QUEST_SELECT_REWARD_CHOICE){
            if(safeIntent.getQuestId().isEmpty()||safeIntent.getRewardKey().isEmpty()||safeIntent.getChoiceIndex()<0)
                message="奖励选择无效，任务状态没有改变。";
            else if(rewardChoiceService==null)message="奖励选择运行时尚未启用，任务状态没有改变。";
            else try{message=choiceMessage(rewardChoiceService.select(authenticatedPlayerId,
                UUID.fromString(safeIntent.getQuestId()),safeIntent.getRewardKey(),safeIntent.getChoiceIndex(),System.currentTimeMillis()));}
            catch(IllegalArgumentException invalid){message="奖励选择无效，任务状态没有改变。";}
        }else if(actionType==TerminalActionType.QUEST_TOGGLE_TRACKING){
            if(safeIntent.getQuestId().isEmpty())message="未选择要追踪的任务。";
            else if(questTrackingService==null)message="任务追踪运行时尚未启用。";
            else try{java.util.Optional<Boolean> tracked=questTrackingService.toggle(authenticatedPlayerId,
                UUID.fromString(safeIntent.getQuestId()),System.currentTimeMillis());message=!tracked.isPresent()?"任务不存在或尚未发布。":tracked.get().booleanValue()?"已加入跨服任务追踪。":"已取消任务追踪。";}
            catch(IllegalArgumentException invalid){message="任务标识无效，追踪状态没有改变。";}
        }
        QuestCenterPage page=questCenterQuery.loadPage(participant,request);
        boolean detail=!safeIntent.getQuestId().isEmpty()&&actionType!=TerminalActionType.QUEST_BACK;
        return questCenterMapper.map(page,safeIntent.getFilter(),safeIntent.getQuery(),safeIntent.getQuestId(),detail,message);
    }

    private static TerminalQuestCenterSectionSnapshot buildQuestManagementSnapshot(UUID playerId,String playerName,
        TerminalQuestActionPayload intent,TerminalActionType actionType){
        if(questDefinitionManagementQuery==null)return TerminalQuestCenterSectionSnapshot.unavailable("任务编辑管理运行时尚未启用。");
        QuestEditorActor actor=new QuestEditorActor(playerId,playerName);
        String message="任务定义由 PostgreSQL 跨服真源管理。";
        return buildQuestManagementSnapshot(actor,intent,actionType,message);
    }

    private static TerminalQuestCenterSectionSnapshot buildQuestManagementSnapshot(QuestEditorActor actor,
        TerminalQuestActionPayload intent,TerminalActionType actionType,String initialMessage){
        String message=initialMessage;
        if(actionType==TerminalActionType.QUEST_ADMIN_CREATE){
            if(questDraftManagementService==null)message="任务定义写入运行时尚未启用，草稿没有创建。";
            else try{message=managementMutationMessage(actionType,questDraftManagementService.createFromTemplate(actor,
                QuestDraftTemplate.valueOf(intent.getManagementTemplate().toUpperCase(Locale.ROOT)),intent.getQuery()));}
            catch(IllegalArgumentException invalid){message="草稿模板或名称无效，没有创建任务。";}
        }
        if(actionType==TerminalActionType.QUEST_ADMIN_PUBLISH||actionType==TerminalActionType.QUEST_ADMIN_RETIRE){
            if(questDraftManagementService==null)message="任务定义写入运行时尚未启用，状态没有改变。";
            else if(intent.getQuestId().isEmpty()||intent.getManagementVersion()<1)message="所选任务版本无效，状态没有改变。";
            else try{
                UUID questId=UUID.fromString(intent.getQuestId());
                QuestDefinitionImpactReport impact=actionType==TerminalActionType.QUEST_ADMIN_RETIRE
                    ?definitionImpact(actor,questId):null;
                if(actionType==TerminalActionType.QUEST_ADMIN_RETIRE&&impact==null)
                    message="影响预检不可用，任务没有退役。";
                else if(actionType==TerminalActionType.QUEST_ADMIN_RETIRE&&!impact.isSafeToRetire())
                    message=impact.isTruncated()?"影响范围超过安全上限，任务没有退役。"
                        :"仍有 "+impact.getDependents().size()+" 个依赖任务和 "+impact.getPlacements().size()+" 个章节放置，任务没有退役。";
                else {
                    QuestDraftManagementResult result=actionType==TerminalActionType.QUEST_ADMIN_PUBLISH
                        ?questDraftManagementService.publish(actor,questId,intent.getManagementVersion(),intent.getManagementHash(),System.currentTimeMillis())
                        :questDraftManagementService.retire(actor,questId,intent.getManagementVersion());
                    message=managementMutationMessage(actionType,result);
                }
            }catch(IllegalArgumentException invalid){message="所选任务标识无效，状态没有改变。";}
        }
        QuestDefinitionLifecycle lifecycle=null;String requested=intent.getManagementLifecycle();
        if(!requested.isEmpty())try{lifecycle=QuestDefinitionLifecycle.valueOf(requested.toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ignored){lifecycle=null;}
        AuthenticatedQuestDefinitionManagementQuery.Result result=questDefinitionManagementQuery.load(
            actor,new QuestDefinitionManagementRequest(intent.getQuery(),lifecycle,
                intent.getManagementPage(),20));
        if(result.getStatus()!=AuthenticatedQuestDefinitionManagementQuery.Status.SUCCESS)return new TerminalQuestCenterSectionSnapshot(
            "FORBIDDEN","只有服务端授权的任务管理员可以打开编辑管理。",TerminalQuestCenterSectionSnapshot.View.BROWSE,
            "","","all","",java.util.Collections.<TerminalQuestCenterSectionSnapshot.Chapter>emptyList(),0,6,0,
            java.util.Collections.<TerminalQuestCenterSectionSnapshot.Quest>emptyList(),0,7,0,null);
        QuestDefinitionManagementPage page=result.getPage();List<TerminalQuestCenterSectionSnapshot.Definition> definitions=new ArrayList<TerminalQuestCenterSectionSnapshot.Definition>();
        for(StoredQuestDefinition stored:page.getDefinitions()){com.jsirgalaxybase.quest.core.QuestDefinition definition=stored.getDefinition();definitions.add(new TerminalQuestCenterSectionSnapshot.Definition(
            definition.getId().toString(),definition.getVersion(),definition.getName(),stored.getLifecycle().name(),
            stored.getContentHash(),stored.getPublishedAt(),definition.getTasks().size(),definition.getRewards().size()));}
        TerminalQuestCenterSectionSnapshot.DefinitionDetail detail=null;
        if(!intent.getQuestId().isEmpty()&&intent.getManagementVersion()>0)try{
            AuthenticatedQuestDefinitionManagementQuery.DefinitionResult found=questDefinitionManagementQuery.find(
                actor,UUID.fromString(intent.getQuestId()),intent.getManagementVersion());
            if(found.getStatus()==AuthenticatedQuestDefinitionManagementQuery.DefinitionResult.Status.SUCCESS){
                detail=managementDetail(found.getDefinition(),definitionImpact(actor,found.getDefinition().getDefinition().getId()));
                AuthenticatedQuestDefinitionManagementQuery.Result candidates=questDefinitionManagementQuery.load(actor,
                    new QuestDefinitionManagementRequest("",QuestDefinitionLifecycle.PUBLISHED,0,50));
                if(candidates.getStatus()==AuthenticatedQuestDefinitionManagementQuery.Status.SUCCESS){definitions.clear();for(StoredQuestDefinition stored:candidates.getPage().getDefinitions()){
                    com.jsirgalaxybase.quest.core.QuestDefinition definition=stored.getDefinition();if(definition.getId().toString().equals(intent.getQuestId()))continue;
                    definitions.add(new TerminalQuestCenterSectionSnapshot.Definition(definition.getId().toString(),definition.getVersion(),definition.getName(),stored.getLifecycle().name(),stored.getContentHash(),stored.getPublishedAt(),definition.getTasks().size(),definition.getRewards().size()));}}
            }
            else if(found.getStatus()==AuthenticatedQuestDefinitionManagementQuery.DefinitionResult.Status.NOT_FOUND)message="所选任务版本不存在或已被移除。";
            else message="当前玩家没有读取任务定义的权限。";
        }catch(IllegalArgumentException invalid){message="所选任务标识无效。";}
        TerminalQuestCenterSectionSnapshot.Management management=new TerminalQuestCenterSectionSnapshot.Management(
            intent.getQuery(),lifecycle==null?"":lifecycle.name(),page.getPage(),page.getPageSize(),page.getTotal(),definitions,detail,
            managementTypes(QuestElementKind.TASK),managementTypes(QuestElementKind.REWARD));
        return new TerminalQuestCenterSectionSnapshot("READY",message,
            TerminalQuestCenterSectionSnapshot.View.ADMIN,"","","all",intent.getQuery(),
            java.util.Collections.<TerminalQuestCenterSectionSnapshot.Chapter>emptyList(),0,6,0,
            java.util.Collections.<TerminalQuestCenterSectionSnapshot.Quest>emptyList(),0,7,0,null,management);
    }

    private static String managementMutationMessage(TerminalActionType actionType,QuestDraftManagementResult result){
        if(result==null)return "任务定义操作没有返回结果，状态可能未改变。";
        if(result.getStatus()==QuestDraftManagementResult.Status.SUCCESS)
            return actionType==TerminalActionType.QUEST_ADMIN_CREATE?"新任务草稿已创建。":actionType==TerminalActionType.QUEST_ADMIN_PUBLISH?"任务版本已发布。":actionType==TerminalActionType.QUEST_ADMIN_RETIRE?"任务版本已退役。":"草稿修改已保存。";
        if(result.getStatus()==QuestDraftManagementResult.Status.FORBIDDEN)return "当前玩家没有管理任务定义的权限。";
        if(result.getStatus()==QuestDraftManagementResult.Status.NOT_FOUND)return "所选任务版本不存在。";
        if(result.getStatus()==QuestDraftManagementResult.Status.CONFLICT)return "任务版本已经变化，请刷新后重试。";
        if(!result.getIssues().isEmpty()){QuestEditorValidationIssue issue=result.getIssues().get(0);return "发布校验失败："+safeBounded(issue.getMessage(),256);}
        return "任务定义校验失败，状态没有改变。";
    }

    private static QuestDefinitionImpactReport definitionImpact(QuestEditorActor actor,UUID questId){
        if(questDefinitionImpactQuery==null)return null;
        try{AuthenticatedQuestDefinitionImpactQuery.Result result=questDefinitionImpactQuery.load(actor,questId);
            return result.getStatus()==AuthenticatedQuestDefinitionImpactQuery.Status.SUCCESS?result.getReport():null;
        }catch(RuntimeException unavailable){GalaxyBase.LOG.warn("Quest definition impact preview failed closed for {}",questId,unavailable);return null;}
    }

    private static TerminalQuestCenterSectionSnapshot.DefinitionDetail managementDetail(StoredQuestDefinition stored,
        QuestDefinitionImpactReport impact){
        com.jsirgalaxybase.quest.core.QuestDefinition definition=stored.getDefinition();TerminalQuestCenterSectionSnapshot.Definition summary=new TerminalQuestCenterSectionSnapshot.Definition(definition.getId().toString(),definition.getVersion(),definition.getName(),stored.getLifecycle().name(),stored.getContentHash(),stored.getPublishedAt(),definition.getTasks().size(),definition.getRewards().size());
        List<String> prerequisites=new ArrayList<String>();for(UUID value:definition.getPrerequisites()){if(prerequisites.size()>=64)break;prerequisites.add(value.toString());}
        List<TerminalQuestCenterSectionSnapshot.Element> tasks=new ArrayList<TerminalQuestCenterSectionSnapshot.Element>();for(com.jsirgalaxybase.quest.core.TaskDefinition value:definition.getTasks()){if(tasks.size()>=64)break;tasks.add(new TerminalQuestCenterSectionSnapshot.Element(value.getKey(),value.getTypeId(),value.isOptional(),boundedParameters(value.getParameters())));}
        List<TerminalQuestCenterSectionSnapshot.Element> rewards=new ArrayList<TerminalQuestCenterSectionSnapshot.Element>();for(com.jsirgalaxybase.quest.core.RewardDefinition value:definition.getRewards()){if(rewards.size()>=64)break;rewards.add(new TerminalQuestCenterSectionSnapshot.Element(value.getKey(),value.getTypeId(),false,boundedParameters(value.getParameters())));}
        java.util.Map<String,String> options=new java.util.LinkedHashMap<String,String>();com.jsirgalaxybase.quest.core.QuestBehavior behavior=definition.getBehavior();options.put("repeat.cooldownMillis",String.valueOf(definition.getRepeatPolicy().getCooldownMillis()));options.put("repeat.relative",String.valueOf(definition.getRepeatPolicy().isRelative()));options.put("behavior.visibility",behavior.getVisibility().name());options.put("behavior.icon",safeBounded(behavior.getIconReference(),256));options.put("behavior.main",String.valueOf(behavior.isMain()));options.put("behavior.silent",String.valueOf(behavior.isSilent()));options.put("behavior.autoClaim",String.valueOf(behavior.isAutoClaim()));options.put("behavior.progressWhileLocked",String.valueOf(behavior.isProgressWhileLocked()));options.put("behavior.simultaneous",String.valueOf(behavior.isSimultaneous()));options.put("behavior.global",String.valueOf(behavior.isGlobal()));options.put("behavior.globalShare",String.valueOf(behavior.isGlobalShare()));options.put("behavior.updateSound",safeBounded(behavior.getUpdateSound(),256));options.put("behavior.completeSound",safeBounded(behavior.getCompleteSound(),256));
        if(impact!=null){options.put("impact.direct",String.valueOf(directDependents(impact)));options.put("impact.transitive",String.valueOf(Math.max(0,impact.getDependents().size()-directDependents(impact))));options.put("impact.placements",String.valueOf(impact.getPlacements().size()));options.put("impact.truncated",String.valueOf(impact.isTruncated()));options.put("impact.safeToRetire",String.valueOf(impact.isSafeToRetire()));options.put("impact.summary",impactSummary(impact));}
        return new TerminalQuestCenterSectionSnapshot.DefinitionDetail(summary,safeBounded(definition.getDescription(),4096),definition.getPrerequisiteLogic().name(),definition.getTaskLogic().name(),prerequisites,tasks,rewards,options);
    }

    private static int directDependents(QuestDefinitionImpactReport impact){int count=0;for(QuestDefinitionImpactReport.Dependent value:impact.getDependents())if(value.isDirect())count++;return count;}
    private static String impactSummary(QuestDefinitionImpactReport impact){StringBuilder value=new StringBuilder();int count=0;for(QuestDefinitionImpactReport.Dependent dependent:impact.getDependents()){if(count++>=5)break;if(value.length()>0)value.append("、");value.append(dependent.getName());}for(QuestDefinitionImpactReport.Placement placement:impact.getPlacements()){if(count++>=8)break;if(value.length()>0)value.append("、");value.append("章节 ").append(placement.getChapterName());}return safeBounded(value.toString(),512);}

    private static java.util.Map<String,String> boundedParameters(java.util.Map<String,String> source){java.util.Map<String,String> result=new java.util.LinkedHashMap<String,String>();if(source!=null)for(java.util.Map.Entry<String,String> entry:source.entrySet()){if(result.size()>=64)break;result.put(safeBounded(entry.getKey(),128),safeBounded(entry.getValue(),1024));}return result;}
    private static List<TerminalQuestCenterSectionSnapshot.ElementType> managementTypes(QuestElementKind kind){List<TerminalQuestCenterSectionSnapshot.ElementType> result=new ArrayList<TerminalQuestCenterSectionSnapshot.ElementType>();if(questDraftManagementService==null)return result;for(QuestElementTypeDescriptor type:questDraftManagementService.listTypes(kind)){if(result.size()>=32)break;List<TerminalQuestCenterSectionSnapshot.Field> fields=new ArrayList<TerminalQuestCenterSectionSnapshot.Field>();for(QuestEditorFieldDescriptor field:type.getFields()){if(fields.size()>=32)break;fields.add(new TerminalQuestCenterSectionSnapshot.Field(safeBounded(field.getKey(),128),safeBounded(field.getLabel(),128),field.getType().name(),field.isRequired(),field.getMinimum(),field.getMaximum(),field.getOptions().size()>32?field.getOptions().subList(0,32):field.getOptions()));}result.add(new TerminalQuestCenterSectionSnapshot.ElementType(safeBounded(type.getTypeId(),128),safeBounded(type.getDisplayName(),128),kind.name(),fields));}return result;}
    private static String safeBounded(String value,int max){String result=value==null?"":value;return result.length()<=max?result:result.substring(0,max);}

    private static String claimMessage(RewardClaimStatus status){
        if(status==RewardClaimStatus.CLAIMED)return "奖励已进入跨服交付队列。";
        if(status==RewardClaimStatus.ALREADY_CLAIMED)return "该任务奖励已经领取。";
        if(status==RewardClaimStatus.NEEDS_CHOICE)return "请先选择奖励内容。";
        if(status==RewardClaimStatus.NOT_OWNED)return "该奖励不属于当前玩家。";
        if(status==RewardClaimStatus.INVALID_STATE)return "奖励当前状态不可领取，请刷新后重试。";
        return "未找到可领取的任务奖励。";
    }

    private static String choiceMessage(RewardChoiceSelectionStatus status){
        if(status==RewardChoiceSelectionStatus.SELECTED)return "奖励选项已确认，可以领取任务奖励。";
        if(status==RewardChoiceSelectionStatus.ALREADY_SELECTED)return "该奖励选项已经确认。";
        if(status==RewardChoiceSelectionStatus.CONFLICT)return "奖励选项已经锁定，不能改选。";
        if(status==RewardChoiceSelectionStatus.INVALID)return "该奖励选项不可用，请刷新后重试。";
        return "未找到可选择的任务奖励。";
    }

    private static TerminalOpenApproval.PageSnapshot createPublicServicePageSnapshot(EntityPlayer player) {
        return createLinePageSnapshot(
            TerminalPage.PUBLIC_SERVICE,
            TerminalHomeSnapshotProvider.INSTANCE.createPublicServicePageLines(player));
    }

    private static TerminalOpenApproval.PageSnapshot createMarketPageSnapshot(TerminalPage selectedPage,
        MarketActionContext marketContext) {
        TerminalPage effectivePage = selectedPage != null && selectedPage.isMarketPage() ? selectedPage : TerminalPage.MARKET;
        TerminalMarketSectionSnapshot snapshot = marketContext.snapshot == null
            ? marketPageFacade.createSnapshot(null, effectivePage, TerminalMarketActionPayload.empty(), null)
            : marketContext.snapshot;
        List<TerminalOpenApproval.Section> sections = new ArrayList<TerminalOpenApproval.Section>();
        if (effectivePage == TerminalPage.MARKET_STANDARDIZED || effectivePage == TerminalPage.MARKET_ACCOUNT_CENTER) {
            sections.add(new TerminalOpenApproval.Section(
                effectivePage == TerminalPage.MARKET_ACCOUNT_CENTER ? "market_account_center_runtime" : "market_standardized_runtime",
                effectivePage == TerminalPage.MARKET_ACCOUNT_CENTER ? "订单与资产中心" : "标准商品运行态",
                snapshot.getServiceState(),
                snapshot.getSummaryNotice()));
            sections.add(new TerminalOpenApproval.Section(
                effectivePage == TerminalPage.MARKET_ACCOUNT_CENTER ? "market_account_center_summary" : "market_standardized_focus",
                effectivePage == TerminalPage.MARKET_ACCOUNT_CENTER ? "当前委托与交付摘要" : "当前交易焦点",
                effectivePage == TerminalPage.MARKET_ACCOUNT_CENTER
                    ? "当前委托 " + snapshot.getHistoryTotalEntries() + " 条 | 冻结资金 " + snapshot.getFrozenFunds()
                    : snapshot.getSelectedProductName() + " | 买一 " + snapshot.getHighestBid() + " / 卖一 " + snapshot.getLowestAsk(),
                effectivePage == TerminalPage.MARKET_ACCOUNT_CENTER
                    ? "待收货 " + snapshot.getClaimableQuantity() + "；分页由服务端真实总数计算。"
                    : "24h 成交量 " + snapshot.getVolume24h() + " | 待收货 " + snapshot.getClaimableQuantity()));
        } else if (effectivePage == TerminalPage.MARKET_CUSTOM) {
            TerminalCustomMarketSectionSnapshot customSnapshot = marketContext.customSnapshot == null
                ? marketPageFacade.createCustomSnapshot(null, TerminalCustomMarketActionPayload.empty(), null)
                : marketContext.customSnapshot;
            sections.add(new TerminalOpenApproval.Section(
                "market_custom_runtime",
                "定制商品运行态",
                customSnapshot.getServiceState(),
                customSnapshot.getBrowserHint()));
            sections.add(new TerminalOpenApproval.Section(
                "market_custom_focus",
                "当前 listing",
                customSnapshot.getSelectedTitle() + " | " + customSnapshot.getSelectedPrice(),
                customSnapshot.getSelectedStatus() + " | " + customSnapshot.getSelectedActionHint()));
            return new TerminalOpenApproval.PageSnapshot(
                TerminalPage.MARKET.getId(),
                effectivePage.getTitle(),
                effectivePage.getLead(),
                sections,
                null,
                null,
                customSnapshot,
                null);
        } else if (effectivePage == TerminalPage.MARKET_EXCHANGE) {
            TerminalExchangeMarketSectionSnapshot exchangeSnapshot = marketContext.exchangeSnapshot == null
                ? marketPageFacade.createExchangeSnapshot(null, TerminalExchangeMarketActionPayload.empty(), null)
                : marketContext.exchangeSnapshot;
            sections.add(new TerminalOpenApproval.Section(
                "market_exchange_runtime",
                "汇率运行态",
                exchangeSnapshot.getServiceState(),
                exchangeSnapshot.getBrowserHint()));
            sections.add(new TerminalOpenApproval.Section(
                "market_exchange_quote",
                "当前 quote",
                exchangeSnapshot.getSelectedTargetTitle() + " | " + exchangeSnapshot.getEffectiveExchangeValue() + " STARCOIN",
                exchangeSnapshot.getLimitStatus() + " | " + exchangeSnapshot.getExecutionHint()));
            return new TerminalOpenApproval.PageSnapshot(
                TerminalPage.MARKET.getId(),
                effectivePage.getTitle(),
                effectivePage.getLead(),
                sections,
                null,
                null,
                null,
                exchangeSnapshot);
        } else {
            sections.add(new TerminalOpenApproval.Section(
                "market_overview_summary",
                "共享摘要",
                snapshot.getServiceState(),
                snapshot.getBrowserHint()));
            sections.add(new TerminalOpenApproval.Section(
                "market_overview_standardized",
                "标准商品市场入口",
                "最新成交价 " + snapshot.getLatestTradePrice() + " | 待收货 " + snapshot.getClaimableQuantity(),
                "标准商品市场提供目录浏览、即时交易、限价委托、撤单与待收货处理。"));
            sections.add(new TerminalOpenApproval.Section(
                "market_overview_boundary",
                "市场分区",
                "总入口用于选择标准商品、定制商品或汇率市场。",
                "各市场独立展示目录、详情和可执行操作。"));
        }
        return new TerminalOpenApproval.PageSnapshot(
            TerminalPage.MARKET.getId(),
            effectivePage.getTitle(),
            effectivePage.getLead(),
                sections,
                null,
                snapshot,
                null,
                null);
    }

    private static TerminalOpenApproval.PageSnapshot createServerToolsPageSnapshot(ServerToolsActionContext context) {
        TerminalServerToolsSectionSnapshot snapshot = context == null || context.snapshot == null
            ? TerminalServerToolsSectionSnapshot.placeholder() : context.snapshot;
        List<TerminalOpenApproval.Section> sections = new ArrayList<TerminalOpenApproval.Section>();
        sections.add(new TerminalOpenApproval.Section(
            "server_tools_transport_console",
            "群组服传送工具页",
            snapshot.getSelectedWarpTitle(),
            "当前服务器 " + snapshot.getCurrentServerId() + " / 目标 " + snapshot.getSelectedTargetServerId()));
        return new TerminalOpenApproval.PageSnapshot(
            TerminalPage.SERVER_TOOLS.getId(),
            TerminalPage.SERVER_TOOLS.getTitle(),
            TerminalPage.SERVER_TOOLS.getLead(),
            sections,
            null,
            null,
            null,
            null,
            snapshot);
    }

    private static TerminalOpenApproval.PageSnapshot createBankPageSnapshot(BankActionContext bankContext) {
        TerminalBankSnapshot snapshot = bankContext.snapshot == null ? bankPageFacade.createSnapshot(null) : bankContext.snapshot;
        boolean accountOpened = isAccountOpened(snapshot);
        boolean serviceAvailable = isServiceAvailable(snapshot);
        TerminalBankActionPayload bankPayload = bankContext.payload == null ? TerminalBankActionPayload.empty() : bankContext.payload;
        TerminalBankSectionSnapshot bankSectionSnapshot = new TerminalBankSectionSnapshot(
            new TerminalBankSectionSnapshot.AccountStatus(
                accountOpened,
                snapshot.getServiceState(),
                accountOpened ? "已开户" : "未开户",
                snapshot.getPlayerStatus(),
                snapshot.getPlayerAccountNo(),
                snapshot.getPlayerUpdatedAt(),
                serviceAvailable && !accountOpened),
            new TerminalBankSectionSnapshot.BalanceSummary(
                snapshot.getPlayerBalance(),
                snapshot.getExchangeBalance(),
                snapshot.getExchangeStatus(),
                snapshot.getTransferState(),
                serviceAvailable && accountOpened),
            new TerminalBankSectionSnapshot.TransferForm(
                bankPayload.getTargetPlayerName(),
                bankPayload.getAmountText(),
                bankPayload.getComment(),
                serviceAvailable && accountOpened),
            buildBankActionFeedback(snapshot, bankContext.actionResult),
            toLedgerLines(snapshot));

        List<TerminalOpenApproval.Section> sections = new ArrayList<TerminalOpenApproval.Section>();
        sections.add(new TerminalOpenApproval.Section(
            "bank_account_state",
            "开户状态",
            bankSectionSnapshot.getAccountStatus().getAccountLabel() + " | " + snapshot.getPlayerBalance(),
            snapshot.getPlayerStatus() + " | 账户编号 " + snapshot.getPlayerAccountNo()));
        sections.add(new TerminalOpenApproval.Section(
            "bank_transfer_state",
            "转账与公开储备",
            snapshot.getTransferState(),
            "公开储备 " + snapshot.getExchangeBalance() + " | " + snapshot.getExchangeStatus()));
        return new TerminalOpenApproval.PageSnapshot(
            TerminalPage.BANK.getId(),
            TerminalPage.BANK.getTitle(),
            TerminalPage.BANK.getLead(),
            sections,
            bankSectionSnapshot);
    }

    private static TerminalOpenApproval.PageSnapshot createLinePageSnapshot(TerminalPage page, String[] lines) {
        List<TerminalOpenApproval.Section> sections = new ArrayList<TerminalOpenApproval.Section>();
        if (lines != null) {
            for (int index = 0; index < lines.length; index++) {
                sections.add(new TerminalOpenApproval.Section(
                    page.getId() + "_section_" + (index + 1),
                    page.getLabel() + " 占位 " + (index + 1),
                    lines[index],
                    "当前仍是只读 section 占位内容，用于验证 page -> section 宿主切换与刷新协议。"));
            }
        }
        if (sections.isEmpty()) {
            sections.add(TerminalOpenApproval.Section.placeholder());
        }
        return new TerminalOpenApproval.PageSnapshot(page.getId(), page.getTitle(), page.getLead(), sections);
    }

    private static String buildStatusDetail(TerminalHomeSnapshot snapshot, TerminalPage selectedPage,
        TerminalActionType actionType, BankActionContext bankContext, MarketActionContext marketContext,
        ServerToolsActionContext serverToolsContext) {
        String base = TerminalOpenSummaryFormatter.buildStatusBandDetail(snapshot) + " | " + selectedPage.getLabel();
        if (selectedPage.isMarketPage()) {
            TerminalMarketSectionSnapshot marketSnapshot = marketContext.snapshot;
            String marketDetail = marketSnapshot == null ? "市场摘要不可用"
                : marketSnapshot.getSelectedProductName() + " | " + marketSnapshot.getServiceState();
            if (marketContext.actionResult != null) {
                marketDetail = marketDetail + " | " + marketContext.actionResult.getBody();
            }
            return base + " | " + marketDetail;
        }
        if (selectedPage.isServerToolsPage()) {
            TerminalServerToolsSectionSnapshot serverToolsSnapshot = serverToolsContext == null ? null : serverToolsContext.snapshot;
            String detail = serverToolsSnapshot == null ? "传送页不可用"
                : serverToolsSnapshot.getCurrentServerId() + " | " + serverToolsSnapshot.getServiceState();
            if (serverToolsContext != null && serverToolsContext.actionFeedback != null) {
                detail = detail + " | " + serverToolsContext.actionFeedback.getBody();
            }
            return base + " | " + detail;
        }
        if (!selectedPage.isBankPage()) {
            String actionDetail = actionType == TerminalActionType.REFRESH_PAGE ? "已刷新当前分区"
                : actionType == TerminalActionType.SELECT_PAGE ? "已切换到当前分区宿主"
                    : "首页壳已进入完整业务页迁移阶段";
            return base + " | " + actionDetail;
        }

        TerminalBankSnapshot bankSnapshot = bankContext.snapshot;
        String bankDetail = bankSnapshot == null ? "银行摘要不可用"
            : bankSnapshot.getPlayerBalance() + " | " + bankSnapshot.getServiceState();
        if (bankContext.actionResult != null) {
            bankDetail = bankDetail + " | " + TerminalNotification.stripFormatting(bankContext.actionResult.getMessage());
        }
        return base + " | " + bankDetail;
    }

    private static List<TerminalOpenApproval.NotificationEntry> createNotifications(EntityPlayer player, TerminalPage selectedPage,
        TerminalActionType actionType, BankActionContext bankContext, MarketActionContext marketContext,
        ServerToolsActionContext serverToolsContext) {
        List<TerminalOpenApproval.NotificationEntry> notifications = new ArrayList<TerminalOpenApproval.NotificationEntry>();
        if (selectedPage.isMarketPage()) {
            if (marketContext.actionResult != null) {
                notifications.add(new TerminalOpenApproval.NotificationEntry(
                    marketContext.actionResult.getTitle(),
                    marketContext.actionResult.getBody(),
                    marketContext.actionResult.getSeverity().name(),
                    isRecoveryFeedback(marketContext.actionResult.getTitle(), marketContext.actionResult.getBody())
                        ? "market-recovery" : "market",
                    TerminalPage.MARKET_ACCOUNT_CENTER.getId(), ""));
            } else if (actionType == TerminalActionType.SELECT_PAGE) {
                notifications.add(new TerminalOpenApproval.NotificationEntry(
                    "已切换市场分区",
                    selectedPage == TerminalPage.MARKET_STANDARDIZED
                        ? "已进入标准商品市场，可浏览正式目录与实时行情。"
                        : "已返回市场总入口，可选择标准商品、定制商品或汇率市场。",
                    TerminalNotificationSeverity.INFO.name()));
            }
            notifications.add(new TerminalOpenApproval.NotificationEntry(
                "市场服务已接入",
                "市场总入口、标准商品、定制商品与汇率市场均使用统一操作和刷新流程。",
                TerminalNotificationSeverity.INFO.name()));
            return notifications;
        }
        if (selectedPage.isServerToolsPage()) {
            if (serverToolsContext != null && serverToolsContext.actionFeedback != null) {
                notifications.add(new TerminalOpenApproval.NotificationEntry(
                    serverToolsContext.actionFeedback.getTitle(),
                    serverToolsContext.actionFeedback.getBody(),
                    serverToolsContext.actionFeedback.getSeverityName(),
                    "transfer-ticket", TerminalPage.SERVER_TOOLS.getId(), ""));
            } else if (actionType == TerminalActionType.SELECT_PAGE) {
                notifications.add(new TerminalOpenApproval.NotificationEntry(
                    "已切换传送分区",
                    "已进入传送页，可浏览传送点并确认跨服传送。",
                    TerminalNotificationSeverity.INFO.name()));
            }
            notifications.add(new TerminalOpenApproval.NotificationEntry(
                "传送服务已接入",
                "当前开放系统传送点浏览与确认，传送状态会在页面内回写。",
                TerminalNotificationSeverity.INFO.name()));
            return notifications;
        }
        if (selectedPage.isBankPage()) {
            if (bankContext.actionResult != null) {
                notifications.add(new TerminalOpenApproval.NotificationEntry(
                    bankContext.actionResult.getSeverity().getDefaultTitle(),
                    TerminalNotification.stripFormatting(bankContext.actionResult.getMessage()),
                    bankContext.actionResult.getSeverity().name()));
            } else if (actionType == TerminalActionType.SELECT_PAGE) {
                notifications.add(new TerminalOpenApproval.NotificationEntry(
                    "已切换分区",
                    "已进入银行页，可查看账户、余额与转账状态。",
                    TerminalNotificationSeverity.INFO.name()));
            }
            notifications.add(new TerminalOpenApproval.NotificationEntry(
                "银行服务已接入",
                "开户状态、余额摘要和转账确认均使用统一银行操作和刷新流程。",
                TerminalNotificationSeverity.INFO.name()));
            return notifications;
        }

        if (selectedPage == TerminalPage.PROPERTY) {
            LandModule land = resolveLandModule();
            String playerRef = player == null || player.getUniqueID() == null ? "" : player.getUniqueID().toString();
            com.jsirgalaxybase.modules.land.application.LandProtectionRuntime.ShadowNotice shadow =
                land == null || land.getProtectionRuntime() == null ? null
                    : land.getProtectionRuntime().getLatestShadowNotice(playerRef);
            if (shadow != null) {
                notifications.add(new TerminalOpenApproval.NotificationEntry("保护观察记录",
                    "SHADOW 模式记录到 " + shadow.getAction() + "；当前未取消动作。区块 " + shadow.getChunk(),
                    TerminalNotificationSeverity.WARNING.name(), "land-shadow", TerminalPage.PROPERTY.getId(), ""));
            }
        }

        if (actionType == TerminalActionType.SELECT_PAGE) {
            notifications.add(new TerminalOpenApproval.NotificationEntry(
                "已切换分区",
                "已进入" + selectedPage.getLabel() + "。",
                TerminalNotificationSeverity.INFO.name()));
        } else if (actionType == TerminalActionType.REFRESH_PAGE) {
            notifications.add(new TerminalOpenApproval.NotificationEntry(
                "分区快照已刷新",
                selectedPage.getLabel() + " 已完成刷新。",
                TerminalNotificationSeverity.INFO.name()));
        }
        notifications.add(new TerminalOpenApproval.NotificationEntry(
            "银河终端已就绪",
            "当前页面数据来自服务器快照，刷新后会保留最新有效响应。",
            TerminalNotificationSeverity.INFO.name()));
        return notifications;
    }

    private static boolean isRecoveryFeedback(String title, String body) {
        String value = ((title == null ? "" : title) + " " + (body == null ? "" : body)).toLowerCase(Locale.ROOT);
        return value.contains("恢复") || value.contains("交付") || value.contains("收货") || value.contains("custody")
            || value.contains("recovery");
    }

    private static TerminalBankSectionSnapshot.ActionFeedback buildBankActionFeedback(TerminalBankSnapshot snapshot,
        TerminalBankingService.ActionResult actionResult) {
        if (actionResult != null) {
            return new TerminalBankSectionSnapshot.ActionFeedback(
                actionResult.getSeverity().getDefaultTitle(),
                TerminalNotification.stripFormatting(actionResult.getMessage()),
                actionResult.getSeverity().name());
        }
        if (snapshot == null) {
            return TerminalBankSectionSnapshot.ActionFeedback.placeholder();
        }
        return new TerminalBankSectionSnapshot.ActionFeedback(
            "银行动作反馈",
            snapshot.getTransferState(),
            isServiceAvailable(snapshot) ? TerminalNotificationSeverity.INFO.name() : TerminalNotificationSeverity.WARNING.name());
    }

    private static boolean isAccountOpened(TerminalBankSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        String accountNo = normalize(snapshot.getPlayerAccountNo(), "未分配");
        String balance = normalize(snapshot.getPlayerBalance(), "未开户");
        return !"未分配".equals(accountNo) && !"未开户".equals(balance) && !"不可用".equals(balance) && !"读取失败".equals(balance);
    }

    private static boolean isServiceAvailable(TerminalBankSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        String serviceState = normalize(snapshot.getServiceState(), "");
        return !(serviceState.contains("未接入") || serviceState.contains("不可用") || serviceState.contains("失败") || serviceState.contains("仅限"));
    }

    private static List<String> toLedgerLines(TerminalBankSnapshot snapshot) {
        if (snapshot == null) {
            return Arrays.asList("当前没有个人流水摘要。");
        }
        String[] lines = snapshot.getPlayerLedgerLines();
        List<String> results = new ArrayList<String>();
        if (lines != null) {
            for (String line : lines) {
                if (line != null && !line.trim().isEmpty()) {
                    results.add(line.trim());
                }
            }
        }
        if (results.isEmpty()) {
            results.add("当前没有个人流水摘要。");
        }
        return results;
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    static interface BankPageFacade {

        TerminalBankSnapshot createSnapshot(EntityPlayer player);

        TerminalBankingService.ActionResult openOwnAccount(EntityPlayer player);

        TerminalBankingService.ActionResult transferToPlayer(EntityPlayer player, String targetPlayerName, long amount,
            String comment);
    }

    static interface MarketPageFacade {

        TerminalMarketSectionSnapshot createSnapshot(EntityPlayer player, TerminalPage selectedPage,
            TerminalMarketActionPayload payload, TerminalActionFeedback actionFeedback);

        TerminalActionFeedback submitLimitBuy(EntityPlayer player, TerminalMarketActionPayload payload);

        TerminalActionFeedback submitDepositHeld(EntityPlayer player, TerminalMarketActionPayload payload);

        TerminalActionFeedback submitLimitSell(EntityPlayer player, TerminalMarketActionPayload payload);

        TerminalActionFeedback submitInstantBuy(EntityPlayer player, TerminalMarketActionPayload payload);

        TerminalActionFeedback submitInstantSell(EntityPlayer player, TerminalMarketActionPayload payload);

        TerminalActionFeedback cancelOrder(EntityPlayer player, TerminalMarketActionPayload payload);

        TerminalActionFeedback claimAsset(EntityPlayer player, TerminalMarketActionPayload payload);

        TerminalCustomMarketSectionSnapshot createCustomSnapshot(EntityPlayer player, TerminalCustomMarketActionPayload payload,
            TerminalActionFeedback actionFeedback);

        TerminalActionFeedback purchaseCustomListing(EntityPlayer player, TerminalCustomMarketActionPayload payload);

        TerminalActionFeedback publishCustomListing(EntityPlayer player, TerminalCustomMarketActionPayload payload);

        TerminalActionFeedback cancelCustomListing(EntityPlayer player, TerminalCustomMarketActionPayload payload);

        TerminalActionFeedback claimCustomListing(EntityPlayer player, TerminalCustomMarketActionPayload payload);

        TerminalExchangeMarketSectionSnapshot createExchangeSnapshot(EntityPlayer player,
            TerminalExchangeMarketActionPayload payload, TerminalActionFeedback actionFeedback);

        TerminalActionFeedback refreshExchangeQuote(EntityPlayer player);

        TerminalActionFeedback submitExchange(EntityPlayer player, TerminalExchangeMarketActionPayload payload);
    }

    static interface ServerToolsPageFacade {

        TerminalServerToolsSectionSnapshot createSnapshot(EntityPlayer player,
            TerminalServerToolsActionPayload payload,
            TerminalServerToolsSectionSnapshot.ActionFeedback actionFeedback);

        TerminalServerToolsSectionSnapshot.ActionFeedback confirmWarp(EntityPlayerMP player, String warpName);

        TerminalServerToolsSectionSnapshot.ActionFeedback confirmQuickAction(EntityPlayerMP player, String quickAction);

        default TerminalServerToolsSectionSnapshot.ActionFeedback confirmHome(EntityPlayerMP player, String homeName) {
            return unsupportedHomeFeedback();
        }

        default TerminalServerToolsSectionSnapshot.ActionFeedback setHome(EntityPlayerMP player, String homeName) {
            return unsupportedHomeFeedback();
        }

        default TerminalServerToolsSectionSnapshot.ActionFeedback deleteHome(EntityPlayerMP player, String homeName) {
            return unsupportedHomeFeedback();
        }

        default TerminalServerToolsSectionSnapshot.ActionFeedback createTpa(EntityPlayerMP player,
            String targetPlayerName, String targetServerId) { return unsupportedTpaFeedback(); }
        default TerminalServerToolsSectionSnapshot.ActionFeedback acceptTpa(EntityPlayerMP player,
            String requesterPlayerName) { return unsupportedTpaFeedback(); }
        default TerminalServerToolsSectionSnapshot.ActionFeedback denyTpa(EntityPlayerMP player,
            String requesterPlayerName) { return unsupportedTpaFeedback(); }
        default TerminalServerToolsSectionSnapshot.ActionFeedback cancelTpa(EntityPlayerMP player,
            String targetPlayerName, String targetServerId) { return unsupportedTpaFeedback(); }

        default TerminalServerToolsSectionSnapshot.ActionFeedback unsupportedTpaFeedback() {
            return new TerminalServerToolsSectionSnapshot.ActionFeedback("TPA 操作不可用",
                "当前 ServerTools 运行时不支持 TPA 请求。", TerminalNotificationSeverity.WARNING.name());
        }

        default TerminalServerToolsSectionSnapshot.ActionFeedback unsupportedHomeFeedback() {
            return new TerminalServerToolsSectionSnapshot.ActionFeedback("Home 操作不可用",
                "当前 ServerTools 运行时不支持个人 Home 管理。", TerminalNotificationSeverity.WARNING.name());
        }
    }

    static interface ServerToolsRuntimeProvider {

        ServerToolsRuntimeBridge resolve();
    }

    static interface ServerToolsRuntimeBridge {

        boolean isRuntimeAvailable();

        String getLocalServerId();

        List<ServerDescriptor> listServers();

        List<ServerWarp> listWarps();

        List<TransferTicket> findRecentTickets(String playerUuid, int limit);

        default List<PlayerHome> listHomes(String playerUuid) {
            return new ArrayList<PlayerHome>();
        }

        default List<TpaRequest> listOutgoingTpa(EntityPlayerMP player, int limit) {
            return new ArrayList<TpaRequest>();
        }

        default List<TpaRequest> listIncomingTpa(EntityPlayerMP player, int limit) {
            return new ArrayList<TpaRequest>();
        }

        default TpaRequest createTpa(EntityPlayerMP player, String targetPlayerName, String targetServerId) {
            throw new UnsupportedOperationException("TPA is not supported by this runtime");
        }

        default TpaRequest acceptTpa(EntityPlayerMP player, String requesterPlayerName) {
            throw new UnsupportedOperationException("TPA is not supported by this runtime");
        }

        default TpaRequest denyTpa(EntityPlayerMP player, String requesterPlayerName) {
            throw new UnsupportedOperationException("TPA is not supported by this runtime");
        }

        default TpaRequest cancelTpa(EntityPlayerMP player, String targetPlayerName, String targetServerId) {
            throw new UnsupportedOperationException("TPA is not supported by this runtime");
        }

        TeleportDispatchPlan prepareWarpTeleport(EntityPlayerMP player, String warpName);

        TeleportDispatchPlan prepareHomeTeleport(EntityPlayerMP player);

        default TeleportDispatchPlan prepareHomeTeleport(EntityPlayerMP player, String homeName) {
            return prepareHomeTeleport(player);
        }

        default PlayerHome setHome(EntityPlayerMP player, String homeName) {
            throw new UnsupportedOperationException("personal Home management is not supported by this runtime");
        }

        default boolean deleteHome(EntityPlayerMP player, String homeName) {
            throw new UnsupportedOperationException("personal Home management is not supported by this runtime");
        }

        TeleportDispatchPlan prepareBackTeleport(EntityPlayerMP player);

        TeleportDispatchPlan prepareSpawnTeleport(EntityPlayerMP player);

        GatewayDispatchResult dispatchTeleport(EntityPlayerMP player, TeleportDispatchPlan dispatchPlan);

        EntityPlayerMP findOnlinePlayer(String playerName);
    }

    private static final class DefaultBankPageFacade implements BankPageFacade {

        @Override
        public TerminalBankSnapshot createSnapshot(EntityPlayer player) {
            return TerminalBankSnapshotProvider.INSTANCE.create(player);
        }

        @Override
        public TerminalBankingService.ActionResult openOwnAccount(EntityPlayer player) {
            return TerminalBankingService.INSTANCE.openOwnAccount(player);
        }

        @Override
        public TerminalBankingService.ActionResult transferToPlayer(EntityPlayer player, String targetPlayerName,
            long amount, String comment) {
            return TerminalBankingService.INSTANCE.transferToPlayer(player, targetPlayerName, amount, comment);
        }
    }

    private static final class DefaultMarketPageFacade implements MarketPageFacade {

        @Override
        public TerminalMarketSectionSnapshot createSnapshot(EntityPlayer player, TerminalPage selectedPage,
            TerminalMarketActionPayload payload, TerminalActionFeedback actionFeedback) {
            return TerminalMarketSectionService.INSTANCE.createSnapshot(player, selectedPage, payload, actionFeedback);
        }

        @Override
        public TerminalActionFeedback submitLimitBuy(EntityPlayer player, TerminalMarketActionPayload payload) {
            return TerminalMarketSectionService.INSTANCE.submitLimitBuy(player, payload);
        }

        @Override
        public TerminalActionFeedback submitDepositHeld(EntityPlayer player, TerminalMarketActionPayload payload) {
            return TerminalMarketSectionService.INSTANCE.submitDepositHeld(player, payload);
        }

        @Override
        public TerminalActionFeedback submitLimitSell(EntityPlayer player, TerminalMarketActionPayload payload) {
            return TerminalMarketSectionService.INSTANCE.submitLimitSell(player, payload);
        }

        @Override
        public TerminalActionFeedback submitInstantBuy(EntityPlayer player, TerminalMarketActionPayload payload) {
            return TerminalMarketSectionService.INSTANCE.submitInstantBuy(player, payload);
        }

        @Override
        public TerminalActionFeedback submitInstantSell(EntityPlayer player, TerminalMarketActionPayload payload) {
            return TerminalMarketSectionService.INSTANCE.submitInstantSell(player, payload);
        }

        @Override
        public TerminalActionFeedback cancelOrder(EntityPlayer player, TerminalMarketActionPayload payload) {
            return TerminalMarketSectionService.INSTANCE.cancelOrder(player, payload);
        }

        @Override
        public TerminalActionFeedback claimAsset(EntityPlayer player, TerminalMarketActionPayload payload) {
            return TerminalMarketSectionService.INSTANCE.claimAsset(player, payload);
        }

        @Override
        public TerminalCustomMarketSectionSnapshot createCustomSnapshot(EntityPlayer player,
            TerminalCustomMarketActionPayload payload, TerminalActionFeedback actionFeedback) {
            return TerminalMarketSectionService.INSTANCE.createCustomSnapshot(player, payload, actionFeedback);
        }

        @Override
        public TerminalActionFeedback purchaseCustomListing(EntityPlayer player, TerminalCustomMarketActionPayload payload) {
            return TerminalMarketSectionService.INSTANCE.purchaseCustomListing(player, payload);
        }

        @Override
        public TerminalActionFeedback publishCustomListing(EntityPlayer player, TerminalCustomMarketActionPayload payload) {
            return TerminalMarketSectionService.INSTANCE.publishCustomListing(player, payload);
        }

        @Override
        public TerminalActionFeedback cancelCustomListing(EntityPlayer player, TerminalCustomMarketActionPayload payload) {
            return TerminalMarketSectionService.INSTANCE.cancelCustomListing(player, payload);
        }

        @Override
        public TerminalActionFeedback claimCustomListing(EntityPlayer player, TerminalCustomMarketActionPayload payload) {
            return TerminalMarketSectionService.INSTANCE.claimCustomListing(player, payload);
        }

        @Override
        public TerminalExchangeMarketSectionSnapshot createExchangeSnapshot(EntityPlayer player,
            TerminalExchangeMarketActionPayload payload, TerminalActionFeedback actionFeedback) {
            return TerminalMarketSectionService.INSTANCE.createExchangeSnapshot(player, payload, actionFeedback);
        }

        @Override
        public TerminalActionFeedback refreshExchangeQuote(EntityPlayer player) {
            return TerminalMarketSectionService.INSTANCE.refreshExchangeQuote(player);
        }

        @Override
        public TerminalActionFeedback submitExchange(EntityPlayer player, TerminalExchangeMarketActionPayload payload) {
            return TerminalMarketSectionService.INSTANCE.submitExchange(player, payload);
        }
    }

    private static final class DefaultServerToolsPageFacade implements ServerToolsPageFacade {

        @Override
        public TerminalServerToolsSectionSnapshot createSnapshot(EntityPlayer player,
            TerminalServerToolsActionPayload payload,
            TerminalServerToolsSectionSnapshot.ActionFeedback actionFeedback) {
            ServerToolsRuntimeBridge runtime = serverToolsRuntimeProvider.resolve();
            if (runtime == null || !runtime.isRuntimeAvailable()) {
                return unavailableSnapshot(actionFeedback);
            }
            List<ServerDescriptor> servers = runtime.listServers();
            List<ServerWarp> warps = runtime.listWarps();
            List<TransferTicket> recentTickets = player == null ? new ArrayList<TransferTicket>()
                : runtime.findRecentTickets(player.getUniqueID().toString(), 3);
            List<PlayerHome> homes = player == null ? new ArrayList<PlayerHome>()
                : runtime.listHomes(player.getUniqueID().toString());
            List<TpaRequest> outgoingTpa = player instanceof EntityPlayerMP
                ? runtime.listOutgoingTpa((EntityPlayerMP) player, 10) : new ArrayList<TpaRequest>();
            List<TpaRequest> incomingTpa = player instanceof EntityPlayerMP
                ? runtime.listIncomingTpa((EntityPlayerMP) player, 10) : new ArrayList<TpaRequest>();
            TpaViewLists tpaViews = TpaViewLists.from(outgoingTpa, incomingTpa);
            String selectedWarpName = payload == null ? "" : payload.getWarpName();
            ServerWarp selectedWarp = findWarp(warps, selectedWarpName);
            String selectedHomeName = payload == null ? "" : payload.getHomeName();
            PlayerHome selectedHome = findHome(homes, selectedHomeName);
            return new TerminalServerToolsSectionSnapshot(
                "ServerTools warp runtime online",
                normalize(runtime.getLocalServerId(), "unknown"),
                toServerLines(servers),
                toServerIds(servers),
                toWarpLines(warps),
                toWarpNames(warps),
                toWarpSubtitles(warps),
                toWarpStateLabels(warps),
                toRecentTransferLines(recentTickets),
                toHomeLines(homes),
                toHomeNames(homes),
                toHomeSubtitles(homes),
                tpaViews.directions,
                tpaViews.counterpartyNames,
                tpaViews.targetServerIds,
                tpaViews.statusLabels,
                selectedWarpName,
                selectedWarp == null ? "未选择 warp" : displayWarpTitle(selectedWarp),
                selectedWarp == null ? "当前没有可查看的 warp 详情。" : describeWarp(selectedWarp),
                selectedWarp == null ? "--" : describeWarpTargetServer(selectedWarp),
                selectedWarp == null ? "--" : describeWarpTargetLocation(selectedWarp),
                selectedWarp == null ? "当前没有额外传送说明。" : normalize(selectedWarp.getDescription(), "当前没有额外传送说明。"),
                selectedWarp != null && selectedWarp.isEnabled(),
                selectedHomeName,
                selectedHome == null ? "--" : describeHomeTargetServer(selectedHome),
                selectedHome == null ? "--" : describeHomeTargetLocation(selectedHome),
                selectedHome == null ? "当前没有可查看的个人 Home。" : describeHome(selectedHome),
                resolveRecentSourceServerId(recentTickets),
                resolveRecentTargetServerId(recentTickets),
                resolveRecentTransferStatus(recentTickets),
                resolveRecentTransferTime(recentTickets),
                resolveRecentTransferSummary(recentTickets),
                actionFeedback == null ? new TerminalServerToolsSectionSnapshot.ActionFeedback(
                    "传送动作反馈",
                    "选择 warp 后点击确认传送，执行前会再次弹窗确认。",
                    TerminalNotificationSeverity.INFO.name()) : actionFeedback);
        }

        @Override
        public TerminalServerToolsSectionSnapshot.ActionFeedback confirmWarp(EntityPlayerMP player, String warpName) {
            ServerToolsRuntimeBridge runtime = serverToolsRuntimeProvider.resolve();
            if (runtime == null || !runtime.isRuntimeAvailable()) {
                return new TerminalServerToolsSectionSnapshot.ActionFeedback(
                    "传送失败",
                    "ServerTools runtime 不可用，请检查 dedicated server 启动日志与 PostgreSQL / Cluster 配置。",
                    TerminalNotificationSeverity.ERROR.name());
            }
            try {
                TeleportDispatchPlan dispatchPlan = runtime.prepareWarpTeleport(player, warpName);
                GatewayDispatchResult result = runtime.dispatchTeleport(resolveLiveSubject(runtime, dispatchPlan), dispatchPlan);
                return toActionFeedback(result, warpName);
            } catch (RuntimeException exception) {
                return new TerminalServerToolsSectionSnapshot.ActionFeedback(
                    "传送失败",
                    exception.getMessage() == null ? "Teleport failed" : exception.getMessage(),
                    TerminalNotificationSeverity.ERROR.name());
            }
        }

        @Override
        public TerminalServerToolsSectionSnapshot.ActionFeedback confirmQuickAction(EntityPlayerMP player,
            String quickAction) {
            ServerToolsRuntimeBridge runtime = serverToolsRuntimeProvider.resolve();
            if (runtime == null || !runtime.isRuntimeAvailable()) {
                return new TerminalServerToolsSectionSnapshot.ActionFeedback(
                    "传送失败",
                    "ServerTools runtime 不可用，请检查 dedicated server 启动日志与 PostgreSQL / Cluster 配置。",
                    TerminalNotificationSeverity.ERROR.name());
            }
            try {
                String action = quickAction == null ? "" : quickAction.trim().toLowerCase(Locale.ROOT);
                TeleportDispatchPlan dispatchPlan;
                String actionLabel;
                if ("home".equals(action)) {
                    dispatchPlan = runtime.prepareHomeTeleport(player);
                    actionLabel = "默认家园";
                } else if ("back".equals(action)) {
                    dispatchPlan = runtime.prepareBackTeleport(player);
                    actionLabel = "返回上一位置";
                } else if ("spawn".equals(action)) {
                    dispatchPlan = runtime.prepareSpawnTeleport(player);
                    actionLabel = "当前服出生点";
                } else {
                    return new TerminalServerToolsSectionSnapshot.ActionFeedback(
                        "传送已拒绝", "未知快捷传送动作。", TerminalNotificationSeverity.ERROR.name());
                }
                GatewayDispatchResult result = runtime.dispatchTeleport(resolveLiveSubject(runtime, dispatchPlan),
                    dispatchPlan);
                return toActionFeedback(result, actionLabel);
            } catch (RuntimeException exception) {
                return new TerminalServerToolsSectionSnapshot.ActionFeedback(
                    "传送失败",
                    exception.getMessage() == null ? "Teleport failed" : exception.getMessage(),
                    TerminalNotificationSeverity.ERROR.name());
            }
        }

        @Override
        public TerminalServerToolsSectionSnapshot.ActionFeedback confirmHome(EntityPlayerMP player, String homeName) {
            ServerToolsRuntimeBridge runtime = serverToolsRuntimeProvider.resolve();
            if (runtime == null || !runtime.isRuntimeAvailable()) return runtimeUnavailableFeedback();
            try {
                TeleportDispatchPlan plan = runtime.prepareHomeTeleport(player, homeName);
                return toActionFeedback(runtime.dispatchTeleport(resolveLiveSubject(runtime, plan), plan), "Home " + homeName);
            } catch (RuntimeException exception) {
                return failureFeedback("Home 传送失败", exception);
            }
        }

        @Override
        public TerminalServerToolsSectionSnapshot.ActionFeedback setHome(EntityPlayerMP player, String homeName) {
            ServerToolsRuntimeBridge runtime = serverToolsRuntimeProvider.resolve();
            if (runtime == null || !runtime.isRuntimeAvailable()) return runtimeUnavailableFeedback();
            try {
                PlayerHome home = runtime.setHome(player, homeName);
                return new TerminalServerToolsSectionSnapshot.ActionFeedback("Home 已设定",
                    "已将 " + home.getHomeName() + " 保存到 " + describeHomeTargetServer(home) + "。",
                    TerminalNotificationSeverity.SUCCESS.name());
            } catch (RuntimeException exception) {
                return failureFeedback("Home 设定失败", exception);
            }
        }

        @Override
        public TerminalServerToolsSectionSnapshot.ActionFeedback deleteHome(EntityPlayerMP player, String homeName) {
            ServerToolsRuntimeBridge runtime = serverToolsRuntimeProvider.resolve();
            if (runtime == null || !runtime.isRuntimeAvailable()) return runtimeUnavailableFeedback();
            try {
                if (!runtime.deleteHome(player, homeName)) {
                    return new TerminalServerToolsSectionSnapshot.ActionFeedback("Home 未删除",
                        "未找到个人 Home: " + homeName + "。", TerminalNotificationSeverity.WARNING.name());
                }
                return new TerminalServerToolsSectionSnapshot.ActionFeedback("Home 已删除",
                    "已删除个人 Home: " + homeName + "。", TerminalNotificationSeverity.SUCCESS.name());
            } catch (RuntimeException exception) {
                return failureFeedback("Home 删除失败", exception);
            }
        }

        @Override
        public TerminalServerToolsSectionSnapshot.ActionFeedback createTpa(EntityPlayerMP player,
            String targetPlayerName, String targetServerId) {
            ServerToolsRuntimeBridge runtime = serverToolsRuntimeProvider.resolve();
            if (runtime == null || !runtime.isRuntimeAvailable()) return runtimeUnavailableFeedback();
            try {
                TpaRequest request = runtime.createTpa(player, targetPlayerName, targetServerId);
                return new TerminalServerToolsSectionSnapshot.ActionFeedback("TPA 请求已发送",
                    "已向 " + request.getTargetPlayerName() + "（" + request.getTargetServerId()
                        + "）发出请求，30 秒内等待对方确认。",
                    TerminalNotificationSeverity.SUCCESS.name());
            } catch (RuntimeException exception) {
                return failureFeedback("TPA 请求失败", exception);
            }
        }

        @Override
        public TerminalServerToolsSectionSnapshot.ActionFeedback acceptTpa(EntityPlayerMP player,
            String requesterPlayerName) {
            return completeTpa(player, requesterPlayerName, "", "接受", new TpaCompletion() {
                @Override public TpaRequest complete(ServerToolsRuntimeBridge runtime, EntityPlayerMP actor,
                    String otherPlayer, String serverId) { return runtime.acceptTpa(actor, otherPlayer); }
            });
        }

        @Override
        public TerminalServerToolsSectionSnapshot.ActionFeedback denyTpa(EntityPlayerMP player,
            String requesterPlayerName) {
            return completeTpa(player, requesterPlayerName, "", "已拒绝", new TpaCompletion() {
                @Override public TpaRequest complete(ServerToolsRuntimeBridge runtime, EntityPlayerMP actor,
                    String otherPlayer, String serverId) { return runtime.denyTpa(actor, otherPlayer); }
            });
        }

        @Override
        public TerminalServerToolsSectionSnapshot.ActionFeedback cancelTpa(EntityPlayerMP player,
            String targetPlayerName, String targetServerId) {
            return completeTpa(player, targetPlayerName, targetServerId, "已取消", new TpaCompletion() {
                @Override public TpaRequest complete(ServerToolsRuntimeBridge runtime, EntityPlayerMP actor,
                    String otherPlayer, String serverId) { return runtime.cancelTpa(actor, otherPlayer, serverId); }
            });
        }

        private static TerminalServerToolsSectionSnapshot.ActionFeedback completeTpa(EntityPlayerMP player,
            String otherPlayer, String targetServerId, String action, TpaCompletion completion) {
            ServerToolsRuntimeBridge runtime = serverToolsRuntimeProvider.resolve();
            if (runtime == null || !runtime.isRuntimeAvailable()) return runtimeUnavailableFeedback();
            try {
                TpaRequest request = completion.complete(runtime, player, otherPlayer, targetServerId);
                return new TerminalServerToolsSectionSnapshot.ActionFeedback("TPA " + action,
                    "与 " + (request == null ? otherPlayer : request.getRequesterPlayerName()) + " 的请求状态已更新。",
                    TerminalNotificationSeverity.SUCCESS.name());
            } catch (RuntimeException exception) {
                return failureFeedback("TPA 操作失败", exception);
            }
        }

        private interface TpaCompletion {
            TpaRequest complete(ServerToolsRuntimeBridge runtime, EntityPlayerMP actor, String otherPlayer,
                String serverId);
        }

        private static TerminalServerToolsSectionSnapshot unavailableSnapshot(
            TerminalServerToolsSectionSnapshot.ActionFeedback actionFeedback) {
            return new TerminalServerToolsSectionSnapshot(
                "ServerTools runtime unavailable",
                "unknown",
                Arrays.asList("服务器目录不可用: runtime 未准备完成。"),
                Arrays.asList(""),
                Arrays.asList("当前没有可用系统 warp。"),
                Arrays.asList(""),
                Arrays.asList("当前没有额外说明。"),
                Arrays.asList("不可用"),
                Arrays.asList("当前没有最近传送记录。"),
                "",
                "未选择 warp",
                "ServerTools runtime 不可用，无法读取 warp 列表。",
                "--",
                "--",
                "ServerTools runtime 不可用。",
                false,
                "--",
                "--",
                "不可用",
                "--",
                "当前没有最近传送记录。",
                actionFeedback == null ? new TerminalServerToolsSectionSnapshot.ActionFeedback(
                    "传送页不可用",
                    "ServerTools runtime 不可用，请检查 dedicated server 启动日志与 PostgreSQL / Cluster 配置。",
                    TerminalNotificationSeverity.WARNING.name()) : actionFeedback);
        }

        private static List<String> toServerLines(List<ServerDescriptor> servers) {
            List<String> lines = new ArrayList<String>();
            for (ServerDescriptor server : servers) {
                lines.add(server.getServerId() + " | " + server.getDisplayName()
                    + (server.isLocalServer() ? " | 当前" : "")
                    + (server.isEnabled() ? " | 在线目录" : " | 已禁用"));
            }
            if (lines.isEmpty()) {
                lines.add("服务器目录暂不可用。");
            }
            return lines;
        }

        private static List<String> toServerIds(List<ServerDescriptor> servers) {
            List<String> ids = new ArrayList<String>();
            for (ServerDescriptor server : servers) {
                ids.add(server.getServerId());
            }
            if (ids.isEmpty()) {
                ids.add("");
            }
            return ids;
        }

        private static List<String> toWarpLines(List<ServerWarp> warps) {
            List<String> lines = new ArrayList<String>();
            for (ServerWarp warp : warps) {
                lines.add((warp.isEnabled() ? "[可用] " : "[禁用] ") + displayWarpTitle(warp));
            }
            if (lines.isEmpty()) {
                lines.add("当前没有可用系统 warp。");
            }
            return lines;
        }

        private static List<String> toWarpNames(List<ServerWarp> warps) {
            List<String> names = new ArrayList<String>();
            for (ServerWarp warp : warps) {
                names.add(warp.getWarpName());
            }
            if (names.isEmpty()) {
                names.add("");
            }
            return names;
        }

        private static List<String> toWarpSubtitles(List<ServerWarp> warps) {
            List<String> subtitles = new ArrayList<String>();
            for (ServerWarp warp : warps) {
                if (warp == null) {
                    continue;
                }
                String description = normalize(warp.getDescription(), "");
                if (!description.isEmpty()) {
                    subtitles.add(description);
                } else if (warp.getTarget() != null) {
                    subtitles.add("目标服 " + normalize(warp.getTarget().getServerId(), "unknown"));
                } else {
                    subtitles.add("当前没有额外说明。");
                }
            }
            if (subtitles.isEmpty()) {
                subtitles.add("当前没有额外说明。");
            }
            return subtitles;
        }

        private static List<String> toWarpStateLabels(List<ServerWarp> warps) {
            List<String> labels = new ArrayList<String>();
            for (ServerWarp warp : warps) {
                labels.add(warp != null && warp.isEnabled() ? "可用" : "禁用");
            }
            if (labels.isEmpty()) {
                labels.add("不可用");
            }
            return labels;
        }

        private static List<String> toRecentTransferLines(List<TransferTicket> tickets) {
            List<String> lines = new ArrayList<String>();
            for (TransferTicket ticket : tickets) {
                if (ticket == null || ticket.getTarget() == null) {
                    continue;
                }
                lines.add(formatTicketLine(ticket));
            }
            if (lines.isEmpty()) {
                lines.add("当前没有最近传送记录。");
            }
            return lines;
        }

        private static List<String> toHomeLines(List<PlayerHome> homes) {
            List<String> lines = new ArrayList<String>();
            for (PlayerHome home : homes) {
                if (home != null) lines.add(home.getHomeName() + " | " + describeHomeTargetServer(home));
            }
            if (lines.isEmpty()) lines.add("当前没有已设定的 Home。");
            return lines;
        }

        private static List<String> toHomeNames(List<PlayerHome> homes) {
            List<String> names = new ArrayList<String>();
            for (PlayerHome home : homes) if (home != null) names.add(home.getHomeName());
            if (names.isEmpty()) names.add("");
            return names;
        }

        private static List<String> toHomeSubtitles(List<PlayerHome> homes) {
            List<String> subtitles = new ArrayList<String>();
            for (PlayerHome home : homes) if (home != null) subtitles.add(describeHomeTargetLocation(home));
            if (subtitles.isEmpty()) subtitles.add("可在当前位置设定第一个 Home。");
            return subtitles;
        }

        /** A terminal-safe projection: only the current user's counterpart, server and state are exposed. */
        private static final class TpaViewLists {
            private final List<String> directions = new ArrayList<String>();
            private final List<String> counterpartyNames = new ArrayList<String>();
            private final List<String> targetServerIds = new ArrayList<String>();
            private final List<String> statusLabels = new ArrayList<String>();

            private static TpaViewLists from(List<TpaRequest> outgoing, List<TpaRequest> incoming) {
                TpaViewLists views = new TpaViewLists();
                views.append(outgoing, "OUTGOING");
                views.append(incoming, "INCOMING");
                return views;
            }

            private void append(List<TpaRequest> requests, String direction) {
                if (requests == null) return;
                for (TpaRequest request : requests) {
                    if (request == null || request.getStatus() == null) continue;
                    directions.add(direction);
                    counterpartyNames.add("INCOMING".equals(direction) ? request.getRequesterPlayerName()
                        : request.getTargetPlayerName());
                    targetServerIds.add(normalize(request.getTargetServerId(), "--"));
                    statusLabels.add(request.getStatus().name());
                }
            }
        }

        private static String formatTicketLine(TransferTicket ticket) {
            String timestamp = formatInstant(ticket.getUpdatedAt());
            String statusMessage = normalize(ticket.getStatusMessage(), "无额外状态说明");
            return timestamp + " | " + ticket.getSourceServerId() + " -> " + ticket.getTarget().getServerId()
                + " | " + ticket.getStatus().name() + " | " + statusMessage;
        }

        private static ServerWarp findWarp(List<ServerWarp> warps, String warpName) {
            if (warpName == null || warpName.trim().isEmpty()) {
                return null;
            }
            for (ServerWarp warp : warps) {
                if (warp.getWarpName().equalsIgnoreCase(warpName.trim())) {
                    return warp;
                }
            }
            return null;
        }

        private static PlayerHome findHome(List<PlayerHome> homes, String homeName) {
            if (homeName == null || homeName.trim().isEmpty()) return null;
            for (PlayerHome home : homes) {
                if (home != null && home.getHomeName().equalsIgnoreCase(homeName.trim())) return home;
            }
            return null;
        }

        private static String describeHomeTargetServer(PlayerHome home) {
            return home == null || home.getTarget() == null ? "--" : normalize(home.getTarget().getServerId(), "--");
        }

        private static String describeHomeTargetLocation(PlayerHome home) {
            if (home == null || home.getTarget() == null) return "--";
            return "dim " + home.getTarget().getDimensionId() + " / " + Math.round(home.getTarget().getX())
                + ", " + Math.round(home.getTarget().getY()) + ", " + Math.round(home.getTarget().getZ());
        }

        private static String describeHome(PlayerHome home) {
            return "Home " + home.getHomeName() + " -> " + describeHomeTargetServer(home) + " / "
                + describeHomeTargetLocation(home);
        }

        private static TerminalServerToolsSectionSnapshot.ActionFeedback runtimeUnavailableFeedback() {
            return new TerminalServerToolsSectionSnapshot.ActionFeedback("Home 操作失败",
                "ServerTools runtime 不可用，请检查 dedicated server 启动日志与 PostgreSQL / Cluster 配置。",
                TerminalNotificationSeverity.ERROR.name());
        }

        private static TerminalServerToolsSectionSnapshot.ActionFeedback failureFeedback(String title,
            RuntimeException exception) {
            return new TerminalServerToolsSectionSnapshot.ActionFeedback(title,
                exception.getMessage() == null ? "ServerTools 操作失败。" : exception.getMessage(),
                TerminalNotificationSeverity.ERROR.name());
        }

        private static String displayWarpTitle(ServerWarp warp) {
            String displayName = normalize(warp.getDisplayName(), "");
            return displayName.isEmpty() || displayName.equals(warp.getWarpName())
                ? warp.getWarpName()
                : displayName + " / " + warp.getWarpName();
        }

        private static String describeWarpTargetServer(ServerWarp warp) {
            return warp == null || warp.getTarget() == null ? "--" : normalize(warp.getTarget().getServerId(), "--");
        }

        private static String describeWarpTargetLocation(ServerWarp warp) {
            if (warp == null || warp.getTarget() == null) {
                return "--";
            }
            return "dim " + warp.getTarget().getDimensionId() + " / "
                + Math.round(warp.getTarget().getX()) + ", "
                + Math.round(warp.getTarget().getY()) + ", "
                + Math.round(warp.getTarget().getZ());
        }

        private static String describeWarp(ServerWarp warp) {
            String target = warp.getTarget() == null ? "target=unknown"
                : "target=" + warp.getTarget().getServerId() + " / " + Math.round(warp.getTarget().getX())
                    + ", " + Math.round(warp.getTarget().getY()) + ", " + Math.round(warp.getTarget().getZ());
            String description = normalize(warp.getDescription(), "没有额外说明。");
            return target + " | " + description;
        }

        private static String resolveRecentSourceServerId(List<TransferTicket> tickets) {
            TransferTicket ticket = firstTicket(tickets);
            return ticket == null ? "--" : normalize(ticket.getSourceServerId(), "--");
        }

        private static String resolveRecentTargetServerId(List<TransferTicket> tickets) {
            TransferTicket ticket = firstTicket(tickets);
            return ticket == null || ticket.getTarget() == null ? "--"
                : normalize(ticket.getTarget().getServerId(), "--");
        }

        private static String resolveRecentTransferStatus(List<TransferTicket> tickets) {
            TransferTicket ticket = firstTicket(tickets);
            return ticket == null || ticket.getStatus() == null ? "暂无记录" : ticket.getStatus().name();
        }

        private static String resolveRecentTransferTime(List<TransferTicket> tickets) {
            TransferTicket ticket = firstTicket(tickets);
            return ticket == null ? "--" : formatInstant(ticket.getUpdatedAt());
        }

        private static String resolveRecentTransferSummary(List<TransferTicket> tickets) {
            TransferTicket ticket = firstTicket(tickets);
            return ticket == null ? "当前没有最近传送记录。"
                : normalize(ticket.getStatusMessage(), "当前没有最近传送记录。");
        }

        private static TransferTicket firstTicket(List<TransferTicket> tickets) {
            return tickets == null || tickets.isEmpty() ? null : tickets.get(0);
        }

        private static EntityPlayerMP resolveLiveSubject(ServerToolsRuntimeBridge runtime, TeleportDispatchPlan dispatchPlan) {
            if (runtime.getLocalServerId() == null || !runtime.getLocalServerId().equals(dispatchPlan.getSourceServerId())) {
                return null;
            }
            return runtime.findOnlinePlayer(dispatchPlan.getSubjectPlayerName());
        }

        private static TerminalServerToolsSectionSnapshot.ActionFeedback toActionFeedback(GatewayDispatchResult result,
            String warpName) {
            if (result.getStatus() == GatewayDispatchResult.Status.COMPLETED_LOCAL) {
                return new TerminalServerToolsSectionSnapshot.ActionFeedback(
                    "本服传送完成",
                    "已执行 warp: " + warpName,
                    TerminalNotificationSeverity.SUCCESS.name());
            }
            if (result.getStatus() == GatewayDispatchResult.Status.PENDING_REMOTE) {
                return new TerminalServerToolsSectionSnapshot.ActionFeedback(
                    "跨服传送已提交",
                    result.getMessage() == null ? "Transfer ticket created / pending remote." : result.getMessage(),
                    TerminalNotificationSeverity.SUCCESS.name());
            }
            return new TerminalServerToolsSectionSnapshot.ActionFeedback(
                "传送失败",
                result.getMessage() == null ? "Teleport failed" : result.getMessage(),
                TerminalNotificationSeverity.ERROR.name());
        }
    }

    private static final class DefaultServerToolsRuntimeProvider implements ServerToolsRuntimeProvider {

        @Override
        public ServerToolsRuntimeBridge resolve() {
            if (GalaxyBase.proxy == null || GalaxyBase.proxy.getModuleManager() == null) {
                return null;
            }
            ServerToolsModule module = GalaxyBase.proxy.getModuleManager().findModule(ServerToolsModule.class);
            return module == null ? null : new ModuleBackedServerToolsRuntimeBridge(module);
        }
    }

    private static final class ModuleBackedServerToolsRuntimeBridge implements ServerToolsRuntimeBridge {

        private final ServerToolsModule module;

        private ModuleBackedServerToolsRuntimeBridge(ServerToolsModule module) {
            this.module = module;
        }

        @Override
        public boolean isRuntimeAvailable() {
            return module != null && module.isRuntimeAvailable() && module.getPlayerTeleportService() != null;
        }

        @Override
        public String getLocalServerId() {
            return module == null ? null : module.getLocalServerId();
        }

        @Override
        public List<ServerDescriptor> listServers() {
            ClusterInfrastructure clusterInfrastructure = module == null ? null : module.getClusterInfrastructure();
            if (clusterInfrastructure == null || clusterInfrastructure.getServerDirectory() == null) {
                return new ArrayList<ServerDescriptor>();
            }
            try {
                return clusterInfrastructure.getServerDirectory().listAll();
            } catch (RuntimeException ignored) {
                return new ArrayList<ServerDescriptor>();
            }
        }

        @Override
        public List<ServerWarp> listWarps() {
            if (module == null || module.getPlayerTeleportService() == null) {
                return new ArrayList<ServerWarp>();
            }
            try {
                return module.getPlayerTeleportService().listWarps();
            } catch (RuntimeException ignored) {
                return new ArrayList<ServerWarp>();
            }
        }

        @Override
        public List<TransferTicket> findRecentTickets(String playerUuid, int limit) {
            ClusterInfrastructure clusterInfrastructure = module == null ? null : module.getClusterInfrastructure();
            if (clusterInfrastructure == null || clusterInfrastructure.getTeleportTicketRepository() == null
                || playerUuid == null || playerUuid.trim().isEmpty()) {
                return new ArrayList<TransferTicket>();
            }
            try {
                return clusterInfrastructure.getTeleportTicketRepository().findRecentForPlayer(playerUuid, limit);
            } catch (RuntimeException ignored) {
                return new ArrayList<TransferTicket>();
            }
        }

        @Override
        public List<PlayerHome> listHomes(String playerUuid) {
            if (module == null || module.getPlayerTeleportService() == null || playerUuid == null || playerUuid.trim().isEmpty()) {
                return new ArrayList<PlayerHome>();
            }
            try {
                return module.getPlayerTeleportService().listHomes(playerUuid);
            } catch (RuntimeException ignored) {
                return new ArrayList<PlayerHome>();
            }
        }

        @Override
        public List<TpaRequest> listOutgoingTpa(EntityPlayerMP player, int limit) {
            if (module == null || module.getPlayerTeleportService() == null || player == null) {
                return new ArrayList<TpaRequest>();
            }
            return module.getPlayerTeleportService().listRecentTpaRequestsForRequester(module.getLocalServerId(),
                player.getUniqueID().toString(), limit);
        }

        @Override
        public List<TpaRequest> listIncomingTpa(EntityPlayerMP player, int limit) {
            if (module == null || module.getPlayerTeleportService() == null || player == null) {
                return new ArrayList<TpaRequest>();
            }
            return module.getPlayerTeleportService().listRecentTpaRequestsForTarget(module.getLocalServerId(),
                player.getCommandSenderName(), limit);
        }

        @Override
        public TpaRequest createTpa(EntityPlayerMP player, String targetPlayerName, String targetServerId) {
            return module.getPlayerTeleportService().createTpaRequest(module.captureActor(player),
                PlayerTeleportService.newRequestId("terminal-tpa"), targetPlayerName, targetServerId, Instant.now());
        }

        @Override
        public TpaRequest acceptTpa(EntityPlayerMP player, String requesterPlayerName) {
            return module.getPlayerTeleportService().acceptTpa(module.captureActor(player), requesterPlayerName,
                Instant.now());
        }

        @Override
        public TpaRequest denyTpa(EntityPlayerMP player, String requesterPlayerName) {
            return module.getPlayerTeleportService().declineTpa(module.captureActor(player), requesterPlayerName,
                Instant.now());
        }

        @Override
        public TpaRequest cancelTpa(EntityPlayerMP player, String targetPlayerName, String targetServerId) {
            return module.getPlayerTeleportService().cancelTpa(module.captureActor(player), targetPlayerName,
                targetServerId, Instant.now());
        }

        @Override
        public TeleportDispatchPlan prepareWarpTeleport(EntityPlayerMP player, String warpName) {
            return module.getPlayerTeleportService().prepareWarpTeleport(
                module.captureActor(player),
                PlayerTeleportService.newRequestId("terminal-warp"),
                warpName);
        }

        @Override
        public TeleportDispatchPlan prepareHomeTeleport(EntityPlayerMP player) {
            return prepareHomeTeleport(player, "home");
        }

        @Override
        public TeleportDispatchPlan prepareHomeTeleport(EntityPlayerMP player, String homeName) {
            return module.getPlayerTeleportService().prepareHomeTeleport(module.captureActor(player),
                PlayerTeleportService.newRequestId("terminal-home"), homeName);
        }

        @Override
        public PlayerHome setHome(EntityPlayerMP player, String homeName) {
            return module.getPlayerTeleportService().setHome(module.captureActor(player), homeName);
        }

        @Override
        public boolean deleteHome(EntityPlayerMP player, String homeName) {
            return module.getPlayerTeleportService().deleteHome(player.getUniqueID().toString(), homeName);
        }

        @Override
        public TeleportDispatchPlan prepareBackTeleport(EntityPlayerMP player) {
            return module.getPlayerTeleportService().prepareBackTeleport(module.captureActor(player),
                PlayerTeleportService.newRequestId("terminal-back"));
        }

        @Override
        public TeleportDispatchPlan prepareSpawnTeleport(EntityPlayerMP player) {
            World world = player == null ? null : player.worldObj;
            if (world == null) {
                throw new IllegalStateException("Current world is unavailable");
            }
            ChunkCoordinates spawn = world.getSpawnPoint();
            int y = spawn.posY;
            while (world.getBlock(spawn.posX, y, spawn.posZ).isNormalCube()) {
                y += 2;
            }
            TeleportTarget target = new TeleportTarget(module.getLocalServerId(), world.provider.dimensionId,
                spawn.posX + 0.5D, y + 0.1D, spawn.posZ + 0.5D, 0.0F, 0.0F);
            return module.getPlayerTeleportService().prepareSpawnTeleport(module.captureActor(player),
                PlayerTeleportService.newRequestId("terminal-spawn"), target);
        }

        @Override
        public GatewayDispatchResult dispatchTeleport(EntityPlayerMP player, TeleportDispatchPlan dispatchPlan) {
            return module.dispatchTeleport(player, dispatchPlan);
        }

        @Override
        public EntityPlayerMP findOnlinePlayer(String playerName) {
            return module.findOnlinePlayer(playerName);
        }
    }

    private static String formatInstant(Instant instant) {
        return instant == null ? "--" : SERVER_TOOLS_TIME_FORMATTER.format(instant);
    }

    private static final class BankActionContext {

        private final TerminalBankSnapshot snapshot;
        private final TerminalBankActionPayload payload;
        private final TerminalBankingService.ActionResult actionResult;

        private BankActionContext(TerminalBankSnapshot snapshot, TerminalBankActionPayload payload,
            TerminalBankingService.ActionResult actionResult) {
            this.snapshot = snapshot;
            this.payload = payload == null ? TerminalBankActionPayload.empty() : payload;
            this.actionResult = actionResult;
        }
    }

    private static final class MarketActionContext {

        private final TerminalMarketSectionSnapshot snapshot;
        private final TerminalCustomMarketSectionSnapshot customSnapshot;
        private final TerminalExchangeMarketSectionSnapshot exchangeSnapshot;
        private final TerminalActionFeedback actionResult;

        private MarketActionContext(TerminalMarketSectionSnapshot snapshot,
            TerminalCustomMarketSectionSnapshot customSnapshot,
            TerminalExchangeMarketSectionSnapshot exchangeSnapshot,
            TerminalActionFeedback actionResult) {
            this.snapshot = snapshot == null ? TerminalMarketSectionSnapshot.placeholder(TerminalPage.MARKET.getId()) : snapshot;
            this.customSnapshot = customSnapshot;
            this.exchangeSnapshot = exchangeSnapshot;
            this.actionResult = actionResult;
        }
    }

    private static final class ServerToolsActionContext {

        private final TerminalServerToolsSectionSnapshot snapshot;
        private final TerminalServerToolsActionPayload payload;
        private final TerminalServerToolsSectionSnapshot.ActionFeedback actionFeedback;

        private ServerToolsActionContext(TerminalServerToolsSectionSnapshot snapshot,
            TerminalServerToolsActionPayload payload,
            TerminalServerToolsSectionSnapshot.ActionFeedback actionFeedback) {
            this.snapshot = snapshot == null ? TerminalServerToolsSectionSnapshot.placeholder() : snapshot;
            this.payload = payload == null ? TerminalServerToolsActionPayload.empty() : payload;
            this.actionFeedback = actionFeedback;
        }
    }

    private static final class LandActionContext {

        private final TerminalLandSectionSnapshot snapshot;
        private final TerminalLandActionPayload payload;

        private LandActionContext(TerminalLandSectionSnapshot snapshot, TerminalLandActionPayload payload) {
            this.snapshot = snapshot == null ? TerminalLandSectionSnapshot.unavailable() : snapshot;
            this.payload = payload == null ? TerminalLandActionPayload.empty() : payload;
        }
    }
}
