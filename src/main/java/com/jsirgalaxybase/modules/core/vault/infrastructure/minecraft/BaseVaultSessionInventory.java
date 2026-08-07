package com.jsirgalaxybase.modules.core.vault.infrastructure.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.modules.core.vault.application.BaseVaultService;
import com.jsirgalaxybase.modules.core.vault.domain.VaultSlot;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/** In-memory view used by a single native Container session. */
public final class BaseVaultSessionInventory implements IInventory {

    private List<VaultSlot> expectedSlots;
    private final ItemStack[] stacks;

    public BaseVaultSessionInventory(BaseVaultService.VaultView view) {
        this.expectedSlots = new ArrayList<VaultSlot>(view.getSlots());
        this.stacks = new ItemStack[view.getSlots().size()];
        for (int index = 0; index < stacks.length; index++) {
            stacks[index] = copy(expectedSlots.get(index).getStack());
        }
    }

    public List<VaultSlot> getOpeningSlots() {
        return Collections.unmodifiableList(expectedSlots);
    }

    public void refreshExpectedSlots(BaseVaultService.VaultView view) {
        expectedSlots = new ArrayList<VaultSlot>(view.getSlots());
        for (int index = 0; index < stacks.length; index++) {
            stacks[index] = copy(expectedSlots.get(index).getStack());
        }
    }

    public List<ItemStack> snapshotStacks() {
        List<ItemStack> result = new ArrayList<ItemStack>(stacks.length);
        for (ItemStack stack : stacks) {
            result.add(copy(stack));
        }
        return result;
    }

    public void restore(List<ItemStack> snapshot) {
        for (int index = 0; index < stacks.length; index++) {
            stacks[index] = snapshot != null && index < snapshot.size() ? copy(snapshot.get(index)) : null;
        }
        markDirty();
    }

    @Override
    public int getSizeInventory() { return stacks.length; }

    @Override
    public ItemStack getStackInSlot(int slot) { return slot >= 0 && slot < stacks.length ? stacks[slot] : null; }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        ItemStack stack = getStackInSlot(slot);
        if (stack == null) {
            return null;
        }
        if (stack.stackSize <= amount) {
            stacks[slot] = null;
            markDirty();
            return stack;
        }
        ItemStack result = stack.splitStack(amount);
        if (stack.stackSize <= 0) {
            stacks[slot] = null;
        }
        markDirty();
        return result;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack stack = getStackInSlot(slot);
        stacks[slot] = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot < 0 || slot >= stacks.length) {
            return;
        }
        stacks[slot] = copy(stack);
        if (stacks[slot] != null && stacks[slot].stackSize > getInventoryStackLimit()) {
            stacks[slot].stackSize = getInventoryStackLimit();
        }
        markDirty();
    }

    @Override
    public String getInventoryName() { return "Base Vault"; }

    @Override
    public boolean hasCustomInventoryName() { return true; }

    @Override
    public int getInventoryStackLimit() { return 64; }

    @Override
    public void markDirty() { }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) { return player != null; }

    @Override
    public void openInventory() { }

    @Override
    public void closeInventory() { }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) { return stack != null && stack.stackSize > 0; }

    private static ItemStack copy(ItemStack stack) { return stack == null ? null : stack.copy(); }
}
