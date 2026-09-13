package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import net.minecraft.entity.player.EntityPlayerMP;

/** Persists the inventory and its idempotency marker in the same player-data write. */
public interface QuestPlayerDataFlusher {
    void flush(EntityPlayerMP player);
}
