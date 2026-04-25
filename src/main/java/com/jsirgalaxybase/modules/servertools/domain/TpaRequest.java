package com.jsirgalaxybase.modules.servertools.domain;

import java.time.Instant;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;

public class TpaRequest {

    private final String requestId;
    private final String requesterPlayerUuid;
    private final String requesterPlayerName;
    private final String requesterServerId;
    private final TeleportTarget requesterOrigin;
    private final String targetPlayerName;
    private final String targetServerId;
    private final TpaRequestStatus status;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Instant updatedAt;

    public TpaRequest(String requestId, String requesterPlayerUuid, String requesterPlayerName,
        String requesterServerId, TeleportTarget requesterOrigin, String targetPlayerName, String targetServerId,
        TpaRequestStatus status, Instant createdAt, Instant expiresAt, Instant updatedAt) {
        this.requestId = requestId;
        this.requesterPlayerUuid = requesterPlayerUuid;
        this.requesterPlayerName = requesterPlayerName;
        this.requesterServerId = requesterServerId;
        this.requesterOrigin = requesterOrigin;
        this.targetPlayerName = targetPlayerName;
        this.targetServerId = targetServerId;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.updatedAt = updatedAt;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getRequesterPlayerUuid() {
        return requesterPlayerUuid;
    }

    public String getRequesterPlayerName() {
        return requesterPlayerName;
    }

    public String getRequesterServerId() {
        return requesterServerId;
    }

    public TeleportTarget getRequesterOrigin() {
        return requesterOrigin;
    }

    public String getTargetPlayerName() {
        return targetPlayerName;
    }

    public String getTargetServerId() {
        return targetServerId;
    }

    public TpaRequestStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public TpaRequest withStatus(TpaRequestStatus nextStatus, Instant now) {
        return new TpaRequest(requestId, requesterPlayerUuid, requesterPlayerName, requesterServerId, requesterOrigin,
            targetPlayerName, targetServerId, nextStatus, createdAt, expiresAt, now);
    }
}