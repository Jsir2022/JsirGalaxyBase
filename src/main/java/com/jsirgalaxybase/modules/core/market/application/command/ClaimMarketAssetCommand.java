package com.jsirgalaxybase.modules.core.market.application.command;

public class ClaimMarketAssetCommand {

    private final String requestId;
    private final String playerRef;
    private final String sourceServerId;
    private final long custodyId;

    public ClaimMarketAssetCommand(String requestId, String playerRef, String sourceServerId, long custodyId) {
        this.requestId = requestId;
        this.playerRef = playerRef;
        this.sourceServerId = sourceServerId;
        this.custodyId = custodyId;
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

    public long getCustodyId() {
        return custodyId;
    }
}