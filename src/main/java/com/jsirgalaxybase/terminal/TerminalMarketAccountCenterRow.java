package com.jsirgalaxybase.terminal;

/** Structured wire row for the order and asset center; display names stay client-localized. */
public final class TerminalMarketAccountCenterRow {
    private final String recordId, kind, registryName, side, orderType, status, createdAt, message;
    private final int meta;
    private final long unitPrice, originalQuantity, filledQuantity, remainingQuantity, relatedOrderId;
    private final long grossAmount, feeAmount, updatedAtEpochSeconds, reservedFunds;
    private final boolean cancelable;

    public TerminalMarketAccountCenterRow(String recordId, String kind, String registryName, int meta, String side,
        String orderType, long unitPrice, long originalQuantity, long filledQuantity, long remainingQuantity,
        String status, String createdAt, long relatedOrderId, long grossAmount, long feeAmount, String message,
        boolean cancelable, long updatedAtEpochSeconds, long reservedFunds) {
        this.recordId = safe(recordId); this.kind = safe(kind); this.registryName = safe(registryName);
        this.meta = Math.max(0, meta); this.side = safe(side); this.orderType = safe(orderType);
        this.unitPrice = Math.max(0L, unitPrice); this.originalQuantity = Math.max(0L, originalQuantity);
        this.filledQuantity = Math.max(0L, filledQuantity); this.remainingQuantity = Math.max(0L, remainingQuantity);
        this.status = safe(status); this.createdAt = safe(createdAt); this.relatedOrderId = Math.max(0L, relatedOrderId);
        this.grossAmount = Math.max(0L, grossAmount); this.feeAmount = Math.max(0L, feeAmount);
        this.message = safe(message); this.cancelable = cancelable;
        this.updatedAtEpochSeconds = Math.max(0L, updatedAtEpochSeconds); this.reservedFunds = Math.max(0L, reservedFunds);
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
    public String getRecordId() { return recordId; } public String getKind() { return kind; }
    public String getRegistryName() { return registryName; } public int getMeta() { return meta; }
    public String getSide() { return side; } public String getOrderType() { return orderType; }
    public long getUnitPrice() { return unitPrice; } public long getOriginalQuantity() { return originalQuantity; }
    public long getFilledQuantity() { return filledQuantity; } public long getRemainingQuantity() { return remainingQuantity; }
    public String getStatus() { return status; } public String getCreatedAt() { return createdAt; }
    public long getRelatedOrderId() { return relatedOrderId; } public long getGrossAmount() { return grossAmount; }
    public long getFeeAmount() { return feeAmount; } public String getMessage() { return message; }
    public boolean isCancelable() { return cancelable; } public long getUpdatedAtEpochSeconds() { return updatedAtEpochSeconds; }
    public long getReservedFunds() { return reservedFunds; }
    public String getIconRef() { return registryName.isEmpty() ? "" : registryName + "@" + meta; }
}
