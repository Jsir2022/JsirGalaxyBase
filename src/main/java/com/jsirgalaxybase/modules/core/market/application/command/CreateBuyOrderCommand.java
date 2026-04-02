package com.jsirgalaxybase.modules.core.market.application.command;

public class CreateBuyOrderCommand {

    private final String requestId;
    private final String playerRef;
    private final String sourceServerId;
    private final String productKey;
    private final long quantity;
    private final boolean stackable;
    private final long unitPrice;

    public CreateBuyOrderCommand(String requestId, String playerRef, String sourceServerId, String productKey,
        long quantity, boolean stackable, long unitPrice) {
        this.requestId = requestId;
        this.playerRef = playerRef;
        this.sourceServerId = sourceServerId;
        this.productKey = productKey;
        this.quantity = quantity;
        this.stackable = stackable;
        this.unitPrice = unitPrice;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getPlayerRef() {
        return playerRef;
    }

    public String getSourceServerId() {
        return sourceServerId;
    }

    public String getProductKey() {
        return productKey;
    }

    public long getQuantity() {
        return quantity;
    }

    public boolean isStackable() {
        return stackable;
    }

    public long getUnitPrice() {
        return unitPrice;
    }
}