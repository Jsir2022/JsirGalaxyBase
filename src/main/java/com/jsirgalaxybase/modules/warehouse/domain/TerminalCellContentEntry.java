package com.jsirgalaxybase.modules.warehouse.domain;

import net.minecraft.item.ItemStack;

/** One item type exposed by the server-authoritative contents of the installed Cell. */
public final class TerminalCellContentEntry {
    private final ItemStack stack;
    private final long quantity;

    public TerminalCellContentEntry(ItemStack stack, long quantity) {
        if (stack == null || stack.getItem() == null) throw new IllegalArgumentException("stack is required");
        this.stack = stack.copy();
        this.stack.stackSize = 1;
        this.quantity = Math.max(0L, quantity);
    }

    public ItemStack getStack() { return stack.copy(); }
    public long getQuantity() { return quantity; }
}
