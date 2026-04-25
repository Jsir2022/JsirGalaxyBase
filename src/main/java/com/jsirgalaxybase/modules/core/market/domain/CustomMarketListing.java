package com.jsirgalaxybase.modules.core.market.domain;

import java.time.Instant;

public class CustomMarketListing {

    private final long listingId;
    private final String sellerPlayerRef;
    private final String buyerPlayerRef;
    private final long askingPrice;
    private final String currencyCode;
    private final CustomMarketListingStatus listingStatus;
    private final CustomMarketDeliveryStatus deliveryStatus;
    private final String sourceServerId;
    private final Instant createdAt;
    private final Instant updatedAt;

    public CustomMarketListing(long listingId, String sellerPlayerRef, String buyerPlayerRef, long askingPrice,
        String currencyCode, CustomMarketListingStatus listingStatus, CustomMarketDeliveryStatus deliveryStatus,
        String sourceServerId, Instant createdAt, Instant updatedAt) {
        this.listingId = listingId;
        this.sellerPlayerRef = sellerPlayerRef;
        this.buyerPlayerRef = buyerPlayerRef;
        this.askingPrice = askingPrice;
        this.currencyCode = currencyCode;
        this.listingStatus = listingStatus;
        this.deliveryStatus = deliveryStatus;
        this.sourceServerId = sourceServerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getListingId() {
        return listingId;
    }

    public String getSellerPlayerRef() {
        return sellerPlayerRef;
    }

    public String getBuyerPlayerRef() {
        return buyerPlayerRef;
    }

    public long getAskingPrice() {
        return askingPrice;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public CustomMarketListingStatus getListingStatus() {
        return listingStatus;
    }

    public CustomMarketDeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public String getSourceServerId() {
        return sourceServerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public CustomMarketListing withBuyerAndState(String newBuyerPlayerRef, CustomMarketListingStatus newListingStatus,
        CustomMarketDeliveryStatus newDeliveryStatus, Instant newUpdatedAt) {
        return new CustomMarketListing(listingId, sellerPlayerRef, newBuyerPlayerRef, askingPrice, currencyCode,
            newListingStatus, newDeliveryStatus, sourceServerId, createdAt, newUpdatedAt);
    }

    public CustomMarketListing withState(CustomMarketListingStatus newListingStatus,
        CustomMarketDeliveryStatus newDeliveryStatus, Instant newUpdatedAt) {
        return withBuyerAndState(buyerPlayerRef, newListingStatus, newDeliveryStatus, newUpdatedAt);
    }

    public CustomMarketListing withDeliveryStatus(CustomMarketDeliveryStatus newDeliveryStatus, Instant newUpdatedAt) {
        return new CustomMarketListing(listingId, sellerPlayerRef, buyerPlayerRef, askingPrice, currencyCode,
            listingStatus, newDeliveryStatus, sourceServerId, createdAt, newUpdatedAt);
    }
}