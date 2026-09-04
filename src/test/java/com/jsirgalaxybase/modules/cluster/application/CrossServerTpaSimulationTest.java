package com.jsirgalaxybase.modules.cluster.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

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
import com.jsirgalaxybase.modules.servertools.application.AcceptedTpaSourceDispatcher;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.domain.BackRecord;
import com.jsirgalaxybase.modules.servertools.domain.PlayerHome;
import com.jsirgalaxybase.modules.servertools.domain.RandomTeleportRecord;
import com.jsirgalaxybase.modules.servertools.domain.ServerWarp;
import com.jsirgalaxybase.modules.servertools.domain.TeleportActor;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;
import com.jsirgalaxybase.modules.servertools.domain.TpaRequest;
import com.jsirgalaxybase.modules.servertools.domain.TpaRequestStatus;
import com.jsirgalaxybase.modules.servertools.infrastructure.AllowAllPlayerPermissionPolicy;
import com.jsirgalaxybase.modules.servertools.port.PlayerTeleportRepository;

/**
 * Cross-server TPA simulation with no fake player, proxy, or live Minecraft server.
 *
 * <p>The test keeps the real TPA service, source dispatcher, cluster ticket service and target restore service in the
 * same chain. Only the online connection gateway and local teleport executor are deterministic recorders.</p>
 */
public class CrossServerTpaSimulationTest {

    @Test
    public void acceptedTpaFlowsFromSourceDispatchToTargetRestoreWithoutSecondConnection() {
        Instant now = Instant.now();
        InMemoryPlayerTeleportRepository playerRepository = new InMemoryPlayerTeleportRepository();
        InMemoryServerDirectory directory = directory();
        PlayerTeleportService tpaService = new PlayerTeleportService(playerRepository,
            new AllowAllPlayerPermissionPolicy(), directory);
        TeleportActor requester = actor("requester-uuid", "Requester", "server-alpha", 12, 70, -8);
        TeleportActor acceptor = actor("acceptor-uuid", "Acceptor", "server-beta", 240, 82, 480);

        TpaRequest pending = tpaService.createTpaRequest(requester, "tpa-simulation-1", "Acceptor", "server-beta",
            now);
        TpaRequest accepted = tpaService.acceptTpa(acceptor, "Requester", now.plusSeconds(1));
        assertEquals(TpaRequestStatus.ACCEPTED, accepted.getStatus());
        assertEquals(240.0D, accepted.getAcceptedTarget().getX(), 0.0001D);

        InMemoryTicketRepository tickets = new InMemoryTicketRepository();
        RecordingGateway gateway = new RecordingGateway();
        ClusterTeleportService sourceCluster = new ClusterTeleportService("server-alpha", directory, tickets, gateway,
            new RecordingLocalTeleport());
        AcceptedTpaSourceDispatcher sourceDispatcher = new AcceptedTpaSourceDispatcher(tpaService);

        List<AcceptedTpaSourceDispatcher.DispatchAttempt> firstSweep = sourceDispatcher.dispatchAcceptedRequests(
            requester, now.plusSeconds(2), new ClusterDispatchExecutor(sourceCluster));
        assertEquals(1, firstSweep.size());
        assertFalse(firstSweep.get(0).isFailed());
        assertEquals(1, gateway.calls);
        assertNotNull(gateway.lastTicket);
        assertEquals("tpa-dispatch-" + pending.getRequestId(), gateway.lastTicket.getRequestId());
        assertEquals("server-alpha", gateway.lastTicket.getSourceServerId());
        assertEquals("server-beta", gateway.lastTicket.getTarget().getServerId());
        assertEquals(TransferTicketStatus.DISPATCHED,
            tickets.findByRequestId(gateway.lastTicket.getRequestId()).get().getStatus());

        // A repeated source tick is locally suppressed. A fresh process would still hit the durable ticket and not
        // call the gateway again (the ClusterTeleportService requestId lookup is the cross-restart guard).
        assertTrue(sourceDispatcher.dispatchAcceptedRequests(requester, now.plusSeconds(3),
            new ClusterDispatchExecutor(sourceCluster)).isEmpty());
        AcceptedTpaSourceDispatcher afterRestartDispatcher = new AcceptedTpaSourceDispatcher(tpaService);
        assertEquals(1, afterRestartDispatcher.dispatchAcceptedRequests(requester, now.plusSeconds(4),
            new ClusterDispatchExecutor(sourceCluster)).size());
        assertEquals(1, gateway.calls);

        RecordingLocalTeleport targetTeleport = new RecordingLocalTeleport();
        PlayerArrivalRestoreService targetRestore = new PlayerArrivalRestoreService("server-beta", tickets,
            targetTeleport);
        assertTrue(targetRestore.tryRestorePlayer(requester.getPlayerUuid(), null, "simulation-arrival"));
        assertEquals(240.0D, targetTeleport.lastTarget.getX(), 0.0001D);
        assertEquals(82.0D, targetTeleport.lastTarget.getY(), 0.0001D);
        assertEquals(TransferTicketStatus.COMPLETED,
            tickets.findByRequestId(gateway.lastTicket.getRequestId()).get().getStatus());
    }

