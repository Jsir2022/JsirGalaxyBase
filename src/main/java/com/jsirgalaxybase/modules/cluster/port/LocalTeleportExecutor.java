package com.jsirgalaxybase.modules.cluster.port;

import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;

public interface LocalTeleportExecutor {

    void teleport(EntityPlayerMP player, TeleportTarget target);
}