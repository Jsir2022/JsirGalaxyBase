package com.jsirgalaxybase.modules.core.market.application.command;

public class CancelBuyOrderCommand {

    private final String requestId;
    private final String playerRef;
    private final String sourceServerId;
    private final long orderId;

    public CancelBuyOrderCommand(String requestId, String playerRef, String sourceServerId, long orderId) {
        this.requestId = requestId;
        this.playerRef = playerRef;
        this.sourceServerId = sourceServerId;
        this.orderId = orderId;
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

    public long getOrderId() {
        return orderId;
    }
}