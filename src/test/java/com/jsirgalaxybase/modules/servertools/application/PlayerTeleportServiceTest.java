package com.jsirgalaxybase.modules.servertools.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.jsirgalaxybase.modules.cluster.domain.ServerDescriptor;
import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.cluster.port.ServerDirectory;
import com.jsirgalaxybase.modules.servertools.domain.BackRecord;
import com.jsirgalaxybase.modules.servertools.domain.PlayerHome;
import com.jsirgalaxybase.modules.servertools.domain.RandomTeleportRecord;
import com.jsirgalaxybase.modules.servertools.domain.ServerWarp;
import com.jsirgalaxybase.modules.servertools.domain.TeleportActor;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;
import com.jsirgalaxybase.modules.servertools.domain.TeleportKind;
import com.jsirgalaxybase.modules.servertools.domain.TpaRequest;
import com.jsirgalaxybase.modules.servertools.domain.TpaRequestStatus;
import com.jsirgalaxybase.modules.servertools.infrastructure.AllowAllPlayerPermissionPolicy;
import com.jsirgalaxybase.modules.servertools.port.PlayerTeleportRepository;

public class PlayerTeleportServiceTest {

    @Test
    public void prepareHomeTeleportCreatesBackRecordAndUsesStoredTarget() {
        FakePlayerTeleportRepository repository = new FakePlayerTeleportRepository();
        PlayerTeleportService service = new PlayerTeleportService(repository, new AllowAllPlayerPermissionPolicy(3));
        TeleportActor actor = actor("player-a-uuid", "PlayerA", "server-alpha", target("server-alpha", 0, 10, 65, 10));
        repository.saveHome(new PlayerHome(actor.getPlayerUuid(), "mine",
            target("server-beta", 2, 120, 75, 300), Instant.now(), Instant.now()));

        TeleportDispatchPlan plan = service.prepareHomeTeleport(actor, "req-home-1", "mine");

        assertEquals(TeleportKind.HOME, plan.getTeleportKind());
        assertEquals("server-beta", plan.getTarget().getServerId());
        assertEquals(actor.getCurrentLocation().getX(), repository.backRecords.get(actor.getPlayerUuid()).getTarget().getX(),
            0.0001D);
    }

    @Test
    public void prepareWarpTeleportSupportsCrossServerWarpTargets() {
        FakePlayerTeleportRepository repository = new FakePlayerTeleportRepository();
        PlayerTeleportService service = new PlayerTeleportService(repository, new AllowAllPlayerPermissionPolicy());
        TeleportActor actor = actor("player-b-uuid", "PlayerB", "server-alpha", target("server-alpha", 0, 0, 70, 0));
        repository.saveWarp(new ServerWarp("hub", "Hub", "Cluster lobby", target("server-hub", 0, 5, 80, 5), true,
            Instant.now(), Instant.now()));

        TeleportDispatchPlan plan = service.prepareWarpTeleport(actor, "req-warp-1", "hub");

        assertEquals(TeleportKind.WARP, plan.getTeleportKind());
        assertEquals("server-hub", plan.getTarget().getServerId());
        assertEquals("player-b-uuid", plan.getSubjectPlayerUuid());
    }

    @Test
    public void acceptTpaUsesRequesterOriginAsBackRecordAndTargetsAcceptorLocation() {
        FakePlayerTeleportRepository repository = new FakePlayerTeleportRepository();
        PlayerTeleportService service = new PlayerTeleportService(repository, new AllowAllPlayerPermissionPolicy());
        Instant now = Instant.now();
        repository.saveTpaRequest(new TpaRequest("req-tpa-create", "requester-uuid", "Requester", "server-alpha",
            target("server-alpha", 0, 1, 65, 1), "Acceptor", "server-beta", TpaRequestStatus.PENDING, now,
            now.plusSeconds(30), now));

        TeleportActor acceptor = actor("acceptor-uuid", "Acceptor", "server-beta",
            target("server-beta", 7, 200, 90, 200));
        TeleportDispatchPlan plan = service.acceptTpa(acceptor, "req-tpa-accept", "Requester", now.plusSeconds(5));

        assertEquals(TeleportKind.TPA, plan.getTeleportKind());
        assertEquals("requester-uuid", plan.getSubjectPlayerUuid());
        assertEquals("server-alpha", plan.getSourceServerId());
        assertEquals(acceptor.getCurrentLocation().getX(), plan.getTarget().getX(), 0.0001D);
        assertEquals(TpaRequestStatus.ACCEPTED, repository.updatedRequest.getStatus());
        assertEquals("server-alpha", repository.backRecords.get("requester-uuid").getTarget().getServerId());
    }

    @Test
    public void prepareRandomTeleportPersistsRtpRecord() {
        FakePlayerTeleportRepository repository = new FakePlayerTeleportRepository();
        PlayerTeleportService service = new PlayerTeleportService(repository, new AllowAllPlayerPermissionPolicy());
        TeleportActor actor = actor("player-c-uuid", "PlayerC", "server-alpha", target("server-alpha", 0, 30, 70, 30));
        TeleportTarget randomTarget = target("server-alpha", 0, 500, 72, -300);

        TeleportDispatchPlan plan = service.prepareRandomTeleport(actor, "req-rtp-1", randomTarget, Instant.now());

        assertEquals(TeleportKind.RTP, plan.getTeleportKind());
        assertEquals(1, repository.rtpRecords.size());
        assertEquals(randomTarget.getX(), repository.rtpRecords.get(0).getTarget().getX(), 0.0001D);
    }

