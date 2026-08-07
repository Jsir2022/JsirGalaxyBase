package com.jsirgalaxybase.terminal.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.terminal.TerminalCustomMarketActionPayload;
import com.jsirgalaxybase.terminal.TerminalCustomMarketSectionSnapshot;
import com.jsirgalaxybase.terminal.TerminalExchangeMarketActionPayload;
import com.jsirgalaxybase.terminal.TerminalExchangeMarketSectionSnapshot;
import com.jsirgalaxybase.terminal.TerminalMarketActionPayload;
import com.jsirgalaxybase.terminal.TerminalMarketSectionSnapshot;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketCatalogEntry;
import com.jsirgalaxybase.modules.core.market.application.StandardizedMarketAdmissionDecision;
import com.jsirgalaxybase.modules.core.vault.application.BaseVaultService;
import com.jsirgalaxybase.modules.core.vault.domain.VaultSlot;

public final class TerminalMarketSectionService {

    public static final TerminalMarketSectionService INSTANCE = new TerminalMarketSectionService();

    private TerminalMarketSectionService() {}

    public TerminalMarketSectionSnapshot createSnapshot(EntityPlayer player, TerminalPage selectedPage,
        TerminalMarketActionPayload payload, TerminalActionFeedback actionFeedback) {
        TerminalMarketActionPayload marketPayload = payload == null ? TerminalMarketActionPayload.empty() : payload;
        TerminalMarketSnapshot snapshot = TerminalMarketService.INSTANCE.createSnapshot(player, new SectionMarketSnapshotRequest(marketPayload));
        TerminalMarketActionPayload draftPayload = applyStandardizedDraftDefaults(marketPayload, snapshot);
        if (!draftPayload.encode().equals(marketPayload.encode())) {
            snapshot = TerminalMarketService.INSTANCE.createSnapshot(player, new SectionMarketSnapshotRequest(draftPayload));
        }
        TerminalActionFeedback effectiveFeedback = actionFeedback == null
            ? defaultFeedback(selectedPage, snapshot)
            : actionFeedback;
        return new TerminalMarketSectionSnapshot(
            selectedPage == null ? TerminalPage.MARKET.getId() : selectedPage.getId(),
            snapshot.serviceState,
            snapshot.browserHint,
            toList(snapshot.productKeys),
            toList(snapshot.productLabels),
            snapshot.selectedProductKey,
            snapshot.selectedProductName,
            snapshot.selectedProductUnit,
            snapshot.latestTradePrice,
            snapshot.highestBid,
            snapshot.lowestAsk,
            snapshot.bestBidQuantity,
            snapshot.bestAskQuantity,
            snapshot.volume24h,
            snapshot.turnover24h,
            snapshot.sourceAvailable,
            snapshot.lockedEscrowQuantity,
            snapshot.claimableQuantity,
            snapshot.frozenFunds,
            snapshot.summaryNotice,
            snapshot.sourceMode,
            snapshot.sellerSettlement,
            snapshot.limitBuyPreview,
            snapshot.limitSellPreview,
            snapshot.instantBuyPreview,
            snapshot.instantSellPreview,
            toList(snapshot.askLines),
            toList(snapshot.bidLines),
            toList(snapshot.myOrderLines),
            toList(snapshot.myOrderIds),
            toList(snapshot.myOrderCancelableFlags),
            toList(snapshot.claimLines),
            toList(snapshot.claimIds),
            toList(snapshot.ruleLines),
            TerminalMarketService.INSTANCE.canDepositSelectedHeld(player, snapshot.selectedProductKey),
            new TerminalMarketSectionSnapshot.LimitBuyDraft(
                snapshot.selectedProductKey,
                draftPayload.getLimitBuyPriceText(),
                draftPayload.getLimitBuyQuantityText(),
                hasSelectedProduct(snapshot) && hasPositiveAmount(draftPayload.getLimitBuyPriceText())
                    && hasPositiveAmount(draftPayload.getLimitBuyQuantityText())),
            new TerminalMarketSectionSnapshot.LimitSellDraft(
                snapshot.selectedProductKey,
                draftPayload.getLimitSellPriceText(),
                draftPayload.getLimitSellQuantityText(),
                hasSelectedProduct(snapshot) && hasAvailableStock(snapshot) && hasPositiveAmount(draftPayload.getLimitSellPriceText())
                    && hasPositiveAmount(draftPayload.getLimitSellQuantityText())),
            new TerminalMarketSectionSnapshot.InstantDraft(
                snapshot.selectedProductKey,
                draftPayload.getInstantBuyQuantityText(),
                hasSelectedProduct(snapshot) && hasAskLiquidity(snapshot) && hasPositiveAmount(draftPayload.getInstantBuyQuantityText())),
            new TerminalMarketSectionSnapshot.InstantDraft(
                snapshot.selectedProductKey,
                draftPayload.getInstantSellQuantityText(),
                hasSelectedProduct(snapshot) && hasAvailableStock(snapshot) && hasBidLiquidity(snapshot)
                    && hasPositiveAmount(draftPayload.getInstantSellQuantityText())),
            toSnapshotFeedback(effectiveFeedback)).withCatalogPage(
                toCatalogProducts(snapshot),
                snapshot.catalogPage.getQuery(),
                snapshot.catalogPage.getPageIndex(),
                snapshot.catalogPage.getPageSize(),
                snapshot.catalogPage.getTotalEntries(),
            snapshot.catalogPage.hasPreviousPage(),
                snapshot.catalogPage.hasNextPage())
            .withVaultAssets(toVaultAssets(player));
    }

