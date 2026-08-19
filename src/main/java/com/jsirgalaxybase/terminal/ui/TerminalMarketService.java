package com.jsirgalaxybase.terminal.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.module.ModuleManager;
import com.jsirgalaxybase.terminal.TerminalMarketActionPayload;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.banking.application.PlayerBankAccountProvisioner;
import com.jsirgalaxybase.modules.core.market.application.CustomMarketService;
import com.jsirgalaxybase.modules.core.market.application.TaskCoinExchangeService;
import com.jsirgalaxybase.modules.core.market.application.TaskCoinExchangePlanner;
import com.jsirgalaxybase.modules.core.market.application.TaskCoinCatalog;
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketAdmissionDecision;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogEntry;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogPage;
import com.jsirgalaxybase.modules.core.market.application.StandardizedSpotMarketService;
import com.jsirgalaxybase.modules.core.market.application.read.StandardizedMarketReadRepository;
import com.jsirgalaxybase.modules.core.market.application.command.CancelCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CancelBuyOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CancelSellOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.ClaimCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.application.command.ClaimMarketAssetCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CreateBuyOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CreateSellOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.DepositMarketInventoryCommand;
import com.jsirgalaxybase.modules.core.market.application.command.PurchaseCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.application.command.PublishCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketDeliveryStatus;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketListingStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyInventory;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrder;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderHistoryPage;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderHistoryQuery;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderSide;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord;
import com.jsirgalaxybase.modules.core.market.domain.MarketTradeHistoryPage;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationHistoryPage;
import com.jsirgalaxybase.modules.core.market.domain.MarketOperationLog;
import com.jsirgalaxybase.modules.core.market.domain.MarketAccountCenterQuery;
import com.jsirgalaxybase.modules.core.market.domain.MarketAccountCenterSnapshot;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketExecutionResult;
import com.jsirgalaxybase.modules.core.market.infrastructure.MarketInfrastructure;
import com.jsirgalaxybase.modules.core.vault.application.BaseVaultService;
import com.jsirgalaxybase.modules.core.vault.domain.VaultSlot;
import com.jsirgalaxybase.modules.core.vault.domain.VaultOperation;
import com.jsirgalaxybase.modules.core.vault.domain.VaultOperationHistoryPage;
import com.jsirgalaxybase.modules.core.vault.domain.VaultOperationStatus;
import com.jsirgalaxybase.modules.core.vault.application.VaultItemStackCodec;
import com.jsirgalaxybase.modules.core.vault.infrastructure.VaultCustomMarketDeliveryPort;
import com.jsirgalaxybase.modules.core.market.port.MarketCustodyInventoryRepository;
import com.jsirgalaxybase.modules.core.market.port.AccountInventoryResolver;
import com.jsirgalaxybase.modules.core.market.port.MarketOrderBookRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketTradeRecordRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOperationLogRepository;

import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;

final class TerminalMarketService {

    static final TerminalMarketService INSTANCE = new TerminalMarketService();

    private static final int PRODUCT_LIMIT = 12;
    private static final int BOOK_DEPTH = 6;
    private static final int ORDER_LIMIT = 100;
    private static final int CLAIM_LIMIT = 4;
    private static final int CUSTOM_LISTING_LIMIT = 50;
    private static final int CUSTOM_BROWSER_PAGE_SIZE = 12;
    private static final int CUSTOM_SCOPE_ACTIVE = 0;
    private static final int CUSTOM_SCOPE_SELLING = 1;
    private static final int CUSTOM_SCOPE_PENDING = 2;
    private static final String EXCHANGE_TARGET_TASK_COIN = "task-coin-formal";
    private static final int TAKER_FEE_BASIS_POINTS = 80;
    private static final int MAKER_FEE_BASIS_POINTS = 20;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm")
        .withLocale(Locale.ROOT)
        .withZone(ZoneId.systemDefault());

    private TerminalMarketService() {}

    TerminalCustomMarketSnapshot createCustomSnapshot(EntityPlayer player, int selectedScope, String selectedListingId,
        String query, int pageIndex) {
        CustomContext context = resolveCustomContext();
        if (!context.isReady() || context.vaultService == null) {
            return createUnavailableCustomSnapshot(context.unavailableMessage, selectedScope);
        }

        String playerRef = resolvePlayerRef(player);
        com.jsirgalaxybase.modules.core.market.application.CustomMarketBrowseResult browsePage =
            context.customMarketService.browsePage(describeCustomScopeKey(selectedScope), playerRef, query,
                Math.max(0, pageIndex) * CUSTOM_BROWSER_PAGE_SIZE, CUSTOM_BROWSER_PAGE_SIZE);
        List<CustomMarketService.ListingView> currentViews = browsePage.getListingViews();
        List<CustomMarketService.ListingView> activeViews = selectedScope == CUSTOM_SCOPE_ACTIVE ? currentViews
            : Collections.<CustomMarketService.ListingView>emptyList();
        List<CustomMarketService.ListingView> sellingViews = selectedScope == CUSTOM_SCOPE_SELLING ? currentViews
            : Collections.<CustomMarketService.ListingView>emptyList();
        List<CustomMarketService.ListingView> pendingViews = selectedScope == CUSTOM_SCOPE_PENDING ? currentViews
            : Collections.<CustomMarketService.ListingView>emptyList();
        CustomMarketService.ListingView selectedView = findCustomSelectedView(
            selectedListingId,
            activeViews,
            sellingViews,
            pendingViews);
        // A selected row can be outside the currently fetched browse window.
        // Resolve it by id so the detail route never depends on a UI page cache.
        if (selectedView == null && selectedListingId != null && !selectedListingId.trim().isEmpty()) {
            try {
                selectedView = context.customMarketService.inspectListing(Long.parseLong(selectedListingId.trim()));
            } catch (RuntimeException ignored) {
                selectedView = null;
            }
        }
        return new TerminalCustomMarketSnapshot(
            "定制商品市场在线",
            buildCustomBrowserHint(selectedScope, activeViews, sellingViews, pendingViews),
            describeCustomScope(selectedScope),
            buildCustomListingLines(activeViews),
            buildCustomListingIds(activeViews),
            buildCustomListingIconRefs(activeViews),
            buildCustomListingLines(sellingViews),
            buildCustomListingIds(sellingViews),
            buildCustomListingIconRefs(sellingViews),
            buildCustomListingLines(pendingViews),
            buildCustomListingIds(pendingViews),
            buildCustomListingIconRefs(pendingViews),
            selectedView == null ? "" : String.valueOf(selectedView.getListing().getListingId()),
            selectedView == null ? "未选中挂牌" : selectedView.getSnapshot().getDisplayName(),
            selectedView == null ? "--" : formatAmount(selectedView.getListing().getAskingPrice()) + " "
                + selectedView.getListing().getCurrencyCode(),
            selectedView == null ? "未选中详情" : selectedView.getListing().getListingStatus() + " / "
                + selectedView.getListing().getDeliveryStatus(),
            selectedView == null ? "请先在左侧选择一条挂牌" : buildCustomCounterparty(playerRef, selectedView),
            selectedView == null ? "--" : selectedView.getSnapshot().getItemId() + " @" + selectedView.getSnapshot().getMeta(),
            selectedView == null ? "选择后显示成交与交付状态" : buildCustomTradeSummary(selectedView),
            buildCustomActionHint(playerRef, selectedView),
            canBuyCustomListing(playerRef, selectedView) ? "1" : "0",
            canCancelCustomListing(playerRef, selectedView) ? "1" : "0",
            canClaimCustomListing(playerRef, selectedView) ? "1" : "0",
            activeViews, sellingViews, pendingViews, currentViews, browsePage.getTotalEntries());
    }

    TerminalExchangeMarketSnapshot createExchangeSnapshot(EntityPlayer player, String selectedTargetCode,
        String selectedCoinCode, int selectedVaultSlot) {
        ExchangeContext context = resolveExchangeContext();
        TerminalExchangeQuoteView quoteView = buildExchangeQuoteView(resolvePlayerRef(player),
            resolveVaultStack(player, context, selectedVaultSlot), context);
        TaskCoinCatalog.Entry selectedCoin = TaskCoinCatalog.defaultCatalog().find(selectedCoinCode).orElse(null);
        String targetTitle = selectedCoin == null ? "任务书硬币正式兑换" : selectedCoin.getDisplayName();
        String targetSummary = selectedCoin == null
            ? "从个人 Base Vault 选择目录内任务书硬币即可刷新正式报价。"
            : selectedCoin.getFamilyDisplayName() + " / " + selectedCoin.getTier() + " / 面值 "
                + formatAmount(selectedCoin.getFaceValue()) + "，实际执行仍以所选 Vault 格位为准。";
        return new TerminalExchangeMarketSnapshot(
            quoteView.serviceState,
            "从 Base Vault 选择目录内任务书硬币即可刷新正式报价。",
            new String[] { EXCHANGE_TARGET_TASK_COIN },
            new String[] { "任务书硬币 -> STARCOIN | 75 种正式币" },
            EXCHANGE_TARGET_TASK_COIN,
            selectedCoin == null ? "" : selectedCoin.getRegistryName(),
            targetTitle,
            targetSummary,
            quoteView.heldSummary,
            quoteView.inputRegistryName,
            quoteView.pairCode,
            quoteView.inputAssetCode,
            quoteView.outputAssetCode,
            quoteView.ruleVersion,
            quoteView.limitStatus,
            quoteView.reasonCode,
            quoteView.notes,
            quoteView.inputQuantity,
            quoteView.nominalFaceValue,
            quoteView.effectiveExchangeValue,
            quoteView.contributionValue,
            quoteView.discountStatus,
            quoteView.exchangeRateDisplay,
            quoteView.executionHint,
            quoteView.executableFlag,
            TaskCoinCatalog.defaultCatalog().getEntries());
    }

    TerminalMarketSnapshot createSnapshot(EntityPlayer player, TerminalMarketSnapshotRequest controller) {
        ExchangeContext exchangeContext = resolveExchangeContext();
        TerminalExchangeQuoteView exchangeQuoteView = buildExchangeQuoteView((ItemStack) null, exchangeContext);
        MarketContext context = resolveContext();
        // Formal catalog and Base Vault are now the only market item sources.
        // Never infer a market product from the player's held stack here.
        if (!context.isReady()) {
            return createUnavailableSnapshot(context.unavailableMessage, exchangeQuoteView);
        }

        String playerRef = resolvePlayerRef(player);
        StandardizedMarketCatalogPage catalogPage = browseCatalogPage(context, controller, playerRef);
        List<String> productKeys = productKeys(catalogPage.getEntries());
        String selectedProductKey = normalizeSelectedProductKey(controller.getSelectedProductKey(), productKeys);
        if (selectedProductKey == null) {
            return attachCatalogBrowserData(
                createEmptySnapshot(productKeys, context, exchangeQuoteView),
                catalogPage,
                buildCatalogMarketSummaries(
                    context,
                    catalogPage,
                    playerRef,
                    null,
                    Collections.<MarketTradeRecord>emptyList(),
                    Collections.<MarketTradeRecord>emptyList(),
                    Collections.<MarketOrder>emptyList(),
                    Collections.<MarketOrder>emptyList(),
                    0L,
                    0L,
                    0L,
                    controller.getChartRange()));
        }

        StandardizedMarketProduct product = requireTradableProduct(context.spotMarketService, selectedProductKey);
        List<MarketOrder> asks = context.orderRepository.findOpenSellOrdersByProductKey(selectedProductKey);
        List<MarketOrder> bids = context.orderRepository.findOpenBuyOrdersByProductKey(selectedProductKey);
        List<MarketTradeRecord> recentTrades = context.tradeRecordRepository.findByProductKey(selectedProductKey, BOOK_DEPTH);
        List<MarketTradeRecord> dayTrades = context.tradeRecordRepository.findByProductKeySince(
            selectedProductKey,
            Instant.now().minusSeconds(24L * 60L * 60L),
            64);
        // Product detail is deliberately not the cross-product order center.  It may only
        // offer cancellation for this product's still-actionable orders.
        List<MarketOrder> myOrders = context.orderRepository.findOrdersByOwnerAndProductKey(
            playerRef, selectedProductKey, ORDER_LIMIT);
        List<MarketCustodyInventory> claimables = context.custodyRepository.findByOwnerProductKeyAndStatuses(
            playerRef,
            selectedProductKey,
            Arrays.asList(MarketCustodyStatus.CLAIMABLE));
        List<MarketCustodyInventory> available = context.custodyRepository.findByOwnerProductKeyAndStatuses(
            playerRef,
            selectedProductKey,
            Arrays.asList(MarketCustodyStatus.AVAILABLE));
        List<MarketCustodyInventory> escrow = context.custodyRepository.findByOwnerProductKeyAndStatuses(
            playerRef,
            selectedProductKey,
            Arrays.asList(MarketCustodyStatus.ESCROW_SELL));

        long availableQuantity = sumCustodyQuantity(available);

        DepthQuote instantBuyQuote = quoteDepth(asks, controller.parseInstantBuyQuantity(), true);
        DepthQuote instantSellQuote = quoteDepth(bids, controller.parseInstantSellQuantity(), false);

        TerminalMarketSnapshot result = new TerminalMarketSnapshot(
            "市场服务在线 / 共享 JDBC 运行时已接线",
            buildCatalogBrowserHint(catalogPage),
            toSizedArray(productKeys, PRODUCT_LIMIT),
            buildProductLabels(catalogPage.getEntries()),
            selectedProductKey,
            resolveProductDisplayName(context.spotMarketService, product),
            resolveUnitLabel(product),
            formatLatestTradePrice(recentTrades),
            bids.isEmpty() ? "无买盘" : formatAmount(bids.get(0).getUnitPrice()) + " STARCOIN",
            asks.isEmpty() ? "无卖盘" : formatAmount(asks.get(0).getUnitPrice()) + " STARCOIN",
            bids.isEmpty() ? "0" : formatAmount(bids.get(0).getOpenQuantity()),
            asks.isEmpty() ? "0" : formatAmount(asks.get(0).getOpenQuantity()),
            formatAmount(sumTradeQuantity(dayTrades)),
            formatAmount(sumTradeTurnover(dayTrades)) + " STARCOIN",
            "最新成交价仅用于行情展示，不代表当前仍可按该价格成交。",
            buildBookLines(asks, true),
            buildBookPrices(asks),
            buildBookLines(bids, false),
            buildBookPrices(bids),
            buildLimitBuyPreview(controller, selectedProductKey),
            buildLimitSellPreview(controller, selectedProductKey, availableQuantity),
            buildInstantPreview(instantBuyQuote, true),
            buildInstantPreview(instantSellQuote, false),
            buildSourceMode(selectedProductKey, context.spotMarketService),
            buildSourceAvailable(availableQuantity),
            formatAmount(sumCustodyQuantity(escrow)),
            formatAmount(sumCustodyQuantity(claimables)),
            formatAmount(sumFrozenFunds(myOrders)) + " STARCOIN",
            buildWarehouseNotice(selectedProductKey, availableQuantity),
            buildMyOrderLines(myOrders, true, context.spotMarketService),
            buildMyOrderIds(myOrders),
            buildMyOrderCancelableFlags(myOrders),
            buildClaimLines(claimables),
            buildClaimIds(claimables),
            new String[] {
                "最新成交价不是当前可成交价。",
                "即时买入吃卖盘，即时卖出吃买盘。",
                "卖出时由服务端从个人 Base Vault 自动预留；无需手动存入市场托管。",
                "撤单只撤未成交部分。"
            },
            exchangeQuoteView.serviceState,
            exchangeQuoteView.heldSummary,
            exchangeQuoteView.inputRegistryName,
            exchangeQuoteView.pairCode,
            exchangeQuoteView.inputAssetCode,
            exchangeQuoteView.outputAssetCode,
            exchangeQuoteView.ruleVersion,
            exchangeQuoteView.limitStatus,
            exchangeQuoteView.reasonCode,
            exchangeQuoteView.notes,
            exchangeQuoteView.inputQuantity,
            exchangeQuoteView.nominalFaceValue,
            exchangeQuoteView.effectiveExchangeValue,
            exchangeQuoteView.contributionValue,
            exchangeQuoteView.discountStatus,
            exchangeQuoteView.exchangeRateDisplay,
            exchangeQuoteView.executionHint,
            exchangeQuoteView.executableFlag);
        return attachCatalogBrowserData(result, catalogPage, buildCatalogMarketSummaries(
            context, catalogPage, playerRef, selectedProductKey, recentTrades, dayTrades, bids, asks,
            availableQuantity, sumCustodyQuantity(escrow), sumCustodyQuantity(claimables),
            controller.getChartRange()));
    }

