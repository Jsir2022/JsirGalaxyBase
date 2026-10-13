package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Base64;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.Assume;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jsirgalaxybase.quest.core.BqCompatibleTaskEvaluatorRegistry;
import com.jsirgalaxybase.quest.core.ConsumptionIntent;
import com.jsirgalaxybase.quest.core.ConsumptionRequest;
import com.jsirgalaxybase.quest.core.ConsumptionRequestAuthorizer;
import com.jsirgalaxybase.quest.core.GameplayFact;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.ParticipantMembership;
import com.jsirgalaxybase.quest.core.ParticipantMembershipResolver;
import com.jsirgalaxybase.quest.core.PortableNbt;
import com.jsirgalaxybase.quest.core.QuestCompletionQuery;
import com.jsirgalaxybase.quest.core.QuestConsumptionDetectPlan;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestDefinitionLifecycle;
import com.jsirgalaxybase.quest.core.QuestEngine;
import com.jsirgalaxybase.quest.core.QuestProgressSnapshot;
import com.jsirgalaxybase.quest.core.QuestRuntimeRepository;
import com.jsirgalaxybase.quest.core.QuestAssignmentPolicy;
import com.jsirgalaxybase.quest.core.RewardEntitlement;
import com.jsirgalaxybase.quest.core.ScopedQuestAssignmentPolicy;
import com.jsirgalaxybase.quest.core.StoredQuestDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.quest.core.TaskEvaluatorRegistry;
import com.jsirgalaxybase.quest.core.TaskProgress;

/**
 * Data-driven bridge between the anonymous fixed-DefaultQuests profile counts and the imported runtime task
 * definitions. The counts are refreshed by the read-only behaviorInventory task; no quest/player identifiers are
 * embedded here.
 */
public class BqRuntimeBehaviorProfileMatrixTest {
    private static final String RETRIEVAL = "bq_standard:retrieval";
    private static final String OPTIONAL_RETRIEVAL = "bq_standard:optional_retrieval";
    private static final String CRAFTING = "bq_standard:crafting";
    private static final String HUNT = "bq_standard:hunt";
    private static final String FLUID = "bq_standard:fluid";

    private final Gson gson = new Gson();
    private final BqStandardCodec codec = new BqStandardCodec(gson);
    private final TaskEvaluatorRegistry evaluators = BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch();

    @Test public void everyObservedRetrievalProfileRoutesInventoryVersusQuestDetectCorrectly() {
        Profile[] retrieval = {
            profile(1584, "autoConsume", false, "consume", false, "groupDetect", false,
                "ignoreNBT", false, "partialMatch", true),
            profile(2, "autoConsume", false, "consume", false, "groupDetect", false,
                "ignoreNBT", true, "partialMatch", false),
            profile(1966, "autoConsume", false, "consume", false, "groupDetect", false,
                "ignoreNBT", true, "partialMatch", true),
            profile(4, "autoConsume", false, "consume", false, "groupDetect", true,
                "ignoreNBT", true, "partialMatch", true),
            profile(250, "autoConsume", false, "consume", true, "groupDetect", false,
                "ignoreNBT", false, "partialMatch", true),
            profile(57, "autoConsume", false, "consume", true, "groupDetect", false,
                "ignoreNBT", true, "partialMatch", true)
        };
        assertProfileTotal(3863, retrieval);
        assertObservedRoutes(RETRIEVAL, retrieval);

        Profile[] optional = {
            profile(1, "autoConsume", false, "consume", false, "groupDetect", false,
                "ignoreNBT", false, "partialMatch", false),
            profile(577, "autoConsume", false, "consume", false, "groupDetect", false,
                "ignoreNBT", false, "partialMatch", true),
            profile(3, "autoConsume", false, "consume", false, "groupDetect", false,
                "ignoreNBT", true, "partialMatch", false),
            profile(686, "autoConsume", false, "consume", false, "groupDetect", false,
                "ignoreNBT", true, "partialMatch", true)
        };
        assertProfileTotal(1267, optional);
        assertObservedRoutes(OPTIONAL_RETRIEVAL, optional);
    }

    @Test public void everyObservedCraftingProfileHonoursEventChannelsAndStatisticsOptIn() {
        Profile[] profiles = {
            profile(81, "allowAnvil", false, "allowCraft", true, "allowCraftedFromStatistics", false,
                "allowSmelt", true, "ignoreNBT", false, "partialMatch", true),
            profile(46, "allowAnvil", false, "allowCraft", true, "allowCraftedFromStatistics", false,
                "allowSmelt", true, "ignoreNBT", true, "partialMatch", true),
            profile(6, "allowAnvil", false, "allowCraft", true, "allowCraftedFromStatistics", true,
                "allowSmelt", true, "ignoreNBT", false, "partialMatch", true)
        };
        assertProfileTotal(133, profiles);

        for (Profile profile : profiles) {
            TaskDefinition task = importedTask(CRAFTING, profile, false);
            assertProfileFlags(profile, task);
            assertTrue(evaluators.require(CRAFTING).evaluate(task, TaskProgress.empty(task),
                itemFact("craft")).isComplete());
            assertTrue(evaluators.require(CRAFTING).evaluate(task, TaskProgress.empty(task),
                itemFact("smelt")).isComplete());
            assertFalse("the pinned book disables anvil progress", evaluators.require(CRAFTING).evaluate(task,
                TaskProgress.empty(task), itemFact("anvil")).isComplete());

            TaskProgress statistics = evaluators.require(CRAFTING).evaluate(task, TaskProgress.empty(task),
                itemFact("craft-statistics"));
            assertEquals("only the six statistics-enabled occurrences may use the absolute stats path",
                Boolean.valueOf(profile.flag("allowCraftedFromStatistics")), Boolean.valueOf(statistics.isComplete()));
        }
    }

