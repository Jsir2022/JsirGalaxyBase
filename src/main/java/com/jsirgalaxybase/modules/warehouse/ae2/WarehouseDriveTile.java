/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * This Phase 0 adapter derives its integration shape from AE2 TileDrive
 * (Reference/Applied-Energistics-2-Unofficial). It deliberately reuses AE2's
 * own Cell inventory and Storage Grid contract rather than copying cell data
 * into PostgreSQL or a JGB virtual inventory.
 */
package com.jsirgalaxybase.modules.warehouse.ae2;

import appeng.tile.storage.TileDrive;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.storage.MEInventoryHandler;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * One-cell technical-spike Drive. This is not yet registered as a player-facing
 * block: market transfer, account ownership and a controlled GUI are separate
 * phases. Its superclass remains the single source of truth for AE2 channels,
 * power and Cell contents.
 */
public final class WarehouseDriveTile extends TileDrive {

    private static final int[] NO_AUTOMATION_BAYS = new int[0];

    @Override
    public int getCellCount() {
        return 1;
    }

    @Override
    public int getCellStatus(int slot) {
        return slot == 0 ? super.getCellStatus(0) : 0;
    }

    @Override
    public int getCellType(int slot) {
        return slot == 0 ? super.getCellType(0) : 0;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && super.isItemValidForSlot(slot, stack);
    }

    @Override
    public int[] getAccessibleSlotsBySide(ForgeDirection side) {
        // Cell insertion/removal is a server-authorized owner operation, never hopper/pipe automation.
        return NO_AUTOMATION_BAYS;
    }

    public boolean hasCell() { return getInternalInventory().getStackInSlot(0) != null; }

    public long getStoredItemCount() {
        MEInventoryHandler<IAEItemStack> handler = getCellInvBySlot(0);
        if (handler == null || !(handler.getInternal() instanceof ICellInventory)) return 0L;
        return Math.max(0L, ((ICellInventory) handler.getInternal()).getStoredItemCount());
    }

    public long getStoredItemTypes() {
        MEInventoryHandler<IAEItemStack> handler = getCellInvBySlot(0);
        if (handler == null || !(handler.getInternal() instanceof ICellInventory)) return 0L;
        return Math.max(0L, ((ICellInventory) handler.getInternal()).getStoredItemTypes());
    }
}
