package com.jsirgalaxybase.modules.cluster.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicket;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicketStatus;
import com.jsirgalaxybase.modules.cluster.port.LocalTeleportExecutor;
import com.jsirgalaxybase.modules.cluster.port.TeleportTicketRepository;

public class PlayerArrivalRestoreServiceTest {

    @Test
    public void dispatchedTicketIsRestoredAndCompleted() {
        FakeTicketRepository ticketRepository = new FakeTicketRepository();
        RecordingLocalTeleportExecutor localTeleportExecutor = new RecordingLocalTeleportExecutor();
        PlayerArrivalRestoreService service = new PlayerArrivalRestoreService("server-beta", ticketRepository,
            localTeleportExecutor);
        String playerUuid = "player-uuid-1";
        TransferTicket ticket = createTicket(playerUuid, TransferTicketStatus.DISPATCHED, Instant.now().plusSeconds(30));
        ticketRepository.activeTicket = ticket;

        boolean restored = service.tryRestorePlayer(playerUuid, null, "test");

        assertTrue(restored);
        assertNotNull(localTeleportExecutor.lastTarget);
        assertEquals(TransferTicketStatus.COMPLETED, ticketRepository.updatedTickets.get(0).getStatus());
    }

    @Test
    public void restoreFailureKeepsTicketRetryableUntilExpiry() {
        FakeTicketRepository ticketRepository = new FakeTicketRepository();
        FailingLocalTeleportExecutor localTeleportExecutor = new FailingLocalTeleportExecutor();
        PlayerArrivalRestoreService service = new PlayerArrivalRestoreService("server-beta", ticketRepository,
            localTeleportExecutor);
        String playerUuid = "player-uuid-2";
        TransferTicket ticket = createTicket(playerUuid, TransferTicketStatus.DISPATCHED, Instant.now().plusSeconds(30));
        ticketRepository.activeTicket = ticket;

        boolean restored = service.tryRestorePlayer(playerUuid, null, "test");

        assertFalse(restored);
        assertEquals(TransferTicketStatus.DISPATCHED, ticketRepository.updatedTickets.get(0).getStatus());
        assertTrue(ticketRepository.updatedTickets.get(0).getStatusMessage().contains("retry pending"));
    }

    @Test
    public void expiredTicketsAreMarkedExpired() {
        FakeTicketRepository ticketRepository = new FakeTicketRepository();
        PlayerArrivalRestoreService service = new PlayerArrivalRestoreService("server-beta", ticketRepository,
            new RecordingLocalTeleportExecutor());
        ticketRepository.activeTicket = createTicket("player-uuid-expire", TransferTicketStatus.DISPATCHED,
            Instant.now().minusSeconds(1));

        int expired = service.expireActiveTickets();

        assertEquals(1, expired);
        assertEquals(1, ticketRepository.expireCalls);
        assertEquals(TransferTicketStatus.EXPIRED, ticketRepository.activeTicket.getStatus());
        assertEquals(
            com.jsirgalaxybase.modules.cluster.infrastructure.jdbc.JdbcTeleportTicketRepository.EXPIRED_STATUS_MESSAGE,
            ticketRepository.activeTicket.getStatusMessage());
    }

    @Test
    public void restoreFailureAfterExpiryUsesExpiredMessageInsteadOfRetryMessage() {
        FakeTicketRepository ticketRepository = new FakeTicketRepository();
        FailingLocalTeleportExecutor localTeleportExecutor = new FailingLocalTeleportExecutor();
        PlayerArrivalRestoreService service = new PlayerArrivalRestoreService("server-beta", ticketRepository,
            localTeleportExecutor);
        String playerUuid = "player-uuid-3";
        TransferTicket ticket = createTicket(playerUuid, TransferTicketStatus.DISPATCHED, Instant.now().minusSeconds(1));
        ticketRepository.activeTicket = ticket;

        boolean restored = service.tryRestorePlayer(playerUuid, null, "test");

        assertFalse(restored);
        assertTrue(ticketRepository.updatedTickets.isEmpty());
        assertEquals(TransferTicketStatus.EXPIRED, ticketRepository.activeTicket.getStatus());
        assertTrue(ticketRepository.activeTicket.getStatusMessage().contains("expired"));
        assertFalse(ticketRepository.activeTicket.getStatusMessage().contains("retry pending"));
    }

    private TransferTicket createTicket(String playerUuid, TransferTicketStatus status, Instant expiresAt) {
        return new TransferTicket("ticket-1", "request-1", playerUuid, "PlayerA", "HOME",
            "server-alpha", new TeleportTarget("server-beta", 0, 10, 70, 10, 0.0F, 0.0F), status,
            "waiting for target restore", Instant.now(), expiresAt, Instant.now());
    }

    private static final class FakeTicketRepository implements TeleportTicketRepository {

        private TransferTicket activeTicket;
        private final List<TransferTicket> updatedTickets = new ArrayList<TransferTicket>();
        private int expireCalls;

        @Override
        public TransferTicket save(TransferTicket ticket) {
            return ticket;
        }

        @Override
        public TransferTicket update(TransferTicket ticket) {
            updatedTickets.add(ticket);
            activeTicket = ticket;
            return ticket;
        }

        @Override
        public Optional<TransferTicket> findByRequestId(String requestId) {
            return Optional.empty();
        }

        @Override
        public Optional<TransferTicket> findActiveForTargetPlayer(String targetServerId, String playerUuid,
            Instant now) {
            if (activeTicket == null) {
                return Optional.empty();
            }
            if (!activeTicket.getTarget().getServerId().equals(targetServerId)) {
                return Optional.empty();
            }
            if (!activeTicket.getPlayerUuid().equals(playerUuid)) {
                return Optional.empty();
            }
            if (!activeTicket.isActiveAt(now)) {
                return Optional.empty();
            }
            if (activeTicket.getStatus() != TransferTicketStatus.DISPATCHED) {
                return Optional.empty();
            }
            return Optional.of(activeTicket);
        }

        @Override
        public int expireActiveTickets(Instant now) {
            expireCalls++;
            if (activeTicket != null && (activeTicket.getStatus() == TransferTicketStatus.PENDING_GATEWAY
                || activeTicket.getStatus() == TransferTicketStatus.DISPATCHED)
                && !activeTicket.getExpiresAt().isAfter(now)) {
                activeTicket = activeTicket.withStatus(TransferTicketStatus.EXPIRED,
                    com.jsirgalaxybase.modules.cluster.infrastructure.jdbc.JdbcTeleportTicketRepository.EXPIRED_STATUS_MESSAGE,
                    now);
                return 1;
            }
            return 0;
        }
    }

    private static class RecordingLocalTeleportExecutor implements LocalTeleportExecutor {

        private TeleportTarget lastTarget;

        @Override
        public void teleport(EntityPlayerMP player, TeleportTarget target) {
            lastTarget = target;
        }
    }

    private static final class FailingLocalTeleportExecutor implements LocalTeleportExecutor {

        @Override
        public void teleport(EntityPlayerMP player, TeleportTarget target) {
            throw new IllegalStateException("dimension is not ready");
        }
    }

}