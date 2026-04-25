package com.jsirgalaxybase.modules.cluster.infrastructure;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.cluster.port.LocalTeleportExecutor;

public class MinecraftLocalTeleportExecutor implements LocalTeleportExecutor {

    @Override
    public void teleport(EntityPlayerMP player, TeleportTarget target) {
        if (player == null) {
            throw new IllegalArgumentException("player must not be null");
        }

        if (player.dimension != target.getDimensionId()) {
            MinecraftServer server = MinecraftServer.getServer();
            WorldServer currentWorld = server.worldServerForDimension(player.dimension);
            WorldServer targetWorld = server.worldServerForDimension(target.getDimensionId());
            server.getConfigurationManager().transferPlayerToDimension(player, target.getDimensionId(),
                new NoPortalTeleporter(targetWorld, target));
            player.playerNetServerHandler.sendPacket(new S1FPacketSetExperience(player.experience,
                player.experienceTotal, player.experienceLevel));
            player.sendPlayerAbilities();
            if (currentWorld.provider.dimensionId == 1 && player.isEntityAlive()) {
                targetWorld.spawnEntityInWorld(player);
                targetWorld.updateEntityWithOptionalForce(player, false);
            }
        }

        placeEntity(player, target);
    }

    private void placeEntity(Entity entity, TeleportTarget target) {
        entity.motionX = 0.0D;
        entity.motionY = 0.0D;
        entity.motionZ = 0.0D;
        entity.fallDistance = 0.0F;
        if (entity instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) entity;
            player.playerNetServerHandler.setPlayerLocation(target.getX(), target.getY(), target.getZ(),
                target.getYaw(), target.getPitch());
            return;
        }
        entity.setLocationAndAngles(target.getX(), target.getY(), target.getZ(), target.getYaw(),
            target.getPitch());
    }
}