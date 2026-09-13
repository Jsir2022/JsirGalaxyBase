package com.jsirgalaxybase.quest.core;

import java.util.List;

public interface RewardDeliveryRepository {
    List<RewardDeliveryLease> leaseAvailable(String workerId, long now, long leaseUntil, int limit);
    boolean markDelivered(String entitlementKey, String workerId, int attempt, long deliveredAt);
    boolean markFailed(String entitlementKey, String workerId, int attempt, String error, long failedAt, long retryAt,
        boolean retryable);
}
