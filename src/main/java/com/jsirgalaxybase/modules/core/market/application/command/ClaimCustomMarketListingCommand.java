package com.jsirgalaxybase.modules.core.market.application.command;

public class ClaimCustomMarketListingCommand {

    private final String requestId;
    private final String buyerPlayerRef;
    private final String sourceServerId;
    private final long listingId;

    public ClaimCustomMarketListingCommand(String requestId, String buyerPlayerRef, String sourceServerId,
        long listingId) {
        this.requestId = requestId;
        this.buyerPlayerRef = buyerPlayerRef;
        this.sourceServerId = sourceServerId;
        this.listingId = listingId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getBuyerPlayerRef() {
        return buyerPlayerRef;
    }

    public String getSourceServerId() {
        return sourceServerId;
    }

    public long getListingId() {
        return listingId;
    }
}