    @Test
    public void onlyTheOriginalSourceServerCanDispatchAndExpiredAcceptedRequestsAreIgnored() {
        Instant now = Instant.now();
        InMemoryPlayerTeleportRepository playerRepository = new InMemoryPlayerTeleportRepository();
        InMemoryServerDirectory directory = directory();
        PlayerTeleportService tpaService = new PlayerTeleportService(playerRepository,
            new AllowAllPlayerPermissionPolicy(), directory);
        TeleportActor requester = actor("requester-uuid", "Requester", "server-alpha", 0, 70, 0);
        TeleportActor acceptor = actor("acceptor-uuid", "Acceptor", "server-beta", 32, 75, 32);
        tpaService.createTpaRequest(requester, "tpa-simulation-source", "Acceptor", "server-beta", now);
        tpaService.acceptTpa(acceptor, "Requester", now.plusSeconds(1));

        InMemoryTicketRepository tickets = new InMemoryTicketRepository();
        RecordingGateway gateway = new RecordingGateway();
        ClusterTeleportService sourceCluster = new ClusterTeleportService("server-alpha", directory, tickets, gateway,
            new RecordingLocalTeleport());
        AcceptedTpaSourceDispatcher dispatcher = new AcceptedTpaSourceDispatcher(tpaService);
        TeleportActor movedRequester = actor("requester-uuid", "Requester", "server-beta", 32, 75, 32);

        assertTrue(dispatcher.dispatchAcceptedRequests(movedRequester, now.plusSeconds(2),
            new ClusterDispatchExecutor(sourceCluster)).isEmpty());
        assertEquals(0, gateway.calls);
        assertTrue(dispatcher.dispatchAcceptedRequests(requester, now.plusSeconds(30),
            new ClusterDispatchExecutor(sourceCluster)).isEmpty());
        assertEquals(0, gateway.calls);
    }

    private static InMemoryServerDirectory directory() {
        InMemoryServerDirectory directory = new InMemoryServerDirectory();
        directory.servers.put("server-alpha", new ServerDescriptor("server-alpha", "Alpha", null, true, true,
            Instant.now(), Instant.now()));
        directory.servers.put("server-beta", new ServerDescriptor("server-beta", "Beta", null, false, true,
            Instant.now(), Instant.now()));
        return directory;
    }

    private static TeleportActor actor(String uuid, String name, String serverId, double x, double y, double z) {
        return new TeleportActor(uuid, name, serverId, new TeleportTarget(serverId, 0, x, y, z, 0.0F, 0.0F));
    }

    private static final class ClusterDispatchExecutor implements AcceptedTpaSourceDispatcher.DispatchExecutor {

        private final ClusterTeleportService clusterTeleportService;

        private ClusterDispatchExecutor(ClusterTeleportService clusterTeleportService) {
            this.clusterTeleportService = clusterTeleportService;
        }

        @Override
        public GatewayDispatchResult dispatch(TeleportDispatchPlan plan) {
            return clusterTeleportService.dispatchTeleport(null, plan.getRequestId(), plan.getSubjectPlayerUuid(),
                plan.getSubjectPlayerName(), plan.getSourceServerId(), plan.getTeleportKind().name(), plan.getTarget());
        }
    }

    private static final class RecordingGateway implements GatewayAdapter {

        private int calls;
        private TransferTicket lastTicket;

        @Override
        public GatewayDispatchResult dispatchRemote(EntityPlayerMP livePlayer, TransferTicket ticket,
            ServerDescriptor targetServer) {
            calls++;
            lastTicket = ticket;
            return GatewayDispatchResult.pendingRemote("simulated connect",
                ticket.withStatus(TransferTicketStatus.DISPATCHED, "simulated connect", Instant.now()));
        }
    }

    private static final class RecordingLocalTeleport implements LocalTeleportExecutor {

        private TeleportTarget lastTarget;

        @Override
        public void teleport(EntityPlayerMP player, TeleportTarget target) {
            lastTarget = target;
        }
    }

    private static final class InMemoryServerDirectory implements ServerDirectory {

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

    private static final class InMemoryTicketRepository implements TeleportTicketRepository {

        private final Map<String, TransferTicket> ticketsByRequestId = new HashMap<String, TransferTicket>();

        @Override
        public TransferTicket save(TransferTicket ticket) {
            ticketsByRequestId.put(ticket.getRequestId(), ticket);
            return ticket;
        }

