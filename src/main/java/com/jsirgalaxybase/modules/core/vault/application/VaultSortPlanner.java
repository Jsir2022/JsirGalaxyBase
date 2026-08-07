package com.jsirgalaxybase.modules.core.vault.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Server-owned, deterministic Base Vault ordering. */
public final class VaultSortPlanner {

    public static final String POLICY_VERSION = "registry-meta-nbt-v1";

    private VaultSortPlanner() {}

    public static List<ItemStack> sort(List<ItemStack> source, int slotCount) {
        if (source == null || slotCount <= 0) {
            throw new VaultException("Vault sort requires a positive slot capacity");
        }
        List<StackGroup> groups = new ArrayList<StackGroup>();
        for (ItemStack stack : source) {
            if (stack == null || stack.getItem() == null || stack.stackSize <= 0) continue;
            StackGroup group = findGroup(groups, stack);
            if (group == null) groups.add(new StackGroup(stack));
            else group.quantity += stack.stackSize;
        }
        Collections.sort(groups, new Comparator<StackGroup>() {
            @Override
            public int compare(StackGroup left, StackGroup right) {
                int result = registryName(left.template).compareTo(registryName(right.template));
                if (result != 0) return result;
                result = left.template.getItemDamage() - right.template.getItemDamage();
                return result != 0 ? result : canonicalIdentity(left.template).compareTo(canonicalIdentity(right.template));
            }
        });
        List<ItemStack> result = new ArrayList<ItemStack>(slotCount);
        for (StackGroup group : groups) {
            int remaining = group.quantity;
            int limit = Math.min(64, group.template.getMaxStackSize());
            if (limit <= 0) throw new VaultException("Vault sort encountered an item without a stack limit");
            while (remaining > 0) {
                if (result.size() >= slotCount) throw new VaultException("Vault sort would overflow its fixed capacity");
                ItemStack placed = group.template.copy();
                placed.stackSize = Math.min(limit, remaining);
                result.add(placed);
                remaining -= placed.stackSize;
            }
        }
        while (result.size() < slotCount) result.add(null);
        return result;
    }

    private static StackGroup findGroup(List<StackGroup> groups, ItemStack candidate) {
        for (StackGroup group : groups) {
            if (group.template.isItemEqual(candidate) && ItemStack.areItemStackTagsEqual(group.template, candidate)) {
                return group;
            }
        }
        return null;
    }

    private static String registryName(ItemStack stack) {
        Object name = Item.itemRegistry.getNameForObject(stack.getItem());
        return name == null ? stack.getItem().getUnlocalizedName() : String.valueOf(name);
    }

    private static String canonicalIdentity(ItemStack stack) {
        ItemStack identity = stack.copy();
        identity.stackSize = 1;
        return VaultItemStackCodec.encode(identity);
    }

    private static final class StackGroup {
        private final ItemStack template;
        private int quantity;
        private StackGroup(ItemStack stack) {
            template = stack.copy();
            template.stackSize = 1;
            quantity = stack.stackSize;
        }
    }
}