    @Test public void everyObservedHuntProfileHonoursSubtypeAndNbtFlags() {
        Profile[] profiles = {
            profile(2, "ignoreNBT", false, "subtypes", true),
            profile(1, "ignoreNBT", true, "subtypes", false),
            profile(101, "ignoreNBT", true, "subtypes", true)
        };
        assertProfileTotal(104, profiles);

        for (Profile profile : profiles) {
            TaskDefinition task = importedTask(HUNT, profile, false);
            assertProfileFlags(profile, task);
            GameplayFact aliasKill = fact("galaxy:entity_killed", map("subject", "SpecialZombie",
                "subjectAliases", "Zombie|Mob", "amount", "1", "entity.nbtPortable",
                portableNbt("CustomName", "Boss", "Health", "20")));
            boolean aliasExpected = profile.flag("subtypes");
            assertEquals("subtype alias behavior must follow the imported profile", Boolean.valueOf(aliasExpected),
                Boolean.valueOf(evaluators.require(HUNT).evaluate(task, TaskProgress.empty(task), aliasKill)
                    .isComplete()));

            if (!profile.flag("ignoreNBT")) {
                GameplayFact wrongNbt = fact("galaxy:entity_killed", map("subject", "Zombie",
                    "amount", "1", "entity.nbtPortable", portableNbt("CustomName", "Not Boss")));
                assertFalse(evaluators.require(HUNT).evaluate(task, TaskProgress.empty(task), wrongNbt).isComplete());
            }
        }
    }

    @Test public void everyObservedHuntRequiredCountMapsToItsRuntimeThreshold() {
        IntProfile[] profiles = {
            intProfile(35, 1), intProfile(21, 10), intProfile(1, 100), intProfile(4, 125),
            intProfile(4, 15), intProfile(20, 20), intProfile(5, 25), intProfile(2, 250),
            intProfile(3, 4), intProfile(1, 40), intProfile(3, 5), intProfile(2, 50),
            intProfile(2, 6), intProfile(1, 75)
        };
        assertIntProfileTotal(104, profiles);
        for (IntProfile profile : profiles) {
            JsonObject raw = taskJson(HUNT, profile(profile.occurrences, "ignoreNBT", true, "subtypes", true), false);
            raw.addProperty("required:3", profile.value);
            TaskDefinition task = importedTask(raw);
            assertEquals(Integer.toString(profile.value), task.getParameters().get("required"));
            assertEquals(Integer.toString(profile.value), task.getParameters().get("target"));
            TaskProgress progress = TaskProgress.empty(task);
            if (profile.value > 1) {
                progress = evaluators.require(HUNT).evaluate(task, progress, huntFact(profile.value - 1));
                assertFalse("the source profile's required count is a completion threshold", progress.isComplete());
            }
            progress = evaluators.require(HUNT).evaluate(task, progress, huntFact(1));
            assertTrue("the imported source-profile threshold must complete exactly at required",
                progress.isComplete());
        }
    }

    @Test public void everyObservedLocationDimensionProfileUsesTheImportedExactDimension() {
        IntProfile[] profiles = {
            intProfile(2, -1), intProfile(2, 1), intProfile(1, 112), intProfile(1, 227),
            intProfile(2, 28), intProfile(1, 29), intProfile(1, 36), intProfile(2, 41),
            intProfile(1, 42), intProfile(1, 47), intProfile(1, 49), intProfile(1, 50),
            intProfile(13, 55), intProfile(1, 56), intProfile(1, 60), intProfile(1, 64), intProfile(1, 7)
        };
        assertIntProfileTotal(33, profiles);
        for (IntProfile profile : profiles) {
            JsonObject raw = locationTaskJson(profile.value);
            TaskDefinition task = importedTask(raw);
            assertEquals(Integer.toString(profile.value), task.getParameters().get("dimension"));
            assertTrue(evaluators.require("bq_standard:location").evaluate(task, TaskProgress.empty(task),
                fact("galaxy:location_observed", map("dimension", Integer.toString(profile.value),
                    "biome", "0", "x", "0", "y", "0", "z", "0"))).isComplete());
            assertFalse("BQ compares the configured dimension literally; -1 is not a wildcard",
                evaluators.require("bq_standard:location").evaluate(task, TaskProgress.empty(task),
                    fact("galaxy:location_observed", map("dimension", Integer.toString(profile.value + 1),
                        "biome", "0", "x", "0", "y", "0", "z", "0"))).isComplete());
        }
    }

