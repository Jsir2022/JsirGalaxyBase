package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.util.List;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.modules.quest.application.TrackedQuestSnapshotFactory;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.terminal.network.TerminalNetwork;
import com.jsirgalaxybase.terminal.network.TrackedQuestSnapshotMessage;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/** Low-frequency Base-owned synchronization for the small tracked-quest HUD projection. */
public final class TrackedQuestSyncController {
    private static final int REFRESH_TICKS=100;
    private final TrackedQuestSnapshotFactory snapshots;private int remaining=REFRESH_TICKS;
    public TrackedQuestSyncController(TrackedQuestSnapshotFactory snapshots){if(snapshots==null)throw new IllegalArgumentException("snapshots are required");this.snapshots=snapshots;}
    @SubscribeEvent public void onLogin(PlayerLoggedInEvent event){if(event.player instanceof EntityPlayerMP)send((EntityPlayerMP)event.player);}
    @SubscribeEvent public void onServerTick(TickEvent.ServerTickEvent event){if(event.phase!=TickEvent.Phase.END||--remaining>0)return;remaining=REFRESH_TICKS;MinecraftServer server=MinecraftServer.getServer();if(server==null||server.getConfigurationManager()==null)return;List<EntityPlayerMP> players=server.getConfigurationManager().playerEntityList;for(EntityPlayerMP player:players)if(player!=null)send(player);}
    private void send(EntityPlayerMP player){try{TerminalNetwork.CHANNEL.sendTo(new TrackedQuestSnapshotMessage(snapshots.create(ParticipantId.player(player.getUniqueID()),System.currentTimeMillis())),player);}catch(RuntimeException failure){GalaxyBase.LOG.error("Unable to refresh tracked quests for {}",player.getCommandSenderName(),failure);}}
}
