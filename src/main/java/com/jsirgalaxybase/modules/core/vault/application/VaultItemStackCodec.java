package com.jsirgalaxybase.modules.core.vault.application;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

/** Complete ItemStack persistence shared by Vault and custom-market delivery. */
public final class VaultItemStackCodec {

    private VaultItemStackCodec() {}

    public static String encode(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            throw new VaultException("vault item snapshot requires a non-empty ItemStack");
        }
        NBTTagCompound tag = new NBTTagCompound();
        stack.writeToNBT(tag);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(tag, output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException exception) {
            throw new VaultException("failed to encode vault ItemStack", exception);
        }
    }

    public static ItemStack decode(String encoded) {
        NBTTagCompound tag = null;
        try {
            tag = CompressedStreamTools.readCompressed(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)));
            ItemStack stack = ItemStack.loadItemStackFromNBT(tag);
            if (stack != null && stack.getItem() != null && stack.stackSize > 0) {
                return stack;
            }
        } catch (IOException ignored) {
            // Old snapshots may still be recoverable from their NBT identity below.
        } catch (IllegalArgumentException ignored) {
            // Invalid Base64 is handled as an explicit vault error after fallback.
        }
        ItemStack recovered = restoreFromTag(tag);
        if (recovered == null || recovered.getItem() == null || recovered.stackSize <= 0) {
            throw new VaultException("vault ItemStack snapshot cannot be restored");
        }
        return recovered;
    }

    private static ItemStack restoreFromTag(NBTTagCompound tag) {
        if (tag == null) {
            return null;
        }
        Item item = null;
        if (tag.hasKey("id", 8)) {
            String id = tag.getString("id");
            int separator = id.indexOf(':');
            if (separator > 0 && separator < id.length() - 1) {
                item = GameRegistry.findItem(id.substring(0, separator), id.substring(separator + 1));
            }
            if (item == null) {
                Object registered = Item.itemRegistry.getObject(id);
                item = registered instanceof Item ? (Item) registered : null;
            }
        } else if (tag.hasKey("id", 99)) {
            item = Item.getItemById(tag.getShort("id"));
        }
        if (item == null) {
            return null;
        }
        int count = tag.hasKey("Count", 99) ? tag.getByte("Count") : 1;
        int meta = tag.hasKey("Damage", 99) ? tag.getShort("Damage") : 0;
        ItemStack stack = new ItemStack(item, Math.max(1, count), Math.max(0, meta));
        if (tag.hasKey("tag", 10)) {
            stack.setTagCompound(tag.getCompoundTag("tag"));
        }
        return stack;
    }
}
