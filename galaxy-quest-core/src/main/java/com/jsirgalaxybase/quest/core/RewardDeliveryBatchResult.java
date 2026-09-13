package com.jsirgalaxybase.quest.core;

public final class RewardDeliveryBatchResult {
    private final int leased;
    private final int delivered;
    private final int retryScheduled;
    private final int abandoned;
    private final int staleConfirmations;

    RewardDeliveryBatchResult(int leased, int delivered, int retryScheduled, int abandoned, int staleConfirmations) {
        this.leased = leased;
        this.delivered = delivered;
        this.retryScheduled = retryScheduled;
        this.abandoned = abandoned;
        this.staleConfirmations = staleConfirmations;
    }

    public int getLeased() { return leased; }
    public int getDelivered() { return delivered; }
    public int getRetryScheduled() { return retryScheduled; }
    public int getAbandoned() { return abandoned; }
    public int getStaleConfirmations() { return staleConfirmations; }
}
