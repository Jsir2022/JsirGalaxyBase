package com.jsirgalaxybase.quest.core;

import java.util.List;

public interface RewardDeliveryRepository {
    List<RewardDeliveryLease> leaseAvailable(String workerId, long now, long leaseUntil, int limit);
    boolean markDelivered(String entitlementKey, String workerId, int attempt, long deliveredAt);
    boolean markDeferred(String entitlementKey,String workerId,int attempt,String reason,long deferredAt,long retryAt);
    boolean markFailed(String entitlementKey, String workerId, int attempt, String error, long failedAt, long retryAt,
        boolean retryable);
}
