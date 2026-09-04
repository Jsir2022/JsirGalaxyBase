package com.jsirgalaxybase.modules.servertools.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import com.jsirgalaxybase.modules.servertools.domain.TargetServerRtpProfile;
import com.jsirgalaxybase.modules.servertools.domain.TpaRequest;
import com.jsirgalaxybase.modules.servertools.domain.TpaRequestStatus;
import com.jsirgalaxybase.modules.servertools.port.PlayerPermissionPolicy;
import com.jsirgalaxybase.modules.servertools.port.PlayerTeleportRepository;

public class PlayerTeleportService {

    private static final long DEFAULT_TPA_TIMEOUT_SECONDS = 30L;

    private final PlayerTeleportRepository repository;
    private final PlayerPermissionPolicy permissionPolicy;
    private final ServerDirectory serverDirectory;

    public PlayerTeleportService(PlayerTeleportRepository repository, PlayerPermissionPolicy permissionPolicy) {
        this(repository, permissionPolicy, null);
    }

    public PlayerTeleportService(PlayerTeleportRepository repository, PlayerPermissionPolicy permissionPolicy,
        ServerDirectory serverDirectory) {
        this.repository = repository;
        this.permissionPolicy = permissionPolicy;
        this.serverDirectory = serverDirectory;
    }

    public PlayerHome setHome(TeleportActor actor, String homeName) {
        String normalizedName = normalizeKey(homeName, "homeName");
        Optional<PlayerHome> existing = repository.findHome(actor.getPlayerUuid(), normalizedName);
        List<PlayerHome> homes = repository.listHomes(actor.getPlayerUuid());
        if (!existing.isPresent() && homes.size() >= permissionPolicy.getMaxHomes(actor)) {
            throw new ServerToolsException("Home limit reached for this player");
        }
        Instant now = Instant.now();
        return repository.saveHome(new PlayerHome(actor.getPlayerUuid(), normalizedName, actor.getCurrentLocation(),
            existing.isPresent() ? existing.get().getCreatedAt() : now, now));
    }

    public List<PlayerHome> listHomes(String playerUuid) {
        return repository.listHomes(requireText(playerUuid, "playerUuid"));
    }

    public boolean deleteHome(String playerUuid, String homeName) {
        return repository.deleteHome(requireText(playerUuid, "playerUuid"), normalizeKey(homeName, "homeName"));
    }

    public List<ServerWarp> listWarps() {
        return repository.listWarps();
    }

    public TeleportDispatchPlan prepareHomeTeleport(TeleportActor actor, String requestId, String homeName) {
        PlayerHome home = repository.findHome(actor.getPlayerUuid(), normalizeKey(homeName, "homeName")).orElse(null);
        if (home == null) {
            throw newNotFound("Home not found: " + homeName);
        }
        permissionPolicy.validateTeleport(actor, TeleportKind.HOME, home.getTarget());
        saveBackRecord(actor, TeleportKind.HOME, actor.getCurrentLocation(), Instant.now());
        return buildPlan(requestId, actor.getPlayerUuid(), actor.getPlayerName(), actor.getSourceServerId(),
            TeleportKind.HOME, home.getTarget());
    }

    public TeleportDispatchPlan prepareWarpTeleport(TeleportActor actor, String requestId, String warpName) {
        ServerWarp warp = repository.findWarp(normalizeKey(warpName, "warpName")).orElse(null);
        if (warp == null) {
            throw newNotFound("Warp not found: " + warpName);
        }
        if (!warp.isEnabled()) {
            throw new ServerToolsException("Warp is disabled: " + warpName);
        }
        permissionPolicy.validateWarp(actor, warp);
        permissionPolicy.validateTeleport(actor, TeleportKind.WARP, warp.getTarget());
        saveBackRecord(actor, TeleportKind.WARP, actor.getCurrentLocation(), Instant.now());
        return buildPlan(requestId, actor.getPlayerUuid(), actor.getPlayerName(), actor.getSourceServerId(),
            TeleportKind.WARP, warp.getTarget());
    }

    public TeleportDispatchPlan prepareSpawnTeleport(TeleportActor actor, String requestId, TeleportTarget spawnTarget) {
        permissionPolicy.validateTeleport(actor, TeleportKind.SPAWN, spawnTarget);
        saveBackRecord(actor, TeleportKind.SPAWN, actor.getCurrentLocation(), Instant.now());
        return buildPlan(requestId, actor.getPlayerUuid(), actor.getPlayerName(), actor.getSourceServerId(),
            TeleportKind.SPAWN, spawnTarget);
    }

