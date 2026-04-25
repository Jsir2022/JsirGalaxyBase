package com.jsirgalaxybase.modules.core.market.domain;

import java.time.Instant;

public class CustomMarketTradeRecord {

    private final long tradeId;
    private final long listingId;
    private final String sellerPlayerRef;
    private final String buyerPlayerRef;
    private final long settledAmount;
    private final String currencyCode;
    private final CustomMarketDeliveryStatus deliveryStatus;
    private final Instant createdAt;

    public CustomMarketTradeRecord(long tradeId, long listingId, String sellerPlayerRef, String buyerPlayerRef,
        long settledAmount, String currencyCode, CustomMarketDeliveryStatus deliveryStatus, Instant createdAt) {
        this.tradeId = tradeId;
        this.listingId = listingId;
        this.sellerPlayerRef = sellerPlayerRef;
        this.buyerPlayerRef = buyerPlayerRef;
        this.settledAmount = settledAmount;
        this.currencyCode = currencyCode;
        this.deliveryStatus = deliveryStatus;
        this.createdAt = createdAt;
    }

    public long getTradeId() {
        return tradeId;
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

    public long getSettledAmount() {
        return settledAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public CustomMarketDeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public CustomMarketTradeRecord withDeliveryStatus(CustomMarketDeliveryStatus newDeliveryStatus) {
        return new CustomMarketTradeRecord(tradeId, listingId, sellerPlayerRef, buyerPlayerRef, settledAmount,
            currencyCode, newDeliveryStatus, createdAt);
    }
}