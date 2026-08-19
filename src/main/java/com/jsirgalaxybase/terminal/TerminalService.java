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

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.cluster.domain.ServerDescriptor;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicket;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.vault.application.BaseVaultService;
import com.jsirgalaxybase.modules.core.vault.domain.VaultSlot;
import com.jsirgalaxybase.modules.core.vault.infrastructure.minecraft.BaseVaultGuiHandler;
import com.jsirgalaxybase.modules.cluster.infrastructure.ClusterInfrastructure;
import com.jsirgalaxybase.modules.servertools.ServerToolsModule;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.domain.ServerWarp;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;
import com.jsirgalaxybase.terminal.ui.TerminalBankSnapshot;
import com.jsirgalaxybase.terminal.ui.TerminalBankSnapshotProvider;
import com.jsirgalaxybase.terminal.ui.TerminalBankingService;
import com.jsirgalaxybase.terminal.ui.TerminalActionFeedback;
import com.jsirgalaxybase.terminal.ui.TerminalHomeSnapshot;
import com.jsirgalaxybase.terminal.ui.TerminalHomeSnapshotProvider;
import com.jsirgalaxybase.terminal.ui.TerminalMarketSectionService;
import com.jsirgalaxybase.terminal.ui.TerminalNotification;
import com.jsirgalaxybase.terminal.ui.TerminalNotificationSeverity;
import com.jsirgalaxybase.terminal.ui.TerminalPage;

public final class TerminalService {

    private static final DateTimeFormatter SERVER_TOOLS_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.ROOT).withZone(ZoneId.systemDefault());

    static BankPageFacade bankPageFacade = new DefaultBankPageFacade();
    static MarketPageFacade marketPageFacade = new DefaultMarketPageFacade();
    static ServerToolsPageFacade serverToolsPageFacade = new DefaultServerToolsPageFacade();
    static ServerToolsRuntimeProvider serverToolsRuntimeProvider = new DefaultServerToolsRuntimeProvider();
    static final TerminalExchangeQuoteConfirmationGate exchangeQuoteConfirmationGate =
        new TerminalExchangeQuoteConfirmationGate();

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
            && TerminalPage.fromId(pageId) == TerminalPage.VAULT) {
            if (!BaseVaultGuiHandler.openPersonalVault(player)) {
                GalaxyBase.LOG.warn("Base Vault GUI was requested before its server runtime was ready for {}",
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
            createPageSnapshots(player, snapshot, bankContext, marketContext, serverToolsContext, selectedPage),
            createNotifications(selectedPage, actionType, bankContext, marketContext, serverToolsContext),
            normalizedSessionToken);
    }

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
            }
        }

        TerminalServerToolsSectionSnapshot latestSnapshot =
            serverToolsPageFacade.createSnapshot(player, serverToolsPayload, actionFeedback);
        return new ServerToolsActionContext(latestSnapshot, serverToolsPayload, actionFeedback);
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
            TerminalPage.SERVER_TOOLS,
            TerminalPage.BANK,
            TerminalPage.VAULT };
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
        ServerToolsActionContext serverToolsContext, TerminalPage selectedPage) {
        List<TerminalOpenApproval.PageSnapshot> pageSnapshots = new ArrayList<TerminalOpenApproval.PageSnapshot>();
        pageSnapshots.add(createHomePageSnapshot(snapshot));
        pageSnapshots.add(createCareerPageSnapshot(player));
        pageSnapshots.add(createPublicServicePageSnapshot(player));
        pageSnapshots.add(createMarketPageSnapshot(selectedPage, marketContext));
        pageSnapshots.add(createBankPageSnapshot(bankContext));
        pageSnapshots.add(createServerToolsPageSnapshot(serverToolsContext));
        pageSnapshots.add(createVaultPageSnapshot(player));
        return pageSnapshots;
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

    private static TerminalOpenApproval.PageSnapshot createCareerPageSnapshot(EntityPlayer player) {
        return createLinePageSnapshot(TerminalPage.CAREER, TerminalHomeSnapshotProvider.INSTANCE.createCareerPageLines(player));
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

    private static List<TerminalOpenApproval.NotificationEntry> createNotifications(TerminalPage selectedPage,
        TerminalActionType actionType, BankActionContext bankContext, MarketActionContext marketContext,
        ServerToolsActionContext serverToolsContext) {
        List<TerminalOpenApproval.NotificationEntry> notifications = new ArrayList<TerminalOpenApproval.NotificationEntry>();
        if (selectedPage.isMarketPage()) {
            if (marketContext.actionResult != null) {
                notifications.add(new TerminalOpenApproval.NotificationEntry(
                    marketContext.actionResult.getTitle(),
                    marketContext.actionResult.getBody(),
                    marketContext.actionResult.getSeverity().name()));
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
                    serverToolsContext.actionFeedback.getSeverityName()));
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

        TeleportDispatchPlan prepareWarpTeleport(EntityPlayerMP player, String warpName);

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
            String selectedWarpName = payload == null ? "" : payload.getWarpName();
            ServerWarp selectedWarp = findWarp(warps, selectedWarpName);
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
                selectedWarpName,
                selectedWarp == null ? "未选择 warp" : displayWarpTitle(selectedWarp),
                selectedWarp == null ? "当前没有可查看的 warp 详情。" : describeWarp(selectedWarp),
                selectedWarp == null ? "--" : describeWarpTargetServer(selectedWarp),
                selectedWarp == null ? "--" : describeWarpTargetLocation(selectedWarp),
                selectedWarp == null ? "当前没有额外传送说明。" : normalize(selectedWarp.getDescription(), "当前没有额外传送说明。"),
                selectedWarp != null && selectedWarp.isEnabled(),
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
        public TeleportDispatchPlan prepareWarpTeleport(EntityPlayerMP player, String warpName) {
            return module.getPlayerTeleportService().prepareWarpTeleport(
                module.captureActor(player),
                PlayerTeleportService.newRequestId("terminal-warp"),
                warpName);
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
}
