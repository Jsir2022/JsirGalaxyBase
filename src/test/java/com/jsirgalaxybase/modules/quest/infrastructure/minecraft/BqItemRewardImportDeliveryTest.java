package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.jsirgalaxybase.quest.bqimport.BqQuestDefinitionImporter;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.RewardDeliveryLease;
import com.jsirgalaxybase.quest.core.RewardDeliveryOutcome;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

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

    @Test
    public void importedBqItemRewardPreservesTypedNestedNbtListsAndArrays() throws Exception {
        Path source = Files.createTempFile("bq-item-reward-typed-nbt", ".json");
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":22,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Typed NBT integration\"}},"
            + "\"tasks:9\":{},\"rewards:9\":{\"0:10\":{\"index:3\":0,"
            + "\"rewardID:8\":\"bq_standard:item\",\"rewards:9\":{\"0:10\":{"
            + "\"id:8\":\"test:bq_typed_reward\",\"Damage:2\":5,\"Count:3\":1,"
            + "\"tag:10\":{\"byteValue:1\":7,\"shortValue:2\":123,\"intValue:3\":4567,"
            + "\"longValue:4\":1234567890123,\"floatValue:5\":1.25,\"doubleValue:6\":2.5,"
            + "\"bytes:7\":[1,-2],\"ints:11\":[3,4],"
            + "\"entries:9\":{\"0:10\":{\"owner:8\":\"VAR_NAME\",\"rank:3\":9,"
            + "\"tokens:9\":{\"0:8\":\"VAR_UUID\"}}},"
            + "\"nested:10\":{\"enabled:1\":1}}}}}}}";
        Files.write(source, json.getBytes(StandardCharsets.UTF_8));
        try {
            RewardDefinition importedReward = new BqQuestDefinitionImporter().read(source)
                .getDefinition().getRewards().get(0);
            UUID playerId = UUID.randomUUID();
            FakePlayerState player = new FakePlayerState("TypedRewardPlayer", 36);
            Item rewardItem = new Item().setHasSubtypes(true);
            MinecraftItemRewardDeliveryHandler handler = MinecraftItemRewardDeliveryHandler.forTest(
                "bq_standard:item", requested -> {
                    assertEquals(playerId, requested);
                    return player;
                }, registryName -> "test:bq_typed_reward".equals(registryName) ? rewardItem : null);
            RewardDeliveryLease lease = new RewardDeliveryLease("imported-bq-typed-nbt-entitlement",
                ParticipantId.player(playerId), importedReward, 1, "test-worker", 100L);

            RewardDeliveryOutcome outcome = handler.deliver(lease);
            assertTrue(outcome.getError(), outcome.isDelivered());

            ItemStack granted = nonEmpty(player.inventory).get(0);
            assertEquals(5, granted.getItemDamage());
            NBTTagCompound tag = granted.getTagCompound();
            assertTrue(tag.hasKey("byteValue", 1));
            assertEquals(7, tag.getByte("byteValue"));
            assertTrue(tag.hasKey("shortValue", 2));
            assertEquals(123, tag.getShort("shortValue"));
            assertTrue(tag.hasKey("intValue", 3));
            assertEquals(4567, tag.getInteger("intValue"));
            assertTrue(tag.hasKey("longValue", 4));
            assertEquals(1234567890123L, tag.getLong("longValue"));
            assertTrue(tag.hasKey("floatValue", 5));
            assertEquals(1.25F, tag.getFloat("floatValue"), 0.0F);
            assertTrue(tag.hasKey("doubleValue", 6));
            assertEquals(2.5D, tag.getDouble("doubleValue"), 0.0D);
            assertArrayEquals(new byte[] {1, -2}, tag.getByteArray("bytes"));
            assertArrayEquals(new int[] {3, 4}, tag.getIntArray("ints"));

            NBTTagList entries = tag.getTagList("entries", 10);
            assertEquals(1, entries.tagCount());
            NBTTagCompound entry = entries.getCompoundTagAt(0);
            assertEquals("TypedRewardPlayer", entry.getString("owner"));
            assertEquals(9, entry.getInteger("rank"));
            assertEquals(1, entry.getTagList("tokens", 8).tagCount());
            assertEquals(playerId.toString(), entry.getTagList("tokens", 8).getStringTagAt(0));
            assertTrue(tag.getCompoundTag("nested").hasKey("enabled", 1));
            assertEquals(1, tag.getCompoundTag("nested").getByte("enabled"));
        } finally {
            Files.deleteIfExists(source);
        }
    }

    @Test
    public void importedBqMaximumItemRewardSplitsAndDropsOverflowOnce() throws Exception {
        Path source = Files.createTempFile("bq-item-reward-overflow", ".json");
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":21,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Large reward integration\"}},"
            + "\"tasks:9\":{},\"rewards:9\":{\"0:10\":{\"index:3\":0,"
            + "\"rewardID:8\":\"bq_standard:item\",\"rewards:9\":{\"0:10\":{"
            + "\"id:8\":\"test:bq_large_reward\",\"Damage:2\":0,\"Count:3\":8192}}}}}";
        Files.write(source, json.getBytes(StandardCharsets.UTF_8));
        try {
            RewardDefinition importedReward = new BqQuestDefinitionImporter().read(source)
                .getDefinition().getRewards().get(0);
            UUID playerId = UUID.randomUUID();
            FakePlayerState player = new FakePlayerState("LargeQuestRewardPlayer", 36);
            Item rewardItem = new Item();
            MinecraftItemRewardDeliveryHandler handler = MinecraftItemRewardDeliveryHandler.forTest(
                "bq_standard:item", requested -> {
                    assertEquals(playerId, requested);
                    return player;
                }, registryName -> "test:bq_large_reward".equals(registryName) ? rewardItem : null);
            RewardDeliveryLease lease = new RewardDeliveryLease("imported-bq-max-item-entitlement",
                ParticipantId.player(playerId), importedReward, 1, "test-worker", 100L);

            assertTrue(handler.deliver(lease).isDelivered());

            List<ItemStack> granted = nonEmpty(player.inventory);
            assertEquals(36, granted.size());
            assertEquals(Collections.nCopies(36, 64), stackSizes(granted));
            assertEquals(8192, count(rewardItem, granted) + count(rewardItem, player.dropped));
            assertEquals(2304, count(rewardItem, granted));
            assertEquals(92, player.dropped.size());
            assertEquals(Collections.nCopies(92, 64), stackSizes(player.dropped));
            assertEquals(5888, count(rewardItem, player.dropped));
            assertTrue(player.persisted.getCompoundTag(QuestRewardMarker.ROOT)
                .getBoolean(QuestRewardMarker.key("imported-bq-max-item-entitlement")));
            assertEquals(2, player.flushCount);

            assertTrue(handler.deliver(lease).isDelivered());
            assertEquals(92, player.dropped.size());
            assertEquals(8192, count(rewardItem, nonEmpty(player.inventory)) + count(rewardItem, player.dropped));
            assertEquals(2, player.flushCount);
        } finally {
            Files.deleteIfExists(source);
        }
    }

    @Test
    public void importedBqChoiceRewardDeliversOnlyTheSelectedImportedOption() throws Exception {
        Path source = Files.createTempFile("bq-choice-reward-delivery", ".json");
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":18,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Choice reward integration\"}},"
            + "\"tasks:9\":{},\"rewards:9\":{\"0:10\":{\"index:3\":0,"
            + "\"rewardID:8\":\"bq_standard:choice\",\"choices:9\":{"
            + "\"0:10\":{\"id:8\":\"test:bq_emerald\",\"Damage:2\":2,\"Count:3\":5,"
            + "\"tag:10\":{\"owner:8\":\"VAR_NAME\"}},"
            + "\"1:10\":{\"id:8\":\"test:bq_diamond\",\"Damage:2\":4,\"Count:3\":3,"
            + "\"tag:10\":{\"boundTo:8\":\"VAR_UUID\"}}}}}}";
        Files.write(source, json.getBytes(StandardCharsets.UTF_8));
        try {
            RewardDefinition importedReward = new BqQuestDefinitionImporter().read(source)
                .getDefinition().getRewards().get(0);
            assertEquals("bq_standard:choice", importedReward.getTypeId());
            assertEquals("player-required", importedReward.getParameters().get("selectionMode"));
            assertEquals("2", importedReward.getParameters().get("item.count"));
            assertEquals("test:bq_diamond", importedReward.getParameters().get("item.1.registryName"));
            assertEquals("4", importedReward.getParameters().get("item.1.meta"));
            assertEquals("3", importedReward.getParameters().get("item.1.amount"));

            Map<String, String> selectedParameters = new LinkedHashMap<String, String>(
                importedReward.getParameters());
            selectedParameters.put("choice.index", "1");
            RewardDefinition selectedReward = new RewardDefinition(importedReward.getKey(),
                importedReward.getTypeId(), selectedParameters);
            UUID playerId = UUID.randomUUID();
            FakePlayerState player = new FakePlayerState("ChoiceRewardPlayer", 36);
            Item emerald = new Item();
            Item diamond = new Item().setHasSubtypes(true);
            MinecraftItemRewardDeliveryHandler handler = MinecraftItemRewardDeliveryHandler.forTest(
                "bq_standard:choice", requested -> {
                    assertEquals(playerId, requested);
                    return player;
                }, registryName -> "test:bq_emerald".equals(registryName) ? emerald
                    : "test:bq_diamond".equals(registryName) ? diamond : null);
            RewardDeliveryLease lease = new RewardDeliveryLease("imported-bq-choice-entitlement",
                ParticipantId.player(playerId), selectedReward, 1, "test-worker", 100L);

            assertTrue(handler.deliver(lease).isDelivered());

            List<ItemStack> granted = nonEmpty(player.inventory);
            assertEquals(1, granted.size());
            assertEquals(3, count(diamond, granted));
            assertEquals(0, count(emerald, granted));
            assertSame(diamond, granted.get(0).getItem());
            assertEquals(4, granted.get(0).getItemDamage());
            assertEquals(playerId.toString(), granted.get(0).getTagCompound().getString("boundTo"));
            assertTrue(player.persisted.getCompoundTag(QuestRewardMarker.ROOT)
                .getBoolean(QuestRewardMarker.key("imported-bq-choice-entitlement")));
            assertEquals(1, player.flushCount);

            assertTrue(handler.deliver(lease).isDelivered());
            assertEquals(3, count(diamond, nonEmpty(player.inventory)));
            assertEquals(1, player.flushCount);
        } finally {
            Files.deleteIfExists(source);
        }
    }

    @Test
    public void importedBqXpRewardPreservesLevelVersusPointSemanticsThroughDelivery() throws Exception {
        RewardDefinition levelReward = importXpReward(19, 2, true);
        RewardDefinition pointReward = importXpReward(20, 2, false);
        assertEquals("levels", levelReward.getParameters().get("xpUnit"));
        assertEquals("points", pointReward.getParameters().get("xpUnit"));

        UUID playerId = UUID.randomUUID();
        FakeExperiencePort levels = new FakeExperiencePort(273, 15, 0.5F);
        RewardDeliveryLease levelLease = new RewardDeliveryLease("imported-bq-xp-levels",
            ParticipantId.player(playerId), levelReward, 1, "test-worker", 100L);
        RewardDeliveryOutcome levelOutcome = MinecraftXpRewardDeliveryHandler.deliverToPlayer(levelLease, levels);
        assertTrue(levelOutcome.getError(), levelOutcome.isDelivered());
        assertEquals(307, levels.total);
        assertEquals(17, levels.level);
        assertEquals(15F / 47F, levels.progress, 0.0001F);
        assertTrue(levels.markers.contains(QuestRewardMarker.key("imported-bq-xp-levels")));
        assertEquals(java.util.Arrays.asList("flush", "sync"), levels.events);
        assertTrue(MinecraftXpRewardDeliveryHandler.deliverToPlayer(levelLease, levels).isDelivered());
        assertEquals(307, levels.total);
        assertEquals(java.util.Arrays.asList("flush", "sync"), levels.events);

        FakeExperiencePort points = new FakeExperiencePort(273, 15, 0.5F);
        RewardDeliveryLease pointLease = new RewardDeliveryLease("imported-bq-xp-points",
            ParticipantId.player(playerId), pointReward, 1, "test-worker", 100L);
        RewardDeliveryOutcome pointOutcome = MinecraftXpRewardDeliveryHandler.deliverToPlayer(pointLease, points);
        assertTrue(pointOutcome.getError(), pointOutcome.isDelivered());
        assertEquals(275, points.total);
        assertEquals(16, points.level);
        assertEquals(3F / 42F, points.progress, 0.0001F);
        assertFalse(points.markers.contains(QuestRewardMarker.key("imported-bq-xp-levels")));
        assertTrue(points.markers.contains(QuestRewardMarker.key("imported-bq-xp-points")));
        assertTrue(MinecraftXpRewardDeliveryHandler.deliverToPlayer(pointLease, points).isDelivered());
        assertEquals(275, points.total);
        assertEquals(java.util.Arrays.asList("flush", "sync"), points.events);
    }

    private static RewardDefinition importXpReward(int questId, int amount, boolean levels) throws Exception {
        Path source = Files.createTempFile("bq-xp-reward-delivery", ".json");
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":" + questId
            + ",\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"XP reward integration\"}},"
            + "\"tasks:9\":{},\"rewards:9\":{\"0:10\":{\"index:3\":0,"
            + "\"rewardID:8\":\"bq_standard:xp\",\"amount:3\":" + amount
            + ",\"isLevels:1\":" + (levels ? 1 : 0) + "}}}";
        Files.write(source, json.getBytes(StandardCharsets.UTF_8));
        try {
            return new BqQuestDefinitionImporter().read(source).getDefinition().getRewards().get(0);
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
        private final List<ItemStack> dropped = new ArrayList<ItemStack>();
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
        @Override public void dropOverflow(ItemStack stack) { dropped.add(stack.copy()); }
        @Override public void flush() { flushCount++; }
    }

    private static final class FakeExperiencePort implements MinecraftXpRewardDeliveryHandler.ExperiencePort {
        private int total;
        private int level;
        private float progress;
        private final java.util.Set<String> markers = new java.util.HashSet<String>();
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
        @Override public void flush() { events.add("flush"); }
        @Override public void syncExperience() { events.add("sync"); }
    }
}
