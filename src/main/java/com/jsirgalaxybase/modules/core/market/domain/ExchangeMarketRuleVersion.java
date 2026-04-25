package com.jsirgalaxybase.modules.core.market.domain;

public class ExchangeMarketRuleVersion {

    private final String ruleKey;
    private final String displayName;

    public ExchangeMarketRuleVersion(String ruleKey, String displayName) {
        this.ruleKey = ruleKey;
        this.displayName = displayName;
    }

    public String getRuleKey() {
        return ruleKey;
    }

    public String getDisplayName() {
        return displayName;
    }
}