    public TerminalActionFeedback submitLimitBuy(EntityPlayer player, TerminalMarketActionPayload payload) {
        TerminalMarketActionPayload marketPayload = payload == null ? TerminalMarketActionPayload.empty() : payload;
        return TerminalMarketService.INSTANCE.submitLimitBuy(
            player,
            marketPayload.getSelectedProductKey(),
            marketPayload.parseQuantity(),
            marketPayload.parsePrice());
    }

    public TerminalActionFeedback claimAsset(EntityPlayer player, TerminalMarketActionPayload payload) {
        TerminalMarketActionPayload marketPayload = payload == null ? TerminalMarketActionPayload.empty() : payload;
        return TerminalMarketService.INSTANCE.claimAsset(player, marketPayload.parseCustodyId());
    }

    public TerminalActionFeedback submitDepositHeld(EntityPlayer player, TerminalMarketActionPayload payload) {
        TerminalMarketActionPayload marketPayload = payload == null ? TerminalMarketActionPayload.empty() : payload;
        return TerminalMarketService.INSTANCE.submitDepositFromVault(player, marketPayload.getSelectedProductKey(),
            marketPayload.parseVaultDepositQuantity());
    }

    public TerminalActionFeedback submitLimitSell(EntityPlayer player, TerminalMarketActionPayload payload) {
        TerminalMarketActionPayload marketPayload = payload == null ? TerminalMarketActionPayload.empty() : payload;
        return TerminalMarketService.INSTANCE.submitLimitSell(
            player,
            marketPayload.getSelectedProductKey(),
            marketPayload.parseLimitSellQuantity(),
            marketPayload.parseLimitSellPrice());
    }

    public TerminalActionFeedback submitInstantBuy(EntityPlayer player, TerminalMarketActionPayload payload) {
        TerminalMarketActionPayload marketPayload = payload == null ? TerminalMarketActionPayload.empty() : payload;
        return TerminalMarketService.INSTANCE.submitInstantBuy(
            player,
            marketPayload.getSelectedProductKey(),
            marketPayload.parseInstantBuyQuantity());
    }

    public TerminalActionFeedback submitInstantSell(EntityPlayer player, TerminalMarketActionPayload payload) {
        TerminalMarketActionPayload marketPayload = payload == null ? TerminalMarketActionPayload.empty() : payload;
        return TerminalMarketService.INSTANCE.submitInstantSell(
            player,
            marketPayload.getSelectedProductKey(),
            marketPayload.parseInstantSellQuantity());
    }

    public TerminalActionFeedback cancelOrder(EntityPlayer player, TerminalMarketActionPayload payload) {
        TerminalMarketActionPayload marketPayload = payload == null ? TerminalMarketActionPayload.empty() : payload;
        return TerminalMarketService.INSTANCE.cancelOrder(player, marketPayload.parseOrderId());
    }

