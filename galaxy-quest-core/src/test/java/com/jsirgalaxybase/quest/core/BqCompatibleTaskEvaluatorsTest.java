package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class BqCompatibleTaskEvaluatorsTest {
    @Test
    public void standardRegistryExposesFirstImplementedBqTypes() {
        Map<String, TaskEvaluator> values = BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch().snapshot();
        assertEquals(13, values.size());
        assertTrue(values.containsKey("bq_standard:hunt"));
        assertTrue(values.containsKey("bq_standard:crafting"));
        assertTrue(values.containsKey("bq_standard:checkbox"));
        assertTrue(values.containsKey("bq_standard:block_break"));
        assertTrue(values.containsKey("bq_standard:retrieval"));
        assertTrue(values.containsKey("bq_standard:optional_retrieval"));
        assertTrue(values.containsKey("bq_standard:fluid"));
        assertTrue(values.containsKey("bq_standard:location"));
        assertTrue(values.containsKey("bq_standard:meeting"));
        assertTrue(values.containsKey("bq_standard:scoreboard"));
        assertTrue(values.containsKey("bq_standard:xp"));
        assertTrue(values.containsKey("bq_standard:interact_entity"));
        assertTrue(values.containsKey("bq_standard:interact_item"));
    }

    @Test
    public void huntSupportsSubtypeAliasesAndDamageFilter() {
        SubjectCountTaskEvaluator evaluator = new SubjectCountTaskEvaluator("bq_standard:hunt", "galaxy:entity_killed");
        Map<String, String> params = map("target", "3", "subject", "Zombie", "subtypes", "true", "damageType", "player");
        TaskDefinition task = new TaskDefinition("hunt", "bq_standard:hunt", false, params);
        GameplayFact fact = fact("galaxy:entity_killed", map("subject", "SpecialZombie", "subjectAliases", "Zombie|Mob",
            "damageType", "player", "amount", "1"));
        assertEquals(1L, evaluator.evaluate(task, TaskProgress.empty(task), fact).getValue());
        assertEquals(0L, evaluator.evaluate(task, TaskProgress.empty(task), fact("galaxy:entity_killed",
            map("subject", "SpecialZombie", "subjectAliases", "Zombie", "damageType", "lava"))).getValue());
    }

    @Test public void huntAppliesCanonicalPartialEntityNbtWhenRequired() {
        SubjectCountTaskEvaluator evaluator = new SubjectCountTaskEvaluator("bq_standard:hunt", "galaxy:entity_killed");
        Map<String, PortableNbt> required = new LinkedHashMap<String, PortableNbt>();
        required.put("CustomName", PortableNbt.string("Boss"));
        Map<String, PortableNbt> actual = new LinkedHashMap<String, PortableNbt>(required);
        actual.put("Health", PortableNbt.number("20"));
        TaskDefinition task = new TaskDefinition("hunt", "bq_standard:hunt", false,
            map("target", "1", "subject", "Zombie", "ignoreNBT", "false",
                "targetNBT.portable", PortableNbt.compound(required).encode()));
        assertTrue(evaluator.evaluate(task, TaskProgress.empty(task), fact("galaxy:entity_killed",
            map("subject", "Zombie", "amount", "1", "entity.nbtPortable",
                PortableNbt.compound(actual).encode()))).isComplete());
    }

    @Test
    public void craftingAdvancesEveryMatchingVectorEntryAndHonoursChannel() {
        ItemVectorTaskEvaluator evaluator = new ItemVectorTaskEvaluator("bq_standard:crafting", "galaxy:item_produced");
        Map<String, String> params = map("target", "7", "item.count", "2", "item.0.registryName", "minecraft:iron_ingot",
            "item.0.meta", "0", "item.0.amount", "3", "item.0.oreDictionary", "ingotIron",
            "item.1.registryName", "minecraft:iron_ingot", "item.1.meta", "32767", "item.1.amount", "4",
            "item.1.oreDictionary", "", "allowCraft", "true", "allowSmelt", "false", "ignoreNBT", "true");
        TaskDefinition task = new TaskDefinition("craft", "bq_standard:crafting", false, params);
        TaskProgress start = new TaskProgress("craft", Arrays.asList(0L, 0L), Arrays.asList(3L, 4L), false);
        GameplayFact craft = fact("galaxy:item_produced", map("registryName", "minecraft:iron_ingot", "meta", "0",
            "oreDictionary", "ingotIron", "channel", "craft", "amount", "2"));
        assertEquals(Arrays.asList(2L, 2L), evaluator.evaluate(task, start, craft).getValues());
        GameplayFact smelt = fact("galaxy:item_produced", map("registryName", "minecraft:iron_ingot", "meta", "0",
            "channel", "smelt", "amount", "2"));
        assertEquals(start.getValues(), evaluator.evaluate(task, start, smelt).getValues());
    }

    @Test
    public void craftingStatisticsUsesAbsoluteCountAndRequiresExplicitOption() {
        ItemVectorTaskEvaluator evaluator = new ItemVectorTaskEvaluator("bq_standard:crafting", "galaxy:item_produced");
        Map<String, String> params = map("target", "10", "item.count", "1", "item.0.registryName",
            "minecraft:iron_ingot", "item.0.meta", "0", "item.0.amount", "10",
            "allowCraftedFromStatistics", "true");
        TaskDefinition task = new TaskDefinition("craft", "bq_standard:crafting", false, params);
        GameplayFact statistics = fact("galaxy:item_produced", map("registryName", "minecraft:iron_ingot",
            "meta", "0", "amount", "7", "channel", "craft-statistics"));
        TaskProgress result = evaluator.evaluate(task, TaskProgress.empty(task), statistics);
        assertEquals(Arrays.asList(7L), result.getValues());
        GameplayFact lower = fact("galaxy:item_produced", map("registryName", "minecraft:iron_ingot",
            "meta", "0", "amount", "3", "channel", "craft-statistics"));
        assertEquals(Arrays.asList(7L), evaluator.evaluate(task, result, lower).getValues());
        params.put("allowCraftedFromStatistics", "false");
        TaskDefinition disabled = new TaskDefinition("craft", "bq_standard:crafting", false, params);
        assertEquals(Arrays.asList(0L), evaluator.evaluate(disabled, TaskProgress.empty(disabled), statistics).getValues());
    }

    @Test
    public void checkboxRequiresServerBoundTaskKey() {
        ConfirmationTaskEvaluator evaluator = new ConfirmationTaskEvaluator("bq_standard:checkbox", "galaxy:task_confirmed");
        TaskDefinition task = new TaskDefinition("check", "bq_standard:checkbox", false, map("target", "1"));
        assertTrue(evaluator.evaluate(task, TaskProgress.empty(task),
            fact("galaxy:task_confirmed", map("taskKey", "check"))).isComplete());
        assertFalse(evaluator.evaluate(task, TaskProgress.empty(task),
            fact("galaxy:task_confirmed", map("taskKey", "other"))).isComplete());
    }

    @Test
    public void blockBreakAdvancesOnlyFirstMatchingRequirement() {
        BlockVectorTaskEvaluator evaluator = new BlockVectorTaskEvaluator("bq_standard:block_break", "galaxy:block_broken");
        Map<String, String> params = map("progressMode", "block-vector", "target", "4", "block.count", "2",
            "block.0.registryName", "minecraft:stone", "block.0.meta", "0", "block.0.amount", "2",
            "block.0.oreDictionary", "stone", "block.0.nbt", "{}", "block.1.registryName", "minecraft:stone",
            "block.1.meta", "32767", "block.1.amount", "2", "block.1.oreDictionary", "", "block.1.nbt", "{}");
        TaskDefinition task = new TaskDefinition("break", "bq_standard:block_break", false, params);
        TaskProgress progress = TaskProgress.empty(task);
        TaskProgress next = evaluator.evaluate(task, progress, fact("galaxy:block_broken",
            map("registryName", "minecraft:stone", "meta", "0", "oreDictionary", "stone")));
        assertEquals(Arrays.asList(1L, 0L), next.getValues());
    }

    @Test
    public void retrievalUsesAbsoluteSnapshotAndDoesNotConsume() {
        InventorySnapshotTaskEvaluator evaluator = new InventorySnapshotTaskEvaluator("bq_standard:retrieval",
            "galaxy:inventory_observed");
        Map<String, String> params = map("progressMode", "item-vector", "target", "8", "item.count", "2",
            "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0", "item.0.amount", "5",
            "item.0.oreDictionary", "ingotIron", "item.1.registryName", "minecraft:gold_ingot", "item.1.meta", "0",
            "item.1.amount", "3", "item.1.oreDictionary", "ingotGold", "consume", "false", "ignoreNBT", "true");
        TaskDefinition task = new TaskDefinition("retrieve", "bq_standard:retrieval", false, params);
        GameplayFact snapshot = fact("galaxy:inventory_observed", map("inventory.count", "3",
            "inventory.0.registryName", "minecraft:iron_ingot", "inventory.0.meta", "0",
            "inventory.0.oreDictionary", "ingotIron", "inventory.0.amount", "2",
            "inventory.1.registryName", "minecraft:iron_ingot", "inventory.1.meta", "0",
            "inventory.1.oreDictionary", "ingotIron", "inventory.1.amount", "4",
            "inventory.2.registryName", "minecraft:gold_ingot", "inventory.2.meta", "0",
            "inventory.2.oreDictionary", "ingotGold", "inventory.2.amount", "1"));
        assertEquals(Arrays.asList(5L, 1L), evaluator.evaluate(task, TaskProgress.empty(task), snapshot).getValues());

        TaskProgress previous = new TaskProgress("retrieve", Arrays.asList(4L, 2L), Arrays.asList(5L, 3L), false);
        GameplayFact smaller = fact("galaxy:inventory_observed", map("inventory.count", "1",
            "inventory.0.registryName", "minecraft:iron_ingot", "inventory.0.meta", "0",
            "inventory.0.oreDictionary", "ingotIron", "inventory.0.amount", "1"));
        assertEquals(Arrays.asList(1L, 0L), evaluator.evaluate(task, previous, smaller).getValues());

        params.put("consume", "true");
        TaskDefinition consuming = new TaskDefinition("retrieve", "bq_standard:retrieval", false, params);
        assertEquals(Arrays.asList(0L, 0L), evaluator.evaluate(consuming, TaskProgress.empty(consuming), snapshot).getValues());
    }

    @Test
    public void retrievalDoesNotCountOneStackTwiceAcrossRequirements() {
        InventorySnapshotTaskEvaluator evaluator = new InventorySnapshotTaskEvaluator("bq_standard:retrieval",
            "galaxy:inventory_observed");
        Map<String, String> params = map("progressMode", "item-vector", "target", "8", "item.count", "2",
            "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0", "item.0.amount", "4",
            "item.0.oreDictionary", "ingotIron", "item.1.registryName", "minecraft:iron_ingot", "item.1.meta", "0",
            "item.1.amount", "4", "item.1.oreDictionary", "ingotIron", "consume", "false", "ignoreNBT", "true");
        TaskDefinition task = new TaskDefinition("retrieve", "bq_standard:retrieval", false, params);
        GameplayFact snapshot = fact("galaxy:inventory_observed", map("inventory.count", "1",
            "inventory.0.registryName", "minecraft:iron_ingot", "inventory.0.meta", "0",
            "inventory.0.oreDictionary", "ingotIron", "inventory.0.amount", "5"));
        assertEquals(Arrays.asList(4L, 1L), evaluator.evaluate(task, TaskProgress.empty(task), snapshot).getValues());
    }

    @Test
    public void fluidSnapshotAllocatesMillibucketsAndRejectsConsumeMode() {
        FluidSnapshotTaskEvaluator evaluator = new FluidSnapshotTaskEvaluator("bq_standard:fluid",
            "galaxy:fluid_inventory_observed");
        Map<String, String> params = map("progressMode", "fluid-vector", "target", "3000", "fluid.count", "2",
            "fluid.0.name", "water", "fluid.0.amount", "1000", "fluid.0.nbt", "{}",
            "fluid.1.name", "water", "fluid.1.amount", "2000", "fluid.1.nbt", "{}", "consume", "false",
            "ignoreNBT", "true");
        TaskDefinition task = new TaskDefinition("fluid", "bq_standard:fluid", false, params);
        GameplayFact snapshot = fact("galaxy:fluid_inventory_observed", map("fluidInventory.count", "1",
            "fluidInventory.0.name", "water", "fluidInventory.0.amount", "2500"));
        assertEquals(Arrays.asList(1000L, 1500L),
            evaluator.evaluate(task, TaskProgress.empty(task), snapshot).getValues());
        params.put("consume", "true");
        TaskDefinition consuming = new TaskDefinition("fluid", "bq_standard:fluid", false, params);
        assertEquals(Arrays.asList(0L, 0L),
            evaluator.evaluate(consuming, TaskProgress.empty(consuming), snapshot).getValues());
    }

    @Test
    public void locationSupportsDistanceBiomeInvertAndRequiresSpecialEvidence() {
        LocationTaskEvaluator evaluator = new LocationTaskEvaluator("bq_standard:location", "galaxy:location_observed");
        Map<String, String> params = map("target", "1", "dimension", "7", "posX", "10", "posY", "64",
            "posZ", "10", "range", "5", "biome", "3", "structure", "", "visible", "false",
            "invert", "false", "taxiCabDist", "false");
        TaskDefinition task = new TaskDefinition("visit", "bq_standard:location", false, params);
        assertTrue(evaluator.evaluate(task, TaskProgress.empty(task), fact("galaxy:location_observed",
            map("dimension", "7", "x", "13", "y", "64", "z", "14", "biome", "3"))).isComplete());

        params.put("taxiCabDist", "true");
        TaskDefinition taxi = new TaskDefinition("visit", "bq_standard:location", false, params);
        assertFalse(evaluator.evaluate(taxi, TaskProgress.empty(taxi), fact("galaxy:location_observed",
            map("dimension", "7", "x", "13", "y", "64", "z", "14", "biome", "3"))).isComplete());

        params.put("invert", "true");
        assertTrue(evaluator.evaluate(new TaskDefinition("visit", "bq_standard:location", false, params),
            TaskProgress.empty(taxi), fact("galaxy:location_observed",
                map("dimension", "0", "x", "0", "y", "64", "z", "0", "biome", "1"))).isComplete());

        params.put("structure", "Village");
        TaskDefinition special = new TaskDefinition("visit", "bq_standard:location", false, params);
        assertFalse(evaluator.evaluate(special, TaskProgress.empty(special), fact("galaxy:location_observed",
            map("dimension", "0", "x", "0", "y", "64", "z", "0", "biome", "1"))).isComplete());
    }

    @Test
    public void meetingRequiresCompleteRadiusAndCountsSubtypeAliases() {
        MeetingTaskEvaluator evaluator = new MeetingTaskEvaluator("bq_standard:meeting",
            "galaxy:nearby_entities_observed");
        Map<String, String> params = map("target", "1", "subject", "Villager", "range", "4", "amount", "2",
            "subtypes", "true", "ignoreNBT", "true");
        TaskDefinition task = new TaskDefinition("meet", "bq_standard:meeting", false, params);
        GameplayFact tooSmall = fact("galaxy:nearby_entities_observed", map("observedRadius", "3",
            "nearbyEntity.count", "2", "nearbyEntity.0.subject", "Villager", "nearbyEntity.1.subject", "Villager"));
        assertFalse(evaluator.evaluate(task, TaskProgress.empty(task), tooSmall).isComplete());

        GameplayFact complete = fact("galaxy:nearby_entities_observed", map("observedRadius", "4",
            "nearbyEntity.count", "2", "nearbyEntity.0.subject", "CustomVillager",
            "nearbyEntity.0.subjectAliases", "Villager|Creature", "nearbyEntity.1.subject", "Villager",
            "nearbyEntity.1.subjectAliases", ""));
        assertTrue(evaluator.evaluate(task, TaskProgress.empty(task), complete).isComplete());

        params.put("subtypes", "false");
        params.put("amount", "1");
        TaskDefinition exact = new TaskDefinition("meet", "bq_standard:meeting", false, params);
        GameplayFact subtypeOnly = fact("galaxy:nearby_entities_observed", map("observedRadius", "4",
            "nearbyEntity.count", "1", "nearbyEntity.0.subject", "CustomVillager",
            "nearbyEntity.0.subjectAliases", "Villager"));
        assertFalse(evaluator.evaluate(exact, TaskProgress.empty(exact), subtypeOnly).isComplete());
    }

    @Test
    public void committedConsumptionAdvancesOnlyBoundConsumingTaskAndSaturates() {
        ConsumptionAppliedTaskEvaluator evaluator = new ConsumptionAppliedTaskEvaluator(
            "bq_standard:retrieval", "item");
        Map<String, String> params = map("progressMode", "item-vector", "item.count", "2",
            "item.0.amount", "5", "item.1.amount", "3", "target", "8", "consume", "true");
        TaskDefinition task = new TaskDefinition("submit", "bq_standard:retrieval", false, params);
        GameplayFact applied = fact("galaxy:consumption_applied", map("taskKey", "submit", "kind", "item",
            "value.count", "2", "value.0", "4", "value.1", "9"));
        TaskProgress result = evaluator.evaluate(task, TaskProgress.empty(task), applied);
        assertEquals(Arrays.asList(4L, 3L), result.getValues());
        assertFalse(result.isComplete());
        result = evaluator.evaluate(task, result, fact("galaxy:consumption_applied",
            map("taskKey", "submit", "kind", "item", "value.count", "2", "value.0", "1", "value.1", "0")));
        assertTrue(result.isComplete());

        params.put("consume", "false");
        TaskDefinition observation = new TaskDefinition("submit", "bq_standard:retrieval", false, params);
        assertEquals(Arrays.asList(0L, 0L), evaluator.evaluate(observation, TaskProgress.empty(observation), applied)
            .getValues());
    }

    @Test public void scoreboardSupportsEveryBqComparisonAndLatchesCompletion() {
        ScoreboardTaskEvaluator evaluator = new ScoreboardTaskEvaluator("bq_standard:scoreboard",
            "galaxy:score_observed");
        for (String[] sample : new String[][] { { "EQUAL", "5", "5" }, { "LESS_THAN", "4", "5" },
            { "MORE_THAN", "6", "5" }, { "LESS_OR_EQUAL", "5", "5" },
            { "MORE_OR_EQUAL", "5", "5" }, { "NOT", "6", "5" } }) {
            TaskDefinition task = new TaskDefinition("score", "bq_standard:scoreboard", false,
                map("target", "1", "subject", "machine", "scoreTarget", sample[2], "operation", sample[0]));
            TaskProgress result = evaluator.evaluate(task, TaskProgress.empty(task),
                fact("galaxy:score_observed", map("subject", "machine", "value", sample[1])));
            assertTrue(sample[0], result.isComplete());
        }
        TaskDefinition negative = new TaskDefinition("score", "bq_standard:scoreboard", false,
            map("target", "1", "subject", "debt", "scoreTarget", "-2", "operation", "LESS_OR_EQUAL"));
        assertTrue(evaluator.evaluate(negative, TaskProgress.empty(negative),
            fact("galaxy:score_observed", map("subject", "debt", "value", "-3"))).isComplete());
    }

    @Test public void nonConsumingXpUsesAbsoluteSnapshotAndConsumingXpRejectsSnapshotShortcut() {
        XpSnapshotTaskEvaluator evaluator = new XpSnapshotTaskEvaluator("bq_standard:xp", "galaxy:xp_observed");
        TaskDefinition observed = new TaskDefinition("xp", "bq_standard:xp", false,
            map("target", "100", "consume", "false"));
        TaskProgress first = evaluator.evaluate(observed, TaskProgress.empty(observed),
            fact("galaxy:xp_observed", map("value", "80")));
        assertEquals(80L, first.getValue());
        TaskProgress decreased = evaluator.evaluate(observed, first,
            fact("galaxy:xp_observed", map("value", "40")));
        assertEquals(40L, decreased.getValue());
        TaskDefinition consuming = new TaskDefinition("xp", "bq_standard:xp", false,
            map("target", "100", "consume", "true"));
        assertEquals(0L, evaluator.evaluate(consuming, TaskProgress.empty(consuming),
            fact("galaxy:xp_observed", map("value", "1000"))).getValue());
    }

    @Test public void interactionEvaluatorHonorsActionSubtypeHeldItemAndBlock() {
        InteractionTaskEvaluator entity = new InteractionTaskEvaluator("bq_standard:interact_entity",
            "galaxy:entity_interacted", true);
        TaskDefinition entityTask = new TaskDefinition("talk", "bq_standard:interact_entity", false,
            map("target", "2", "subject", "Villager", "targetSubtypes", "true", "onInteract", "true",
                "onHit", "false", "ignoreTargetNBT", "true", "ignoreItemNBT", "true",
                "heldItem.registryName", "minecraft:emerald", "heldItem.meta", "0"));
        GameplayFact talk = fact("galaxy:entity_interacted", map("subject", "CustomVillager",
            "subjectAliases", "Villager|Creature", "hit", "false", "heldItem.registryName",
            "minecraft:emerald", "heldItem.meta", "0"));
        TaskProgress first = entity.evaluate(entityTask, TaskProgress.empty(entityTask), talk);
        assertEquals(1L, first.getValue());
        assertTrue(entity.evaluate(entityTask, first, talk).isComplete());
        assertEquals(0L, entity.evaluate(entityTask, TaskProgress.empty(entityTask),
            fact("galaxy:entity_interacted", map("subject", "Villager", "hit", "true",
                "heldItem.registryName", "minecraft:emerald", "heldItem.meta", "0"))).getValue());

        InteractionTaskEvaluator item = new InteractionTaskEvaluator("bq_standard:interact_item",
            "galaxy:item_interacted", false);
        TaskDefinition blockTask = new TaskDefinition("use", "bq_standard:interact_item", false,
            map("target", "1", "onInteract", "true", "block.registryName", "minecraft:chest",
                "block.meta", "32767", "ignoreNbt", "true"));
        assertTrue(item.evaluate(blockTask, TaskProgress.empty(blockTask), fact("galaxy:item_interacted",
            map("hit", "false", "block.registryName", "minecraft:chest", "block.meta", "3"))).isComplete());
    }

    @Test public void interactionUsesCanonicalPartialNbtAndRejectsDifferentValues() {
        InteractionTaskEvaluator entity = new InteractionTaskEvaluator("bq_standard:interact_entity",
            "galaxy:entity_interacted", true);
        Map<String, PortableNbt> requiredFields = new LinkedHashMap<String, PortableNbt>();
        requiredFields.put("Profession", PortableNbt.number("2"));
        Map<String, PortableNbt> actualFields = new LinkedHashMap<String, PortableNbt>(requiredFields);
        actualFields.put("Career", PortableNbt.number("1"));
        TaskDefinition task = new TaskDefinition("talk", "bq_standard:interact_entity", false,
            map("target", "1", "subject", "Villager", "onInteract", "true", "ignoreTargetNBT", "false",
                "targetNBT.portable", PortableNbt.compound(requiredFields).encode(), "ignoreItemNBT", "true"));
        assertTrue(entity.evaluate(task, TaskProgress.empty(task), fact("galaxy:entity_interacted",
            map("subject", "Villager", "hit", "false", "entity.nbtPortable",
                PortableNbt.compound(actualFields).encode()))).isComplete());
        actualFields.put("Profession", PortableNbt.number("3"));
        assertFalse(entity.evaluate(task, TaskProgress.empty(task), fact("galaxy:entity_interacted",
            map("subject", "Villager", "hit", "false", "entity.nbtPortable",
                PortableNbt.compound(actualFields).encode()))).isComplete());
    }

    private static GameplayFact fact(String type, Map<String, String> attributes) {
        return new GameplayFact("s2", UUID.randomUUID().toString(), UUID.randomUUID(), type, 1L, attributes);
    }

    private static Map<String, String> map(String... values) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int i = 0; i < values.length; i += 2) result.put(values[i], values[i + 1]);
        return result;
    }
}