    @Test public void pinnedDefaultQuestsProfilesMatchTheExecutableMatrix() throws Exception {
        String source = System.getenv("BQ_TEST_DEFAULT_QUESTS");
        Assume.assumeTrue("read-only pinned DefaultQuests mount is required for source-profile audit",
            source != null && !source.trim().isEmpty());
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(Paths.get(source));
        assertEquals("9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc",
            manifest.getSourceHash());
        BqBehaviorInventory inventory = BqBehaviorInventory.from(manifest);
        inventory.requireRuntimeReady();

        assertEquals(3863, inventory.getTasks().get(RETRIEVAL).getCount());
        assertEquals(counts("autoConsume=false;consume=false;groupDetect=false;ignoreNBT=false;partialMatch=true", 1584,
                "autoConsume=false;consume=false;groupDetect=false;ignoreNBT=true;partialMatch=false", 2,
                "autoConsume=false;consume=false;groupDetect=false;ignoreNBT=true;partialMatch=true", 1966,
                "autoConsume=false;consume=false;groupDetect=true;ignoreNBT=true;partialMatch=true", 4,
                "autoConsume=false;consume=true;groupDetect=false;ignoreNBT=false;partialMatch=true", 250,
                "autoConsume=false;consume=true;groupDetect=false;ignoreNBT=true;partialMatch=true", 57),
            inventory.getTasks().get(RETRIEVAL).getBehaviorProfiles());
        assertEquals(1267, inventory.getTasks().get(OPTIONAL_RETRIEVAL).getCount());
        assertEquals(counts("autoConsume=false;consume=false;groupDetect=false;ignoreNBT=false;partialMatch=false", 1,
                "autoConsume=false;consume=false;groupDetect=false;ignoreNBT=false;partialMatch=true", 577,
                "autoConsume=false;consume=false;groupDetect=false;ignoreNBT=true;partialMatch=false", 3,
                "autoConsume=false;consume=false;groupDetect=false;ignoreNBT=true;partialMatch=true", 686),
            inventory.getTasks().get(OPTIONAL_RETRIEVAL).getBehaviorProfiles());
        assertEquals(counts("allowAnvil=false;allowCraft=true;allowCraftedFromStatistics=false;allowSmelt=true;ignoreNBT=false;partialMatch=true", 81,
                "allowAnvil=false;allowCraft=true;allowCraftedFromStatistics=false;allowSmelt=true;ignoreNBT=true;partialMatch=true", 46,
                "allowAnvil=false;allowCraft=true;allowCraftedFromStatistics=true;allowSmelt=true;ignoreNBT=false;partialMatch=true", 6),
            inventory.getTasks().get(CRAFTING).getBehaviorProfiles());
        assertEquals(frequencies("required", 1, 35, 10, 21, 100, 1, 125, 4, 15, 4, 20, 20, 25, 5,
            250, 2, 4, 3, 40, 1, 5, 3, 50, 2, 6, 2, 75, 1),
            inventory.getTasks().get(HUNT).getIntegerOptionProfiles());
        assertEquals(frequencies("dimension", -1, 2, 1, 2, 112, 1, 227, 1, 28, 2, 29, 1, 36, 1,
            41, 2, 42, 1, 47, 1, 49, 1, 50, 1, 55, 13, 56, 1, 60, 1, 64, 1, 7, 1,
            "biome", -1, 33, "posX", 0, 33, "posY", 0, 33, "posZ", 0, 33, "range", -1, 33),
            inventory.getTasks().get("bq_standard:location").getIntegerOptionProfiles());
        assertEquals(frequencies("amount", 1, 1, "range", 4, 1),
            inventory.getTasks().get("bq_standard:meeting").getIntegerOptionProfiles());
        assertEquals(valueFrequencies("1000", 6, "144", 8, "5000", 1, "576", 1),
            inventory.getTasks().get(FLUID).getFluidAmountProfiles());
        assertEquals(valueFrequencies("0", 1, "1", 1), inventory.getTasks().get("bq_standard:block_break")
            .getBlockAmountProfiles());
        assertEquals(valueFrequencies("-1", 1, "0", 1), inventory.getTasks().get("bq_standard:block_break")
            .getBlockMetaProfiles());
        assertEquals(counts("ignoreDisabled=false", 4181),
            inventory.getRewards().get("bq_standard:item").getBehaviorProfiles());
        assertEquals(counts("ignoreDisabled=false", 957),
            inventory.getRewards().get("bq_standard:choice").getBehaviorProfiles());
        assertEquals(counts("ignoreDisabled=true", 308),
            inventory.getRewards().get("bq_standard:questcompletion").getBehaviorProfiles());
        assertEquals(frequencies("amount", 1, 1, 10, 1),
            inventory.getRewards().get("bq_standard:xp").getIntegerOptionProfiles());

        assertItemAmountProfileCoversAllRequirements(inventory, RETRIEVAL, 5539);
        assertItemAmountProfileCoversAllRequirements(inventory, OPTIONAL_RETRIEVAL, 3274);
        assertItemAmountProfileCoversAllRequirements(inventory, CRAFTING, 179);
        assertEveryItemAmountProfileReachesItsImportedEvaluator(RETRIEVAL,
            profile(1, "autoConsume", false, "consume", false, "groupDetect", false,
                "ignoreNBT", true, "partialMatch", true), inventory);
        assertEveryItemAmountProfileReachesItsImportedEvaluator(OPTIONAL_RETRIEVAL,
            profile(1, "autoConsume", false, "consume", false, "groupDetect", false,
                "ignoreNBT", true, "partialMatch", true), inventory);
        assertEveryItemAmountProfileReachesItsImportedEvaluator(CRAFTING,
            profile(1, "allowAnvil", false, "allowCraft", true, "allowCraftedFromStatistics", false,
                "allowSmelt", true, "ignoreNBT", true, "partialMatch", true), inventory);
        assertPinnedConsumingRetrievalProfilesReachQuestDetectAuthorization(manifest);
    }

