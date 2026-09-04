package com.jsirgalaxybase.modules.warehouse.application;

import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentAction;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentMutation;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentSnapshot;

import net.minecraft.item.ItemStack;

/** Testable boundary around AE2's NBT-backed Cell inventory. */
public interface TerminalWarehouseCellAccess {
    TerminalCellContentSnapshot inspect(ItemStack cell, long bayVersion, int pageIndex, int pageSize);
    TerminalCellContentMutation mutate(ItemStack cell, TerminalCellContentAction action, ItemStack target,
        long quantity);
}
