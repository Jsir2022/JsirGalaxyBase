package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.jsirgalaxybase.quest.core.ParticipantType;
import com.jsirgalaxybase.quest.core.RewardDeliveryHandler;
import com.jsirgalaxybase.quest.core.RewardDeliveryLease;
import com.jsirgalaxybase.quest.core.RewardDeliveryOutcome;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Crash-safe player inventory delivery for imported BQ item and choice rewards. */
public final class MinecraftItemRewardDeliveryHandler implements RewardDeliveryHandler {
    private final String typeId;
    private final QuestPlayerResolver players;
    private final QuestPlayerDataFlusher flusher;

    public MinecraftItemRewardDeliveryHandler(String typeId, QuestPlayerResolver players,
        QuestPlayerDataFlusher flusher) {
        if (!"bq_standard:item".equals(typeId) && !"bq_standard:choice".equals(typeId)) {
            throw new IllegalArgumentException("unsupported item reward type: " + typeId);
        }
        if (players == null || flusher == null) throw new IllegalArgumentException("players and flusher are required");
        this.typeId = typeId;
        this.players = players;
        this.flusher = flusher;
    }

    @Override public String getTypeId() { return typeId; }

    @Override public RewardDeliveryOutcome deliver(RewardDeliveryLease lease) {
        if (lease.getParticipantId().getType() != ParticipantType.PLAYER) {
            return RewardDeliveryOutcome.failed("unsupported-participant:" + lease.getParticipantId().getType(), false);
        }
        EntityPlayerMP player = players.findOnline(lease.getParticipantId().getId());
        if (player == null) return RewardDeliveryOutcome.deferred("player-offline");
        String marker = QuestRewardMarker.key(lease.getEntitlementKey());
        NBTTagCompound persisted = QuestRewardMarker.persisted(player);
        NBTTagCompound markers = persisted.getCompoundTag(QuestRewardMarker.ROOT);
        if (markers.getBoolean(marker)) return RewardDeliveryOutcome.delivered();

        List<ItemStack> before = snapshot(player);
        List<ItemStack> after;
        try {
            after = plan(before, rewardStacks(lease, player));
        } catch (IllegalArgumentException invalid) {
            return RewardDeliveryOutcome.failed("invalid-reward:" + invalid.getMessage(), false);
        }
        if (after == null) return RewardDeliveryOutcome.failed("inventory-full", true);
        restore(player, after);
        markers.setBoolean(marker, true);
        persisted.setTag(QuestRewardMarker.ROOT, markers);
        try {
            flusher.flush(player);
        } catch (RuntimeException failure) {
            restore(player, before);
            markers.removeTag(marker);
            persisted.setTag(QuestRewardMarker.ROOT, markers);
            return RewardDeliveryOutcome.failed("player-save-failed:" + failure.getClass().getSimpleName(), true);
        }
        return RewardDeliveryOutcome.delivered();
    }

    List<ItemStack> rewardStacks(RewardDeliveryLease lease, EntityPlayerMP player) {
        Map<String, String> values = lease.getReward().getParameters();
        int count = integer(values.get("item.count"), -1);
        if (count < 0) throw new IllegalArgumentException("missing item.count");
        int first = 0;
        int last = count;
        if ("bq_standard:choice".equals(typeId)) {
            first = integer(values.get("choice.index"), -1);
            if (first < 0 || first >= count) throw new IllegalArgumentException("invalid choice.index");
            last = first + 1;
        }
        List<ItemStack> result = new ArrayList<ItemStack>();
        for (int i = first; i < last; i++) {
            String prefix = "item." + i + ".";
            String registryName = values.get(prefix + "registryName");
            Object found = registryName == null ? null : Item.itemRegistry.getObject(registryName);
            if (!(found instanceof Item)) throw new IllegalArgumentException("unknown item " + registryName);
            int amount = integer(values.get(prefix + "amount"), 0);
            if (amount <= 0) continue;
            ItemStack base = new ItemStack((Item) found, 1, integer(values.get(prefix + "meta"), 0));
            NBTTagCompound tag = BqTypedNbtCodec.compound(values.get(prefix + "nbt"));
            if (!tag.hasNoTags()) base.setTagCompound(BqTypedNbtCodec.replacePlayerVariables(tag,
                player.getCommandSenderName(), lease.getParticipantId().getId().toString()));
            while (amount > 0) {
                ItemStack part = base.copy();
                part.stackSize = Math.min(amount, Math.min(part.getMaxStackSize(), 64));
                result.add(part);
                amount -= part.stackSize;
            }
        }
        return result;
    }

    static List<ItemStack> plan(List<ItemStack> inventory, List<ItemStack> rewards) {
        List<ItemStack> working = copy(inventory);
        for (ItemStack reward : rewards) if (!insert(working, reward)) return null;
        return working;
    }

    private static boolean insert(List<ItemStack> inventory, ItemStack input) {
        ItemStack remaining = input.copy();
        for (ItemStack existing : inventory) {
            if (existing == null || existing.getItem() != remaining.getItem()
                || existing.getItemDamage() != remaining.getItemDamage()
                || !ItemStack.areItemStackTagsEqual(existing, remaining)) continue;
            int moved = Math.min(remaining.stackSize,
                Math.max(0, Math.min(existing.getMaxStackSize(), 64) - existing.stackSize));
            existing.stackSize += moved;
            remaining.stackSize -= moved;
            if (remaining.stackSize <= 0) return true;
        }
        for (int slot = 0; slot < inventory.size() && remaining.stackSize > 0; slot++) {
            if (inventory.get(slot) != null) continue;
            int moved = Math.min(remaining.stackSize, Math.min(remaining.getMaxStackSize(), 64));
            ItemStack placed = remaining.copy();
            placed.stackSize = moved;
            inventory.set(slot, placed);
            remaining.stackSize -= moved;
        }
        return remaining.stackSize <= 0;
    }

    private static List<ItemStack> snapshot(EntityPlayerMP player) {
        List<ItemStack> values = new ArrayList<ItemStack>(player.inventory.getSizeInventory());
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack value = player.inventory.getStackInSlot(i);
            values.add(value == null ? null : value.copy());
        }
        return values;
    }

    private static List<ItemStack> copy(List<ItemStack> values) {
        List<ItemStack> result = new ArrayList<ItemStack>(values.size());
        for (ItemStack value : values) result.add(value == null ? null : value.copy());
        return result;
    }

    private static void restore(EntityPlayerMP player, List<ItemStack> values) {
        for (int i = 0; i < values.size(); i++) player.inventory.setInventorySlotContents(i,
            values.get(i) == null ? null : values.get(i).copy());
        player.inventory.markDirty();
    }

    private static int integer(String value, int fallback) {
        try { return value == null ? fallback : Integer.parseInt(value); }
        catch (NumberFormatException invalid) { throw new IllegalArgumentException("invalid integer " + value); }
    }
}
