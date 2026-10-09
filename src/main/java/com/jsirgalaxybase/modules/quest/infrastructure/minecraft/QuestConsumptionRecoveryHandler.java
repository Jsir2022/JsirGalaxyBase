package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestConsumptionService;
import com.jsirgalaxybase.quest.core.ConsumptionSubmission;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraft.entity.player.EntityPlayerMP;

/** Resumes durable item submissions only when the authenticated player is online on the source server. */
public final class QuestConsumptionRecoveryHandler {
    private final AuthenticatedQuestConsumptionService service;

    public QuestConsumptionRecoveryHandler(AuthenticatedQuestConsumptionService service) {
        if (service == null) throw new IllegalArgumentException("service is required");
        this.service = service;
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event == null || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        try {
            java.util.List<ConsumptionSubmission> resumed = service.resumePendingForPlayer(
                player.getUniqueID(), System.currentTimeMillis());
            if (!resumed.isEmpty()) GalaxyBase.LOG.info("Resumed " + resumed.size()
                + " pending quest item submission(s) for " + player.getCommandSenderName());
        } catch (RuntimeException failure) {
            GalaxyBase.LOG.error("Failed to resume pending quest item submissions for "
                + player.getCommandSenderName(), failure);
        }
    }
}
