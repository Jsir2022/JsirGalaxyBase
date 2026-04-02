package com.jsirgalaxybase.modules.core.market.domain;

import java.time.Instant;

public class MarketCustodyInventory {

    private final long custodyId;
    private final String ownerPlayerRef;
    private final StandardizedMarketProduct product;
    private final boolean stackable;
    private final long quantity;
    private final MarketCustodyStatus status;
    private final long relatedOrderId;
    private final long relatedOperationId;
    private final String sourceServerId;
    private final Instant createdAt;
    private final Instant updatedAt;

    public MarketCustodyInventory(long custodyId, String ownerPlayerRef, StandardizedMarketProduct product,
        boolean stackable, long quantity, MarketCustodyStatus status, long relatedOrderId, long relatedOperationId,
        String sourceServerId, Instant createdAt, Instant updatedAt) {
        this.custodyId = custodyId;
        this.ownerPlayerRef = ownerPlayerRef;
        this.product = product;
        this.stackable = stackable;
        this.quantity = quantity;
        this.status = status;
        this.relatedOrderId = relatedOrderId;
        this.relatedOperationId = relatedOperationId;
        this.sourceServerId = sourceServerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getCustodyId() {
        return custodyId;
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

    public long getQuantity() {
        return quantity;
    }

    public MarketCustodyStatus getStatus() {
        return status;
    }

    public long getRelatedOrderId() {
        return relatedOrderId;
    }

    public long getRelatedOperationId() {
        return relatedOperationId;
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

    public MarketCustodyInventory withState(MarketCustodyStatus newStatus, long newRelatedOrderId,
        Instant newUpdatedAt) {
        return withStateAndLinks(newStatus, quantity, newRelatedOrderId, relatedOperationId, newUpdatedAt);
    }

    public MarketCustodyInventory withStateAndQuantity(MarketCustodyStatus newStatus, long newQuantity,
        long newRelatedOrderId, Instant newUpdatedAt) {
        return withStateAndLinks(newStatus, newQuantity, newRelatedOrderId, relatedOperationId, newUpdatedAt);
    }

    public MarketCustodyInventory withStateAndLinks(MarketCustodyStatus newStatus, long newQuantity,
        long newRelatedOrderId, long newRelatedOperationId, Instant newUpdatedAt) {
        return new MarketCustodyInventory(custodyId, ownerPlayerRef, product, stackable, newQuantity, newStatus,
            newRelatedOrderId, newRelatedOperationId, sourceServerId, createdAt, newUpdatedAt);
    }
}