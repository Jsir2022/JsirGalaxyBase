package com.jsirgalaxybase.modules.cluster.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.cluster.domain.ServerDescriptor;
import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicket;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicketStatus;
import com.jsirgalaxybase.modules.cluster.port.GatewayAdapter;
import com.jsirgalaxybase.modules.cluster.port.LocalTeleportExecutor;
import com.jsirgalaxybase.modules.cluster.port.ServerDirectory;
import com.jsirgalaxybase.modules.cluster.port.TeleportTicketRepository;

public class ClusterTeleportServiceTest {

    @Test
    public void remoteDispatchCreatesTicketAndReturnsPendingResult() {
        FakeServerDirectory serverDirectory = new FakeServerDirectory();
        serverDirectory.upsertLocalServer("server-alpha", "server-alpha");
        serverDirectory.servers.put("server-beta", new ServerDescriptor("server-beta", "server-beta", null, false,
            true, Instant.now(), Instant.now()));
        FakeTicketRepository ticketRepository = new FakeTicketRepository();
        RecordingGatewayAdapter gatewayAdapter = new RecordingGatewayAdapter();
        ClusterTeleportService service = new ClusterTeleportService("server-alpha", serverDirectory, ticketRepository,
            gatewayAdapter, new NoOpLocalTeleportExecutor());

        GatewayDispatchResult result = service.dispatchTeleport(null, "req-remote-1", "player-uuid", "PlayerA",
            "server-alpha", "HOME", new TeleportTarget("server-beta", 0, 10, 70, 10, 0.0F, 0.0F));

        assertEquals(GatewayDispatchResult.Status.PENDING_REMOTE, result.getStatus());
        assertEquals(1, ticketRepository.savedTickets.size());
        assertEquals(1, ticketRepository.updatedTickets.size());
        assertNotNull(gatewayAdapter.lastTicket);
        assertEquals("server-beta", gatewayAdapter.lastTicket.getTarget().getServerId());
        assertEquals(TransferTicketStatus.DISPATCHED, ticketRepository.updatedTickets.get(0).getStatus());
        assertEquals("proxy dispatch requested", ticketRepository.updatedTickets.get(0).getStatusMessage());
    }

    @Test
    public void failedRemoteDispatchStillPersistsReturnedFailedTicket() {
        FakeServerDirectory serverDirectory = new FakeServerDirectory();
        serverDirectory.upsertLocalServer("server-alpha", "server-alpha");
        serverDirectory.servers.put("server-gamma", new ServerDescriptor("server-gamma", "server-gamma", null,
            false, true, Instant.now(), Instant.now()));
        FakeTicketRepository ticketRepository = new FakeTicketRepository();
        ClusterTeleportService service = new ClusterTeleportService("server-alpha", serverDirectory, ticketRepository,
            new FailingGatewayAdapter(), new NoOpLocalTeleportExecutor());

        GatewayDispatchResult result = service.dispatchTeleport(null, "req-remote-fail", "player-uuid", "PlayerA",
            "server-alpha", "HOME", new TeleportTarget("server-gamma", 0, 10, 70, 10, 0.0F, 0.0F));

        assertEquals(GatewayDispatchResult.Status.FAILED, result.getStatus());
        assertEquals(1, ticketRepository.updatedTickets.size());
        assertEquals(TransferTicketStatus.FAILED, ticketRepository.updatedTickets.get(0).getStatus());
        assertEquals("failed", ticketRepository.updatedTickets.get(0).getStatusMessage());
    }

    @Test(expected = IllegalStateException.class)
    public void unknownTargetServerIsRejectedBeforeTicketCreation() {
        ClusterTeleportService service = new ClusterTeleportService("server-alpha", new FakeServerDirectory(),
            new FakeTicketRepository(), new RecordingGatewayAdapter(), new NoOpLocalTeleportExecutor());

        service.dispatchTeleport(null, "req-remote-2", "player-uuid", "PlayerA", "server-alpha", "WARP",
            new TeleportTarget("missing-server", 0, 0, 64, 0, 0.0F, 0.0F));
    }

