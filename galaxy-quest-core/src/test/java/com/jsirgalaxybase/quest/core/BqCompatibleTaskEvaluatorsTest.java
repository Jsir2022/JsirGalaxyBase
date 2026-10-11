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

    @Test
    public void huntCapsNewEventProgressAtRequiredValueLikeBq() {
        SubjectCountTaskEvaluator evaluator = new SubjectCountTaskEvaluator("bq_standard:hunt", "galaxy:entity_killed");
        TaskDefinition task = new TaskDefinition("hunt", "bq_standard:hunt", false,
            map("target", "2", "subject", "Zombie", "subtypes", "true"));
        TaskProgress observed = evaluator.evaluate(task, TaskProgress.empty(task), fact("galaxy:entity_killed",
            map("subject", "Zombie", "amount", "7")));

        assertEquals("BQ TaskHunt clamps progress to the configured requirement", 2L, observed.getValue());
        assertTrue(observed.isComplete());

        TaskProgress historical = new TaskProgress("hunt", 5L, 2L, false);
        assertEquals("an already-persisted overshoot is not rewritten downward", 5L,
            evaluator.evaluate(task, historical, fact("galaxy:entity_killed", map("subject", "Zombie",
                "amount", "1"))).getValue());
    }

    @Test
    public void huntRejectsMissingTargetAndUnregisteredEntitySubject() {
        SubjectCountTaskEvaluator evaluator = new SubjectCountTaskEvaluator("bq_standard:hunt", "galaxy:entity_killed");
        GameplayFact registeredZombie = fact("galaxy:entity_killed", map("subject", "Zombie",
            "subjectAliases", "Zombie", "damageType", "player", "amount", "1"));
        TaskDefinition missingTarget = new TaskDefinition("hunt", "bq_standard:hunt", false,
            map("target", "1", "subtypes", "true"));
        assertEquals("BQ cannot resolve a missing target through EntityList", 0L,
            evaluator.evaluate(missingTarget, TaskProgress.empty(missingTarget), registeredZombie).getValue());

        TaskDefinition zombieHunt = new TaskDefinition("hunt", "bq_standard:hunt", false,
            map("target", "1", "subject", "Zombie", "subtypes", "true"));
        GameplayFact unregisteredEntity = fact("galaxy:entity_killed", map("subject", "",
            "subjectAliases", "Zombie|Mob", "damageType", "player", "amount", "1"));
        assertEquals("BQ rejects an entity without an EntityList name even if its class has a registered parent",
            0L, evaluator.evaluate(zombieHunt, TaskProgress.empty(zombieHunt), unregisteredEntity).getValue());
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
    public void craftingMatchesAnyDamageValueForDamageableItemsLikeBq() {
        ItemVectorTaskEvaluator evaluator = new ItemVectorTaskEvaluator("bq_standard:crafting", "galaxy:item_produced");
        TaskDefinition task = new TaskDefinition("craft", "bq_standard:crafting", false,
            map("target", "1", "item.count", "1", "item.0.registryName", "gtnh:durable_tool",
                "item.0.meta", "0", "item.0.amount", "1", "ignoreNBT", "true"));
        TaskProgress start = TaskProgress.empty(task);

        TaskProgress damagedOutput = evaluator.evaluate(task, start, fact("galaxy:item_produced",
            map("registryName", "gtnh:durable_tool", "meta", "31", "damageable", "true",
                "channel", "craft", "amount", "1")));
        assertEquals(Arrays.asList(1L), damagedOutput.getValues());

        TaskProgress wrongVariant = evaluator.evaluate(task, start, fact("galaxy:item_produced",
            map("registryName", "gtnh:durable_tool", "meta", "31", "damageable", "false",
                "channel", "craft", "amount", "1")));
        assertEquals(Arrays.asList(0L), wrongVariant.getValues());
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
    public void craftingUsesPerSourceMaximumThenSumsAcrossServers() {
        ItemVectorTaskEvaluator evaluator = new ItemVectorTaskEvaluator("bq_standard:crafting",
            "galaxy:item_produced");
        TaskDefinition task = new TaskDefinition("craft", "bq_standard:crafting", false,
            map("progressMode", "item-vector", "item.count", "1", "item.0.registryName",
                "minecraft:iron_ingot", "item.0.meta", "0", "item.0.amount", "20",
                "allowCraft", "true", "allowCraftedFromStatistics", "true"));
        UUID player = UUID.randomUUID();
        TaskProgress progress = TaskProgress.empty(task);

        progress = evaluator.evaluate(task, progress, sourceFact("alpha", player, "alpha-craft",
            map("registryName", "minecraft:iron_ingot", "meta", "0", "amount", "2", "channel", "craft")));
        progress = evaluator.evaluate(task, progress, sourceFact("alpha", player, "alpha-statistics",
            map("registryName", "minecraft:iron_ingot", "meta", "0", "amount", "3",
                "channel", "craft-statistics")));
        assertEquals("event and absolute statistics from one server must not count the same output twice",
            Arrays.asList(3L), progress.getValues());

        progress = evaluator.evaluate(task, progress, sourceFact("beta", player, "beta-statistics",
            map("registryName", "minecraft:iron_ingot", "meta", "0", "amount", "4",
                "channel", "craft-statistics")));
        assertEquals("each server's local maximum contributes to the player's total",
            Arrays.asList(7L), progress.getValues());

        progress = evaluator.evaluate(task, progress, sourceFact("alpha", player, "alpha-stale-statistics",
            map("registryName", "minecraft:iron_ingot", "meta", "0", "amount", "1",
                "channel", "craft-statistics")));
        assertEquals("a lower snapshot cannot reduce that source's lifetime count", Arrays.asList(7L),
            progress.getValues());

        progress = evaluator.evaluate(task, progress, sourceFact("beta", player, "beta-craft-1",
            map("registryName", "minecraft:iron_ingot", "meta", "0", "amount", "2", "channel", "craft")));
        assertEquals("source events and source statistics use max while different sources add",
            Arrays.asList(7L), progress.getValues());
        progress = evaluator.evaluate(task, progress, sourceFact("beta", player, "beta-craft-2",
            map("registryName", "minecraft:iron_ingot", "meta", "0", "amount", "3", "channel", "craft")));
        assertEquals(Arrays.asList(8L), progress.getValues());
    }

    @Test
    public void craftingStatisticsOnlyRefreshRequirementsBelowTargetOnThatSource() {
        ItemVectorTaskEvaluator evaluator = new ItemVectorTaskEvaluator("bq_standard:crafting",
            "galaxy:item_produced");
        TaskDefinition task = new TaskDefinition("craft", "bq_standard:crafting", false,
            map("progressMode", "item-vector", "item.count", "2",
                "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0", "item.0.amount", "2",
                "item.1.registryName", "minecraft:gold_ingot", "item.1.meta", "0", "item.1.amount", "4",
                "allowCraft", "true", "allowCraftedFromStatistics", "true"));
        UUID player = UUID.randomUUID();
        TaskProgress progress = evaluator.evaluate(task, TaskProgress.empty(task), sourceFact("alpha", player,
            "alpha-iron-craft", map("registryName", "minecraft:iron_ingot", "meta", "0", "amount", "2",
                "channel", "craft")));

        progress = evaluator.evaluate(task, progress, sourceFact("alpha", player, "alpha-iron-statistics",
            map("registryName", "minecraft:iron_ingot", "meta", "0", "amount", "9",
                "channel", "craft-statistics")));
        assertEquals("BQ skips the absolute statistic for a requirement already completed by this source's event",
            Arrays.asList(2L, 0L), progress.getValues());
        assertEquals(Arrays.asList(0L, 0L), progress.getSourceContributions().get("alpha").getObservedValues());

        progress = evaluator.evaluate(task, progress, sourceFact("beta", player, "beta-iron-statistics",
            map("registryName", "minecraft:iron_ingot", "meta", "0", "amount", "6",
                "channel", "craft-statistics")));
        assertEquals("another source's under-target requirement is still independently observed",
            Arrays.asList(8L, 0L), progress.getValues());

        progress = evaluator.evaluate(task, progress, sourceFact("alpha", player, "alpha-gold-statistics",
            map("registryName", "minecraft:gold_ingot", "meta", "0", "amount", "7",
                "channel", "craft-statistics")));
        assertEquals("unfinished components retain BQ's unbounded absolute statistic value",
            Arrays.asList(8L, 7L), progress.getValues());
        assertTrue(progress.isComplete());
    }

    @Test
    public void craftingStatisticsUsesBqItemIdCounterWithoutMetaOrNbtFiltering() {
        ItemVectorTaskEvaluator evaluator = new ItemVectorTaskEvaluator("bq_standard:crafting", "galaxy:item_produced");
        Map<String, PortableNbt> requiredNbt = new LinkedHashMap<String, PortableNbt>();
        requiredNbt.put("display", PortableNbt.string("Named Tool"));
        TaskDefinition task = new TaskDefinition("craft", "bq_standard:crafting", false,
            map("target", "2", "item.count", "1", "item.0.registryName", "gtnh:durable_tool",
                "item.0.meta", "0", "item.0.amount", "2", "item.0.nbtPortable",
                PortableNbt.compound(requiredNbt).encode(), "ignoreNBT", "false",
                "allowCraftedFromStatistics", "true"));
        GameplayFact statistics = fact("galaxy:item_produced", map("registryName", "gtnh:durable_tool",
            "meta", "99", "amount", "2", "channel", "craft-statistics"));

        assertTrue(evaluator.evaluate(task, TaskProgress.empty(task), statistics).isComplete());
    }

    @Test
    public void craftingStatisticsAutomaticRefreshRequiresMatchingEnabledOutputEvent() {
        ItemVectorTaskEvaluator evaluator = new ItemVectorTaskEvaluator("bq_standard:crafting", "galaxy:item_produced");
        TaskDefinition task = new TaskDefinition("craft", "bq_standard:crafting", false,
            map("target", "2", "item.count", "1", "item.0.registryName", "minecraft:iron_ingot",
                "item.0.meta", "0", "item.0.amount", "2", "allowCraft", "true", "allowSmelt", "false",
                "allowAnvil", "false", "allowCraftedFromStatistics", "true"));
        TaskProgress start = TaskProgress.empty(task);
        Map<String, String> unrelated = map("registryName", "minecraft:iron_ingot", "meta", "0", "amount", "2",
            "channel", "craft-statistics", "trigger.channel", "craft",
            "trigger.registryName", "minecraft:gold_ingot", "trigger.meta", "0");
        assertEquals(Arrays.asList(0L), evaluator.evaluate(task, start,
            fact("galaxy:item_produced", unrelated)).getValues());

        Map<String, String> matchingSmelt = map("registryName", "minecraft:iron_ingot", "meta", "0", "amount", "2",
            "channel", "craft-statistics", "trigger.channel", "smelt",
            "trigger.registryName", "minecraft:iron_ingot", "trigger.meta", "0");
        assertEquals(Arrays.asList(0L), evaluator.evaluate(task, start,
            fact("galaxy:item_produced", matchingSmelt)).getValues());

        Map<String, String> matchingCraft = map("registryName", "minecraft:iron_ingot", "meta", "0", "amount", "2",
            "channel", "craft-statistics", "trigger.channel", "craft",
            "trigger.registryName", "minecraft:iron_ingot", "trigger.meta", "0");
        assertTrue(evaluator.evaluate(task, start, fact("galaxy:item_produced", matchingCraft)).isComplete());
    }

    @Test
    public void craftingStatisticsFromOneTaskDoNotRefreshAnotherTaskWithoutMatchingTrigger() {
        ItemVectorTaskEvaluator evaluator = new ItemVectorTaskEvaluator("bq_standard:crafting",
            "galaxy:item_produced");
        TaskDefinition triggeredTask = new TaskDefinition("triggered", "bq_standard:crafting", false,
            map("progressMode", "item-vector", "target", "8", "item.count", "2",
                "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0", "item.0.amount", "4",
                "item.1.registryName", "minecraft:gold_ingot", "item.1.meta", "0", "item.1.amount", "4",
                "allowCraft", "true", "allowCraftedFromStatistics", "true"));
        TaskDefinition otherTask = new TaskDefinition("other", "bq_standard:crafting", false,
            map("progressMode", "item-vector", "target", "4", "item.count", "1",
                "item.0.registryName", "minecraft:gold_ingot", "item.0.meta", "0", "item.0.amount", "4",
                "allowCraft", "true", "allowCraftedFromStatistics", "true"));
        GameplayFact goldStatisticsTriggeredByIron = fact("galaxy:item_produced", map(
            "registryName", "minecraft:gold_ingot", "meta", "0", "amount", "3",
            "channel", "craft-statistics", "trigger.channel", "craft",
            "trigger.registryName", "minecraft:iron_ingot", "trigger.meta", "0"));

        assertEquals("the Task whose iron output triggered BQ Detect refreshes its gold requirement",
            Arrays.asList(0L, 3L), evaluator.evaluate(triggeredTask, TaskProgress.empty(triggeredTask),
                goldStatisticsTriggeredByIron).getValues());
        assertEquals("another Task requiring only gold must not receive the iron-triggered Detect",
            Arrays.asList(0L), evaluator.evaluate(otherTask, TaskProgress.empty(otherTask),
                goldStatisticsTriggeredByIron).getValues());
    }

    @Test
    public void craftingStatisticsRefreshOtherRequirementsAfterMatchingOutputInSameTask() {
        ItemVectorTaskEvaluator evaluator = new ItemVectorTaskEvaluator("bq_standard:crafting",
            "galaxy:item_produced");
        TaskDefinition task = new TaskDefinition("craft", "bq_standard:crafting", false,
            map("progressMode", "item-vector", "target", "8", "item.count", "2",
                "item.0.registryName", "minecraft:iron_ingot",
                "item.0.meta", "0", "item.0.amount", "4", "item.1.registryName", "minecraft:gold_ingot",
                "item.1.meta", "0", "item.1.amount", "4", "allowCraft", "true", "allowSmelt", "true",
                "allowAnvil", "true", "allowCraftedFromStatistics", "true"));

        for (String triggerChannel : Arrays.asList("craft", "smelt", "anvil")) {
            GameplayFact goldStatistics = fact("galaxy:item_produced", map("registryName", "minecraft:gold_ingot",
                "meta", "0", "amount", "3", "channel", "craft-statistics", "trigger.channel",
                triggerChannel, "trigger.registryName", "minecraft:iron_ingot", "trigger.meta", "0"));
            TaskProgress refreshed = evaluator.evaluate(task, TaskProgress.empty(task), goldStatistics);
            assertEquals(triggerChannel, Arrays.asList(0L, 3L), refreshed.getValues());
        }
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
    public void blockBreakUsesNegativeMetadataAsDirectWildcardButNotOreWildcardSentinel() {
        BlockVectorTaskEvaluator evaluator = new BlockVectorTaskEvaluator("bq_standard:block_break",
            "galaxy:block_broken");
        TaskDefinition oreWildcard = new TaskDefinition("break", "bq_standard:block_break", false,
            map("target", "1", "block.count", "1", "block.0.registryName", "minecraft:stone",
                "block.0.meta", "32767", "block.0.amount", "1", "block.0.oreDictionary", ""));
        GameplayFact sameBlockDifferentMeta = fact("galaxy:block_broken",
            map("registryName", "minecraft:stone", "meta", "3", "oreDictionary", ""));

        assertFalse(evaluator.evaluate(oreWildcard, TaskProgress.empty(oreWildcard), sameBlockDifferentMeta)
            .isComplete());

        TaskDefinition legacyWildcard = new TaskDefinition("break", "bq_standard:block_break", false,
            map("target", "1", "block.count", "1", "block.0.registryName", "minecraft:stone",
                "block.0.meta", "-1", "block.0.amount", "1", "block.0.oreDictionary", ""));
        assertTrue(evaluator.evaluate(legacyWildcard, TaskProgress.empty(legacyWildcard), sameBlockDifferentMeta)
            .isComplete());
    }

    @Test
    public void blockBreakKeepsOreDictionaryAndPartialNbtMatchingForWildcardSentinel() {
        BlockVectorTaskEvaluator evaluator = new BlockVectorTaskEvaluator("bq_standard:block_break",
            "galaxy:block_broken");
        String requiredNbt = PortableNbt.compound(withNbt("variant", "reinforced")).encode();
        TaskDefinition task = new TaskDefinition("break", "bq_standard:block_break", false,
            map("target", "1", "block.count", "1", "block.0.registryName", "minecraft:stone",
                "block.0.meta", "32767", "block.0.amount", "1", "block.0.oreDictionary", "oreTest",
                "block.0.nbtPortable", requiredNbt));
        Map<String, String> matching = map("registryName", "example:ore_block", "meta", "4",
            "oreDictionary", "oreTest", "nbtPortable",
            PortableNbt.compound(withNbt("variant", "reinforced", "owner", "test")).encode());
        assertTrue(evaluator.evaluate(task, TaskProgress.empty(task), fact("galaxy:block_broken", matching))
            .isComplete());

        matching.put("nbtPortable", PortableNbt.compound(withNbt("variant", "unreinforced", "owner", "test"))
            .encode());
        assertFalse(evaluator.evaluate(task, TaskProgress.empty(task), fact("galaxy:block_broken", matching))
            .isComplete());
    }

    @Test
    public void zeroAmountBlockPlaceholderDoesNotPreventRealRequirementCompletion() {
        BlockVectorTaskEvaluator evaluator = new BlockVectorTaskEvaluator("bq_standard:block_break", "galaxy:block_broken");
        TaskDefinition task = new TaskDefinition("break", "bq_standard:block_break", false,
            map("progressMode", "block-vector", "target", "1", "block.count", "2",
                "block.0.registryName", "minecraft:log", "block.0.meta", "-1", "block.0.amount", "1",
                "block.1.registryName", "minecraft:air", "block.1.meta", "0", "block.1.amount", "0"));
        TaskProgress before = TaskProgress.empty(task);
        assertEquals(Arrays.asList(0L, 0L), before.getValues());
        TaskProgress after = evaluator.evaluate(task, before, fact("galaxy:block_broken",
            map("registryName", "minecraft:log", "meta", "0")));
        assertEquals(Arrays.asList(1L, 0L), after.getValues());
        assertTrue(after.isComplete());
    }

    @Test
    public void retrievalUsesAbsoluteSnapshotAndDoesNotConsume() {
        InventorySnapshotTaskEvaluator evaluator = new InventorySnapshotTaskEvaluator("bq_standard:retrieval",
            "galaxy:inventory_observed");
        Map<String, String> params = map("progressMode", "item-vector", "target", "8", "item.count", "2",
            "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0", "item.0.amount", "5",
            "item.0.oreDictionary", "ingotIron", "item.1.registryName", "minecraft:gold_ingot", "item.1.meta", "0",
            "item.1.amount", "3", "item.1.oreDictionary", "ingotGold", "consume", "0", "ignoreNBT", "1");
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

        TaskProgress oneRequirementAlreadySatisfied = new TaskProgress("retrieve", Arrays.asList(5L, 2L),
            Arrays.asList(5L, 3L), false);
        assertEquals(Arrays.asList(5L, 0L), evaluator.evaluate(task, oneRequirementAlreadySatisfied, smaller).getValues());
        params.put("groupDetect", "true");
        TaskDefinition groupDetect = new TaskDefinition("retrieve", "bq_standard:retrieval", false, params);
        assertEquals(Arrays.asList(1L, 0L), evaluator.evaluate(groupDetect, oneRequirementAlreadySatisfied, smaller)
            .getValues());

        params.put("consume", "1");
        TaskDefinition consuming = new TaskDefinition("retrieve", "bq_standard:retrieval", false, params);
        assertEquals(Arrays.asList(0L, 0L), evaluator.evaluate(consuming, TaskProgress.empty(consuming), snapshot).getValues());
    }

    @Test
    public void retrievalReadsNumericBqBooleanFlagsWithoutInvertingConsumeOrNbtRules() {
        InventorySnapshotTaskEvaluator evaluator = new InventorySnapshotTaskEvaluator("bq_standard:retrieval",
            "galaxy:inventory_observed");
        Map<String, PortableNbt> requiredNbt = new LinkedHashMap<String, PortableNbt>();
        requiredNbt.put("CustomName", PortableNbt.string("Required"));
        TaskDefinition task = new TaskDefinition("retrieve", "bq_standard:retrieval", false,
            map("progressMode", "item-vector", "target", "1", "item.count", "1",
                "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0", "item.0.amount", "1",
                "item.0.nbtPortable", PortableNbt.compound(requiredNbt).encode(),
                "consume", "0", "ignoreNBT", "0", "partialMatch", "1"));
        GameplayFact matching = fact("galaxy:inventory_observed", map("inventory.count", "1",
            "inventory.0.registryName", "minecraft:iron_ingot", "inventory.0.meta", "0",
            "inventory.0.amount", "1", "inventory.0.nbtPortable", PortableNbt.compound(withNbt(
                "CustomName", "Required", "Lore", "Extra")).encode()));
        assertTrue(evaluator.evaluate(task, TaskProgress.empty(task), matching).isComplete());

        GameplayFact mismatching = fact("galaxy:inventory_observed", map("inventory.count", "1",
            "inventory.0.registryName", "minecraft:iron_ingot", "inventory.0.meta", "0",
            "inventory.0.amount", "1", "inventory.0.nbtPortable", PortableNbt.compound(withNbt(
                "CustomName", "Wrong")).encode()));
        assertFalse(evaluator.evaluate(task, TaskProgress.empty(task), mismatching).isComplete());

        TaskDefinition consuming = new TaskDefinition("retrieve", "bq_standard:retrieval", false,
            map("progressMode", "item-vector", "target", "1", "item.count", "1",
                "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0", "item.0.amount", "1",
                "consume", "1"));
        assertEquals(Arrays.asList(0L), evaluator.evaluate(consuming, TaskProgress.empty(consuming), matching).getValues());
    }

    @Test
    public void retrievalRequiresEmptyNbtToMatchEmptyObservedTagLikeBq() {
        InventorySnapshotTaskEvaluator evaluator = new InventorySnapshotTaskEvaluator("bq_standard:retrieval",
            "galaxy:inventory_observed");
        TaskDefinition task = new TaskDefinition("retrieve", "bq_standard:retrieval", false,
            map("progressMode", "item-vector", "item.count", "1", "item.0.registryName", "minecraft:iron_ingot",
                "item.0.meta", "0", "item.0.amount", "1", "item.0.nbtPortable", PortableNbt.compound(
                    new LinkedHashMap<String, PortableNbt>()).encode(), "ignoreNBT", "false", "partialMatch", "true"));
        GameplayFact tagged = fact("galaxy:inventory_observed", map("inventory.count", "1",
            "inventory.0.registryName", "minecraft:iron_ingot", "inventory.0.meta", "0", "inventory.0.amount", "1",
            "inventory.0.nbtPortable", PortableNbt.compound(withNbt("display", "Custom")).encode()));
        GameplayFact untagged = fact("galaxy:inventory_observed", map("inventory.count", "1",
            "inventory.0.registryName", "minecraft:iron_ingot", "inventory.0.meta", "0", "inventory.0.amount", "1",
            "inventory.0.nbtPortable", PortableNbt.compound(new LinkedHashMap<String, PortableNbt>()).encode()));

        assertEquals(Arrays.asList(0L), evaluator.evaluate(task, TaskProgress.empty(task), tagged).getValues());
        assertTrue(evaluator.evaluate(task, TaskProgress.empty(task), untagged).isComplete());
    }

    @Test
    public void optionalRetrievalWithPartialMatchDisabledStillAllowsExtraCompoundFieldsLikeBq() {
        InventorySnapshotTaskEvaluator evaluator = new InventorySnapshotTaskEvaluator(
            "bq_standard:optional_retrieval", "galaxy:inventory_observed");
        TaskDefinition task = new TaskDefinition("optional", "bq_standard:optional_retrieval", true,
            map("progressMode", "item-vector", "item.count", "1", "item.0.registryName", "mod:quest_item",
                "item.0.meta", "0", "item.0.amount", "1", "item.0.nbtPortable",
                PortableNbt.compound(withNbt("grade", "2")).encode(), "consume", "false",
                "ignoreNBT", "false", "partialMatch", "false"));
        GameplayFact withAdditionalMetadata = fact("galaxy:inventory_observed", map("inventory.count", "1",
            "inventory.0.registryName", "mod:quest_item", "inventory.0.meta", "0", "inventory.0.amount", "1",
            "inventory.0.nbtPortable", PortableNbt.compound(withNbt("grade", "2", "owner", "Alex")).encode()));
        GameplayFact missingRequiredField = fact("galaxy:inventory_observed", map("inventory.count", "1",
            "inventory.0.registryName", "mod:quest_item", "inventory.0.meta", "0", "inventory.0.amount", "1",
            "inventory.0.nbtPortable", PortableNbt.compound(withNbt("owner", "Alex")).encode()));

        assertTrue(evaluator.evaluate(task, TaskProgress.empty(task), withAdditionalMetadata).isComplete());
        assertEquals(Arrays.asList(0L), evaluator.evaluate(task, TaskProgress.empty(task), missingRequiredField)
            .getValues());
    }

    @Test
    public void retrievalOreDictionaryPreservesBqReverseNbtComparisonDirection() {
        InventorySnapshotTaskEvaluator evaluator = new InventorySnapshotTaskEvaluator("bq_standard:retrieval",
            "galaxy:inventory_observed");
        TaskDefinition oreTask = new TaskDefinition("retrieve", "bq_standard:retrieval", false,
            map("progressMode", "item-vector", "item.count", "1", "item.0.registryName", "mod:required_item",
                "item.0.meta", "0", "item.0.amount", "1", "item.0.oreDictionary", "ingotExample",
                "item.0.nbtPortable", PortableNbt.compound(withNbt("grade", "2", "extra", "required")).encode(),
                "ignoreNBT", "false", "partialMatch", "true"));
        GameplayFact oreSampleSubset = fact("galaxy:inventory_observed", map("inventory.count", "1",
            "inventory.0.registryName", "mod:other_ingot", "inventory.0.meta", "0", "inventory.0.amount", "1",
            "inventory.0.oreDictionary", "ingotExample", "inventory.0.nbtPortable",
            PortableNbt.compound(withNbt("grade", "2")).encode()));
        GameplayFact oreSampleSuperset = fact("galaxy:inventory_observed", map("inventory.count", "1",
            "inventory.0.registryName", "mod:other_ingot", "inventory.0.meta", "0", "inventory.0.amount", "1",
            "inventory.0.oreDictionary", "ingotExample", "inventory.0.nbtPortable",
            PortableNbt.compound(withNbt("grade", "2", "extra", "required", "unexpected", "value")).encode()));

        assertTrue(evaluator.evaluate(oreTask, TaskProgress.empty(oreTask), oreSampleSubset).isComplete());
        assertEquals(Arrays.asList(0L), evaluator.evaluate(oreTask, TaskProgress.empty(oreTask), oreSampleSuperset).getValues());

        ItemVectorTaskEvaluator consuming = new ItemVectorTaskEvaluator("bq_standard:retrieval",
            "galaxy:item_consumed");
        TaskDefinition consumeTask = new TaskDefinition("consume", "bq_standard:retrieval", false,
            map("progressMode", "item-vector", "target", "1", "item.count", "1",
                "item.0.registryName", "mod:required_item", "item.0.meta", "0", "item.0.amount", "1",
                "item.0.oreDictionary", "ingotExample",
                "item.0.nbtPortable", PortableNbt.compound(withNbt("grade", "2", "extra", "required")).encode(),
                "ignoreNBT", "false", "partialMatch", "true", "consume", "true"));
        GameplayFact consumed = fact("galaxy:item_consumed", map("registryName", "mod:other_ingot", "meta", "0",
            "amount", "1", "oreDictionary", "ingotExample", "nbtPortable",
            PortableNbt.compound(withNbt("grade", "2")).encode()));
        assertTrue(consuming.evaluate(consumeTask, TaskProgress.empty(consumeTask), consumed).isComplete());
        GameplayFact consumedSuperset = fact("galaxy:item_consumed", map("registryName", "mod:other_ingot", "meta", "0",
            "amount", "1", "oreDictionary", "ingotExample", "nbtPortable",
            PortableNbt.compound(withNbt("grade", "2", "extra", "required", "unexpected", "value")).encode()));
        assertEquals(Arrays.asList(0L), consuming.evaluate(consumeTask, TaskProgress.empty(consumeTask),
            consumedSuperset).getValues());

        TaskDefinition directTask = new TaskDefinition("direct", "bq_standard:retrieval", false,
            map("progressMode", "item-vector", "target", "1", "item.count", "1",
                "item.0.registryName", "mod:other_ingot", "item.0.meta", "0", "item.0.amount", "1",
                "item.0.nbtPortable", PortableNbt.compound(withNbt("grade", "2")).encode(),
                "ignoreNBT", "false", "partialMatch", "true", "consume", "true"));
        assertTrue(consuming.evaluate(directTask, TaskProgress.empty(directTask), consumedSuperset).isComplete());

        TaskDefinition emptyTagTask = new TaskDefinition("empty-tag", "bq_standard:retrieval", false,
            map("progressMode", "item-vector", "target", "1", "item.count", "1",
                "item.0.registryName", "mod:other_ingot", "item.0.meta", "0", "item.0.amount", "1",
                "item.0.nbtPortable", PortableNbt.compound(new LinkedHashMap<String, PortableNbt>()).encode(),
                "ignoreNBT", "false", "partialMatch", "true", "consume", "true"));
        assertEquals(Arrays.asList(0L), consuming.evaluate(emptyTagTask, TaskProgress.empty(emptyTagTask),
            consumed).getValues());
    }

    @Test
    public void craftingOreDictionaryUsesBqReverseNbtComparisonDirection() {
        ItemVectorTaskEvaluator evaluator = new ItemVectorTaskEvaluator("bq_standard:crafting", "galaxy:item_produced");
        TaskDefinition task = new TaskDefinition("craft", "bq_standard:crafting", false,
            map("progressMode", "item-vector", "target", "1", "item.count", "1",
                "item.0.registryName", "mod:required_item", "item.0.meta", "0", "item.0.amount", "1",
                "item.0.oreDictionary", "ingotExample",
                "item.0.nbtPortable", PortableNbt.compound(withNbt("grade", "2", "extra", "required")).encode(),
                "ignoreNBT", "false", "partialMatch", "true", "allowCraft", "true", "allowSmelt", "false"));
        GameplayFact subset = fact("galaxy:item_produced", map("registryName", "mod:other_ingot", "meta", "0",
            "amount", "1", "oreDictionary", "ingotExample", "channel", "craft", "nbtPortable",
            PortableNbt.compound(withNbt("grade", "2")).encode()));
        GameplayFact superset = fact("galaxy:item_produced", map("registryName", "mod:other_ingot", "meta", "0",
            "amount", "1", "oreDictionary", "ingotExample", "channel", "craft", "nbtPortable",
            PortableNbt.compound(withNbt("grade", "2", "extra", "required", "unexpected", "value")).encode()));

        assertTrue(evaluator.evaluate(task, TaskProgress.empty(task), subset).isComplete());
        assertEquals(Arrays.asList(0L), evaluator.evaluate(task, TaskProgress.empty(task), superset).getValues());
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
    public void retrievalKeepsBqSlotOrderWhenOverlappingNbtRequirementsCompeteForStacks() {
        InventorySnapshotTaskEvaluator evaluator = new InventorySnapshotTaskEvaluator("bq_standard:retrieval",
            "galaxy:inventory_observed");
        String broadNbt = PortableNbt.compound(withNbt("grade", "2")).encode();
        String narrowNbt = PortableNbt.compound(withNbt("grade", "2", "variant", "special")).encode();
        TaskDefinition task = new TaskDefinition("retrieve", "bq_standard:retrieval", false,
            map("progressMode", "item-vector", "target", "2", "item.count", "2",
                "item.0.registryName", "gtnh:quest_ingot",
                "item.0.meta", "0", "item.0.amount", "1", "item.0.nbtPortable", broadNbt,
                "item.1.registryName", "gtnh:quest_ingot", "item.1.meta", "0", "item.1.amount", "1",
                "item.1.nbtPortable", narrowNbt, "ignoreNBT", "false", "partialMatch", "false",
                "consume", "false"));
        InventoryObservationAttributeCodec codec = new InventoryObservationAttributeCodec();
        InventoryObservationEntry special = new InventoryObservationEntry("gtnh:quest_ingot", 0, 1L,
            "", "", PortableNbt.compound(withNbt("grade", "2", "variant", "special")).encode());
        InventoryObservationEntry plain = new InventoryObservationEntry("gtnh:quest_ingot", 0, 1L,
            "", "", broadNbt);

        // BQ TaskRetrieval.Detector visits slots first and requirements in definition order within each slot.
        // The first requirement can consume the special stack before the narrower requirement sees it.
        assertEquals(Arrays.asList(1L, 0L), evaluator.evaluate(task, TaskProgress.empty(task),
            fact("galaxy:inventory_observed", codec.encode(Arrays.asList(special, plain)))).getValues());
        assertEquals(Arrays.asList(1L, 1L), evaluator.evaluate(task, TaskProgress.empty(task),
            fact("galaxy:inventory_observed", codec.encode(Arrays.asList(plain, special)))).getValues());
    }

    @Test
    public void retrievalKeepsSatisfiedComponentAndReallocatesInventoryToLaterRequirements() {
        InventorySnapshotTaskEvaluator evaluator = new InventorySnapshotTaskEvaluator("bq_standard:retrieval",
            "galaxy:inventory_observed");
        Map<String, String> params = map("progressMode", "item-vector", "target", "8", "item.count", "2",
            "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0", "item.0.amount", "4",
            "item.1.registryName", "minecraft:iron_ingot", "item.1.meta", "0", "item.1.amount", "4",
            "consume", "false", "groupDetect", "false", "ignoreNBT", "true");
        TaskDefinition task = new TaskDefinition("retrieve", "bq_standard:retrieval", false, params);
        TaskProgress previous = new TaskProgress("retrieve", Arrays.asList(4L, 0L), Arrays.asList(4L, 4L), false);
        GameplayFact smaller = fact("galaxy:inventory_observed", map("inventory.count", "1",
            "inventory.0.registryName", "minecraft:iron_ingot", "inventory.0.meta", "0",
            "inventory.0.amount", "2"));
        assertEquals(Arrays.asList(4L, 2L), evaluator.evaluate(task, previous, smaller).getValues());
        GameplayFact empty = fact("galaxy:inventory_observed", map("inventory.count", "0"));
        assertEquals(Arrays.asList(4L, 0L), evaluator.evaluate(task, previous, empty).getValues());
    }

    @Test
    public void retrievalMatchesAnyDamageValueForDamageableItemsLikeBq() {
        InventorySnapshotTaskEvaluator evaluator = new InventorySnapshotTaskEvaluator("bq_standard:retrieval",
            "galaxy:inventory_observed");
        TaskDefinition task = new TaskDefinition("retrieve", "bq_standard:retrieval", false,
            map("progressMode", "item-vector", "target", "2", "item.count", "1",
                "item.0.registryName", "gtnh:durable_tool", "item.0.meta", "0", "item.0.amount", "2",
                "ignoreNBT", "true", "consume", "false"));
        TaskProgress start = TaskProgress.empty(task);
        GameplayFact damaged = fact("galaxy:inventory_observed", map("inventory.count", "1",
            "inventory.0.registryName", "gtnh:durable_tool", "inventory.0.meta", "31",
            "inventory.0.damageable", "true", "inventory.0.amount", "2"));
        assertEquals(Arrays.asList(2L), evaluator.evaluate(task, start, damaged).getValues());

        GameplayFact nonDamageableVariant = fact("galaxy:inventory_observed", map("inventory.count", "1",
            "inventory.0.registryName", "gtnh:durable_tool", "inventory.0.meta", "31",
            "inventory.0.damageable", "false", "inventory.0.amount", "2"));
        assertEquals(Arrays.asList(0L), evaluator.evaluate(task, start, nonDamageableVariant).getValues());
    }

    @Test
    public void retrievalMatchesRepeatedCompoundNbtListEntriesLikeBq() {
        InventorySnapshotTaskEvaluator evaluator = new InventorySnapshotTaskEvaluator("bq_standard:retrieval",
            "galaxy:inventory_observed");
        Map<String, PortableNbt> matchingFields = new LinkedHashMap<String, PortableNbt>();
        matchingFields.put("id", PortableNbt.number("7"));
        PortableNbt matchingEntry = PortableNbt.compound(matchingFields);
        Map<String, PortableNbt> nonMatchingFields = new LinkedHashMap<String, PortableNbt>();
        nonMatchingFields.put("id", PortableNbt.number("8"));
        PortableNbt nonMatchingEntry = PortableNbt.compound(nonMatchingFields);

        Map<String, PortableNbt> requiredTagFields = new LinkedHashMap<String, PortableNbt>();
        requiredTagFields.put("ench", PortableNbt.list(Arrays.asList(matchingEntry, matchingEntry)));
        String requiredTag = PortableNbt.compound(requiredTagFields).encode();
        Map<String, PortableNbt> actualTagFields = new LinkedHashMap<String, PortableNbt>();
        actualTagFields.put("ench", PortableNbt.list(Arrays.asList(matchingEntry, nonMatchingEntry)));
        String actualTag = PortableNbt.compound(actualTagFields).encode();

        TaskDefinition task = new TaskDefinition("retrieve", "bq_standard:retrieval", false,
            map("progressMode", "item-vector", "item.count", "1", "item.0.registryName", "gtnh:quest_item",
                "item.0.meta", "0", "item.0.amount", "1", "item.0.nbtPortable", requiredTag,
                "ignoreNBT", "false", "partialMatch", "true", "consume", "false"));
        GameplayFact inventory = fact("galaxy:inventory_observed", map("inventory.count", "1",
            "inventory.0.registryName", "gtnh:quest_item", "inventory.0.meta", "0", "inventory.0.amount", "1",
            "inventory.0.nbtPortable", actualTag));

        assertEquals(Arrays.asList(1L), evaluator.evaluate(task, TaskProgress.empty(task), inventory).getValues());
    }

    @Test
    public void groupDetectRecalculatesIncompleteComponentsFromFreshAggregateSnapshot() {
        InventorySnapshotTaskEvaluator evaluator = new InventorySnapshotTaskEvaluator("bq_standard:retrieval",
            "galaxy:inventory_observed");
        TaskDefinition task = new TaskDefinition("retrieve", "bq_standard:retrieval", false,
            map("progressMode", "item-vector", "target", "8", "item.count", "2",
                "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0", "item.0.amount", "4",
                "item.1.registryName", "minecraft:gold_ingot", "item.1.meta", "0", "item.1.amount", "4",
                "groupDetect", "true", "consume", "false"));
        TaskProgress previous = new TaskProgress("retrieve", Arrays.asList(3L, 0L), Arrays.asList(4L, 4L), false);
        GameplayFact aggregate = fact("galaxy:inventory_observed", map("inventory.count", "1",
            "inventory.0.registryName", "minecraft:iron_ingot", "inventory.0.meta", "0",
            "inventory.0.amount", "2"));
        assertEquals(Arrays.asList(2L, 0L), evaluator.evaluate(task, previous, aggregate).getValues());
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
        assertEquals("BQ non-consuming detection may reuse one observed fluid stack for another requirement",
            Arrays.asList(1000L, 2000L),
            evaluator.evaluate(task, TaskProgress.empty(task), snapshot).getValues());
        params.put("consume", "true");
        TaskDefinition consuming = new TaskDefinition("fluid", "bq_standard:fluid", false, params);
        assertEquals(Arrays.asList(0L, 0L),
            evaluator.evaluate(consuming, TaskProgress.empty(consuming), snapshot).getValues());
    }

    @Test
    public void fluidSnapshotUsesExactBqTagPresenceWhenNbtMatchingIsEnabled() {
        FluidSnapshotTaskEvaluator evaluator = new FluidSnapshotTaskEvaluator("bq_standard:fluid",
            "galaxy:fluid_inventory_observed");
        Map<String, String> params = map("progressMode", "fluid-vector", "target", "1000", "fluid.count", "1",
            "fluid.0.name", "water", "fluid.0.amount", "1000", "fluid.0.nbt", "{}",
            "fluid.0.nbtPortable", "", "fluid.0.nbtPresent", "false", "consume", "false", "ignoreNBT", "false");
        TaskDefinition noTag = new TaskDefinition("fluid-no-tag", "bq_standard:fluid", false, params);
        GameplayFact untagged = fact("galaxy:fluid_inventory_observed", map("fluidInventory.count", "1",
            "fluidInventory.0.name", "water", "fluidInventory.0.amount", "1000",
            "fluidInventory.0.nbtPresent", "false"));
        GameplayFact explicitlyEmpty = fact("galaxy:fluid_inventory_observed", map("fluidInventory.count", "1",
            "fluidInventory.0.name", "water", "fluidInventory.0.amount", "1000",
            "fluidInventory.0.nbtPresent", "true"));
        assertTrue(evaluator.evaluate(noTag, TaskProgress.empty(noTag), untagged).isComplete());
        assertFalse(evaluator.evaluate(noTag, TaskProgress.empty(noTag), explicitlyEmpty).isComplete());

        params.put("fluid.0.nbtPresent", "true");
        TaskDefinition emptyTag = new TaskDefinition("fluid-empty-tag", "bq_standard:fluid", false, params);
        assertTrue(evaluator.evaluate(emptyTag, TaskProgress.empty(emptyTag), explicitlyEmpty).isComplete());
        assertFalse(evaluator.evaluate(emptyTag, TaskProgress.empty(emptyTag), untagged).isComplete());
    }

    @Test
    public void fluidGroupDetectReplacesEveryComponentButNormalDetectKeepsSatisfiedComponents() {
        FluidSnapshotTaskEvaluator evaluator = new FluidSnapshotTaskEvaluator("bq_standard:fluid",
            "galaxy:fluid_inventory_observed");
        Map<String, String> groupParameters = map("progressMode", "fluid-vector", "target", "2000",
            "fluid.count", "2", "fluid.0.name", "water", "fluid.0.amount", "1000",
            "fluid.1.name", "lava", "fluid.1.amount", "1000", "consume", "false",
            "ignoreNBT", "true", "groupDetect", "true");
        TaskDefinition groupTask = new TaskDefinition("fluid", "bq_standard:fluid", false, groupParameters);
        TaskProgress previous = new TaskProgress("fluid", Arrays.asList(1000L, 500L),
            Arrays.asList(1000L, 1000L), false);
        GameplayFact lavaOnly = fact("galaxy:fluid_inventory_observed", map("fluidInventory.count", "1",
            "fluidInventory.0.name", "lava", "fluidInventory.0.amount", "500"));

        assertEquals(Arrays.asList(0L, 500L), evaluator.evaluate(groupTask, previous, lavaOnly).getValues());

        groupParameters.put("groupDetect", "false");
        TaskDefinition normalTask = new TaskDefinition("fluid", "bq_standard:fluid", false, groupParameters);
        assertEquals(Arrays.asList(1000L, 500L), evaluator.evaluate(normalTask, previous, lavaOnly).getValues());
    }

    @Test
    public void partyFluidSnapshotIsRestrictedToImportedBqFluidTasks() {
        FluidSnapshotTaskEvaluator evaluator = new FluidSnapshotTaskEvaluator("bq_standard:fluid",
            "galaxy:fluid_inventory_observed");
        Map<String, String> parameters = map("factType", "galaxy:fluid_inventory_observed",
            "progressMode", "fluid-vector", "target", "1000",
            "fluid.count", "1", "fluid.0.name", "water", "fluid.0.amount", "1000",
            "consume", "false", "ignoreNBT", "true");
        TaskDefinition authored = new TaskDefinition("fluid", "bq_standard:fluid", false, parameters);
        parameters.put("bq.raw", "{}");
        TaskDefinition imported = new TaskDefinition("fluid", "bq_standard:fluid", false, parameters);
        TaskProgress previous = new TaskProgress("fluid", 250L, 1000L, false);
        GameplayFact partySnapshot = fact("galaxy:fluid_inventory_observed", map(
            "fluidInventory.count", "1", "fluidInventory.0.name", "water", "fluidInventory.0.amount", "1000",
            "inventory.bqPartySnapshot", "true", "inventory.bqPartyId", UUID.randomUUID().toString()));

        assertSame(previous, evaluator.evaluate(authored, previous, partySnapshot));
        assertTrue(evaluator.evaluate(imported, previous, partySnapshot).isComplete());
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
        assertTrue(evaluator.evaluate(special, TaskProgress.empty(special), fact("galaxy:location_observed",
            map("dimension", "0", "x", "0", "y", "64", "z", "0", "biome", "1"))).isComplete());
        assertFalse(evaluator.evaluate(special, TaskProgress.empty(special), fact("galaxy:location_observed",
            map("dimension", "7", "x", "10", "y", "64", "z", "10", "biome", "3"))).isComplete());
        assertTrue(evaluator.evaluate(special, TaskProgress.empty(special), fact("galaxy:location_observed",
            map("dimension", "7", "x", "10", "y", "64", "z", "10", "biome", "3",
                LocationTaskEvaluator.structureEvidenceKey("Village"), "false"))).isComplete());
    }

    @Test
    public void locationVisibilityMatchesBqShortCircuitAndInfiniteRangeRules() {
        LocationTaskEvaluator evaluator = new LocationTaskEvaluator("bq_standard:location", "galaxy:location_observed");
        Map<String, String> params = map("target", "1", "dimension", "7", "posX", "10", "posY", "64",
            "posZ", "10", "range", "-1", "biome", "3", "structure", "Village", "visible", "true",
            "invert", "false", "taxiCabDist", "false");
        TaskDefinition task = new TaskDefinition("visit", "bq_standard:location", false, params);
        GameplayFact noLineOfSightData = fact("galaxy:location_observed", map("dimension", "7", "x", "10",
            "y", "64", "z", "10", "biome", "3", LocationTaskEvaluator.structureEvidenceKey("Village"),
            "true"));

        // BQ only performs its visibility raycast for positive, finite ranges.
        assertTrue(evaluator.evaluate(task, TaskProgress.empty(task), noLineOfSightData).isComplete());

        params.put("range", "5");
        TaskDefinition finite = new TaskDefinition("visit", "bq_standard:location", false, params);
        assertFalse(evaluator.evaluate(finite, TaskProgress.empty(finite), noLineOfSightData).isComplete());
        GameplayFact blocked = fact("galaxy:location_observed", map("dimension", "7", "x", "10", "y", "64",
            "z", "10", "biome", "3", LocationTaskEvaluator.structureEvidenceKey("Village"), "true",
            LocationTaskEvaluator.visibilityEvidenceKey(params), "false"));
        params.put("invert", "true");
        TaskDefinition inverted = new TaskDefinition("visit", "bq_standard:location", false, params);
        assertTrue(evaluator.evaluate(inverted, TaskProgress.empty(inverted), blocked).isComplete());
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
    public void meetingFiltersLargestSharedSnapshotByEachTasksBqExpandedPlayerAabb() {
        MeetingTaskEvaluator evaluator = new MeetingTaskEvaluator("bq_standard:meeting",
            "galaxy:nearby_entities_observed");
        TaskDefinition task = new TaskDefinition("meet", "bq_standard:meeting", false,
            map("target", "1", "subject", "Villager", "range", "4", "amount", "1",
                "subtypes", "true", "ignoreNBT", "true"));

        Map<String, String> outsideSmallRange = meetingSnapshot("5", "5.4");
        assertFalse("a matching entity found by the merged radius must not satisfy a smaller BQ query",
            evaluator.evaluate(task, TaskProgress.empty(task), fact("galaxy:nearby_entities_observed",
                outsideSmallRange)).isComplete());

        Map<String, String> intersectsSmallRange = meetingSnapshot("4.2", "4.6");
        assertTrue("BQ tests entity-box intersection with player.boundingBox.expand(range), not center distance",
            evaluator.evaluate(task, TaskProgress.empty(task), fact("galaxy:nearby_entities_observed",
                intersectsSmallRange)).isComplete());

        Map<String, String> boundaryOnly = meetingSnapshot("4.3", "4.8");
        assertFalse("touching the query face without overlap does not intersect the BQ AABB",
            evaluator.evaluate(task, TaskProgress.empty(task), fact("galaxy:nearby_entities_observed",
                boundaryOnly)).isComplete());

        Map<String, String> missingGeometry = map("observedRadius", "8", "nearbyEntity.count", "1",
            "nearbyEntity.0.subject", "Villager");
        assertFalse("an oversized snapshot without per-entity bounds cannot prove task-range membership",
            evaluator.evaluate(task, TaskProgress.empty(task), fact("galaxy:nearby_entities_observed",
                missingGeometry)).isComplete());

        Map<String, String> malformedGeometry = meetingSnapshot("4.2", "4.6");
        malformedGeometry.put("nearbyEntity.0.maxX", "not-a-number");
        assertFalse("malformed geometry fails closed instead of escaping the evaluator",
            evaluator.evaluate(task, TaskProgress.empty(task), fact("galaxy:nearby_entities_observed",
                malformedGeometry)).isComplete());
    }

    private static Map<String, String> meetingSnapshot(String entityMinX, String entityMaxX) {
        return map("observedRadius", "8", "nearbyEntity.count", "1",
            "observer.minX", "-0.3", "observer.minY", "0", "observer.minZ", "-0.3",
            "observer.maxX", "0.3", "observer.maxY", "1.8", "observer.maxZ", "0.3",
            "nearbyEntity.0.subject", "Villager", "nearbyEntity.0.subjectAliases", "Villager",
            "nearbyEntity.0.minX", entityMinX, "nearbyEntity.0.minY", "0.1",
            "nearbyEntity.0.minZ", "-0.2", "nearbyEntity.0.maxX", entityMaxX,
            "nearbyEntity.0.maxY", "1.9", "nearbyEntity.0.maxZ", "0.2");
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

    @Test
    public void consumingFluidCreditsTheWholeIndivisibleContainerAmount() {
        Map<String, String> parameters = map("progressMode", "fluid-vector", "fluid.count", "1",
            "fluid.0.amount", "1000", "target", "1000", "consume", "true");
        TaskDefinition task = new TaskDefinition("fluid", "bq_standard:fluid", false, parameters);
        TaskProgress current = new TaskProgress("fluid", 400L, 1000L, false);
        GameplayFact applied = fact("galaxy:consumption_applied", map("taskKey", "fluid", "kind", "fluid",
            "value.count", "1", "value.0", "750"));

        TaskProgress result = new ConsumptionAppliedTaskEvaluator("bq_standard:fluid", "fluid")
            .evaluate(task, current, applied);

        assertEquals("BQ adds the full amount drained, even when it exceeds the remaining 600 mB",
            Arrays.asList(1150L), result.getValues());
        assertTrue(result.isComplete());
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
        assertEquals("BQ ignores entity types absent from EntityList even if a registered superclass alias exists",
            0L, entity.evaluate(entityTask, TaskProgress.empty(entityTask), fact("galaxy:entity_interacted",
                map("subject", "", "subjectAliases", "Villager|Creature", "hit", "false",
                    "heldItem.registryName", "minecraft:emerald", "heldItem.meta", "0"))).getValue());
        TaskDefinition missingTarget = new TaskDefinition("talk-missing", "bq_standard:interact_entity", false,
            map("target", "1", "onInteract", "true", "ignoreTargetNBT", "true", "ignoreItemNBT", "true"));
        assertEquals("BQ cannot resolve an empty target ID", 0L,
            entity.evaluate(missingTarget, TaskProgress.empty(missingTarget), talk).getValue());
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

    private static GameplayFact sourceFact(String sourceServer, UUID playerId, String eventId,
        Map<String, String> attributes) {
        return new GameplayFact(sourceServer, eventId, playerId, "galaxy:item_produced", 1L, attributes);
    }

    private static Map<String, PortableNbt> withNbt(String... values) {
        Map<String, PortableNbt> result = new LinkedHashMap<String, PortableNbt>();
        for (int i = 0; i < values.length; i += 2) result.put(values[i], PortableNbt.string(values[i + 1]));
        return result;
    }

    private static Map<String, String> map(String... values) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int i = 0; i < values.length; i += 2) result.put(values[i], values[i + 1]);
        return result;
    }
}
