package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.CommandRewardInvocation;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestLogic;
import com.jsirgalaxybase.quest.core.QuestTransaction;
import com.jsirgalaxybase.quest.core.RepeatPolicy;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.RewardDeliveryBatchResult;
import com.jsirgalaxybase.quest.core.RewardDeliveryLease;
import com.jsirgalaxybase.quest.core.RewardDeliveryOutcome;
import com.jsirgalaxybase.quest.core.RewardDeliveryRepository;
import com.jsirgalaxybase.quest.core.RewardDeliveryWorker;
import com.jsirgalaxybase.quest.core.RewardEntitlement;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.RegistryNamespaced;

public class MinecraftRewardDeliveryTest {
    @Test public void rewardInsertionMergesThenUsesEmptySlotsWithoutMutatingSource() {
        Item item = new Item();
        List<ItemStack> before = new ArrayList<ItemStack>(Arrays.asList(new ItemStack(item, 60), null));
        List<ItemStack> after = MinecraftItemRewardDeliveryHandler.plan(before,
            Arrays.asList(new ItemStack(item, 10)));
        assertEquals(60, before.get(0).stackSize);
        assertEquals(64, after.get(0).stackSize);
        assertEquals(6, after.get(1).stackSize);
    }

    @Test public void rewardInsertionIgnoresDamageForItemsWithoutSubtypesLikeVanillaInventory() {
        Item item = new Item();
        List<ItemStack> before = new ArrayList<ItemStack>(Arrays.asList(new ItemStack(item, 60, 1), null));

        List<ItemStack> after = MinecraftItemRewardDeliveryHandler.plan(before,
            Arrays.asList(new ItemStack(item, 10, 2)));

        assertEquals(60, before.get(0).stackSize);
        assertEquals(64, after.get(0).stackSize);
        assertEquals(1, after.get(0).getItemDamage());
        assertEquals(6, after.get(1).stackSize);
        assertEquals(2, after.get(1).getItemDamage());
    }

    @Test public void damagedStacksDoNotMergeEvenWhenItemAllowsMultiplePerStack() {
        Item item = new Item().setMaxDamage(100).setMaxStackSize(16);
        List<ItemStack> before = new ArrayList<ItemStack>(Arrays.asList(new ItemStack(item, 3, 7), null));

        List<ItemStack> after = MinecraftItemRewardDeliveryHandler.plan(before,
            Arrays.asList(new ItemStack(item, 2, 7)));

        assertEquals(3, after.get(0).stackSize);
        assertEquals(7, after.get(0).getItemDamage());
        assertEquals(2, after.get(1).stackSize);
        assertEquals(7, after.get(1).getItemDamage());
    }

    @Test public void oversizedItemStacksRespectPlayerInventoryLimitWhenPlannedAtomically() {
        Item item = new Item();
        item.setMaxStackSize(128);
        List<ItemStack> before = Arrays.asList(null, null, null);

        List<ItemStack> after = MinecraftItemRewardDeliveryHandler.plan(before,
            Arrays.asList(new ItemStack(item, 130)));

        assertEquals(Arrays.asList(null, null, null), before);
        assertEquals(64, after.get(0).stackSize);
        assertEquals(64, after.get(1).stackSize);
        assertEquals(2, after.get(2).stackSize);
    }

    @Test public void observedMaximumRewardAmountDefersWithoutPartiallyMutatingInventory() {
        Item item = new Item();
        List<ItemStack> before = new ArrayList<ItemStack>(Collections.<ItemStack>nCopies(36, null));

        List<ItemStack> after = MinecraftItemRewardDeliveryHandler.plan(before,
            Arrays.asList(new ItemStack(item, 8192)));

        assertNull(after);
        for (ItemStack slot : before) assertNull(slot);
    }

    @Test public void oversizedRewardCanBeDeliveredInPersistedInventorySizedBatches() {
        Item item = new Item();
        NBTTagCompound progress = new NBTTagCompound();
        int total = 0;
        int batches = 0;
        while (total < 8192 && batches < 8) {
            List<ItemStack> emptyInventory = new ArrayList<ItemStack>(Collections.<ItemStack>nCopies(36, null));
            int alreadyGranted = QuestRewardMarker.granted(progress, 0);
            MinecraftItemRewardDeliveryHandler.PartialPlan batch = MinecraftItemRewardDeliveryHandler.planPartial(
                emptyInventory, Arrays.asList(new ItemStack(item, 8192 - alreadyGranted)));

            total = alreadyGranted + batch.inserted[0];
            QuestRewardMarker.setGranted(progress, 0, total);
            assertEquals(batch.inserted[0], batch.inventory.stream()
                .mapToInt(stack -> stack == null ? 0 : stack.stackSize).sum());
            for (ItemStack slot : emptyInventory) assertNull(slot);
            batches++;
        }

        assertEquals(4, batches);
        assertEquals(8192, total);
        assertEquals(8192, QuestRewardMarker.granted(progress, 0));
    }

