package com.jsirgalaxybase.modules.warehouse.domain;

public enum AssetActivityType {
    STANDARD_BUY_DEPOSITED,
    STANDARD_SELL_SETTLED,
    CUSTOM_BUY_DELIVERED,
    CUSTOM_SELL_SETTLED,
    ASSET_RETURNED,
    DELIVERY_FAILED,
    RECOVERY_REQUIRED,
    RECOVERY_COMPLETED
}
