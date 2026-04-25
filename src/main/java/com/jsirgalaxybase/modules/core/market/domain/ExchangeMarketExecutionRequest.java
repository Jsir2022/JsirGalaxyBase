package com.jsirgalaxybase.modules.core.market.domain;

public class ExchangeMarketExecutionRequest {

    private final String requestId;
    private final String playerRef;
    private final String sourceServerId;
    private final String operatorRef;
    private final String inputRegistryName;
    private final long inputQuantity;

    public ExchangeMarketExecutionRequest(String requestId, String playerRef, String sourceServerId,
        String operatorRef, String inputRegistryName, long inputQuantity) {
        this.requestId = requestId;
        this.playerRef = playerRef;
        this.sourceServerId = sourceServerId;
        this.operatorRef = operatorRef;
        this.inputRegistryName = inputRegistryName;
        this.inputQuantity = inputQuantity;
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

    public String getOperatorRef() {
        return operatorRef;
    }

    public String getInputRegistryName() {
        return inputRegistryName;
    }

    public long getInputQuantity() {
        return inputQuantity;
    }
}