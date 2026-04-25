package com.jsirgalaxybase.terminal.ui;

final class TerminalMarketSnapshot {

    final String serviceState;
    final String browserHint;
    final String[] productKeys;
    final String[] productLabels;
    final String selectedProductKey;
    final String selectedProductName;
    final String selectedProductUnit;
    final String latestTradePrice;
    final String highestBid;
    final String lowestAsk;
    final String bestBidQuantity;
    final String bestAskQuantity;
    final String volume24h;
    final String turnover24h;
    final String summaryNotice;
    final String[] askLines;
    final String[] askPrices;
    final String[] bidLines;
    final String[] bidPrices;
    final String limitBuyPreview;
    final String limitSellPreview;
    final String instantBuyPreview;
    final String instantSellPreview;
    final String sourceMode;
    final String sourceAvailable;
    final String lockedEscrowQuantity;
    final String claimableQuantity;
    final String frozenFunds;
    final String sellerSettlement;
    final String[] myOrderLines;
    final String[] myOrderIds;
    final String[] myOrderCancelableFlags;
    final String[] claimLines;
    final String[] claimIds;
    final String[] ruleLines;
    final String exchangeServiceState;
    final String exchangeHeldSummary;
    final String exchangeInputRegistryName;
    final String exchangePairCode;
    final String exchangeInputAssetCode;
    final String exchangeOutputAssetCode;
    final String exchangeRuleVersion;
    final String exchangeLimitStatus;
    final String exchangeReasonCode;
    final String exchangeNotes;
    final String exchangeInputQuantity;
    final String exchangeNominalFaceValue;
    final String exchangeEffectiveExchangeValue;
    final String exchangeContributionValue;
    final String exchangeDiscountStatus;
    final String exchangeRateDisplay;
    final String exchangeExecutionHint;
    final String exchangeExecutableFlag;

    TerminalMarketSnapshot(String serviceState, String browserHint, String[] productKeys, String[] productLabels,
        String selectedProductKey, String selectedProductName, String selectedProductUnit, String latestTradePrice,
        String highestBid, String lowestAsk, String bestBidQuantity, String bestAskQuantity, String volume24h,
        String turnover24h, String summaryNotice, String[] askLines, String[] askPrices, String[] bidLines,
        String[] bidPrices, String limitBuyPreview, String limitSellPreview, String instantBuyPreview,
        String instantSellPreview, String sourceMode, String sourceAvailable, String lockedEscrowQuantity,
        String claimableQuantity, String frozenFunds, String sellerSettlement, String[] myOrderLines,
        String[] myOrderIds, String[] myOrderCancelableFlags, String[] claimLines, String[] claimIds,
        String[] ruleLines, String exchangeServiceState, String exchangeHeldSummary,
        String exchangeInputRegistryName, String exchangePairCode, String exchangeInputAssetCode,
        String exchangeOutputAssetCode, String exchangeRuleVersion, String exchangeLimitStatus,
        String exchangeReasonCode, String exchangeNotes, String exchangeInputQuantity,
        String exchangeNominalFaceValue, String exchangeEffectiveExchangeValue,
        String exchangeContributionValue, String exchangeDiscountStatus, String exchangeRateDisplay,
        String exchangeExecutionHint, String exchangeExecutableFlag) {
        this.serviceState = serviceState;
        this.browserHint = browserHint;
        this.productKeys = productKeys;
        this.productLabels = productLabels;
        this.selectedProductKey = selectedProductKey;
        this.selectedProductName = selectedProductName;
        this.selectedProductUnit = selectedProductUnit;
        this.latestTradePrice = latestTradePrice;
        this.highestBid = highestBid;
        this.lowestAsk = lowestAsk;
        this.bestBidQuantity = bestBidQuantity;
        this.bestAskQuantity = bestAskQuantity;
        this.volume24h = volume24h;
        this.turnover24h = turnover24h;
        this.summaryNotice = summaryNotice;
        this.askLines = askLines;
        this.askPrices = askPrices;
        this.bidLines = bidLines;
        this.bidPrices = bidPrices;
        this.limitBuyPreview = limitBuyPreview;
        this.limitSellPreview = limitSellPreview;
        this.instantBuyPreview = instantBuyPreview;
        this.instantSellPreview = instantSellPreview;
        this.sourceMode = sourceMode;
        this.sourceAvailable = sourceAvailable;
        this.lockedEscrowQuantity = lockedEscrowQuantity;
        this.claimableQuantity = claimableQuantity;
        this.frozenFunds = frozenFunds;
        this.sellerSettlement = sellerSettlement;
        this.myOrderLines = myOrderLines;
        this.myOrderIds = myOrderIds;
        this.myOrderCancelableFlags = myOrderCancelableFlags;
        this.claimLines = claimLines;
        this.claimIds = claimIds;
        this.ruleLines = ruleLines;
        this.exchangeServiceState = exchangeServiceState;
        this.exchangeHeldSummary = exchangeHeldSummary;
        this.exchangeInputRegistryName = exchangeInputRegistryName;
        this.exchangePairCode = exchangePairCode;
        this.exchangeInputAssetCode = exchangeInputAssetCode;
        this.exchangeOutputAssetCode = exchangeOutputAssetCode;
        this.exchangeRuleVersion = exchangeRuleVersion;
        this.exchangeLimitStatus = exchangeLimitStatus;
        this.exchangeReasonCode = exchangeReasonCode;
        this.exchangeNotes = exchangeNotes;
        this.exchangeInputQuantity = exchangeInputQuantity;
        this.exchangeNominalFaceValue = exchangeNominalFaceValue;
        this.exchangeEffectiveExchangeValue = exchangeEffectiveExchangeValue;
        this.exchangeContributionValue = exchangeContributionValue;
        this.exchangeDiscountStatus = exchangeDiscountStatus;
        this.exchangeRateDisplay = exchangeRateDisplay;
        this.exchangeExecutionHint = exchangeExecutionHint;
        this.exchangeExecutableFlag = exchangeExecutableFlag;
    }
}