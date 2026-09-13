package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import com.jsirgalaxybase.quest.core.QuestEditorActor;
import com.jsirgalaxybase.quest.core.QuestEditorAuthorization;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

/** Resolves editor authority from the live authenticated server player. */
public final class MinecraftQuestEditorAuthorization implements QuestEditorAuthorization {
    @Override
    public boolean canManageDefinitions(QuestEditorActor actor) {
        if (actor == null) return false;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) return false;
        for (Object value : server.getConfigurationManager().playerEntityList) {
            if (!(value instanceof EntityPlayerMP)) continue;
            EntityPlayerMP player = (EntityPlayerMP) value;
            if (actor.getId().equals(player.getUniqueID())) {
                return player.canCommandSenderUseCommand(2, "jgbquest");
            }
        }
        return false;
    }
}
