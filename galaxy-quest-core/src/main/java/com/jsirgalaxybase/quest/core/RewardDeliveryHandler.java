package com.jsirgalaxybase.quest.core;

/** One exact reward-type implementation behind the common recoverable worker. */
public interface RewardDeliveryHandler {
    String getTypeId();
    RewardDeliveryOutcome deliver(RewardDeliveryLease lease);
}