        @Override
        public TransferTicket update(TransferTicket ticket) {
            ticketsByRequestId.put(ticket.getRequestId(), ticket);
            return ticket;
        }

        @Override
        public Optional<TransferTicket> findByRequestId(String requestId) {
            return Optional.ofNullable(ticketsByRequestId.get(requestId));
        }

        @Override
        public Optional<TransferTicket> findActiveForTargetPlayer(String targetServerId, String playerUuid,
            Instant now) {
            for (TransferTicket ticket : ticketsByRequestId.values()) {
                if (ticket.getTarget().getServerId().equals(targetServerId) && ticket.getPlayerUuid().equals(playerUuid)
                    && ticket.getStatus() == TransferTicketStatus.DISPATCHED && ticket.isActiveAt(now)) {
                    return Optional.of(ticket);
                }
            }
            return Optional.empty();
        }

        @Override
        public List<TransferTicket> findRecentForPlayer(String playerUuid, int limit) {
            List<TransferTicket> result = new ArrayList<TransferTicket>();
            for (TransferTicket ticket : ticketsByRequestId.values()) {
                if (ticket.getPlayerUuid().equals(playerUuid) && result.size() < limit) {
                    result.add(ticket);
                }
            }
            return result;
        }

        @Override
        public int expireActiveTickets(Instant now) {
            int expired = 0;
            for (TransferTicket ticket : new ArrayList<TransferTicket>(ticketsByRequestId.values())) {
                if ((ticket.getStatus() == TransferTicketStatus.PENDING_GATEWAY
                    || ticket.getStatus() == TransferTicketStatus.DISPATCHED) && !ticket.getExpiresAt().isAfter(now)) {
                    update(ticket.withStatus(TransferTicketStatus.EXPIRED, "simulation expired", now));
                    expired++;
                }
            }
            return expired;
        }
    }

    private static final class InMemoryPlayerTeleportRepository implements PlayerTeleportRepository {

        private final Map<String, PlayerHome> homes = new HashMap<String, PlayerHome>();
        private final Map<String, BackRecord> backRecords = new HashMap<String, BackRecord>();
        private final Map<String, ServerWarp> warps = new HashMap<String, ServerWarp>();
        private final List<TpaRequest> tpaRequests = new ArrayList<TpaRequest>();

        @Override
        public List<PlayerHome> listHomes(String playerUuid) {
            return new ArrayList<PlayerHome>(homes.values());
        }

        @Override
        public Optional<PlayerHome> findHome(String playerUuid, String homeName) {
            return Optional.ofNullable(homes.get(playerUuid + ":" + homeName));
        }

        @Override
        public PlayerHome saveHome(PlayerHome playerHome) {
            homes.put(playerHome.getPlayerUuid() + ":" + playerHome.getHomeName(), playerHome);
            return playerHome;
        }

        @Override
        public boolean deleteHome(String playerUuid, String homeName) {
            return homes.remove(playerUuid + ":" + homeName) != null;
        }

        @Override
        public Optional<BackRecord> findBackRecord(String playerUuid) {
            return Optional.ofNullable(backRecords.get(playerUuid));
        }

        @Override
        public BackRecord saveBackRecord(BackRecord backRecord) {
            backRecords.put(backRecord.getPlayerUuid(), backRecord);
            return backRecord;
        }

        @Override
        public List<ServerWarp> listWarps() {
            return new ArrayList<ServerWarp>(warps.values());
        }

        @Override
        public Optional<ServerWarp> findWarp(String warpName) {
            return Optional.ofNullable(warps.get(warpName));
        }

        @Override
        public ServerWarp saveWarp(ServerWarp serverWarp) {
            warps.put(serverWarp.getWarpName(), serverWarp);
            return serverWarp;
        }

        @Override
        public TpaRequest saveTpaRequest(TpaRequest tpaRequest) {
            tpaRequests.add(tpaRequest);
            return tpaRequest;
        }

        @Override
        public TpaRequest updateTpaRequest(TpaRequest tpaRequest) {
            for (int index = 0; index < tpaRequests.size(); index++) {
                if (tpaRequests.get(index).getRequestId().equals(tpaRequest.getRequestId())) {
                    tpaRequests.set(index, tpaRequest);
                    return tpaRequest;
                }
            }
            tpaRequests.add(tpaRequest);
            return tpaRequest;
        }

        @Override
        public Optional<TpaRequest> findPendingTpaRequest(String requesterPlayerName, String targetPlayerName,
            String targetServerId, Instant now) {
            for (TpaRequest request : tpaRequests) {
                if (request.getStatus() == TpaRequestStatus.PENDING
                    && request.getRequesterPlayerName().equalsIgnoreCase(requesterPlayerName)
                    && request.getTargetPlayerName().equalsIgnoreCase(targetPlayerName)
                    && request.getTargetServerId().equals(targetServerId) && request.getExpiresAt().isAfter(now)) {
                    return Optional.of(request);
                }
            }
            return Optional.empty();
        }

