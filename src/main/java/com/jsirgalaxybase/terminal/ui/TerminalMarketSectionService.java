package com.jsirgalaxybase.terminal.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;

import com.jsirgalaxybase.terminal.TerminalCustomMarketActionPayload;
import com.jsirgalaxybase.terminal.TerminalCustomMarketSectionSnapshot;
import com.jsirgalaxybase.terminal.TerminalExchangeMarketActionPayload;
import com.jsirgalaxybase.terminal.TerminalExchangeMarketSectionSnapshot;
import com.jsirgalaxybase.terminal.TerminalMarketActionPayload;
import com.jsirgalaxybase.terminal.TerminalMarketSectionSnapshot;

public final class TerminalMarketSectionService {

    public static final TerminalMarketSectionService INSTANCE = new TerminalMarketSectionService();

    private TerminalMarketSectionService() {}

    public TerminalMarketSectionSnapshot createSnapshot(EntityPlayer player, TerminalPage selectedPage,
        TerminalMarketActionPayload payload, TerminalActionFeedback actionFeedback) {
        TerminalMarketActionPayload marketPayload = payload == null ? TerminalMarketActionPayload.empty() : payload;
        TerminalMarketSnapshot snapshot = TerminalMarketService.INSTANCE.createSnapshot(player, new SectionMarketSnapshotRequest(marketPayload));
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
            snapshot.volume24h,
            snapshot.turnover24h,
            snapshot.sourceAvailable,
            snapshot.lockedEscrowQuantity,
            snapshot.claimableQuantity,
            snapshot.frozenFunds,
            snapshot.summaryNotice,
            toList(snapshot.askLines),
            toList(snapshot.bidLines),
            toList(snapshot.myOrderLines),
            toList(snapshot.claimLines),
            toList(snapshot.claimIds),
            toList(snapshot.ruleLines),
            new TerminalMarketSectionSnapshot.LimitBuyDraft(
                snapshot.selectedProductKey,
                marketPayload.getPriceText(),
                marketPayload.getQuantityText(),
                !snapshot.selectedProductKey.isEmpty()),
            toSnapshotFeedback(effectiveFeedback));
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
            toList(snapshot.sellingListingLines),
            toList(snapshot.sellingListingIds),
            toList(snapshot.pendingListingLines),
            toList(snapshot.pendingListingIds),
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
            exchangePayload.getSelectedTargetCode());
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

    public TerminalActionFeedback cancelCustomListing(EntityPlayer player, TerminalCustomMarketActionPayload payload) {
        return TerminalMarketService.INSTANCE.cancelCustomListing(player, payload == null ? 0L : payload.parseSelectedListingId());
    }

    public TerminalActionFeedback claimCustomListing(EntityPlayer player, TerminalCustomMarketActionPayload payload) {
        return TerminalMarketService.INSTANCE.claimCustomListing(player, payload == null ? 0L : payload.parseSelectedListingId());
    }

    public TerminalActionFeedback refreshExchangeQuote(EntityPlayer player) {
        return TerminalMarketService.INSTANCE.refreshExchangeQuote(player);
    }

    public TerminalActionFeedback submitExchangeHeld(EntityPlayer player) {
        return TerminalMarketService.INSTANCE.submitExchangeHeld(player);
    }

    private TerminalActionFeedback defaultFeedback(TerminalPage selectedPage, TerminalMarketSnapshot snapshot) {
        if (selectedPage == TerminalPage.MARKET_STANDARDIZED) {
            return TerminalActionFeedback.info("标准商品市场状态", snapshot.summaryNotice, 3200L);
        }
        return TerminalActionFeedback.info("市场总入口摘要", snapshot.browserHint, 3200L);
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
        public long parseInstantBuyQuantity() {
            return 0L;
        }

        @Override
        public long parseInstantSellQuantity() {
            return 0L;
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
            return 0L;
        }

        @Override
        public long parseLimitSellPrice() {
            return 0L;
        }
    }
}
