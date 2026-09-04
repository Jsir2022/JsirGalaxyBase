package com.jsirgalaxybase.modules.warehouse.domain;

import net.minecraft.item.ItemStack;

/** Audited Cell-content mutation result. appliedNow prevents request replay from duplicating cursor changes. */
public final class TerminalCellContentReceipt {
    private final TerminalWarehouseBayReceipt bayReceipt;
    private final ItemStack movedStack;
    private final long movedQuantity;
    private final boolean appliedNow;

    public TerminalCellContentReceipt(TerminalWarehouseBayReceipt bayReceipt, ItemStack movedStack,
        long movedQuantity, boolean appliedNow) {
        if (bayReceipt == null) throw new IllegalArgumentException("bayReceipt is required");
        this.bayReceipt = bayReceipt;
        this.movedStack = movedStack == null ? null : movedStack.copy();
        this.movedQuantity = Math.max(0L, movedQuantity);
        this.appliedNow = appliedNow;
    }

    public TerminalWarehouseBayReceipt getBayReceipt() { return bayReceipt; }
    public ItemStack getMovedStack() { return movedStack == null ? null : movedStack.copy(); }
    public long getMovedQuantity() { return movedQuantity; }
    public boolean isAppliedNow() { return appliedNow; }
    public boolean isSuccess() { return bayReceipt.isSuccess(); }
}
