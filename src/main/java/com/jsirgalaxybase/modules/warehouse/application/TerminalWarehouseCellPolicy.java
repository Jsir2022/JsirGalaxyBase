package com.jsirgalaxybase.modules.warehouse.application;

import net.minecraft.item.ItemStack;

/** Narrow seam so persistence tests never need the client/runtime AE2 registry. */
public interface TerminalWarehouseCellPolicy {
    boolean isValidCell(ItemStack stack);
}