    @Test public void rewardProgressNbtIsValidatedAndCanBeCleared() {
        NBTTagCompound progress = new NBTTagCompound();
        assertEquals(0, QuestRewardMarker.granted(progress, 2));
        QuestRewardMarker.setGranted(progress, 2, 2304);
        assertEquals(2304, QuestRewardMarker.granted(progress, 2));
        QuestRewardMarker.setGranted(progress, 2, 0);
        assertEquals(0, QuestRewardMarker.granted(progress, 2));
        progress.setString("3", "corrupt");
        try {
            QuestRewardMarker.granted(progress, 3);
            throw new AssertionError("wrong NBT type should be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("invalid reward progress"));
        }
    }

    @Test public void fullInventoryRejectsWholeReward() {
        Item item = new Item();
        List<ItemStack> before = Arrays.asList(new ItemStack(item, 64));
        assertNull(MinecraftItemRewardDeliveryHandler.plan(before, Arrays.asList(new ItemStack(item, 1))));
        assertEquals(64, before.get(0).stackSize);
    }

    @Test public void rewardSnapshotUsesMainInventoryAndDoesNotTreatArmorAsFreeStorage() {
        Item backpackItem = new Item();
        Item rewardItem = new Item();
        Item armorItem = new Item();
        ItemStack[] mainInventory = new ItemStack[36];
        ItemStack[] armorInventory = new ItemStack[4];
        for (int i = 0; i < mainInventory.length; i++) mainInventory[i] = new ItemStack(backpackItem, 64);
        ItemStack equippedArmor = new ItemStack(armorItem, 1);
        armorInventory[0] = equippedArmor;

        List<ItemStack> before = MinecraftItemRewardDeliveryHandler.snapshot(mainInventory);
        assertEquals(36, before.size());
        assertNull(MinecraftItemRewardDeliveryHandler.plan(before,
            Arrays.asList(new ItemStack(rewardItem, 1))));
        MinecraftItemRewardDeliveryHandler.restore(mainInventory, before);

        assertSame(equippedArmor, armorInventory[0]);
        assertEquals(64, mainInventory[0].stackSize);
    }

    @Test public void emptyItemRewardDoesNotChangeInventory() {
        Item item = new Item();
        List<ItemStack> before = Arrays.asList(new ItemStack(item, 64));
        List<ItemStack> after = MinecraftItemRewardDeliveryHandler.plan(before,
            Collections.<ItemStack>emptyList());
        assertEquals(64, before.get(0).stackSize);
        assertEquals(64, after.get(0).stackSize);
    }

    @Test public void choiceRewardMaterializesOnlyTheSelectedBqItemStack() {
        Item alternative = new Item();
        Item selected = new Item();

        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("item.count", "2");
        parameters.put("choice.index", "1");
        parameters.put("item.0.registryName", "galaxybase:choice_test_alternative");
        parameters.put("item.0.amount", "999");
        parameters.put("item.1.registryName", "galaxybase:choice_test_selected");
        parameters.put("item.1.amount", "130");
        parameters.put("item.1.meta", "3");
        RewardDefinition reward = new RewardDefinition("choice", "bq_standard:choice", parameters);
        RewardDeliveryLease lease = new RewardDeliveryLease("choice-entitlement", ParticipantId.player(UUID.randomUUID()),
            reward, 1, "worker", 10L);
        QuestPlayerResolver unusedPlayers = playerId -> null;
        QuestPlayerDataFlusher unusedFlusher = player -> { };
        MinecraftItemRewardDeliveryHandler.ItemLookup itemLookup = registryName -> {
            if ("galaxybase:choice_test_alternative".equals(registryName)) return alternative;
            if ("galaxybase:choice_test_selected".equals(registryName)) return selected;
            return null;
        };

        List<ItemStack> stacks = new MinecraftItemRewardDeliveryHandler("bq_standard:choice", unusedPlayers,
            unusedFlusher, itemLookup).rewardStacks(lease, null);

        assertEquals(3, stacks.size());
        assertSame(selected, stacks.get(0).getItem());
        assertSame(selected, stacks.get(1).getItem());
        assertSame(selected, stacks.get(2).getItem());
        assertEquals(Arrays.asList(64, 64, 2), Arrays.asList(
            stacks.get(0).stackSize, stacks.get(1).stackSize, stacks.get(2).stackSize));
        assertEquals(3, stacks.get(0).getItemDamage());
    }

    @Test public void nonPositiveItemRewardCountMatchesBqMinimumStackSize() {
        Item item = new Item();
        UUID playerId = UUID.randomUUID();
        MinecraftItemRewardDeliveryHandler handler = MinecraftItemRewardDeliveryHandler.forTest("bq_standard:item",
            ignored -> null, name -> "test:reward".equals(name) ? item : null);

        List<ItemStack> zeroCount = handler.rewardStacks(itemLease("zero-count", playerId, "test:reward", 0), null);
        List<ItemStack> negativeCount = handler.rewardStacks(
            itemLease("negative-count", playerId, "test:reward", -5), null);

        assertEquals(1, zeroCount.size());
        assertEquals(1, zeroCount.get(0).stackSize);
        assertEquals(1, negativeCount.size());
        assertEquals(1, negativeCount.get(0).stackSize);
    }

    @Test public void nonPositiveChoiceRewardIsNoOpAndCompletesItsDeliveryMarker() {
        Item item = new Item();
        UUID playerId = UUID.randomUUID();
        FakePlayerState player = new FakePlayerState(2);
        RewardDefinition reward = new RewardDefinition("choice", "bq_standard:choice", choiceParameters(0));
        RewardDeliveryLease negativeLease = new RewardDeliveryLease("negative-choice", ParticipantId.player(playerId),
            new RewardDefinition("choice", "bq_standard:choice", choiceParameters(-3)), 1, "worker", 10L);
        RewardDeliveryLease lease = new RewardDeliveryLease("zero-choice", ParticipantId.player(playerId), reward,
            1, "worker", 10L);
        MinecraftItemRewardDeliveryHandler handler = MinecraftItemRewardDeliveryHandler.forTest(
            "bq_standard:choice", ignored -> player, name -> "test:choice".equals(name) ? item : null);

        assertTrue(handler.rewardStacks(lease, null).isEmpty());
        assertTrue(handler.rewardStacks(negativeLease, null).isEmpty());
        RewardDeliveryOutcome first = handler.deliver(lease);

        assertTrue(first.getError(), first.isDelivered());
        assertEquals(0, countItem(player.inventory, item));
        assertTrue(player.persisted.getCompoundTag(QuestRewardMarker.ROOT)
            .getBoolean(QuestRewardMarker.key("zero-choice")));
        assertEquals(1, player.flushCount);

        RewardDeliveryOutcome replay = handler.deliver(lease);

        assertTrue(replay.isDelivered());
        assertEquals(0, countItem(player.inventory, item));
        assertEquals(1, player.flushCount);
    }

    private static Map<String, String> choiceParameters(int amount) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("item.count", "1");
        parameters.put("choice.index", "0");
        parameters.put("item.0.registryName", "test:choice");
        parameters.put("item.0.amount", Integer.toString(amount));
        parameters.put("item.0.meta", "0");
        return parameters;
    }

