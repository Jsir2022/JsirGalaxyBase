package com.jsirgalaxybase.modules.core.market.domain;

import java.time.Instant;

public class CustomMarketAuditLog {

    private final long auditId;
    private final String requestId;
    private final CustomMarketAuditType auditType;
    private final String playerRef;
    private final String requestSemanticsKey;
    private final long listingId;
    private final long tradeId;
    private final String sourceServerId;
    private final String message;
    private final Instant createdAt;
    private final Instant updatedAt;

    public CustomMarketAuditLog(long auditId, String requestId, CustomMarketAuditType auditType, String playerRef,
        String requestSemanticsKey, long listingId, long tradeId, String sourceServerId, String message,
        Instant createdAt, Instant updatedAt) {
        this.auditId = auditId;
        this.requestId = requestId;
        this.auditType = auditType;
        this.playerRef = playerRef;
        this.requestSemanticsKey = requestSemanticsKey;
        this.listingId = listingId;
        this.tradeId = tradeId;
        this.sourceServerId = sourceServerId;
        this.message = message;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getAuditId() {
        return auditId;
    }

    public String getRequestId() {
        return requestId;
    }

    public CustomMarketAuditType getAuditType() {
        return auditType;
    }

    public String getPlayerRef() {
        return playerRef;
    }

    public String getRequestSemanticsKey() {
        return requestSemanticsKey;
    }

    public long getListingId() {
        return listingId;
    }

    public long getTradeId() {
        return tradeId;
    }

    public String getSourceServerId() {
        return sourceServerId;
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
}