package com.jsirgalaxybase.quest.core;

public final class RewardDeliveryOutcome {
    private static final RewardDeliveryOutcome DELIVERED = new RewardDeliveryOutcome(true, false, false, "");
    private final boolean delivered;
    private final boolean retryable;
    private final boolean deferred;
    private final String error;

    private RewardDeliveryOutcome(boolean delivered, boolean retryable, boolean deferred, String error) {
        this.delivered = delivered;
        this.retryable = retryable;
        this.deferred = deferred;
        this.error = error == null ? "" : error;
    }

    public static RewardDeliveryOutcome delivered() { return DELIVERED; }
    public static RewardDeliveryOutcome failed(String error, boolean retryable) {
        return new RewardDeliveryOutcome(false, retryable, false, error);
    }
    /** A recoverable external condition, such as an offline player, which must never exhaust delivery attempts. */
    public static RewardDeliveryOutcome deferred(String reason) {
        return new RewardDeliveryOutcome(false, true, true, reason);
    }

    public boolean isDelivered() { return delivered; }
    public boolean isRetryable() { return retryable; }
    public boolean isDeferred() { return deferred; }
    public String getError() { return error; }
}
