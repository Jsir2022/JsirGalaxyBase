package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.ParticipantType;
import com.jsirgalaxybase.quest.core.RewardDeliveryHandler;
import com.jsirgalaxybase.quest.core.RewardDeliveryLease;
import com.jsirgalaxybase.quest.core.RewardDeliveryOutcome;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Marker-based inventory delivery; world drops and final marker persistence are not an atomic operation. */
public final class MinecraftItemRewardDeliveryHandler implements RewardDeliveryHandler {
    private static final int PLAYER_INVENTORY_STACK_LIMIT = 64;

    private final String typeId;
    private final PlayerLookup players;
    private final ItemLookup itemLookup;

    public MinecraftItemRewardDeliveryHandler(String typeId, QuestPlayerResolver players,
        QuestPlayerDataFlusher flusher) {
        this(typeId, livePlayers(players, flusher), registryName -> {
            Object found = registryName == null ? null : Item.itemRegistry.getObject(registryName);
            return found instanceof Item ? (Item) found : null;
        });
    }

    MinecraftItemRewardDeliveryHandler(String typeId, QuestPlayerResolver players,
        QuestPlayerDataFlusher flusher, ItemLookup itemLookup) {
        this(typeId, livePlayers(players, flusher), itemLookup);
    }

    static MinecraftItemRewardDeliveryHandler forTest(String typeId, PlayerLookup players, ItemLookup itemLookup) {
        return new MinecraftItemRewardDeliveryHandler(typeId, players, itemLookup);
    }

    private MinecraftItemRewardDeliveryHandler(String typeId, PlayerLookup players, ItemLookup itemLookup) {
        if (!"bq_standard:item".equals(typeId) && !"bq_standard:choice".equals(typeId)) {
            throw new IllegalArgumentException("unsupported item reward type: " + typeId);
        }
        if (players == null || itemLookup == null) {
            throw new IllegalArgumentException("players and item lookup are required");
        }
        this.typeId = typeId;
        this.players = players;
        this.itemLookup = itemLookup;
    }

    private static PlayerLookup livePlayers(final QuestPlayerResolver players, final QuestPlayerDataFlusher flusher) {
        if (players == null || flusher == null) throw new IllegalArgumentException("players and flusher are required");
        return playerId -> {
            EntityPlayerMP player = players.findOnline(playerId);
            return player == null ? null : new LivePlayerState(player, flusher);
        };
    }

    @Override public String getTypeId() { return typeId; }

