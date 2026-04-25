package com.jsirgalaxybase.modules.core.market.application.command;

public class CancelCustomMarketListingCommand {

    private final String requestId;
    private final String sellerPlayerRef;
    private final String sourceServerId;
    private final long listingId;

    public CancelCustomMarketListingCommand(String requestId, String sellerPlayerRef, String sourceServerId,
        long listingId) {
        this.requestId = requestId;
        this.sellerPlayerRef = sellerPlayerRef;
        this.sourceServerId = sourceServerId;
        this.listingId = listingId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSellerPlayerRef() {
        return sellerPlayerRef;
    }

    public String getSourceServerId() {
        return sourceServerId;
    }

    public long getListingId() {
        return listingId;
    }
}