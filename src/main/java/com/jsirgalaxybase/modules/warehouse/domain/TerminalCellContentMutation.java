package com.jsirgalaxybase.modules.warehouse.domain;

import net.minecraft.item.ItemStack;

/** Result produced by the AE2 Cell adapter before the Bay row is committed. */
public final class TerminalCellContentMutation {
    private final ItemStack cell;
    private final ItemStack movedStack;
    private final long movedQuantity;

    public TerminalCellContentMutation(ItemStack cell, ItemStack movedStack, long movedQuantity) {
        this.cell = copy(cell);
        this.movedStack = copy(movedStack);
        this.movedQuantity = Math.max(0L, movedQuantity);
    }

    public ItemStack getCell() { return copy(cell); }
    public ItemStack getMovedStack() { return copy(movedStack); }
    public long getMovedQuantity() { return movedQuantity; }
    private static ItemStack copy(ItemStack stack) { return stack == null ? null : stack.copy(); }
}
