package com.jsirgalaxybase.quest.core;

/** Implementations must use entitlementKey as an idempotency key for external delivery. */
public interface RewardDeliveryPort {
    RewardDeliveryOutcome deliver(RewardDeliveryLease lease);
}
