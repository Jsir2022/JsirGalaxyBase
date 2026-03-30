package com.jsirgalaxybase.modules.core.banking.domain;

import java.time.Instant;

public class CoinExchangeRecord {

    private final long exchangeId;
    private final long transactionId;
    private final String playerRef;
    private final String coinFamily;
    private final String coinTier;
    private final long coinFaceValue;
    private final long coinQuantity;
    private final long effectiveExchangeValue;
    private final long contributionValue;
    private final String ruleVersion;
    private final String sourceServerId;
    private final String extraJson;
    private final Instant createdAt;

    public CoinExchangeRecord(long exchangeId, long transactionId, String playerRef, String coinFamily,
        String coinTier, long coinFaceValue, long coinQuantity, long effectiveExchangeValue,
        long contributionValue, String ruleVersion, String sourceServerId, String extraJson, Instant createdAt) {
        this.exchangeId = exchangeId;
        this.transactionId = transactionId;
        this.playerRef = playerRef;
        this.coinFamily = coinFamily;
        this.coinTier = coinTier;
        this.coinFaceValue = coinFaceValue;
        this.coinQuantity = coinQuantity;
        this.effectiveExchangeValue = effectiveExchangeValue;
        this.contributionValue = contributionValue;
        this.ruleVersion = ruleVersion;
        this.sourceServerId = sourceServerId;
        this.extraJson = extraJson;
        this.createdAt = createdAt;
    }

    public long getExchangeId() {
        return exchangeId;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public String getPlayerRef() {
        return playerRef;
    }

    public String getCoinFamily() {
        return coinFamily;
    }

    public String getCoinTier() {
        return coinTier;
    }

    public long getCoinFaceValue() {
        return coinFaceValue;
    }

    public long getCoinQuantity() {
        return coinQuantity;
    }

    public long getEffectiveExchangeValue() {
        return effectiveExchangeValue;
    }

    public long getContributionValue() {
        return contributionValue;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public String getSourceServerId() {
        return sourceServerId;
    }

    public String getExtraJson() {
        return extraJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}