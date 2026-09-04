package com.jsirgalaxybase.modules.cluster.port;

import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicket;

/** Resolves a durable ticket target on the target server immediately before arrival placement. */
public interface ArrivalTargetResolver {
    TeleportTarget resolve(TransferTicket ticket, EntityPlayerMP player);

    /** Called only after the local executor has actually placed the player. */
    void afterSuccessfulRestore(TransferTicket ticket, TeleportTarget resolvedTarget);
}
