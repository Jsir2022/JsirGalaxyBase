package com.jsirgalaxybase.terminal.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.module.ModuleManager;
import com.jsirgalaxybase.modules.core.InstitutionCoreModule;
import com.jsirgalaxybase.modules.core.market.application.CustomMarketService;
import com.jsirgalaxybase.modules.core.market.application.TaskCoinExchangeService;
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketAdmissionDecision;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogFactory;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketProductParser;
import com.jsirgalaxybase.modules.core.market.application.StandardizedSpotMarketService;
import com.jsirgalaxybase.modules.core.market.application.command.CancelCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CancelBuyOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CancelSellOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.ClaimCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.application.command.ClaimMarketAssetCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CreateBuyOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.CreateSellOrderCommand;
import com.jsirgalaxybase.modules.core.market.application.command.DepositMarketInventoryCommand;
import com.jsirgalaxybase.modules.core.market.application.command.PurchaseCustomMarketListingCommand;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketDeliveryStatus;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketListingStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyInventory;
import com.jsirgalaxybase.modules.core.market.domain.MarketCustodyStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrder;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderSide;
import com.jsirgalaxybase.modules.core.market.domain.MarketOrderStatus;
import com.jsirgalaxybase.modules.core.market.domain.MarketTradeRecord;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.domain.ExchangeMarketExecutionResult;
import com.jsirgalaxybase.modules.core.market.infrastructure.MarketInfrastructure;
import com.jsirgalaxybase.modules.core.market.port.MarketCustodyInventoryRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketOrderBookRepository;
import com.jsirgalaxybase.modules.core.market.port.MarketTradeRecordRepository;

import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;

final class TerminalMarketService {

    static final TerminalMarketService INSTANCE = new TerminalMarketService();

    private static final int PRODUCT_LIMIT = 8;
    private static final int BOOK_DEPTH = 6;
    private static final int ORDER_LIMIT = 6;
    private static final int CLAIM_LIMIT = 4;
    private static final int CUSTOM_LISTING_LIMIT = 50;
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

    TerminalCustomMarketSnapshot createCustomSnapshot(EntityPlayer player, int selectedScope, String selectedListingId) {
        CustomContext context = resolveCustomContext();
        if (!context.isReady()) {
            return createUnavailableCustomSnapshot(context.unavailableMessage, selectedScope);
        }

        String playerRef = resolvePlayerRef(player);
        List<CustomMarketService.ListingView> activeViews = context.customMarketService.browseListings(CUSTOM_LISTING_LIMIT);
        List<CustomMarketService.ListingView> sellingViews = context.customMarketService.listSellerActiveListings(
            playerRef, CUSTOM_LISTING_LIMIT);
        List<CustomMarketService.ListingView> pendingViews = mergeCustomViews(
            context.customMarketService.listBuyerPendingClaims(playerRef),
            context.customMarketService.listSellerPendingDeliveries(playerRef),
            CUSTOM_LISTING_LIMIT);
        CustomMarketService.ListingView selectedView = findCustomSelectedView(
            selectedListingId,
            activeViews,
            sellingViews,
            pendingViews);
        return new TerminalCustomMarketSnapshot(
            "定制商品市场在线 / listing-first GUI 已接线",
            buildCustomBrowserHint(selectedScope, activeViews, sellingViews, pendingViews),
            describeCustomScope(selectedScope),
            buildCustomListingLines(activeViews),
            buildCustomListingIds(activeViews),
            buildCustomListingLines(sellingViews),
            buildCustomListingIds(sellingViews),
            buildCustomListingLines(pendingViews),
            buildCustomListingIds(pendingViews),
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
            canClaimCustomListing(playerRef, selectedView) ? "1" : "0");
    }

    TerminalExchangeMarketSnapshot createExchangeSnapshot(EntityPlayer player, String selectedTargetCode) {
        TerminalExchangeQuoteView quoteView = buildExchangeQuoteView(player, resolveExchangeContext());
        boolean selected = EXCHANGE_TARGET_TASK_COIN.equals(selectedTargetCode);
        return new TerminalExchangeMarketSnapshot(
            quoteView.serviceState,
            selected ? "当前处于正式兑换详情层，可刷新报价或确认执行。" : "先选择兑换标的，再进入正式报价详情。",
            new String[] { EXCHANGE_TARGET_TASK_COIN },
            new String[] { "任务书硬币 -> STARCOIN | formal quote" },
            selected ? EXCHANGE_TARGET_TASK_COIN : "",
            selected ? "任务书硬币正式兑换" : "未选择兑换标的",
            selected ? "当前详情页只聚焦 formal quote、ruleVersion、limitStatus 与执行门禁。" : "点击左侧唯一标的后进入详情。",
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
            selected ? quoteView.executionHint : "请选择标的后再刷新报价或确认兑换。",
            selected ? quoteView.executableFlag : "0");
    }

