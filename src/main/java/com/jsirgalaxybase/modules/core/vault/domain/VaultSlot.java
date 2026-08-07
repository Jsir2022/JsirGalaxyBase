package com.jsirgalaxybase.modules.core.vault.domain;

import net.minecraft.item.ItemStack;

public final class VaultSlot {

    private final int slotIndex;
    private final ItemStack stack;
    private final long version;

    public VaultSlot(int slotIndex, ItemStack stack, long version) {
        this.slotIndex = slotIndex;
        this.stack = stack == null ? null : stack.copy();
        this.version = version;
    }

    public int getSlotIndex() { return slotIndex; }
    public ItemStack getStack() { return stack == null ? null : stack.copy(); }
    public long getVersion() { return version; }
    public boolean isEmpty() { return stack == null || stack.stackSize <= 0; }
}
