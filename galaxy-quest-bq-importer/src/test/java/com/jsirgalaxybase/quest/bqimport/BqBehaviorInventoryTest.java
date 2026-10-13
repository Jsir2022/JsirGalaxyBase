package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

import org.junit.Test;

public class BqBehaviorInventoryTest {
    @Test public void countsRealTypeAndParameterShapesAndGatesOnlyUnverifiedRuntimeTypes() throws Exception {
        Path root = Files.createTempDirectory("bq-behavior-inventory");
        Path quests = Files.createDirectories(root.resolve("Quests/line"));
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        write(quests.resolve("one.json"), 1, "bq_standard:checkbox", "bq_standard:xp", true);
        write(quests.resolve("two.json"), 2, "bq_standard:checkbox", "addon:reward", true);

        BqMigrationManifest manifest = new BqMigrationPlanner().plan(root);
        BqBehaviorInventory inventory = BqBehaviorInventory.from(manifest);
        assertEquals(2, inventory.getTasks().get("bq_standard:checkbox").getCount());
        assertEquals(Integer.valueOf(2), inventory.getTasks().get("bq_standard:checkbox")
            .getOptionKeys().get("consume"));
        assertEquals(Integer.valueOf(2), inventory.getTasks().get("bq_standard:checkbox")
            .getBehaviorFlags().get("consume").get("true"));
        assertEquals(Integer.valueOf(1), inventory.getRewards().get("bq_standard:xp")
            .getBehaviorFlags().get("ignoreDisabled").get("true"));
        assertTrue(inventory.toText().contains("behaviorFlags={ignoreDisabled={true=1}}"));
        assertTrue(inventory.toText().contains("TASK|bq_standard:checkbox|2|SUPPORTED"));
        assertTrue(inventory.toText().contains("REWARD|addon:reward|1|UNKNOWN"));
        assertEquals(inventory.toText(), BqBehaviorInventory.from(new BqMigrationPlanner().plan(root)).toText());
        try {
            inventory.requireRuntimeReady();
            org.junit.Assert.fail("unverified behavior must block publishing");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("TASK bq_standard:checkbox OPTION consume=UNVERIFIED"));
            assertTrue(expected.getMessage().contains("REWARD addon:reward=UNKNOWN"));
        }
    }

    @Test public void aKnownTypeWithAnUnverifiedOptionIsBlockedBeforeApply() throws Exception {
        Path root = Files.createTempDirectory("bq-runtime-unknown-option");
        Path quests = Files.createDirectories(root.resolve("Quests/line"));
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        Path source = quests.resolve("quest.json");
        write(source, 1, "bq_standard:checkbox", "bq_standard:item");
        String json = new String(Files.readAllBytes(source), StandardCharsets.UTF_8).replace(
            "\"taskID:8\":\"bq_standard:checkbox\"",
            "\"taskID:8\":\"bq_standard:checkbox\",\"futureFlag:8\":\"on\"");
        Files.write(source, json.getBytes(StandardCharsets.UTF_8));

        try {
            BqBehaviorInventory.from(new BqMigrationPlanner().plan(root)).requireRuntimeReady();
            org.junit.Assert.fail("a parsed but unverified task option must block publication");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("TASK bq_standard:checkbox OPTION futureFlag=UNVERIFIED"));
        }
    }

    @Test public void malformedBooleanBehaviorOptionIsBlockedEvenWhenItsTypeIsSupported() throws Exception {
        Path root = Files.createTempDirectory("bq-runtime-invalid-option");
        Path quests = Files.createDirectories(root.resolve("Quests/line"));
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        Path source = quests.resolve("quest.json");
        write(source, 1, "bq_standard:checkbox", "bq_standard:xp");
        String json = new String(Files.readAllBytes(source), StandardCharsets.UTF_8).replace(
            "\"ignoreDisabled:1\":1", "\"ignoreDisabled:1\":2");
        Files.write(source, json.getBytes(StandardCharsets.UTF_8));

        try {
            BqBehaviorInventory.from(new BqMigrationPlanner().plan(root)).requireRuntimeReady();
            org.junit.Assert.fail("malformed BQ boolean values must block publication");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Invalid BetterQuesting boolean option ignoreDisabled=2"));
        }
    }

    @Test public void outOfRangeIntegerOptionsAreReportedAndBlockRuntimePublication() throws Exception {
        Path root = Files.createTempDirectory("bq-runtime-invalid-integer");
        Path quests = Files.createDirectories(root.resolve("Quests/line"));
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":41,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Hunt\"}},"
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:hunt\","
            + "\"required:3\":2147483648,\"target:8\":\"Zombie\"}},\"rewards:9\":{}}";
        Files.write(quests.resolve("quest.json"), json.getBytes(StandardCharsets.UTF_8));

        BqBehaviorInventory inventory = BqBehaviorInventory.from(new BqMigrationPlanner().plan(root));
        assertEquals(Integer.valueOf(1), inventory.getTasks().get("bq_standard:hunt")
            .getInvalidIntegerOptions().get("required"));
        assertTrue(inventory.toText().contains("invalidIntegerOptions={required=1}"));
        try {
            inventory.requireRuntimeReady();
            org.junit.Assert.fail("out-of-range integer options must block publication");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("TASK bq_standard:hunt INTEGER required=INVALID_VALUE"));
        }
    }

    @Test public void inventoryReportsAnonymousFrequencyOfValidIntegerOptionValues() throws Exception {
        Path root = Files.createTempDirectory("bq-runtime-integer-profiles");
        Path quests = Files.createDirectories(root.resolve("Quests/line"));
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":42,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Private Name\"}},"
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:hunt\","
            + "\"required:3\":2,\"target:8\":\"Zombie\"}},\"rewards:9\":{}}";
        Files.write(quests.resolve("quest.json"), json.getBytes(StandardCharsets.UTF_8));

        BqBehaviorInventory inventory = BqBehaviorInventory.from(new BqMigrationPlanner().plan(root));
        assertEquals(Integer.valueOf(1), inventory.getTasks().get("bq_standard:hunt")
            .getIntegerOptionProfiles().get("required").get("2"));
        assertTrue(inventory.toText().contains("integerOptionProfiles={required={2=1}}"));
        assertTrue(!inventory.toText().contains("Private Name") && !inventory.toText().contains("Zombie"));
    }

    @Test public void applyRejectsRecognizedButUnverifiedBehaviorBeforeDatabaseConfiguration() throws Exception {
        Path root = Files.createTempDirectory("bq-behavior-apply-gate");
        Path quests = Files.createDirectories(root.resolve("Quests/line"));
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        write(quests.resolve("quest.json"), 1, "bq_standard:xp", "bq_standard:item");
        try {
            BqMigrationMain.main(new String[] { "apply", root.toString() });
            org.junit.Assert.fail("unverified behavior must block the CLI apply command");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("TASK bq_standard:xp=ADAPTED"));
        }
    }

    @Test public void currentGtnhRuntimeTypesPassTheExecutablePublicationGate() throws Exception {
        Path root = Files.createTempDirectory("bq-runtime-ready");
        Path quests = Files.createDirectories(root.resolve("Quests/line"));
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        write(quests.resolve("quest.json"), 1, "bq_standard:checkbox", "bq_standard:item");

        BqBehaviorInventory inventory = BqBehaviorInventory.from(new BqMigrationPlanner().plan(root));
        inventory.requireRuntimeReady();
        assertEquals(BqCompatibilityStatus.SUPPORTED,
            BqRuntimeCapabilityRegistry.taskStatus("bq_standard:checkbox"));
        assertEquals(BqCompatibilityStatus.SUPPORTED,
            BqRuntimeCapabilityRegistry.rewardStatus("bq_standard:item"));
        assertTrue(BqRuntimeCapabilityRegistry.taskEvidence().containsKey("bq_standard:retrieval"));
        assertTrue(BqRuntimeCapabilityRegistry.rewardEvidence().containsKey("bq_standard:choice"));
    }

    @Test public void countsOreDictionaryAndNbtMatchingIntersectionsWithoutPrintingItemData() throws Exception {
        Path root = Files.createTempDirectory("bq-behavior-item-match");
        Path quests = Files.createDirectories(root.resolve("Quests/line"));
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":9,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Q\"}},"
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:retrieval\","
            + "\"ignoreNBT:1\":0,\"requiredItems:9\":{\"0:10\":{\"id:8\":\"mod:required\","
            + "\"Damage:2\":0,\"Count:3\":1,\"OreDict:8\":\"ingotExample\","
            + "\"tag:10\":{\"grade:3\":2,\"numbers:9\":{\"0:3\":1,\"1:3\":2},"
            + "\"entries:9\":{\"0:10\":{\"kind:8\":\"private\"}}}}}}},"
            + "\"rewards:9\":{\"0:10\":{\"index:3\":0,\"rewardID:8\":\"bq_standard:xp\","
            + "\"amount:3\":1}}}";
        Files.write(quests.resolve("quest.json"), json.getBytes(StandardCharsets.UTF_8));

        BqBehaviorInventory inventory = BqBehaviorInventory.from(new BqMigrationPlanner().plan(root));
        BqBehaviorInventory.TypeUsage retrieval = inventory.getTasks().get("bq_standard:retrieval");
        assertEquals(Integer.valueOf(1), retrieval.getItemMatchProfiles().get("itemRequirements"));
        assertEquals(Integer.valueOf(1), retrieval.getItemMatchProfiles().get("oreDictionary"));
        assertEquals(Integer.valueOf(1), retrieval.getItemMatchProfiles().get("nonEmptyNbt"));
        assertEquals(Integer.valueOf(1), retrieval.getItemMatchProfiles().get("nbtRoot.compound"));
        assertEquals(Integer.valueOf(2), retrieval.getItemMatchProfiles().get("nbtNode.list"));
        assertEquals(Integer.valueOf(2), retrieval.getItemMatchProfiles().get("nbtListElement.number"));
        assertEquals(Integer.valueOf(1), retrieval.getItemMatchProfiles().get("nbtListElement.compound"));
        assertEquals(Integer.valueOf(2), retrieval.getItemMatchProfiles()
            .get("nbtListElementWithNbtCheckEnabled.number"));
        assertEquals(Integer.valueOf(1), retrieval.getItemMatchProfiles()
            .get("nbtListElementWithNbtCheckEnabled.compound"));
        assertEquals(Integer.valueOf(1), retrieval.getItemMatchProfiles().get("itemRequirementsWithNbtCheckEnabled"));
        assertEquals(Integer.valueOf(1), retrieval.getItemMatchProfiles()
            .get("itemRequirementsWithNbtCheckEnabledWithData"));
        assertEquals(Integer.valueOf(1), retrieval.getItemMatchProfiles().get("oreDictionaryAndNbtCheckEnabledWithData"));
        assertTrue(inventory.toText().contains("itemMatchProfiles={"));
        assertTrue(!inventory.toText().contains("ingotExample") && !inventory.toText().contains("mod:required")
            && !inventory.toText().contains("private"));
    }

    @Test public void profilesJointRetrievalFlagsWithoutPrintingQuestIdentifiers() throws Exception {
        Path root = Files.createTempDirectory("bq-retrieval-behavior-profiles");
        Path quests = Files.createDirectories(root.resolve("Quests/line"));
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":81,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Private Quest Name\"}},"
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:retrieval\","
            + "\"consume:1\":0,\"groupDetect:1\":0,\"ignoreNBT:1\":0,\"partialMatch:1\":0,"
            + "\"requiredItems:9\":{\"0:10\":{\"id:8\":\"mod:private_item\","
            + "\"Count:3\":1,\"tag:10\":{\"grade:3\":2}}}}},\"rewards:9\":{}}";
        Files.write(quests.resolve("quest.json"), json.getBytes(StandardCharsets.UTF_8));

        BqBehaviorInventory inventory = BqBehaviorInventory.from(new BqMigrationPlanner().plan(root));
        BqBehaviorInventory.TypeUsage retrieval = inventory.getTasks().get("bq_standard:retrieval");
        String profile = "consume=false;groupDetect=false;ignoreNBT=false;partialMatch=false";

        assertEquals(Integer.valueOf(1), retrieval.getBehaviorProfiles().get(profile));
        assertEquals(Integer.valueOf(1), retrieval.getItemMatchProfiles()
            .get("partialMatchDisabledAndNbtCheckEnabledWithData"));
        assertTrue(inventory.toText().contains("behaviorProfiles={" + profile + "=1}"));
        assertTrue(!inventory.toText().contains("Private Quest Name")
            && !inventory.toText().contains("mod:private_item"));
    }

    @Test public void profilesQuestCompletionSourcesAndTargetsWithoutPrintingIdentifiers() throws Exception {
        Path root = Files.createTempDirectory("bq-quest-completion-inventory");
        Path quests = Files.createDirectories(root.resolve("Quests/line"));
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        String targetId = encodeQuestId(2L);
        String sourceJson = "{\"questIDHigh:4\":0,\"questIDLow:4\":1,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Source\","
            + "\"repeatTime:3\":120,\"isGlobal:1\":1,\"globalShare:1\":0,\"autoClaim:1\":1}},"
            + "\"rewards:9\":{\"0:10\":{\"index:3\":0,"
            + "\"rewardID:8\":\"bq_standard:questcompletion\",\"questID:8\":\"" + targetId + "\"}}}";
        String targetJson = "{\"questIDHigh:4\":0,\"questIDLow:4\":2,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Target\","
            + "\"repeatTime:3\":-1,\"autoClaim:1\":1,\"isGlobal:1\":1}}}";
        Files.write(quests.resolve("source.json"), sourceJson.getBytes(StandardCharsets.UTF_8));
        Files.write(quests.resolve("target.json"), targetJson.getBytes(StandardCharsets.UTF_8));

        BqBehaviorInventory inventory = BqBehaviorInventory.from(new BqMigrationPlanner().plan(root));
        assertEquals(Integer.valueOf(1), inventory.getQuestCompletionProfiles().get("references"));
        assertEquals(Integer.valueOf(1), inventory.getQuestCompletionProfiles().get("uniqueSourceQuests"));
        assertEquals(Integer.valueOf(1), inventory.getQuestCompletionProfiles().get("uniqueTargetQuests"));
        assertEquals(Integer.valueOf(1), inventory.getQuestCompletionProfiles().get("sourceRepeatable"));
        assertEquals(Integer.valueOf(1), inventory.getQuestCompletionProfiles().get("sourceGlobal"));
        assertEquals(Integer.valueOf(0), inventory.getQuestCompletionProfiles().get("sourceGlobalShare"));
        assertEquals(Integer.valueOf(1), inventory.getQuestCompletionProfiles().get("sourceAutoClaim"));
        assertEquals(Integer.valueOf(1), inventory.getQuestCompletionProfiles().get("targetGlobal"));
        assertEquals(Integer.valueOf(1), inventory.getQuestCompletionProfiles().get("targetAutoClaim"));
        assertTrue(inventory.toText().contains("QUEST_COMPLETION|references={"));
        assertTrue(!inventory.toText().contains(targetId));
    }

    @Test public void profilesGlobalCheckboxSemanticsWithoutQuestIdentifiers() throws Exception {
        Path root = Files.createTempDirectory("bq-global-checkbox-inventory");
        Path quests = Files.createDirectories(root.resolve("Quests/line"));
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        String individualClaim = globalCheckboxQuest(11L, "Secret Global Quest A", false);
        String sharedClaim = globalCheckboxQuest(12L, "Secret Global Quest B", true);
        Files.write(quests.resolve("one.json"), individualClaim.getBytes(StandardCharsets.UTF_8));
        Files.write(quests.resolve("two.json"), sharedClaim.getBytes(StandardCharsets.UTF_8));

        BqBehaviorInventory inventory = BqBehaviorInventory.from(new BqMigrationPlanner().plan(root));
        String individualProfile = "global=true,globalShare=false,autoClaim=false,lockedProgress=false,"
            + "simultaneous=false,repeatable=false";
        String sharedProfile = "global=true,globalShare=true,autoClaim=false,lockedProgress=false,"
            + "simultaneous=false,repeatable=false";
        assertEquals(1, inventory.getQuestProfiles().get(individualProfile).getQuestCount());
        assertEquals(Integer.valueOf(1), inventory.getQuestProfiles().get(individualProfile)
            .getTaskTypes().get("bq_standard:checkbox"));
        assertEquals(1, inventory.getQuestProfiles().get(sharedProfile).getQuestCount());
        assertEquals(Integer.valueOf(1), inventory.getQuestProfiles().get(sharedProfile)
            .getTaskTypes().get("bq_standard:checkbox"));
        String text = inventory.toText();
        assertTrue(text.contains("QUEST_PROFILE|" + individualProfile + "|quests=1"
            + "|tasks={bq_standard:checkbox=1}|rewards={}"));
        assertTrue(text.contains("QUEST_PROFILE|" + sharedProfile + "|quests=1"
            + "|tasks={bq_standard:checkbox=1}|rewards={}"));
        assertTrue(!text.contains("Secret Global Quest") && !text.contains(encodeQuestId(11L))
            && !text.contains(encodeQuestId(12L)));
    }

    @Test public void profilesLocationCriteriaWithoutExposingQuestNamesStructuresOrCoordinates() throws Exception {
        Path root = Files.createTempDirectory("bq-location-inventory");
        Path quests = Files.createDirectories(root.resolve("Quests/line"));
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":3,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Secret Quest Name\"}},"
            + "\"tasks:9\":{"
            + "\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:location\","
            + "\"name:8\":\"Secret Location Name\",\"dimension:3\":2,\"biome:3\":-1,"
            + "\"posX:3\":100,\"posY:3\":64,\"posZ:3\":-20,\"range:3\":0,"
            + "\"structure:8\":\"SecretStructureId\",\"visible:1\":0,\"invert:1\":0,"
            + "\"taxiCabDist:1\":0,\"hideInfo:1\":1},"
            + "\"1:10\":{\"index:3\":1,\"taskID:8\":\"bq_standard:location\","
            + "\"dimension:3\":7,\"biome:3\":42,\"posX:3\":0,\"posY:3\":0,\"posZ:3\":0,"
            + "\"range:3\":64,\"structure:8\":\"\",\"visible:1\":1,\"invert:1\":1,"
            + "\"taxiCabDist:1\":1,\"hideInfo:1\":0}},\"rewards:9\":{}}";
        Files.write(quests.resolve("locations.json"), json.getBytes(StandardCharsets.UTF_8));

        BqBehaviorInventory inventory = BqBehaviorInventory.from(new BqMigrationPlanner().plan(root));

        assertEquals(2, inventory.getLocationTaskCount());
        assertEquals(1, inventory.getDistinctLocationStructures());
        assertEquals(Integer.valueOf(1), inventory.getLocationProfiles().get("biome").get("-1"));
        assertEquals(Integer.valueOf(1), inventory.getLocationProfiles().get("biome").get("42"));
        assertEquals(Integer.valueOf(1), inventory.getLocationProfiles().get("range").get("unbounded"));
        assertEquals(Integer.valueOf(1), inventory.getLocationProfiles().get("range").get("finite"));
        assertEquals(Integer.valueOf(1), inventory.getLocationProfiles().get("structure").get("configured"));
        assertEquals(Integer.valueOf(1), inventory.getLocationProfiles().get("structure").get("empty"));
        assertEquals(Integer.valueOf(2), inventory.getLocationProfiles().get("repeatable").get("false"));
        assertEquals(Integer.valueOf(2), inventory.getLocationProfiles().get("autoClaim").get("false"));
        assertEquals(Integer.valueOf(2), inventory.getLocationProfiles().get("hasRewards").get("false"));
        assertEquals(Integer.valueOf(2), inventory.getLocationProfiles().get("repeatCycle").get("none"));
        assertEquals(Integer.valueOf(2), inventory.getLocationProfiles().get("repeatClaimGate").get("notRepeating"));
        String text = inventory.toText();
        String profile = text.substring(text.indexOf("LOCATION_PROFILE|"));
        assertTrue(profile.startsWith("LOCATION_PROFILE|tasks=2|uniqueStructures=1"));
        assertTrue(!text.contains("Secret Quest Name") && !text.contains("Secret Location Name"));
        assertTrue(!profile.contains("SecretStructureId") && !profile.contains("100"));
    }

    @Test public void profilesLocationRepeatScheduleAndClaimGateWithoutQuestIdentifiers() throws Exception {
        Path root = Files.createTempDirectory("bq-location-repeat-inventory");
        Path quests = Files.createDirectories(root.resolve("Quests/line"));
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":8,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Private Quest Name\","
            + "\"repeatTime:3\":120,\"repeat_relative:1\":0,\"autoClaim:1\":0}},"
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:location\","
            + "\"dimension:3\":55,\"biome:3\":-1,\"range:3\":0}},"
            + "\"rewards:9\":{\"0:10\":{\"index:3\":0,\"rewardID:8\":\"bq_standard:xp\","
            + "\"amount:3\":1}}}";
        Files.write(quests.resolve("repeat.json"), json.getBytes(StandardCharsets.UTF_8));

        BqBehaviorInventory inventory = BqBehaviorInventory.from(new BqMigrationPlanner().plan(root));
        assertEquals(Integer.valueOf(1), inventory.getLocationProfiles().get("repeatCycle").get("aligned-120"));
        assertEquals(Integer.valueOf(1), inventory.getLocationProfiles().get("autoClaim").get("false"));
        assertEquals(Integer.valueOf(1), inventory.getLocationProfiles().get("hasRewards").get("true"));
        assertEquals(Integer.valueOf(1), inventory.getLocationProfiles().get("repeatClaimGate").get("repeat-manualClaim"));
        String profile = inventory.toText().substring(inventory.toText().indexOf("LOCATION_PROFILE|"));
        assertTrue(profile.contains("repeatCycle={aligned-120=1}"));
        assertTrue(!profile.contains("Private Quest Name") && !profile.contains("repeat.json"));
    }

    @Test public void profilesFluidRequirementVolumesWithoutPrintingFluidIdentityOrTags() throws Exception {
        Path root = Files.createTempDirectory("bq-fluid-volume-inventory");
        Path quests = Files.createDirectories(root.resolve("Quests/line"));
        Files.createDirectories(root.resolve("QuestLines"));
        Files.write(root.resolve("QuestLinesOrder.txt"), new byte[0]);
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":91,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Private Fluid Quest\"}},"
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:fluid\","
            + "\"requiredFluids:9\":{\"0:10\":{\"FluidName:8\":\"private:fluid_a\","
            + "\"Amount:3\":250,\"Tag:10\":{\"secret:8\":\"secretA\"}},"
            + "\"1:10\":{\"FluidName:8\":\"private:fluid_b\",\"Amount:3\":750}}}},"
            + "\"rewards:9\":{}}";
        Files.write(quests.resolve("fluid.json"), json.getBytes(StandardCharsets.UTF_8));

        BqBehaviorInventory inventory = BqBehaviorInventory.from(new BqMigrationPlanner().plan(root));
        BqBehaviorInventory.TypeUsage fluid = inventory.getTasks().get("bq_standard:fluid");
        assertEquals(2, fluid.getFluidRequirements());
        assertEquals(Integer.valueOf(1), fluid.getFluidAmountProfiles().get("250"));
        assertEquals(Integer.valueOf(1), fluid.getFluidAmountProfiles().get("750"));
        String text = inventory.toText();
        assertTrue(text.contains("fluidRequirements=2|fluidAmountProfiles={250=1, 750=1}"));
        assertTrue(!text.contains("private:fluid_a") && !text.contains("private:fluid_b")
            && !text.contains("secretA") && !text.contains("Private Fluid Quest") && !text.contains("fluid.json"));
    }

    private static String encodeQuestId(long leastSignificantBits) {
        return Base64.getEncoder().encodeToString(ByteBuffer.allocate(16).putLong(0L)
            .putLong(leastSignificantBits).array());
    }

    private static String globalCheckboxQuest(long id, String name, boolean globalShare) {
        return "{\"questIDHigh:4\":0,\"questIDLow:4\":" + id
            + ",\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"" + name
            + "\",\"isGlobal:1\":1,\"globalShare:1\":" + (globalShare ? 1 : 0) + "}},"
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"}},"
            + "\"rewards:9\":{}}";
    }

    private static void write(Path path, int id, String task, String reward) throws Exception {
        write(path, id, task, reward, false);
    }

    private static void write(Path path, int id, String task, String reward, boolean includeConsume)
        throws Exception {
        String rewardOptions;
        if ("bq_standard:xp".equals(reward)) {
            rewardOptions = "\"amount:3\":3,\"ignoreDisabled:1\":1";
        } else if ("bq_standard:item".equals(reward)) {
            rewardOptions = "\"rewards:9\":{}";
        } else {
            rewardOptions = "\"ignoreDisabled:1\":1";
        }
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":" + id
            + ",\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Q\"}},"
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"" + task
            + "\"" + (includeConsume ? ",\"consume:1\":1" : "")
            + "}},\"rewards:9\":{\"0:10\":{\"index:3\":0,\"rewardID:8\":\""
            + reward + "\"," + rewardOptions + "}}}";
        Files.write(path, json.getBytes(StandardCharsets.UTF_8));
    }
}