    @Override public RewardDeliveryOutcome deliver(RewardDeliveryLease lease) {
        if (lease.getParticipantId().getType() != ParticipantType.PLAYER) {
            return RewardDeliveryOutcome.failed("unsupported-participant:" + lease.getParticipantId().getType(), false);
        }
        PlayerState player = players.findOnline(lease.getParticipantId().getId());
        if (player == null) return RewardDeliveryOutcome.deferred("player-offline");
        String marker = QuestRewardMarker.key(lease.getEntitlementKey());
        NBTTagCompound persisted = player.persisted();
        boolean hadMarkers = persisted.hasKey(QuestRewardMarker.ROOT, 10);
        NBTTagCompound previousMarkers = hadMarkers
            ? (NBTTagCompound) persisted.getCompoundTag(QuestRewardMarker.ROOT).copy() : new NBTTagCompound();
        NBTTagCompound markers = (NBTTagCompound) previousMarkers.copy();
        if (markers.getBoolean(marker)) return RewardDeliveryOutcome.delivered();

        boolean hadProgress = persisted.hasKey(QuestRewardMarker.PROGRESS_ROOT, 10);
        NBTTagCompound previousProgress = hadProgress
            ? (NBTTagCompound) persisted.getCompoundTag(QuestRewardMarker.PROGRESS_ROOT).copy() : new NBTTagCompound();
        NBTTagCompound progressRoot = (NBTTagCompound) previousProgress.copy();
        if (progressRoot.hasKey(marker) && !progressRoot.hasKey(marker, 10)) {
            return RewardDeliveryOutcome.failed("invalid-reward-progress", false);
        }
        NBTTagCompound progress = progressRoot.hasKey(marker, 10)
            ? (NBTTagCompound) progressRoot.getCompoundTag(marker).copy() : new NBTTagCompound();

        List<ItemStack> before = player.snapshotMainInventory();
        List<ItemStack> rewards;
        try {
            rewards = rewardStacksForPlayerName(lease, player.name());
        } catch (IllegalArgumentException invalid) {
            return RewardDeliveryOutcome.failed("invalid-reward:" + invalid.getMessage(), false);
        }
        List<ItemStack> remaining = new ArrayList<ItemStack>();
        List<Integer> partIndexes = new ArrayList<Integer>();
        int[] granted = new int[rewards.size()];
        for (int i = 0; i < rewards.size(); i++) {
            ItemStack reward = rewards.get(i);
            final int alreadyGranted;
            try {
                alreadyGranted = QuestRewardMarker.granted(progress, i);
            } catch (IllegalArgumentException invalidProgress) {
                return RewardDeliveryOutcome.failed("invalid-reward-progress", false);
            }
            if (alreadyGranted > reward.stackSize) {
                return RewardDeliveryOutcome.failed("invalid-reward-progress", false);
            }
            granted[i] = alreadyGranted;
            int outstanding = reward.stackSize - alreadyGranted;
            if (outstanding > 0) {
                ItemStack remainder = reward.copy();
                remainder.stackSize = outstanding;
                remaining.add(remainder);
                partIndexes.add(i);
            }
        }

        PartialPlan plan = planPartial(before, remaining);
        boolean insertedAny = false;
        for (int i = 0; i < plan.inserted.length; i++) {
            int amount = plan.inserted[i];
            if (amount <= 0) continue;
            insertedAny = true;
            int partIndex = partIndexes.get(i);
            granted[partIndex] += amount;
            if (granted[partIndex] > rewards.get(partIndex).stackSize) {
                return RewardDeliveryOutcome.failed("invalid-reward-progress", false);
            }
        }
        List<ItemStack> overflow = new ArrayList<ItemStack>();
        List<Integer> overflowPartIndexes = new ArrayList<Integer>();
        for (int i = 0; i < plan.inserted.length; i++) {
            int amount = remaining.get(i).stackSize - plan.inserted[i];
            if (amount <= 0) continue;
            ItemStack stack = remaining.get(i).copy();
            stack.stackSize = amount;
            overflow.add(stack);
            overflowPartIndexes.add(partIndexes.get(i));
        }
        if (!overflow.isEmpty()) {
            return deliverOverflow(player, persisted, hadMarkers, previousMarkers, hadProgress, previousProgress,
                markers, marker, progressRoot, progress, before, plan, granted, overflow, overflowPartIndexes);
        }
        boolean complete = true;
        for (int i = 0; i < rewards.size(); i++) {
            if (granted[i] < rewards.get(i).stackSize) complete = false;
        }
        if (!complete && !insertedAny) return RewardDeliveryOutcome.deferred("waiting-for-inventory-capacity");

        player.restoreMainInventory(plan.inventory);
        player.markInventoryDirty();
        if (complete) {
            markers.setBoolean(marker, true);
            progressRoot.removeTag(marker);
            persisted.setTag(QuestRewardMarker.ROOT, markers);
            if (progressRoot.hasNoTags()) persisted.removeTag(QuestRewardMarker.PROGRESS_ROOT);
            else persisted.setTag(QuestRewardMarker.PROGRESS_ROOT, progressRoot);
        } else {
            for (int i = 0; i < granted.length; i++) QuestRewardMarker.setGranted(progress, i, granted[i]);
            progressRoot.setTag(marker, progress);
            persisted.setTag(QuestRewardMarker.PROGRESS_ROOT, progressRoot);
        }
        try {
            player.flush();
        } catch (RuntimeException failure) {
            player.restoreMainInventory(before);
            player.markInventoryDirty();
            restoreTag(persisted, QuestRewardMarker.ROOT, hadMarkers, previousMarkers);
            restoreTag(persisted, QuestRewardMarker.PROGRESS_ROOT, hadProgress, previousProgress);
            return RewardDeliveryOutcome.failed("player-save-failed:" + failure.getClass().getSimpleName(), true);
        }
        return complete ? RewardDeliveryOutcome.delivered()
            : RewardDeliveryOutcome.deferred("waiting-for-inventory-capacity");
    }

    List<ItemStack> rewardStacks(RewardDeliveryLease lease, EntityPlayerMP player) {
        return rewardStacksForPlayerName(lease, player == null ? null : player.getCommandSenderName());
    }

