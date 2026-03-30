package com.jsirgalaxybase.modules.core.banking.application.command;

public class CoinExchangeSettlementCommand {

    private final String requestId;
    private final long reserveAccountId;
    private final long playerAccountId;
    private final String playerRef;
    private final String sourceServerId;
    private final String operatorRef;
    private final String coinFamily;
    private final String coinTier;
    private final long coinFaceValue;
    private final long coinQuantity;
    private final long effectiveExchangeValue;
    private final long contributionValue;
    private final String exchangeRuleVersion;
    private final String businessRef;
    private final String comment;
    private final String extraJson;

    public CoinExchangeSettlementCommand(String requestId, long reserveAccountId, long playerAccountId,
        String playerRef, String sourceServerId, String operatorRef, String coinFamily, String coinTier,
        long coinFaceValue, long coinQuantity, long effectiveExchangeValue, long contributionValue,
        String exchangeRuleVersion, String businessRef, String comment, String extraJson) {
        this.requestId = requestId;
        this.reserveAccountId = reserveAccountId;
        this.playerAccountId = playerAccountId;
        this.playerRef = playerRef;
        this.sourceServerId = sourceServerId;
        this.operatorRef = operatorRef;
        this.coinFamily = coinFamily;
        this.coinTier = coinTier;
        this.coinFaceValue = coinFaceValue;
        this.coinQuantity = coinQuantity;
        this.effectiveExchangeValue = effectiveExchangeValue;
        this.contributionValue = contributionValue;
        this.exchangeRuleVersion = exchangeRuleVersion;
        this.businessRef = businessRef;
        this.comment = comment;
        this.extraJson = extraJson;
    }

    public String getRequestId() {
        return requestId;
    }

    public long getReserveAccountId() {
        return reserveAccountId;
    }

    public long getPlayerAccountId() {
        return playerAccountId;
    }

    public String getPlayerRef() {
        return playerRef;
    }

    public String getSourceServerId() {
        return sourceServerId;
    }

    public String getOperatorRef() {
        return operatorRef;
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

    public String getExchangeRuleVersion() {
        return exchangeRuleVersion;
    }

    public String getBusinessRef() {
        return businessRef;
    }

    public String getComment() {
        return comment;
    }

    public String getExtraJson() {
        return extraJson;
    }
}