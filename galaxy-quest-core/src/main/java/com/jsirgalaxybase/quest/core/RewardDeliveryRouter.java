package com.jsirgalaxybase.quest.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic exact-type routing; unknown rewards are quarantined instead of silently claimed. */
public final class RewardDeliveryRouter implements RewardDeliveryPort {
    private final Map<String, RewardDeliveryHandler> handlers;

    public RewardDeliveryRouter(List<RewardDeliveryHandler> handlers) {
        if (handlers == null) throw new IllegalArgumentException("handlers must not be null");
        Map<String, RewardDeliveryHandler> indexed = new LinkedHashMap<String, RewardDeliveryHandler>();
        for (RewardDeliveryHandler handler : handlers) {
            if (handler == null || handler.getTypeId() == null || handler.getTypeId().trim().isEmpty()) {
                throw new IllegalArgumentException("reward handler type must not be blank");
            }
            String type = handler.getTypeId().trim();
            if (indexed.put(type, handler) != null) throw new IllegalArgumentException(
                "duplicate reward handler: " + type);
        }
        this.handlers = indexed;
    }

    @Override
    public RewardDeliveryOutcome deliver(RewardDeliveryLease lease) {
        if (lease == null) return RewardDeliveryOutcome.failed("missing reward lease", false);
        String type = lease.getReward().getTypeId();
        RewardDeliveryHandler handler = handlers.get(type);
        if (handler == null) return RewardDeliveryOutcome.failed("unsupported reward type: " + type, false);
        return handler.deliver(lease);
    }
}
