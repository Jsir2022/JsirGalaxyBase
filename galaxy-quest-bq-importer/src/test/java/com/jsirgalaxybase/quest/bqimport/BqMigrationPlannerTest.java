package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

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

    @Test public void prerequisiteCyclesAndDuplicateQuestIdsFailClosedButCrossChapterPlacementIsAllowed() throws Exception {
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
        assertFalse(manifest.toText().contains("DUPLICATE_CHAPTER_PLACEMENT"));
    }

    @Test public void sameQuestInTwoChaptersIsApplicable() throws Exception {
        Path root = root("bq-plan-cross-chapter");
        writeQuest(root, "a.json", 7, "{}", "{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"}}");
        Path first = Files.createDirectories(root.resolve("QuestLines/Main-AAAAAAAAAAAAAAAAAAAAAQ=="));
        Path second = Files.createDirectories(root.resolve("QuestLines/Second-AAAAAAAAAAAAAAAAAAAAAg=="));
        Files.write(root.resolve("QuestLinesOrder.txt"), ("AAAAAAAAAAAAAAAAAAAAAQ==: Main\n"
            + "AAAAAAAAAAAAAAAAAAAAAg==: Second\n").getBytes(StandardCharsets.UTF_8));
        String entry = "{\"questIDHigh:4\":0,\"questIDLow:4\":7,\"sizeX:3\":24,\"sizeY:3\":24,\"x:3\":0,\"y:3\":0}";
        Files.write(first.resolve("a.json"), entry.getBytes(StandardCharsets.UTF_8));
        Files.write(second.resolve("a.json"), entry.getBytes(StandardCharsets.UTF_8));
        assertTrue(new BqMigrationPlanner().plan(root).isApplicable());
    }

    @Test public void emptyBqItemRewardIsApplicableNoOp() throws Exception {
        Path root = root("bq-plan-empty-item-reward");
        String quest = "{\"questIDHigh:4\":0,\"questIDLow:4\":9,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Empty reward\"}},"
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"}},"
            + "\"rewards:9\":{\"0:10\":{\"index:3\":0,\"rewardID:8\":\"bq_standard:item\",\"rewards:9\":{}}}}";
        Files.write(root.resolve("Quests/line/quest.json"), quest.getBytes(StandardCharsets.UTF_8));
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(root);
        assertTrue(manifest.toText(), manifest.isApplicable());
        assertEquals("0", manifest.getQuests().get(0).getDefinition().getRewards().get(0)
            .getParameters().get("item.count"));
    }

    @Test public void seededExclusiveChoiceCycleIsPreservedButUnseededCycleIsBlocked() throws Exception {
        Path seeded = root("bq-plan-seeded-xor");
        writeQuest(seeded, "seed.json", 7, "{}",
            "{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"}}");
        writeXorQuest(seeded, "first.json", 8, 7, 9);
        writeXorQuest(seeded, "second.json", 9, 7, 8);
        BqMigrationManifest applicable = new BqMigrationPlanner().plan(seeded);
        assertTrue(applicable.toText(), applicable.isApplicable());
        assertTrue(applicable.toText().contains("WARNING|SEEDED_XOR_CYCLE"));
        assertFalse(applicable.toText().contains("PREREQUISITE_CYCLE"));

        Path unseeded = root("bq-plan-unseeded-xor");
        writeXorQuest(unseeded, "first.json", 8, 9);
        writeXorQuest(unseeded, "second.json", 9, 8);
        BqMigrationManifest blocked = new BqMigrationPlanner().plan(unseeded);
        assertFalse(blocked.isApplicable());
        assertTrue(blocked.toText().contains("ERROR|PREREQUISITE_CYCLE"));
    }

    @Test public void supportedTypesStillFailClosedWhenNormalizedParametersAreInvalid() throws Exception {
        Path root = root("bq-plan-invalid-normalized");
        writeQuest(root, "quest.json", 12, "{}",
            "{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:xp\",\"amount:3\":0}} ");
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(root);
        assertFalse(manifest.isApplicable());
        assertTrue(manifest.toText().contains("INVALID_ELEMENT_PARAMETER"));
        assertTrue(manifest.toText().contains("tasks.task-0.amount"));
        BqCompatibilityReport compatibility = BqCompatibilityReport.create(manifest);
        assertEquals(BqCompatibilityStatus.BLOCKED,
            compatibility.getElementCompatibility().get("TASK|00000000-0000-0000-0000-00000000000c|task-0|bq_standard:xp"));
    }

    @Test public void xpRewardRejectsValuesThatCannotBeDeliveredByTheRuntimeHandler() throws Exception {
        for (String amount : new String[] { "-1", "2147483648", "not-a-number" }) {
            Path root = root("bq-plan-invalid-xp-reward");
            String quest = "{\"questIDHigh:4\":0,\"questIDLow:4\":31,"
                + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"XP\"}},"
                + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"}},"
                + "\"rewards:9\":{\"0:10\":{\"index:3\":0,\"rewardID:8\":\"bq_standard:xp\","
                + "\"amount:3\":" + amount + "}}}";
            Files.write(root.resolve("Quests/line/xp.json"), quest.getBytes(StandardCharsets.UTF_8));
            BqMigrationManifest manifest = new BqMigrationPlanner().plan(root);
            assertFalse(manifest.toText(), manifest.isApplicable());
            assertTrue(manifest.toText().contains("INVALID_REWARD_PARAMETER"));
            assertTrue(manifest.toText().contains("XP reward amount"));
        }
    }

    @Test public void outOfRangeTaskIntegerProducesBlockedPlanInsteadOfAbortingDryRun() throws Exception {
        Path root = root("bq-plan-invalid-task-integer");
        String quest = "{\"questIDHigh:4\":0,\"questIDLow:4\":34,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Hunt\"}},"
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:hunt\","
            + "\"required:3\":2147483648,\"target:8\":\"Zombie\"}},\"rewards:9\":{}}";
        Files.write(root.resolve("Quests/line/hunt.json"), quest.getBytes(StandardCharsets.UTF_8));

        BqMigrationManifest manifest = new BqMigrationPlanner().plan(root);
        assertFalse(manifest.toText(), manifest.isApplicable());
        assertTrue(manifest.toText().contains("INVALID_TASK_PARAMETER"));
        assertTrue(manifest.toText().contains("task parameter required must fit a signed 32-bit integer"));
        assertTrue(BqCompatibilityReport.create(manifest).getElementCompatibility().get(
            "TASK|00000000-0000-0000-0000-000000000022|task-0|bq_standard:hunt")
            == BqCompatibilityStatus.BLOCKED);
    }

    @Test public void questCompletionRewardsRejectMissingAndSelfTargets() throws Exception {
        Path root = root("bq-plan-invalid-completion-target");
        writeQuestCompletion(root, "missing.json", 32, 99);
        writeQuestCompletion(root, "self.json", 33, 33);
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(root);
        assertFalse(manifest.toText(), manifest.isApplicable());
        assertTrue(manifest.toText().contains("MISSING_QUEST_COMPLETION_TARGET"));
        assertTrue(manifest.toText().contains("SELF_QUEST_COMPLETION_TARGET"));
    }

    private static void writeQuestCompletion(Path root, String file, long questId, long targetId)
        throws Exception {
        String compactTarget = Base64.getUrlEncoder().withoutPadding().encodeToString(
            ByteBuffer.allocate(16).putLong(0L).putLong(targetId).array());
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":" + questId
            + ",\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Completion\"}},"
            + "\"tasks:9\":{},\"rewards:9\":{\"0:10\":{\"index:3\":0,"
            + "\"rewardID:8\":\"bq_standard:questcompletion\",\"questID:8\":\""
            + compactTarget + "\"}}}";
        Files.write(root.resolve("Quests/line").resolve(file), json.getBytes(StandardCharsets.UTF_8));
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

    private static void writeXorQuest(Path root, String file, long low, long... prerequisiteIds) throws Exception {
        StringBuilder prerequisites = new StringBuilder("{");
        for (int i = 0; i < prerequisiteIds.length; i++) {
            if (i > 0) prerequisites.append(',');
            prerequisites.append('"').append(i).append(":10\":{\"questIDHigh:4\":0,\"questIDLow:4\":")
                .append(prerequisiteIds[i]).append('}');
        }
        prerequisites.append('}');
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":" + low + ","
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Choice\","
            + "\"questLogic:8\":\"XOR\"}},\"preRequisites:9\":" + prerequisites + ","
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"}},"
            + "\"rewards:9\":{}}";
        Files.write(root.resolve("Quests/line").resolve(file), json.getBytes(StandardCharsets.UTF_8));
    }
}