    @Test
    public void repeatedRequestIdWithFailedTicketReturnsFailedResult() {
        FakeServerDirectory serverDirectory = new FakeServerDirectory();
        serverDirectory.upsertLocalServer("server-alpha", "server-alpha");
        serverDirectory.servers.put("server-beta", new ServerDescriptor("server-beta", "server-beta", null,
            false, true, Instant.now(), Instant.now()));
        FakeTicketRepository ticketRepository = new FakeTicketRepository();
        ticketRepository.existingTicket = new TransferTicket("ticket-failed", "req-existing-failed", "player-uuid",
            "PlayerA", "HOME", "server-alpha", new TeleportTarget("server-beta", 0, 10, 70, 10, 0.0F, 0.0F),
            TransferTicketStatus.FAILED, "dispatch failed", Instant.now(), Instant.now().plusSeconds(60),
            Instant.now());
        ClusterTeleportService service = new ClusterTeleportService("server-alpha", serverDirectory, ticketRepository,
            new RecordingGatewayAdapter(), new NoOpLocalTeleportExecutor());

        GatewayDispatchResult result = service.dispatchTeleport(null, "req-existing-failed", "player-uuid", "PlayerA",
            "server-alpha", "HOME", new TeleportTarget("server-beta", 0, 10, 70, 10, 0.0F, 0.0F));

        assertEquals(GatewayDispatchResult.Status.FAILED, result.getStatus());
        assertEquals("dispatch failed", result.getMessage());
        assertEquals(0, ticketRepository.savedTickets.size());
    }

    @Test
    public void repeatedRequestIdWithCompletedTicketReturnsCompletedResult() {
        FakeServerDirectory serverDirectory = new FakeServerDirectory();
        serverDirectory.upsertLocalServer("server-alpha", "server-alpha");
        serverDirectory.servers.put("server-beta", new ServerDescriptor("server-beta", "server-beta", null,
            false, true, Instant.now(), Instant.now()));
        FakeTicketRepository ticketRepository = new FakeTicketRepository();
        ticketRepository.existingTicket = new TransferTicket("ticket-completed", "req-existing-completed",
            "player-uuid", "PlayerA", "HOME", "server-alpha",
            new TeleportTarget("server-beta", 0, 10, 70, 10, 0.0F, 0.0F), TransferTicketStatus.COMPLETED,
            "restore completed", Instant.now(), Instant.now().plusSeconds(60), Instant.now());
        ClusterTeleportService service = new ClusterTeleportService("server-alpha", serverDirectory, ticketRepository,
            new RecordingGatewayAdapter(), new NoOpLocalTeleportExecutor());

        GatewayDispatchResult result = service.dispatchTeleport(null, "req-existing-completed", "player-uuid",
            "PlayerA", "server-alpha", "HOME", new TeleportTarget("server-beta", 0, 10, 70, 10, 0.0F, 0.0F));

        assertEquals(GatewayDispatchResult.Status.COMPLETED_LOCAL, result.getStatus());
        assertEquals("restore completed", result.getMessage());
        assertEquals(0, ticketRepository.savedTickets.size());
    }