    public TeleportDispatchPlan prepareBackTeleport(TeleportActor actor, String requestId) {
        BackRecord backRecord = repository.findBackRecord(actor.getPlayerUuid()).orElse(null);
        if (backRecord == null) {
            throw newNotFound("No back record is available");
        }
        permissionPolicy.validateTeleport(actor, TeleportKind.BACK, backRecord.getTarget());
        saveBackRecord(actor, TeleportKind.BACK, actor.getCurrentLocation(), Instant.now());
        return buildPlan(requestId, actor.getPlayerUuid(), actor.getPlayerName(), actor.getSourceServerId(),
            TeleportKind.BACK, backRecord.getTarget());
    }

    public TpaRequest createTpaRequest(TeleportActor requester, String requestId, String targetPlayerName,
        String targetServerId, Instant now) {
        String normalizedTargetName = normalizePlayerName(targetPlayerName);
        String normalizedTargetServer = requireText(targetServerId, "targetServerId").trim();
        validateTargetServer(normalizedTargetServer);
        if (normalizedTargetServer.equals(requester.getSourceServerId())
            && normalizedTargetName.equalsIgnoreCase(requester.getPlayerName())) {
            throw new ServerToolsException("Cannot create a TPA request to yourself");
        }
        repository.expirePendingTpaRequests(now);
        Optional<TpaRequest> existing = repository.findPendingTpaRequest(requester.getPlayerName(), normalizedTargetName,
            normalizedTargetServer, now);
        if (existing.isPresent()) {
            return existing.get();
        }
        TpaRequest request = new TpaRequest(requireText(requestId, "requestId"), requester.getPlayerUuid(),
            requester.getPlayerName(), requester.getSourceServerId(), requester.getCurrentLocation(),
            normalizedTargetName, normalizedTargetServer, TpaRequestStatus.PENDING, now,
            now.plusSeconds(DEFAULT_TPA_TIMEOUT_SECONDS), now);
        return repository.saveTpaRequest(request);
    }

    public TpaRequest acceptTpa(TeleportActor acceptor, String requesterPlayerName, Instant now) {
        repository.expirePendingTpaRequests(now);
        TpaRequest request = repository.acceptPendingTpaRequest(normalizePlayerName(requesterPlayerName),
            acceptor.getPlayerName(), acceptor.getSourceServerId(), acceptor.getCurrentLocation(), now).orElse(null);
        if (request == null) {
            throw newNotFound("No pending TPA request from " + requesterPlayerName + " is available on this server");
        }
        return request;
    }

    public TpaRequest declineTpa(TeleportActor decliner, String requesterPlayerName, Instant now) {
        repository.expirePendingTpaRequests(now);
        TpaRequest request = repository.declinePendingTpaRequest(normalizePlayerName(requesterPlayerName),
            decliner.getPlayerName(), decliner.getSourceServerId(), now).orElse(null);
        if (request == null) {
            throw newNotFound("No pending TPA request from " + requesterPlayerName + " is available on this server");
        }
        return request;
    }

    public TpaRequest cancelTpa(TeleportActor requester, String targetPlayerName, String targetServerId, Instant now) {
        repository.expirePendingTpaRequests(now);
        TpaRequest request = repository.cancelPendingTpaRequest(requester.getPlayerUuid(),
            normalizePlayerName(targetPlayerName), requireText(targetServerId, "targetServerId").trim(), now)
            .orElse(null);
        if (request == null) {
            throw newNotFound("No pending TPA request for " + targetPlayerName + " is available");
        }
        return request;
    }

    public List<TpaRequest> listPendingTpaRequestsForTarget(String targetServerId, String targetPlayerName,
        Instant now) {
        return repository.listPendingTpaRequestsForTarget(requireText(targetServerId, "targetServerId"),
            normalizePlayerName(targetPlayerName), now);
    }

    public List<TpaRequest> listRecentTpaRequestsForRequester(String requesterServerId, String requesterPlayerUuid,
        int limit) {
        return repository.listRecentTpaRequestsForRequester(requireText(requesterServerId, "requesterServerId"),
            requireText(requesterPlayerUuid, "requesterPlayerUuid"), limit);
    }

    public List<TpaRequest> listRecentTpaRequestsForTarget(String targetServerId, String targetPlayerName, int limit) {
        return repository.listRecentTpaRequestsForTarget(requireText(targetServerId, "targetServerId"),
            normalizePlayerName(targetPlayerName), limit);
    }

    public TeleportDispatchPlan prepareAcceptedTpaTeleport(TeleportActor requester, TpaRequest request, Instant now) {
        if (request == null || request.getStatus() != TpaRequestStatus.ACCEPTED
            || request.getAcceptedTarget() == null || !request.getExpiresAt().isAfter(now)
            || !requester.getPlayerUuid().equals(request.getRequesterPlayerUuid())
            || !requester.getSourceServerId().equals(request.getRequesterServerId())) {
            throw new ServerToolsException("Accepted TPA request is no longer dispatchable");
        }
        saveBackRecord(requester, TeleportKind.TPA, requester.getCurrentLocation(), now);
        return buildPlan(tpaDispatchRequestId(request), requester.getPlayerUuid(), requester.getPlayerName(),
            requester.getSourceServerId(), TeleportKind.TPA, request.getAcceptedTarget());
    }

