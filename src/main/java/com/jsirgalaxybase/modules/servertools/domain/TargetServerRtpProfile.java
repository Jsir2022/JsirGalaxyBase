package com.jsirgalaxybase.modules.servertools.domain;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;

/**
 * A server-controlled random-teleport area. The fallback target is only the
 * durable ticket landing contract; the target server chooses the actual safe
 * random block after the player has arrived there.
 */
public final class TargetServerRtpProfile {

    private final String serverId;
    private final int dimensionId;
    private final double centerX;
    private final double fallbackY;
    private final double centerZ;
    private final double minDistance;
    private final double maxDistance;

    public TargetServerRtpProfile(String serverId, int dimensionId, double centerX, double fallbackY,
        double centerZ, double minDistance, double maxDistance) {
        if (serverId == null || serverId.trim().isEmpty()) throw new IllegalArgumentException("rtp serverId must not be blank");
        if (!finite(centerX) || !finite(fallbackY) || !finite(centerZ) || !finite(minDistance) || !finite(maxDistance)
            || minDistance < 0.0D || maxDistance < minDistance || maxDistance > 30000000.0D) {
            throw new IllegalArgumentException("invalid target-server RTP profile range");
        }
        this.serverId = serverId.trim();
        this.dimensionId = dimensionId;
        this.centerX = centerX;
        this.fallbackY = fallbackY;
        this.centerZ = centerZ;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
    }

    public String getServerId() { return serverId; }
    public int getDimensionId() { return dimensionId; }
    public double getCenterX() { return centerX; }
    public double getFallbackY() { return fallbackY; }
    public double getCenterZ() { return centerZ; }
    public double getMinDistance() { return minDistance; }
    public double getMaxDistance() { return maxDistance; }

    public TeleportTarget getFallbackTarget() {
        return new TeleportTarget(serverId, dimensionId, centerX + 0.5D, fallbackY, centerZ + 0.5D, 0.0F, 0.0F);
    }

    private static boolean finite(double value) { return !Double.isNaN(value) && !Double.isInfinite(value); }
}