    OrderHistorySnapshot createOrderHistorySnapshot(EntityPlayer player, TerminalMarketActionPayload payload) {
        MarketContext context = resolveContext();
        TerminalMarketActionPayload request = payload == null ? TerminalMarketActionPayload.empty() : payload;
        // The history workbench has four fixed-height rows. Keep the server page
        // contract aligned with that viewport, including requests from older clients.
        int pageSize = TerminalMarketActionPayload.DEFAULT_HISTORY_PAGE_SIZE;
        if (!context.isReady()) {
            return OrderHistorySnapshot.empty(request.getHistoryPage(), pageSize);
        }
        String productKey = "CURRENT".equals(request.getHistoryProductScope())
            ? request.getSelectedProductKey() : "";
        MarketOrderSide side = parseHistorySide(request.getHistorySide());
        MarketOrderHistoryQuery.StatusGroup status = parseHistoryStatus(request.getHistoryStatus());
        MarketOrderHistoryQuery query = new MarketOrderHistoryQuery(
            productKey,
            side,
            status,
            historyCutoff(request.getHistoryTime()),
            request.getHistoryQuery(),
            request.getHistoryPage(),
            pageSize);
        MarketOrderHistoryPage page = context.orderRepository.findOrderHistory(resolvePlayerRef(player), query);
        List<MarketOrder> orders = page == null ? Collections.<MarketOrder>emptyList() : page.getOrders();
        return new OrderHistorySnapshot(
            buildMyOrderLines(orders, false, context.spotMarketService),
            buildMyOrderIds(orders),
            buildMyOrderCancelableFlags(orders),
            page == null ? 0 : page.getTotalEntries(),
            page == null ? request.getHistoryPage() : page.getPageIndex(),
            page == null ? pageSize : page.getPageSize());
    }

    MarketAccountCenterSnapshot createAccountCenterSnapshot(EntityPlayer player, TerminalMarketActionPayload payload) {
        MarketContext context = resolveContext();
        TerminalMarketActionPayload request = payload == null ? TerminalMarketActionPayload.empty() : payload;
        MarketAccountCenterQuery.Tab tab = parseCenterTab(request.getCenterTab());
        MarketAccountCenterQuery query = new MarketAccountCenterQuery(tab, request.getHistoryQuery(),
            "CURRENT".equals(request.getHistoryProductScope()) ? request.getSelectedProductKey() : "",
            parseHistorySide(request.getHistorySide()), parseCenterStatus(request.getHistoryStatus()),
            historyCutoff(request.getHistoryTime()), request.getHistoryPage(), request.getHistoryPageSize(),
            request.getFocusedRecordId());
        if (!context.isReady()) {
            return new MarketAccountCenterSnapshot(tab, null,
                new MarketAccountCenterSnapshot.PageMetadata(0, query.getPageIndex(), query.getPageSize()),
                null, null, null, null);
        }
        String playerRef = resolvePlayerRef(player);
        MarketAccountCenterSnapshot.AccountSummary summary = buildCenterSummary(player, context, playerRef);
        if (tab == MarketAccountCenterQuery.Tab.FILLS) {
            MarketTradeHistoryPage page = context.tradeRecordRepository.findPersonalTradeHistory(playerRef, query);
            List<MarketAccountCenterSnapshot.FillRow> rows = new ArrayList<MarketAccountCenterSnapshot.FillRow>();
            for (MarketTradeRecord trade : page.getTrades()) {
                boolean buy = playerRef.equals(trade.getBuyerPlayerRef());
                rows.add(new MarketAccountCenterSnapshot.FillRow(trade, buy ? MarketOrderSide.BUY : MarketOrderSide.SELL,
                    buy ? trade.getBuyOrderId() : trade.getSellOrderId()));
            }
            return new MarketAccountCenterSnapshot(tab, summary, new MarketAccountCenterSnapshot.PageMetadata(
                page.getTotalEntries(), page.getPageIndex(), page.getPageSize()), null, rows, null, null);
        }
        if (tab == MarketAccountCenterQuery.Tab.ASSETS_AND_DELIVERY) {
            return buildDeliveryCenterSnapshot(context, playerRef, query, summary);
        }
        MarketOrderHistoryQuery.StatusGroup orderStatus = tab == MarketAccountCenterQuery.Tab.OPEN_ORDERS
            ? MarketOrderHistoryQuery.StatusGroup.OPEN : parseCenterHistoryOrderStatus(request.getHistoryStatus());
        MarketOrderHistoryPage page = context.orderRepository.findOrderHistory(playerRef,
            new MarketOrderHistoryQuery(query.getProductKey(), query.getSide(), orderStatus, query.getCreatedAfter(),
                query.getSearchText(), query.getPageIndex(), query.getPageSize()));
        List<MarketAccountCenterSnapshot.OrderRow> openRows = new ArrayList<MarketAccountCenterSnapshot.OrderRow>();
        List<MarketAccountCenterSnapshot.HistoryRow> historyRows = new ArrayList<MarketAccountCenterSnapshot.HistoryRow>();
        for (MarketOrder order : page.getOrders()) {
            if (tab == MarketAccountCenterQuery.Tab.OPEN_ORDERS) openRows.add(new MarketAccountCenterSnapshot.OrderRow(order));
            else historyRows.add(new MarketAccountCenterSnapshot.HistoryRow(order));
        }
        return new MarketAccountCenterSnapshot(tab, summary, new MarketAccountCenterSnapshot.PageMetadata(
            page.getTotalEntries(), page.getPageIndex(), page.getPageSize()), openRows, null, null, historyRows);
    }

    private MarketAccountCenterSnapshot buildDeliveryCenterSnapshot(MarketContext context, String playerRef,
        MarketAccountCenterQuery query, MarketAccountCenterSnapshot.AccountSummary summary) {
        List<MarketAccountCenterSnapshot.DeliveryRow> all = new ArrayList<MarketAccountCenterSnapshot.DeliveryRow>();
        List<MarketCustodyInventory> claims = context.custodyRepository.findByOwnerAndStatus(playerRef,
            MarketCustodyStatus.CLAIMABLE);
        int matchingClaims = 0;
        boolean includePending = query.getStatusGroup() == MarketAccountCenterQuery.StatusGroup.ALL
            || query.getStatusGroup() == MarketAccountCenterQuery.StatusGroup.ACTIVE;
        for (MarketCustodyInventory claim : claims) {
            if (!includePending) continue;
            if (!query.getProductKey().isEmpty() && !query.getProductKey().equals(claim.getProduct().getProductKey())) continue;
            if (!query.getSearchText().isEmpty()
                && !claim.getProduct().getProductKey().toLowerCase(Locale.ROOT)
                    .contains(query.getSearchText().toLowerCase(Locale.ROOT))
                && !String.valueOf(claim.getCustodyId()).contains(query.getSearchText())) continue;
            matchingClaims++;
            all.add(new MarketAccountCenterSnapshot.DeliveryRow("C" + claim.getCustodyId(), "PENDING_DELIVERY",
                "CLAIMABLE", "待收货；Base Vault 满仓时保持此状态", claim.getProduct(), claim.getQuantity(),
                claim.getRelatedOrderId(), claim.getUpdatedAt()));
        }
        int needed = Math.min((query.getPageIndex() + 1) * query.getPageSize(),
            (MarketAccountCenterQuery.MAX_PAGE_INDEX + 1) * MarketAccountCenterQuery.MAX_PAGE_SIZE);
        MarketOperationHistoryPage operations = collectMarketExceptions(context, playerRef, query, needed);
        for (MarketOperationLog operation : operations.getOperations()) {
            all.add(new MarketAccountCenterSnapshot.DeliveryRow("O" + operation.getOperationId(),
                operation.getOperationType().name(), operation.getStatus().name(), safeOperationMessage(operation),
                null, 0L, operation.getRelatedOrderId(), operation.getUpdatedAt()));
        }
        VaultOperationStatus vaultStatus = query.getStatusGroup() == MarketAccountCenterQuery.StatusGroup.RECOVERY_REQUIRED
            ? VaultOperationStatus.RECOVERY_REQUIRED : null;
        VaultOperationHistoryPage vaultOperations = collectVaultExceptions(context, playerRef, query, vaultStatus, needed);
        for (VaultOperation operation : vaultOperations.getOperations()) {
            all.add(new MarketAccountCenterSnapshot.DeliveryRow("V" + operation.getOperationId(),
                "BASE_VAULT_" + operation.getOperationType(), operation.getStatus().name(),
                operation.getMessage(), productFromVaultOperation(operation), operation.getQuantity(), 0L,
                operation.getUpdatedAt()));
        }
        boolean vaultFull = summary.getVaultTotalSlots() > 0
            && summary.getVaultUsedSlots() >= summary.getVaultTotalSlots() && !claims.isEmpty();
        boolean fullMatchesSearch = query.getSearchText().isEmpty()
            || "base vault 满仓 vault_full".contains(query.getSearchText().toLowerCase(Locale.ROOT));
        if (vaultFull && includePending && fullMatchesSearch) {
            all.add(new MarketAccountCenterSnapshot.DeliveryRow("VAULT_FULL", "VAULT_FULL", "EXCEPTION",
                "Base Vault 已满，待收货资产将保留在受审计托管中", null, 0L, 0L, Instant.now()));
        }
        Collections.sort(all, (left, right) -> {
            Instant leftTime = left.getUpdatedAt() == null ? Instant.EPOCH : left.getUpdatedAt();
            Instant rightTime = right.getUpdatedAt() == null ? Instant.EPOCH : right.getUpdatedAt();
            return rightTime.compareTo(leftTime);
        });
        int total = matchingClaims + operations.getTotalEntries() + vaultOperations.getTotalEntries()
            + (vaultFull && includePending && fullMatchesSearch ? 1 : 0);
        int page = total <= 0 ? 0 : Math.min(query.getPageIndex(), (total - 1) / query.getPageSize());
        int start = Math.min(all.size(), page * query.getPageSize());
        int end = Math.min(all.size(), start + query.getPageSize());
        return new MarketAccountCenterSnapshot(query.getTab(), summary,
            new MarketAccountCenterSnapshot.PageMetadata(total, page, query.getPageSize()), null, null,
            new ArrayList<MarketAccountCenterSnapshot.DeliveryRow>(all.subList(start, end)), null);
    }

    private MarketOperationHistoryPage collectMarketExceptions(MarketContext context, String playerRef,
        MarketAccountCenterQuery query, int needed) {
        List<MarketOperationLog> collected = new ArrayList<MarketOperationLog>();
        int total = 0;
        for (int pageIndex = 0; collected.size() < needed; pageIndex++) {
            MarketOperationHistoryPage page = context.operationLogRepository.findPersonalExceptions(playerRef,
                new MarketAccountCenterQuery(query.getTab(), query.getSearchText(), "", null, query.getStatusGroup(),
                    query.getCreatedAfter(), pageIndex, MarketAccountCenterQuery.MAX_PAGE_SIZE,
                    query.getFocusedRecordId()));
            total = page.getTotalEntries();
            collected.addAll(page.getOperations());
            if (collected.size() >= total || page.getOperations().isEmpty()) break;
        }
        return new MarketOperationHistoryPage(collected, total, 0, Math.max(1, needed));
    }

    private VaultOperationHistoryPage collectVaultExceptions(MarketContext context, String playerRef,
        MarketAccountCenterQuery query, VaultOperationStatus status, int needed) {
        List<VaultOperation> collected = new ArrayList<VaultOperation>();
        int total = 0;
        for (int pageIndex = 0; collected.size() < needed; pageIndex++) {
            VaultOperationHistoryPage page = context.vaultService.findPersonalExceptionalOperations(playerRef,
                query.getSearchText(), status, query.getCreatedAfter(), pageIndex,
                MarketAccountCenterQuery.MAX_PAGE_SIZE);
            total = page.getTotalEntries();
            collected.addAll(page.getOperations());
            if (collected.size() >= total || page.getOperations().isEmpty()) break;
        }
        return new VaultOperationHistoryPage(collected, total, 0, Math.max(1, needed));
    }

    private StandardizedMarketProduct productFromVaultOperation(VaultOperation operation) {
        if (operation == null || operation.getItemSnapshot() == null || operation.getItemSnapshot().trim().isEmpty()) {
            return null;
        }
        try {
            ItemStack stack = VaultItemStackCodec.decode(operation.getItemSnapshot());
            Object registry = stack == null ? null : Item.itemRegistry.getNameForObject(stack.getItem());
            return registry == null ? null : new StandardizedMarketProduct(String.valueOf(registry), stack.getItemDamage());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private MarketAccountCenterSnapshot.AccountSummary buildCenterSummary(EntityPlayer player, MarketContext context,
        String playerRef) {
        int used = 0;
        int totalSlots = 0;
        try {
            BaseVaultService.VaultView vault = context.vaultService.viewPersonalVault(playerRef);
            totalSlots = vault.getSlots().size();
            for (VaultSlot slot : vault.getSlots()) if (slot != null && slot.getStack() != null) used++;
        } catch (RuntimeException ignored) { }
        int pending = context.custodyRepository.findByOwnerAndStatus(playerRef, MarketCustodyStatus.CLAIMABLE).size();
        int recovery = context.operationLogRepository.findPersonalExceptions(playerRef,
            new MarketAccountCenterQuery(MarketAccountCenterQuery.Tab.ASSETS_AND_DELIVERY, "", "", null,
                MarketAccountCenterQuery.StatusGroup.ALL, null, 0, 1, "")).getTotalEntries();
        recovery += context.vaultService.findPersonalExceptionalOperations(playerRef, "", null, null, 0, 1)
            .getTotalEntries();
        TerminalBankSnapshot bank = TerminalBankSnapshotProvider.INSTANCE.create(player);
        long frozenFunds = bank.getPlayerStatus() != null && bank.getPlayerStatus().contains("冻结")
            ? parsePositiveAmount(bank.getPlayerStatus()) : context.orderRepository.sumReservedFundsByOwner(playerRef);
        return new MarketAccountCenterSnapshot.AccountSummary(parsePositiveAmount(bank.getPlayerBalance()),
            frozenFunds, used, totalSlots,
            context.orderRepository.countActiveOrdersByOwner(playerRef), pending, recovery);
    }

    private long parsePositiveAmount(String value) {
        if (value == null || value.trim().isEmpty()) return 0L;
        StringBuilder digits = new StringBuilder();
        boolean started = false;
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch >= '0' && ch <= '9') {
                digits.append(ch);
                started = true;
            } else if (ch == ',' && started) {
                continue;
            } else if (started) {
                break;
            }
        }
        if (digits.length() == 0) return 0L;
        try { return Math.max(0L, Long.parseLong(digits.toString())); }
        catch (NumberFormatException ignored) { return 0L; }
    }