    @Test public void rewardOverflowUsesBqItemSpecificMaximumStackSize() {
        Item item = new Item().setMaxStackSize(128);
        UUID playerId = UUID.randomUUID();
        RewardDeliveryLease lease = itemLease("oversized-max-stack", playerId, "test:wide-stack", 130);
        FakePlayerState player = new FakePlayerState(0);
        MinecraftItemRewardDeliveryHandler handler = itemHandler(playerId, player, "test:wide-stack", item);

        List<ItemStack> plannedRewardStacks = handler.rewardStacks(lease, null);
        RewardDeliveryOutcome outcome = handler.deliver(lease);

        assertEquals(Arrays.asList(128, 2), stackSizes(plannedRewardStacks));
        assertTrue(outcome.getError(), outcome.isDelivered());
        assertEquals(Arrays.asList(128, 2), stackSizes(player.dropped));
    }

    @Test public void fullInventoryDropsBqOverflowAndReplayDoesNotDropAgain() {
        Item item = new Item();
        FakePlayerState player = new FakePlayerState(1);
        player.inventory.set(0, new ItemStack(item, 60));
        UUID playerId = UUID.randomUUID();
        RewardDeliveryLease lease = itemLease("overflow-entitlement", playerId, "test:reward", 130);
        MinecraftItemRewardDeliveryHandler handler = itemHandler(playerId, player, "test:reward", item);

        RewardDeliveryOutcome first = handler.deliver(lease);

        assertTrue(first.getError(), first.isDelivered());
        assertEquals(64, player.inventory.get(0).stackSize);
        assertEquals(Arrays.asList(60, 64, 2), stackSizes(player.dropped));
        assertTrue(player.persisted.getCompoundTag(QuestRewardMarker.ROOT)
            .getBoolean(QuestRewardMarker.key("overflow-entitlement")));

        RewardDeliveryOutcome replay = handler.deliver(lease);

        assertTrue(replay.isDelivered());
        assertEquals(Arrays.asList(60, 64, 2), stackSizes(player.dropped));
        assertEquals(2, player.flushCount);
    }

    @Test public void overflowSaveFailurePersistsDeliveryProgressBeforeRetry() {
        Item item = new Item();
        FakePlayerState player = new FakePlayerState(1);
        player.inventory.set(0, new ItemStack(item, 60));
        player.failAtFlush = 2;
        UUID playerId = UUID.randomUUID();
        RewardDeliveryLease lease = itemLease("overflow-retry", playerId, "test:reward", 130);
        MinecraftItemRewardDeliveryHandler handler = itemHandler(playerId, player, "test:reward", item);

        RewardDeliveryOutcome failed = handler.deliver(lease);

        assertTrue(failed.isRetryable());
        assertEquals(Arrays.asList(60, 64, 2), stackSizes(player.dropped));
        assertFalse(player.persisted.getCompoundTag(QuestRewardMarker.ROOT)
            .getBoolean(QuestRewardMarker.key("overflow-retry")));
        NBTTagCompound progress = player.persisted.getCompoundTag(QuestRewardMarker.PROGRESS_ROOT)
            .getCompoundTag(QuestRewardMarker.key("overflow-retry"));
        assertEquals(64, QuestRewardMarker.granted(progress, 0));
        assertEquals(64, QuestRewardMarker.granted(progress, 1));
        assertEquals(2, QuestRewardMarker.granted(progress, 2));

        RewardDeliveryOutcome retry = handler.deliver(lease);

        assertTrue(retry.getError(), retry.isDelivered());
        assertEquals(Arrays.asList(60, 64, 2), stackSizes(player.dropped));
        assertTrue(player.persisted.getCompoundTag(QuestRewardMarker.ROOT)
            .getBoolean(QuestRewardMarker.key("overflow-retry")));
    }