    @Test
    public void createTpaRequestRejectsUnknownTargetServerWithoutPersistingRequest() {
        FakePlayerTeleportRepository repository = new FakePlayerTeleportRepository();
        FakeServerDirectory serverDirectory = new FakeServerDirectory();
        serverDirectory.servers.put("server-alpha", new ServerDescriptor("server-alpha", "Alpha", null, true, true,
            Instant.now(), Instant.now()));
        PlayerTeleportService service = new PlayerTeleportService(repository, new AllowAllPlayerPermissionPolicy(),
            serverDirectory);
        TeleportActor actor = actor("player-d-uuid", "PlayerD", "server-alpha", target("server-alpha", 0, 1, 70, 1));

        try {
            service.createTpaRequest(actor, "req-tpa-invalid", "Target", "missing-server", Instant.now());
        } catch (ServerToolsException expected) {
            assertEquals(0, repository.savedTpaRequestCount);
            assertTrue(expected.getMessage().contains("Unknown target server"));
            return;
        }

        throw new AssertionError("Expected ServerToolsException for unknown target server");
    }

    @Test
    public void createTpaRequestRejectsDisabledTargetServerWithoutPersistingRequest() {
        FakePlayerTeleportRepository repository = new FakePlayerTeleportRepository();
        FakeServerDirectory serverDirectory = new FakeServerDirectory();
        serverDirectory.servers.put("server-alpha", new ServerDescriptor("server-alpha", "Alpha", null, true, true,
            Instant.now(), Instant.now()));
        serverDirectory.servers.put("server-disabled", new ServerDescriptor("server-disabled", "Disabled", null,
            false, false, Instant.now(), Instant.now()));
        PlayerTeleportService service = new PlayerTeleportService(repository, new AllowAllPlayerPermissionPolicy(),
            serverDirectory);
        TeleportActor actor = actor("player-e-uuid", "PlayerE", "server-alpha", target("server-alpha", 0, 1, 70, 1));

        try {
            service.createTpaRequest(actor, "req-tpa-disabled", "Target", "server-disabled", Instant.now());
        } catch (ServerToolsException expected) {
            assertEquals(0, repository.savedTpaRequestCount);
            assertTrue(expected.getMessage().contains("disabled"));
            return;
        }

        throw new AssertionError("Expected ServerToolsException for disabled target server");
    }

    private TeleportActor actor(String uuid, String playerName, String serverId, TeleportTarget location) {
        return new TeleportActor(uuid, playerName, serverId, location);
    }

    private TeleportTarget target(String serverId, int dimensionId, double x, double y, double z) {
        return new TeleportTarget(serverId, dimensionId, x, y, z, 0.0F, 0.0F);
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

    private static final class FakePlayerTeleportRepository implements PlayerTeleportRepository {

        private final Map<String, PlayerHome> homes = new HashMap<String, PlayerHome>();
        private final Map<String, BackRecord> backRecords = new HashMap<String, BackRecord>();
        private final Map<String, ServerWarp> warps = new HashMap<String, ServerWarp>();
        private final List<RandomTeleportRecord> rtpRecords = new ArrayList<RandomTeleportRecord>();
        private int savedTpaRequestCount;
        private TpaRequest pendingRequest;
        private TpaRequest updatedRequest;

        @Override
        public List<PlayerHome> listHomes(String playerUuid) {
            List<PlayerHome> list = new ArrayList<PlayerHome>();
            for (PlayerHome home : homes.values()) {
                if (home.getPlayerUuid().equals(playerUuid)) {
                    list.add(home);
                }
            }
            return list;
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
            return homes.remove(playerUuid + ":" + homeName.toLowerCase()) != null;
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
            savedTpaRequestCount++;
            pendingRequest = tpaRequest;
            return tpaRequest;
        }

        @Override
        public TpaRequest updateTpaRequest(TpaRequest tpaRequest) {
            updatedRequest = tpaRequest;
            pendingRequest = tpaRequest;
            return tpaRequest;
        }

        @Override
        public Optional<TpaRequest> findPendingTpaRequest(String requesterPlayerName, String targetPlayerName,
            String targetServerId, Instant now) {
            if (pendingRequest == null) {
                return Optional.empty();
            }
            boolean matches = pendingRequest.getStatus() == TpaRequestStatus.PENDING
                && pendingRequest.getRequesterPlayerName().equalsIgnoreCase(requesterPlayerName)
                && pendingRequest.getTargetPlayerName().equalsIgnoreCase(targetPlayerName)
                && pendingRequest.getTargetServerId().equals(targetServerId)
                && pendingRequest.getExpiresAt().isAfter(now);
            return matches ? Optional.of(pendingRequest) : Optional.<TpaRequest>empty();
        }

        @Override
        public int expirePendingTpaRequests(Instant now) {
            if (pendingRequest != null && pendingRequest.getStatus() == TpaRequestStatus.PENDING
                && !pendingRequest.getExpiresAt().isAfter(now)) {
                pendingRequest = pendingRequest.withStatus(TpaRequestStatus.EXPIRED, now);
                return 1;
            }
            return 0;
        }

        @Override
        public RandomTeleportRecord saveRandomTeleportRecord(RandomTeleportRecord randomTeleportRecord) {
            rtpRecords.add(randomTeleportRecord);
            return randomTeleportRecord;
        }
    }
}