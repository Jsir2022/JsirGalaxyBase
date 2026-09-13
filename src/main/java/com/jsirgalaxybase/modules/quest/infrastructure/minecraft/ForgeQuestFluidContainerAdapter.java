package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

/** Forge 1.7.10 implementation; all mutation is performed on a one-item copy. */
public final class ForgeQuestFluidContainerAdapter implements QuestFluidContainerAdapter {
    @Override
    public FluidView inspect(ItemStack one) {
        if (one == null || one.getItem() == null) return null;
        FluidStack fluid = one.getItem() instanceof IFluidContainerItem
            ? ((IFluidContainerItem) one.getItem()).getFluid(one)
            : FluidContainerRegistry.getFluidForFilledItem(one);
        return view(fluid);
    }

    @Override
    public FluidDrain drain(ItemStack one, int maximumAmount) {
        if (one == null || one.getItem() == null || maximumAmount <= 0) return null;
        FluidStack drained;
        ItemStack returned;
        if (one.getItem() instanceof IFluidContainerItem) {
            drained = ((IFluidContainerItem) one.getItem()).drain(one, maximumAmount, true);
            returned = one;
        } else {
            drained = FluidContainerRegistry.getFluidForFilledItem(one);
            returned = FluidContainerRegistry.drainFluidContainer(one);
        }
        FluidView view = view(drained);
        return view == null ? null : new FluidDrain(view, returned);
    }

    private FluidView view(FluidStack fluid) {
        if (fluid == null || fluid.getFluid() == null || fluid.amount <= 0) return null;
        return new FluidView(fluid.getFluid().getName(), fluid.amount, fluid.tag);
    }
}