    TerminalMarketSnapshot createSnapshot(EntityPlayer player, TerminalMarketSnapshotRequest controller) {
        ExchangeContext exchangeContext = resolveExchangeContext();
        TerminalExchangeQuoteView exchangeQuoteView = buildExchangeQuoteView(player, exchangeContext);
        MarketContext context = resolveContext();
        HeldMarketItem heldItem = context.isReady()
            ? resolveHeldMarketItem(player, context.spotMarketService)
            : resolveHeldMarketItemFallback(player);
        if (!context.isReady()) {
            return createUnavailableSnapshot(heldItem, context.unavailableMessage, exchangeQuoteView);
        }

        String playerRef = resolvePlayerRef(player);
        Set<String> discoveredProductKeys = new LinkedHashSet<String>();
        if (heldItem != null) {
            discoveredProductKeys.add(heldItem.productKey);
        }
        appendTradableKeys(discoveredProductKeys, context.orderRepository.findActiveProductKeys(PRODUCT_LIMIT),
            context.spotMarketService);
        appendTradableKeys(discoveredProductKeys, context.tradeRecordRepository.findDistinctProductKeys(PRODUCT_LIMIT),
            context.spotMarketService);
        appendTradableKeys(discoveredProductKeys,
            context.orderRepository.findDistinctProductKeysByOwner(playerRef, PRODUCT_LIMIT),
            context.spotMarketService);
        appendTradableKeys(
            discoveredProductKeys,
            context.custodyRepository.findDistinctProductKeysByOwner(playerRef, PRODUCT_LIMIT),
            context.spotMarketService);

        List<String> productKeys = limitKeys(discoveredProductKeys, PRODUCT_LIMIT);
        String selectedProductKey = normalizeSelectedProductKey(controller.getSelectedProductKey(), productKeys, heldItem);
        if (selectedProductKey == null) {
            return createEmptySnapshot(productKeys, heldItem, context, exchangeQuoteView);
        }

        StandardizedMarketProduct product = requireTradableProduct(context.spotMarketService, selectedProductKey);
        List<MarketOrder> asks = context.orderRepository.findOpenSellOrdersByProductKey(selectedProductKey);
        List<MarketOrder> bids = context.orderRepository.findOpenBuyOrdersByProductKey(selectedProductKey);
        List<MarketTradeRecord> recentTrades = context.tradeRecordRepository.findByProductKey(selectedProductKey, BOOK_DEPTH);
        List<MarketTradeRecord> dayTrades = context.tradeRecordRepository.findByProductKeySince(
            selectedProductKey,
            Instant.now().minusSeconds(24L * 60L * 60L),
            64);
        List<MarketOrder> myOrders = context.orderRepository.findOrdersByOwnerAndProductKey(playerRef, selectedProductKey,
            ORDER_LIMIT);
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

        return new TerminalMarketSnapshot(
            "市场服务在线 / 共享 JDBC 运行时已接线",
            buildBrowserHint(productKeys, heldItem),
            toSizedArray(productKeys, PRODUCT_LIMIT),
            buildProductLabels(productKeys, context.spotMarketService),
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
            buildWarehouseNotice(heldItem, selectedProductKey, availableQuantity),
            buildMyOrderLines(myOrders),
            buildMyOrderIds(myOrders),
            buildMyOrderCancelableFlags(myOrders),
            buildClaimLines(claimables),
            buildClaimIds(claimables),
            new String[] {
                "最新成交价不是当前可成交价。",
                "即时买入吃卖盘，即时卖出吃买盘。",
                "卖单只消耗统一仓储 AVAILABLE，手持物品必须先存入。",
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
    }

    TerminalActionFeedback refreshExchangeQuote(EntityPlayer player) {
        TerminalExchangeQuoteView quoteView = buildExchangeQuoteView(player, resolveExchangeContext());
        if (!quoteView.hasFormalQuote()) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "报价刷新失败",
                quoteView.notes,
                3600L);
        }
        TerminalNotificationSeverity severity = "1".equals(quoteView.executableFlag)
            ? TerminalNotificationSeverity.INFO
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

    TerminalActionFeedback submitExchangeHeld(EntityPlayer player) {
        EntityPlayerMP serverPlayer = requireServerPlayer(player);
        if (serverPlayer == null) {
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.ERROR,
                "汇率兑换失败",
                "当前客户端上下文不能直接执行汇率兑换。",
                4200L);
        }

        ExchangeContext context = resolveExchangeContext();
        if (!context.isReady()) {
            return exchangeUnavailableFeedback(context.unavailableMessage);
        }

        try {
            ExchangeMarketExecutionResult result = context.exchangeService.exchangeHeldCoinFormal(serverPlayer)
                .getFormalResult();
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
            CustomMarketService.CancelListingResult result = context.customMarketService.cancelListing(
                new CancelCustomMarketListingCommand(
                    newRequestId("terminal-custom-market-cancel"),
                    serverPlayer.getUniqueID().toString(),
                    context.sourceServerId,
                    listingId));
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
            CustomMarketService.ClaimListingResult result = context.customMarketService.claimPurchasedListing(
                new ClaimCustomMarketListingCommand(
                    newRequestId("terminal-custom-market-claim"),
                    serverPlayer.getUniqueID().toString(),
                    context.sourceServerId,
                    listingId));
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
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "买单已提交",
                "orderId="
                    + result.getOrder().getOrderId()
                    + "，冻结 "
                    + formatAmount(result.getOrder().getReservedFunds())
                    + " STARCOIN"
                    + (result.getTradeRecords().isEmpty() ? "。" : "，已立即撮合 " + result.getTradeRecords().size() + " 笔。"),
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
            StandardizedMarketProduct product = requireTradableProduct(context.spotMarketService, productKey);
            StandardizedSpotMarketService.CreateSellOrderResult result = context.spotMarketService.createSellOrder(
                new CreateSellOrderCommand(
                    newRequestId("terminal-market-sell"),
                    serverPlayer.getUniqueID().toString(),
                    context.sourceServerId,
                    product.getProductKey(),
                    quantity,
                    resolveStackability(product),
                    unitPrice));
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "卖单已提交",
                "orderId="
                    + result.getOrder().getOrderId()
                    + "，锁定 AVAILABLE 数量 "
                    + formatAmount(quantity)
                    + (result.getTradeRecords().isEmpty() ? "。" : "，已立即撮合 " + result.getTradeRecords().size() + " 笔。"),
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
            if (result.getOrder().getOpenQuantity() > 0L) {
                context.spotMarketService.cancelBuyOrder(new CancelBuyOrderCommand(
                    newRequestId("terminal-market-buy-now-cancel"),
                    serverPlayer.getUniqueID().toString(),
                    context.sourceServerId,
                    result.getOrder().getOrderId()));
                return TerminalActionFeedback.of(
                    TerminalNotificationSeverity.WARNING,
                    "即时买入部分成交",
                    "已成交 " + formatAmount(result.getOrder().getFilledQuantity()) + "，剩余未成交部分已自动撤回。",
                    4200L);
            }
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "即时买入完成",
                "已按当前卖盘成交 " + formatAmount(quantity) + "，预计总额 " + formatAmount(quote.totalWithFee) + " STARCOIN。",
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
            StandardizedSpotMarketService.CreateSellOrderResult result = context.spotMarketService.createSellOrder(
                new CreateSellOrderCommand(
                    newRequestId("terminal-market-sell-now"),
                    serverPlayer.getUniqueID().toString(),
                    context.sourceServerId,
                    product.getProductKey(),
                    quantity,
                    resolveStackability(product),
                    quote.extremeUnitPrice));
            if (result.getOrder().getOpenQuantity() > 0L) {
                context.spotMarketService.cancelSellOrder(new CancelSellOrderCommand(
                    newRequestId("terminal-market-sell-now-cancel"),
                    serverPlayer.getUniqueID().toString(),
                    context.sourceServerId,
                    result.getOrder().getOrderId()));
                return TerminalActionFeedback.of(
                    TerminalNotificationSeverity.WARNING,
                    "即时卖出部分成交",
                    "已成交 " + formatAmount(result.getOrder().getFilledQuantity()) + "，剩余未成交部分已转回 AVAILABLE。",
                    4200L);
            }
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "即时卖出完成",
                "已按当前买盘成交 " + formatAmount(quantity) + "，预计净到账 " + formatAmount(quote.netAfterFee) + " STARCOIN。",
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
            HeldMarketItem heldItem = requireHeldDepositItem(requireServerPlayer(player), selectedProductKey,
                context.spotMarketService);
            return heldItem != null && heldItem.snapshot != null && heldItem.snapshot.stackSize > 0;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    TerminalActionFeedback submitDepositSelectedHeld(EntityPlayer player, String selectedProductKey) {
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

        HeldMarketItem heldItem;
        try {
            heldItem = requireHeldDepositItem(serverPlayer, selectedProductKey, context.spotMarketService);
        } catch (RuntimeException exception) {
            return errorFeedback("存入失败", exception);
        }

        applyHeldSellDeduction(serverPlayer, heldItem.snapshot, heldItem.snapshot.stackSize);
        try {
            StandardizedSpotMarketService.DepositInventoryResult result = context.spotMarketService.depositInventory(
                new DepositMarketInventoryCommand(
                    newRequestId("terminal-market-deposit"),
                    serverPlayer.getUniqueID().toString(),
                    context.sourceServerId,
                    heldItem.productKey,
                    heldItem.snapshot.stackSize,
                    heldItem.stackable));
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "已存入仓储",
                "已将 " + formatAmount(heldItem.snapshot.stackSize) + " 单位存入 AVAILABLE，当前可用库存 "
                    + formatAmount(result.getTotalAvailableQuantity()) + "。",
                3600L);
        } catch (RuntimeException exception) {
            restoreHeldItem(serverPlayer, heldItem.snapshot);
            return errorFeedback("存入失败", exception);
        }
    }

    TerminalActionFeedback cancelOrder(EntityPlayer player, long orderId) {
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
            Optional<MarketOrder> order = context.orderRepository.findById(orderId);
            if (!order.isPresent()) {
                throw new MarketOperationException("orderId 对应的订单不存在");
            }
            if (order.get().getSide() == MarketOrderSide.BUY) {
                StandardizedSpotMarketService.CancelBuyOrderResult result = context.spotMarketService.cancelBuyOrder(
                    new CancelBuyOrderCommand(
                        newRequestId("terminal-market-buy-cancel"),
                        serverPlayer.getUniqueID().toString(),
                        context.sourceServerId,
                        orderId));
                return TerminalActionFeedback.of(
                    TerminalNotificationSeverity.SUCCESS,
                    "买单已撤销",
                    "orderId=" + result.getOrder().getOrderId() + "，已释放 " + formatAmount(result.getReleasedFunds())
                        + " STARCOIN。",
                    3600L);
            }

            StandardizedSpotMarketService.CancelSellOrderResult result = context.spotMarketService.cancelSellOrder(
                new CancelSellOrderCommand(
                    newRequestId("terminal-market-sell-cancel"),
                    serverPlayer.getUniqueID().toString(),
                    context.sourceServerId,
                    orderId));
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "卖单已撤销",
                "orderId=" + result.getOrder().getOrderId() + "，剩余数量已转回 AVAILABLE。",
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
            StandardizedSpotMarketService.ClaimMarketAssetResult result = context.spotMarketService.claimMarketAsset(
                new ClaimMarketAssetCommand(
                    newRequestId("terminal-market-claim"),
                    serverPlayer.getUniqueID().toString(),
                    context.sourceServerId,
                    custodyId));
            return TerminalActionFeedback.of(
                TerminalNotificationSeverity.SUCCESS,
                "资产已提取",
                "custodyId=" + result.getCustody().getCustodyId() + "，数量 " + formatAmount(result.getCustody().getQuantity())
                    + " 已发放到玩家背包。",
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

    private TerminalMarketSnapshot createUnavailableSnapshot(HeldMarketItem heldItem, String message,
        TerminalExchangeQuoteView exchangeQuoteView) {
        return new TerminalMarketSnapshot(
            "市场服务不可用",
            message,
            toSizedArray(heldItem == null ? new ArrayList<String>() : Arrays.asList(heldItem.productKey), PRODUCT_LIMIT),
            toSizedArray(heldItem == null ? new ArrayList<String>() : Arrays.asList(heldItem.displayLabel), PRODUCT_LIMIT),
            heldItem == null ? "" : heldItem.productKey,
            heldItem == null ? "未选中商品" : heldItem.displayName,
            heldItem == null ? "标准化单位" : heldItem.unitLabel,
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
            heldItem == null ? "当前没有可存入仓储的手持标准化金属。" : "当前只能读取手持商品，市场运行时离线时不可存入或卖出。",
            "0",
            "0",
            "0",
            "0 STARCOIN",
            "运行时离线，无法确认 AVAILABLE / ESCROW / CLAIMABLE 状态。",
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

    private TerminalMarketSnapshot createEmptySnapshot(List<String> productKeys, HeldMarketItem heldItem,
        MarketContext context, TerminalExchangeQuoteView exchangeQuoteView) {
        String browserHint = productKeys.isEmpty()
            ? (heldItem == null ? "当前没有活跃商品，也没有检测到可存入的手持标准化金属物品。"
                : "当前没有活跃商品，已检测到你的手持标准化金属物品，可先存入后作为首个交易标的。")
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
            "请选择商品后填写限价卖单，卖出只会消耗 AVAILABLE。",
            "请选择商品后填写即时买入数量。",
            "请选择商品后填写即时卖出数量，卖出只会消耗 AVAILABLE。",
            heldItem == null ? "当前未检测到手持标准化金属物品。" : "已检测到手持商品，但尚未选中详情页商品。",
            "0",
            "0",
            "0",
            "0 STARCOIN",
            "当前运行时卖出资金直接记入银行账户，卖单只从 AVAILABLE 扣减。",
            emptyArray(ORDER_LIMIT),
            emptyArray(ORDER_LIMIT),
            emptyArray(ORDER_LIMIT),
            emptyArray(CLAIM_LIMIT),
            emptyArray(CLAIM_LIMIT),
            new String[] {
                "点击商品后才能查看订单簿。",
                "即时成交仍按真实盘口撮合，不按最新成交价直接结算。",
                "当前运行时卖单来源是统一仓储 AVAILABLE，不直接消耗手持物品。",
                "CLAIMABLE 资产可在详情页直接提取。"
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
        ItemStack heldStack = player == null ? null : player.getHeldItem();
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

        EntityPlayerMP serverPlayer = requireServerPlayer(player);
        if (serverPlayer == null) {
            return TerminalExchangeQuoteView.empty(
                "汇率市场不可用",
                heldSummary,
                inputRegistryName,
                "当前客户端上下文不能直接读取正式报价。",
                "当前不能继续执行兑换。");
        }
        if (heldStack == null || heldStack.getItem() == null || heldStack.stackSize <= 0) {
            return TerminalExchangeQuoteView.empty(
                "汇率市场在线 / 等待手持任务书硬币",
                "当前未检测到手持物品",
                "--",
                "请先把任务书硬币拿在手上，再刷新报价。",
                "当前不能继续执行兑换。");
        }

        try {
            return TerminalExchangeQuoteView.fromQuote(
                heldSummary,
                context.exchangeService.previewHeldCoinFormal(serverPlayer).getFormalQuote());
        } catch (RuntimeException exception) {
            return TerminalExchangeQuoteView.empty(
                "汇率市场在线 / 报价失败",
                heldSummary,
                inputRegistryName,
                toClientSafeMessage(exception),
                "当前不能继续执行兑换。");
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
            return "当前未检测到手持物品";
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

    private void appendTradableKeys(Set<String> target, List<String> keys, StandardizedSpotMarketService spotMarketService) {
        if (keys == null) {
            return;
        }
        for (String key : keys) {
            if (key == null || key.trim().isEmpty()) {
                continue;
            }
            try {
                target.add(requireTradableProduct(spotMarketService, key.trim()).getProductKey());
            } catch (RuntimeException ignored) {
                // Ignore non-standardized or non-metal products in terminal browsing.
            }
        }
    }

    private List<String> limitKeys(Set<String> keys, int limit) {
        List<String> result = new ArrayList<String>(Math.min(keys.size(), limit));
        for (String key : keys) {
            result.add(key);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private String normalizeSelectedProductKey(String requestedProductKey, List<String> productKeys, HeldMarketItem heldItem) {
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
            && selectedView.getListing().getListingStatus() == CustomMarketListingStatus.ACTIVE
            && playerRef != null && !playerRef.equals(selectedView.getListing().getSellerPlayerRef());
    }

    private boolean canCancelCustomListing(String playerRef, CustomMarketService.ListingView selectedView) {
        return selectedView != null && selectedView.getListing() != null
            && playerRef != null && playerRef.equals(selectedView.getListing().getSellerPlayerRef())
            && selectedView.getListing().getListingStatus() == CustomMarketListingStatus.ACTIVE;
    }

    private boolean canClaimCustomListing(String playerRef, CustomMarketService.ListingView selectedView) {
        return selectedView != null && selectedView.getListing() != null
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
            "0");
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String buildBrowserHint(List<String> productKeys, HeldMarketItem heldItem) {
        if (productKeys.isEmpty()) {
            return heldItem == null ? "当前没有可浏览商品。" : "已把你手持的标准化金属物品加入浏览区。";
        }
        if (heldItem != null && !productKeys.contains(heldItem.productKey)) {
            return "当前有活跃商品，且检测到你手持的另一个标准化金属物品。";
        }
        return "点击左侧商品后，右侧将显示真实订单簿、交易动作和个人状态。";
    }

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
        return "冻结预计 " + formatAmount(gross + fee) + " STARCOIN = 本金 " + formatAmount(gross) + " + taker 费 " + formatAmount(fee)
            + "。若与卖盘交叉，将立即撮合；否则进入订单簿等待。";
    }

    private String buildLimitSellPreview(TerminalMarketSnapshotRequest controller, String selectedProductKey,
        long availableQuantity) {
        long quantity = controller.parseLimitSellQuantity();
        long unitPrice = controller.parseLimitSellPrice();
        if (selectedProductKey == null || selectedProductKey.isEmpty()) {
            return "先选择商品。";
        }
        if (quantity <= 0L || unitPrice <= 0L) {
            return "填写价格与数量后，将显示 AVAILABLE 仓储卖出摘要。";
        }
        if (quantity > availableQuantity) {
            return "AVAILABLE 数量不足，当前最多只能提交 " + formatAmount(availableQuantity) + "。请先存入。";
        }
        long gross = unitPrice * quantity;
        long makerFee = calculateFee(gross, MAKER_FEE_BASIS_POINTS);
        return "将锁定 AVAILABLE 仓储数量 " + formatAmount(quantity) + "，若挂入簿内，预估成交后净到账约 "
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
            return "预计按当前卖盘成交 " + formatAmount(quote.matchedQuantity) + "，总额约 "
                + formatAmount(quote.totalWithFee) + " STARCOIN，最高吃到价格 " + formatAmount(quote.extremeUnitPrice) + "。";
        }
        return "预计按当前买盘成交 " + formatAmount(quote.matchedQuantity) + "，净到账约 "
            + formatAmount(quote.netAfterFee) + " STARCOIN，最低吃到价格 " + formatAmount(quote.extremeUnitPrice) + "。";
    }

    private String buildSourceMode(String selectedProductKey, StandardizedSpotMarketService spotMarketService) {
        if (selectedProductKey == null || selectedProductKey.isEmpty()) {
            return "当前未选中商品。";
        }
        StandardizedMarketAdmissionDecision decision = inspectRuntimeCatalogProduct(spotMarketService, selectedProductKey);
        return "目录版本=" + decision.getCatalogVersion().getVersionKey() + " | 来源=" + decision.getSourceKey()
            + " | 卖出来源=统一仓储 AVAILABLE";
    }

    private String buildSourceAvailable(long availableQuantity) {
        return formatAmount(availableQuantity);
    }

    private String buildWarehouseNotice(HeldMarketItem heldItem, String selectedProductKey, long availableQuantity) {
        if (selectedProductKey == null || selectedProductKey.isEmpty()) {
            return "请选择商品后查看 AVAILABLE / ESCROW / CLAIMABLE 状态。";
        }
        if (availableQuantity > 0L) {
            return "当前商品 AVAILABLE=" + formatAmount(availableQuantity) + "，可直接挂卖单或即时卖出。";
        }
        if (heldItem != null && selectedProductKey.equals(heldItem.productKey)) {
            return "当前 AVAILABLE 为 0，但检测到匹配手持目录准入商品。请先使用“存入当前手持”。";
        }
        return "当前 AVAILABLE 为 0。若要卖出，请先把对应目录准入商品存入统一仓储。";
    }

    private String[] buildMyOrderLines(List<MarketOrder> orders) {
        List<String> lines = new ArrayList<String>(ORDER_LIMIT);
        if (orders != null) {
            for (MarketOrder order : orders) {
                lines.add(
                    "#" + order.getOrderId() + " | " + order.getSide() + " | 价 " + formatAmount(order.getUnitPrice())
                        + " | 总 " + formatAmount(order.getOriginalQuantity()) + " | 成 " + formatAmount(order.getFilledQuantity())
                        + " | 剩 " + formatAmount(order.getOpenQuantity()) + " | " + order.getStatus() + " | "
                        + formatInstant(order.getCreatedAt()));
                if (lines.size() >= ORDER_LIMIT) {
                    break;
                }
            }
        }
        if (lines.isEmpty()) {
            lines.add("当前商品下没有你的订单。");
        }
        return toSizedArray(lines, ORDER_LIMIT);
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
                lines.add("custodyId=" + custody.getCustodyId() + " | 数量 " + formatAmount(custody.getQuantity())
                    + " | 状态 " + custody.getStatus());
                if (lines.size() >= CLAIM_LIMIT) {
                    break;
                }
            }
        }
        if (lines.isEmpty()) {
            lines.add("当前商品下没有可提取的 CLAIMABLE 资产。");
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

    private HeldMarketItem resolveHeldMarketItem(EntityPlayer player, StandardizedSpotMarketService spotMarketService) {
        if (!(player instanceof EntityPlayerMP)) {
            return null;
        }
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        ItemStack heldStack = serverPlayer.inventory.getCurrentItem();
        if (heldStack == null || heldStack.getItem() == null || heldStack.stackSize <= 0) {
            return null;
        }
        try {
            StandardizedMarketProduct product = inspectRuntimeCatalogStack(spotMarketService, heldStack).requireProduct();
            return new HeldMarketItem(
                product.getProductKey(),
                heldStack.copy(),
                heldStack.getMaxStackSize() > 1,
                heldStack.getDisplayName(),
                resolveUnitLabel(product),
                heldStack.getDisplayName() + " | " + product.getProductKey());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private HeldMarketItem resolveHeldMarketItemFallback(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) {
            return null;
        }
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        ItemStack heldStack = serverPlayer.inventory.getCurrentItem();
        if (heldStack == null || heldStack.getItem() == null || heldStack.stackSize <= 0) {
            return null;
        }
        try {
            StandardizedMarketProduct product = StandardizedMarketCatalogFactory
                .createDefaultCatalog(new StandardizedMarketProductParser())
                .evaluateStack(heldStack)
                .requireProduct();
            return new HeldMarketItem(
                product.getProductKey(),
                heldStack.copy(),
                heldStack.getMaxStackSize() > 1,
                heldStack.getDisplayName(),
                resolveUnitLabel(product),
                heldStack.getDisplayName() + " | " + product.getProductKey());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private HeldMarketItem requireHeldDepositItem(EntityPlayerMP player, String productKey,
        StandardizedSpotMarketService spotMarketService) {
        if (player == null) {
            throw new MarketOperationException("当前上下文没有在线玩家实体，无法执行仓储存入");
        }
        HeldMarketItem heldItem = resolveHeldMarketItem(player, spotMarketService);
        if (heldItem == null) {
            throw new MarketOperationException("请先把选中商品对应的标准化金属物品拿在手上，再执行存入");
        }
        if (!heldItem.productKey.equals(productKey)) {
            throw new MarketOperationException("当前手持物品与选中商品不一致，不能存入该商品仓储");
        }
        return heldItem;
    }

    private void applyHeldSellDeduction(EntityPlayerMP player, ItemStack snapshot, long quantity) {
        ItemStack remaining = snapshot.copy();
        remaining.stackSize = snapshot.stackSize - (int) quantity;
        player.inventory.setInventorySlotContents(player.inventory.currentItem, remaining.stackSize > 0 ? remaining : null);
        player.inventory.markDirty();
        if (player.openContainer != null) {
            player.openContainer.detectAndSendChanges();
        }
    }

    private void restoreHeldItem(EntityPlayerMP player, ItemStack snapshot) {
        player.inventory.setInventorySlotContents(player.inventory.currentItem, snapshot.copy());
        player.inventory.markDirty();
        if (player.openContainer != null) {
            player.openContainer.detectAndSendChanges();
        }
    }

    private EntityPlayerMP requireServerPlayer(EntityPlayer player) {
        return player instanceof EntityPlayerMP ? (EntityPlayerMP) player : null;
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
        return message == null || message.trim().isEmpty() ? "请查看服务端日志确认失败原因。" : message.trim();
    }

    private String newRequestId(String prefix) {
        return prefix + ":" + UUID.randomUUID().toString();
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
                institutionCoreModule.getBankingSourceServerId()),
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
        if (marketInfrastructure == null || spotMarketService == null) {
            return MarketContext.unavailable("市场运行时未完成装配；若服务端日志提示缺少 market_order 等表，需要先补表。");
        }

        return new MarketContext(
            spotMarketService,
            marketInfrastructure.getOrderBookRepository(),
            marketInfrastructure.getCustodyInventoryRepository(),
            marketInfrastructure.getTradeRecordRepository(),
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
        if (customMarketService == null) {
            return CustomContext.unavailable("CustomMarketService 未装配完成。");
        }

        return new CustomContext(
            customMarketService,
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
        final long totalWithFee;
        final long netAfterFee;

        private DepthQuote(long requestedQuantity, long availableQuantity, long matchedQuantity, long extremeUnitPrice,
            long grossAmount, long fee) {
            this.requestedQuantity = requestedQuantity;
            this.availableQuantity = availableQuantity;
            this.matchedQuantity = matchedQuantity;
            this.extremeUnitPrice = extremeUnitPrice;
            this.grossAmount = grossAmount;
            this.totalWithFee = grossAmount + fee;
            this.netAfterFee = grossAmount - fee;
        }

        boolean canFullyFill() {
            return requestedQuantity > 0L && matchedQuantity >= requestedQuantity;
        }
    }

    private static final class HeldMarketItem {

        private final String productKey;
        private final ItemStack snapshot;
        private final boolean stackable;
        private final String displayName;
        private final String unitLabel;
        private final String displayLabel;

        private HeldMarketItem(String productKey, ItemStack snapshot, boolean stackable, String displayName,
            String unitLabel, String displayLabel) {
            this.productKey = productKey;
            this.snapshot = snapshot;
            this.stackable = stackable;
            this.displayName = displayName;
            this.unitLabel = unitLabel;
            this.displayLabel = displayLabel;
        }
    }

    private static final class MarketContext {

        private final StandardizedSpotMarketService spotMarketService;
        private final MarketOrderBookRepository orderRepository;
        private final MarketCustodyInventoryRepository custodyRepository;
        private final MarketTradeRecordRepository tradeRecordRepository;
        private final String sourceServerId;
        private final String unavailableMessage;
        private final boolean ready;

        private MarketContext(StandardizedSpotMarketService spotMarketService, MarketOrderBookRepository orderRepository,
            MarketCustodyInventoryRepository custodyRepository, MarketTradeRecordRepository tradeRecordRepository,
            String sourceServerId, String unavailableMessage, boolean ready) {
            this.spotMarketService = spotMarketService;
            this.orderRepository = orderRepository;
            this.custodyRepository = custodyRepository;
            this.tradeRecordRepository = tradeRecordRepository;
            this.sourceServerId = sourceServerId;
            this.unavailableMessage = unavailableMessage;
            this.ready = ready;
        }

        private static MarketContext unavailable(String unavailableMessage) {
            return new MarketContext(null, null, null, null, null, unavailableMessage, false);
        }

        private boolean isReady() {
            return ready;
        }
    }

    private static final class ExchangeContext {

        private final TaskCoinExchangeService exchangeService;
        private final String unavailableMessage;
        private final boolean ready;

        private ExchangeContext(TaskCoinExchangeService exchangeService, String unavailableMessage, boolean ready) {
            this.exchangeService = exchangeService;
            this.unavailableMessage = unavailableMessage;
            this.ready = ready;
        }

        private static ExchangeContext unavailable(String unavailableMessage) {
            return new ExchangeContext(null, unavailableMessage, false);
        }

        private boolean isReady() {
            return ready;
        }
    }

    private static final class CustomContext {

        private final CustomMarketService customMarketService;
        private final String sourceServerId;
        private final String unavailableMessage;
        private final boolean ready;

        private CustomContext(CustomMarketService customMarketService, String sourceServerId,
            String unavailableMessage, boolean ready) {
            this.customMarketService = customMarketService;
            this.sourceServerId = sourceServerId;
            this.unavailableMessage = unavailableMessage;
            this.ready = ready;
        }

        private static CustomContext unavailable(String unavailableMessage) {
            return new CustomContext(null, null, unavailableMessage, false);
        }

        private boolean isReady() {
            return ready && customMarketService != null;
        }
    }
}
