package com.jsirgalaxybase.modules.core.vault.domain;

import java.time.Instant;

public final class VaultOperation {

    private final long operationId;
    private final String requestId;
    private final long accountId;
    private final String operationType;
    private final String sourceDomain;
    private final String targetDomain;
    private final String itemSnapshot;
    private final int quantity;
    private final VaultOperationStatus status;
    private final String message;
    private final Instant createdAt;
    private final Instant updatedAt;

    public VaultOperation(long operationId, String requestId, long accountId, String operationType, String sourceDomain,
        String targetDomain, String itemSnapshot, int quantity, VaultOperationStatus status, String message,
        Instant createdAt, Instant updatedAt) {
        this.operationId = operationId;
        this.requestId = requestId;
        this.accountId = accountId;
        this.operationType = operationType;
        this.sourceDomain = sourceDomain;
        this.targetDomain = targetDomain;
        this.itemSnapshot = itemSnapshot;
        this.quantity = quantity;
        this.status = status;
        this.message = message;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getOperationId() { return operationId; }
    public String getRequestId() { return requestId; }
    public long getAccountId() { return accountId; }
    public String getOperationType() { return operationType; }
    public String getSourceDomain() { return sourceDomain; }
    public String getTargetDomain() { return targetDomain; }
    public String getItemSnapshot() { return itemSnapshot; }
    public int getQuantity() { return quantity; }
    public VaultOperationStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public VaultOperation withStatus(VaultOperationStatus nextStatus, String nextMessage, Instant now) {
        return new VaultOperation(operationId, requestId, accountId, operationType, sourceDomain, targetDomain,
            itemSnapshot, quantity, nextStatus, nextMessage, createdAt, now);
    }
}