    private List<ItemStack> rewardStacksForPlayerName(RewardDeliveryLease lease, String playerName) {
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
            Item found = itemLookup.find(registryName);
            if (found == null) throw new IllegalArgumentException("unknown item " + registryName);
            int configuredAmount = integer(values.get(prefix + "amount"), 0);
            // RewardChoice returns before BigItemStack.getCombinedStacks() for non-positive choices.
            if ("bq_standard:choice".equals(typeId) && configuredAmount <= 0) continue;
            // RewardItem delegates to BigItemStack.getCombinedStacks(), which guarantees at least one item.
            int amount = Math.max(1, configuredAmount);
            ItemStack base = new ItemStack(found, 1, integer(values.get(prefix + "meta"), 0));
            NBTTagCompound tag = BqTypedNbtCodec.compound(values.get(prefix + "nbt"));
            if (!tag.hasNoTags()) base.setTagCompound(BqTypedNbtCodec.replacePlayerVariables(tag,
                playerName, lease.getParticipantId().getId().toString()));
            while (amount > 0) {
                ItemStack part = base.copy();
                part.stackSize = Math.min(amount, part.getMaxStackSize());
                result.add(part);
                amount -= part.stackSize;
            }
        }
        return result;
    }

    interface ItemLookup {
        Item find(String registryName);
    }

    interface PlayerLookup {
        PlayerState findOnline(UUID playerId);
    }

    interface PlayerState {
        String name();
        NBTTagCompound persisted();
        List<ItemStack> snapshotMainInventory();
        void restoreMainInventory(List<ItemStack> values);
        void markInventoryDirty();
        void dropOverflow(ItemStack stack);
        void flush();
    }

    private static RewardDeliveryOutcome deliverOverflow(PlayerState player, NBTTagCompound persisted,
        boolean hadMarkers, NBTTagCompound previousMarkers, boolean hadProgress, NBTTagCompound previousProgress,
        NBTTagCompound markers, String marker, NBTTagCompound progressRoot, NBTTagCompound progress,
        List<ItemStack> before, PartialPlan plan, int[] granted, List<ItemStack> overflow,
        List<Integer> overflowPartIndexes) {
        player.restoreMainInventory(plan.inventory);
        player.markInventoryDirty();
        for (int i = 0; i < granted.length; i++) QuestRewardMarker.setGranted(progress, i, granted[i]);
        progressRoot.setTag(marker, progress);
        persisted.setTag(QuestRewardMarker.PROGRESS_ROOT, progressRoot);
        try {
            player.flush();
        } catch (RuntimeException failure) {
            player.restoreMainInventory(before);
            player.markInventoryDirty();
            restoreTag(persisted, QuestRewardMarker.ROOT, hadMarkers, previousMarkers);
            restoreTag(persisted, QuestRewardMarker.PROGRESS_ROOT, hadProgress, previousProgress);
            return RewardDeliveryOutcome.failed("player-save-failed:" + failure.getClass().getSimpleName(), true);
        }

        for (int i = 0; i < overflow.size(); i++) {
            int partIndex = overflowPartIndexes.get(i);
            try {
                player.dropOverflow(overflow.get(i).copy());
                granted[partIndex] += overflow.get(i).stackSize;
            } catch (RuntimeException failure) {
                for (int j = 0; j < granted.length; j++) QuestRewardMarker.setGranted(progress, j, granted[j]);
                progressRoot.setTag(marker, progress);
                persisted.setTag(QuestRewardMarker.PROGRESS_ROOT, progressRoot);
                try { player.flush(); } catch (RuntimeException ignored) {}
                return RewardDeliveryOutcome.failed("world-drop-failed:" + failure.getClass().getSimpleName(), true);
            }
        }

        NBTTagCompound deliveredProgressRoot = (NBTTagCompound) progressRoot.copy();
        NBTTagCompound deliveredProgress = (NBTTagCompound) progress.copy();
        for (int i = 0; i < granted.length; i++) QuestRewardMarker.setGranted(deliveredProgress, i, granted[i]);
        deliveredProgressRoot.setTag(marker, deliveredProgress);

        markers.setBoolean(marker, true);
        persisted.setTag(QuestRewardMarker.ROOT, markers);
        progressRoot.removeTag(marker);
        if (progressRoot.hasNoTags()) persisted.removeTag(QuestRewardMarker.PROGRESS_ROOT);
        else persisted.setTag(QuestRewardMarker.PROGRESS_ROOT, progressRoot);
        try {
            player.flush();
        } catch (RuntimeException failure) {
            restoreTag(persisted, QuestRewardMarker.ROOT, hadMarkers, previousMarkers);
            if (deliveredProgressRoot.hasNoTags()) persisted.removeTag(QuestRewardMarker.PROGRESS_ROOT);
            else persisted.setTag(QuestRewardMarker.PROGRESS_ROOT, deliveredProgressRoot);
            try { player.flush(); } catch (RuntimeException ignored) {}
            return RewardDeliveryOutcome.failed("player-save-failed:" + failure.getClass().getSimpleName(), true);
        }
        return RewardDeliveryOutcome.delivered();
    }

    private static final class LivePlayerState implements PlayerState {
        private final EntityPlayerMP player;
        private final QuestPlayerDataFlusher flusher;

        private LivePlayerState(EntityPlayerMP player, QuestPlayerDataFlusher flusher) {
            this.player = player;
            this.flusher = flusher;
        }

        @Override public String name() { return player.getCommandSenderName(); }
        @Override public NBTTagCompound persisted() { return QuestRewardMarker.persisted(player); }
        @Override public List<ItemStack> snapshotMainInventory() {
            return MinecraftItemRewardDeliveryHandler.snapshot(player.inventory.mainInventory);
        }
        @Override public void restoreMainInventory(List<ItemStack> values) {
            MinecraftItemRewardDeliveryHandler.restore(player.inventory.mainInventory, values);
        }
        @Override public void markInventoryDirty() { player.inventory.markDirty(); }
        @Override public void dropOverflow(ItemStack stack) {
            player.dropPlayerItemWithRandomChoice(stack, false);
        }
        @Override public void flush() { flusher.flush(player); }
    }

    static List<ItemStack> plan(List<ItemStack> inventory, List<ItemStack> rewards) {
        PartialPlan partial = planPartial(inventory, rewards);
        for (int i = 0; i < rewards.size(); i++) {
            if (partial.inserted[i] != rewards.get(i).stackSize) return null;
        }
        return partial.inventory;
    }

    static PartialPlan planPartial(List<ItemStack> inventory, List<ItemStack> rewards) {
        List<ItemStack> working = copy(inventory);
        int[] inserted = new int[rewards.size()];
        for (int i = 0; i < rewards.size(); i++) inserted[i] = insert(working, rewards.get(i));
        return new PartialPlan(working, inserted);
    }

    private static int insert(List<ItemStack> inventory, ItemStack input) {
        ItemStack remaining = input.copy();
        int original = remaining.stackSize;
        for (ItemStack existing : inventory) {
            if (existing == null || !existing.isStackable() || existing.getItem() != remaining.getItem()
                || existing.getHasSubtypes() && existing.getItemDamage() != remaining.getItemDamage()
                || !ItemStack.areItemStackTagsEqual(existing, remaining)) continue;
            int moved = Math.min(remaining.stackSize,
                Math.max(0, Math.min(existing.getMaxStackSize(), PLAYER_INVENTORY_STACK_LIMIT) - existing.stackSize));
            existing.stackSize += moved;
            remaining.stackSize -= moved;
            if (remaining.stackSize <= 0) return original;
        }
        for (int slot = 0; slot < inventory.size() && remaining.stackSize > 0; slot++) {
            if (inventory.get(slot) != null) continue;
            int moved = Math.min(remaining.stackSize,
                Math.min(remaining.getMaxStackSize(), PLAYER_INVENTORY_STACK_LIMIT));
            ItemStack placed = remaining.copy();
            placed.stackSize = moved;
            inventory.set(slot, placed);
            remaining.stackSize -= moved;
        }
        return original - remaining.stackSize;
    }

    private static void restoreTag(NBTTagCompound persisted, String key, boolean existed, NBTTagCompound value) {
        if (existed) persisted.setTag(key, (NBTTagCompound) value.copy());
        else persisted.removeTag(key);
    }

    static final class PartialPlan {
        final List<ItemStack> inventory;
        final int[] inserted;
        PartialPlan(List<ItemStack> inventory, int[] inserted) {
            this.inventory = inventory;
            this.inserted = inserted;
        }
    }

    static List<ItemStack> snapshot(ItemStack[] inventory) {
        List<ItemStack> values = new ArrayList<ItemStack>(inventory.length);
        for (ItemStack value : inventory) values.add(value == null ? null : value.copy());
        return values;
    }

    private static List<ItemStack> copy(List<ItemStack> values) {
        List<ItemStack> result = new ArrayList<ItemStack>(values.size());
        for (ItemStack value : values) result.add(value == null ? null : value.copy());
        return result;
    }

    static void restore(ItemStack[] inventory, List<ItemStack> values) {
        for (int i = 0; i < inventory.length; i++) {
            inventory[i] = i < values.size() && values.get(i) != null ? values.get(i).copy() : null;
        }
    }

    private static int integer(String value, int fallback) {
        try { return value == null ? fallback : Integer.parseInt(value); }
        catch (NumberFormatException invalid) { throw new IllegalArgumentException("invalid integer " + value); }
    }
}
