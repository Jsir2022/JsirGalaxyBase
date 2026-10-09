package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import com.jsirgalaxybase.quest.core.ConsumptionSelection;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/** Captures the authenticated player's selected hotbar stack without accepting item identity from a packet. */
public final class MinecraftConsumptionSelection {
    private MinecraftConsumptionSelection() {}

    public static ConsumptionSelection captureHeldStack(EntityPlayerMP player) {
        if (player == null || player.inventory == null) return null;
        int slot = player.inventory.currentItem;
        if (slot < 0 || slot >= 9) return null;
        ItemStack stack = player.inventory.getStackInSlot(slot);
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) return null;
        return ConsumptionSelection.selectedStack(slot, MinecraftPlayerConsumptionPort.fingerprint(stack));
    }
}
