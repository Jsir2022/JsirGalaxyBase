package com.jsirgalaxybase.modules.core.market.domain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;

import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

public class CustomMarketItemSnapshot {

    private final long snapshotId;
    private final long listingId;
    private final String itemId;
    private final int meta;
    private final int stackSize;
    private final boolean stackable;
    private final String displayName;
    private final String nbtSnapshot;
    private final Instant createdAt;

    public CustomMarketItemSnapshot(long snapshotId, long listingId, String itemId, int meta, int stackSize,
        boolean stackable, String displayName, String nbtSnapshot, Instant createdAt) {
        this.snapshotId = snapshotId;
        this.listingId = listingId;
        this.itemId = itemId;
        this.meta = meta;
        this.stackSize = stackSize;
        this.stackable = stackable;
        this.displayName = displayName;
        this.nbtSnapshot = nbtSnapshot;
        this.createdAt = createdAt;
    }

    public static CustomMarketItemSnapshot capture(long listingId, ItemStack stack, Instant now) {
        if (stack == null || stack.stackSize <= 0) {
            throw new MarketOperationException("listing requires a non-empty item stack");
        }
        if (stack.stackSize != 1) {
            throw new MarketOperationException("custom market listing requires exactly one item in hand");
        }
        Item item = stack.getItem();
        GameRegistry.UniqueIdentifier identifier = item == null ? null : GameRegistry.findUniqueIdentifierFor(item);
        String itemId = identifier == null && item != null ? Item.itemRegistry.getNameForObject(item)
            : identifier == null ? null : identifier.modId + ":" + identifier.name;
        if (itemId == null) {
            itemId = item == null ? "unknown:test-item" : item.getUnlocalizedName();
        }
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new MarketOperationException("custom market item cannot produce snapshot identity");
        }
        int meta = item == null ? 0 : stack.getItemDamage();
        boolean stackable = item == null ? stack.stackSize > 1 : stack.getMaxStackSize() > 1;
        String displayName = readDisplayNameFromTag(stack);
        if (displayName == null) {
            try {
                displayName = stack.getDisplayName();
            } catch (RuntimeException exception) {
                displayName = itemId;
            }
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = itemId;
        }
        return new CustomMarketItemSnapshot(0L, listingId, itemId, meta, stack.stackSize, stackable, displayName,
            encodeStack(stack), now);
    }

    public long getSnapshotId() {
        return snapshotId;
    }

    public long getListingId() {
        return listingId;
    }

    public String getItemId() {
        return itemId;
    }

    public int getMeta() {
        return meta;
    }

    public int getStackSize() {
        return stackSize;
    }

    public boolean isStackable() {
        return stackable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getNbtSnapshot() {
        return nbtSnapshot;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public ItemStack toItemStack() {
        NBTTagCompound decodedTag = null;
        try {
            byte[] bytes = Base64.getDecoder().decode(nbtSnapshot);
            decodedTag = CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes));
            ItemStack stack = ItemStack.loadItemStackFromNBT(decodedTag);
            if (stack != null && stack.getItem() != null) {
                return stack;
            }
        } catch (IOException exception) {
            // fall through to field-based reconstruction below
        } catch (IllegalArgumentException exception) {
            // fall through to field-based reconstruction below
        }
        if (decodedTag != null) {
            ItemStack restoredFromTag = restoreFromTag(decodedTag);
            if (restoredFromTag != null && restoredFromTag.getItem() != null) {
                return restoredFromTag;
            }
        }
        Item restoredItem = resolveItem(itemId);
        if (restoredItem == null) {
            throw new MarketOperationException("custom market snapshot cannot restore item stack");
        }
        ItemStack restoredStack = new ItemStack(restoredItem, stackSize, meta);
        if (displayName != null && !displayName.trim().isEmpty() && !displayName.equals(itemId)) {
            restoredStack.setStackDisplayName(displayName);
        }
        return restoredStack;
    }

    private static String encodeStack(ItemStack stack) {
        NBTTagCompound tag = new NBTTagCompound();
        stack.writeToNBT(tag);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(tag, outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException exception) {
            throw new MarketOperationException("failed to capture custom market snapshot: " + exception.getMessage());
        }
    }

    private static String readDisplayNameFromTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("display")) {
            return null;
        }
        NBTTagCompound display = tag.getCompoundTag("display");
        return display != null && display.hasKey("Name") ? display.getString("Name") : null;
    }

    private static Item resolveItem(String itemId) {
        if (itemId == null) {
            return null;
        }
        int separator = itemId.indexOf(':');
        if (separator > 0 && separator < itemId.length() - 1) {
            Item resolved = GameRegistry.findItem(itemId.substring(0, separator), itemId.substring(separator + 1));
            if (resolved != null) {
                return resolved;
            }
        }
        Object registryValue = Item.itemRegistry.getObject(itemId);
        return registryValue instanceof Item ? (Item) registryValue : null;
    }

    private static ItemStack restoreFromTag(NBTTagCompound tag) {
        Item restoredItem = null;
        if (tag.hasKey("id", 8)) {
            restoredItem = resolveItem(tag.getString("id"));
        } else if (tag.hasKey("id", 99)) {
            restoredItem = Item.getItemById(tag.getShort("id"));
        }
        if (restoredItem == null) {
            return null;
        }
        int restoredCount = tag.hasKey("Count", 99) ? tag.getByte("Count") : 1;
        int restoredDamage = tag.hasKey("Damage", 99) ? tag.getShort("Damage") : 0;
        ItemStack restoredStack = new ItemStack(restoredItem, Math.max(1, restoredCount), Math.max(0, restoredDamage));
        if (tag.hasKey("tag", 10)) {
            restoredStack.setTagCompound(tag.getCompoundTag("tag"));
        }
        return restoredStack;
    }
}