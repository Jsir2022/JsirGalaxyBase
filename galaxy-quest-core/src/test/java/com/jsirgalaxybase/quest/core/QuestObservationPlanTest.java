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

    private static StoredQuestDefinition stored(QuestDefinitionLifecycle lifecycle, TaskDefinition task) {
        QuestDefinition definition = new QuestDefinition(UUID.randomUUID(), 1, "Q", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task),
            Collections.<RewardDefinition>emptyList());
        return new StoredQuestDefinition(definition, lifecycle, "hash", 1L);
    }

    private static TaskDefinition task(String factType, String... values) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("factType", factType);
        parameters.put("target", "1");
        for (int i = 0; i < values.length; i += 2) parameters.put(values[i], values[i + 1]);
        return new TaskDefinition("task", "type", false, parameters);
    }
}