    private void assertPinnedConsumingRetrievalProfilesReachQuestDetectAuthorization(BqMigrationManifest manifest) {
        Map<String, QuestDefinition> representatives = new LinkedHashMap<String, QuestDefinition>();
        for (BqImportedQuest imported : manifest.getQuests()) {
            QuestDefinition definition = imported.getDefinition();
            for (TaskDefinition task : definition.getTasks()) {
                if (!RETRIEVAL.equals(task.getTypeId()) && !OPTIONAL_RETRIEVAL.equals(task.getTypeId())) continue;
                Map<String, String> parameters = task.getParameters();
                if (!"true".equals(parameters.get("consume")) || "true".equals(parameters.get("autoConsume"))) {
                    continue;
                }
                String profile = task.getTypeId() + ";groupDetect=" + parameters.get("groupDetect")
                    + ";ignoreNBT=" + parameters.get("ignoreNBT")
                    + ";partialMatch=" + parameters.get("partialMatch");
                if (!representatives.containsKey(profile)) representatives.put(profile, definition);
            }
        }
        assertEquals("the pinned source has two consuming Retrieval profiles, distinguished by NBT behavior",
            new java.util.LinkedHashSet<String>(Arrays.asList(
                RETRIEVAL + ";groupDetect=false;ignoreNBT=false;partialMatch=true",
                RETRIEVAL + ";groupDetect=false;ignoreNBT=true;partialMatch=true")),
            representatives.keySet());

        for (Map.Entry<String, QuestDefinition> entry : representatives.entrySet()) {
            final QuestDefinition definition = entry.getValue();
            final StoredQuestDefinition stored = new StoredQuestDefinition(definition,
                QuestDefinitionLifecycle.PUBLISHED, "pinned-source-profile", 1L);
            final UUID player = UUID.randomUUID();
            QuestRuntimeRepository emptyProgress = new EmptyRuntimeRepository();
            QuestCompletionQuery completedPrerequisites = participant -> definition.getPrerequisites();
            ParticipantMembershipResolver playerMembership = playerId ->
                Collections.singletonList(ParticipantMembership.player(playerId));
            QuestAssignmentPolicy assignment = new ScopedQuestAssignmentPolicy();
            ConsumptionRequestAuthorizer authorizer = new ConsumptionRequestAuthorizer(
                () -> Collections.singletonList(stored), emptyProgress, completedPrerequisites, new QuestEngine(),
                playerMembership, assignment);
            QuestConsumptionDetectPlan plan = authorizer.planQuestDetect(player, definition.getId(), 100L);
            assertEquals(ParticipantId.player(player), plan.getEvaluationRequest().getMembership().getParticipantId());
            assertTrue("the source-profile task must enter the server-derived Quest Detect list",
                plan.getTaskKeys().contains(taskKeyForProfile(definition, entry.getKey())));

            String taskKey = taskKeyForProfile(definition, entry.getKey());
            ConsumptionRequest request = authorizer.authorize(new ConsumptionIntent("profile:" + taskKey, "s2",
                player, definition.getId(), taskKey, 100L), 100L, ParticipantId.player(player),
                Integer.valueOf(plan.getCycle()));
            TaskDefinition task = taskByKey(definition, taskKey);
            int itemCount = Integer.parseInt(task.getParameters().get("item.count"));
            assertEquals("item", request.getKind());
            assertEquals(ParticipantId.player(player), request.getParticipantId());
            assertEquals(definition.getId(), request.getQuestId());
            assertEquals(definition.getVersion(), request.getDefinitionVersion());
            assertEquals(itemCount, request.getAmounts().size());
            for (int index = 0; index < itemCount; index++) {
                assertEquals(Long.valueOf(Long.parseLong(task.getParameters().get("item." + index + ".amount"))),
                    request.getAmounts().get(index));
            }
            assertEquals(task.getParameters().get("ignoreNBT"),
                request.getResourceParameters().get("ignoreNBT"));
            assertEquals(task.getParameters().get("partialMatch"),
                request.getResourceParameters().get("partialMatch"));
        }
    }

    private String taskKeyForProfile(QuestDefinition definition, String profile) {
        for (TaskDefinition task : definition.getTasks()) {
            Map<String, String> parameters = task.getParameters();
            if ((RETRIEVAL.equals(task.getTypeId()) || OPTIONAL_RETRIEVAL.equals(task.getTypeId()))
                && "true".equals(parameters.get("consume")) && !"true".equals(parameters.get("autoConsume"))) {
                String actual = task.getTypeId() + ";groupDetect=" + parameters.get("groupDetect")
                    + ";ignoreNBT=" + parameters.get("ignoreNBT")
                    + ";partialMatch=" + parameters.get("partialMatch");
                if (profile.equals(actual)) return task.getKey();
            }
        }
        throw new AssertionError("representative task not found for profile " + profile);
    }

    private TaskDefinition taskByKey(QuestDefinition definition, String key) {
        for (TaskDefinition task : definition.getTasks()) if (key.equals(task.getKey())) return task;
        throw new AssertionError("task not found: " + key);
    }

    private static final class EmptyRuntimeRepository implements QuestRuntimeRepository {
        @Override public java.util.Optional<QuestProgressSnapshot> findProgress(ParticipantId participantId,
            UUID questId, int definitionVersion) { return java.util.Optional.empty(); }
        @Override public boolean hasPendingConsumptionSubmission(ParticipantId participantId, UUID questId,
            int definitionVersion, int cycle) { return false; }
        @Override public boolean hasClaimableRewardEntitlement(ParticipantId participantId, UUID questId,
            int definitionVersion, int cycle) { return false; }
        @Override public long findRepeatCycleAnchorMillis(ParticipantId participantId, UUID questId,
            int definitionVersion, int cycle, long fallbackCompletedAt) { return fallbackCompletedAt; }
        @Override public boolean recordFactIfAbsent(GameplayFact fact) { return false; }
        @Override public void saveProgress(QuestProgressSnapshot progress) { }
        @Override public void insertEntitlementsIfAbsent(Set<RewardEntitlement> entitlements) { }
    }

