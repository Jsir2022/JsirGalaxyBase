package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.util.UUID;

import com.jsirgalaxybase.quest.core.QuestOnlinePlayerDirectory;

/** Reads only the authoritative online player list of this Minecraft server instance. */
public final class MinecraftQuestOnlinePlayerDirectory implements QuestOnlinePlayerDirectory {
    private final OnlineServerQuestPlayerResolver players;

    public MinecraftQuestOnlinePlayerDirectory() {
        this(new OnlineServerQuestPlayerResolver());
    }

    MinecraftQuestOnlinePlayerDirectory(OnlineServerQuestPlayerResolver players) {
        if (players == null) throw new IllegalArgumentException("online player resolver is required");
        this.players = players;
    }

    @Override
    public boolean isOnline(UUID playerId) {
        return playerId != null && players.findOnline(playerId) != null;
    }
}
