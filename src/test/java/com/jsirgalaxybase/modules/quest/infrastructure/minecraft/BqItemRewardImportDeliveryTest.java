package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.jsirgalaxybase.quest.bqimport.BqQuestDefinitionImporter;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.RewardDeliveryLease;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class BqItemRewardImportDeliveryTest {
    @Test
    public void importedBqItemRewardFlowsThroughMinecraftDeliveryAndPersistsIdempotencyMarker() throws Exception {
        Path source = Files.createTempFile("bq-item-reward-delivery", ".json");
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":17,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Reward integration\"}},"
            + "\"tasks:9\":{},\"rewards:9\":{\"0:10\":{\"index:3\":0,"
            + "\"rewardID:8\":\"bq_standard:item\",\"rewards:9\":{\"0:10\":{"
            + "\"id:8\":\"test:bq_reward\",\"Damage:2\":3,\"Count:3\":130,"
            + "\"tag:10\":{\"owner:8\":\"VAR_NAME\",\"boundTo:8\":\"VAR_UUID\"}}}}}}";
        Files.write(source, json.getBytes(StandardCharsets.UTF_8));
        try {
            RewardDefinition importedReward = new BqQuestDefinitionImporter().read(source)
                .getDefinition().getRewards().get(0);
            UUID playerId = UUID.randomUUID();
            FakePlayerState player = new FakePlayerState("QuestRewardPlayer", 36);
            Item rewardItem = new Item();
            rewardItem.setHasSubtypes(true);
            rewardItem.setMaxStackSize(128);

            MinecraftItemRewardDeliveryHandler handler = MinecraftItemRewardDeliveryHandler.forTest(
                "bq_standard:item", requested -> {
                    assertEquals(playerId, requested);
                    return player;
                }, registryName -> "test:bq_reward".equals(registryName) ? rewardItem : null);
            RewardDeliveryLease lease = new RewardDeliveryLease("imported-bq-item-entitlement",
                ParticipantId.player(playerId), importedReward, 1, "test-worker", 100L);

            assertTrue(handler.deliver(lease).isDelivered());

            List<ItemStack> granted = nonEmpty(player.inventory);
            assertEquals(3, granted.size());
            assertEquals(130, count(rewardItem, granted));
            assertEquals(java.util.Arrays.asList(64, 64, 2), stackSizes(granted));
            for (ItemStack stack : granted) {
                assertSame(rewardItem, stack.getItem());
                assertEquals(3, stack.getItemDamage());
                assertEquals("QuestRewardPlayer", stack.getTagCompound().getString("owner"));
                assertEquals(playerId.toString(), stack.getTagCompound().getString("boundTo"));
            }
            assertTrue(player.persisted.getCompoundTag(QuestRewardMarker.ROOT)
                .getBoolean(QuestRewardMarker.key("imported-bq-item-entitlement")));
            assertEquals(1, player.flushCount);

            assertTrue(handler.deliver(lease).isDelivered());
            assertEquals(130, count(rewardItem, nonEmpty(player.inventory)));
            assertEquals(1, player.flushCount);
        } finally {
            Files.deleteIfExists(source);
        }
    }

    private static List<ItemStack> nonEmpty(List<ItemStack> inventory) {
        List<ItemStack> result = new ArrayList<ItemStack>();
        for (ItemStack stack : inventory) if (stack != null) result.add(stack);
        return result;
    }

    private static int count(Item item, List<ItemStack> stacks) {
        int result = 0;
        for (ItemStack stack : stacks) if (stack.getItem() == item) result += stack.stackSize;
        return result;
    }

    private static List<Integer> stackSizes(List<ItemStack> stacks) {
        List<Integer> result = new ArrayList<Integer>();
        for (ItemStack stack : stacks) result.add(stack.stackSize);
        return result;
    }

    private static final class FakePlayerState implements MinecraftItemRewardDeliveryHandler.PlayerState {
        private final String name;
        private final List<ItemStack> inventory;
        private final NBTTagCompound persisted = new NBTTagCompound();
        private int flushCount;

        private FakePlayerState(String name, int slots) {
            this.name = name;
            this.inventory = new ArrayList<ItemStack>(Collections.<ItemStack>nCopies(slots, null));
        }

        @Override public String name() { return name; }
        @Override public NBTTagCompound persisted() { return persisted; }
        @Override public List<ItemStack> snapshotMainInventory() {
            List<ItemStack> result = new ArrayList<ItemStack>(inventory.size());
            for (ItemStack stack : inventory) result.add(stack == null ? null : stack.copy());
            return result;
        }
        @Override public void restoreMainInventory(List<ItemStack> values) {
            for (int index = 0; index < inventory.size(); index++) {
                ItemStack stack = index < values.size() ? values.get(index) : null;
                inventory.set(index, stack == null ? null : stack.copy());
            }
        }
        @Override public void markInventoryDirty() { }
        @Override public void dropOverflow(ItemStack stack) { throw new AssertionError("unexpected overflow"); }
        @Override public void flush() { flushCount++; }
    }
}
