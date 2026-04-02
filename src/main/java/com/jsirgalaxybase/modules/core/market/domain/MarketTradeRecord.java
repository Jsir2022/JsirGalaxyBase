package com.jsirgalaxybase.modules.core.market.domain;

import java.time.Instant;

public class MarketTradeRecord {

    private final long tradeId;
    private final String buyerPlayerRef;
    private final String sellerPlayerRef;
    private final StandardizedMarketProduct product;
    private final boolean stackable;
    private final long unitPrice;
    private final long quantity;
    private final long feeAmount;
    private final long buyOrderId;
    private final long sellOrderId;
    private final long operationId;
    private final Instant createdAt;

    public MarketTradeRecord(long tradeId, String buyerPlayerRef, String sellerPlayerRef,
        StandardizedMarketProduct product, boolean stackable, long unitPrice, long quantity, long feeAmount,
        long buyOrderId, long sellOrderId, long operationId, Instant createdAt) {
        this.tradeId = tradeId;
        this.buyerPlayerRef = buyerPlayerRef;
        this.sellerPlayerRef = sellerPlayerRef;
        this.product = product;
        this.stackable = stackable;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.feeAmount = feeAmount;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.operationId = operationId;
        this.createdAt = createdAt;
    }

    public long getTradeId() {
        return tradeId;
    }

    public String getBuyerPlayerRef() {
        return buyerPlayerRef;
    }

    public String getSellerPlayerRef() {
        return sellerPlayerRef;
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

    public long getQuantity() {
        return quantity;
    }

    public long getFeeAmount() {
        return feeAmount;
    }

    public long getBuyOrderId() {
        return buyOrderId;
    }

    public long getSellOrderId() {
        return sellOrderId;
    }

    public long getOperationId() {
        return operationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}