    @Test public void observedMeetingAndBlockBreakProfilesImportIntoTheirRegisteredEvaluators() {
        JsonObject meetingRaw = new JsonObject();
        meetingRaw.addProperty("index:3", 0);
        meetingRaw.addProperty("taskID:8", "bq_standard:meeting");
        meetingRaw.addProperty("target:8", "Villager");
        meetingRaw.addProperty("amount:3", 1);
        meetingRaw.addProperty("range:3", 4);
        meetingRaw.addProperty("subtypes:1", 1);
        meetingRaw.addProperty("ignoreNBT:1", 1);
        TaskDefinition meeting = importedTask(meetingRaw);
        assertEquals("1", meeting.getParameters().get("amount"));
        assertEquals("4", meeting.getParameters().get("range"));
        assertTrue(evaluators.require("bq_standard:meeting").evaluate(meeting, TaskProgress.empty(meeting),
            fact("galaxy:nearby_entities_observed", map("observedRadius", "4", "nearbyEntity.count", "1",
                "nearbyEntity.0.subject", "CustomVillager", "nearbyEntity.0.subjectAliases", "Villager")))
            .isComplete());
        assertFalse(evaluators.require("bq_standard:meeting").evaluate(meeting, TaskProgress.empty(meeting),
            fact("galaxy:nearby_entities_observed", map("observedRadius", "3", "nearbyEntity.count", "1",
                "nearbyEntity.0.subject", "Villager"))).isComplete());

        JsonObject blockRaw = new JsonObject();
        blockRaw.addProperty("index:3", 0);
        blockRaw.addProperty("taskID:8", "bq_standard:block_break");
        JsonObject blocks = new JsonObject();
        JsonObject zeroBlock = new JsonObject();
        zeroBlock.addProperty("blockID:8", "minecraft:dirt");
        zeroBlock.addProperty("meta:3", -1);
        zeroBlock.addProperty("amount:3", 0);
        blocks.add("0:10", zeroBlock);
        JsonObject requiredBlock = new JsonObject();
        requiredBlock.addProperty("blockID:8", "minecraft:stone");
        requiredBlock.addProperty("meta:3", 0);
        requiredBlock.addProperty("amount:3", 1);
        blocks.add("1:10", requiredBlock);
        blockRaw.add("blocks:9", blocks);
        TaskDefinition blockBreak = importedTask(blockRaw);
        assertEquals("2", blockBreak.getParameters().get("block.count"));
        TaskProgress blockProgress = TaskProgress.empty(blockBreak);
        assertFalse("the zero-count source requirement is already satisfied but the positive one is not",
            blockProgress.isComplete());
        blockProgress = evaluators.require("bq_standard:block_break").evaluate(blockBreak, blockProgress,
            fact("galaxy:block_broken", map("registryName", "minecraft:dirt", "meta", "3", "amount", "1")));
        assertFalse("source metadata -1 is the block-break wildcard, amount zero contributes no target",
            blockProgress.isComplete());
        blockProgress = evaluators.require("bq_standard:block_break").evaluate(blockBreak, blockProgress,
            fact("galaxy:block_broken", map("registryName", "minecraft:stone", "meta", "0", "amount", "1")));
        assertTrue(blockProgress.isComplete());

        JsonObject checkboxRaw = new JsonObject();
        checkboxRaw.addProperty("index:3", 0);
        checkboxRaw.addProperty("taskID:8", "bq_standard:checkbox");
        TaskDefinition checkbox = importedTask(checkboxRaw);
        assertTrue(evaluators.require("bq_standard:checkbox").evaluate(checkbox, TaskProgress.empty(checkbox),
            fact("galaxy:task_confirmed", map("taskKey", checkbox.getKey()))).isComplete());
    }

    @Test public void everyObservedFluidProfileUsesInventorySnapshotsUnlessConsumptionWasCommitted() {
        Profile[] profiles = {
            profile(4, "autoConsume", false, "consume", false, "groupDetect", false, "ignoreNBT", false),
            profile(10, "autoConsume", false, "consume", false, "groupDetect", false, "ignoreNBT", true),
            profile(1, "autoConsume", false, "consume", true, "groupDetect", false, "ignoreNBT", false)
        };
        assertProfileTotal(15, profiles);

        for (Profile profile : profiles) {
            TaskDefinition task = importedTask(FLUID, profile, false);
            assertProfileFlags(profile, task);
            GameplayFact inventory = fluidInventoryFact(profile.flag("ignoreNBT")
                ? portableNbt("quality", "wrong") : portableNbt("quality", "2"));
            TaskProgress fromInventory = evaluators.require(FLUID).evaluate(task, TaskProgress.empty(task), inventory);
            if (profile.flag("consume")) {
                assertFalse("a consuming Fluid task cannot be credited by passive inventory observation",
                    fromInventory.isComplete());
                TaskProgress committed = evaluators.require(FLUID).evaluate(task, fromInventory,
                    fact("galaxy:consumption_applied", map("taskKey", task.getKey(), "kind", "fluid",
                        "value.count", "1", "value.0", "1000")));
                assertTrue("Quest Detect only credits the durable consumed-fluid result", committed.isComplete());
            } else {
                assertTrue("the non-consuming fluid branch is an inventory snapshot, not a craft event",
                    fromInventory.isComplete());
                GameplayFact wrongTag = fluidInventoryFact(portableNbt("quality", "wrong"));
                if (!profile.flag("ignoreNBT")) {
                    assertFalse(evaluators.require(FLUID).evaluate(task, TaskProgress.empty(task), wrongTag)
                        .isComplete());
                }
            }
        }
    }

    @Test public void everyObservedFluidRequirementAmountMapsToItsInventoryThreshold() {
        IntProfile[] profiles = { intProfile(6, 1000), intProfile(8, 144), intProfile(1, 5000),
            intProfile(1, 576) };
        assertIntProfileTotal(16, profiles);
        for (IntProfile profile : profiles) {
            JsonObject raw = taskJson(FLUID,
                profile(profile.occurrences, "autoConsume", false, "consume", false,
                    "groupDetect", false, "ignoreNBT", true), false);
            raw.getAsJsonObject("requiredFluids:9").getAsJsonObject("0:10")
                .addProperty("Amount:3", profile.value);
            TaskDefinition task = importedTask(raw);
            assertEquals(Integer.toString(profile.value), task.getParameters().get("fluid.0.amount"));
            assertEquals(Long.toString(profile.value), task.getParameters().get("target"));
            TaskProgress progress = evaluators.require(FLUID).evaluate(task, TaskProgress.empty(task),
                fluidInventoryFact(profile.value - 1, ""));
            assertFalse("fluid snapshot below the source-profile requirement must not complete",
                progress.isComplete());
            assertTrue(evaluators.require(FLUID).evaluate(task, progress,
                fluidInventoryFact(profile.value, "")).isComplete());
        }
    }

