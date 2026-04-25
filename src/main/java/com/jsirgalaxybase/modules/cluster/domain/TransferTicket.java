package com.jsirgalaxybase.modules.cluster.domain;

import java.time.Instant;
import java.util.EnumSet;

public class TransferTicket {

    private static final EnumSet<TransferTicketStatus> ACTIVE_STATUSES = EnumSet.of(
        TransferTicketStatus.PENDING_GATEWAY,
        TransferTicketStatus.DISPATCHED);

    private final String ticketId;
    private final String requestId;
    private final String playerUuid;
    private final String playerName;
    private final String teleportKind;
    private final String sourceServerId;
    private final TeleportTarget target;
    private final TransferTicketStatus status;
    private final String statusMessage;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Instant updatedAt;

    public TransferTicket(String ticketId, String requestId, String playerUuid, String playerName, String teleportKind,
        String sourceServerId, TeleportTarget target, TransferTicketStatus status, String statusMessage,
        Instant createdAt, Instant expiresAt, Instant updatedAt) {
        this.ticketId = ticketId;
        this.requestId = requestId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.teleportKind = teleportKind;
        this.sourceServerId = sourceServerId;
        this.target = target;
        this.status = status;
        this.statusMessage = statusMessage;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.updatedAt = updatedAt;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getTeleportKind() {
        return teleportKind;
    }

    public String getSourceServerId() {
        return sourceServerId;
    }

    public TeleportTarget getTarget() {
        return target;
    }

    public TransferTicketStatus getStatus() {
        return status;
    }

    public String getStatusMessage() {
        return statusMessage;
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

    public TransferTicket withStatus(TransferTicketStatus nextStatus, String nextMessage, Instant now) {
        return new TransferTicket(ticketId, requestId, playerUuid, playerName, teleportKind, sourceServerId, target,
            nextStatus, nextMessage, createdAt, expiresAt, now);
    }

    public boolean isActiveAt(Instant now) {
        return ACTIVE_STATUSES.contains(status) && now != null && expiresAt.isAfter(now);
    }
}