        @Override
        public Optional<TpaRequest> acceptPendingTpaRequest(String requesterPlayerName, String targetPlayerName,
            String targetServerId, TeleportTarget acceptedTarget, Instant now) {
            Optional<TpaRequest> request = findPendingTpaRequest(requesterPlayerName, targetPlayerName, targetServerId,
                now);
            return request.isPresent() ? Optional.of(updateTpaRequest(request.get().withAcceptedTarget(acceptedTarget, now)))
                : Optional.<TpaRequest>empty();
        }

        @Override
        public Optional<TpaRequest> declinePendingTpaRequest(String requesterPlayerName, String targetPlayerName,
            String targetServerId, Instant now) {
            Optional<TpaRequest> request = findPendingTpaRequest(requesterPlayerName, targetPlayerName, targetServerId,
                now);
            return request.isPresent() ? Optional.of(updateTpaRequest(request.get().withStatus(TpaRequestStatus.DECLINED,
                now))) : Optional.<TpaRequest>empty();
        }

        @Override
        public Optional<TpaRequest> cancelPendingTpaRequest(String requesterPlayerUuid, String targetPlayerName,
            String targetServerId, Instant now) {
            for (TpaRequest request : tpaRequests) {
                if (request.getStatus() == TpaRequestStatus.PENDING
                    && request.getRequesterPlayerUuid().equals(requesterPlayerUuid)
                    && request.getTargetPlayerName().equalsIgnoreCase(targetPlayerName)
                    && request.getTargetServerId().equals(targetServerId) && request.getExpiresAt().isAfter(now)) {
                    return Optional.of(updateTpaRequest(request.withStatus(TpaRequestStatus.CANCELLED, now)));
                }
            }
            return Optional.empty();
        }

        @Override
        public List<TpaRequest> listPendingTpaRequestsForTarget(String targetServerId, String targetPlayerName,
            Instant now) {
            List<TpaRequest> result = new ArrayList<TpaRequest>();
            for (TpaRequest request : tpaRequests) {
                if (request.getStatus() == TpaRequestStatus.PENDING && request.getTargetServerId().equals(targetServerId)
                    && request.getTargetPlayerName().equalsIgnoreCase(targetPlayerName)
                    && request.getExpiresAt().isAfter(now)) {
                    result.add(request);
                }
            }
            return result;
        }

        @Override
        public List<TpaRequest> listAcceptedTpaRequestsForRequester(String requesterServerId,
            String requesterPlayerUuid, Instant now) {
            List<TpaRequest> result = new ArrayList<TpaRequest>();
            for (TpaRequest request : tpaRequests) {
                if (request.getStatus() == TpaRequestStatus.ACCEPTED
                    && request.getRequesterServerId().equals(requesterServerId)
                    && request.getRequesterPlayerUuid().equals(requesterPlayerUuid)
                    && request.getExpiresAt().isAfter(now)) {
                    result.add(request);
                }
            }
            return result;
        }

        @Override
        public List<TpaRequest> listRecentTpaRequestsForRequester(String requesterServerId,
            String requesterPlayerUuid, int limit) {
            List<TpaRequest> result = new ArrayList<TpaRequest>();
            for (TpaRequest request : tpaRequests) {
                if (request.getRequesterServerId().equals(requesterServerId)
                    && request.getRequesterPlayerUuid().equals(requesterPlayerUuid) && result.size() < limit) {
                    result.add(request);
                }
            }
            return result;
        }

        @Override
        public List<TpaRequest> listRecentTpaRequestsForTarget(String targetServerId, String targetPlayerName,
            int limit) {
            List<TpaRequest> result = new ArrayList<TpaRequest>();
            for (TpaRequest request : tpaRequests) {
                if (request.getTargetServerId().equals(targetServerId)
                    && request.getTargetPlayerName().equalsIgnoreCase(targetPlayerName) && result.size() < limit) {
                    result.add(request);
                }
            }
            return result;
        }

        @Override
        public int expirePendingTpaRequests(Instant now) {
            int expired = 0;
            for (TpaRequest request : new ArrayList<TpaRequest>(tpaRequests)) {
                if (request.getStatus() == TpaRequestStatus.PENDING && !request.getExpiresAt().isAfter(now)) {
                    updateTpaRequest(request.withStatus(TpaRequestStatus.EXPIRED, now));
                    expired++;
                }
            }
            return expired;
        }

        @Override
        public RandomTeleportRecord saveRandomTeleportRecord(RandomTeleportRecord randomTeleportRecord) {
            return randomTeleportRecord;
        }
    }
}
