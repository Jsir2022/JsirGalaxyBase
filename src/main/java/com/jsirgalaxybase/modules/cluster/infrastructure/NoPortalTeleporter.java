package com.jsirgalaxybase.modules.cluster.infrastructure;

import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;

public class NoPortalTeleporter extends Teleporter {

    private final TeleportTarget destination;

    public NoPortalTeleporter(WorldServer worldServer, TeleportTarget destination) {
        super(worldServer);
        this.destination = destination;
    }

    @Override
    public void removeStalePortalLocations(long worldTime) {}

    @Override
    public void placeInPortal(Entity entity, double posX, double posY, double posZ, float yaw) {
        placeInExistingPortal(entity, posX, posY, posZ, yaw);
    }

    @Override
    public boolean placeInExistingPortal(Entity entity, double posX, double posY, double posZ, float yaw) {
        entity.motionX = 0.0D;
        entity.motionY = 0.0D;
        entity.motionZ = 0.0D;
        entity.fallDistance = 0.0F;
        entity.setLocationAndAngles(destination.getX(), destination.getY(), destination.getZ(),
            destination.getYaw(), destination.getPitch());
        return true;
    }

    @Override
    public boolean makePortal(Entity entity) {
        return false;
    }
}