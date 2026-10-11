package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntSupplier;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.GameplayFact;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestDefinitionLifecycle;
import com.jsirgalaxybase.quest.core.QuestLogic;
import com.jsirgalaxybase.quest.core.QuestObservationPlan;
import com.jsirgalaxybase.quest.core.QuestObservationPlanProvider;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.StoredQuestDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class QuestCraftingStatisticsDispatchTest {

    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000071");
    private static final UUID PARTY_MEMBER_ID = UUID.fromString("00000000-0000-0000-0000-000000000072");
    private static final Item IRON = new Item();
    private static final Item GOLD = new Item();
    private static final Item DIAMOND = new Item();
    private static final Item STICK = new Item();

    @Test
    public void craftingAmountFallbackMatchesBqShiftCraftBoundary() {
        final int[] fallbackCalls = {0};
        IntSupplier recipeAmount = new IntSupplier() {
            @Override public int getAsInt() {
                fallbackCalls[0]++;
                return 4;
            }
        };

        assertEquals(4, QuestGameplayEventHandler.resolveCraftedOutputAmount(0, true, recipeAmount));
        assertEquals("BQ recomputes only the zero-sized shift-craft result", 1, fallbackCalls[0]);
        assertEquals(-2, QuestGameplayEventHandler.resolveCraftedOutputAmount(-2, true, recipeAmount));
        assertEquals(2, QuestGameplayEventHandler.resolveCraftedOutputAmount(2, true, recipeAmount));
        assertEquals(0, QuestGameplayEventHandler.resolveCraftedOutputAmount(0, false, recipeAmount));
        assertEquals("negative/positive reports and missing matrices must not invoke the fallback",
            1, fallbackCalls[0]);
    }

    @Test
    public void craftingOutputsDispatchAllStatisticTargetsFromMatchingTaskOnly() {
        QuestObservationPlan plan = QuestObservationPlan.fromPublished(Arrays.asList(
            stored(craftingTask("metal", "minecraft:iron_ingot", "minecraft:gold_ingot")),
            stored(craftingTask("gem", "minecraft:diamond"))));
        assertTrue("published Crafting definitions should enable statistic observation",
            plan.observesCraftingStatistics());
        RecordingSink sink = new RecordingSink();
        RecordingStatisticFactProducer statisticFacts = new RecordingStatisticFactProducer();
        assertEquals(Arrays.asList("minecraft:iron_ingot", "minecraft:gold_ingot"),
            statisticNames(plan.craftingStatisticsForOutput(statisticFacts.outputAttributes(
                new ItemStack(IRON), 3, "craft"))));
        QuestGameplayEventHandler handler = new QuestGameplayEventHandler(sink,
            new MinecraftQuestFactFactory("crafting-test"),
            new QuestObservationPlanProvider() {
                @Override public QuestObservationPlan current() { return plan; }
            }, new com.jsirgalaxybase.quest.core.QuestClock() {
                @Override public long currentTimeMillis() { return 1L; }
            }, statisticFacts, new QuestGameplayEventHandler.CraftingStatisticMemberResolver() {
                @Override public List<UUID> localOnlineMembers(
                    net.minecraft.entity.player.EntityPlayer triggerPlayer) {
                    return Arrays.asList(PLAYER_ID);
                }
            });

        handler.submitCraftingStatistics(null, new ItemStack(STICK), 1, "craft");
        assertTrue("unrelated output must not sample any crafting statistic", statisticFacts.requested.isEmpty());
        assertTrue("unrelated output must not persist a statistic fact", sink.facts.isEmpty());

        handler.submitCraftingStatistics(null, new ItemStack(IRON), 3, "craft");
        handler.submitCraftingStatistics(null, new ItemStack(IRON), 1, "smelt");
        handler.submitCraftingStatistics(null, new ItemStack(IRON), 1, "anvil");

        assertEquals(Arrays.asList("minecraft:iron_ingot", "minecraft:gold_ingot",
            "minecraft:iron_ingot", "minecraft:gold_ingot",
            "minecraft:iron_ingot", "minecraft:gold_ingot"), requestedNames(statisticFacts.requested));
        assertEquals(Arrays.asList("craft", "craft", "smelt", "smelt", "anvil", "anvil"),
            requestedChannels(statisticFacts.requested));
        assertEquals("each selected statistic target is persisted as one fact", 6, sink.facts.size());
        for (GameplayFact fact : sink.facts) {
            assertEquals("craft-statistics", fact.getAttributes().get("channel"));
            assertEquals("minecraft:iron_ingot", fact.getAttributes().get("trigger.registryName"));
            assertEquals("true", fact.getAttributes().get(
                com.jsirgalaxybase.quest.core.BqPartyCraftingStatisticsScope.PERSONAL_ONLINE_SNAPSHOT));
        }
    }

    @Test
    public void bqStatisticsSnapshotEachLocalOnlinePartyMemberPersonallyWithoutSumming() {
        QuestObservationPlan plan = QuestObservationPlan.fromPublished(Collections.singletonList(
            stored(craftingTask("metal", "minecraft:iron_ingot", "minecraft:gold_ingot"))));
        RecordingSink sink = new RecordingSink();
        RecordingStatisticFactProducer statisticFacts = new RecordingStatisticFactProducer();
        QuestGameplayEventHandler handler = new QuestGameplayEventHandler(sink,
            new MinecraftQuestFactFactory("crafting-party-test"),
            new QuestObservationPlanProvider() {
                @Override public QuestObservationPlan current() { return plan; }
            }, new com.jsirgalaxybase.quest.core.QuestClock() {
                @Override public long currentTimeMillis() { return 1L; }
            }, statisticFacts, new QuestGameplayEventHandler.CraftingStatisticMemberResolver() {
                @Override public List<UUID> localOnlineMembers(
                    net.minecraft.entity.player.EntityPlayer triggerPlayer) {
                    // Offline Party members are deliberately absent from this server-authored local online view.
                    return Arrays.asList(PLAYER_ID, PARTY_MEMBER_ID);
                }
            });

        handler.submitCraftingStatistics(null, new ItemStack(IRON), 1, "craft");

        assertEquals("two target counters are read separately for each of two online members", 4,
            sink.facts.size());
        assertEquals(Arrays.asList(PLAYER_ID, PLAYER_ID, PARTY_MEMBER_ID, PARTY_MEMBER_ID),
            statisticPlayerIds(statisticFacts.requested));
        assertEquals(Arrays.asList("5", "5", "2", "2"), statisticAmounts(sink.facts));
        for (int index = 0; index < sink.facts.size(); index++) {
            GameplayFact fact = sink.facts.get(index);
            assertEquals("statistics are personal player facts, not Party aggregate facts",
                fact.getPlayerId(), statisticFacts.requested.get(index).playerId);
            assertEquals("true", fact.getAttributes().get(
                com.jsirgalaxybase.quest.core.BqPartyCraftingStatisticsScope.PERSONAL_ONLINE_SNAPSHOT));
        }
    }

    @Test
    public void automaticBqDetectionRetriesFailedImportedTaskOnTheNextPeriodicPass() {
        TaskDefinition authoredTask = craftingTask("authored", "minecraft:gold_ingot");
        Map<String, String> authoredParameters = new LinkedHashMap<String, String>(authoredTask.getParameters());
        authoredParameters.remove("bq.raw");
        authoredTask = new TaskDefinition(authoredTask.getKey(), authoredTask.getTypeId(),
            authoredTask.isOptional(), authoredParameters);
        QuestObservationPlan plan = QuestObservationPlan.fromPublished(Arrays.asList(
            stored(craftingTask("imported-a", "minecraft:iron_ingot")),
            stored(authoredTask),
            stored(craftingTask("imported-b", "minecraft:diamond"))));
        List<String> attempted = new ArrayList<String>();
        final int[] transientFailures = {0};
        QuestGameplayEventHandler handler = new QuestGameplayEventHandler(new RecordingSink(),
            new MinecraftQuestFactFactory("automatic-crafting-test"),
            new QuestObservationPlanProvider() {
                @Override public QuestObservationPlan current() { return plan; }
            },
            new com.jsirgalaxybase.quest.core.QuestClock() {
                @Override public long currentTimeMillis() { return 100L; }
            },
            new RecordingStatisticFactProducer(),
            new QuestGameplayEventHandler.CraftingStatisticMemberResolver() {
                @Override public List<UUID> localOnlineMembers(
                    net.minecraft.entity.player.EntityPlayer triggerPlayer) {
                    return Collections.singletonList(PLAYER_ID);
                }
            },
            new QuestGameplayEventHandler.AutomaticCraftingStatisticsDetector() {
                @Override public void detect(UUID playerId,
                    com.jsirgalaxybase.quest.core.CraftingStatisticsDetectionTarget target, long now) {
                    attempted.add(target.getTaskKey());
                    if ("imported-a".equals(target.getTaskKey()) && transientFailures[0] == 0) {
                        transientFailures[0]++;
                        throw new IllegalStateException("simulated PostgreSQL outage");
                    }
                }
            });

        handler.detectAutomaticCraftingStatistics(PLAYER_ID, plan, 100L);
        handler.detectAutomaticCraftingStatistics(PLAYER_ID, plan, 4_000L);

        assertEquals("authored tasks stay excluded; a failed imported target cannot block siblings and retries",
            Arrays.asList("imported-a", "imported-b", "imported-a", "imported-b"), attempted);
        assertEquals("the simulated transient database failure happens only on the first pass", 1,
            transientFailures[0]);
    }

    private static List<String> requestedNames(List<RequestedStatistic> values) {
        List<String> names = new ArrayList<String>();
        for (RequestedStatistic value : values) names.add(value.registryName);
        return names;
    }

    private static List<String> statisticNames(List<com.jsirgalaxybase.quest.core.QuestCraftingStatisticObservation> values) {
        List<String> names = new ArrayList<String>();
        for (com.jsirgalaxybase.quest.core.QuestCraftingStatisticObservation value : values) {
            names.add(value.getRegistryName());
        }
        return names;
    }

    private static List<String> requestedChannels(List<RequestedStatistic> values) {
        List<String> channels = new ArrayList<String>();
        for (RequestedStatistic value : values) channels.add(value.triggerChannel);
        return channels;
    }

    private static List<UUID> statisticPlayerIds(List<RequestedStatistic> values) {
        List<UUID> ids = new ArrayList<UUID>();
        for (RequestedStatistic value : values) ids.add(value.playerId);
        return ids;
    }

    private static List<String> statisticAmounts(List<GameplayFact> values) {
        List<String> amounts = new ArrayList<String>();
        for (GameplayFact value : values) amounts.add(value.getAttributes().get("amount"));
        return amounts;
    }

    private static StoredQuestDefinition stored(TaskDefinition... tasks) {
        QuestDefinition definition = new QuestDefinition(UUID.randomUUID(), 1, "Crafting", "",
            QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(), Arrays.asList(tasks),
            Collections.<RewardDefinition>emptyList());
        return new StoredQuestDefinition(definition, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L);
    }

    private static TaskDefinition craftingTask(String key, String... registryNames) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("factType", "galaxy:item_produced");
        parameters.put("allowCraft", "1");
        parameters.put("allowSmelt", "1");
        parameters.put("allowAnvil", "1");
        parameters.put("allowCraftedFromStatistics", "1");
        parameters.put("bq.raw", "{}");
        parameters.put("item.count", Integer.toString(registryNames.length));
        for (int index = 0; index < registryNames.length; index++) {
            parameters.put("item." + index + ".registryName", registryNames[index]);
            parameters.put("item." + index + ".meta", "0");
        }
        return new TaskDefinition(key, "bq_standard:crafting", false, parameters);
    }

    private static final class RecordingStatisticFactProducer
        implements QuestGameplayEventHandler.CraftingStatisticFactProducer {
        private final List<RequestedStatistic> requested = new ArrayList<RequestedStatistic>();
        private int nextEvent;

        @Override
        public Map<String, String> outputAttributes(ItemStack trigger, int amount, String channel) {
            String registryName = trigger.getItem() == IRON ? "minecraft:iron_ingot"
                : trigger.getItem() == GOLD ? "minecraft:gold_ingot"
                : trigger.getItem() == DIAMOND ? "minecraft:diamond" : "minecraft:stick";
            Map<String, String> attributes = new LinkedHashMap<String, String>();
            attributes.put("registryName", registryName);
            attributes.put("meta", "0");
            attributes.put("damageable", "false");
            attributes.put("amount", Integer.toString(amount));
            attributes.put("channel", channel);
            attributes.put("oreDictionary", "");
            return attributes;
        }

        @Override
        public GameplayFact create(net.minecraft.entity.player.EntityPlayer triggerPlayer, UUID observedPlayerId,
            String registryName, int meta, ItemStack trigger, String triggerChannel,
            boolean bqPartyOnlineSnapshot) {
            requested.add(new RequestedStatistic(observedPlayerId, registryName, meta, triggerChannel));
            Map<String, String> attributes = new LinkedHashMap<String, String>();
            attributes.put("registryName", registryName);
            attributes.put("meta", Integer.toString(meta));
            attributes.put("channel", "craft-statistics");
            long count = PARTY_MEMBER_ID.equals(observedPlayerId) ? 2L : 5L;
            attributes.put("amount", Long.toString(count));
            attributes.put("trigger.registryName", "minecraft:iron_ingot");
            attributes.put("trigger.channel", triggerChannel);
            if (bqPartyOnlineSnapshot) attributes.put(
                com.jsirgalaxybase.quest.core.BqPartyCraftingStatisticsScope.PERSONAL_ONLINE_SNAPSHOT, "true");
            return new GameplayFact("crafting-test", "statistics-" + nextEvent++, observedPlayerId,
                "galaxy:item_produced", 1L, attributes);
        }
    }

    private static final class RequestedStatistic {
        private final UUID playerId;
        private final String registryName;
        private final int meta;
        private final String triggerChannel;

        private RequestedStatistic(UUID playerId, String registryName, int meta, String triggerChannel) {
            this.playerId = playerId;
            this.registryName = registryName;
            this.meta = meta;
            this.triggerChannel = triggerChannel;
        }
    }

    private static final class RecordingSink implements QuestFactSink {
        private final List<GameplayFact> facts = new ArrayList<GameplayFact>();

        @Override public void accept(GameplayFact fact) { facts.add(fact); }
        @Override public boolean beginInventorySession(GameplayFact fact) { return false; }
        @Override public void endInventorySession(UUID playerId, UUID sessionId) { }
        @Override public void reconcileLockedProgress(UUID playerId, long now) { }
    }
}
