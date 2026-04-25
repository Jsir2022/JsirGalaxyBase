package com.jsirgalaxybase.modules.core.market.domain;

public class TaskCoinExchangeQuote {

    private final TaskCoinDescriptor descriptor;
    private final int quantity;
    private final long inputTotalFaceValue;
    private final long effectiveExchangeValue;
    private final long contributionValue;
    private final String exchangeRuleVersion;

    public TaskCoinExchangeQuote(TaskCoinDescriptor descriptor, int quantity, long effectiveExchangeValue,
        long contributionValue, String exchangeRuleVersion) {
        this(descriptor, quantity, descriptor == null ? 0L : descriptor.getFaceValue() * (long) quantity,
            effectiveExchangeValue, contributionValue, exchangeRuleVersion);
    }

    public TaskCoinExchangeQuote(TaskCoinDescriptor descriptor, int quantity, long inputTotalFaceValue,
        long effectiveExchangeValue, long contributionValue, String exchangeRuleVersion) {
        this.descriptor = descriptor;
        this.quantity = quantity;
        this.inputTotalFaceValue = inputTotalFaceValue;
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

    public long getInputQuantity() {
        return quantity;
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

    public String getExchangeRuleVersion() {
        return exchangeRuleVersion;
    }

    public int getDiscountBasisPoints() {
        if (inputTotalFaceValue <= 0L) {
            return 0;
        }
        long retainedBasisPoints = (effectiveExchangeValue * 10000L) / inputTotalFaceValue;
        if (retainedBasisPoints >= 10000L) {
            return 0;
        }
        return (int) (10000L - retainedBasisPoints);
    }
}