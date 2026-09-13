package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;

/** Resolves only an online, server-authoritative player for an inventory mutation. */
public interface QuestPlayerResolver {
    EntityPlayerMP findOnline(UUID playerId);
}
