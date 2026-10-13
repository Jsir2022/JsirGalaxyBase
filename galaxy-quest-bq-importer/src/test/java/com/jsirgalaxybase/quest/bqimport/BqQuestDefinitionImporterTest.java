package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.GameplayFact;
import com.jsirgalaxybase.quest.core.InventorySnapshotTaskEvaluator;
import com.jsirgalaxybase.quest.core.QuestEngine;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.quest.core.TaskProgress;

public class BqQuestDefinitionImporterTest {
    @Test
    public void importsTypedNbtJsonWithoutDroppingRawTaskAndRewardData() throws Exception {
        Path file = Files.createTempFile("bq-quest", ".json");
        String json = "{\"questIDHigh:4\":1,\"questIDLow:4\":2,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"测试任务\",\"desc:8\":\"说明\","
            + "\"questLogic:8\":\"AND\",\"taskLogic:8\":\"OR\",\"repeatTime:3\":40,"
            + "\"repeat_relative:1\":0,\"visibility:8\":\"SECRET\",\"autoClaim:1\":1,"
            + "\"lockedProgress:1\":1,\"isSilent:1\":1,\"icon:10\":{\"id:8\":\"minecraft:book\"}}},"
            + "\"preRequisites:9\":{},\"tasks:9\":{\"0:10\":{\"index:3\":0,"
            + "\"taskID:8\":\"bq_standard:retrieval\",\"consume:1\":1,\"requiredItems:9\":{\"0:10\":{"
            + "\"id:8\":\"minecraft:iron_ingot\",\"Damage:2\":0,\"Count:3\":64}}}},"
            + "\"rewards:9\":{\"0:10\":{\"index:3\":0,\"rewardID:8\":\"bq_standard:item\"},"
            + "\"1:10\":{\"index:3\":1,\"rewardID:8\":\"bq_standard:questcompletion\"},"
            + "\"2:10\":{\"index:3\":2,\"rewardID:8\":\"bq_standard:command\",\"ignoreDisabled:1\":0},"
            + "\"3:10\":{\"index:3\":3,\"rewardID:8\":\"bq_standard:scoreboard\"}}}";
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        BqImportedQuest imported = new BqQuestDefinitionImporter().read(file);
        BqImportedQuest importedAgain = new BqQuestDefinitionImporter().read(file);
        assertEquals("测试任务", imported.getDefinition().getName());
        assertEquals("bq_standard:retrieval", imported.getDefinition().getTasks().get(0).getTypeId());
        assertTrue(imported.getDefinition().getTasks().get(0).getParameters().get("bq.raw").contains("consume:1"));
        assertEquals("item-vector", imported.getDefinition().getTasks().get(0).getParameters().get("progressMode"));
        assertEquals("false", imported.getDefinition().getTasks().get(0).getParameters().get("bq.partyShare"));
        assertEquals(com.jsirgalaxybase.quest.core.QuestParticipantScope.PLAYER,
            imported.getDefinition().getBehavior().getParticipantScope());
        assertEquals("64", imported.getDefinition().getTasks().get(0).getParameters().get("target"));
        assertEquals("minecraft:iron_ingot",
            imported.getDefinition().getTasks().get(0).getParameters().get("item.0.registryName"));
        assertEquals("bq_standard:item", imported.getDefinition().getRewards().get(0).getTypeId());
        assertEquals("false", imported.getDefinition().getRewards().get(0).getParameters().get("ignoreDisabled"));
        assertEquals("true", imported.getDefinition().getRewards().get(1).getParameters().get("ignoreDisabled"));
        assertEquals("false", imported.getDefinition().getRewards().get(2).getParameters().get("ignoreDisabled"));
        assertEquals("true", imported.getRewardSpecs().get(1).getOptions().get("ignoreDisabled"));
        assertEquals("true", imported.getDefinition().getRewards().get(3).getParameters().get("ignoreDisabled"));
        assertEquals(2L, imported.getDefinition().getId().getLeastSignificantBits());
        assertEquals("true", imported.getTaskSpecs().get(0).getOptions().get("consume"));
        assertEquals(com.jsirgalaxybase.quest.core.QuestVisibility.SECRET,
            imported.getDefinition().getBehavior().getVisibility());
        assertTrue(imported.getDefinition().getBehavior().isAutoClaim());
        assertTrue(imported.getDefinition().getBehavior().isProgressWhileLocked());
        assertTrue(imported.getDefinition().getBehavior().isSilent());
        assertFalse(imported.getDefinition().getRepeatPolicy().isRelative());
        assertEquals(2000L, imported.getDefinition().getRepeatPolicy().getCooldownMillis());
        assertTrue(imported.getDefinition().getBehavior().getIconReference().contains("minecraft:book"));
        assertEquals(imported.getRawJson(), importedAgain.getRawJson());
        assertEquals(imported.getDefinition().getTasks().get(0).getParameters(),
            importedAgain.getDefinition().getTasks().get(0).getParameters());
    }