    @Test public void everyObservedRewardTypeAndOptionProfileMapsToItsBaseContract() {
        int occurrenceTotal = 0;
        occurrenceTotal += importReward("bq_standard:item", 4181, false, null);
        occurrenceTotal += importReward("bq_standard:choice", 957, false, "player-required");
        UUID completionTarget = new UUID(0L, 99L);
        String encodedTarget = Base64.getUrlEncoder().withoutPadding().encodeToString(
            ByteBuffer.allocate(16).putLong(completionTarget.getMostSignificantBits())
                .putLong(completionTarget.getLeastSignificantBits()).array());
        JsonObject completionRaw = rewardJson("bq_standard:questcompletion", true);
        completionRaw.addProperty("questID:8", encodedTarget);
        BqStandardRewardSpec completionSpec = codec.reward(completionRaw, 0);
        Map<String, String> completion = codec.rewardParameters(completionSpec, gson.toJson(completionRaw));
        assertEquals("true", completion.get("ignoreDisabled"));
        assertEquals(completionTarget.toString(), completion.get("questUuid"));
        occurrenceTotal += 308;

        for (IntProfile profile : new IntProfile[] { intProfile(1, 1), intProfile(1, 10) }) {
            JsonObject xpRaw = rewardJson("bq_standard:xp", false);
            xpRaw.addProperty("amount:3", profile.value);
            xpRaw.addProperty("isLevels:1", 1);
            BqStandardRewardSpec xpSpec = codec.reward(xpRaw, 0);
            Map<String, String> xp = codec.rewardParameters(xpSpec, gson.toJson(xpRaw));
            assertEquals("false", xp.get("ignoreDisabled"));
            assertEquals("true", xp.get("isLevels"));
            assertEquals("levels", xp.get("xpUnit"));
            assertEquals(Integer.toString(profile.value), xp.get("amount"));
            occurrenceTotal += 1;
        }
        assertEquals("the four allowlisted reward profiles cover every source occurrence", 5448,
            occurrenceTotal);
    }

    private void assertObservedRoutes(String type, Profile[] profiles) {
        for (Profile profile : profiles) {
            TaskDefinition task = importedTask(type, profile, false);
            assertProfileFlags(profile, task);
            GameplayFact snapshot = inventoryFact(profile.flag("ignoreNBT")
                ? portableNbt("grade", "wrong") : portableNbt("grade", "2", "extra", "kept"));
            TaskProgress progress = evaluators.require(type).evaluate(task, TaskProgress.empty(task), snapshot);
            if (profile.flag("consume")) {
                assertFalse("consume=true must not be completed by an inventory snapshot", progress.isComplete());
                progress = evaluators.require(type).evaluate(task, progress,
                    fact("galaxy:consumption_applied", map("taskKey", task.getKey(), "kind", "item",
                        "value.count", "1", "value.0", "2")));
                assertTrue("consuming Retrieval advances only after the Quest Detect commit", progress.isComplete());
            } else {
                assertTrue("non-consuming Retrieval is completed from the player inventory snapshot",
                    progress.isComplete());
                if (!profile.flag("ignoreNBT")) {
                    assertFalse("NBT-enabled profiles reject mismatched required NBT",
                        evaluators.require(type).evaluate(task, TaskProgress.empty(task),
                            inventoryFact(portableNbt("grade", "wrong"))).isComplete());
                }
            }
        }

        Profile groupDetect = null;
        for (Profile profile : profiles) if (profile.flag("groupDetect")) groupDetect = profile;
        if (groupDetect != null) {
            TaskDefinition grouped = importedTwoRequirementTask(type, groupDetect);
            TaskProgress previous = new TaskProgress(grouped.getKey(), longs(2L, 0L), longs(2L, 2L), false);
            TaskProgress refreshed = evaluators.require(type).evaluate(grouped, previous,
                fact("galaxy:inventory_observed", map("inventory.count", "0")));
            assertEquals("BQ groupDetect recalculates previously satisfied components from the fresh aggregate",
                longs(0L, 0L), refreshed.getValues());
        }
    }

    private TaskDefinition importedTask(JsonObject raw) {
        BqStandardTaskSpec spec = codec.task(raw, 0);
        return new TaskDefinition(spec.getKey(), spec.getTypeId(), BqStandardCodec.isOptionalTaskType(spec.getTypeId()),
            codec.taskParameters(spec, gson.toJson(raw)));
    }

    private JsonObject locationTaskJson(int dimension) {
        JsonObject task = new JsonObject();
        task.addProperty("index:3", 0);
        task.addProperty("taskID:8", "bq_standard:location");
        task.addProperty("name:8", "Location profile");
        task.addProperty("dimension:3", dimension);
        task.addProperty("biome:3", -1);
        task.addProperty("posX:3", 0);
        task.addProperty("posY:3", 0);
        task.addProperty("posZ:3", 0);
        task.addProperty("range:3", -1);
        task.addProperty("structure:8", "");
        task.addProperty("hideInfo:1", 0);
        task.addProperty("invert:1", 0);
        task.addProperty("taxiCabDist:1", 0);
        task.addProperty("visible:1", 0);
        return task;
    }

    private GameplayFact huntFact(int amount) {
        return fact("galaxy:entity_killed", map("subject", "Zombie", "amount", Integer.toString(amount)));
    }

