package com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import com.jsirgalaxybase.modules.warehouse.ae2.WarehouseDriveTile;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveHealth;
import com.jsirgalaxybase.modules.warehouse.domain.WarehouseDriveRecord;

/** Reads only a currently loaded server chunk; it never force-loads a Drive for a terminal refresh. */
public final class WarehouseDriveHealthResolver {
    private WarehouseDriveHealthResolver() {}
    public static WarehouseDriveHealth inspectLoaded(WarehouseDriveRecord record) {
        if (record == null) return null;
        MinecraftServer server = MinecraftServer.getServer();
        WorldServer world = server == null ? null : server.worldServerForDimension(record.getKey().getDimensionId());
        if (world == null || !world.getChunkProvider().chunkExists(record.getKey().getChunkX(), record.getKey().getChunkZ())) {
            return new WarehouseDriveHealth(record, false, false, false, false, 0L, 0L);
        }
        boolean present = WarehouseBlockRegistry.isWarehouseDrive(world.getBlock(record.getKey().getBlockX(),
            record.getKey().getBlockY(), record.getKey().getBlockZ()));
        if (!present || !(world.getTileEntity(record.getKey().getBlockX(), record.getKey().getBlockY(), record.getKey().getBlockZ()) instanceof WarehouseDriveTile)) {
            return new WarehouseDriveHealth(record, true, false, false, false, 0L, 0L);
        }
        WarehouseDriveTile tile = (WarehouseDriveTile) world.getTileEntity(record.getKey().getBlockX(), record.getKey().getBlockY(), record.getKey().getBlockZ());
        return new WarehouseDriveHealth(record, true, true, tile.hasCell(), tile.isPowered(), tile.getStoredItemCount(), tile.getStoredItemTypes());
    }
}