    @Test
    public void optionalRetrievalImportStaysOptionalThroughInventoryProgressAndQuestCompletion() throws Exception {
        Path file = Files.createTempFile("bq-optional-retrieval", ".json");
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":23,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Optional retrieval\"}},"
            + "\"preRequisites:9\":{},\"tasks:9\":{"
            + "\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"},"
            + "\"1:10\":{\"index:3\":1,\"taskID:8\":\"bq_standard:optional_retrieval\","
            + "\"requiredItems:9\":{\"0:10\":{\"id:8\":\"minecraft:iron_ingot\","
            + "\"Damage:2\":0,\"Count:3\":5}}}},\"rewards:9\":{}}";
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));

        com.jsirgalaxybase.quest.core.QuestDefinition definition =
            new BqQuestDefinitionImporter().read(file).getDefinition();
        TaskDefinition optional = definition.getTasks().get(1);
        assertEquals("bq_standard:optional_retrieval", optional.getTypeId());
        assertTrue("BQ Optional Retrieval is ignored by Quest completion", optional.isOptional());

        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("inventory.count", "1");
        attributes.put("inventory.0.registryName", "minecraft:iron_ingot");
        attributes.put("inventory.0.meta", "0");
        attributes.put("inventory.0.amount", "3");
        attributes.put("inventory.0.damageable", "false");
        GameplayFact inventory = new GameplayFact("test", "optional-retrieval", UUID.randomUUID(),
            "galaxy:inventory_observed", 1L, attributes);
        TaskProgress optionalProgress = new InventorySnapshotTaskEvaluator(optional.getTypeId(),
            "galaxy:inventory_observed").evaluate(optional, TaskProgress.empty(optional), inventory);
        assertEquals(Arrays.asList(3L), optionalProgress.getValues());
        assertFalse(optionalProgress.isComplete());

        Map<String, TaskProgress> observations = new LinkedHashMap<String, TaskProgress>();
        observations.put(definition.getTasks().get(0).getKey(), new TaskProgress(
            definition.getTasks().get(0).getKey(), 1L, 1L, true));
        observations.put(optional.getKey(), optionalProgress);
        com.jsirgalaxybase.quest.core.QuestEvaluation result = new QuestEngine().evaluate(
            UUID.randomUUID(), definition, null, observations, Collections.<UUID>emptySet(), 10L);
        assertEquals(QuestStatus.COMPLETED, result.getProgress().getStatus());
        assertFalse(result.getProgress().getTasks().get(optional.getKey()).isComplete());
    }

    @Test
    public void canonicalizesNumericBetterQuestingBooleanOptionsButPreservesRawSource() throws Exception {
        Path file = Files.createTempFile("bq-boolean-options", ".json");
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":19,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Flags\"}},"
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:retrieval\","
            + "\"consume:1\":1,\"ignoreNBT:1\":0,\"partialMatch:1\":1,"
            + "\"requiredItems:9\":{\"0:10\":{\"id:8\":\"minecraft:iron_ingot\",\"Count:3\":1}}}},"
            + "\"rewards:9\":{}}";
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        BqImportedQuest imported = new BqQuestDefinitionImporter().read(file);
        assertEquals("true", imported.getTaskSpecs().get(0).getOptions().get("consume"));
        assertEquals("false", imported.getTaskSpecs().get(0).getOptions().get("ignoreNBT"));
        assertEquals("true", imported.getTaskSpecs().get(0).getOptions().get("partialMatch"));
        assertEquals("true", imported.getDefinition().getTasks().get(0).getParameters().get("consume"));
        assertTrue(imported.getDefinition().getTasks().get(0).getParameters().get("bq.raw").contains("consume:1"));
    }

    @Test public void malformedQuestBooleanDoesNotSilentlyUseItsDefault() throws Exception {
        Path file = Files.createTempFile("bq-invalid-quest-flag", ".json");
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":21,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Flags\",\"autoClaim:1\":\"sometimes\"}},"
            + "\"tasks:9\":{},\"rewards:9\":{}}";
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        try {
            new BqQuestDefinitionImporter().read(file);
            fail("malformed BQ quest flags must not silently become false");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Invalid BetterQuesting boolean property autoClaim=sometimes"));
        }
    }

    @Test public void unknownQuestVisibilityDoesNotSilentlyBecomeNormal() throws Exception {
        Path file = Files.createTempFile("bq-invalid-visibility", ".json");
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":22,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Visibility\","
            + "\"visibility:8\":\"FUTURE_MODE\"}},\"tasks:9\":{},\"rewards:9\":{}}";
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        try {
            new BqQuestDefinitionImporter().read(file);
            fail("unknown BQ visibility must not silently become NORMAL");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("unsupported BQ quest visibility: FUTURE_MODE"));
        }
    }

    @Test public void missingXpRewardAmountPreservesBqNbtReadDefaultOfZero() throws Exception {
        Path file = Files.createTempFile("bq-xp-default", ".json");
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":23,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"XP\"}},"
            + "\"tasks:9\":{},\"rewards:9\":{\"0:10\":{\"index:3\":0,"
            + "\"rewardID:8\":\"bq_standard:xp\"}}}";
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        BqImportedQuest imported = new BqQuestDefinitionImporter().read(file);
        assertEquals("0", imported.getDefinition().getRewards().get(0).getParameters().get("amount"));
        assertEquals("points", imported.getDefinition().getRewards().get(0).getParameters().get("xpUnit"));
    }

    @Test public void rejectsFractionalOrOutOfRangeRepeatTimeRatherThanChangingTheCooldown() throws Exception {
        Path file = Files.createTempFile("bq-invalid-repeat-time", ".json");
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":23,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Repeat\","
            + "\"repeatTime:3\":2147483648}},\"tasks:9\":{},\"rewards:9\":{}}";
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> new BqQuestDefinitionImporter().read(file));

        Files.write(file, json.replace("2147483648", "1.5").getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> new BqQuestDefinitionImporter().read(file));
    }

    @Test public void planningReportsFractionalOrOutOfRangeRuntimeIntegerOptions() throws Exception {
        String[] invalidTaskJson = {
            "\"taskID:8\":\"bq_standard:hunt\",\"required:3\":2147483648",
            "\"taskID:8\":\"bq_standard:location\",\"dimension:3\":1.5",
            "\"taskID:8\":\"bq_standard:meeting\",\"range:3\":-2147483649"
        };
        for (String task : invalidTaskJson) {
            Path root = Files.createTempDirectory("bq-invalid-task-int");
            Path quests = Files.createDirectories(root.resolve("Quests/line"));
            Files.createDirectories(root.resolve("QuestLines"));
            Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
            String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":24,"
                + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Numeric\"}},"
                + "\"tasks:9\":{\"0:10\":{" + task + "}},\"rewards:9\":{}}";
            Files.write(quests.resolve("quest.json"), json.getBytes(StandardCharsets.UTF_8));
            BqMigrationManifest manifest = new BqMigrationPlanner().plan(root);
            assertFalse(manifest.toText(), manifest.isApplicable());
            assertTrue(manifest.toText(), manifest.toText().contains("INVALID_TASK_PARAMETER"));
            assertTrue(manifest.toText(), manifest.toText().contains("must fit a signed 32-bit integer"));
        }
    }

    @Test
    public void compatibilityReportNeverSilentlyDropsUnknownTypes() throws Exception {
        Path root = Files.createTempDirectory("bq-default");
        Path questDir = Files.createDirectories(root.resolve("Quests/line"));
        Files.write(questDir.resolve("quest.json"), ("{\"questIDHigh:4\":0,\"questIDLow:4\":7,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Q\"}},\"tasks:9\":{"
            + "\"0:10\":{\"index:3\":0,\"taskID:8\":\"addon:optional_special\"}},\"rewards:9\":{}}")
                .getBytes(StandardCharsets.UTF_8));
        BqImportedQuest imported = new BqQuestDefinitionImporter().readDirectory(root).get(0);
        assertFalse("unknown task names must not grant optional completion semantics",
            imported.getDefinition().getTasks().get(0).isOptional());
        assertEquals("an unsupported required task must keep the imported quest incomplete", QuestStatus.AVAILABLE,
            new QuestEngine().evaluate(UUID.randomUUID(), imported.getDefinition(), null,
                Collections.<String, TaskProgress>emptyMap(), Collections.<UUID>emptySet(), 10L)
                .getProgress().getStatus());
        BqCompatibilityReport report = BqCompatibilityReport.create(Collections.singletonList(imported),
            Collections.singleton("bq_standard:retrieval"), Collections.<String>emptySet());
        assertEquals(1, report.getQuestCount());
        assertTrue(report.getUnknownTaskTypes().contains("addon:optional_special"));
        assertEquals(BqCompatibilityStatus.UNKNOWN, report.getTaskCompatibility().get("addon:optional_special"));
    }

    @Test public void importsExclusivePrerequisitesWithoutSilentlyChangingThemToAnd() throws Exception {
        Path file = Files.createTempFile("bq-xor-quest", ".json");
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":15,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Choice\","
            + "\"questLogic:8\":\"XOR\"}},\"tasks:9\":{},\"rewards:9\":{}}";
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        assertEquals(com.jsirgalaxybase.quest.core.QuestLogic.XOR,
            new BqQuestDefinitionImporter().read(file).getDefinition().getPrerequisiteLogic());
    }
}
