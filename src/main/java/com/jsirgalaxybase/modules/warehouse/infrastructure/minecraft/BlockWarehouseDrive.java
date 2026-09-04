/* SPDX-License-Identifier: LGPL-3.0-or-later
 * Derived integration shape from AE2 BlockDrive; JGB owns authorization only. */
package com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft;

import appeng.block.storage.BlockDrive;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.jsirgalaxybase.modules.warehouse.ae2.WarehouseDriveTile;

/** AE2-rendered Drive whose cell actions are delegated to the JGB server runtime. */
public final class BlockWarehouseDrive extends BlockDrive {
    public BlockWarehouseDrive() {
        super();
        setBlockName("warehouse_drive");
        setTileEntity(WarehouseDriveTile.class);
    }

    @Override
    public boolean onActivated(World world, int x, int y, int z, EntityPlayer player, int side,
        float hitX, float hitY, float hitZ) {
        if (world == null || player == null) return false;
        if (!world.isRemote) WarehouseDriveRuntime.handleActivated(world, x, y, z, player);
        // Deliberately do not delegate to BlockDrive: it would open AE2's ten-bay GUI.
        return true;
    }
}