    public TerminalCustomMarketSectionSnapshot createCustomSnapshot(EntityPlayer player,
        TerminalCustomMarketActionPayload payload, TerminalActionFeedback actionFeedback) {
        TerminalCustomMarketActionPayload customPayload = payload == null ? TerminalCustomMarketActionPayload.empty() : payload;
        TerminalCustomMarketSnapshot snapshot = TerminalMarketService.INSTANCE.createCustomSnapshot(
            player,
            toCustomScope(customPayload.getSelectedScope()),
            customPayload.getSelectedListingId());
        TerminalActionFeedback feedback = actionFeedback == null
            ? TerminalActionFeedback.info("定制商品市场状态", snapshot.selectedActionHint, 3200L)
            : actionFeedback;
        return new TerminalCustomMarketSectionSnapshot(
            snapshot.serviceState,
            snapshot.browserHint,
            snapshot.scopeLabel,
            toList(snapshot.activeListingLines),
            toList(snapshot.activeListingIds),
            toList(snapshot.activeListingIconRefs),
            toList(snapshot.sellingListingLines),
            toList(snapshot.sellingListingIds),
            toList(snapshot.sellingListingIconRefs),
            toList(snapshot.pendingListingLines),
            toList(snapshot.pendingListingIds),
            toList(snapshot.pendingListingIconRefs),
            snapshot.selectedListingId,
            snapshot.selectedTitle,
            snapshot.selectedPrice,
            snapshot.selectedStatus,
            snapshot.selectedCounterparty,
            snapshot.selectedItemIdentity,
            snapshot.selectedTradeSummary,
            snapshot.selectedActionHint,
            "1".equals(snapshot.selectedCanBuyFlag),
            "1".equals(snapshot.selectedCanCancelFlag),
            "1".equals(snapshot.selectedCanClaimFlag),
            new TerminalCustomMarketSectionSnapshot.ActionFeedback(
                feedback.getTitle(),
                feedback.getBody(),
                feedback.getSeverity().name()));
    }

    public TerminalExchangeMarketSectionSnapshot createExchangeSnapshot(EntityPlayer player,
        TerminalExchangeMarketActionPayload payload, TerminalActionFeedback actionFeedback) {
        TerminalExchangeMarketActionPayload exchangePayload = payload == null ? TerminalExchangeMarketActionPayload.empty() : payload;
        TerminalExchangeMarketSnapshot snapshot = TerminalMarketService.INSTANCE.createExchangeSnapshot(
            player,
            exchangePayload.getSelectedTargetCode(), exchangePayload.getSelectedVaultSlot());
        TerminalActionFeedback feedback = actionFeedback == null
            ? TerminalActionFeedback.info("汇率市场状态", snapshot.executionHint, 3200L)
            : actionFeedback;
        return new TerminalExchangeMarketSectionSnapshot(
            snapshot.serviceState,
            snapshot.browserHint,
            toList(snapshot.targetCodes),
            toList(snapshot.targetLabels),
            snapshot.selectedTargetCode,
            snapshot.selectedTargetTitle,
            snapshot.selectedTargetSummary,
            snapshot.heldSummary,
            snapshot.inputRegistryName,
            snapshot.pairCode,
            snapshot.inputAssetCode,
            snapshot.outputAssetCode,
            snapshot.ruleVersion,
            snapshot.limitStatus,
            snapshot.reasonCode,
            snapshot.notes,
            snapshot.inputQuantity,
            snapshot.nominalFaceValue,
            snapshot.effectiveExchangeValue,
            snapshot.contributionValue,
            snapshot.discountStatus,
            snapshot.rateDisplay,
            snapshot.executionHint,
            "1".equals(snapshot.executableFlag),
            new TerminalExchangeMarketSectionSnapshot.ActionFeedback(
                feedback.getTitle(),
                feedback.getBody(),
                feedback.getSeverity().name()));
    }

    public TerminalActionFeedback purchaseCustomListing(EntityPlayer player, TerminalCustomMarketActionPayload payload) {
        return TerminalMarketService.INSTANCE.purchaseCustomListing(player, payload == null ? 0L : payload.parseSelectedListingId());
    }

    public TerminalActionFeedback publishCustomListing(EntityPlayer player, TerminalCustomMarketActionPayload payload) {
        return TerminalMarketService.INSTANCE.publishCustomListing(player, payload == null ? 0L : payload.parsePublishPrice(),
            payload == null ? -1 : payload.getSelectedVaultSlot());
    }

