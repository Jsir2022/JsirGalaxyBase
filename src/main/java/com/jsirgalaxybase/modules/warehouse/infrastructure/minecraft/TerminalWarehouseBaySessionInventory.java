package com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft;

import com.jsirgalaxybase.modules.warehouse.application.TerminalWarehouseCellInspector;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalWarehouseBay;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/** One-slot in-memory view for a server-owned terminal Bay session. */
public final class TerminalWarehouseBaySessionInventory implements IInventory {
    private ItemStack cell;
    private long openingVersion;

    public TerminalWarehouseBaySessionInventory(TerminalWarehouseBay bay) {
        cell = bay == null ? null : copy(bay.getCell()); openingVersion = bay == null ? 0L : bay.getVersion();
    }
    public long getOpeningVersion() { return openingVersion; }
    public ItemStack getCell() { return copy(cell); }
    public void refresh(TerminalWarehouseBay bay) { cell = bay == null ? null : copy(bay.getCell()); openingVersion = bay == null ? 0L : bay.getVersion(); }
    public void restore(ItemStack value) { cell = copy(value); }
    @Override public int getSizeInventory() { return 1; }
    @Override public ItemStack getStackInSlot(int slot) { return slot == 0 ? cell : null; }
    @Override public ItemStack decrStackSize(int slot, int amount) {
        if (slot != 0 || cell == null) return null;
        if (cell.stackSize <= amount) { ItemStack removed = cell; cell = null; return removed; }
        return cell.splitStack(amount);
    }
    @Override public ItemStack getStackInSlotOnClosing(int slot) { ItemStack result = getStackInSlot(slot); cell = null; return result; }
    @Override public void setInventorySlotContents(int slot, ItemStack stack) { if (slot == 0) { cell = copy(stack); if (cell != null) cell.stackSize = 1; } }
    @Override public String getInventoryName() { return "银河仓储存储单元槽"; }
    @Override public boolean hasCustomInventoryName() { return true; }
    @Override public int getInventoryStackLimit() { return 1; }
    @Override public void markDirty() { }
    @Override public boolean isUseableByPlayer(EntityPlayer player) { return player != null; }
    @Override public void openInventory() { }
    @Override public void closeInventory() { }
    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return slot == 0 && TerminalWarehouseCellInspector.isValidCell(stack); }
    private static ItemStack copy(ItemStack stack) { return stack == null ? null : stack.copy(); }
}
