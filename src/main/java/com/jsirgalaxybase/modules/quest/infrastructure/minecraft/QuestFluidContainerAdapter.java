package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Isolates Forge fluid-container mechanics from the deterministic inventory planner. */
public interface QuestFluidContainerAdapter {
    FluidView inspect(ItemStack oneContainer);
    FluidDrain drain(ItemStack oneContainer, int maximumAmount);

    final class FluidView {
        private final String name;
        private final int amount;
        private final NBTTagCompound tag;
        public FluidView(String name, int amount, NBTTagCompound tag) {
            this.name = name;
            this.amount = amount;
            this.tag = tag == null ? null : (NBTTagCompound) tag.copy();
        }
        public String getName() { return name; }
        public int getAmount() { return amount; }
        public NBTTagCompound getTag() { return tag == null ? null : (NBTTagCompound) tag.copy(); }
    }

    final class FluidDrain {
        private final FluidView drained;
        private final ItemStack returnedContainer;
        public FluidDrain(FluidView drained, ItemStack returnedContainer) {
            this.drained = drained;
            this.returnedContainer = returnedContainer == null ? null : returnedContainer.copy();
        }
        public FluidView getDrained() { return drained; }
        public ItemStack getReturnedContainer() {
            return returnedContainer == null ? null : returnedContainer.copy();
        }
    }
}