    public TerminalActionFeedback cancelCustomListing(EntityPlayer player, TerminalCustomMarketActionPayload payload) {
        return TerminalMarketService.INSTANCE.cancelCustomListing(player, payload == null ? 0L : payload.parseSelectedListingId());
    }

    public TerminalActionFeedback claimCustomListing(EntityPlayer player, TerminalCustomMarketActionPayload payload) {
        return TerminalMarketService.INSTANCE.claimCustomListing(player, payload == null ? 0L : payload.parseSelectedListingId());
    }

    public TerminalActionFeedback refreshExchangeQuote(EntityPlayer player) {
        return TerminalMarketService.INSTANCE.refreshExchangeQuote(player);
    }

    public TerminalActionFeedback submitExchange(EntityPlayer player, TerminalExchangeMarketActionPayload payload) {
        return TerminalMarketService.INSTANCE.submitExchange(player, payload == null ? -1 : payload.getSelectedVaultSlot());
    }

    private TerminalActionFeedback defaultFeedback(TerminalPage selectedPage, TerminalMarketSnapshot snapshot) {
        if (selectedPage == TerminalPage.MARKET_STANDARDIZED) {
            return TerminalActionFeedback.info("标准商品市场状态", snapshot.summaryNotice, 3200L);
        }
        return TerminalActionFeedback.info("市场总入口摘要", snapshot.browserHint, 3200L);
    }

    static TerminalMarketActionPayload applyStandardizedDraftDefaults(TerminalMarketActionPayload payload,
        TerminalMarketSnapshot snapshot) {
        TerminalMarketActionPayload current = payload == null ? TerminalMarketActionPayload.empty() : payload;
        if (!hasSelectedProduct(snapshot)) {
            return current;
        }
        String limitBuyPrice = chooseCurrentOrDefault(current.getLimitBuyPriceText(),
            firstPositiveAmount(snapshot.askPrices),
            firstPositiveAmount(snapshot.bidPrices),
            positiveAmountText(snapshot.latestTradePrice));
        String limitBuyQuantity = chooseCurrentOrDefault(current.getLimitBuyQuantityText(),
            hasPositiveAmount(limitBuyPrice) ? "1" : "");
        String limitSellPrice = chooseCurrentOrDefault(current.getLimitSellPriceText(),
            firstPositiveAmount(snapshot.bidPrices),
            firstPositiveAmount(snapshot.askPrices),
            positiveAmountText(snapshot.latestTradePrice));
        String limitSellQuantity = chooseCurrentOrDefault(current.getLimitSellQuantityText(),
            hasAvailableStock(snapshot) && hasPositiveAmount(limitSellPrice) ? "1" : "");
        String instantBuyQuantity = chooseCurrentOrDefault(current.getInstantBuyQuantityText(),
            hasAskLiquidity(snapshot) ? "1" : "");
        String instantSellQuantity = chooseCurrentOrDefault(current.getInstantSellQuantityText(),
            hasAvailableStock(snapshot) && hasBidLiquidity(snapshot) ? "1" : "");
        return new TerminalMarketActionPayload(
            snapshot.selectedProductKey,
            limitBuyPrice,
            limitBuyQuantity,
            current.getCustodyIdText(),
            current.getOrderIdText(),
            limitSellPrice,
            limitSellQuantity,
            instantBuyQuantity,
            instantSellQuantity,
            current.getBrowserQuery(),
            String.valueOf(current.getBrowserPage()),
            current.getBrowserFilter(),
            current.getVaultDepositQuantityText());
    }

    private static boolean hasSelectedProduct(TerminalMarketSnapshot snapshot) {
        return snapshot != null && snapshot.selectedProductKey != null && !snapshot.selectedProductKey.trim().isEmpty();
    }

    private static boolean hasAvailableStock(TerminalMarketSnapshot snapshot) {
        return snapshot != null && parsePositiveAmount(snapshot.sourceAvailable) > 0L;
    }

    private static boolean hasAskLiquidity(TerminalMarketSnapshot snapshot) {
        return snapshot != null && (parsePositiveAmount(snapshot.bestAskQuantity) > 0L || !firstPositiveAmount(snapshot.askPrices).isEmpty());
    }