    private MarketAccountCenterQuery.Tab parseCenterTab(String value) {
        try { return MarketAccountCenterQuery.Tab.valueOf(value == null ? "OPEN_ORDERS" : value); }
        catch (IllegalArgumentException ignored) { return MarketAccountCenterQuery.Tab.OPEN_ORDERS; }
    }

    private MarketAccountCenterQuery.StatusGroup parseCenterStatus(String value) {
        if ("OPEN".equals(value)) return MarketAccountCenterQuery.StatusGroup.ACTIVE;
        if ("FILLED".equals(value)) return MarketAccountCenterQuery.StatusGroup.FILLED;
        if ("CLOSED".equals(value)) return MarketAccountCenterQuery.StatusGroup.CLOSED;
        if ("RECOVERY_REQUIRED".equals(value)) return MarketAccountCenterQuery.StatusGroup.RECOVERY_REQUIRED;
        return MarketAccountCenterQuery.StatusGroup.ALL;
    }

    private MarketOrderHistoryQuery.StatusGroup parseCenterHistoryOrderStatus(String value) {
        if ("FILLED".equals(value)) return MarketOrderHistoryQuery.StatusGroup.FILLED;
        if ("CLOSED".equals(value)) return MarketOrderHistoryQuery.StatusGroup.CLOSED;
        return MarketOrderHistoryQuery.StatusGroup.HISTORICAL;
    }

    private String safeOperationMessage(MarketOperationLog operation) {
        String message = operation == null ? "" : operation.getMessage();
        return message == null || message.trim().isEmpty() ? "需要人工检查的市场资产操作" : message.trim();
    }

    private MarketOrderSide parseHistorySide(String value) {
        if ("BUY".equals(value)) return MarketOrderSide.BUY;
        if ("SELL".equals(value)) return MarketOrderSide.SELL;
        return null;
    }

    private MarketOrderHistoryQuery.StatusGroup parseHistoryStatus(String value) {
        try {
            return MarketOrderHistoryQuery.StatusGroup.valueOf(value == null ? "ALL" : value);
        } catch (IllegalArgumentException ignored) {
            return MarketOrderHistoryQuery.StatusGroup.ALL;
        }
    }

    private Instant historyCutoff(String value) {
        long days = "DAY".equals(value) ? 1L : "WEEK".equals(value) ? 7L : "MONTH".equals(value) ? 30L : 0L;
        return days == 0L ? null : Instant.now().minusSeconds(days * 24L * 60L * 60L);
    }