    public List<TpaRequest> listAcceptedTpaRequestsForRequester(String requesterServerId, String requesterPlayerUuid,
        Instant now) {
        return repository.listAcceptedTpaRequestsForRequester(requireText(requesterServerId, "requesterServerId"),
            requireText(requesterPlayerUuid, "requesterPlayerUuid"), now);
    }

    public int expirePendingTpaRequests(Instant now) {
        return repository.expirePendingTpaRequests(now);
    }

    public static String tpaDispatchRequestId(TpaRequest request) {
        return "tpa-dispatch-" + request.getRequestId();
    }

    public TeleportDispatchPlan prepareRandomTeleport(TeleportActor actor, String requestId, TeleportTarget target,
        Instant now) {
        permissionPolicy.validateTeleport(actor, TeleportKind.RTP, target);
        saveBackRecord(actor, TeleportKind.RTP, actor.getCurrentLocation(), now);
        repository.saveRandomTeleportRecord(new RandomTeleportRecord(0L, requestId, actor.getPlayerUuid(),
            actor.getSourceServerId(), target, now));
        return buildPlan(requestId, actor.getPlayerUuid(), actor.getPlayerName(), actor.getSourceServerId(),
            TeleportKind.RTP, target);
    }

    /**
     * Creates only the cross-server ticket plan. The target server selects and
     * audits the actual safe block after the player arrives; source servers
     * never read or load a remote world to fake that decision.
     */
    public TeleportDispatchPlan prepareTargetServerRandomTeleport(TeleportActor actor, String requestId,
        TargetServerRtpProfile profile, Instant now) {
        if (profile == null) throw new IllegalArgumentException("target RTP profile must not be null");
        validateTargetServer(profile.getServerId());
        TeleportTarget target = profile.getFallbackTarget();
        permissionPolicy.validateTeleport(actor, TeleportKind.RTP, target);
        saveBackRecord(actor, TeleportKind.RTP, actor.getCurrentLocation(), now);
        return buildPlan(requestId, actor.getPlayerUuid(), actor.getPlayerName(), actor.getSourceServerId(),
            TeleportKind.RTP, target);
    }

    public RandomTeleportRecord recordResolvedTargetServerRandomTeleport(String requestId, String playerUuid,
        String sourceServerId, TeleportTarget resolvedTarget, Instant now) {
        if (resolvedTarget == null) throw new IllegalArgumentException("resolved target must not be null");
        return repository.saveRandomTeleportRecord(new RandomTeleportRecord(0L, requireText(requestId, "requestId"),
            requireText(playerUuid, "playerUuid"), requireText(sourceServerId, "sourceServerId"), resolvedTarget, now));
    }

    private void saveBackRecord(TeleportActor actor, TeleportKind teleportKind, TeleportTarget target, Instant now) {
        saveBackRecord(actor.getPlayerUuid(), teleportKind, target, now);
    }

    private void saveBackRecord(String playerUuid, TeleportKind teleportKind, TeleportTarget target, Instant now) {
        repository.saveBackRecord(new BackRecord(playerUuid, teleportKind, target, now));
    }

    private TeleportDispatchPlan buildPlan(String requestId, String subjectPlayerUuid, String subjectPlayerName,
        String sourceServerId, TeleportKind teleportKind, TeleportTarget target) {
        return new TeleportDispatchPlan(requireText(requestId, "requestId"), requireText(subjectPlayerUuid,
            "subjectPlayerUuid"), requireText(subjectPlayerName, "subjectPlayerName"),
            requireText(sourceServerId, "sourceServerId"), teleportKind, target);
    }

    private String normalizeKey(String raw, String fieldName) {
        return requireText(raw, fieldName).trim().toLowerCase();
    }

    private String normalizePlayerName(String raw) {
        return requireText(raw, "playerName").trim();
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private ServerToolsException newNotFound(final String message) {
        return new ServerToolsException(message);
    }

    private void validateTargetServer(String targetServerId) {
        if (serverDirectory == null) {
            return;
        }
        ServerDescriptor serverDescriptor = serverDirectory.findById(targetServerId).orElse(null);
        if (serverDescriptor == null) {
            throw new ServerToolsException("Unknown target server: " + targetServerId);
        }
        if (!serverDescriptor.isEnabled()) {
            throw new ServerToolsException("Target server is disabled: " + targetServerId);
        }
    }

    public static String newRequestId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString();
    }
}
