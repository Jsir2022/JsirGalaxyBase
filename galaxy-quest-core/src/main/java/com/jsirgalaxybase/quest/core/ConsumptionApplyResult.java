package com.jsirgalaxybase.quest.core;

import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConsumptionApplyResult {
    private final boolean applied;
    private final String evidence;
    private final List<Long> appliedAmounts;

    private ConsumptionApplyResult(boolean applied, String evidence, List<Long> appliedAmounts) {
        this.applied = applied;
        this.evidence = Objects.requireNonNull(evidence, "evidence");
        this.appliedAmounts = Collections.unmodifiableList(new ArrayList<Long>(
            appliedAmounts == null ? Collections.<Long>emptyList() : appliedAmounts));
    }

    public static ConsumptionApplyResult applied(String evidence) {
        return new ConsumptionApplyResult(true, evidence, Collections.<Long>emptyList());
    }
    public static ConsumptionApplyResult applied(String evidence, List<Long> amounts) {
        if (amounts == null || amounts.isEmpty()) throw new IllegalArgumentException("applied amounts must not be empty");
        boolean any = false;
        for (Long amount : amounts) {
            if (amount == null || amount < 0) throw new IllegalArgumentException("applied amounts must not be negative");
            any |= amount > 0;
        }
        if (!any) throw new IllegalArgumentException("at least one applied amount must be positive");
        return new ConsumptionApplyResult(true, evidence, amounts);
    }
    public static ConsumptionApplyResult insufficient(String reason) {
        return new ConsumptionApplyResult(false, reason, Collections.<Long>emptyList());
    }
    public boolean isApplied() { return applied; }
    public String getEvidence() { return evidence; }
    public List<Long> getAppliedAmounts() { return appliedAmounts; }
}
