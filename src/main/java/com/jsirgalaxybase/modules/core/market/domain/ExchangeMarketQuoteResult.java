package com.jsirgalaxybase.modules.core.market.domain;

public class ExchangeMarketQuoteResult {

    private final ExchangeMarketPairDefinition pairDefinition;
    private final ExchangeMarketRuleVersion ruleVersion;
    private final ExchangeMarketLimitPolicy limitPolicy;
    private final String requestId;
    private final String playerRef;
    private final String sourceServerId;
    private final String inputRegistryName;
    private final String inputFamily;
    private final String inputTier;
    private final long inputUnitFaceValue;
    private final long inputQuantity;
    private final long inputTotalFaceValue;
    private final long effectiveExchangeValue;
    private final long contributionValue;
    private final long exchangeRateNumerator;
    private final long exchangeRateDenominator;
    private final int discountBasisPoints;
    private final String notes;

    public ExchangeMarketQuoteResult(ExchangeMarketPairDefinition pairDefinition,
        ExchangeMarketRuleVersion ruleVersion, ExchangeMarketLimitPolicy limitPolicy, String requestId,
        String playerRef, String sourceServerId, String inputRegistryName, String inputFamily, String inputTier,
        long inputUnitFaceValue, long inputQuantity, long inputTotalFaceValue, long effectiveExchangeValue,
        long contributionValue, long exchangeRateNumerator, long exchangeRateDenominator, int discountBasisPoints,
        String notes) {
        this.pairDefinition = pairDefinition;
        this.ruleVersion = ruleVersion;
        this.limitPolicy = limitPolicy;
        this.requestId = requestId;
        this.playerRef = playerRef;
        this.sourceServerId = sourceServerId;
        this.inputRegistryName = inputRegistryName;
        this.inputFamily = inputFamily;
        this.inputTier = inputTier;
        this.inputUnitFaceValue = inputUnitFaceValue;
        this.inputQuantity = inputQuantity;
        this.inputTotalFaceValue = inputTotalFaceValue;
        this.effectiveExchangeValue = effectiveExchangeValue;
        this.contributionValue = contributionValue;
        this.exchangeRateNumerator = exchangeRateNumerator;
        this.exchangeRateDenominator = exchangeRateDenominator;
        this.discountBasisPoints = discountBasisPoints;
        this.notes = notes;
    }

    public ExchangeMarketPairDefinition getPairDefinition() {
        return pairDefinition;
    }

    public ExchangeMarketRuleVersion getRuleVersion() {
        return ruleVersion;
    }

    public ExchangeMarketLimitPolicy getLimitPolicy() {
        return limitPolicy;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getPlayerRef() {
        return playerRef;
    }

    public String getSourceServerId() {
        return sourceServerId;
    }

    public String getInputRegistryName() {
        return inputRegistryName;
    }

    public String getInputFamily() {
        return inputFamily;
    }

    public String getInputTier() {
        return inputTier;
    }

    public long getInputUnitFaceValue() {
        return inputUnitFaceValue;
    }

    public long getInputQuantity() {
        return inputQuantity;
    }

    public long getInputTotalFaceValue() {
        return inputTotalFaceValue;
    }

    public long getEffectiveExchangeValue() {
        return effectiveExchangeValue;
    }

    public long getContributionValue() {
        return contributionValue;
    }

    public long getExchangeRateNumerator() {
        return exchangeRateNumerator;
    }

    public long getExchangeRateDenominator() {
        return exchangeRateDenominator;
    }

    public int getDiscountBasisPoints() {
        return discountBasisPoints;
    }

    public String getNotes() {
        return notes;
    }
}