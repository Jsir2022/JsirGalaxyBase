/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Derived from GTNH ServerUtilities ChunkDimPos.
 * Adapted for immutable, cross-server personal land identity in JsirGalaxyBase.
 */
package com.jsirgalaxybase.modules.land.domain;

import java.util.Objects;

public final class LandChunkKey implements Comparable<LandChunkKey> {

    private final String serverId;
    private final int dimensionId;
    private final int chunkX;
    private final int chunkZ;

    public LandChunkKey(String serverId, int dimensionId, int chunkX, int chunkZ) {
        this.serverId = requireText(serverId, "serverId");
        this.dimensionId = dimensionId;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public static LandChunkKey fromBlockCoordinates(String serverId, int dimensionId, int blockX, int blockZ) {
        return new LandChunkKey(serverId, dimensionId, blockX >> 4, blockZ >> 4);
    }

    public String getServerId() {
        return serverId;
    }

    public int getDimensionId() {
        return dimensionId;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public int getCenterBlockX() {
        return (chunkX << 4) + 8;
    }

    public int getCenterBlockZ() {
        return (chunkZ << 4) + 8;
    }

    @Override
    public int compareTo(LandChunkKey other) {
        int server = serverId.compareTo(other.serverId);
        if (server != 0) return server;
        int dimension = Integer.compare(dimensionId, other.dimensionId);
        if (dimension != 0) return dimension;
        int x = Integer.compare(chunkX, other.chunkX);
        return x != 0 ? x : Integer.compare(chunkZ, other.chunkZ);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LandChunkKey)) return false;
        LandChunkKey that = (LandChunkKey) other;
        return dimensionId == that.dimensionId && chunkX == that.chunkX && chunkZ == that.chunkZ
            && serverId.equals(that.serverId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverId, dimensionId, chunkX, chunkZ);
    }

    @Override
    public String toString() {
        return serverId + ":[" + dimensionId + "@" + chunkX + "," + chunkZ + "]";
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