    @Test public void itemDeliveryPersistsInventoryAndEntitlementMarkerAndReplayIsIdempotent() {
        Item item = new Item();
        FakePlayerState player = new FakePlayerState(2);
        player.inventory.set(0, new ItemStack(item, 60));
        UUID playerId = UUID.randomUUID();
        RewardDeliveryLease lease = itemLease("item-entitlement", playerId, "test:reward", 10);
        MinecraftItemRewardDeliveryHandler handler = itemHandler(playerId, player, "test:reward", item);

        RewardDeliveryOutcome first = handler.deliver(lease);

        assertTrue(first.getError(), first.isDelivered());
        assertEquals(64, player.inventory.get(0).stackSize);
        assertEquals(6, player.inventory.get(1).stackSize);
        assertTrue(player.persisted.getCompoundTag(QuestRewardMarker.ROOT)
            .getBoolean(QuestRewardMarker.key("item-entitlement")));
        assertEquals(1, player.flushCount);

        RewardDeliveryOutcome duplicate = handler.deliver(lease);

        assertTrue(duplicate.isDelivered());
        assertEquals(64, player.inventory.get(0).stackSize);
        assertEquals(6, player.inventory.get(1).stackSize);
        assertEquals(1, player.flushCount);
    }

    @Test public void overflowProgressAndInventorySurviveCompressedPlayerNbtReloadAndRetry()
        throws IOException, ReflectiveOperationException {
        Item item = registerTestItemForNbtRoundTrip();
        UUID playerId = UUID.randomUUID();
        RewardDeliveryLease lease = itemLease("restart-progress", playerId, "test:reward", 70);
        FakePlayerState beforeRestart = new FakePlayerState(36);
        for (int slot = 0; slot < 35; slot++) beforeRestart.inventory.set(slot, new ItemStack(item, 64));
        beforeRestart.failNextDrop = true;
        MinecraftItemRewardDeliveryHandler firstHandler = itemHandler(
            playerId, beforeRestart, "test:reward", item);

        RewardDeliveryOutcome interrupted = firstHandler.deliver(lease);

        assertTrue(interrupted.isRetryable());
        assertEquals(36 * 64, countItem(beforeRestart.inventory, item));
        assertTrue(beforeRestart.dropped.isEmpty());
        String marker = QuestRewardMarker.key("restart-progress");
        NBTTagCompound progress = beforeRestart.persisted.getCompoundTag(QuestRewardMarker.PROGRESS_ROOT)
            .getCompoundTag(marker);
        assertEquals(64, QuestRewardMarker.granted(progress, 0));

        FakePlayerState afterRestart = reloadFromCompressedPlayerNbt(beforeRestart);
        assertEquals(36 * 64, countItem(afterRestart.inventory, item));
        assertEquals(64, QuestRewardMarker.granted(afterRestart.persisted
            .getCompoundTag(QuestRewardMarker.PROGRESS_ROOT).getCompoundTag(marker), 0));
        MinecraftItemRewardDeliveryHandler resumedHandler = itemHandler(
            playerId, afterRestart, "test:reward", item);

        RewardDeliveryOutcome resumed = resumedHandler.deliver(lease);

        assertTrue(resumed.getError(), resumed.isDelivered());
        assertEquals(36 * 64, countItem(afterRestart.inventory, item));
        assertEquals(Collections.singletonList(6), stackSizes(afterRestart.dropped));
        assertTrue(afterRestart.persisted.getCompoundTag(QuestRewardMarker.ROOT).getBoolean(marker));
        assertFalse(afterRestart.persisted.hasKey(QuestRewardMarker.PROGRESS_ROOT, 10));
    }

    @Test public void delayedPreviousCycleItemRewardRecoveryDoesNotReuseNextCycleMarker()
        throws IOException, ReflectiveOperationException {
        Item item = registerTestItemForNbtRoundTrip();
        Item filler = registerTestItemForNbtRoundTrip();
        UUID playerId = UUID.randomUUID();
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("item.count", "1");
        parameters.put("item.0.registryName", "test:repeat-reward");
        parameters.put("item.0.amount", "70");
        parameters.put("item.0.meta", "0");
        RewardDefinition reward = new RewardDefinition("repeat-item", "bq_standard:item", parameters);
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Repeated item reward", "",
            QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(), Collections.emptyList(),
            Collections.singletonList(reward), RepeatPolicy.after(1000L, true));
        RewardEntitlement previousCycle = new RewardEntitlement(ParticipantId.player(playerId), quest, reward, 0);
        RewardEntitlement currentCycle = new RewardEntitlement(ParticipantId.player(playerId), quest, reward, 1);
        RewardDeliveryLease previousLease = itemLease(previousCycle, 1, "server-a");
        RewardDeliveryLease currentLease = itemLease(currentCycle, 1, "server-b");
        assertFalse(previousLease.getEntitlementKey().equals(currentLease.getEntitlementKey()));

        FakePlayerState beforeRestart = new FakePlayerState(36);
        for (int slot = 0; slot < 35; slot++) beforeRestart.inventory.set(slot, new ItemStack(filler, 64));
        beforeRestart.failNextDrop = true;
        RewardDeliveryOutcome partial = itemHandler(playerId, beforeRestart, "test:repeat-reward", item)
            .deliver(previousLease);
        assertTrue(partial.isRetryable());
        FakePlayerState afterRestart = reloadFromCompressedPlayerNbt(beforeRestart);

