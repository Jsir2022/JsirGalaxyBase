package com.jsirgalaxybase.modules.itempolicy.domain;

public final class ItemPolicyDecision {

    private static final ItemPolicyDecision ALLOWED = new ItemPolicyDecision(true, null, "allowed");

    private final boolean allowed;
    private final String ruleId;
    private final String reason;

    private ItemPolicyDecision(boolean allowed, String ruleId, String reason) {
        this.allowed = allowed;
        this.ruleId = ruleId;
        this.reason = reason;
    }

    public static ItemPolicyDecision allowed() { return ALLOWED; }

    public static ItemPolicyDecision denied(String ruleId) {
        return new ItemPolicyDecision(false, ruleId, "item denied by rule " + ruleId);
    }

    public boolean isAllowed() { return allowed; }
    public String getRuleId() { return ruleId; }
    public String getReason() { return reason; }
}
