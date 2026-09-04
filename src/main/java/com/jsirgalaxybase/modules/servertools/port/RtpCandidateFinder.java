package com.jsirgalaxybase.modules.servertools.port;

import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.servertools.domain.TargetServerRtpProfile;

/** Selects a safe target on the server that owns the target dimension. */
public interface RtpCandidateFinder {
    TeleportTarget findSafeTarget(TargetServerRtpProfile profile, EntityPlayerMP player);
}
