package com.jsirgalaxybase.quest.core;

public final class RewardDeliveryOutcome {
    private static final RewardDeliveryOutcome DELIVERED = new RewardDeliveryOutcome(true, false, "");
    private final boolean delivered;
    private final boolean retryable;
    private final String error;

    private RewardDeliveryOutcome(boolean delivered, boolean retryable, String error) {
        this.delivered = delivered;
        this.retryable = retryable;
        this.error = error == null ? "" : error;
    }

    public static RewardDeliveryOutcome delivered() { return DELIVERED; }
    public static RewardDeliveryOutcome failed(String error, boolean retryable) {
        return new RewardDeliveryOutcome(false, retryable, error);
    }

    public boolean isDelivered() { return delivered; }
    public boolean isRetryable() { return retryable; }
    public String getError() { return error; }
}
