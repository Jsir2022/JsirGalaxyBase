package com.jsirgalaxybase.modules.core.market.port;

import com.jsirgalaxybase.modules.core.market.domain.CustomMarketItemSnapshot;

/**
 * Delivers a single custom-market escrow item before its database delivery state is finalized.
 */
public interface CustomMarketDeliveryPort {

    void deliver(String deliveryRequestId, String playerRef, String sourceServerId, CustomMarketItemSnapshot snapshot);
}
