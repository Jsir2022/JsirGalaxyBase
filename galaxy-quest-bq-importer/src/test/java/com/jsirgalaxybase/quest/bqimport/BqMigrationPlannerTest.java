package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class BqMigrationPlannerTest {
    @Test public void manifestIsDeterministicAndUnknownTypesBlockApply() throws Exception {
        Path root = Files.createTempDirectory("bq-plan");
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        Path questDir = Files.createDirectories(root.resolve("Quests/line"));
        Files.write(questDir.resolve("quest.json"), ("{\"questIDHigh:4\":0,\"questIDLow:4\":7,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Q\"}},\"tasks:9\":{"
            + "\"0:10\":{\"index:3\":0,\"taskID:8\":\"addon:special\"}},\"rewards:9\":{}}")
                .getBytes(StandardCharsets.UTF_8));
        BqMigrationManifest first = new BqMigrationPlanner().plan(root);
        BqMigrationManifest second = new BqMigrationPlanner().plan(root);
        assertEquals(first.getBatchId(), second.getBatchId());
        assertEquals(first.getSourceHash(), second.getSourceHash());
        assertFalse(first.isApplicable());
        assertTrue(first.toText().contains("UNSUPPORTED_TASK"));
        assertTrue(first.toText().contains("UNPLACED_QUEST"));
    }

    @Test public void supportedUnplacedQuestIsAWarningOnly() throws Exception {
        Path root = Files.createTempDirectory("bq-plan-supported");
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        Path questDir = Files.createDirectories(root.resolve("Quests/line"));
        Files.write(questDir.resolve("quest.json"), ("{\"questIDHigh:4\":0,\"questIDLow:4\":8,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Q\"}},\"tasks:9\":{"
            + "\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"}},\"rewards:9\":{}}")
                .getBytes(StandardCharsets.UTF_8));
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(root);
        assertTrue(manifest.isApplicable());
        assertTrue(manifest.toText().contains("WARNING|UNPLACED_QUEST"));
    }

    @Test public void missingPrerequisiteInvalidParametersAndDanglingChapterFailClosed() throws Exception {
        Path root = root("bq-plan-invalid");
        writeQuest(root, "quest.json", 7, "{\"0:10\":{\"questIDHigh:4\":0,\"questIDLow:4\":99}}",
            "{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:retrieval\",\"requiredItems:9\":{}}}");
        Path line = Files.createDirectories(root.resolve("QuestLines/Main-AAAAAAAAAAAAAAAAAAAAAQ=="));
        Files.createDirectories(root.resolve("QuestLines/Unordered-AAAAAAAAAAAAAAAAAAAAAg=="));
        Files.write(root.resolve("QuestLinesOrder.txt"), ("AAAAAAAAAAAAAAAAAAAAAQ==: Main\n"
            + "AAAAAAAAAAAAAAAAAAAAAw==: Missing directory\n").getBytes(StandardCharsets.UTF_8));
        Files.write(line.resolve("missing.json"),
            "{\"questIDHigh:4\":0,\"questIDLow:4\":8,\"sizeX:3\":24,\"sizeY:3\":24,\"x:3\":0,\"y:3\":0}"
                .getBytes(StandardCharsets.UTF_8));
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(root);
        assertFalse(manifest.isApplicable());
        assertTrue(manifest.toText().contains("MISSING_PREREQUISITE"));
        assertTrue(manifest.toText().contains("INVALID_TASK_PARAMETER"));
        assertTrue(manifest.toText().contains("DANGLING_CHAPTER_ENTRY"));
        assertTrue(manifest.toText().contains("MISSING_CHAPTER_ORDER"));
        assertTrue(manifest.toText().contains("DANGLING_CHAPTER_ORDER"));
        BqCompatibilityReport matrix=BqCompatibilityReport.create(manifest);
        assertEquals(BqCompatibilityStatus.BLOCKED,matrix.getTaskCompatibility().get("bq_standard:retrieval"));
        assertTrue(matrix.toText().contains("TASK|00000000-0000-0000-0000-000000000007|task-0|bq_standard:retrieval=BLOCKED"));
    }

    @Test public void prerequisiteCyclesDuplicateQuestIdsAndDuplicatePlacementFailClosed() throws Exception {
        Path root = root("bq-plan-cycle");
        writeQuest(root, "a.json", 7, "{\"0:10\":{\"questIDHigh:4\":0,\"questIDLow:4\":8}}",
            "{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"}}");
        writeQuest(root, "b.json", 8, "{\"0:10\":{\"questIDHigh:4\":0,\"questIDLow:4\":7}}",
            "{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"}}");
        writeQuest(root, "duplicate.json", 8, "{\"0:10\":{\"questIDHigh:4\":0,\"questIDLow:4\":7}}",
            "{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"}}");
        Path line = Files.createDirectories(root.resolve("QuestLines/Main-AAAAAAAAAAAAAAAAAAAAAQ=="));
        Path secondLine = Files.createDirectories(root.resolve("QuestLines/Second-AAAAAAAAAAAAAAAAAAAAAg=="));
        Files.write(root.resolve("QuestLinesOrder.txt"), ("AAAAAAAAAAAAAAAAAAAAAQ==: Main\n"
            + "AAAAAAAAAAAAAAAAAAAAAg==: Second\n").getBytes(StandardCharsets.UTF_8));
        String entry="{\"questIDHigh:4\":0,\"questIDLow:4\":7,\"sizeX:3\":24,\"sizeY:3\":24,\"x:3\":0,\"y:3\":0}";
        Files.write(line.resolve("a.json"), entry.getBytes(StandardCharsets.UTF_8));
        Files.write(secondLine.resolve("a-copy.json"), entry.getBytes(StandardCharsets.UTF_8));
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(root);
        assertFalse(manifest.isApplicable());
        assertTrue(manifest.toText().contains("DUPLICATE_QUEST_UUID"));
        assertTrue(manifest.toText().contains("PREREQUISITE_CYCLE"));
        assertTrue(manifest.toText().contains("DUPLICATE_CHAPTER_PLACEMENT"));
    }

    @Test public void supportedTypesStillFailClosedWhenNormalizedParametersAreInvalid() throws Exception {
        Path root = root("bq-plan-invalid-normalized");
        writeQuest(root, "quest.json", 12, "{}",
            "{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:xp\",\"amount:3\":0}} ");
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(root);
        assertFalse(manifest.isApplicable());
        assertTrue(manifest.toText().contains("INVALID_ELEMENT_PARAMETER"));
        assertTrue(manifest.toText().contains("tasks.task-0.amount"));
    }

    private static Path root(String prefix) throws Exception {
        Path root = Files.createTempDirectory(prefix);
        Files.createDirectories(root.resolve("QuestLines"));
        Files.createDirectories(root.resolve("Quests/line"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        return root;
    }

    private static void writeQuest(Path root,String file,long low,String prerequisites,String tasks) throws Exception {
        String json="{\"questIDHigh:4\":0,\"questIDLow:4\":"+low+","
            +"\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Q"+low+"\"}},"
            +"\"preRequisites:9\":"+prerequisites+",\"tasks:9\":"+tasks+",\"rewards:9\":{}}";
        Files.write(root.resolve("Quests/line").resolve(file),json.getBytes(StandardCharsets.UTF_8));
    }
}