    @Test
    public void repeatedRequestIdWithExpiredTicketReturnsFailedResult() {
        FakeServerDirectory serverDirectory = new FakeServerDirectory();
        serverDirectory.upsertLocalServer("server-alpha", "server-alpha");
        serverDirectory.servers.put("server-beta", new ServerDescriptor("server-beta", "server-beta", null,
            false, true, Instant.now(), Instant.now()));
        FakeTicketRepository ticketRepository = new FakeTicketRepository();
        ticketRepository.existingTicket = new TransferTicket("ticket-expired", "req-existing-expired", "player-uuid",
            "PlayerA", "HOME", "server-alpha", new TeleportTarget("server-beta", 0, 10, 70, 10, 0.0F, 0.0F),
            TransferTicketStatus.EXPIRED, "transfer expired", Instant.now(), Instant.now().minusSeconds(5),
            Instant.now());
        ClusterTeleportService service = new ClusterTeleportService("server-alpha", serverDirectory, ticketRepository,
            new RecordingGatewayAdapter(), new NoOpLocalTeleportExecutor());

        GatewayDispatchResult result = service.dispatchTeleport(null, "req-existing-expired", "player-uuid", "PlayerA",
            "server-alpha", "HOME", new TeleportTarget("server-beta", 0, 10, 70, 10, 0.0F, 0.0F));

        assertEquals(GatewayDispatchResult.Status.FAILED, result.getStatus());
        assertEquals("transfer expired", result.getMessage());
        assertEquals(0, ticketRepository.savedTickets.size());
    }

    private static final class FakeServerDirectory implements ServerDirectory {

        private final Map<String, ServerDescriptor> servers = new HashMap<String, ServerDescriptor>();

        @Override
        public Optional<ServerDescriptor> findById(String serverId) {
            return Optional.ofNullable(servers.get(serverId));
        }

        @Override
        public List<ServerDescriptor> listAll() {
            return new ArrayList<ServerDescriptor>(servers.values());
        }

        @Override
        public ServerDescriptor upsertLocalServer(String serverId, String displayName) {
            ServerDescriptor descriptor = new ServerDescriptor(serverId, displayName, null, true, true, Instant.now(),
                Instant.now());
            servers.put(serverId, descriptor);
            return descriptor;
        }
    }

    private static final class FakeTicketRepository implements TeleportTicketRepository {

        private final List<TransferTicket> savedTickets = new ArrayList<TransferTicket>();
        private final List<TransferTicket> updatedTickets = new ArrayList<TransferTicket>();
        private TransferTicket existingTicket;

        @Override
        public TransferTicket save(TransferTicket ticket) {
            savedTickets.add(ticket);
            return ticket;
        }

        @Override
        public TransferTicket update(TransferTicket ticket) {
            updatedTickets.add(ticket);
            return ticket;
        }

        @Override
        public Optional<TransferTicket> findByRequestId(String requestId) {
            if (existingTicket != null && existingTicket.getRequestId().equals(requestId)) {
                return Optional.of(existingTicket);
            }
            for (TransferTicket ticket : savedTickets) {
                if (ticket.getRequestId().equals(requestId)) {
                    return Optional.of(ticket);
                }
            }
            return Optional.empty();
        }

        @Override
        public Optional<TransferTicket> findActiveForTargetPlayer(String targetServerId, String playerUuid,
            Instant now) {
            return Optional.empty();
        }

        @Override
        public int expireActiveTickets(Instant now) {
            return 0;
        }
    }

    private static final class RecordingGatewayAdapter implements GatewayAdapter {

        private TransferTicket lastTicket;

        @Override
        public GatewayDispatchResult dispatchRemote(net.minecraft.entity.player.EntityPlayerMP livePlayer,
            TransferTicket ticket, ServerDescriptor targetServer) {
            lastTicket = ticket;
            return GatewayDispatchResult.pendingRemote("proxy dispatch requested",
                ticket.withStatus(TransferTicketStatus.DISPATCHED, "proxy dispatch requested", Instant.now()));
        }
    }

    private static final class FailingGatewayAdapter implements GatewayAdapter {

        @Override
        public GatewayDispatchResult dispatchRemote(net.minecraft.entity.player.EntityPlayerMP livePlayer,
            TransferTicket ticket, ServerDescriptor targetServer) {
            return GatewayDispatchResult.failed("failed", ticket.withStatus(TransferTicketStatus.FAILED, "failed",
                Instant.now()));
        }
    }

    private static final class NoOpLocalTeleportExecutor implements LocalTeleportExecutor {

        @Override
        public void teleport(net.minecraft.entity.player.EntityPlayerMP player,
            com.jsirgalaxybase.modules.cluster.domain.TeleportTarget target) {}
    }
}