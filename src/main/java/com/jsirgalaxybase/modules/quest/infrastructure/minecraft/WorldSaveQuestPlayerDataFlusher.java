package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import net.minecraft.entity.player.EntityPlayerMP;

/** Uses the world's public player file contract so inventory and marker reach disk before the saga advances. */
public final class WorldSaveQuestPlayerDataFlusher implements QuestPlayerDataFlusher {
    @Override
    public void flush(EntityPlayerMP player) {
        player.worldObj.getSaveHandler().getSaveHandler().writePlayerData(player);
    }
}
