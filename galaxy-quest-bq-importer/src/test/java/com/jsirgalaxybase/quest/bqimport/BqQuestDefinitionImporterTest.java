package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.Test;

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
            + "\"rewards:9\":{\"0:10\":{\"index:3\":0,\"rewardID:8\":\"bq_standard:item\"}}}";
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        BqImportedQuest imported = new BqQuestDefinitionImporter().read(file);
        BqImportedQuest importedAgain = new BqQuestDefinitionImporter().read(file);
        assertEquals("测试任务", imported.getDefinition().getName());
        assertEquals("bq_standard:retrieval", imported.getDefinition().getTasks().get(0).getTypeId());
        assertTrue(imported.getDefinition().getTasks().get(0).getParameters().get("bq.raw").contains("consume:1"));
        assertEquals("item-vector", imported.getDefinition().getTasks().get(0).getParameters().get("progressMode"));
        assertEquals("64", imported.getDefinition().getTasks().get(0).getParameters().get("target"));
        assertEquals("minecraft:iron_ingot",
            imported.getDefinition().getTasks().get(0).getParameters().get("item.0.registryName"));
        assertEquals("bq_standard:item", imported.getDefinition().getRewards().get(0).getTypeId());
        assertEquals(2L, imported.getDefinition().getId().getLeastSignificantBits());
        assertEquals("1", imported.getTaskSpecs().get(0).getOptions().get("consume"));
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
    public void compatibilityReportNeverSilentlyDropsUnknownTypes() throws Exception {
        Path root = Files.createTempDirectory("bq-default");
        Path questDir = Files.createDirectories(root.resolve("Quests/line"));
        Files.write(questDir.resolve("quest.json"), ("{\"questIDHigh:4\":0,\"questIDLow:4\":7,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Q\"}},\"tasks:9\":{"
            + "\"0:10\":{\"index:3\":0,\"taskID:8\":\"addon:special\"}},\"rewards:9\":{}}")
                .getBytes(StandardCharsets.UTF_8));
        BqCompatibilityReport report = BqCompatibilityReport.create(new BqQuestDefinitionImporter().readDirectory(root),
            Collections.singleton("bq_standard:retrieval"), Collections.<String>emptySet());
        assertEquals(1, report.getQuestCount());
        assertTrue(report.getUnknownTaskTypes().contains("addon:special"));
        assertEquals(BqCompatibilityStatus.UNKNOWN, report.getTaskCompatibility().get("addon:special"));
    }
}
