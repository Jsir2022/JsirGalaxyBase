package com.jsirgalaxybase.modules.core.market.application.command;

public class DepositMarketInventoryCommand {

    private final String requestId;
    private final String playerRef;
    private final String sourceServerId;
    private final String productKey;
    private final long quantity;
    private final boolean stackable;

    public DepositMarketInventoryCommand(String requestId, String playerRef, String sourceServerId, String productKey,
        long quantity, boolean stackable) {
        this.requestId = requestId;
        this.playerRef = playerRef;
        this.sourceServerId = sourceServerId;
        this.productKey = productKey;
        this.quantity = quantity;
        this.stackable = stackable;
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
}