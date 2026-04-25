package com.jsirgalaxybase.modules.cluster.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.cluster.domain.ServerDescriptor;
import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicket;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicketStatus;
import com.jsirgalaxybase.modules.cluster.port.GatewayAdapter;
import com.jsirgalaxybase.modules.cluster.port.LocalTeleportExecutor;
import com.jsirgalaxybase.modules.cluster.port.ServerDirectory;
import com.jsirgalaxybase.modules.cluster.port.TeleportTicketRepository;

public class ClusterTeleportService {

    private static final long DEFAULT_TICKET_TTL_SECONDS = 60L;

    private final String localServerId;
    private final ServerDirectory serverDirectory;
    private final TeleportTicketRepository ticketRepository;
    private final GatewayAdapter gatewayAdapter;
    private final LocalTeleportExecutor localTeleportExecutor;

    public ClusterTeleportService(String localServerId, ServerDirectory serverDirectory,
        TeleportTicketRepository ticketRepository, GatewayAdapter gatewayAdapter,
        LocalTeleportExecutor localTeleportExecutor) {
        this.localServerId = localServerId;
        this.serverDirectory = serverDirectory;
        this.ticketRepository = ticketRepository;
        this.gatewayAdapter = gatewayAdapter;
        this.localTeleportExecutor = localTeleportExecutor;
    }

    public GatewayDispatchResult dispatchTeleport(EntityPlayerMP livePlayer, String requestId, String playerUuid,
        String playerName, String sourceServerId, String teleportKind, TeleportTarget target) {
        requireText(requestId, "requestId");
        requireText(playerUuid, "playerUuid");
        requireText(playerName, "playerName");
        requireText(sourceServerId, "sourceServerId");
        requireText(teleportKind, "teleportKind");
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }

        Optional<ServerDescriptor> targetServer = serverDirectory.findById(target.getServerId());
        if (!targetServer.isPresent()) {
            throw new IllegalStateException("Unknown target server: " + target.getServerId());
        }
        if (!targetServer.get().isEnabled()) {
            throw new IllegalStateException("Target server is disabled: " + target.getServerId());
        }

        if (localServerId.equals(target.getServerId()) && localServerId.equals(sourceServerId)) {
            if (livePlayer == null) {
                throw new IllegalStateException("Local teleport requires a live player handle on the source server");
            }
            localTeleportExecutor.teleport(livePlayer, target);
            return GatewayDispatchResult.completedLocal("teleported locally");
        }

        Instant now = Instant.now();
        TransferTicket existing = ticketRepository.findByRequestId(requestId).orElse(null);
        if (existing != null) {
            return mapExistingTicket(existing);
        }

        TransferTicket ticket = new TransferTicket(UUID.randomUUID().toString(), requestId, playerUuid, playerName,
            teleportKind, sourceServerId, target, TransferTicketStatus.PENDING_GATEWAY,
            "Gateway dispatch queued", now, now.plusSeconds(DEFAULT_TICKET_TTL_SECONDS), now);
        TransferTicket saved = ticketRepository.save(ticket);
        GatewayDispatchResult dispatchResult = gatewayAdapter.dispatchRemote(livePlayer, saved, targetServer.get());
        if (dispatchResult.getTicket() != null) {
            TransferTicket returnedTicket = dispatchResult.getTicket();
            String persistedMessage = dispatchResult.getMessage() == null ? returnedTicket.getStatusMessage()
                : dispatchResult.getMessage();
            ticketRepository.update(returnedTicket.withStatus(returnedTicket.getStatus(), persistedMessage,
                Instant.now()));
        }
        return dispatchResult;
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private GatewayDispatchResult mapExistingTicket(TransferTicket existing) {
        String message = existing.getStatusMessage();
        if (existing.getStatus() == TransferTicketStatus.PENDING_GATEWAY
            || existing.getStatus() == TransferTicketStatus.DISPATCHED) {
            return GatewayDispatchResult.pendingRemote(message, existing);
        }
        if (existing.getStatus() == TransferTicketStatus.COMPLETED) {
            return GatewayDispatchResult.completedLocal(
                message == null ? "Teleport ticket was already completed" : message);
        }
        return GatewayDispatchResult.failed(message == null ? "Teleport ticket is not in a retryable state" : message,
            existing);
    }
}