    private static boolean hasBidLiquidity(TerminalMarketSnapshot snapshot) {
        return snapshot != null && (parsePositiveAmount(snapshot.bestBidQuantity) > 0L || !firstPositiveAmount(snapshot.bidPrices).isEmpty());
    }

    private static String chooseCurrentOrDefault(String current, String firstCandidate) {
        return chooseCurrentOrDefault(current, firstCandidate, "", "");
    }

    private static String chooseCurrentOrDefault(String current, String firstCandidate, String secondCandidate,
        String thirdCandidate) {
        if (hasPositiveAmount(current)) {
            return current.trim();
        }
        if (hasPositiveAmount(firstCandidate)) {
            return firstCandidate.trim();
        }
        if (hasPositiveAmount(secondCandidate)) {
            return secondCandidate.trim();
        }
        return hasPositiveAmount(thirdCandidate) ? thirdCandidate.trim() : "";
    }

    private static boolean hasPositiveAmount(String value) {
        return parsePositiveAmount(value) > 0L;
    }

    private static String firstPositiveAmount(String[] values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String amount = positiveAmountText(value);
            if (!amount.isEmpty()) {
                return amount;
            }
        }
        return "";
    }

    private static String positiveAmountText(String value) {
        long parsed = parsePositiveAmount(value);
        return parsed > 0L ? String.valueOf(parsed) : "";
    }

    private static long parsePositiveAmount(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        String trimmed = value.trim();
        try {
            long parsed = Long.parseLong(trimmed);
            return Math.max(0L, parsed);
        } catch (NumberFormatException ignored) {
            StringBuilder digits = new StringBuilder();
            for (int index = 0; index < trimmed.length(); index++) {
                char ch = trimmed.charAt(index);
                if (ch >= '0' && ch <= '9') {
                    digits.append(ch);
                } else if (digits.length() > 0) {
                    break;
                }
            }
            if (digits.length() == 0) {
                return 0L;
            }
            try {
                return Long.parseLong(digits.toString());
            } catch (NumberFormatException overflow) {
                return 0L;
            }
        }
    }

    private TerminalMarketSectionSnapshot.ActionFeedback toSnapshotFeedback(TerminalActionFeedback feedback) {
        if (feedback == null) {
            return TerminalMarketSectionSnapshot.ActionFeedback.placeholder();
        }
        return new TerminalMarketSectionSnapshot.ActionFeedback(
            feedback.getTitle(),
            feedback.getBody(),
            feedback.getSeverity().name());
    }

    private List<String> toList(String[] values) {
        if (values == null || values.length == 0) {
            return Collections.emptyList();
        }
        List<String> results = new ArrayList<String>(values.length);
        for (String value : values) {
            results.add(value == null ? "" : value.trim());
        }
        return results;
    }

    private List<TerminalMarketSectionSnapshot.CatalogProduct> toCatalogProducts(TerminalMarketSnapshot snapshot) {
        if (snapshot == null || snapshot.catalogPage == null || snapshot.catalogPage.getEntries().isEmpty()) {
            return Collections.emptyList();
        }
        List<TerminalMarketSectionSnapshot.CatalogProduct> products =
            new ArrayList<TerminalMarketSectionSnapshot.CatalogProduct>();
        for (StandardizedMarketCatalogEntry entry : snapshot.catalogPage.getEntries()) {
            if (entry == null || entry.getProduct() == null) {
                continue;
            }
            String productKey = entry.getProduct().getProductKey();
            String tradability = entry.isEnabled()
                ? (productKey.equals(snapshot.selectedProductKey) ? "已选中" : "可交易")
                : "已停用";
            TerminalMarketSnapshot.CatalogMarketSummary summary = snapshot.catalogMarketSummaries.get(productKey);
            products.add(new TerminalMarketSectionSnapshot.CatalogProduct(
                productKey,
                entry.getProduct().getRegistryName(),
                entry.getProduct().getMeta(),
                entry.getDisplayName(),
                entry.getUnitLabel(),
                entry.getSortOrder(),
                entry.isEnabled(),
                entry.getReferencePrice(),
                tradability,
                toCatalogSummary(summary)));
        }
        return products;
    }

    private TerminalMarketSectionSnapshot.CatalogMarketSummary toCatalogSummary(
        TerminalMarketSnapshot.CatalogMarketSummary summary) {
        if (summary == null) {
            return TerminalMarketSectionSnapshot.CatalogMarketSummary.empty();
        }
        List<TerminalMarketSectionSnapshot.PricePoint> points =
            new ArrayList<TerminalMarketSectionSnapshot.PricePoint>();
        for (TerminalMarketSnapshot.MarketPricePoint point : summary.pricePoints) {
            points.add(new TerminalMarketSectionSnapshot.PricePoint(point.price, point.quantity, point.epochSeconds));
        }
        return new TerminalMarketSectionSnapshot.CatalogMarketSummary(summary.latestTrade, summary.bestBid,
            summary.bestAsk, summary.volume24h, summary.available, summary.escrow, summary.claimable,
            summary.dayChange, points);
    }

    private List<TerminalMarketSectionSnapshot.VaultAsset> toVaultAssets(EntityPlayer player) {
        if (player == null) {
            return Collections.emptyList();
        }
        try {
            BaseVaultService vaultService = TerminalMarketService.INSTANCE.resolveBaseVaultServiceForTerminal();
            if (vaultService == null) {
                return Collections.emptyList();
            }
            List<TerminalMarketSectionSnapshot.VaultAsset> assets =
                new ArrayList<TerminalMarketSectionSnapshot.VaultAsset>();
            for (VaultSlot slot : vaultService.viewPersonalVault(player.getUniqueID().toString()).getSlots()) {
                ItemStack stack = slot == null ? null : slot.getStack();
                if (stack == null || stack.stackSize <= 0 || stack.getItem() == null) {
                    continue;
                }
                StandardizedMarketAdmissionDecision admission = TerminalMarketService.inspectRuntimeCatalogStack(
                    TerminalMarketService.INSTANCE.resolveSpotMarketServiceForTerminal(), stack);
                boolean eligible = admission != null && admission.isAdmitted();
                String productKey = eligible ? admission.requireProduct().getProductKey() : "";
                String reason = eligible ? "可存入标准市场" : (admission == null ? "未准入标准市场目录"
                    : admission.getDetailMessage());
                Object registry = Item.itemRegistry.getNameForObject(stack.getItem());
                assets.add(new TerminalMarketSectionSnapshot.VaultAsset(slot.getSlotIndex(),
                    registry == null ? "" : String.valueOf(registry), stack.getItemDamage(), stack.getDisplayName(),
                    stack.stackSize, productKey, eligible, reason));
            }
            return assets;
        } catch (RuntimeException ignored) {
            return Collections.emptyList();
        }
    }

    private int toCustomScope(String scope) {
        if ("selling".equalsIgnoreCase(scope)) {
            return 1;
        }
        if ("pending".equalsIgnoreCase(scope)) {
            return 2;
        }
        return 0;
    }

    private static final class SectionMarketSnapshotRequest implements TerminalMarketSnapshotRequest {

        private final TerminalMarketActionPayload payload;

        private SectionMarketSnapshotRequest(TerminalMarketActionPayload payload) {
            this.payload = payload == null ? TerminalMarketActionPayload.empty() : payload;
        }

        @Override
        public String getSelectedProductKey() {
            return payload.getSelectedProductKey();
        }

        @Override
        public String getBrowserQuery() { return payload.getBrowserQuery(); }

        @Override
        public int getBrowserPage() { return payload.getBrowserPage(); }

        @Override
        public String getBrowserFilter() { return payload.getBrowserFilter(); }

        @Override
        public String getChartRange() { return payload.getChartRange(); }

        @Override
        public long parseInstantBuyQuantity() {
            return payload.parseInstantBuyQuantity();
        }

        @Override
        public long parseInstantSellQuantity() {
            return payload.parseInstantSellQuantity();
        }

        @Override
        public long parseLimitBuyQuantity() {
            return payload.parseQuantity();
        }

        @Override
        public long parseLimitBuyPrice() {
            return payload.parsePrice();
        }

        @Override
        public long parseLimitSellQuantity() {
            return payload.parseLimitSellQuantity();
        }

        @Override
        public long parseLimitSellPrice() {
            return payload.parseLimitSellPrice();
        }
    }
}