    private int importReward(String type, int occurrences, boolean ignoreDisabled, String expectedSelectionMode) {
        JsonObject raw = rewardJson(type, ignoreDisabled);
        BqStandardRewardSpec spec = codec.reward(raw, 0);
        Map<String, String> parameters = codec.rewardParameters(spec, gson.toJson(raw));
        assertEquals(type, spec.getTypeId());
        assertEquals(Boolean.toString(ignoreDisabled), parameters.get("ignoreDisabled"));
        if (expectedSelectionMode != null) assertEquals(expectedSelectionMode, parameters.get("selectionMode"));
        return occurrences;
    }

    private JsonObject rewardJson(String type, boolean ignoreDisabled) {
        JsonObject reward = new JsonObject();
        reward.addProperty("index:3", 0);
        reward.addProperty("rewardID:8", type);
        reward.addProperty("ignoreDisabled:1", ignoreDisabled ? 1 : 0);
        if ("bq_standard:item".equals(type) || "bq_standard:choice".equals(type)) {
            String itemField = "bq_standard:choice".equals(type) ? "choices" : "rewards";
            JsonObject items = new JsonObject();
            JsonObject item = new JsonObject();
            item.addProperty("id:8", "minecraft:iron_ingot");
            item.addProperty("Damage:2", 0);
            item.addProperty("Count:3", 1);
            items.add("0:10", item);
            reward.add(itemField + ":9", items);
        }
        return reward;
    }

    private TaskDefinition importedTask(String type, Profile profile, boolean twoItems) {
        JsonObject raw = taskJson(type, profile, twoItems);
        BqStandardTaskSpec spec = codec.task(raw, 0);
        return new TaskDefinition(spec.getKey(), spec.getTypeId(), BqStandardCodec.isOptionalTaskType(type),
            codec.taskParameters(spec, gson.toJson(raw)));
    }

    private TaskDefinition importedTwoRequirementTask(String type, Profile profile) {
        JsonObject raw = taskJson(type, profile, true);
        BqStandardTaskSpec spec = codec.task(raw, 0);
        Map<String, String> parameters = codec.taskParameters(spec, gson.toJson(raw));
        return new TaskDefinition(spec.getKey(), spec.getTypeId(), false, parameters);
    }

    private JsonObject taskJson(String type, Profile profile, boolean twoItems) {
        JsonObject task = new JsonObject();
        task.addProperty("index:3", 0);
        task.addProperty("taskID:8", type);
        for (Map.Entry<String, Boolean> flag : profile.flags.entrySet()) {
            task.addProperty(flag.getKey() + ":1", flag.getValue().booleanValue() ? 1 : 0);
        }
        if (HUNT.equals(type)) {
            task.addProperty("target:8", "Zombie");
            task.addProperty("required:3", 1);
            JsonObject targetNbt = new JsonObject();
            targetNbt.addProperty("CustomName:8", "Boss");
            task.add("targetNBT:10", targetNbt);
        } else if (FLUID.equals(type)) {
            JsonObject fluids = new JsonObject();
            JsonObject fluid = new JsonObject();
            fluid.addProperty("FluidName:8", "water");
            fluid.addProperty("Amount:3", 1000);
            JsonObject nbt = new JsonObject();
            nbt.addProperty("quality:8", "2");
            fluid.add("Tag:10", nbt);
            fluids.add("0:10", fluid);
            task.add("requiredFluids:9", fluids);
        } else {
            int count = twoItems ? 2 : 1;
            JsonObject items = new JsonObject();
            for (int index = 0; index < count; index++) {
                JsonObject item = new JsonObject();
                item.addProperty("id:8", index == 0 ? "minecraft:iron_ingot" : "minecraft:gold_ingot");
                item.addProperty("Damage:2", 0);
                item.addProperty("Count:3", 2);
                JsonObject nbt = new JsonObject();
                nbt.addProperty("grade:8", "2");
                item.add("tag:10", nbt);
                items.add(index + ":10", item);
            }
            task.add("requiredItems:9", items);
        }
        return task;
    }

    private void assertProfileFlags(Profile profile, TaskDefinition task) {
        for (Map.Entry<String, Boolean> flag : profile.flags.entrySet()) {
            assertEquals("imported flag " + flag.getKey(), Boolean.toString(flag.getValue().booleanValue()),
                task.getParameters().get(flag.getKey()));
        }
        assertEquals("new-source task contract is personal", "false", task.getParameters().get("bq.partyShare"));
        assertTrue("profile count must be anchored to a real source occurrence", profile.occurrences > 0);
    }

    private GameplayFact itemFact(String channel) {
        return itemFact(channel, 2);
    }

    private GameplayFact itemFact(String channel, int amount) {
        return fact("galaxy:item_produced", map("registryName", "minecraft:iron_ingot", "meta", "0",
            "amount", Integer.toString(amount), "channel", channel, "nbtPortable",
            portableNbt("grade", "2", "extra", "kept")));
    }

    private GameplayFact inventoryFact(String nbt) {
        return inventoryFact(2, nbt);
    }

    private GameplayFact inventoryFact(int amount, String nbt) {
        return fact("galaxy:inventory_observed", map("inventory.count", "1",
            "inventory.0.registryName", "minecraft:iron_ingot", "inventory.0.meta", "0",
            "inventory.0.amount", Integer.toString(amount), "inventory.0.nbtPortable", nbt));
    }

    private GameplayFact fluidInventoryFact(String nbt) {
        return fluidInventoryFact(1000, nbt);
    }

    private GameplayFact fluidInventoryFact(int amount, String nbt) {
        return fact("galaxy:fluid_inventory_observed", map("fluidInventory.count", "1",
            "fluidInventory.0.name", "water", "fluidInventory.0.amount", Integer.toString(amount),
            "fluidInventory.0.nbtPresent", "true", "fluidInventory.0.nbtPortable", nbt));
    }