    TerminalActionFeedback refreshExchangeQuote(EntityPlayer player) {
        TerminalExchangeQuoteView quoteView = buildExchangeQuoteView((ItemStack) null, resolveExchangeContext());
        if (!quoteView.hasFormalQuote()) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "报价刷新失败",
                quoteView.notes,
                3600L);
        }
        TerminalNotificationSeverity severity = "1".equals(quoteView.executableFlag)
            ? TerminalNotificationSeverity.SUCCESS
            : TerminalNotificationSeverity.WARNING;
        String title = "1".equals(quoteView.executableFlag) ? "正式报价已刷新" : "正式报价禁兑";
        return TerminalActionFeedback.of(
            severity,
            title,
            "pair=" + quoteView.pairCode + "，limitStatus=" + quoteView.limitStatus + "，reasonCode="
                + quoteView.reasonCode + "，实际兑换值=" + quoteView.effectiveExchangeValue + "，贡献值="
                + quoteView.contributionValue + "。 " + quoteView.notes,
            3600L);
    }

    TerminalActionFeedback submitExchange(EntityPlayer player, int vaultSlotIndex) {
        EntityPlayerMP serverPlayer = requireServerPlayer(player);
        if (serverPlayer == null) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "汇率兑换失败",
                "当前客户端上下文不能直接执行汇率兑换。",
                4200L);
        }

        ExchangeContext context = resolveExchangeContext();
        if (!context.isReady() || context.vaultService == null) {
            return exchangeUnavailableFeedback(context.unavailableMessage);
        }

        if (vaultSlotIndex < 0) {
            return TerminalActionFeedback.of(TerminalNotificationSeverity.ERROR, "汇率兑换失败",
                "请先在 Base Vault 中选择一格任务书硬币，再刷新正式报价。", 4200L);
        }
        try {
            ensurePersonalMarketAccount(serverPlayer);
            final String requestId = newRequestId("terminal-vault-exchange");
            ExchangeMarketExecutionResult result = context.vaultService.inSharedTransaction(
                new java.util.function.Supplier<ExchangeMarketExecutionResult>() {
                    @Override
                    public ExchangeMarketExecutionResult get() {
                        ItemStack input = context.vaultService.takeVaultItemForInternalTransfer(requestId,
                            serverPlayer.getUniqueID().toString(), vaultSlotIndex, 1, "EXCHANGE_SETTLEMENT");
                        return context.exchangeService.exchangeVaultCoinFormal(requestId,
                            serverPlayer.getUniqueID().toString(), input).getFormalResult();
                    }
                });
            return buildExchangeExecutionFeedback(result);
        } catch (RuntimeException exception) {
            return errorFeedback("汇率兑换失败", exception);
        }
    }

    TerminalActionFeedback purchaseCustomListing(EntityPlayer player, long listingId) {
        EntityPlayerMP serverPlayer = requireServerPlayer(player);
        if (serverPlayer == null) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "购买失败",
                "当前客户端上下文不能直接购买定制商品挂牌。",
                3600L);
        }

        CustomContext context = resolveCustomContext();
        if (!context.isReady()) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "定制商品市场不可用",
                context.unavailableMessage,
                3600L);
        }

        try {
            ensurePersonalMarketAccount(serverPlayer);
            CustomMarketService.PurchaseListingResult result = context.customMarketService.purchaseListing(
                new PurchaseCustomMarketListingCommand(
                    newRequestId("terminal-custom-market-buy"),
                    serverPlayer.getUniqueID().toString(),
                    context.sourceServerId,
                    listingId));
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "挂牌已买下",
                "listingId=" + result.getListing().getListingId() + "，已冻结并结算 "
                    + formatAmount(result.getListing().getAskingPrice()) + " " + result.getListing().getCurrencyCode()
                    + "，当前进入 BUYER_PENDING_CLAIM。",
                3600L);
        } catch (RuntimeException exception) {
            return errorFeedback("购买失败", exception);
        }
    }

    TerminalActionFeedback publishCustomListing(EntityPlayer player, long askingPrice) {
        return publishCustomListing(player, askingPrice, -1);
    }

    TerminalActionFeedback publishCustomListing(EntityPlayer player, long askingPrice, int vaultSlotIndex) {
        EntityPlayerMP serverPlayer = requireServerPlayer(player);
        if (serverPlayer == null) {
            return TerminalActionFeedback.of(TerminalNotificationSeverity.ERROR, "发布失败",
                "当前客户端上下文不能直接发布定制商品挂牌。", 3600L);
        }
        if (askingPrice <= 0L) {
            return TerminalActionFeedback.of(TerminalNotificationSeverity.ERROR, "发布失败",
                "挂牌价格必须为正数。", 3600L);
        }
        if (vaultSlotIndex < 0) {
            return TerminalActionFeedback.of(TerminalNotificationSeverity.ERROR, "发布失败",
                "请先在 Base Vault 中选择一个单件物品，再填写价格发布挂牌。", 3600L);
        }
        CustomContext context = resolveCustomContext();
        if (!context.isReady()) {
            return TerminalActionFeedback.of(TerminalNotificationSeverity.ERROR, "定制商品市场不可用",
                context.unavailableMessage, 3600L);
        }
        try {
            ensurePersonalMarketAccount(serverPlayer);
            final String requestId = newRequestId("terminal-custom-market-publish");
            CustomMarketService.PublishListingResult result = context.vaultService.inSharedTransaction(
                new java.util.function.Supplier<CustomMarketService.PublishListingResult>() {
                    @Override
                    public CustomMarketService.PublishListingResult get() {
                        ItemStack escrow = context.vaultService.takeVaultItemForInternalTransfer(requestId,
                            serverPlayer.getUniqueID().toString(), vaultSlotIndex, 1, "CUSTOM_MARKET_ESCROW");
                        return context.customMarketService.publishListing(new PublishCustomMarketListingCommand(requestId,
                            serverPlayer.getUniqueID().toString(), context.sourceServerId, askingPrice,
                            com.jsirgalaxybase.modules.core.banking.application.BankingConstants.DEFAULT_CURRENCY_CODE,
                            escrow));
                    }
                });
            return TerminalActionFeedback.of(TerminalNotificationSeverity.SUCCESS, "单件挂牌已发布",
                "listingId=" + result.getListing().getListingId() + "，物品=" + result.getSnapshot().getDisplayName()
                    + "，价格=" + formatAmount(result.getListing().getAskingPrice()) + " "
                    + result.getListing().getCurrencyCode() + "，已进入市场托管。",
                3600L);
        } catch (RuntimeException exception) {
            return errorFeedback("发布失败", exception);
        }
    }

    TerminalActionFeedback cancelCustomListing(EntityPlayer player, long listingId) {
        EntityPlayerMP serverPlayer = requireServerPlayer(player);
        if (serverPlayer == null) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "下架失败",
                "当前客户端上下文不能直接下架定制商品挂牌。",
                3600L);
        }

        CustomContext context = resolveCustomContext();
        if (!context.isReady()) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "定制商品市场不可用",
                context.unavailableMessage,
                3600L);
        }

        try {
            ensurePersonalMarketAccount(serverPlayer);
            CustomMarketService.CancelListingResult result = context.customMarketService.cancelListing(
                new CancelCustomMarketListingCommand(
                    newRequestId("terminal-custom-market-cancel"),
                    serverPlayer.getUniqueID().toString(),
                    context.sourceServerId,
                    listingId), new VaultCustomMarketDeliveryPort(context.vaultService));
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "挂牌已下架",
                "listingId=" + result.getListing().getListingId() + "，当前状态="
                    + result.getListing().getListingStatus() + " / " + result.getListing().getDeliveryStatus() + "。",
                3600L);
        } catch (RuntimeException exception) {
            return errorFeedback("下架失败", exception);
        }
    }

    TerminalActionFeedback claimCustomListing(EntityPlayer player, long listingId) {
        EntityPlayerMP serverPlayer = requireServerPlayer(player);
        if (serverPlayer == null) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "提取失败",
                "当前客户端上下文不能直接提取定制商品成交物。",
                3600L);
        }

        CustomContext context = resolveCustomContext();
        if (!context.isReady()) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "定制商品市场不可用",
                context.unavailableMessage,
                3600L);
        }

        try {
            ensurePersonalMarketAccount(serverPlayer);
            CustomMarketService.ClaimListingResult result = context.customMarketService.claimPurchasedListing(
                new ClaimCustomMarketListingCommand(
                    newRequestId("terminal-custom-market-claim"),
                    serverPlayer.getUniqueID().toString(),
                    context.sourceServerId,
                    listingId), new VaultCustomMarketDeliveryPort(context.vaultService));
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "成交物已完成提取",
                "listingId=" + result.getListing().getListingId() + "，当前交付状态="
                    + result.getListing().getDeliveryStatus() + "。",
                3600L);
        } catch (RuntimeException exception) {
            return errorFeedback("提取失败", exception);
        }
    }

    TerminalActionFeedback submitLimitBuy(EntityPlayer player, String productKey, long quantity, long unitPrice) {
        EntityPlayerMP serverPlayer = requireServerPlayer(player);
        if (serverPlayer == null) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "买单提交失败",
                "当前客户端上下文不能直接提交买单请求。",
                3600L);
        }

        MarketContext context = resolveContext();
        if (!context.isReady()) {
            return unavailableFeedback(context.unavailableMessage);
        }

        try {
            ensurePersonalMarketAccount(serverPlayer);
            StandardizedMarketProduct product = requireTradableProduct(context.spotMarketService, productKey);
            boolean stackable = resolveStackability(product);
            StandardizedSpotMarketService.CreateBuyOrderResult result = context.spotMarketService.createBuyOrder(
                new CreateBuyOrderCommand(
                    newRequestId("terminal-market-buy"),
                    serverPlayer.getUniqueID().toString(),
                    context.sourceServerId,
                    product.getProductKey(),
                    quantity,
                    stackable,
                    unitPrice));
            int pendingDeliveries = autoDeliverClaimables(context, result.getClaimableAssets());
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "买单已提交",
                "orderId="
                    + result.getOrder().getOrderId()
                    + "，当前剩余资金预留 "
                    + formatAmount(result.getOrder().getReservedFunds())
                    + " STARCOIN"
                    + (result.getTradeRecords().isEmpty() ? "。" : "，已立即撮合 " + result.getTradeRecords().size() + " 笔。")
                    + "手续费只按实际成交收取，未成交预留会在撤单或完成后释放。"
                    + describePendingDeliveries(pendingDeliveries),
                3600L);
        } catch (RuntimeException exception) {
            return errorFeedback("买单提交失败", exception);
        }
    }

    TerminalActionFeedback submitLimitSell(EntityPlayer player, String productKey, long quantity, long unitPrice) {
        EntityPlayerMP serverPlayer = requireServerPlayer(player);
        if (serverPlayer == null) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "卖单提交失败",
                "当前客户端上下文不能直接提交卖单请求。",
                3600L);
        }

        MarketContext context = resolveContext();
        if (!context.isReady()) {
            return unavailableFeedback(context.unavailableMessage);
        }

        try {
            ensurePersonalMarketAccount(serverPlayer);
            StandardizedMarketProduct product = requireTradableProduct(context.spotMarketService, productKey);
            StandardizedSpotMarketService.CreateSellOrderResult result = createSellOrderFromAccountInventory(context,
                serverPlayer.getUniqueID().toString(), product, quantity, unitPrice, "terminal-market-sell");
            int pendingDeliveries = autoDeliverClaimables(context, result.getClaimableAssets());
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "卖单已提交",
                "orderId="
                    + result.getOrder().getOrderId()
                    + "，已从账户仓预留数量 "
                    + formatAmount(quantity)
                    + (result.getTradeRecords().isEmpty() ? "。" : "，已立即撮合 " + result.getTradeRecords().size() + " 笔。")
                    + describePendingDeliveries(pendingDeliveries),
                3600L);
        } catch (RuntimeException exception) {
            return errorFeedback("卖单提交失败", exception);
        }
    }

    TerminalActionFeedback submitInstantBuy(EntityPlayer player, String productKey, long quantity) {
        EntityPlayerMP serverPlayer = requireServerPlayer(player);
        if (serverPlayer == null) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "即时买入失败",
                "当前客户端上下文不能直接提交即时买入请求。",
                3600L);
        }

        MarketContext context = resolveContext();
        if (!context.isReady()) {
            return unavailableFeedback(context.unavailableMessage);
        }

        List<MarketOrder> asks = context.orderRepository.findOpenSellOrdersByProductKey(productKey);
        DepthQuote quote = quoteDepth(asks, quantity, true);
        if (!quote.canFullyFill()) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "即时买入失败",
                "当前卖盘深度不足，最多可成交 " + formatAmount(quote.availableQuantity) + "。",
                3600L);
        }

        try {
            ensurePersonalMarketAccount(serverPlayer);
            StandardizedMarketProduct product = requireTradableProduct(context.spotMarketService, productKey);
            boolean stackable = resolveStackability(product);
            StandardizedSpotMarketService.CreateBuyOrderResult result = context.spotMarketService.createBuyOrder(
                new CreateBuyOrderCommand(
                    newRequestId("terminal-market-buy-now"),
                    serverPlayer.getUniqueID().toString(),
                    context.sourceServerId,
                    product.getProductKey(),
                    quantity,
                    stackable,
                    quote.extremeUnitPrice));
            int pendingDeliveries = autoDeliverClaimables(context, result.getClaimableAssets());
            if (result.getOrder().getOpenQuantity() > 0L) {
                try {
                    context.spotMarketService.cancelBuyOrder(new CancelBuyOrderCommand(
                        newRequestId("terminal-market-buy-now-cancel"),
                        serverPlayer.getUniqueID().toString(),
                        context.sourceServerId,
                        result.getOrder().getOrderId()));
                    return TerminalActionFeedback.of(
                        TerminalNotificationSeverity.WARNING,
                        "即时买入部分成交",
                        "已成交 " + formatAmount(result.getOrder().getFilledQuantity()) + "，剩余未成交部分已自动撤回。"
                            + describePendingDeliveries(pendingDeliveries),
                        4200L);
                } catch (RuntimeException cancelException) {
                    return buildInstantBuyResidualCancelFailureFeedback(result, cancelException);
                }
            }
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "即时买入完成",
                "已按当前卖盘成交 " + formatAmount(quantity) + "，预计总额 " + formatAmount(quote.totalWithFee)
                    + " STARCOIN。" + describePendingDeliveries(pendingDeliveries),
                3600L);
        } catch (RuntimeException exception) {
            return errorFeedback("即时买入失败", exception);
        }
    }

    TerminalActionFeedback submitInstantSell(EntityPlayer player, String productKey, long quantity) {
        EntityPlayerMP serverPlayer = requireServerPlayer(player);
        if (serverPlayer == null) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "即时卖出失败",
                "当前客户端上下文不能直接提交即时卖出请求。",
                3600L);
        }

        MarketContext context = resolveContext();
        if (!context.isReady()) {
            return unavailableFeedback(context.unavailableMessage);
        }

        StandardizedMarketProduct product;
        try {
            product = requireTradableProduct(context.spotMarketService, productKey);
        } catch (RuntimeException exception) {
            return errorFeedback("即时卖出失败", exception);
        }

        DepthQuote quote = quoteDepth(context.orderRepository.findOpenBuyOrdersByProductKey(productKey), quantity, false);
        if (!quote.canFullyFill()) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "即时卖出失败",
                "当前买盘深度不足，最多可成交 " + formatAmount(quote.availableQuantity) + "。",
                3600L);
        }

        try {
            ensurePersonalMarketAccount(serverPlayer);
            StandardizedSpotMarketService.CreateSellOrderResult result = createSellOrderFromAccountInventory(context,
                serverPlayer.getUniqueID().toString(), product, quantity, quote.extremeUnitPrice,
                "terminal-market-sell-now");
            int pendingDeliveries = autoDeliverClaimables(context, result.getClaimableAssets());
            if (result.getOrder().getOpenQuantity() > 0L) {
                try {
                    StandardizedSpotMarketService.CancelSellOrderResult cancellation =
                        context.spotMarketService.cancelSellOrder(new CancelSellOrderCommand(
                        newRequestId("terminal-market-sell-now-cancel"),
                        serverPlayer.getUniqueID().toString(),
                        context.sourceServerId,
                        result.getOrder().getOrderId()));
                    boolean returned = returnCancelledSellToAccountInventory(context,
                        serverPlayer.getUniqueID().toString(), cancellation.getCustody());
                    return TerminalActionFeedback.of(
                        returned ? TerminalNotificationSeverity.WARNING : TerminalNotificationSeverity.ERROR,
                        "即时卖出部分成交",
                        "已成交 " + formatAmount(result.getOrder().getFilledQuantity())
                            + (returned ? "，剩余未成交部分已返还账户仓。" : "，剩余资产暂存恢复区，未重复发放。")
                            + describePendingDeliveries(pendingDeliveries),
                        4200L);
                } catch (RuntimeException cancelException) {
                    return buildInstantSellResidualCancelFailureFeedback(result, cancelException);
                }
            }
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "即时卖出完成",
                "已按当前买盘成交 " + formatAmount(quantity) + "，预计净到账 " + formatAmount(quote.netAfterFee)
                    + " STARCOIN。" + describePendingDeliveries(pendingDeliveries),
                3600L);
        } catch (RuntimeException exception) {
            return errorFeedback("即时卖出失败", exception);
        }
    }

    boolean canDepositSelectedHeld(EntityPlayer player, String selectedProductKey) {
        if (selectedProductKey == null || selectedProductKey.trim().isEmpty()) {
            return false;
        }
        MarketContext context = resolveContext();
        if (!context.isReady()) {
            return false;
        }
        try {
            EntityPlayerMP serverPlayer = requireServerPlayer(player);
            if (serverPlayer == null) {
                return false;
            }
            StandardizedMarketProduct product = context.spotMarketService.inspectCatalogProduct(selectedProductKey)
                .requireProduct();
            return context.vaultService.countPersonalProduct(serverPlayer.getUniqueID().toString(),
                product.getRegistryName(), product.getMeta()) > 0;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * Transfers an explicit quantity from the personal Base Vault into market custody.
     * The request id is deliberately shared by both legs so audit and recovery see one operation.
     */
    TerminalActionFeedback submitDepositFromVault(EntityPlayer player, String selectedProductKey, long requestedQuantity) {
        EntityPlayerMP serverPlayer = requireServerPlayer(player);
        if (serverPlayer == null) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "存入失败",
                "当前客户端上下文不能直接提交存入请求。",
                3600L);
        }

        MarketContext context = resolveContext();
        if (!context.isReady()) {
            return unavailableFeedback(context.unavailableMessage);
        }

        StandardizedMarketProduct product;
        try {
            ensurePersonalMarketAccount(serverPlayer);
            product = context.spotMarketService.inspectCatalogProduct(selectedProductKey).requireProduct();
            final StandardizedMarketProduct selectedProduct = product;
            final String playerRef = serverPlayer.getUniqueID().toString();
            final long requested = requestedQuantity;
            final String requestId = newRequestId("vault-to-market");
            StandardizedSpotMarketService.DepositInventoryResult result = context.vaultService.inSharedTransaction(
                new java.util.function.Supplier<StandardizedSpotMarketService.DepositInventoryResult>() {
                    @Override
                    public StandardizedSpotMarketService.DepositInventoryResult get() {
                        int available = context.vaultService.countPersonalProduct(playerRef,
                            selectedProduct.getRegistryName(), selectedProduct.getMeta());
                        if (available <= 0) {
                            throw new MarketOperationException("Base Vault 中没有可存入市场托管的选中标准商品");
                        }
                        if (requested <= 0L) {
                            throw new MarketOperationException("请先在个人仓选择有效的存入数量");
                        }
                        if (requested > available) {
                            throw new MarketOperationException("个人仓可用数量不足，当前仅有 " + available + " 单位");
                        }
                        ItemStack vaultStack = context.vaultService.takeStandardizedProduct(requestId,
                            playerRef, selectedProduct.getRegistryName(), selectedProduct.getMeta(), (int) requested,
                            "MARKET_CUSTODY");
                        return context.spotMarketService.depositInventory(new DepositMarketInventoryCommand(
                            requestId, playerRef, context.sourceServerId, selectedProduct.getProductKey(),
                            vaultStack.stackSize, vaultStack.getMaxStackSize() > 1));
                    }
                });
            long deposited = result.getCustody().getQuantity();
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "已存入市场托管",
                "已将个人仓中的 " + formatAmount(deposited) + " 单位转入市场可售库存，当前可售 "
                    + formatAmount(result.getTotalAvailableQuantity()) + "。",
                3600L);
        } catch (RuntimeException exception) {
            return errorFeedback("存入失败", exception);
        }
    }

    /** Legacy route kept for old clients; new terminal payloads always provide an explicit quantity. */
    TerminalActionFeedback submitDepositSelectedHeld(EntityPlayer player, String selectedProductKey) {
        return TerminalActionFeedback.of(TerminalNotificationSeverity.ERROR, "需要选择个人仓资产",
            "请在市场详情中打开个人仓选择器，选择数量后再存入市场托管。", 3600L);
    }

    private StandardizedSpotMarketService.CreateSellOrderResult createSellOrderFromAccountInventory(
        final MarketContext context, final String playerRef, final StandardizedMarketProduct product,
        final long quantity, final long unitPrice, String requestPrefix) {
        if (quantity > Integer.MAX_VALUE) {
            throw new MarketOperationException("单次卖出数量超过账户仓可处理上限");
        }
        final String correlationId = newRequestId(requestPrefix);
        return context.vaultService.inSharedTransaction(
            new java.util.function.Supplier<StandardizedSpotMarketService.CreateSellOrderResult>() {

                @Override
                public StandardizedSpotMarketService.CreateSellOrderResult get() {
                    long sellable = context.accountInventoryResolver.countSellable(playerRef, product);
                    if (sellable < quantity) {
                        throw new MarketOperationException("账户仓可卖数量不足，当前仅有 " + sellable + " 单位");
                    }
                    ItemStack reserved = context.accountInventoryResolver.reserveForSell(
                        correlationId + ":reserve", playerRef, product, (int) quantity);
                    context.spotMarketService.depositInventory(new DepositMarketInventoryCommand(
                        correlationId + ":custody", playerRef, context.sourceServerId, product.getProductKey(),
                        reserved.stackSize, reserved.getMaxStackSize() > 1));
                    return context.spotMarketService.createSellOrder(new CreateSellOrderCommand(
                        correlationId + ":order", playerRef, context.sourceServerId, product.getProductKey(), quantity,
                        resolveStackability(product), unitPrice));
                }
            });
    }

    private int autoDeliverClaimables(MarketContext context, List<MarketCustodyInventory> claimableAssets) {
        if (claimableAssets == null || claimableAssets.isEmpty()) {
            return 0;
        }
        int pending = 0;
        for (MarketCustodyInventory custody : claimableAssets) {
            if (custody == null || custody.getStatus() != MarketCustodyStatus.CLAIMABLE) {
                continue;
            }
            try {
                context.spotMarketService.claimMarketAsset(new ClaimMarketAssetCommand(
                    "market-auto-delivery:" + custody.getCustodyId(), custody.getOwnerPlayerRef(),
                    context.sourceServerId, custody.getCustodyId()));
            } catch (RuntimeException deliveryFailure) {
                pending++;
            }
        }
        return pending;
    }

    private boolean returnCancelledSellToAccountInventory(MarketContext context, String playerRef,
        MarketCustodyInventory availableCustody) {
        if (availableCustody == null || availableCustody.getQuantity() <= 0L) {
            return true;
        }
        try {
            MarketCustodyInventory claimable = context.custodyRepository.update(availableCustody.withStateAndQuantity(
                MarketCustodyStatus.CLAIMABLE, availableCustody.getQuantity(), 0L, Instant.now()));
            context.spotMarketService.claimMarketAsset(new ClaimMarketAssetCommand(
                "market-cancel-return:" + claimable.getCustodyId(), playerRef, context.sourceServerId,
                claimable.getCustodyId()));
            return true;
        } catch (RuntimeException deliveryFailure) {
            return false;
        }
    }

    private static String describePendingDeliveries(int pendingDeliveries) {
        return pendingDeliveries <= 0 ? "" : " 有 " + pendingDeliveries + " 笔成交因账户仓容量或交付状态暂待收货。";
    }

    TerminalActionFeedback cancelOrder(EntityPlayer player, long orderId) {
        return cancelOrder(player, orderId, "", 0L);
    }

    TerminalActionFeedback cancelOrder(EntityPlayer player, long orderId, String requestId,
        long expectedUpdatedAtEpochSecond) {
        EntityPlayerMP serverPlayer = requireServerPlayer(player);
        if (serverPlayer == null) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "撤单失败",
                "当前客户端上下文不能直接提交撤单请求。",
                3600L);
        }

        MarketContext context = resolveContext();
        if (!context.isReady()) {
            return unavailableFeedback(context.unavailableMessage);
        }

        try {
            ensurePersonalMarketAccount(serverPlayer);
            Optional<MarketOrder> order = context.orderRepository.findById(orderId);
            if (!order.isPresent()) {
                throw new MarketOperationException("orderId 对应的订单不存在");
            }
            String playerRef = serverPlayer.getUniqueID().toString();
            if (!playerRef.equals(order.get().getOwnerPlayerRef())) {
                throw new MarketOperationException("当前订单不属于登录玩家");
            }
            if ((order.get().getStatus() != MarketOrderStatus.OPEN
                && order.get().getStatus() != MarketOrderStatus.PARTIALLY_FILLED)
                || order.get().getOpenQuantity() <= 0L) {
                return TerminalActionFeedback.of(TerminalNotificationSeverity.WARNING, "订单状态已变化",
                    "该订单已不可撤销，订单中心将刷新为服务端最新状态。", 4200L);
            }
            long currentVersion = order.get().getUpdatedAt() == null ? 0L : order.get().getUpdatedAt().getEpochSecond();
            if (expectedUpdatedAtEpochSecond > 0L && currentVersion != expectedUpdatedAtEpochSecond) {
                return TerminalActionFeedback.of(TerminalNotificationSeverity.WARNING, "订单状态已变化",
                    "确认期间订单发生了成交或状态更新，请核对最新剩余量后重新撤单。", 4200L);
            }
            String effectiveRequestId = requestId == null || requestId.trim().isEmpty()
                ? newRequestId("terminal-market-cancel") : requestId.trim();
            if (order.get().getSide() == MarketOrderSide.BUY) {
                StandardizedSpotMarketService.CancelBuyOrderResult result = context.spotMarketService.cancelBuyOrder(
                    new CancelBuyOrderCommand(
                        effectiveRequestId,
                        playerRef,
                        context.sourceServerId,
                        orderId,
                        expectedUpdatedAtEpochSecond));
                return TerminalActionFeedback.of(
                    TerminalNotificationSeverity.SUCCESS,
                    "买单已撤销",
                    "orderId=" + result.getOrder().getOrderId() + "，已释放 " + formatAmount(result.getReleasedFunds())
                        + " STARCOIN。",
                    3600L);
            }

            StandardizedSpotMarketService.CancelSellOrderResult result = context.spotMarketService.cancelSellOrder(
                new CancelSellOrderCommand(
                    effectiveRequestId,
                    playerRef,
                    context.sourceServerId,
                    orderId,
                    expectedUpdatedAtEpochSecond));
            boolean returned = returnCancelledSellToAccountInventory(context,
                playerRef, result.getCustody());
            return TerminalActionFeedback.of(
                returned ? TerminalNotificationSeverity.SUCCESS : TerminalNotificationSeverity.WARNING,
                "卖单已撤销",
                "orderId=" + result.getOrder().getOrderId()
                    + (returned ? "，剩余数量已返还账户仓。" : "，账户仓暂不可接收，剩余资产已保留待恢复。"),
                3600L);
        } catch (RuntimeException exception) {
            return errorFeedback("撤单失败", exception);
        }
    }

    TerminalActionFeedback claimAsset(EntityPlayer player, long custodyId) {
        EntityPlayerMP serverPlayer = requireServerPlayer(player);
        if (serverPlayer == null) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "提取失败",
                "当前客户端上下文不能直接提交提取请求。",
                3600L);
        }

        MarketContext context = resolveContext();
        if (!context.isReady()) {
            return unavailableFeedback(context.unavailableMessage);
        }

        try {
            ensurePersonalMarketAccount(serverPlayer);
            StandardizedSpotMarketService.ClaimMarketAssetResult result = context.spotMarketService.claimMarketAsset(
                new ClaimMarketAssetCommand(
                    newRequestId("terminal-market-claim"),
                    serverPlayer.getUniqueID().toString(),
                    context.sourceServerId,
                    custodyId));
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "资产已提取",
                "数量 " + formatAmount(result.getCustody().getQuantity()) + " 已存入个人仓。",
                3600L);
        } catch (RuntimeException exception) {
            return errorFeedback("提取失败", exception);
        }
    }

    ItemStack resolveDisplayStack(String productKey) {
        if (productKey == null || productKey.trim().isEmpty()) {
            return null;
        }
        MarketContext context = resolveContext();
        if (!context.isReady()) {
            return null;
        }
        return resolveDisplayStack(context.spotMarketService, productKey);
    }

    private ItemStack resolveDisplayStack(StandardizedSpotMarketService spotMarketService, String productKey) {
        try {
            StandardizedMarketProduct product = requireTradableProduct(spotMarketService, productKey);
            Item item = resolveItem(product);
            return item == null ? null : new ItemStack(item, 1, product.getMeta());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static DepthQuote quoteDepth(List<MarketOrder> orders, long requestedQuantity, boolean buySide) {
        long remaining = Math.max(0L, requestedQuantity);
        long available = 0L;
        long grossAmount = 0L;
        long extremePrice = 0L;
        for (MarketOrder order : orders) {
            if (order == null || order.getOpenQuantity() <= 0L) {
                continue;
            }
            available += order.getOpenQuantity();
            if (remaining <= 0L) {
                continue;
            }
            long matched = Math.min(remaining, order.getOpenQuantity());
            grossAmount += matched * order.getUnitPrice();
            extremePrice = order.getUnitPrice();
            remaining -= matched;
        }
        long matchedQuantity = Math.max(0L, requestedQuantity - remaining);
        long fee = matchedQuantity <= 0L ? 0L : calculateFee(grossAmount, TAKER_FEE_BASIS_POINTS);
        return new DepthQuote(requestedQuantity, available, matchedQuantity, extremePrice, grossAmount, fee);
    }

    private TerminalMarketSnapshot createUnavailableSnapshot(String message,
        TerminalExchangeQuoteView exchangeQuoteView) {
        return new TerminalMarketSnapshot(
            "市场服务不可用",
            message,
            emptyArray(PRODUCT_LIMIT),
            emptyArray(PRODUCT_LIMIT),
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
            "当前无法读取市场运行时。",
            emptyArray(BOOK_DEPTH),
            emptyArray(BOOK_DEPTH),
            emptyArray(BOOK_DEPTH),
            emptyArray(BOOK_DEPTH),
            "市场服务不可用。",
            "市场服务不可用。",
            "市场服务不可用。",
            "市场服务不可用。",
            "市场运行时离线，当前不能读取正式目录或个人账户仓。",
            "0",
            "0",
            "0",
            "0 STARCOIN",
            "运行时离线，无法确认可售、卖单锁定和待收货状态。",
            emptyArray(ORDER_LIMIT),
            emptyArray(ORDER_LIMIT),
            emptyArray(ORDER_LIMIT),
            emptyArray(CLAIM_LIMIT),
            emptyArray(CLAIM_LIMIT),
            new String[] {
                "当前市场运行时未就绪。",
                "请先确认 dedicated server 已完成银行与市场模块启动。",
                "若日志提示缺表，需要先补 market_order 等市场表。",
                "不要把当前空状态误判为 GUI 自身故障。"
            },
            exchangeQuoteView.serviceState,
            exchangeQuoteView.heldSummary,
            exchangeQuoteView.inputRegistryName,
            exchangeQuoteView.pairCode,
            exchangeQuoteView.inputAssetCode,
            exchangeQuoteView.outputAssetCode,
            exchangeQuoteView.ruleVersion,
            exchangeQuoteView.limitStatus,
            exchangeQuoteView.reasonCode,
            exchangeQuoteView.notes,
            exchangeQuoteView.inputQuantity,
            exchangeQuoteView.nominalFaceValue,
            exchangeQuoteView.effectiveExchangeValue,
            exchangeQuoteView.contributionValue,
            exchangeQuoteView.discountStatus,
            exchangeQuoteView.exchangeRateDisplay,
            exchangeQuoteView.executionHint,
            exchangeQuoteView.executableFlag);
    }

    private TerminalMarketSnapshot createEmptySnapshot(List<String> productKeys,
        MarketContext context, TerminalExchangeQuoteView exchangeQuoteView) {
        String browserHint = productKeys.isEmpty()
            ? "正式商品目录当前为空；请由管理员启用目录商品。"
            : "请选择左侧商品进入交易详情。";
        return new TerminalMarketSnapshot(
            context.isReady() ? "市场服务在线 / 暂无选中商品" : "市场服务不可用",
            browserHint,
            toSizedArray(productKeys, PRODUCT_LIMIT),
            buildProductLabels(productKeys, context.spotMarketService),
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
            "先点击一个商品，再查看订单簿和交易动作。",
            emptyArray(BOOK_DEPTH),
            emptyArray(BOOK_DEPTH),
            emptyArray(BOOK_DEPTH),
            emptyArray(BOOK_DEPTH),
            "请选择商品后填写限价买单。",
            "请选择商品后填写限价卖单，卖出只会消耗账户仓库存。",
            "请选择商品后填写即时买入数量。",
            "请选择商品后填写即时卖出数量，卖出只会消耗账户仓库存。",
            "尚未选择正式目录商品。",
            "0",
            "0",
            "0",
            "0 STARCOIN",
            "当前运行时卖出资金直接记入银行账户，卖单只从账户仓预留库存。",
            emptyArray(ORDER_LIMIT),
            emptyArray(ORDER_LIMIT),
            emptyArray(ORDER_LIMIT),
            emptyArray(CLAIM_LIMIT),
            emptyArray(CLAIM_LIMIT),
            new String[] {
                "点击商品后才能查看订单簿。",
                "即时成交仍按真实盘口撮合，不按最新成交价直接结算。",
                "卖单来源是个人账户仓与市场托管，不读取玩家背包。",
                "待收货资产可在详情页接收至账户仓。"
            },
            exchangeQuoteView.serviceState,
            exchangeQuoteView.heldSummary,
            exchangeQuoteView.inputRegistryName,
            exchangeQuoteView.pairCode,
            exchangeQuoteView.inputAssetCode,
            exchangeQuoteView.outputAssetCode,
            exchangeQuoteView.ruleVersion,
            exchangeQuoteView.limitStatus,
            exchangeQuoteView.reasonCode,
            exchangeQuoteView.notes,
            exchangeQuoteView.inputQuantity,
            exchangeQuoteView.nominalFaceValue,
            exchangeQuoteView.effectiveExchangeValue,
            exchangeQuoteView.contributionValue,
            exchangeQuoteView.discountStatus,
            exchangeQuoteView.exchangeRateDisplay,
            exchangeQuoteView.executionHint,
            exchangeQuoteView.executableFlag);
    }

    private TerminalExchangeQuoteView buildExchangeQuoteView(EntityPlayer player, ExchangeContext context) {
        if (context == null || !context.isReady()) {
            return TerminalExchangeQuoteView.empty(
                "汇率市场不可用",
                "当前未选择 Base Vault 物品",
                "--",
                context == null ? "汇率市场运行时未完成装配。" : context.unavailableMessage,
                "当前不能继续执行兑换。");
        }
        if (!(player instanceof EntityPlayerMP)) {
            return TerminalExchangeQuoteView.empty(
                "汇率市场不可用",
                "当前客户端上下文不能读取兑换资产",
                "--",
                "当前客户端上下文不能直接读取正式报价。",
                "当前不能继续执行兑换。");
        }
        // Compatibility-only overload. Terminal routes use the Base Vault
        // overload below and must never infer a quote from the held stack.
        return buildExchangeQuoteView(resolvePlayerRef(player), null, context);
    }

    private TerminalExchangeQuoteView buildExchangeQuoteView(ItemStack heldStack, ExchangeContext context) {
        return buildExchangeQuoteView("preview-player", heldStack, context);
    }

    private TerminalExchangeQuoteView buildExchangeQuoteView(String playerRef, ItemStack heldStack,
        ExchangeContext context) {
        String heldSummary = buildExchangeHeldSummary(heldStack);
        String inputRegistryName = resolveItemRegistryName(heldStack == null ? null : heldStack.getItem());
        if (!context.isReady()) {
            return TerminalExchangeQuoteView.empty(
                "汇率市场不可用",
                heldSummary,
                inputRegistryName,
                context.unavailableMessage,
                "当前不能继续执行兑换。");
        }

        if (heldStack == null || heldStack.getItem() == null || heldStack.stackSize <= 0) {
            return TerminalExchangeQuoteView.empty(
                "汇率市场在线 / 等待 Vault 任务书硬币",
                "当前未选择 Base Vault 物品",
                "--",
                "请先在 Base Vault 中选择任务书硬币，再刷新报价。",
                "当前不能继续执行兑换。");
        }

        try {
            return TerminalExchangeQuoteView.fromQuote(
                heldSummary,
                context.exchangeService.previewVaultCoinFormal(playerRef, heldStack).getFormalQuote());
        } catch (RuntimeException exception) {
            return TerminalExchangeQuoteView.empty(
                "汇率市场在线 / 报价失败",
                heldSummary,
                inputRegistryName,
                toClientSafeMessage(exception),
                "当前不能继续执行兑换。");
        }
    }

    private ItemStack resolveVaultStack(EntityPlayer player, ExchangeContext context, int slotIndex) {
        if (context == null || context.vaultService == null || slotIndex < 0) {
            return null;
        }
        String playerRef = resolvePlayerRef(player);
        if (playerRef == null || playerRef.isEmpty()) {
            return null;
        }
        try {
            List<VaultSlot> slots = context.vaultService.viewPersonalVault(playerRef).getSlots();
            return slotIndex >= slots.size() ? null : slots.get(slotIndex).getStack();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private TerminalActionFeedback buildExchangeExecutionFeedback(ExchangeMarketExecutionResult result) {
        return TerminalActionFeedback.of(
            TerminalNotificationSeverity.SUCCESS,
            "汇率兑换已入账",
            "transactionId=" + result.getPostingResult().getTransaction().getTransactionId() + "，pair="
                + result.getQuoteResult().getPairDefinition().getPairCode() + "，实际兑换值="
                + formatAmount(result.getQuoteResult().getEffectiveExchangeValue()) + " STARCOIN，贡献值="
                + formatAmount(result.getQuoteResult().getContributionValue()) + "，ruleVersion="
                + result.getQuoteResult().getRuleVersion().getRuleKey() + "，reasonCode="
                + result.getQuoteResult().getLimitPolicy().getReasonCode() + "。",
            4200L);
    }

    private TerminalActionFeedback exchangeUnavailableFeedback(String message) {
        return TerminalActionFeedback.of(TerminalNotificationSeverity.ERROR, "汇率市场不可用", message, 4200L);
    }

    private String buildExchangeHeldSummary(ItemStack heldStack) {
        if (heldStack == null || heldStack.getItem() == null || heldStack.stackSize <= 0) {
            return "当前未选择 Base Vault 资产";
        }
        String displayName;
        try {
            displayName = heldStack.getDisplayName();
        } catch (RuntimeException ignored) {
            displayName = "未命名物品";
        }
        return displayName + " x" + formatAmount(heldStack.stackSize);
    }

    private String resolveItemRegistryName(Item item) {
        if (item == null) {
            return "";
        }
        GameRegistry.UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(item);
        if (identifier != null) {
            return identifier.modId + ":" + identifier.name;
        }
        Object fallback = GameData.getItemRegistry().getNameForObject(item);
        return fallback == null ? "" : String.valueOf(fallback);
    }

    private static long calculateFee(long amount, int basisPoints) {
        if (amount <= 0L || basisPoints <= 0) {
            return 0L;
        }
        return amount * basisPoints / 10000L;
    }

    private String normalizeSelectedProductKey(String requestedProductKey, List<String> productKeys) {
        if (requestedProductKey != null && !requestedProductKey.trim().isEmpty()) {
            for (String productKey : productKeys) {
                if (requestedProductKey.equals(productKey)) {
                    return productKey;
                }
            }
        }
        return null;
    }

    private List<CustomMarketService.ListingView> mergeCustomViews(List<CustomMarketService.ListingView> first,
        List<CustomMarketService.ListingView> second, int limit) {
        List<CustomMarketService.ListingView> result = new ArrayList<CustomMarketService.ListingView>();
        appendCustomViews(result, first, limit);
        appendCustomViews(result, second, limit);
        return result;
    }

    private CustomMarketService.ListingView findCustomSelectedView(String selectedListingId,
        List<CustomMarketService.ListingView> activeViews, List<CustomMarketService.ListingView> sellingViews,
        List<CustomMarketService.ListingView> pendingViews) {
        if (selectedListingId == null || selectedListingId.trim().isEmpty()) {
            return null;
        }
        List<CustomMarketService.ListingView> combined = new ArrayList<CustomMarketService.ListingView>();
        appendCustomViews(combined, activeViews);
        appendCustomViews(combined, sellingViews);
        appendCustomViews(combined, pendingViews);
        for (CustomMarketService.ListingView view : combined) {
            if (view != null && view.getListing() != null
                && selectedListingId.equals(String.valueOf(view.getListing().getListingId()))) {
                return view;
            }
        }
        return null;
    }

    private void appendCustomViews(List<CustomMarketService.ListingView> target,
        List<CustomMarketService.ListingView> source) {
        appendCustomViews(target, source, Integer.MAX_VALUE);
    }

    private void appendCustomViews(List<CustomMarketService.ListingView> target,
        List<CustomMarketService.ListingView> source, int limit) {
        if (source == null) {
            return;
        }
        for (CustomMarketService.ListingView view : source) {
            if (view != null) {
                target.add(view);
                if (target.size() >= limit) {
                    return;
                }
            }
        }
    }

    private String buildCustomBrowserHint(int selectedScope, List<CustomMarketService.ListingView> activeViews,
        List<CustomMarketService.ListingView> sellingViews, List<CustomMarketService.ListingView> pendingViews) {
        if (selectedScope == CUSTOM_SCOPE_SELLING) {
            return sellingViews.isEmpty() ? "你当前没有仍在出售中的挂牌。" : "先从你的挂牌里选一条，再看详情或执行 cancel。";
        }
        if (selectedScope == CUSTOM_SCOPE_PENDING) {
            return pendingViews.isEmpty() ? "你当前没有待处理成交记录。" : "待处理包含买家待领取与卖家待买家领取记录，先选一条看详情。";
        }
        return activeViews.isEmpty() ? "当前没有 active custom listings。" : "先浏览挂牌，再点进单条 listing 详情执行 buy。";
    }

    private String describeCustomScope(int scope) {
        if (scope == CUSTOM_SCOPE_SELLING) {
            return "我的出售";
        }
        if (scope == CUSTOM_SCOPE_PENDING) {
            return "我的待处理";
        }
        return "全部挂牌";
    }

    private String describeCustomScopeKey(int scope) {
        if (scope == CUSTOM_SCOPE_SELLING) return "selling";
        if (scope == CUSTOM_SCOPE_PENDING) return "pending";
        return "active";
    }

    private String[] buildCustomListingLines(List<CustomMarketService.ListingView> views) {
        List<String> lines = new ArrayList<String>(CUSTOM_LISTING_LIMIT);
        if (views != null) {
            for (CustomMarketService.ListingView view : views) {
                if (view == null || view.getListing() == null || view.getSnapshot() == null) {
                    continue;
                }
                lines.add("#" + view.getListing().getListingId() + " | " + view.getSnapshot().getDisplayName()
                    + " | " + formatAmount(view.getListing().getAskingPrice()) + " "
                    + view.getListing().getCurrencyCode() + " | " + view.getListing().getListingStatus());
                if (lines.size() >= CUSTOM_LISTING_LIMIT) {
                    break;
                }
            }
        }
        if (lines.isEmpty()) {
            lines.add("当前范围下没有可显示挂牌。");
        }
        return toSizedArray(lines, CUSTOM_LISTING_LIMIT);
    }

    private String[] buildCustomListingIds(List<CustomMarketService.ListingView> views) {
        List<String> ids = new ArrayList<String>(CUSTOM_LISTING_LIMIT);
        if (views != null) {
            for (CustomMarketService.ListingView view : views) {
                if (view != null && view.getListing() != null) {
                    ids.add(String.valueOf(view.getListing().getListingId()));
                    if (ids.size() >= CUSTOM_LISTING_LIMIT) {
                        break;
                    }
                }
            }
        }
        return toSizedArray(ids, CUSTOM_LISTING_LIMIT);
    }

    private String[] buildCustomListingIconRefs(List<CustomMarketService.ListingView> views) {
        List<String> refs = new ArrayList<String>(CUSTOM_LISTING_LIMIT);
        if (views != null) {
            for (CustomMarketService.ListingView view : views) {
                if (view == null || view.getSnapshot() == null) {
                    continue;
                }
                refs.add(view.getSnapshot().getItemId() + "@" + view.getSnapshot().getMeta()
                    + " x" + Math.max(1, view.getSnapshot().getStackSize()));
                if (refs.size() >= CUSTOM_LISTING_LIMIT) {
                    break;
                }
            }
        }
        return toSizedArray(refs, CUSTOM_LISTING_LIMIT);
    }

    private String buildCustomCounterparty(String playerRef, CustomMarketService.ListingView selectedView) {
        if (selectedView == null || selectedView.getListing() == null) {
            return "请先选择挂牌";
        }
        String seller = selectedView.getListing().getSellerPlayerRef();
        String buyer = selectedView.getListing().getBuyerPlayerRef();
        if (playerRef != null && playerRef.equals(seller)) {
            return "你的挂牌 / 买家=" + safeText(buyer, "暂无") + " / 来源服=" + selectedView.getListing().getSourceServerId();
        }
        return "卖家=" + safeText(seller, "未知") + " / 买家=" + safeText(buyer, "暂无") + " / 来源服="
            + selectedView.getListing().getSourceServerId();
    }

    private String buildCustomTradeSummary(CustomMarketService.ListingView selectedView) {
        if (selectedView == null || selectedView.getListing() == null) {
            return "--";
        }
        if (selectedView.getTradeRecord() == null) {
            return "当前尚未成交，交付状态=" + selectedView.getListing().getDeliveryStatus();
        }
        return "tradeId=" + selectedView.getTradeRecord().getTradeId() + " / settled="
            + formatAmount(selectedView.getTradeRecord().getSettledAmount()) + " "
            + selectedView.getTradeRecord().getCurrencyCode() + " / delivery="
            + selectedView.getTradeRecord().getDeliveryStatus();
    }

    private String buildCustomActionHint(String playerRef, CustomMarketService.ListingView selectedView) {
        if (selectedView == null || selectedView.getListing() == null) {
            return "先从左侧列表选中一条挂牌。";
        }
        if (CustomMarketService.isUiDemoListing(selectedView.getListing())) {
            return "演示挂牌，仅用于 UI 核验，不会进入真实结算或交付。";
        }
        if (canClaimCustomListing(playerRef, selectedView)) {
            return "当前是你买下且待领取的 listing，可执行 claim。";
        }
        if (canCancelCustomListing(playerRef, selectedView)) {
            return "当前是你的 active listing，可执行 cancel。";
        }
        if (canBuyCustomListing(playerRef, selectedView)) {
            return "当前是他人 active listing，可执行 buy。";
        }
        return "当前详情只读，不能执行 buy / cancel / claim。";
    }

    private boolean canBuyCustomListing(String playerRef, CustomMarketService.ListingView selectedView) {
        return selectedView != null && selectedView.getListing() != null
            && !CustomMarketService.isUiDemoListing(selectedView.getListing())
            && selectedView.getListing().getListingStatus() == CustomMarketListingStatus.ACTIVE
            && playerRef != null && !playerRef.equals(selectedView.getListing().getSellerPlayerRef());
    }

    private boolean canCancelCustomListing(String playerRef, CustomMarketService.ListingView selectedView) {
        return selectedView != null && selectedView.getListing() != null
            && !CustomMarketService.isUiDemoListing(selectedView.getListing())
            && playerRef != null && playerRef.equals(selectedView.getListing().getSellerPlayerRef())
            && selectedView.getListing().getListingStatus() == CustomMarketListingStatus.ACTIVE;
    }

    private boolean canClaimCustomListing(String playerRef, CustomMarketService.ListingView selectedView) {
        return selectedView != null && selectedView.getListing() != null
            && !CustomMarketService.isUiDemoListing(selectedView.getListing())
            && playerRef != null && playerRef.equals(selectedView.getListing().getBuyerPlayerRef())
            && selectedView.getListing().getDeliveryStatus() == CustomMarketDeliveryStatus.BUYER_PENDING_CLAIM;
    }

    private TerminalCustomMarketSnapshot createUnavailableCustomSnapshot(String message, int selectedScope) {
        return new TerminalCustomMarketSnapshot(
            "定制商品市场不可用",
            message,
            describeCustomScope(selectedScope),
            toSizedArray(new ArrayList<String>(), CUSTOM_LISTING_LIMIT),
            toSizedArray(new ArrayList<String>(), CUSTOM_LISTING_LIMIT),
            toSizedArray(new ArrayList<String>(), CUSTOM_LISTING_LIMIT),
            toSizedArray(new ArrayList<String>(), CUSTOM_LISTING_LIMIT),
            toSizedArray(new ArrayList<String>(), CUSTOM_LISTING_LIMIT),
            toSizedArray(new ArrayList<String>(), CUSTOM_LISTING_LIMIT),
            toSizedArray(new ArrayList<String>(), CUSTOM_LISTING_LIMIT),
            toSizedArray(new ArrayList<String>(), CUSTOM_LISTING_LIMIT),
            toSizedArray(new ArrayList<String>(), CUSTOM_LISTING_LIMIT),
            "",
            "未选中挂牌",
            "--",
            "--",
            "当前无法读取 listing 详情",
            "--",
            "--",
            "当前不能继续执行 custom market 动作。",
            "0",
            "0",
            "0",
            Collections.<CustomMarketService.ListingView>emptyList(),
            Collections.<CustomMarketService.ListingView>emptyList(),
            Collections.<CustomMarketService.ListingView>emptyList(),
            Collections.<CustomMarketService.ListingView>emptyList(), 0);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private Map<String, TerminalMarketSnapshot.CatalogMarketSummary> buildCatalogMarketSummaries(MarketContext context,
        StandardizedMarketCatalogPage page, String playerRef, String selectedProductKey,
        List<MarketTradeRecord> selectedRecentTrades, List<MarketTradeRecord> selectedDayTrades,
        List<MarketOrder> selectedBids, List<MarketOrder> selectedAsks, long selectedAvailable,
        long selectedEscrow, long selectedClaimable, String chartRange) {
        if (context == null || page == null || page.getEntries().isEmpty()) {
            return Collections.emptyMap();
        }
        List<StandardizedMarketProduct> products = new ArrayList<StandardizedMarketProduct>();
        Map<String, Long> referencePrices = new HashMap<String, Long>();
        for (StandardizedMarketCatalogEntry entry : page.getEntries()) {
            if (entry != null && entry.getProduct() != null) {
                products.add(entry.getProduct());
                referencePrices.put(entry.getProduct().getProductKey(), Long.valueOf(entry.getReferencePrice()));
            }
        }
        StandardizedMarketReadRepository reader = new StandardizedMarketReadRepository(context.orderRepository,
            context.tradeRecordRepository, context.accountInventoryResolver);
        Instant now = Instant.now();
        Map<String, StandardizedMarketReadRepository.ProductQuote> quotes = reader.readPage(products, playerRef, now);
        Map<String, TerminalMarketSnapshot.CatalogMarketSummary> results =
            new LinkedHashMap<String, TerminalMarketSnapshot.CatalogMarketSummary>();
        for (StandardizedMarketProduct product : products) {
            String productKey = product.getProductKey();
            StandardizedMarketReadRepository.ProductQuote quote = quotes.get(productKey);
            List<MarketTradeRecord> trades = quote == null ? Collections.<MarketTradeRecord>emptyList() : quote.trades;
            List<TerminalMarketSnapshot.MarketPricePoint> points = new ArrayList<TerminalMarketSnapshot.MarketPricePoint>();
            appendIntradayPricePoints(points, trades, now);
            if (productKey.equals(selectedProductKey)) {
                List<StandardizedMarketReadRepository.Candle> candles = reader.readCandles(productKey, now,
                    parseChartRange(chartRange), referencePrices.containsKey(productKey)
                        ? referencePrices.get(productKey).longValue() : 0L);
                points.clear();
                for (StandardizedMarketReadRepository.Candle candle : candles) {
                    points.add(new TerminalMarketSnapshot.MarketPricePoint(candle.open, candle.high, candle.low,
                        candle.close, candle.volume, candle.turnover, candle.startEpochSeconds,
                        candle.source.name()));
                }
            }
            String latest = quote == null || quote.latestPrice <= 0L ? "--"
                : formatAmount(quote.latestPrice) + " STARCOIN";
            String bestBid = quote == null || quote.bestBidPrice <= 0L ? "--"
                : formatAmount(quote.bestBidPrice) + " x" + formatAmount(quote.bestBidQuantity);
            String bestAsk = quote == null || quote.bestAskPrice <= 0L ? "--"
                : formatAmount(quote.bestAskPrice) + " x" + formatAmount(quote.bestAskQuantity);
            results.put(productKey, new TerminalMarketSnapshot.CatalogMarketSummary(latest, bestBid, bestAsk,
                formatAmount(quote == null ? 0L : quote.volume24h),
                formatAmount(quote == null ? 0L : quote.sellableQuantity),
                productKey.equals(selectedProductKey) ? formatAmount(selectedEscrow) : "0",
                productKey.equals(selectedProductKey) ? formatAmount(selectedClaimable) : "0",
                formatDayChange(quote), points));
        }
        return results;
    }

    /** Applies market-data filters and ordering before pagination, rather than reordering one client page. */
    private StandardizedMarketCatalogPage browseCatalogPage(MarketContext context,
        TerminalMarketSnapshotRequest controller, String playerRef) {
        String query = controller.getBrowserQuery();
        String filter = normalizeBrowseOption(controller.getBrowserFilter(), "ALL");
        String sort = normalizeBrowseOption(controller.getBrowserSort(), "DIRECTORY");
        if ("ALL".equals(filter) && "DIRECTORY".equals(sort)) {
            return context.spotMarketService.browseCatalog(query, controller.getBrowserPage(), PRODUCT_LIMIT);
        }

        StandardizedMarketCatalogPage first = context.spotMarketService.browseCatalog(query, 0, 64);
        List<StandardizedMarketCatalogEntry> entries = new ArrayList<StandardizedMarketCatalogEntry>(first.getEntries());
        int totalPages = first.getTotalEntries() == 0 ? 1 : (first.getTotalEntries() + 63) / 64;
        for (int page = 1; page < totalPages && entries.size() < 4096; page++) {
            entries.addAll(context.spotMarketService.browseCatalog(query, page, 64).getEntries());
        }

        List<StandardizedMarketProduct> products = new ArrayList<StandardizedMarketProduct>();
        for (StandardizedMarketCatalogEntry entry : entries) {
            if (entry != null && entry.getProduct() != null) { products.add(entry.getProduct()); }
        }
        StandardizedMarketReadRepository reader = new StandardizedMarketReadRepository(context.orderRepository,
            context.tradeRecordRepository, context.accountInventoryResolver);
        final Map<String, StandardizedMarketReadRepository.ProductQuote> quotes =
            reader.readPage(products, playerRef, Instant.now());

        List<StandardizedMarketCatalogEntry> filtered = new ArrayList<StandardizedMarketCatalogEntry>();
        for (StandardizedMarketCatalogEntry entry : entries) {
            StandardizedMarketReadRepository.ProductQuote quote = quotes.get(entry.getProduct().getProductKey());
            if ("TRADED".equals(filter) && (quote == null || quote.tradeCount24h <= 0L)) { continue; }
            if ("BOOK".equals(filter) && (quote == null
                || quote.bestBidPrice <= 0L && quote.bestAskPrice <= 0L)) { continue; }
            filtered.add(entry);
        }
        if (!"DIRECTORY".equals(sort)) {
            final String requestedSort = sort;
            java.util.Collections.sort(filtered, new java.util.Comparator<StandardizedMarketCatalogEntry>() {
                @Override
                public int compare(StandardizedMarketCatalogEntry left, StandardizedMarketCatalogEntry right) {
                    StandardizedMarketReadRepository.ProductQuote leftQuote = quotes.get(left.getProduct().getProductKey());
                    StandardizedMarketReadRepository.ProductQuote rightQuote = quotes.get(right.getProduct().getProductKey());
                    int compared;
                    if ("GAIN".equals(requestedSort) || "LOSS".equals(requestedSort)) {
                        compared = Double.compare(dayChangePercent(leftQuote), dayChangePercent(rightQuote));
                        if ("GAIN".equals(requestedSort)) { compared = -compared; }
                    } else {
                        long leftValue = "VOLUME".equals(requestedSort) ? quoteVolume(leftQuote) : quotePrice(leftQuote);
                        long rightValue = "VOLUME".equals(requestedSort) ? quoteVolume(rightQuote) : quotePrice(rightQuote);
                        compared = Long.compare(rightValue, leftValue);
                    }
                    return compared != 0 ? compared
                        : left.getProduct().getProductKey().compareTo(right.getProduct().getProductKey());
                }
            });
        }

        int total = filtered.size();
        int maxPage = total == 0 ? 0 : (total - 1) / PRODUCT_LIMIT;
        int page = Math.min(Math.max(0, controller.getBrowserPage()), maxPage);
        int start = Math.min(total, page * PRODUCT_LIMIT);
        int end = Math.min(total, start + PRODUCT_LIMIT);
        return new StandardizedMarketCatalogPage(query, page, PRODUCT_LIMIT, total,
            filtered.subList(start, end));
    }

    private String normalizeBrowseOption(String value, String fallback) {
        String normalized = value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.isEmpty() ? fallback : normalized;
    }

    private long quotePrice(StandardizedMarketReadRepository.ProductQuote quote) {
        return quote == null ? 0L : quote.latestPrice;
    }

    private long quoteVolume(StandardizedMarketReadRepository.ProductQuote quote) {
        return quote == null ? 0L : quote.volume24h;
    }

    private double dayChangePercent(StandardizedMarketReadRepository.ProductQuote quote) {
        if (quote == null || quote.dayOpenPrice <= 0L || quote.latestPrice <= 0L) { return 0.0D; }
        return (quote.latestPrice - quote.dayOpenPrice) * 100.0D / quote.dayOpenPrice;
    }

    private String formatDayChange(StandardizedMarketReadRepository.ProductQuote quote) {
        if (quote == null || quote.dayOpenPrice <= 0L || quote.latestPrice <= 0L) { return "--"; }
        double percent = (quote.latestPrice - quote.dayOpenPrice) * 100.0D / quote.dayOpenPrice;
        return String.format(java.util.Locale.ROOT, "%+.1f%%", Double.valueOf(percent));
    }

    private void appendIntradayPricePoints(List<TerminalMarketSnapshot.MarketPricePoint> target,
        List<MarketTradeRecord> trades, Instant now) {
        if (target == null || trades == null || trades.isEmpty()) { return; }
        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Shanghai");
        Instant dayStart = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant();
        List<MarketTradeRecord> today = new ArrayList<MarketTradeRecord>();
        for (MarketTradeRecord trade : trades) {
            if (trade != null && trade.getCreatedAt() != null && !trade.getCreatedAt().isBefore(dayStart)) {
                today.add(trade);
            }
        }
        if (today.isEmpty()) { return; }
        Collections.sort(today, new java.util.Comparator<MarketTradeRecord>() {
            @Override public int compare(MarketTradeRecord left, MarketTradeRecord right) {
                return left.getCreatedAt().compareTo(right.getCreatedAt());
            }
        });
        int count = Math.min(12, today.size());
        for (int index = 0; index < count; index++) {
            int sourceIndex = count == 1 ? 0 : index * (today.size() - 1) / (count - 1);
            MarketTradeRecord trade = today.get(sourceIndex);
            target.add(new TerminalMarketSnapshot.MarketPricePoint(trade.getUnitPrice(), trade.getQuantity(),
                trade.getCreatedAt().getEpochSecond()));
        }
    }

    private StandardizedMarketReadRepository.ChartRange parseChartRange(String value) {
        if ("1h".equalsIgnoreCase(value)) { return StandardizedMarketReadRepository.ChartRange.ONE_HOUR; }
        if ("7d".equalsIgnoreCase(value)) { return StandardizedMarketReadRepository.ChartRange.WEEK; }
        return StandardizedMarketReadRepository.ChartRange.DAY;
    }

    static TerminalMarketSnapshot attachCatalogBrowserData(TerminalMarketSnapshot snapshot,
        StandardizedMarketCatalogPage catalogPage,
        Map<String, TerminalMarketSnapshot.CatalogMarketSummary> catalogMarketSummaries) {
        if (snapshot == null) {
            return null;
        }
        return snapshot.withCatalogPage(catalogPage).withCatalogMarketSummaries(catalogMarketSummaries);
    }

    private String buildCatalogBrowserHint(StandardizedMarketCatalogPage page) {
        List<StandardizedMarketCatalogEntry> entries = page == null
            ? java.util.Collections.<StandardizedMarketCatalogEntry>emptyList() : page.getEntries();
        if (entries.isEmpty()) {
            return page != null && page.getTotalEntries() > 0
                ? "当前搜索没有命中目录商品。"
                : "正式商品目录当前为空；请由管理员启用目录商品。";
        }
        return "正式目录 " + page.getTotalEntries() + " 项，第 " + (page.getPageIndex() + 1) + " 页。";
    }

    private List<String> productKeys(List<StandardizedMarketCatalogEntry> entries) {
        List<String> keys = new ArrayList<String>();
        if (entries != null) {
            for (StandardizedMarketCatalogEntry entry : entries) {
                if (entry != null && entry.getProduct() != null) {
                    keys.add(entry.getProduct().getProductKey());
                }
            }
        }
        return keys;
    }

    private String[] buildProductLabels(List<StandardizedMarketCatalogEntry> entries) {
        List<String> labels = new ArrayList<String>();
        if (entries != null) {
            for (StandardizedMarketCatalogEntry entry : entries) {
                if (entry == null || entry.getProduct() == null) {
                    continue;
                }
                String displayName = entry.getDisplayName();
                labels.add((displayName == null || displayName.trim().isEmpty() ? entry.getProduct().getProductKey()
                    : displayName.trim()) + " | " + entry.getProduct().getProductKey());
            }
        }
        return toSizedArray(labels, PRODUCT_LIMIT);
    }

    /**
     * Compatibility rendering for an unavailable market runtime. Formal catalog admission is
     * performed by {@link StandardizedSpotMarketService#browseCatalog(String, int, int)};
     * this method only turns pre-existing snapshot keys into readable fallback labels.
     */
    private String[] buildProductLabels(List<String> productKeys, StandardizedSpotMarketService spotMarketService) {
        List<String> labels = new ArrayList<String>(productKeys.size());
        for (String productKey : productKeys) {
            try {
                StandardizedMarketProduct product = requireTradableProduct(spotMarketService, productKey);
                labels.add(resolveProductDisplayName(spotMarketService, product) + " | " + productKey);
            } catch (RuntimeException exception) {
                labels.add(productKey);
            }
        }
        return toSizedArray(labels, PRODUCT_LIMIT);
    }

    private String formatLatestTradePrice(List<MarketTradeRecord> recentTrades) {
        if (recentTrades == null || recentTrades.isEmpty()) {
            return "暂无成交";
        }
        return formatAmount(recentTrades.get(0).getUnitPrice()) + " STARCOIN";
    }

    private String[] buildBookLines(List<MarketOrder> orders, boolean askSide) {
        List<String> lines = new ArrayList<String>(BOOK_DEPTH);
        if (orders != null) {
            for (MarketOrder order : orders) {
                lines.add((askSide ? "卖" : "买") + "价 " + formatAmount(order.getUnitPrice()) + " | 剩余 "
                    + formatAmount(order.getOpenQuantity()) + " | orderId=" + order.getOrderId());
                if (lines.size() >= BOOK_DEPTH) {
                    break;
                }
            }
        }
        if (lines.isEmpty()) {
            lines.add(askSide ? "当前没有卖盘。" : "当前没有买盘。");
        }
        return toSizedArray(lines, BOOK_DEPTH);
    }

    private String[] buildBookPrices(List<MarketOrder> orders) {
        List<String> values = new ArrayList<String>(BOOK_DEPTH);
        if (orders != null) {
            for (MarketOrder order : orders) {
                values.add(String.valueOf(order.getUnitPrice()));
                if (values.size() >= BOOK_DEPTH) {
                    break;
                }
            }
        }
        return toSizedArray(values, BOOK_DEPTH);
    }

    private String buildLimitBuyPreview(TerminalMarketSnapshotRequest controller, String selectedProductKey) {
        long quantity = controller.parseLimitBuyQuantity();
        long unitPrice = controller.parseLimitBuyPrice();
        if (selectedProductKey == null || selectedProductKey.isEmpty()) {
            return "先选择商品。";
        }
        if (quantity <= 0L || unitPrice <= 0L) {
            return "填写价格与数量后，将显示冻结资金摘要。";
        }
        long gross = unitPrice * quantity;
        long fee = calculateFee(gross, TAKER_FEE_BASIS_POINTS);
        return "最多预留 " + formatAmount(gross + fee) + " STARCOIN = 委托本金 " + formatAmount(gross)
            + " + 成交手续费上限 " + formatAmount(fee)
            + "。未成交不收费，剩余预留在撤单或完成后释放。";
    }

    private String buildLimitSellPreview(TerminalMarketSnapshotRequest controller, String selectedProductKey,
        long availableQuantity) {
        long quantity = controller.parseLimitSellQuantity();
        long unitPrice = controller.parseLimitSellPrice();
        if (selectedProductKey == null || selectedProductKey.isEmpty()) {
            return "先选择商品。";
        }
        if (quantity <= 0L || unitPrice <= 0L) {
            return "填写价格与数量后，将显示账户仓卖出摘要。";
        }
        if (quantity > availableQuantity) {
            return "账户仓可售数量不足，当前最多只能提交 " + formatAmount(availableQuantity) + "。";
        }
        long gross = unitPrice * quantity;
        long makerFee = calculateFee(gross, MAKER_FEE_BASIS_POINTS);
        return "将锁定账户仓数量 " + formatAmount(quantity) + "，若挂入簿内，预估成交后净到账约 "
            + formatAmount(gross - makerFee) + " STARCOIN。";
    }

    private String buildInstantPreview(DepthQuote quote, boolean buySide) {
        if (quote.requestedQuantity <= 0L) {
            return buySide ? "填写数量后，将按当前卖盘测深。" : "填写数量后，将按当前买盘测深。";
        }
        if (quote.matchedQuantity <= 0L) {
            return buySide ? "当前没有足够卖盘。" : "当前没有足够买盘。";
        }
        if (!quote.canFullyFill()) {
            return "当前最多可成交 " + formatAmount(quote.availableQuantity) + "，不足以完成本次即时交易。";
        }
        if (buySide) {
            return "预计按当前卖盘成交 " + formatAmount(quote.matchedQuantity) + "，本金约 "
                + formatAmount(quote.grossAmount) + " + taker 费 " + formatAmount(quote.feeAmount)
                + " = 冻结/结算总额约 " + formatAmount(quote.totalWithFee)
                + " STARCOIN，最高吃到价格 " + formatAmount(quote.extremeUnitPrice) + "。";
        }
        return "预计按当前买盘成交 " + formatAmount(quote.matchedQuantity) + "，成交额约 "
            + formatAmount(quote.grossAmount) + " - taker 费 " + formatAmount(quote.feeAmount)
            + " = 净到账约 " + formatAmount(quote.netAfterFee)
            + " STARCOIN，最低吃到价格 " + formatAmount(quote.extremeUnitPrice) + "。";
    }

    private String buildSourceMode(String selectedProductKey, StandardizedSpotMarketService spotMarketService) {
        if (selectedProductKey == null || selectedProductKey.isEmpty()) {
            return "当前未选中商品。";
        }
        StandardizedMarketAdmissionDecision decision = inspectRuntimeCatalogProduct(spotMarketService, selectedProductKey);
        return "目录版本=" + decision.getCatalogVersion().getVersionKey() + " | 来源=" + decision.getSourceKey()
            + " | 卖出来源=个人账户仓";
    }

    private String buildSourceAvailable(long availableQuantity) {
        return formatAmount(availableQuantity);
    }

    private String buildWarehouseNotice(String selectedProductKey, long availableQuantity) {
        if (selectedProductKey == null || selectedProductKey.isEmpty()) {
            return "请选择商品后查看账户仓、卖单锁定和待收货状态。";
        }
        if (availableQuantity > 0L) {
            return "当前商品可售=" + formatAmount(availableQuantity) + "，可直接挂卖单或即时卖出。";
        }
        return "当前可卖数量为 0；卖出时将由服务端从个人账户仓校验并预留库存。";
    }

    private String[] buildMyOrderLines(List<MarketOrder> orders, boolean includeEmptyState,
        StandardizedSpotMarketService marketService) {
        List<String> lines = new ArrayList<String>(ORDER_LIMIT);
        if (orders != null) {
            for (MarketOrder order : orders) {
                lines.add(
                    "#" + order.getOrderId() + " | " + order.getProduct().getProductKey() + " | " + order.getSide()
                        + " | 价 " + formatAmount(order.getUnitPrice())
                        + " | 总 " + formatAmount(order.getOriginalQuantity()) + " | 成 " + formatAmount(order.getFilledQuantity())
                        + " | 剩 " + formatAmount(order.getOpenQuantity()) + " | " + order.getStatus() + " | "
                        + formatInstant(order.getCreatedAt()) + " | "
                        + resolveProductDisplayName(marketService, order.getProduct()) + " | "
                        + (order.getUpdatedAt() == null ? 0L : order.getUpdatedAt().getEpochSecond()) + " | "
                        + order.getProduct().getRegistryName() + " | " + order.getProduct().getMeta() + " | 预留 "
                        + formatAmount(order.getReservedFunds()));
                if (lines.size() >= ORDER_LIMIT) {
                    break;
                }
            }
        }
        if (lines.isEmpty() && includeEmptyState) {
            lines.add("当前账户还没有标准商品订单。");
        }
        return toSizedArray(lines, ORDER_LIMIT);
    }

    static final class OrderHistorySnapshot {
        private final String[] lines;
        private final String[] ids;
        private final String[] cancelableFlags;
        private final int totalEntries;
        private final int pageIndex;
        private final int pageSize;

        private OrderHistorySnapshot(String[] lines, String[] ids, String[] cancelableFlags,
            int totalEntries, int pageIndex, int pageSize) {
            this.lines = lines;
            this.ids = ids;
            this.cancelableFlags = cancelableFlags;
            this.totalEntries = Math.max(0, totalEntries);
            this.pageIndex = Math.max(0, pageIndex);
            this.pageSize = Math.max(1, pageSize);
        }

        static OrderHistorySnapshot empty(int pageIndex, int pageSize) {
            return new OrderHistorySnapshot(new String[0], new String[0], new String[0], 0, pageIndex, pageSize);
        }

        String[] getLines() { return lines; }
        String[] getIds() { return ids; }
        String[] getCancelableFlags() { return cancelableFlags; }
        int getTotalEntries() { return totalEntries; }
        int getPageIndex() { return pageIndex; }
        int getPageSize() { return pageSize; }
    }

    private String[] buildMyOrderIds(List<MarketOrder> orders) {
        List<String> ids = new ArrayList<String>(ORDER_LIMIT);
        if (orders != null) {
            for (MarketOrder order : orders) {
                ids.add(String.valueOf(order.getOrderId()));
                if (ids.size() >= ORDER_LIMIT) {
                    break;
                }
            }
        }
        return toSizedArray(ids, ORDER_LIMIT);
    }

    private String[] buildMyOrderCancelableFlags(List<MarketOrder> orders) {
        List<String> flags = new ArrayList<String>(ORDER_LIMIT);
        if (orders != null) {
            for (MarketOrder order : orders) {
                flags.add(isCancelable(order) ? "1" : "0");
                if (flags.size() >= ORDER_LIMIT) {
                    break;
                }
            }
        }
        return toSizedArray(flags, ORDER_LIMIT);
    }

    private String[] buildClaimLines(List<MarketCustodyInventory> claimables) {
        List<String> lines = new ArrayList<String>(CLAIM_LIMIT);
        if (claimables != null) {
            for (MarketCustodyInventory custody : claimables) {
                lines.add("收货编号 " + custody.getCustodyId() + " | 数量 " + formatAmount(custody.getQuantity())
                    + " | 待存入个人仓");
                if (lines.size() >= CLAIM_LIMIT) {
                    break;
                }
            }
        }
        if (lines.isEmpty()) {
            lines.add("当前商品下没有待收货资产。");
        }
        return toSizedArray(lines, CLAIM_LIMIT);
    }

    private String[] buildClaimIds(List<MarketCustodyInventory> claimables) {
        List<String> ids = new ArrayList<String>(CLAIM_LIMIT);
        if (claimables != null) {
            for (MarketCustodyInventory custody : claimables) {
                ids.add(String.valueOf(custody.getCustodyId()));
                if (ids.size() >= CLAIM_LIMIT) {
                    break;
                }
            }
        }
        return toSizedArray(ids, CLAIM_LIMIT);
    }

    private long sumTradeQuantity(List<MarketTradeRecord> trades) {
        long total = 0L;
        if (trades != null) {
            for (MarketTradeRecord trade : trades) {
                total += trade.getQuantity();
            }
        }
        return total;
    }

    private long sumTradeTurnover(List<MarketTradeRecord> trades) {
        long total = 0L;
        if (trades != null) {
            for (MarketTradeRecord trade : trades) {
                total += trade.getUnitPrice() * trade.getQuantity();
            }
        }
        return total;
    }

    private long sumCustodyQuantity(List<MarketCustodyInventory> custodyInventories) {
        long total = 0L;
        if (custodyInventories != null) {
            for (MarketCustodyInventory custodyInventory : custodyInventories) {
                total += custodyInventory.getQuantity();
            }
        }
        return total;
    }

    private long sumFrozenFunds(List<MarketOrder> orders) {
        long total = 0L;
        if (orders != null) {
            for (MarketOrder order : orders) {
                if (order.getSide() == MarketOrderSide.BUY && isCancelable(order)) {
                    total += order.getReservedFunds();
                }
            }
        }
        return total;
    }

    private boolean isCancelable(MarketOrder order) {
        return order.getStatus() == MarketOrderStatus.OPEN || order.getStatus() == MarketOrderStatus.PARTIALLY_FILLED;
    }

    private String resolveProductDisplayName(StandardizedSpotMarketService spotMarketService,
        StandardizedMarketProduct product) {
        ItemStack stack = resolveDisplayStack(spotMarketService, product.getProductKey());
        if (stack != null) {
            try {
                return stack.getDisplayName();
            } catch (RuntimeException ignored) {
                return product.getProductKey();
            }
        }
        return product.getProductKey();
    }

    private String resolveUnitLabel(StandardizedMarketProduct product) {
        Item item = resolveItem(product);
        if (item == null) {
            return "1 单位";
        }
        int maxStack = new ItemStack(item, 1, product.getMeta()).getMaxStackSize();
        return maxStack > 1 ? "可堆叠单位 / stackable" : "不可堆叠单位 / single";
    }

    private boolean resolveStackability(StandardizedMarketProduct product) {
        Item item = resolveItem(product);
        return item != null && new ItemStack(item, 1, product.getMeta()).getMaxStackSize() > 1;
    }

    private EntityPlayerMP requireServerPlayer(EntityPlayer player) {
        return player instanceof EntityPlayerMP ? (EntityPlayerMP) player : null;
    }

    private void ensurePersonalMarketAccount(EntityPlayerMP player) {
        if (GalaxyBase.proxy == null || GalaxyBase.proxy.getModuleManager() == null) {
            throw new MarketOperationException("银行运行时尚未就绪");
        }
        InstitutionCoreModule core = GalaxyBase.proxy.getModuleManager().findModule(InstitutionCoreModule.class);
        if (core == null || core.getBankingInfrastructure() == null) {
            throw new MarketOperationException("银行运行时尚未就绪");
        }
        PlayerBankAccountProvisioner.ensurePersonalAccount(
            core.getBankingInfrastructure().getBankingApplicationService(),
            player.getUniqueID(), player.getCommandSenderName());
    }

    private String resolvePlayerRef(EntityPlayer player) {
        return player instanceof EntityPlayerMP ? ((EntityPlayerMP) player).getUniqueID().toString() : "client-preview";
    }

    private Item resolveItem(StandardizedMarketProduct product) {
        String registryName = product.getRegistryName();
        int separatorIndex = registryName.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex >= registryName.length() - 1) {
            throw new MarketOperationException("invalid standardized product registry name");
        }
        return GameRegistry.findItem(registryName.substring(0, separatorIndex), registryName.substring(separatorIndex + 1));
    }

    static StandardizedMarketAdmissionDecision inspectRuntimeCatalogProduct(StandardizedSpotMarketService spotMarketService,
        String productKey) {
        if (spotMarketService == null) {
            throw new MarketOperationException("标准商品市场目录运行时未就绪");
        }
        return spotMarketService.inspectCatalogProduct(productKey);
    }

    static StandardizedMarketAdmissionDecision inspectRuntimeCatalogStack(StandardizedSpotMarketService spotMarketService,
        ItemStack stack) {
        if (spotMarketService == null) {
            throw new MarketOperationException("标准商品市场目录运行时未就绪");
        }
        return spotMarketService.inspectCatalogStack(stack);
    }

    BaseVaultService resolveBaseVaultServiceForTerminal() {
        return resolveContext().vaultService;
    }

    StandardizedSpotMarketService resolveSpotMarketServiceForTerminal() {
        return resolveContext().spotMarketService;
    }

    private StandardizedMarketProduct requireTradableProduct(StandardizedSpotMarketService spotMarketService,
        String productKey) {
        return inspectRuntimeCatalogProduct(spotMarketService, productKey).requireProduct();
    }

    private String formatInstant(Instant instant) {
        return instant == null ? "--" : TIME_FORMAT.format(instant);
    }

    private static String formatAmount(long value) {
        return String.format(Locale.ROOT, "%,d", Long.valueOf(value));
    }

    private static String[] toSizedArray(List<String> values, int size) {
        String[] results = new String[size];
        for (int index = 0; index < size; index++) {
            results[index] = index < values.size() ? values.get(index) : "";
        }
        return results;
    }

    private static String[] emptyArray(int size) {
        return new String[size];
    }

    private TerminalActionFeedback unavailableFeedback(String message) {
        return TerminalActionFeedback.of(TerminalNotificationSeverity.ERROR, "市场服务不可用", message, 4200L);
    }

    static TerminalActionFeedback buildInstantBuyResidualCancelFailureFeedback(
        StandardizedSpotMarketService.CreateBuyOrderResult result, RuntimeException cancelException) {
        MarketOrder order = result == null ? null : result.getOrder();
        long filledQuantity = order == null ? 0L : order.getFilledQuantity();
        long openQuantity = order == null ? 0L : order.getOpenQuantity();
        long orderId = order == null ? 0L : order.getOrderId();
        long reservedFunds = order == null ? 0L : order.getReservedFunds();
        String reason = cancelException == null || cancelException.getMessage() == null || cancelException.getMessage().trim().isEmpty()
            ? "请查看服务端日志确认撤回失败原因。"
            : cancelException.getMessage().trim();
        return TerminalActionFeedback.of(
            TerminalNotificationSeverity.WARNING,
            "即时买入部分成交，剩余撤回失败",
            "已真实成交 " + formatAmount(filledQuantity) + "；orderId=" + orderId + " 仍有 "
                + formatAmount(openQuantity) + " 未成交数量未能自动撤回，当前系统中可能仍保留开放买单与 "
                + formatAmount(reservedFunds) + " STARCOIN 冻结资金，请在“我的订单”中继续处理。原因: " + reason,
            5200L);
    }

    static TerminalActionFeedback buildInstantSellResidualCancelFailureFeedback(
        StandardizedSpotMarketService.CreateSellOrderResult result, RuntimeException cancelException) {
        MarketOrder order = result == null ? null : result.getOrder();
        long filledQuantity = order == null ? 0L : order.getFilledQuantity();
        long openQuantity = order == null ? 0L : order.getOpenQuantity();
        long orderId = order == null ? 0L : order.getOrderId();
        String reason = cancelException == null || cancelException.getMessage() == null || cancelException.getMessage().trim().isEmpty()
            ? "请查看服务端日志确认撤回失败原因。"
            : cancelException.getMessage().trim();
        return TerminalActionFeedback.of(
            TerminalNotificationSeverity.WARNING,
            "即时卖出部分成交，剩余撤回失败",
            "已真实成交 " + formatAmount(filledQuantity) + "；orderId=" + orderId + " 仍有 "
                + formatAmount(openQuantity) + " 未成交数量未能自动撤回，当前系统中可能仍保留开放卖单与锁定库存，请在“我的订单”中继续处理。原因: "
                + reason,
            5200L);
    }

    private TerminalActionFeedback errorFeedback(String title, RuntimeException exception) {
        GalaxyBase.LOG.warn("Terminal market action failed: {}", exception.getMessage(), exception);
        return TerminalActionFeedback.of(
            TerminalNotificationSeverity.ERROR,
            title,
            toClientSafeMessage(exception),
            4200L);
    }

    private String toClientSafeMessage(RuntimeException exception) {
        String message = exception == null ? null : exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "市场操作失败，服务端没有返回具体原因。请稍后重试。";
        }
        String normalized = message.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("value too long") || lower.contains("violates") || lower.contains("constraint")) {
            return "市场请求未通过数据约束，操作已安全回滚。请刷新后重试；若仍失败请联系管理员。";
        }
        if (lower.contains("connection refused") || lower.contains("connection is closed")
            || lower.contains("connection timed out") || lower.contains("could not connect")) {
            return "市场数据库暂时无法连接，操作未完成。请稍后重试。";
        }
        if (lower.startsWith("database access failed")) {
            return "市场数据库拒绝了本次操作，事务已回滚。请刷新后重试。";
        }
        return normalized;
    }

    private String newRequestId(String prefix) {
        return MarketRequestIdFactory.newRoot(prefix);
    }

    private ExchangeContext resolveExchangeContext() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null && !server.isDedicatedServer()) {
            return ExchangeContext.unavailable("汇率市场仅在独立服务端启用。");
        }
        if (GalaxyBase.proxy == null) {
            return ExchangeContext.unavailable("GalaxyBase proxy 尚未就绪。");
        }

        ModuleManager moduleManager = GalaxyBase.proxy.getModuleManager();
        if (moduleManager == null) {
            return ExchangeContext.unavailable("模块管理器尚未就绪。");
        }

        InstitutionCoreModule institutionCoreModule = moduleManager.findModule(InstitutionCoreModule.class);
        if (institutionCoreModule == null) {
            return ExchangeContext.unavailable("InstitutionCoreModule 未找到。");
        }
        if (institutionCoreModule.getBankingInfrastructure() == null) {
            return ExchangeContext.unavailable("银行 / 汇率运行时未完成装配，请先确认 institution core 已完成初始化。");
        }
        return new ExchangeContext(
            new TaskCoinExchangeService(
                institutionCoreModule.getBankingInfrastructure(),
                new TaskCoinExchangePlanner(), institutionCoreModule.getBankingSourceServerId(),
                institutionCoreModule.getMarketInfrastructure() == null ? null
                    : institutionCoreModule.getMarketInfrastructure().getOperationLogRepository()),
            institutionCoreModule.getBaseVaultService(),
            null,
            true);
    }

    private MarketContext resolveContext() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null && !server.isDedicatedServer()) {
            return MarketContext.unavailable("市场功能仅在独立服务端启用。");
        }
        if (GalaxyBase.proxy == null) {
            return MarketContext.unavailable("GalaxyBase proxy 尚未就绪。");
        }

        ModuleManager moduleManager = GalaxyBase.proxy.getModuleManager();
        if (moduleManager == null) {
            return MarketContext.unavailable("模块管理器尚未就绪。");
        }

        InstitutionCoreModule institutionCoreModule = moduleManager.findModule(InstitutionCoreModule.class);
        if (institutionCoreModule == null) {
            return MarketContext.unavailable("InstitutionCoreModule 未找到。");
        }

        MarketInfrastructure marketInfrastructure = institutionCoreModule.getMarketInfrastructure();
        StandardizedSpotMarketService spotMarketService = institutionCoreModule.getStandardizedSpotMarketService();
        BaseVaultService vaultService = institutionCoreModule.getBaseVaultService();
        if (marketInfrastructure == null || spotMarketService == null || vaultService == null) {
            return MarketContext.unavailable("市场运行时未完成装配；若服务端日志提示缺少 market_order 等表，需要先补表。");
        }

        return new MarketContext(
            spotMarketService,
            marketInfrastructure.getOrderBookRepository(),
            marketInfrastructure.getCustodyInventoryRepository(),
            marketInfrastructure.getTradeRecordRepository(),
            marketInfrastructure.getOperationLogRepository(),
            vaultService,
            institutionCoreModule.getAccountInventoryResolver(),
            institutionCoreModule.getBankingSourceServerId(),
            null,
            true);
    }

    private CustomContext resolveCustomContext() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null && !server.isDedicatedServer()) {
            return CustomContext.unavailable("定制商品市场仅在独立服务端启用。");
        }
        if (GalaxyBase.proxy == null) {
            return CustomContext.unavailable("GalaxyBase proxy 尚未就绪。");
        }

        ModuleManager moduleManager = GalaxyBase.proxy.getModuleManager();
        if (moduleManager == null) {
            return CustomContext.unavailable("模块管理器尚未就绪。");
        }

        InstitutionCoreModule institutionCoreModule = moduleManager.findModule(InstitutionCoreModule.class);
        if (institutionCoreModule == null) {
            return CustomContext.unavailable("InstitutionCoreModule 未找到。");
        }

        CustomMarketService customMarketService = institutionCoreModule.getCustomMarketService();
        BaseVaultService vaultService = institutionCoreModule.getBaseVaultService();
        if (customMarketService == null || vaultService == null) {
            return CustomContext.unavailable("CustomMarketService 未装配完成。");
        }

        return new CustomContext(
            customMarketService,
            vaultService,
            institutionCoreModule.getBankingSourceServerId(),
            null,
            true);
    }

    static final class DepthQuote {

        final long requestedQuantity;
        final long availableQuantity;
        final long matchedQuantity;
        final long extremeUnitPrice;
        final long grossAmount;
        final long feeAmount;
        final long totalWithFee;
        final long netAfterFee;

        private DepthQuote(long requestedQuantity, long availableQuantity, long matchedQuantity, long extremeUnitPrice,
            long grossAmount, long fee) {
            this.requestedQuantity = requestedQuantity;
            this.availableQuantity = availableQuantity;
            this.matchedQuantity = matchedQuantity;
            this.extremeUnitPrice = extremeUnitPrice;
            this.grossAmount = grossAmount;
            this.feeAmount = fee;
            this.totalWithFee = grossAmount + fee;
            this.netAfterFee = grossAmount - fee;
        }

        boolean canFullyFill() {
            return requestedQuantity > 0L && matchedQuantity >= requestedQuantity;
        }
    }

    private static final class MarketContext {

        private final StandardizedSpotMarketService spotMarketService;
        private final MarketOrderBookRepository orderRepository;
        private final MarketCustodyInventoryRepository custodyRepository;
        private final MarketTradeRecordRepository tradeRecordRepository;
        private final MarketOperationLogRepository operationLogRepository;
        private final BaseVaultService vaultService;
        private final AccountInventoryResolver accountInventoryResolver;
        private final String sourceServerId;
        private final String unavailableMessage;
        private final boolean ready;

        private MarketContext(StandardizedSpotMarketService spotMarketService, MarketOrderBookRepository orderRepository,
            MarketCustodyInventoryRepository custodyRepository, MarketTradeRecordRepository tradeRecordRepository,
            MarketOperationLogRepository operationLogRepository,
            BaseVaultService vaultService, AccountInventoryResolver accountInventoryResolver, String sourceServerId,
            String unavailableMessage, boolean ready) {
            this.spotMarketService = spotMarketService;
            this.orderRepository = orderRepository;
            this.custodyRepository = custodyRepository;
            this.tradeRecordRepository = tradeRecordRepository;
            this.operationLogRepository = operationLogRepository;
            this.vaultService = vaultService;
            this.accountInventoryResolver = accountInventoryResolver;
            this.sourceServerId = sourceServerId;
            this.unavailableMessage = unavailableMessage;
            this.ready = ready;
        }

        private static MarketContext unavailable(String unavailableMessage) {
            return new MarketContext(null, null, null, null, null, null, null, null, unavailableMessage, false);
        }

        private boolean isReady() {
            return ready && vaultService != null && accountInventoryResolver != null;
        }
    }

    private static final class ExchangeContext {

        private final TaskCoinExchangeService exchangeService;
        private final BaseVaultService vaultService;
        private final String unavailableMessage;
        private final boolean ready;

        private ExchangeContext(TaskCoinExchangeService exchangeService, BaseVaultService vaultService,
            String unavailableMessage, boolean ready) {
            this.exchangeService = exchangeService;
            this.vaultService = vaultService;
            this.unavailableMessage = unavailableMessage;
            this.ready = ready;
        }

        // Kept for quote-only callers and compatibility tests. Execution also
        // requires vaultService and checks that at the action boundary.
        private ExchangeContext(TaskCoinExchangeService exchangeService, String unavailableMessage, boolean ready) {
            this(exchangeService, null, unavailableMessage, ready);
        }

        private static ExchangeContext unavailable(String unavailableMessage) {
            return new ExchangeContext(null, null, unavailableMessage, false);
        }

        private boolean isReady() {
            return ready;
        }
    }

    private static final class CustomContext {

        private final CustomMarketService customMarketService;
        private final BaseVaultService vaultService;
        private final String sourceServerId;
        private final String unavailableMessage;
        private final boolean ready;

        private CustomContext(CustomMarketService customMarketService, BaseVaultService vaultService, String sourceServerId,
            String unavailableMessage, boolean ready) {
            this.customMarketService = customMarketService;
            this.vaultService = vaultService;
            this.sourceServerId = sourceServerId;
            this.unavailableMessage = unavailableMessage;
            this.ready = ready;
        }

        private static CustomContext unavailable(String unavailableMessage) {
            return new CustomContext(null, null, null, unavailableMessage, false);
        }

        private boolean isReady() {
            return ready && customMarketService != null && vaultService != null;
        }
    }
}
