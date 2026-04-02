package com.jsirgalaxybase.modules.core.market.domain;

import java.time.Instant;

public class MarketOperationLog {

    private final long operationId;
    private final String requestId;
    private final MarketOperationType operationType;
    private final MarketOperationStatus status;
    private final String sourceServerId;
    private final String playerRef;
    private final String requestSemanticsKey;
    private final String recoveryMetadataKey;
    private final long relatedOrderId;
    private final long relatedCustodyId;
    private final long relatedTradeId;
    private final String message;
    private final Instant createdAt;
    private final Instant updatedAt;

    public MarketOperationLog(long operationId, String requestId, MarketOperationType operationType,
        MarketOperationStatus status, String sourceServerId, String playerRef, String requestSemanticsKey,
        long relatedOrderId, long relatedCustodyId, long relatedTradeId, String message, Instant createdAt,
        Instant updatedAt) {
        this(operationId, requestId, operationType, status, sourceServerId, playerRef, requestSemanticsKey, null,
            relatedOrderId, relatedCustodyId, relatedTradeId, message, createdAt, updatedAt);
    }

    public MarketOperationLog(long operationId, String requestId, MarketOperationType operationType,
        MarketOperationStatus status, String sourceServerId, String playerRef, String requestSemanticsKey,
        String recoveryMetadataKey, long relatedOrderId, long relatedCustodyId, long relatedTradeId, String message,
        Instant createdAt, Instant updatedAt) {
        this.operationId = operationId;
        this.requestId = requestId;
        this.operationType = operationType;
        this.status = status;
        this.sourceServerId = sourceServerId;
        this.playerRef = playerRef;
        this.requestSemanticsKey = requestSemanticsKey;
        this.recoveryMetadataKey = recoveryMetadataKey;
        this.relatedOrderId = relatedOrderId;
        this.relatedCustodyId = relatedCustodyId;
        this.relatedTradeId = relatedTradeId;
        this.message = message;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getOperationId() {
        return operationId;
    }

    public String getRequestId() {
        return requestId;
    }

    public MarketOperationType getOperationType() {
        return operationType;
    }

    public MarketOperationStatus getStatus() {
        return status;
    }

    public String getSourceServerId() {
        return sourceServerId;
    }

    public String getPlayerRef() {
        return playerRef;
    }

    public String getRequestSemanticsKey() {
        return requestSemanticsKey;
    }

    public String getRecoveryMetadataKey() {
        return recoveryMetadataKey;
    }

    public long getRelatedOrderId() {
        return relatedOrderId;
    }

    public long getRelatedCustodyId() {
        return relatedCustodyId;
    }

    public long getRelatedTradeId() {
        return relatedTradeId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public MarketOperationLog withState(MarketOperationStatus newStatus, long newRelatedOrderId,
        long newRelatedCustodyId, long newRelatedTradeId, String newMessage, Instant newUpdatedAt) {
        return withState(newStatus, newRelatedOrderId, newRelatedCustodyId, newRelatedTradeId, newMessage,
            recoveryMetadataKey, newUpdatedAt);
    }

    public MarketOperationLog withState(MarketOperationStatus newStatus, long newRelatedOrderId,
        long newRelatedCustodyId, long newRelatedTradeId, String newMessage, String newRecoveryMetadataKey,
        Instant newUpdatedAt) {
        return new MarketOperationLog(operationId, requestId, operationType, newStatus, sourceServerId, playerRef,
            requestSemanticsKey, newRecoveryMetadataKey, newRelatedOrderId, newRelatedCustodyId, newRelatedTradeId,
            newMessage, createdAt, newUpdatedAt);
    }
}