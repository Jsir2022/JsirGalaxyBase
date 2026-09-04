package com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.modules.warehouse.ae2.WarehouseDriveTile;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;

/** Registers the client/server block identity; runtime authorization remains server-only. */
public final class WarehouseBlockRegistry {
    public static final String BLOCK_NAME = "warehouse_drive";
    private static BlockWarehouseDrive driveBlock;
    private WarehouseBlockRegistry() {}

    public static void registerIfAe2Present() {
        if (driveBlock != null) return;
        if (!Loader.isModLoaded("appliedenergistics2")) {
            GalaxyBase.LOG.warn("AE2 is absent; Warehouse Drive block is not registered");
            return;
        }
        driveBlock = new BlockWarehouseDrive();
        GameRegistry.registerBlock(driveBlock, BLOCK_NAME);
        GameRegistry.registerTileEntity(WarehouseDriveTile.class, GalaxyBase.MODID + ":" + BLOCK_NAME);
        GalaxyBase.LOG.info("Registered personal AE2 Warehouse Drive block");
    }

    public static boolean isWarehouseDrive(Object block) { return driveBlock != null && driveBlock == block; }
    public static BlockWarehouseDrive getDriveBlock() { return driveBlock; }
}
