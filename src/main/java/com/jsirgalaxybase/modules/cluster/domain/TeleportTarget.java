package com.jsirgalaxybase.modules.cluster.domain;

import net.minecraft.entity.player.EntityPlayerMP;

public class TeleportTarget {

    private final String serverId;
    private final int dimensionId;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public TeleportTarget(String serverId, int dimensionId, double x, double y, double z, float yaw, float pitch) {
        if (serverId == null || serverId.trim().isEmpty()) {
            throw new IllegalArgumentException("serverId must not be blank");
        }
        this.serverId = serverId.trim();
        this.dimensionId = dimensionId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static TeleportTarget fromPlayer(EntityPlayerMP player, String serverId) {
        return new TeleportTarget(serverId, player.dimension, player.posX, player.posY, player.posZ,
            player.rotationYaw, player.rotationPitch);
    }

    public String getServerId() {
        return serverId;
    }

    public int getDimensionId() {
        return dimensionId;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}