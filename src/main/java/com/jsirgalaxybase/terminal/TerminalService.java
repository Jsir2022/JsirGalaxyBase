package com.jsirgalaxybase.terminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

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

    static BankPageFacade bankPageFacade = new DefaultBankPageFacade();
    static MarketPageFacade marketPageFacade = new DefaultMarketPageFacade();

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
        return buildTerminalSnapshot(player, pageId, sessionToken, TerminalActionType.fromId(actionType), payload);
    }

    static TerminalOpenApproval buildTerminalSnapshot(EntityPlayer player, String pageId, String sessionToken,
        TerminalActionType actionType, String payload) {
        TerminalPage selectedPage = TerminalPage.fromId(pageId);
        String normalizedPageId = selectedPage.getId();
        String normalizedSessionToken = normalize(sessionToken, "terminal-session");
        String playerName = player == null ? "访客" : player.getCommandSenderName();
        TerminalHomeSnapshot snapshot = TerminalHomeSnapshotProvider.INSTANCE.create(player);
        BankActionContext bankContext = buildBankActionContext(player, selectedPage, actionType, payload);
        MarketActionContext marketContext = buildMarketActionContext(player, selectedPage, actionType, payload);
        return new TerminalOpenApproval(
            normalizedPageId,
            "银河终端 / " + (playerName == null || playerName.trim().isEmpty() ? "访客" : playerName),
            "服务端授权已通过，当前为 phase 9 新终端壳正式入口主链",
            new TerminalOpenApproval.StatusBand(
                "当前页",
                selectedPage.getTitle(),
                buildStatusDetail(snapshot, selectedPage, actionType, bankContext, marketContext),
                "贡献",
                String.valueOf(snapshot == null ? 0 : snapshot.getContribution())),
            createTopLevelNavItems(normalizedPageId),
            createPageSnapshots(player, snapshot, bankContext, marketContext, selectedPage),
            createNotifications(selectedPage, actionType, bankContext, marketContext),
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

    private static MarketActionContext buildMarketActionContext(EntityPlayer player, TerminalPage selectedPage,
        TerminalActionType actionType, String payload) {
        TerminalMarketActionPayload marketPayload = TerminalMarketActionPayload.decode(payload);
        TerminalCustomMarketActionPayload customPayload = TerminalCustomMarketActionPayload.decode(payload);
        TerminalExchangeMarketActionPayload exchangePayload = TerminalExchangeMarketActionPayload.decode(payload);
        TerminalActionFeedback actionResult = null;

        if (selectedPage == TerminalPage.MARKET_STANDARDIZED
            && actionType == TerminalActionType.MARKET_CONFIRM_LIMIT_BUY) {
            actionResult = marketPageFacade.submitLimitBuy(player, marketPayload);
            if (actionResult != null && actionResult.getSeverity() == TerminalNotificationSeverity.SUCCESS) {
                marketPayload = marketPayload.clearedAfterLimitBuySuccess();
            }
        } else if (selectedPage == TerminalPage.MARKET_STANDARDIZED
            && actionType == TerminalActionType.MARKET_CLAIM_ASSET) {
            actionResult = marketPageFacade.claimAsset(player, marketPayload);
            if (actionResult != null && actionResult.getSeverity() == TerminalNotificationSeverity.SUCCESS) {
                marketPayload = marketPayload.clearedAfterClaimSuccess();
            }
        } else if (selectedPage == TerminalPage.MARKET_STANDARDIZED
            && (actionType == TerminalActionType.MARKET_REFRESH || actionType == TerminalActionType.REFRESH_PAGE)) {
            actionResult = TerminalActionFeedback.info("标准商品市场已刷新", "当前商品详情、盘口和 CLAIMABLE 摘要已刷新。", 3200L);
        } else if (selectedPage == TerminalPage.MARKET
            && (actionType == TerminalActionType.MARKET_REFRESH || actionType == TerminalActionType.REFRESH_PAGE)) {
            actionResult = TerminalActionFeedback.info("市场总入口已刷新", "市场共享摘要与入口卡已刷新。", 3200L);
        } else if (selectedPage == TerminalPage.MARKET_CUSTOM
            && (actionType == TerminalActionType.MARKET_CUSTOM_REFRESH || actionType == TerminalActionType.MARKET_REFRESH
                || actionType == TerminalActionType.REFRESH_PAGE || actionType == TerminalActionType.MARKET_CUSTOM_SELECT_LISTING)) {
            actionResult = TerminalActionFeedback.info("定制商品市场已刷新", "listing 浏览、详情与个人资产摘要已刷新。", 3200L);
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
                : TerminalActionFeedback.info("汇率市场已刷新", "标的、quote、rule 与执行门禁已刷新。", 3200L);
        } else if (selectedPage == TerminalPage.MARKET_EXCHANGE
            && actionType == TerminalActionType.MARKET_EXCHANGE_CONFIRM) {
            actionResult = exchangePayload.hasSelectedTarget()
                ? marketPageFacade.submitExchangeHeld(player)
                : TerminalActionFeedback.of(
                    TerminalNotificationSeverity.ERROR,
                    "汇率兑换已拒绝",
                    "服务端拒绝未选择正式兑换标的的确认请求。",
                    3600L);
        }

        TerminalMarketSectionSnapshot latestSnapshot = selectedPage == TerminalPage.MARKET_CUSTOM
            || selectedPage == TerminalPage.MARKET_EXCHANGE
                ? TerminalMarketSectionSnapshot.placeholder(TerminalPage.MARKET.getId())
                : marketPageFacade.createSnapshot(player, selectedPage, marketPayload, actionResult);
        TerminalCustomMarketSectionSnapshot customSnapshot = selectedPage == TerminalPage.MARKET_CUSTOM
            ? marketPageFacade.createCustomSnapshot(player, customPayload, actionResult)
            : null;
        TerminalExchangeMarketSectionSnapshot exchangeSnapshot = selectedPage == TerminalPage.MARKET_EXCHANGE
            ? marketPageFacade.createExchangeSnapshot(player, exchangePayload, actionResult)
            : null;
        return new MarketActionContext(latestSnapshot, customSnapshot, exchangeSnapshot, actionResult);
    }

    private static boolean canOpenTerminal(EntityPlayerMP player) {
        return player != null && player.playerNetServerHandler != null && !player.isDead;
    }

    private static List<TerminalOpenApproval.NavItem> createTopLevelNavItems(String selectedPageId) {
        List<TerminalOpenApproval.NavItem> items = new ArrayList<TerminalOpenApproval.NavItem>();
        TerminalPage[] pages = new TerminalPage[] {
            TerminalPage.HOME,
            TerminalPage.CAREER,
            TerminalPage.PUBLIC_SERVICE,
            TerminalPage.MARKET,
            TerminalPage.BANK };
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
        TerminalPage selectedPage) {
        List<TerminalOpenApproval.PageSnapshot> pageSnapshots = new ArrayList<TerminalOpenApproval.PageSnapshot>();
        pageSnapshots.add(createHomePageSnapshot(snapshot));
        pageSnapshots.add(createCareerPageSnapshot(player));
        pageSnapshots.add(createPublicServicePageSnapshot(player));
        pageSnapshots.add(createMarketPageSnapshot(selectedPage, marketContext));
        pageSnapshots.add(createBankPageSnapshot(bankContext));
        return pageSnapshots;
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
            "phase 7 之后，新壳已具备银行、三类市场的真实 action / snapshot / popup 闭环。"));
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
        if (effectivePage == TerminalPage.MARKET_STANDARDIZED) {
            sections.add(new TerminalOpenApproval.Section(
                "market_standardized_runtime",
                "标准商品运行态",
                snapshot.getServiceState(),
                snapshot.getSummaryNotice()));
            sections.add(new TerminalOpenApproval.Section(
                "market_standardized_focus",
                "当前交易焦点",
                snapshot.getSelectedProductName() + " | 买一 " + snapshot.getHighestBid() + " / 卖一 " + snapshot.getLowestAsk(),
                "24h 成交量 " + snapshot.getVolume24h() + " | CLAIMABLE " + snapshot.getClaimableQuantity()));
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
                "最新成交价 " + snapshot.getLatestTradePrice() + " | CLAIMABLE " + snapshot.getClaimableQuantity(),
                "MARKET_STANDARDIZED 现在承接真实买单与 claim 动作。"));
            sections.add(new TerminalOpenApproval.Section(
                "market_overview_boundary",
                "迁移边界",
                "MARKET 根页继续只做总入口与共享摘要。",
                "三类市场业务页已迁入；cutover 与旧终端删除仍留给 phase 8 / phase 9。"));
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
        TerminalActionType actionType, BankActionContext bankContext, MarketActionContext marketContext) {
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
        if (!selectedPage.isBankPage()) {
            String actionDetail = actionType == TerminalActionType.REFRESH_PAGE ? "已通过最小 snapshot 链刷新当前分区"
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
        TerminalActionType actionType, BankActionContext bankContext, MarketActionContext marketContext) {
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
                        ? "selectedPageId 已切换到标准商品市场，主体区现在直接承接真实标准市场 section。"
                        : "selectedPageId 已切换到市场总入口，主体区继续承接共享摘要和入口卡。",
                    TerminalNotificationSeverity.INFO.name()));
            }
            notifications.add(new TerminalOpenApproval.NotificationEntry(
                "市场页已进入 phase 7 新壳",
                "MARKET 总入口、标准商品、定制商品与汇率市场现在都走 TerminalActionMessage -> TerminalSnapshotMessage 主链。",
                TerminalNotificationSeverity.INFO.name()));
            notifications.add(new TerminalOpenApproval.NotificationEntry(
                "后续阶段边界仍有效",
                "phase 8 只做正式 cutover，phase 9 再删除旧 ModularUI terminal 实现。",
                TerminalNotificationSeverity.WARNING.name()));
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
                    "selectedPageId 已切换到银行页，主体区现在由新 TerminalBankSection 承接真实业务内容。",
                    TerminalNotificationSeverity.INFO.name()));
            }
            notifications.add(new TerminalOpenApproval.NotificationEntry(
                "银行页已迁入新壳",
                "开户状态、余额摘要、转账表单与确认后回写现在都走 TerminalActionMessage -> TerminalSnapshotMessage 主链。",
                TerminalNotificationSeverity.INFO.name()));
            notifications.add(new TerminalOpenApproval.NotificationEntry(
                "phase 7 市场页已迁入",
                "BANK 与三类 MARKET 正式业务页都已由新壳承接，本轮不会把旧页重新嵌回新壳。",
                TerminalNotificationSeverity.WARNING.name()));
            return notifications;
        }

        if (actionType == TerminalActionType.SELECT_PAGE) {
            notifications.add(new TerminalOpenApproval.NotificationEntry(
                "已切换分区",
                "selectedPageId 已切换到 " + selectedPage.getLabel() + "，主体区现在由 section 宿主负责承接该页面。",
                TerminalNotificationSeverity.INFO.name()));
        } else if (actionType == TerminalActionType.REFRESH_PAGE) {
            notifications.add(new TerminalOpenApproval.NotificationEntry(
                "分区快照已刷新",
                selectedPage.getLabel() + " 当前已通过 TerminalActionMessage -> TerminalSnapshotMessage 完成一次最小刷新。",
                TerminalNotificationSeverity.INFO.name()));
        }
        notifications.add(new TerminalOpenApproval.NotificationEntry(
            "市场与银行页均已迁入新壳",
            "当前首页壳已经承接 BANK、MARKET_STANDARDIZED、MARKET_CUSTOM、MARKET_EXCHANGE，后续阶段进入 cutover。",
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

        TerminalActionFeedback claimAsset(EntityPlayer player, TerminalMarketActionPayload payload);

        TerminalCustomMarketSectionSnapshot createCustomSnapshot(EntityPlayer player, TerminalCustomMarketActionPayload payload,
            TerminalActionFeedback actionFeedback);

        TerminalActionFeedback purchaseCustomListing(EntityPlayer player, TerminalCustomMarketActionPayload payload);

        TerminalActionFeedback cancelCustomListing(EntityPlayer player, TerminalCustomMarketActionPayload payload);

        TerminalActionFeedback claimCustomListing(EntityPlayer player, TerminalCustomMarketActionPayload payload);

        TerminalExchangeMarketSectionSnapshot createExchangeSnapshot(EntityPlayer player,
            TerminalExchangeMarketActionPayload payload, TerminalActionFeedback actionFeedback);

        TerminalActionFeedback refreshExchangeQuote(EntityPlayer player);

        TerminalActionFeedback submitExchangeHeld(EntityPlayer player);
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
        public TerminalActionFeedback submitExchangeHeld(EntityPlayer player) {
            return TerminalMarketSectionService.INSTANCE.submitExchangeHeld(player);
        }
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
}
