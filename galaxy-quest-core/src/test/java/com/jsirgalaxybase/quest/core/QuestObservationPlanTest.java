package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class QuestObservationPlanTest {
    @Test
    public void aggregatesPublishedPeriodicFactsAndLargestMeetingRadius() {
        QuestObservationPlan plan = QuestObservationPlan.fromPublished(Arrays.asList(
            stored(QuestDefinitionLifecycle.PUBLISHED, task("galaxy:inventory_observed")),
            stored(QuestDefinitionLifecycle.PUBLISHED, task("galaxy:fluid_inventory_observed")),
            stored(QuestDefinitionLifecycle.PUBLISHED, task("galaxy:location_observed")),
            stored(QuestDefinitionLifecycle.PUBLISHED, task("galaxy:xp_observed", "consume", "false")),
            stored(QuestDefinitionLifecycle.PUBLISHED, task("galaxy:score_observed", "subject", "machine",
                "scoreDisp", "Machine Score", "type", "dummy")),
            stored(QuestDefinitionLifecycle.PUBLISHED, task("galaxy:nearby_entities_observed", "range", "4")),
            stored(QuestDefinitionLifecycle.PUBLISHED, task("galaxy:nearby_entities_observed", "range", "12")),
            stored(QuestDefinitionLifecycle.RETIRED, task("galaxy:nearby_entities_observed", "range", "99"))));
        assertTrue(plan.observesInventory());
        assertTrue(plan.observesFluidInventory());
        assertTrue(plan.observesLocation());
        assertTrue(plan.observesMeeting());
        assertEquals(12, plan.getMeetingRadius());
        assertTrue(plan.observesXp());
        assertEquals(1, plan.getScores().size());
        assertEquals("machine", plan.getScores().get(0).getName());
        assertEquals("Machine Score", plan.getScores().get(0).getDisplayName());
    }

    @Test
    public void collectsOnlyCraftingItemsEnabledForStatistics() {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("factType", "galaxy:item_produced");
        parameters.put("allowCraftedFromStatistics", "true");
        parameters.put("item.count", "2");
        parameters.put("item.0.registryName", "minecraft:iron_ingot");
        parameters.put("item.0.meta", "0");
        parameters.put("item.1.registryName", "minecraft:gold_ingot");
        parameters.put("item.1.meta", "0");
        QuestDefinition definition = new QuestDefinition(UUID.randomUUID(), 1, "Craft", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(new TaskDefinition(
                "craft", "bq_standard:crafting", false, parameters)), Collections.<RewardDefinition>emptyList());
        QuestObservationPlan plan = QuestObservationPlan.fromPublished(Collections.singletonList(
            new StoredQuestDefinition(definition, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L)));
        assertTrue(plan.observesCraftingStatistics());
        assertEquals(2, plan.getCraftingStatistics().size());
        assertEquals("minecraft:iron_ingot", plan.getCraftingStatistics().get(0).getRegistryName());
    }

    @Test
    public void exposesStableAutomaticTargetsOnlyForPublishedImportedBqTasks() {
        UUID importedQuestId = UUID.randomUUID();
        Map<String, String> importedParameters = new LinkedHashMap<String, String>();
        importedParameters.put("factType", "galaxy:item_produced");
        importedParameters.put("allowCraftedFromStatistics", "true");
        importedParameters.put("bq.raw", "{}");
        importedParameters.put("item.count", "1");
        importedParameters.put("item.0.registryName", "minecraft:iron_ingot");
        TaskDefinition importedTask = new TaskDefinition("crafting-1", "bq_standard:crafting", false,
            importedParameters);
        QuestDefinition imported = new QuestDefinition(importedQuestId, 3, "Imported", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(importedTask),
            Collections.<RewardDefinition>emptyList());
        Map<String, String> authoredParameters = new LinkedHashMap<String, String>(importedParameters);
        authoredParameters.remove("bq.raw");
        TaskDefinition authoredTask = new TaskDefinition("custom", "bq_standard:crafting", false,
            authoredParameters);
        QuestDefinition authored = new QuestDefinition(UUID.randomUUID(), 1, "Authored", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(authoredTask),
            Collections.<RewardDefinition>emptyList());

        QuestObservationPlan plan = QuestObservationPlan.fromPublished(Arrays.asList(
            new StoredQuestDefinition(imported, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L),
            new StoredQuestDefinition(authored, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L),
            new StoredQuestDefinition(imported, QuestDefinitionLifecycle.RETIRED, "hash", 1L)));

        assertEquals(1, plan.getAutomaticCraftingStatisticsTargets().size());
        CraftingStatisticsDetectionTarget target = plan.getAutomaticCraftingStatisticsTargets().get(0);
        assertEquals(importedQuestId, target.getQuestId());
        assertEquals(3, target.getQuestVersion());
        assertEquals("crafting-1", target.getTaskKey());
    }

    @Test
    public void selectsStatisticsOnlyForTasksMatchedByTheCraftingOutput() {
        TaskDefinition ironAndGold = craftingTask("craft", "allowCraftedFromStatistics", "true",
            "allowSmelt", "false",
            "item.count", "2", "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0",
            "item.1.registryName", "minecraft:gold_ingot", "item.1.meta", "0");
        TaskDefinition copper = craftingTask("other-craft", "allowCraftedFromStatistics", "true",
            "item.count", "1", "item.0.registryName", "minecraft:copper_ingot", "item.0.meta", "0");
        QuestObservationPlan plan = QuestObservationPlan.fromPublished(Arrays.asList(
            stored(QuestDefinitionLifecycle.PUBLISHED, ironAndGold),
            stored(QuestDefinitionLifecycle.PUBLISHED, copper)));

        assertEquals(Arrays.asList("minecraft:iron_ingot", "minecraft:gold_ingot"),
            statisticNames(plan.craftingStatisticsForOutput(produced("minecraft:iron_ingot", "craft"))));
        assertTrue(plan.craftingStatisticsForOutput(produced("minecraft:diamond", "craft")).isEmpty());
        assertTrue(plan.craftingStatisticsForOutput(produced("minecraft:iron_ingot", "smelt")).isEmpty());
    }

    @Test
    public void separatesImportedBqPartyStatisticsFromOtherPersonalStatistics() {
        TaskDefinition imported = craftingTask("bq", "allowCraftedFromStatistics", "true",
            "item.count", "1", "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0");
        Map<String, String> importedParameters = new LinkedHashMap<String, String>(imported.getParameters());
        importedParameters.put("bq.raw", "{}");
        imported = new TaskDefinition(imported.getKey(), imported.getTypeId(), imported.isOptional(),
            importedParameters);
        TaskDefinition authored = craftingTask("authored", "allowCraftedFromStatistics", "true",
            "item.count", "1", "item.0.registryName", "minecraft:gold_ingot", "item.0.meta", "0");
        QuestObservationPlan plan = QuestObservationPlan.fromPublished(Arrays.asList(
            stored(QuestDefinitionLifecycle.PUBLISHED, imported),
            stored(QuestDefinitionLifecycle.PUBLISHED, authored)));
        Map<String, String> output = produced("minecraft:iron_ingot", "craft");

        assertEquals(Collections.singletonList("minecraft:iron_ingot"),
            statisticNames(plan.bqPartyCraftingStatisticsForOutput(output)));
        assertTrue(plan.otherCraftingStatisticsForOutput(output).isEmpty());
        assertEquals("non-imported tasks keep their personal-only sample path",
            Collections.singletonList("minecraft:gold_ingot"), statisticNames(
                plan.otherCraftingStatisticsForOutput(produced("minecraft:gold_ingot", "craft"))));
    }

    @Test
    public void interpretsNumericBqFlagsWhenBuildingTheObservationPlan() {
        QuestObservationPlan plan = QuestObservationPlan.fromPublished(Arrays.asList(
            stored(QuestDefinitionLifecycle.PUBLISHED, task("galaxy:inventory_observed", "consume", "1")),
            stored(QuestDefinitionLifecycle.PUBLISHED, task("galaxy:fluid_inventory_observed", "consume", "0")),
            stored(QuestDefinitionLifecycle.PUBLISHED, task("galaxy:xp_observed", "consume", "0")),
            stored(QuestDefinitionLifecycle.PUBLISHED, craftingTask())));
        assertFalse(plan.observesInventory());
        assertTrue(plan.observesFluidInventory());
        assertTrue(plan.observesXp());
        assertTrue(plan.observesCraftingStatistics());
    }

    @Test
    public void enablesPartyInventoryLeasesForEveryImportedNonConsumingBqRetrieval() {
        QuestObservationPlan imported = QuestObservationPlan.fromPublished(Arrays.asList(
            stored(QuestDefinitionLifecycle.PUBLISHED, bqRetrieval("bq_standard:retrieval", false, true)),
            stored(QuestDefinitionLifecycle.PUBLISHED, bqRetrieval("bq_standard:optional_retrieval", false, true))));
        assertTrue(imported.observesInventory());
        assertTrue(imported.observesBqPartyInventory());

        QuestObservationPlan consuming = QuestObservationPlan.fromPublished(Collections.singletonList(
            stored(QuestDefinitionLifecycle.PUBLISHED, bqRetrieval("bq_standard:retrieval", true, true))));
        assertFalse(consuming.observesInventory());
        assertFalse(consuming.observesBqPartyInventory());

        QuestObservationPlan authored = QuestObservationPlan.fromPublished(Collections.singletonList(
            stored(QuestDefinitionLifecycle.PUBLISHED, bqRetrieval("bq_standard:retrieval", false, false))));
        assertTrue(authored.observesInventory());
        assertFalse(authored.observesBqPartyInventory());

        QuestObservationPlan nonGroup = QuestObservationPlan.fromPublished(Collections.singletonList(
            stored(QuestDefinitionLifecycle.PUBLISHED, bqRetrieval("bq_standard:retrieval", false, true, false))));
        assertTrue(nonGroup.observesInventory());
        assertTrue(nonGroup.observesBqPartyInventory());
    }

    @Test
    public void enablesPartyInventoryLeasesForImportedNonConsumingBqFluid() {
        QuestObservationPlan imported = QuestObservationPlan.fromPublished(Collections.singletonList(
            stored(QuestDefinitionLifecycle.PUBLISHED, bqFluid(false, true))));
        assertFalse(imported.observesInventory());
        assertTrue(imported.observesFluidInventory());
        assertTrue(imported.observesBqPartyInventory());

        QuestObservationPlan consuming = QuestObservationPlan.fromPublished(Collections.singletonList(
            stored(QuestDefinitionLifecycle.PUBLISHED, bqFluid(true, true))));
        assertFalse(consuming.observesFluidInventory());
        assertFalse(consuming.observesBqPartyInventory());

        QuestObservationPlan authored = QuestObservationPlan.fromPublished(Collections.singletonList(
            stored(QuestDefinitionLifecycle.PUBLISHED, bqFluid(false, false))));
        assertTrue(authored.observesFluidInventory());
        assertFalse(authored.observesBqPartyInventory());
    }

    @Test
    public void enablesLockedProgressReconciliationOnlyForPublishedPlayerQuests() {
        QuestDefinition player = lockedProgressQuest(QuestParticipantScope.PLAYER);
        QuestDefinition party = lockedProgressQuest(QuestParticipantScope.PARTY);
        QuestObservationPlan plan = QuestObservationPlan.fromPublished(Arrays.asList(
            new StoredQuestDefinition(player, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L),
            new StoredQuestDefinition(party, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L),
            new StoredQuestDefinition(lockedProgressQuest(QuestParticipantScope.PLAYER),
                QuestDefinitionLifecycle.RETIRED, "hash", 1L)));
        assertTrue(plan.observesPlayerLockedProgress());

        QuestObservationPlan partyOnly = QuestObservationPlan.fromPublished(Collections.singletonList(
            new StoredQuestDefinition(party, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L)));
        assertFalse(partyOnly.observesPlayerLockedProgress());
        assertFalse(QuestObservationPlan.empty().observesPlayerLockedProgress());
    }

    private static QuestDefinition lockedProgressQuest(QuestParticipantScope scope) {
        return new QuestDefinition(UUID.randomUUID(), 1, "Locked", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.singletonList(task("galaxy:task_confirmed")),
            Collections.<RewardDefinition>emptyList(), RepeatPolicy.never(), QuestBehavior.builder()
                .progressWhileLocked(true).participantScope(scope).build());
    }

    private static StoredQuestDefinition stored(QuestDefinitionLifecycle lifecycle, TaskDefinition task) {
        QuestDefinition definition = new QuestDefinition(UUID.randomUUID(), 1, "Q", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task),
            Collections.<RewardDefinition>emptyList());
        return new StoredQuestDefinition(definition, lifecycle, "hash", 1L);
    }

    private static TaskDefinition craftingTask() {
        return craftingTask("craft", "allowCraftedFromStatistics", "1", "item.count", "1",
            "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0");
    }

    private static TaskDefinition craftingTask(String key, String... values) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("factType", "galaxy:item_produced");
        for (int index = 0; index < values.length; index += 2) {
            parameters.put(values[index], values[index + 1]);
        }
        return new TaskDefinition(key, "bq_standard:crafting", false, parameters);
    }

    private static Map<String, String> produced(String registryName, String channel) {
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("registryName", registryName);
        attributes.put("meta", "0");
        attributes.put("damageable", "false");
        attributes.put("amount", "1");
        attributes.put("channel", channel);
        attributes.put("oreDictionary", "");
        return attributes;
    }

    private static java.util.List<String> statisticNames(
        java.util.List<QuestCraftingStatisticObservation> observations) {
        java.util.List<String> names = new java.util.ArrayList<String>();
        for (QuestCraftingStatisticObservation observation : observations) names.add(observation.getRegistryName());
        return names;
    }

    private static TaskDefinition task(String factType, String... values) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("factType", factType);
        parameters.put("target", "1");
        for (int i = 0; i < values.length; i += 2) parameters.put(values[i], values[i + 1]);
        return new TaskDefinition("task", "type", false, parameters);
    }

    private static TaskDefinition bqRetrieval(String typeId, boolean consume, boolean imported) {
        return bqRetrieval(typeId, consume, imported, true);
    }

    private static TaskDefinition bqRetrieval(String typeId, boolean consume, boolean imported, boolean groupDetect) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("factType", "galaxy:inventory_observed");
        parameters.put("consume", Boolean.toString(consume));
        parameters.put("groupDetect", Boolean.toString(groupDetect));
        parameters.put("target", "1");
        if (imported) parameters.put("bq.raw", "{}");
        return new TaskDefinition("retrieval", typeId, false, parameters);
    }

    private static TaskDefinition bqFluid(boolean consume, boolean imported) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("factType", "galaxy:fluid_inventory_observed");
        parameters.put("consume", Boolean.toString(consume));
        parameters.put("groupDetect", "false");
        parameters.put("target", "1000");
        if (imported) parameters.put("bq.raw", "{}");
        return new TaskDefinition("fluid", "bq_standard:fluid", false, parameters);
    }
}
