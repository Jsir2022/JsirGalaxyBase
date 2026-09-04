package com.jsirgalaxybase.modules.warehouse.domain;

import java.util.Objects;

/** Immutable physical identity of a personal Warehouse Drive on one server. */
public final class WarehouseDriveKey implements Comparable<WarehouseDriveKey> {

    private final String serverId;
    private final int dimensionId;
    private final int blockX;
    private final int blockY;
    private final int blockZ;

    public WarehouseDriveKey(String serverId, int dimensionId, int blockX, int blockY, int blockZ) {
        if (serverId == null || serverId.trim().isEmpty()) throw new IllegalArgumentException("serverId is required");
        this.serverId = serverId.trim();
        this.dimensionId = dimensionId;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
    }

    public String getServerId() { return serverId; }
    public int getDimensionId() { return dimensionId; }
    public int getBlockX() { return blockX; }
    public int getBlockY() { return blockY; }
    public int getBlockZ() { return blockZ; }
    public int getChunkX() { return blockX >> 4; }
    public int getChunkZ() { return blockZ >> 4; }

    @Override
    public int compareTo(WarehouseDriveKey other) {
        int server = serverId.compareTo(other.serverId);
        if (server != 0) return server;
        int dimension = Integer.compare(dimensionId, other.dimensionId);
        if (dimension != 0) return dimension;
        int x = Integer.compare(blockX, other.blockX);
        if (x != 0) return x;
        int y = Integer.compare(blockY, other.blockY);
        return y != 0 ? y : Integer.compare(blockZ, other.blockZ);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof WarehouseDriveKey)) return false;
        WarehouseDriveKey that = (WarehouseDriveKey) other;
        return dimensionId == that.dimensionId && blockX == that.blockX && blockY == that.blockY && blockZ == that.blockZ
            && serverId.equals(that.serverId);
    }

    @Override
    public int hashCode() { return Objects.hash(serverId, dimensionId, blockX, blockY, blockZ); }

    @Override
    public String toString() { return serverId + ":[" + dimensionId + "@" + blockX + "," + blockY + "," + blockZ + "]"; }
}