    private GameplayFact fact(String type, Map<String, String> attributes) {
        return new GameplayFact("profile-matrix", UUID.randomUUID().toString(), UUID.randomUUID(), type, 1L,
            attributes);
    }

    private static Profile profile(int occurrences, Object... flags) {
        Map<String, Boolean> values = new LinkedHashMap<String, Boolean>();
        for (int index = 0; index < flags.length; index += 2) {
            values.put((String) flags[index], (Boolean) flags[index + 1]);
        }
        return new Profile(occurrences, values);
    }

    private static IntProfile intProfile(int occurrences, int value) {
        return new IntProfile(occurrences, value);
    }

    private static void assertProfileTotal(int expected, Profile[] profiles) {
        int actual = 0;
        for (Profile profile : profiles) actual += profile.occurrences;
        assertEquals(expected, actual);
    }

    private static void assertIntProfileTotal(int expected, IntProfile[] profiles) {
        int actual = 0;
        for (IntProfile profile : profiles) actual += profile.occurrences;
        assertEquals(expected, actual);
    }

    private static Map<String, Integer> counts(Object... values) {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (int index = 0; index < values.length; index += 2) {
            result.put((String) values[index], Integer.valueOf(((Number) values[index + 1]).intValue()));
        }
        return result;
    }

    private static Map<String, Integer> valueFrequencies(Object... values) {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(String.valueOf(values[index]), Integer.valueOf(((Number) values[index + 1]).intValue()));
        }
        return result;
    }

    private static Map<String, Map<String, Integer>> frequencies(Object... values) {
        Map<String, Map<String, Integer>> result = new LinkedHashMap<String, Map<String, Integer>>();
        String current = null;
        for (int index = 0; index < values.length;) {
            if (values[index] instanceof String) {
                current = (String) values[index++];
                if (index < values.length && values[index] instanceof Number) continue;
                Map<String, Integer> profile = new LinkedHashMap<String, Integer>();
                result.put(current, profile);
                continue;
            }
            if (current == null) throw new IllegalArgumentException("profile values must begin with a key");
            Object value = values[index++];
            Object occurrences = values[index++];
            Map<String, Integer> profile = result.get(current);
            if (profile == null) {
                profile = new LinkedHashMap<String, Integer>();
                result.put(current, profile);
            }
            profile.put(String.valueOf(value), Integer.valueOf(((Number) occurrences).intValue()));
        }
        return result;
    }

    private void assertEveryItemAmountProfileReachesItsImportedEvaluator(String type, Profile flags,
        BqBehaviorInventory inventory) {
        for (Map.Entry<String, Integer> entry : inventory.getTasks().get(type).getItemAmountProfiles().entrySet()) {
            int required = Integer.parseInt(entry.getKey());
            assertTrue("pinned item requirement amounts must be positive", required > 0);
            JsonObject raw = taskJson(type, flags, false);
            raw.getAsJsonObject("requiredItems:9").getAsJsonObject("0:10")
                .addProperty("Count:3", required);
            TaskDefinition task = importedTask(raw);
            assertEquals(Integer.toString(required), task.getParameters().get("item.0.amount"));
            TaskProgress progress = TaskProgress.empty(task);
            if (required > 1) {
                GameplayFact belowThreshold = RETRIEVAL.equals(type) || OPTIONAL_RETRIEVAL.equals(type)
                    ? inventoryFact(required - 1, portableNbt("grade", "2", "extra", "kept"))
                    : itemFact("craft", required - 1);
                progress = evaluators.require(type).evaluate(task, progress, belowThreshold);
                assertFalse(type + " amount " + required + " must not complete below its source threshold",
                    progress.isComplete());
            }
            GameplayFact exactThreshold = RETRIEVAL.equals(type) || OPTIONAL_RETRIEVAL.equals(type)
                ? inventoryFact(required, portableNbt("grade", "2", "extra", "kept"))
                : itemFact("craft", required);
            assertTrue(type + " amount " + required + " must complete at its source threshold",
                evaluators.require(type).evaluate(task, progress, exactThreshold).isComplete());
        }
    }

    private static void assertItemAmountProfileCoversAllRequirements(BqBehaviorInventory inventory, String type,
        int expectedRequirements) {
        BqBehaviorInventory.TypeUsage usage = inventory.getTasks().get(type);
        int profiled = 0;
        for (Integer count : usage.getItemAmountProfiles().values()) profiled += count.intValue();
        assertEquals(expectedRequirements, profiled);
        assertTrue("source contains numeric item amount values for " + type,
            !usage.getItemAmountProfiles().isEmpty());
    }

    private static Map<String, String> map(String... values) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int index = 0; index < values.length; index += 2) result.put(values[index], values[index + 1]);
        return result;
    }

    private static String portableNbt(String... values) {
        Map<String, PortableNbt> fields = new LinkedHashMap<String, PortableNbt>();
        for (int index = 0; index < values.length; index += 2) {
            fields.put(values[index], PortableNbt.string(values[index + 1]));
        }
        return PortableNbt.compound(fields).encode();
    }

    private static List<Long> longs(long first, long second) {
        List<Long> values = new ArrayList<Long>();
        values.add(Long.valueOf(first));
        values.add(Long.valueOf(second));
        return values;
    }

    private static final class Profile {
        private final int occurrences;
        private final Map<String, Boolean> flags;

        private Profile(int occurrences, Map<String, Boolean> flags) {
            this.occurrences = occurrences;
            this.flags = flags;
        }

        private boolean flag(String key) {
            Boolean value = flags.get(key);
            return value != null && value.booleanValue();
        }
    }

    private static final class IntProfile {
        private final int occurrences;
        private final int value;

        private IntProfile(int occurrences, int value) {
            this.occurrences = occurrences;
            this.value = value;
        }
    }
}
