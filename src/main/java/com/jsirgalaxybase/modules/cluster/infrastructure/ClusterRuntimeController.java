package com.jsirgalaxybase.modules.cluster.infrastructure;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.jsirgalaxybase.modules.cluster.application.PlayerArrivalRestoreService;

import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ClusterRuntimeController {

    private static final int RESTORE_SWEEP_INTERVAL_TICKS = 20;

    private final PlayerArrivalRestoreService playerArrivalRestoreService;
    private int nextSweep = RESTORE_SWEEP_INTERVAL_TICKS;

    public ClusterRuntimeController(PlayerArrivalRestoreService playerArrivalRestoreService) {
        this.playerArrivalRestoreService = playerArrivalRestoreService;
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            playerArrivalRestoreService.tryRestorePlayer((EntityPlayerMP) event.player, "login");
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
        nextSweep = RESTORE_SWEEP_INTERVAL_TICKS;
        playerArrivalRestoreService.expireActiveTickets();
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) {
            return;
        }
        List<EntityPlayerMP> players = server.getConfigurationManager().playerEntityList;
        for (EntityPlayerMP player : players) {
            if (player != null) {
                playerArrivalRestoreService.tryRestorePlayer(player, "tick");
            }
        }
    }
}