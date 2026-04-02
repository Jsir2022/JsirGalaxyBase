package com.jsirgalaxybase.modules.core.market.port;

import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;

public interface MarketClaimDeliveryPort {

    void deliver(String deliveryRequestId, String playerRef, String sourceServerId, StandardizedMarketProduct product,
        boolean stackable, long quantity);
}