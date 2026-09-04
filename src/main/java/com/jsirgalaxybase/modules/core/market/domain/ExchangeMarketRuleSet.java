package com.jsirgalaxybase.modules.core.market.domain;

/**
 * Server-owned, versioned rules for the first formal exchange-market pair.
 *
 * <p>The input cap deliberately mirrors one Base Vault item stack. The current
 * terminal exchange action consumes exactly one selected Vault stack, so a
 * larger quoted quantity would be misleading and could not be settled by that
 * action atomically.</p>
 */
public final class ExchangeMarketRuleSet {

    // coin_exchange_record.exchange_rule_version is VARCHAR(32); keep this
    // stable key comfortably inside that persisted contract.
    public static final String TASK_COIN_RULE_VERSION = "exchange-taskcoin-v1-stack64";
    public static final long TASK_COIN_MAX_INPUT_QUANTITY = 64L;

    private final String ruleVersion;
    private final long maxInputQuantity;

    public ExchangeMarketRuleSet(String ruleVersion, long maxInputQuantity) {
        if (ruleVersion == null || ruleVersion.trim().isEmpty()) {
            throw new IllegalArgumentException("ruleVersion must not be blank");
        }
        if (maxInputQuantity <= 0L) {
            throw new IllegalArgumentException("maxInputQuantity must be positive");
        }
        this.ruleVersion = ruleVersion.trim();
        this.maxInputQuantity = maxInputQuantity;
    }

    public static ExchangeMarketRuleSet taskCoinToStarcoinV1() {
        return new ExchangeMarketRuleSet(TASK_COIN_RULE_VERSION, TASK_COIN_MAX_INPUT_QUANTITY);
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public long getMaxInputQuantity() {
        return maxInputQuantity;
    }

    public boolean permitsInputQuantity(long inputQuantity) {
        return inputQuantity > 0L && inputQuantity <= maxInputQuantity;
    }
}
