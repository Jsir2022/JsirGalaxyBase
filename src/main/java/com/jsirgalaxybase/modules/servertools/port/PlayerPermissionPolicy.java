package com.jsirgalaxybase.modules.servertools.port;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.servertools.domain.ServerWarp;
import com.jsirgalaxybase.modules.servertools.domain.TeleportActor;
import com.jsirgalaxybase.modules.servertools.domain.TeleportKind;

public interface PlayerPermissionPolicy {

    int getMaxHomes(TeleportActor actor);

    void validateTeleport(TeleportActor actor, TeleportKind teleportKind, TeleportTarget target);

    void validateWarp(TeleportActor actor, ServerWarp warp);
}