        MinecraftItemRewardDeliveryHandler restartedHandler = itemHandler(
            playerId, afterRestart, "test:repeat-reward", item);
        RewardDeliveryOutcome currentDelivered = restartedHandler.deliver(currentLease);
        String previousMarker = QuestRewardMarker.key(previousCycle.getEntitlementKey());
        String currentMarker = QuestRewardMarker.key(currentCycle.getEntitlementKey());
        assertTrue(currentDelivered.getError(), currentDelivered.isDelivered());
        assertTrue(afterRestart.persisted.getCompoundTag(QuestRewardMarker.ROOT).getBoolean(currentMarker));
        assertFalse(afterRestart.persisted.getCompoundTag(QuestRewardMarker.ROOT).getBoolean(previousMarker));
        assertEquals(64, QuestRewardMarker.granted(afterRestart.persisted
            .getCompoundTag(QuestRewardMarker.PROGRESS_ROOT).getCompoundTag(previousMarker), 0));

        RewardDeliveryOutcome previousRecovered = restartedHandler.deliver(itemLease(previousCycle, 2, "server-c"));

        assertTrue(previousRecovered.getError(), previousRecovered.isDelivered());
        assertTrue(afterRestart.persisted.getCompoundTag(QuestRewardMarker.ROOT).getBoolean(currentMarker));
        assertTrue(afterRestart.persisted.getCompoundTag(QuestRewardMarker.ROOT).getBoolean(previousMarker));
        assertFalse(afterRestart.persisted.hasKey(QuestRewardMarker.PROGRESS_ROOT, 10));
        assertEquals(64, countItem(afterRestart.inventory, item));
        assertEquals(Arrays.asList(64, 6, 6), stackSizes(afterRestart.dropped));
        assertEquals(140, countItem(afterRestart.inventory, item) + totalStackSize(afterRestart.dropped));
    }

    @Test public void failedPlayerSaveRollsBackInventoryAndMarkerThenCanRetry() {
        Item item = new Item();
        FakePlayerState player = new FakePlayerState(2);
        player.failNextFlush = true;
        UUID playerId = UUID.randomUUID();
        RewardDeliveryLease lease = itemLease("retry-entitlement", playerId, "test:reward", 3);
        MinecraftItemRewardDeliveryHandler handler = itemHandler(playerId, player, "test:reward", item);

        RewardDeliveryOutcome failed = handler.deliver(lease);

        assertTrue(failed.isRetryable());
        assertNull(player.inventory.get(0));
        assertFalse(player.persisted.hasKey(QuestRewardMarker.ROOT));
        assertEquals(1, player.flushCount);

        RewardDeliveryOutcome retried = handler.deliver(lease);

        assertTrue(retried.getError(), retried.isDelivered());
        assertEquals(3, player.inventory.get(0).stackSize);
        assertTrue(player.persisted.getCompoundTag(QuestRewardMarker.ROOT)
            .getBoolean(QuestRewardMarker.key("retry-entitlement")));
        assertEquals(2, player.flushCount);
    }

    @Test public void staleLeaseConfirmationReplaysAfterPlayerSaveWithoutDuplicatingReward() {
        Item item = new Item();
        FakePlayerState player = new FakePlayerState(2);
        UUID playerId = UUID.randomUUID();
        RewardDeliveryLease firstLease = itemLease("lease-replay", playerId, "test:reward", 7, 1, "worker-a");
        RewardDeliveryLease retryLease = itemLease("lease-replay", playerId, "test:reward", 7, 2, "worker-b");
        LeaseRetryRepository repository = new LeaseRetryRepository();
        repository.nextLease = firstLease;
        repository.acceptConfirmation = false;
        RewardDeliveryWorker firstWorker = rewardWorker(repository,
            itemHandler(playerId, player, "test:reward", item));

        RewardDeliveryBatchResult stale = firstWorker.runOnce("worker-a", 1);

        assertEquals(1, stale.getLeased());
        assertEquals(0, stale.getDelivered());
        assertEquals(1, stale.getStaleConfirmations());
        assertEquals(7, countItem(player.inventory, item));
        assertTrue(player.persisted.getCompoundTag(QuestRewardMarker.ROOT)
            .getBoolean(QuestRewardMarker.key("lease-replay")));
        assertEquals(1, player.flushCount);

        repository.nextLease = retryLease;
        repository.acceptConfirmation = true;
        RewardDeliveryWorker restartedWorker = rewardWorker(repository,
            itemHandler(playerId, player, "test:reward", item));
        RewardDeliveryBatchResult recovered = restartedWorker.runOnce("worker-b", 1);

        assertEquals(1, recovered.getDelivered());
        assertEquals(0, recovered.getStaleConfirmations());
        assertEquals(7, countItem(player.inventory, item));
        assertEquals(1, player.flushCount);
        assertEquals(Arrays.asList("worker-a:1", "worker-b:2"), repository.confirmations);
    }

    @Test public void partialInventoryInsertionDropsRemainderAndReplayIsIdempotent() {
        Item filler = new Item();
        Item reward = new Item();
        FakePlayerState player = new FakePlayerState(36);
        for (int slot = 0; slot < 35; slot++) player.inventory.set(slot, new ItemStack(filler, 64));
        UUID playerId = UUID.randomUUID();
        RewardDeliveryLease lease = itemLease("partial-entitlement", playerId, "test:reward", 65);
        MinecraftItemRewardDeliveryHandler handler = itemHandler(playerId, player, "test:reward", reward);

        RewardDeliveryOutcome first = handler.deliver(lease);

        assertTrue(first.getError(), first.isDelivered());
        assertEquals(64, countItem(player.inventory, reward));
        assertEquals(Arrays.asList(1), stackSizes(player.dropped));
        assertTrue(player.persisted.getCompoundTag(QuestRewardMarker.ROOT)
            .getBoolean(QuestRewardMarker.key("partial-entitlement")));
        assertFalse(player.persisted.hasKey(QuestRewardMarker.PROGRESS_ROOT));

        player.inventory.set(34, null);
        RewardDeliveryOutcome second = handler.deliver(lease);

        assertTrue(second.getError(), second.isDelivered());
        assertEquals(64, countItem(player.inventory, reward));
        assertEquals(Arrays.asList(1), stackSizes(player.dropped));
        assertTrue(player.persisted.getCompoundTag(QuestRewardMarker.ROOT)
            .getBoolean(QuestRewardMarker.key("partial-entitlement")));
        assertFalse(player.persisted.hasKey(QuestRewardMarker.PROGRESS_ROOT));
        assertEquals(2, player.flushCount);
    }

    @Test public void typedNbtReplacesPlayerVariablesRecursively() {
        NBTTagCompound decoded = BqTypedNbtCodec.compound(
            "{\"owner:8\":\"VAR_NAME\",\"nested:10\":{\"id:8\":\"VAR_UUID\"}}");
        NBTTagCompound replaced = BqTypedNbtCodec.replacePlayerVariables(decoded, "Alex", "uuid-1");
        assertEquals("Alex", replaced.getString("owner"));
        assertEquals("uuid-1", replaced.getCompoundTag("nested").getString("id"));
        assertEquals("VAR_NAME", decoded.getString("owner"));
    }

    @Test public void xpCurveMatchesBetterQuestingBoundaries() {
        assertEquals(255L, MinecraftXpRewardDeliveryHandler.levelXp(15));
        assertEquals(272L, MinecraftXpRewardDeliveryHandler.levelXp(16));
        assertEquals(825L, MinecraftXpRewardDeliveryHandler.levelXp(30));
        assertEquals(887L, MinecraftXpRewardDeliveryHandler.levelXp(31));
        for (int level : new int[] { 0, 1, 15, 16, 30, 31, 100, 1000000 }) {
            long points = MinecraftXpRewardDeliveryHandler.levelXp(level);
            assertEquals(level, MinecraftXpRewardDeliveryHandler.xpLevel(points));
            assertTrue(MinecraftXpRewardDeliveryHandler.levelXp(level + 1) > points);
        }
    }

    @Test public void xpRewardDeliveryPersistsLevelGrantBeforeSyncAndIsIdempotent() {
        UUID playerId = UUID.randomUUID();
        RewardDeliveryLease lease = xpLease("xp-entitlement", playerId, 2, "levels");
        FakeExperiencePort player = new FakeExperiencePort(273, 15, 0.5F);

        RewardDeliveryOutcome first = MinecraftXpRewardDeliveryHandler.deliverToPlayer(lease, player);

        assertTrue(first.getError(), first.isDelivered());
        assertEquals(Integer.valueOf(307), Integer.valueOf(player.total));
        assertEquals(Integer.valueOf(17), Integer.valueOf(player.level));
        assertEquals(Float.valueOf(15F / 47F), Float.valueOf(player.progress));
        assertTrue(player.markers.contains(QuestRewardMarker.key("xp-entitlement")));
        assertEquals(Arrays.asList("flush", "sync"), player.events);

        RewardDeliveryOutcome duplicate = MinecraftXpRewardDeliveryHandler.deliverToPlayer(lease, player);

        assertTrue(duplicate.isDelivered());
        assertEquals(Integer.valueOf(307), Integer.valueOf(player.total));
        assertEquals(2, player.events.size());
    }

    @Test public void xpRewardDeliveryRestoresExperienceAndMarkerWhenPlayerSaveFails() {
        UUID playerId = UUID.randomUUID();
        RewardDeliveryLease lease = xpLease("xp-failed-entitlement", playerId, 2, "levels");
        FakeExperiencePort player = new FakeExperiencePort(273, 15, 0.5F);
        player.failFlush = true;

        RewardDeliveryOutcome result = MinecraftXpRewardDeliveryHandler.deliverToPlayer(lease, player);

        assertTrue(result.getError(), result.isRetryable());
        assertEquals(Integer.valueOf(273), Integer.valueOf(player.total));
        assertEquals(Integer.valueOf(15), Integer.valueOf(player.level));
        assertEquals(Float.valueOf(0.5F), Float.valueOf(player.progress));
        assertFalse(player.markers.contains(QuestRewardMarker.key("xp-failed-entitlement")));
        assertEquals(Collections.singletonList("flush"), player.events);
    }

    @Test public void entitlementMarkersAreStableAndNamespaced() {
        assertEquals(QuestRewardMarker.key("entitlement"), QuestRewardMarker.key("entitlement"));
        assertFalse(QuestRewardMarker.key("entitlement").equals(QuestRewardMarker.key("other")));
        assertEquals(64, QuestRewardMarker.key("entitlement").length());
    }

    @Test public void relativeScoreRewardComputesOneStableSaturatingAbsoluteTarget() {
        assertEquals(15, MinecraftScoreboardRewardDeliveryHandler.targetScore(10, 5, true));
        assertEquals(5, MinecraftScoreboardRewardDeliveryHandler.targetScore(10, 5, false));
        assertEquals(Integer.MAX_VALUE,
            MinecraftScoreboardRewardDeliveryHandler.targetScore(Integer.MAX_VALUE, 1, true));
        assertEquals(Integer.MIN_VALUE,
            MinecraftScoreboardRewardDeliveryHandler.targetScore(Integer.MIN_VALUE, -1, true));
    }

    @Test public void frozenGroupRecipientIsResolvedAsTheConcretePlayerAndOfflineDeliveryIsDeferred() {
        UUID recipient=UUID.randomUUID();ParticipantId team=ParticipantId.team(UUID.randomUUID());
        RewardDeliveryLease lease=new RewardDeliveryLease("team-entitlement",team,recipient,
            new RewardDefinition("reward","bq_standard:item",Collections.<String,String>emptyMap()),1,"worker",10L);
        QuestPlayerResolver offline=id->{assertEquals(recipient,id);return null;};
        QuestPlayerDataFlusher unused=player->{throw new AssertionError("offline player must not be flushed");};
        RewardDeliveryOutcome item=new MinecraftItemRewardDeliveryHandler("bq_standard:item",offline,unused).deliver(lease);
        RewardDeliveryOutcome xp=new MinecraftXpRewardDeliveryHandler(offline,unused).deliver(lease);
        RewardDeliveryOutcome score=new MinecraftScoreboardRewardDeliveryHandler(offline,unused).deliver(lease);
        RewardDeliveryOutcome command=new MinecraftCommandRewardExecutor(offline,unused).execute(
            new CommandRewardInvocation(lease.getEntitlementKey(),lease.getParticipantId(),"galaxyreward",false));
        assertEquals(team,lease.getProgressParticipantId());assertEquals(ParticipantId.player(recipient),lease.getParticipantId());
        assertTrue(item.isDeferred());assertTrue(xp.isDeferred());assertTrue(score.isDeferred());assertTrue(command.isDeferred());
    }

    private static RewardDeliveryLease itemLease(String entitlement, UUID playerId, String registryName, int amount) {
        return itemLease(entitlement, playerId, registryName, amount, 1, "worker");
    }

    private static RewardDeliveryLease itemLease(RewardEntitlement entitlement, int attempt, String workerId) {
        return new RewardDeliveryLease(entitlement.getEntitlementKey(), entitlement.getParticipantId(),
            entitlement.getReward(), attempt, workerId, 10L);
    }

    private static RewardDeliveryLease xpLease(String entitlement, UUID playerId, int amount, String unit) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("amount", Integer.toString(amount));
        parameters.put("xpUnit", unit);
        return new RewardDeliveryLease(entitlement, ParticipantId.player(playerId),
            new RewardDefinition("xp", "bq_standard:xp", parameters), 1, "worker", 10L);
    }

    private static RewardDeliveryLease itemLease(String entitlement, UUID playerId, String registryName, int amount,
        int attempt, String workerId) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("item.count", "1");
        parameters.put("item.0.registryName", registryName);
        parameters.put("item.0.amount", Integer.toString(amount));
        parameters.put("item.0.meta", "0");
        RewardDefinition reward = new RewardDefinition("item", "bq_standard:item", parameters);
        return new RewardDeliveryLease(entitlement, ParticipantId.player(playerId), reward, attempt, workerId, 10L);
    }

    private static RewardDeliveryWorker rewardWorker(RewardDeliveryRepository repository,
        MinecraftItemRewardDeliveryHandler delivery) {
        QuestTransaction directTransaction = new QuestTransaction() {
            @Override public <T> T inTransaction(Work<T> work) { return work.execute(); }
        };
        return new RewardDeliveryWorker(repository, directTransaction, lease -> delivery.deliver(lease),
            () -> 1000L, 1000L, 50L, 3);
    }

    private static MinecraftItemRewardDeliveryHandler itemHandler(final UUID expectedPlayer, final FakePlayerState player,
        final String registryName, final Item item) {
        return MinecraftItemRewardDeliveryHandler.forTest("bq_standard:item", playerId -> {
            assertEquals(expectedPlayer, playerId);
            return player;
        }, name -> registryName.equals(name) ? item : null);
    }

    private static FakePlayerState reloadFromCompressedPlayerNbt(FakePlayerState player) throws IOException {
        // Mirrors Entity.writeToNBT's ForgeData and uses InventoryPlayer's actual slot/item NBT implementation.
        NBTTagCompound playerSave = new NBTTagCompound();
        NBTTagCompound forgeData = new NBTTagCompound();
        forgeData.setTag(EntityPlayer.PERSISTED_NBT_TAG, (NBTTagCompound) player.persisted.copy());
        playerSave.setTag("ForgeData", forgeData);
        InventoryPlayer sourceInventory = new InventoryPlayer(null);
        for (int slot = 0; slot < player.inventory.size(); slot++) {
            ItemStack stack = player.inventory.get(slot);
            sourceInventory.mainInventory[slot] = stack == null ? null : stack.copy();
        }
        playerSave.setTag("Inventory", sourceInventory.writeToNBT(new NBTTagList()));

        byte[] compressed = CompressedStreamTools.compress(playerSave);
        NBTTagCompound reloadedSave = CompressedStreamTools.readCompressed(new ByteArrayInputStream(compressed));
        InventoryPlayer reloadedInventory = new InventoryPlayer(null);
        reloadedInventory.readFromNBT(reloadedSave.getTagList("Inventory", 10));
        FakePlayerState reloaded = new FakePlayerState(reloadedInventory.mainInventory.length);
        reloaded.persisted = reloadedSave.getCompoundTag("ForgeData")
            .getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        for (int slot = 0; slot < reloadedInventory.mainInventory.length; slot++) {
            ItemStack stack = reloadedInventory.mainInventory[slot];
            reloaded.inventory.set(slot, stack == null ? null : stack.copy());
        }
        return reloaded;
    }

    private static Item registerTestItemForNbtRoundTrip() throws ReflectiveOperationException {
        Item item = new Item();
        Field identityMapField = RegistryNamespaced.class.getDeclaredField("underlyingIntegerMap");
        identityMapField.setAccessible(true);
        ObjectIntIdentityMap identityMap = (ObjectIntIdentityMap) identityMapField.get(Item.itemRegistry);
        int id = Short.MAX_VALUE;
        while (identityMap.func_148744_b(id)) id--;
        identityMap.func_148746_a(item, id);
        assertSame(item, Item.getItemById(id));
        assertEquals(id, Item.getIdFromItem(item));
        return item;
    }

    private static int countItem(List<ItemStack> inventory, Item item) {
        int count = 0;
        for (ItemStack stack : inventory) if (stack != null && stack.getItem() == item) count += stack.stackSize;
        return count;
    }

    private static int totalStackSize(List<ItemStack> stacks) {
        int count = 0;
        for (ItemStack stack : stacks) count += stack.stackSize;
        return count;
    }

    private static List<Integer> stackSizes(List<ItemStack> stacks) {
        List<Integer> result = new ArrayList<Integer>();
        for (ItemStack stack : stacks) result.add(stack.stackSize);
        return result;
    }

    private static final class FakeExperiencePort implements MinecraftXpRewardDeliveryHandler.ExperiencePort {
        private int total;
        private int level;
        private float progress;
        private boolean failFlush;
        private final Set<String> markers = new HashSet<String>();
        private final List<String> events = new ArrayList<String>();

        private FakeExperiencePort(int total, int level, float progress) {
            this.total = total;
            this.level = level;
            this.progress = progress;
        }

        @Override public int experienceTotal() { return total; }
        @Override public int experienceLevel() { return level; }
        @Override public float experienceProgress() { return progress; }
        @Override public void setExperience(int total, int level, float progress) {
            this.total = total;
            this.level = level;
            this.progress = progress;
        }
        @Override public boolean hasMarker(String marker) { return markers.contains(marker); }
        @Override public void setMarker(String marker) { markers.add(marker); }
        @Override public void removeMarker(String marker) { markers.remove(marker); }
        @Override public void flush() {
            events.add("flush");
            if (failFlush) {
                failFlush = false;
                throw new IllegalStateException("simulated XP write failure");
            }
        }
        @Override public void syncExperience() { events.add("sync"); }
    }

    private static final class FakePlayerState implements MinecraftItemRewardDeliveryHandler.PlayerState {
        private final List<ItemStack> inventory;
        private final List<ItemStack> dropped = new ArrayList<ItemStack>();
        private NBTTagCompound persisted = new NBTTagCompound();
        private int flushCount;
        private boolean failNextFlush;
        private boolean failNextDrop;
        private int failAtFlush;

        private FakePlayerState(int slots) {
            inventory = new ArrayList<ItemStack>(Collections.<ItemStack>nCopies(slots, null));
        }

        @Override public String name() { return "RewardTestPlayer"; }
        @Override public NBTTagCompound persisted() { return persisted; }
        @Override public List<ItemStack> snapshotMainInventory() {
            List<ItemStack> copy = new ArrayList<ItemStack>(inventory.size());
            for (ItemStack stack : inventory) copy.add(stack == null ? null : stack.copy());
            return copy;
        }
        @Override public void restoreMainInventory(List<ItemStack> values) {
            for (int index = 0; index < inventory.size(); index++) {
                ItemStack value = index < values.size() ? values.get(index) : null;
                inventory.set(index, value == null ? null : value.copy());
            }
        }
        @Override public void markInventoryDirty() { }
        @Override public void dropOverflow(ItemStack stack) {
            if (failNextDrop) {
                failNextDrop = false;
                throw new IllegalStateException("simulated world-drop failure");
            }
            dropped.add(stack.copy());
        }
        @Override public void flush() {
            flushCount++;
            if (failNextFlush || flushCount == failAtFlush) {
                failNextFlush = false;
                throw new IllegalStateException("simulated player-data write failure");
            }
        }
    }

    private static final class LeaseRetryRepository implements RewardDeliveryRepository {
        private RewardDeliveryLease nextLease;
        private boolean acceptConfirmation;
        private final List<String> confirmations = new ArrayList<String>();

        @Override public List<RewardDeliveryLease> leaseAvailable(String workerId, long now, long leaseUntil, int limit) {
            if (nextLease == null) return Collections.emptyList();
            RewardDeliveryLease lease = nextLease;
            nextLease = null;
            return Collections.singletonList(lease);
        }

        @Override public boolean markDelivered(String entitlementKey, String workerId, int attempt, long deliveredAt) {
            confirmations.add(workerId + ":" + attempt);
            return acceptConfirmation;
        }

        @Override public boolean markDeferred(String entitlementKey, String workerId, int attempt, String reason,
            long deferredAt, long retryAt) {
            throw new AssertionError("unexpected deferred delivery");
        }

        @Override public boolean markFailed(String entitlementKey, String workerId, int attempt, String error,
            long failedAt, long retryAt, boolean retryable) {
            throw new AssertionError("unexpected failed delivery");
        }
    }
}
