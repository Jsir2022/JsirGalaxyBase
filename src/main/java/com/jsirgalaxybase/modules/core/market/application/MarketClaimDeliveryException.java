package com.jsirgalaxybase.modules.core.market.application;

public class MarketClaimDeliveryException extends MarketOperationException {

    private final boolean safeToRestoreClaimable;

    public MarketClaimDeliveryException(String message, boolean safeToRestoreClaimable) {
        super(message);
        this.safeToRestoreClaimable = safeToRestoreClaimable;
    }

    public boolean isSafeToRestoreClaimable() {
        return safeToRestoreClaimable;
    }
}