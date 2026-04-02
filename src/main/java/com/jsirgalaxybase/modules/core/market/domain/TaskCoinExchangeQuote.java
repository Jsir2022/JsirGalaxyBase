package com.jsirgalaxybase.modules.core.market.domain;

public class TaskCoinExchangeQuote {

    private final TaskCoinDescriptor descriptor;
    private final int quantity;
    private final long effectiveExchangeValue;
    private final long contributionValue;
    private final String exchangeRuleVersion;

    public TaskCoinExchangeQuote(TaskCoinDescriptor descriptor, int quantity, long effectiveExchangeValue,
        long contributionValue, String exchangeRuleVersion) {
        this.descriptor = descriptor;
        this.quantity = quantity;
        this.effectiveExchangeValue = effectiveExchangeValue;
        this.contributionValue = contributionValue;
        this.exchangeRuleVersion = exchangeRuleVersion;
    }

    public TaskCoinDescriptor getDescriptor() {
        return descriptor;
    }

    public int getQuantity() {
        return quantity;
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
}