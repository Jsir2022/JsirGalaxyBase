/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * Integration semantics are derived from AE2's TileDrive / ICellHandler
 * contract in Reference/Applied-Energistics-2-Unofficial. This file is a
 * narrow adapter; Cell contents remain owned by AE2.
 */
package com.jsirgalaxybase.modules.warehouse.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.storage.MEInventoryHandler;
import appeng.util.item.AEItemStack;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * Server-side Phase 0 probe for a live {@link WarehouseDriveTile}. It has no
 * market, Vault, persistence or cross-server responsibility. `simulateInject`
 * deliberately invokes AE2 with {@link Actionable#SIMULATE} only.
 */
public final class Ae2WarehouseDriveProbe {

    private final WarehouseDriveTile tile;
    private final WarehouseDriveCellBay bay;
    private final BaseActionSource source = new BaseActionSource();

    public Ae2WarehouseDriveProbe(WarehouseDriveTile tile, WarehouseDriveCellBay bay) {
        if (tile == null || bay == null) throw new IllegalArgumentException("tile and cell bay are required");
        this.tile = tile;
        this.bay = bay;
    }

    public WarehouseDriveProbeResult install(ItemStack cell) {
        if (!tile.isItemValidForSlot(0, cell)) return bay.install(false);
        IInventory inventory = tile.getInternalInventory();
        if (inventory.getStackInSlot(0) != null) return bay.install(true);
        inventory.setInventorySlotContents(0, cell.copy());
        return bay.install(true);
    }

    public WarehouseDriveProbeResult removeIfEmpty() {
        IInventory inventory = tile.getInternalInventory();
        if (inventory.getStackInSlot(0) == null) return bay.remove();
        syncStoredCount();
        WarehouseDriveProbeResult decision = bay.remove();
        if (decision.isSuccess()) inventory.setInventorySlotContents(0, null);
        return decision;
    }

    public WarehouseDriveProbeResult simulateInject(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return WarehouseDriveProbeResult.transfer(
            WarehouseDriveProbeResult.Code.SUCCESS, 0L, 0L);
        MEInventoryHandler<IAEItemStack> handler = tile.getCellInvBySlot(0);
        long requested = stack.stackSize;
        if (handler == null) return bay.evaluateTransfer(false, requested, 0L);
        if (!tile.isPowered()) return bay.evaluateTransfer(false, requested, 0L);
        IAEItemStack input = AEItemStack.create(stack.copy());
        IAEItemStack remainder = handler.injectItems(input, Actionable.SIMULATE, source);
        long left = remainder == null ? 0L : remainder.getStackSize();
        return bay.evaluateTransfer(true, requested, requested - left);
    }

    public void syncStoredCount() {
        MEInventoryHandler<IAEItemStack> handler = tile.getCellInvBySlot(0);
        if (handler == null) {
            bay.updateStoredItems(0L);
            return;
        }
        IMEInventory<IAEItemStack> internal = handler.getInternal();
        if (internal instanceof ICellInventory) {
            bay.updateStoredItems(((ICellInventory) internal).getStoredItemCount());
        }
    }
}
