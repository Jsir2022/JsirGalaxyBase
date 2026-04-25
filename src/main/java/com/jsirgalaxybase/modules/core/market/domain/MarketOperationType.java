package com.jsirgalaxybase.modules.core.market.domain;

public enum MarketOperationType {
    INVENTORY_DEPOSIT,
    SELL_ORDER_CREATE,
    SELL_ORDER_CANCEL,
    BUY_ORDER_CREATE,
    BUY_ORDER_CANCEL,
    MATCH_EXECUTION,
    CLAIMABLE_ASSET_CLAIM
}