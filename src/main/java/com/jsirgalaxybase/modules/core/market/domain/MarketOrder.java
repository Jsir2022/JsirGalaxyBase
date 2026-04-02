package com.jsirgalaxybase.modules.core.market.domain;

import java.time.Instant;

public class MarketOrder {

    private final long orderId;
    private final MarketOrderSide side;
    private final MarketOrderStatus status;
    private final String ownerPlayerRef;
    private final StandardizedMarketProduct product;
    private final boolean stackable;
    private final long unitPrice;
    private final long originalQuantity;
    private final long openQuantity;
    private final long filledQuantity;
    private final long reservedFunds;
    private final long custodyId;
    private final String sourceServerId;
    private final Instant createdAt;
    private final Instant updatedAt;

    public MarketOrder(long orderId, MarketOrderSide side, MarketOrderStatus status, String ownerPlayerRef,
        StandardizedMarketProduct product, boolean stackable, long unitPrice, long originalQuantity, long openQuantity,
        long filledQuantity, long reservedFunds, long custodyId, String sourceServerId, Instant createdAt,
        Instant updatedAt) {
        this.orderId = orderId;
        this.side = side;
        this.status = status;
        this.ownerPlayerRef = ownerPlayerRef;
        this.product = product;
        this.stackable = stackable;
        this.unitPrice = unitPrice;
        this.originalQuantity = originalQuantity;
        this.openQuantity = openQuantity;
        this.filledQuantity = filledQuantity;
        this.reservedFunds = reservedFunds;
        this.custodyId = custodyId;
        this.sourceServerId = sourceServerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getOrderId() {
        return orderId;
    }

    public MarketOrderSide getSide() {
        return side;
    }

    public MarketOrderStatus getStatus() {
        return status;
    }

    public String getOwnerPlayerRef() {
        return ownerPlayerRef;
    }

    public StandardizedMarketProduct getProduct() {
        return product;
    }

    public boolean isStackable() {
        return stackable;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public long getOriginalQuantity() {
        return originalQuantity;
    }

    public long getOpenQuantity() {
        return openQuantity;
    }

    public long getFilledQuantity() {
        return filledQuantity;
    }

    public long getReservedFunds() {
        return reservedFunds;
    }

    public long getCustodyId() {
        return custodyId;
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

    public MarketOrder withLifecycle(MarketOrderStatus newStatus, long newOpenQuantity, long newFilledQuantity,
        long newReservedFunds, Instant newUpdatedAt) {
        return new MarketOrder(orderId, side, newStatus, ownerPlayerRef, product, stackable, unitPrice,
            originalQuantity, newOpenQuantity, newFilledQuantity, newReservedFunds, custodyId, sourceServerId,
            createdAt, newUpdatedAt);
    }
}