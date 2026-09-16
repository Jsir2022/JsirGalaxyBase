package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import com.jsirgalaxybase.quest.core.CommandRewardExecutor;
import com.jsirgalaxybase.quest.core.CommandRewardInvocation;
import com.jsirgalaxybase.quest.core.RewardDeliveryOutcome;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

/** Entitlement-marked command execution. Only commands admitted by the separate policy reach this class. */
public final class MinecraftCommandRewardExecutor implements CommandRewardExecutor {
    private final QuestPlayerResolver players;private final QuestPlayerDataFlusher flusher;
    public MinecraftCommandRewardExecutor(QuestPlayerResolver players,QuestPlayerDataFlusher flusher){if(players==null||flusher==null)throw new IllegalArgumentException("players and flusher are required");this.players=players;this.flusher=flusher;}
    @Override public RewardDeliveryOutcome execute(CommandRewardInvocation invocation){EntityPlayerMP player=players.findOnline(invocation.getParticipantId().getId());
        if(player==null)return RewardDeliveryOutcome.deferred("player-offline");String marker=QuestRewardMarker.key(invocation.getEntitlementKey());
        NBTTagCompound persisted=QuestRewardMarker.persisted(player);NBTTagCompound markers=persisted.getCompoundTag(QuestRewardMarker.ROOT);
        if(markers.getBoolean(marker))return RewardDeliveryOutcome.delivered();MinecraftServer server=MinecraftServer.getServer();if(server==null||server.getCommandManager()==null)return RewardDeliveryOutcome.failed("server-command-manager-unavailable",true);
        ICommandSender sender=invocation.isViaPlayer()?player:server;try{server.getCommandManager().executeCommand(sender,invocation.getCommand());markers.setBoolean(marker,true);persisted.setTag(QuestRewardMarker.ROOT,markers);flusher.flush(player);return RewardDeliveryOutcome.delivered();}
        catch(RuntimeException failure){return RewardDeliveryOutcome.failed("command-failed:"+failure.getClass().getSimpleName(),true);}}
}
