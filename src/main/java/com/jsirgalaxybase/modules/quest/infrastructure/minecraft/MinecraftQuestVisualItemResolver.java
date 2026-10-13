package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Resolves a client-safe quest item description to a Minecraft stack for the terminal render bridge. */
public final class MinecraftQuestVisualItemResolver {
    private MinecraftQuestVisualItemResolver() {}

    public static ItemStack resolve(String registryName, int metadata, String serializedNbt) {
        if (registryName == null || registryName.trim().isEmpty()) return null;
        Object registered = Item.itemRegistry.getObject(registryName.trim());
        if (!(registered instanceof Item)) return null;
        try {
            ItemStack stack = new ItemStack((Item) registered, 1, metadata);
            NBTTagCompound tag = BqTypedNbtCodec.compound(serializedNbt);
            if (tag != null && !tag.hasNoTags()) stack.setTagCompound(tag);
            return stack;
        } catch (RuntimeException invalidNbt) {
            return null;
        }
    }
}
