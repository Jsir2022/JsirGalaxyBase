package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/** Looks up a player from the authoritative server list without accepting a client supplied name. */
public final class OnlineServerQuestPlayerResolver implements QuestPlayerResolver {
    @Override
    public EntityPlayerMP findOnline(UUID playerId) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) return null;
        for (Object value : server.getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) value;
            if (playerId.equals(player.getUniqueID())) return player;
        }
        return null;
    }
}
