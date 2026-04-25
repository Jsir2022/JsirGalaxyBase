package com.jsirgalaxybase.modules.servertools.infrastructure;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.servertools.domain.ServerWarp;
import com.jsirgalaxybase.modules.servertools.domain.TeleportActor;
import com.jsirgalaxybase.modules.servertools.domain.TeleportKind;
import com.jsirgalaxybase.modules.servertools.port.PlayerPermissionPolicy;

public class AllowAllPlayerPermissionPolicy implements PlayerPermissionPolicy {

    private final int maxHomes;

    public AllowAllPlayerPermissionPolicy() {
        this(5);
    }

    public AllowAllPlayerPermissionPolicy(int maxHomes) {
        this.maxHomes = maxHomes;
    }

    @Override
    public int getMaxHomes(TeleportActor actor) {
        return maxHomes;
    }

    @Override
    public void validateTeleport(TeleportActor actor, TeleportKind teleportKind, TeleportTarget target) {}

    @Override
    public void validateWarp(TeleportActor actor, ServerWarp warp) {}
}