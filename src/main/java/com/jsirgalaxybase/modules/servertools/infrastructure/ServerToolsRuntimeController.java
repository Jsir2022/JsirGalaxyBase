package com.jsirgalaxybase.modules.servertools.infrastructure;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.servertools.ServerToolsModule;
import com.jsirgalaxybase.modules.servertools.application.AcceptedTpaSourceDispatcher;
import com.jsirgalaxybase.modules.servertools.application.PlayerTeleportService;
import com.jsirgalaxybase.modules.servertools.domain.TeleportActor;
import com.jsirgalaxybase.modules.servertools.domain.TeleportDispatchPlan;
import com.jsirgalaxybase.modules.servertools.domain.TpaRequest;
import com.jsirgalaxybase.modules.servertools.domain.TpaRequestStatus;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Delivers durable TPA state and dispatches accepted cross-server requests from their source server. */
public class ServerToolsRuntimeController {

    private static final int SWEEP_INTERVAL_TICKS = 20;

    private final ServerToolsModule module;
    private final AcceptedTpaSourceDispatcher acceptedTpaSourceDispatcher;
    private final Set<String> deliveredNotifications = Collections.synchronizedSet(new HashSet<String>());
    private int nextSweep = SWEEP_INTERVAL_TICKS;

    public ServerToolsRuntimeController(ServerToolsModule module) {
        this.module = module;
        acceptedTpaSourceDispatcher = new AcceptedTpaSourceDispatcher(module.getPlayerTeleportService());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            sweepPlayer((EntityPlayerMP) event.player, Instant.now());
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        nextSweep--;
        if (nextSweep > 0) {
            return;
        }
        nextSweep = SWEEP_INTERVAL_TICKS;
        if (!module.isRuntimeAvailable()) {
            return;
        }
        Instant now = Instant.now();
        module.getPlayerTeleportService().expirePendingTpaRequests(now);
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) {
            return;
        }
        List<EntityPlayerMP> players = server.getConfigurationManager().playerEntityList;
        for (EntityPlayerMP player : players) {
            if (player != null) {
                sweepPlayer(player, now);
            }
        }
    }

    private void sweepPlayer(EntityPlayerMP player, Instant now) {
        if (!module.isRuntimeAvailable()) {
            return;
        }
        PlayerTeleportService service = module.getPlayerTeleportService();
        TeleportActor actor = module.captureActor(player);
        notifyIncoming(player, service.listPendingTpaRequestsForTarget(actor.getSourceServerId(), actor.getPlayerName(), now));
        notifyRequesterResults(player, service.listRecentTpaRequestsForRequester(actor.getSourceServerId(),
            actor.getPlayerUuid(), 10));
        dispatchAcceptedRequests(player, actor, now);
    }

    private void notifyIncoming(EntityPlayerMP player, List<TpaRequest> requests) {
        for (TpaRequest request : requests) {
            if (markDelivered("incoming", request)) {
                player.addChatComponentMessage(new ChatComponentTranslation("jsirgalaxybase.servertools.tpa.incoming",
                    request.getRequesterPlayerName(), request.getRequesterServerId(), request.getRequesterPlayerName(),
                    request.getRequesterPlayerName()));
            }
        }
    }

    private void notifyRequesterResults(EntityPlayerMP player, List<TpaRequest> requests) {
        for (TpaRequest request : requests) {
            if (request.getStatus() == TpaRequestStatus.PENDING || request.getStatus() == TpaRequestStatus.ACCEPTED
                || !markDelivered("result", request)) {
                continue;
            }
            String key = "jsirgalaxybase.servertools.tpa.result." + request.getStatus().name().toLowerCase();
            player.addChatComponentMessage(new ChatComponentTranslation(key, request.getTargetPlayerName(),
                request.getTargetServerId()));
        }
    }

    private void dispatchAcceptedRequests(final EntityPlayerMP player, final TeleportActor actor, Instant now) {
        List<AcceptedTpaSourceDispatcher.DispatchAttempt> attempts = acceptedTpaSourceDispatcher
            .dispatchAcceptedRequests(actor, now, new AcceptedTpaSourceDispatcher.DispatchExecutor() {

                @Override
                public GatewayDispatchResult dispatch(TeleportDispatchPlan plan) {
                    return module.dispatchTeleport(player, plan);
                }
            });
        for (AcceptedTpaSourceDispatcher.DispatchAttempt attempt : attempts) {
            if (!attempt.isFailed()) {
                continue;
            }
            RuntimeException exception = attempt.getFailure();
            if (exception != null) {
                GalaxyBase.LOG.warn("Accepted TPA dispatch failed. requestId={}, playerUuid={}, sourceServerId={}",
                    attempt.getRequest().getRequestId(), actor.getPlayerUuid(), actor.getSourceServerId(), exception);
            }
            player.addChatComponentMessage(new ChatComponentTranslation(
                "jsirgalaxybase.servertools.tpa.result.dispatch_failed"));
        }
    }

    private boolean markDelivered(String kind, TpaRequest request) {
        return deliveredNotifications.add(kind + ":" + request.getRequestId() + ":" + request.getStatus().name());
    }
}
