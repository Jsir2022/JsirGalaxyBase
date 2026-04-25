package com.jsirgalaxybase.modules.cluster.application;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicket;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicketStatus;
import com.jsirgalaxybase.modules.cluster.port.LocalTeleportExecutor;
import com.jsirgalaxybase.modules.cluster.port.TeleportTicketRepository;

public class PlayerArrivalRestoreService {

    private static final int MAX_STATUS_MESSAGE_LENGTH = 255;

    private final String localServerId;
    private final TeleportTicketRepository ticketRepository;
    private final LocalTeleportExecutor localTeleportExecutor;
    private final Set<String> inFlightTickets = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    public PlayerArrivalRestoreService(String localServerId, TeleportTicketRepository ticketRepository,
        LocalTeleportExecutor localTeleportExecutor) {
        this.localServerId = localServerId;
        this.ticketRepository = ticketRepository;
        this.localTeleportExecutor = localTeleportExecutor;
    }

    public boolean tryRestorePlayer(EntityPlayerMP player, String trigger) {
        if (player == null) {
            return false;
        }
        return tryRestorePlayer(player.getUniqueID().toString(), player, trigger);
    }

    boolean tryRestorePlayer(String playerUuid, EntityPlayerMP player, String trigger) {
        if (playerUuid == null || playerUuid.trim().isEmpty()) {
            return false;
        }
        Instant now = Instant.now();
        ticketRepository.expireActiveTickets(now);
        TransferTicket ticket = ticketRepository.findActiveForTargetPlayer(localServerId, playerUuid, now)
            .orElse(null);
        if (ticket == null) {
            return false;
        }
        if (!inFlightTickets.add(ticket.getTicketId())) {
            return false;
        }

        try {
            GalaxyBase.LOG.info(
                "Cluster target restore matched ticket. requestId={}, ticketId={}, playerUuid={}, sourceServerId={}, targetServerId={}, trigger={}",
                ticket.getRequestId(),
                ticket.getTicketId(),
                ticket.getPlayerUuid(),
                ticket.getSourceServerId(),
                ticket.getTarget().getServerId(),
                trigger);
            localTeleportExecutor.teleport(player, ticket.getTarget());
            TransferTicket completed = ticket.withStatus(TransferTicketStatus.COMPLETED,
                truncate("Target restore completed on " + localServerId), Instant.now());
            ticketRepository.update(completed);
            GalaxyBase.LOG.info(
                "Cluster target restore completed. requestId={}, ticketId={}, playerUuid={}, sourceServerId={}, targetServerId={}",
                completed.getRequestId(),
                completed.getTicketId(),
                completed.getPlayerUuid(),
                completed.getSourceServerId(),
                completed.getTarget().getServerId());
            return true;
        } catch (RuntimeException exception) {
            Instant failureTime = Instant.now();
            String failureMessage = truncate("Target restore retry pending: " + sanitizeMessage(exception));
            TransferTicket nextTicket = failureTime.isBefore(ticket.getExpiresAt())
                ? ticket.withStatus(TransferTicketStatus.DISPATCHED, failureMessage, failureTime)
                : ticket.withStatus(TransferTicketStatus.EXPIRED,
                    truncate("Target restore expired: " + sanitizeMessage(exception)), failureTime);
            ticketRepository.update(nextTicket);
            GalaxyBase.LOG.warn(
                "Cluster target restore failed. requestId={}, ticketId={}, playerUuid={}, sourceServerId={}, targetServerId={}, nextStatus={}, trigger={}, message={}",
                nextTicket.getRequestId(),
                nextTicket.getTicketId(),
                nextTicket.getPlayerUuid(),
                nextTicket.getSourceServerId(),
                nextTicket.getTarget().getServerId(),
                nextTicket.getStatus().name(),
                trigger,
                nextTicket.getStatusMessage(),
                exception);
            return false;
        } finally {
            inFlightTickets.remove(ticket.getTicketId());
        }
    }

    public int expireActiveTickets() {
        return ticketRepository.expireActiveTickets(Instant.now());
    }

    private String sanitizeMessage(RuntimeException exception) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().trim().isEmpty()) {
            return exception == null ? "unknown restore failure" : exception.getClass().getSimpleName();
        }
        return exception.getMessage().trim();
    }

    private String truncate(String text) {
        if (text == null || text.length() <= MAX_STATUS_MESSAGE_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_STATUS_MESSAGE_LENGTH);
    }
}