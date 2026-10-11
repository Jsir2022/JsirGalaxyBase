package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.sql.DataSource;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Assume;
import org.junit.Test;

import com.jsirgalaxybase.quest.core.AuthenticatedQuestClaimService;
import com.jsirgalaxybase.quest.core.AuthenticatedRewardChoiceService;
import com.jsirgalaxybase.quest.core.AuthenticatedCraftingStatisticsDetectionService;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestConsumptionDetectService;
import com.jsirgalaxybase.quest.core.BqPartyActiveQuestQuery;
import com.jsirgalaxybase.quest.core.ConsumptionApplyResult;
import com.jsirgalaxybase.quest.core.ConsumptionDetectBatchResult;
import com.jsirgalaxybase.quest.core.ConsumptionIntent;
import com.jsirgalaxybase.quest.core.ConsumptionPort;
import com.jsirgalaxybase.quest.core.ConsumptionRequest;
import com.jsirgalaxybase.quest.core.ConsumptionRequestAuthorizer;
import com.jsirgalaxybase.quest.core.ConsumptionSelection;
import com.jsirgalaxybase.quest.core.ConsumptionSubmissionService;
import com.jsirgalaxybase.quest.core.ConsumptionSubmissionStatus;
import com.jsirgalaxybase.quest.core.GameplayFact;
import com.jsirgalaxybase.quest.core.BqCompatibleTaskEvaluatorRegistry;
import com.jsirgalaxybase.quest.core.BqHuntPartyAssignmentPolicy;
import com.jsirgalaxybase.quest.core.FluidObservationAttributeCodec;
import com.jsirgalaxybase.quest.core.FluidObservationEntry;
import com.jsirgalaxybase.quest.core.InventoryObservationAttributeCodec;
import com.jsirgalaxybase.quest.core.InventoryObservationEntry;
import com.jsirgalaxybase.quest.core.FactProcessingStatus;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.ParticipantMembership;
import com.jsirgalaxybase.quest.core.ParticipantMemberRole;
import com.jsirgalaxybase.quest.core.ParticipantMembershipResolver;
import com.jsirgalaxybase.quest.core.ParticipantMutationStatus;
import com.jsirgalaxybase.quest.core.QuestBehavior;
import com.jsirgalaxybase.quest.core.QuestClock;
import com.jsirgalaxybase.quest.core.QuestCompletionRewardDeliveryHandler;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestDefinitionLifecycle;
import com.jsirgalaxybase.quest.core.QuestEngine;
import com.jsirgalaxybase.quest.core.QuestEvaluationPlanner;
import com.jsirgalaxybase.quest.core.QuestEvaluationRequestProvider;
import com.jsirgalaxybase.quest.core.QuestFactProjector;
import com.jsirgalaxybase.quest.core.QuestLogic;
import com.jsirgalaxybase.quest.core.QuestObservationPlan;
import com.jsirgalaxybase.quest.core.QuestObservationPlanProvider;
import com.jsirgalaxybase.quest.core.QuestOnlinePlayerDirectory;
import com.jsirgalaxybase.quest.core.QuestProgressSnapshot;
import com.jsirgalaxybase.quest.core.QuestRuntimeService;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.QuestTransaction;
import com.jsirgalaxybase.quest.core.RewardClaimStatus;
import com.jsirgalaxybase.quest.core.RewardChoiceSelectionStatus;
import com.jsirgalaxybase.quest.core.RewardDeliveryBatchResult;
import com.jsirgalaxybase.quest.core.RewardDeliveryLease;
import com.jsirgalaxybase.quest.core.RewardDeliveryRouter;
import com.jsirgalaxybase.quest.core.RewardDeliveryWorker;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.RepeatPolicy;
import com.jsirgalaxybase.quest.core.StoredQuestDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.quest.bqimport.BqQuestDefinitionImporter;
import com.jsirgalaxybase.quest.postgres.JdbcBqPartyInventorySnapshotRepository;
import com.jsirgalaxybase.quest.postgres.JdbcConsumptionSubmissionRepository;
import com.jsirgalaxybase.quest.postgres.JdbcConsumptionDetectBatchRepository;
import com.jsirgalaxybase.quest.postgres.JdbcParticipantAdministrationRepository;
import com.jsirgalaxybase.quest.postgres.JdbcParticipantMembershipResolver;
import com.jsirgalaxybase.quest.postgres.JdbcQuestCompletionQuery;
import com.jsirgalaxybase.quest.postgres.JdbcQuestCompletionRewardPort;
import com.jsirgalaxybase.quest.postgres.JdbcQuestConnectionManager;
import com.jsirgalaxybase.quest.postgres.JdbcQuestDefinitionRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestRuntimeRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestTransaction;
import com.jsirgalaxybase.quest.postgres.JdbcRewardClaimRepository;
import com.jsirgalaxybase.quest.postgres.JdbcRewardChoiceSelectionRepository;
import com.jsirgalaxybase.quest.postgres.JdbcRewardDeliveryRepository;
import com.jsirgalaxybase.quest.postgres.QuestInventorySnapshotLease;

/** PostgreSQL-only integration coverage for the production BQ PARTY inventory producer. */
public class BqPartyInventorySnapshotFactSinkPostgresTest {

    @Test
    public void aggregatesOnlyFreshDatabaseLeasesAndPublishesThePartyFact() throws Exception {
        Fixture fixture = Fixture.open();
        try {
            UUID peerSession = UUID.randomUUID();
            UUID staleSession = UUID.randomUUID();
            UUID ownerSession = UUID.randomUUID();
            assertTrue(fixture.sink(fixture.recordingSink).beginInventorySession(
                fixture.inventory(fixture.peer, peerSession, 3L)));
            assertTrue(fixture.sink(fixture.recordingSink).beginInventorySession(
                fixture.inventory(fixture.staleMember, staleSession, 900L)));
            fixture.expireLease(fixture.staleMember);
            assertTrue(fixture.sink(fixture.recordingSink).beginInventorySession(
                fixture.inventory(fixture.owner, ownerSession, 4L)));

            GameplayFact aggregate = null;
            for (GameplayFact fact : fixture.recordingSink.facts) {
                if ("true".equals(fact.getAttributes().get("inventory.bqPartySnapshot"))) aggregate = fact;
            }
            assertNotNull("the second member snapshot should publish a database-backed PARTY aggregate", aggregate);
            assertEquals(fixture.party.getId().toString(), aggregate.getAttributes().get("inventory.bqPartyId"));
            assertEquals(fixture.owner, aggregate.getPlayerId());
            assertEquals("1", aggregate.getAttributes().get("inventory.count"));
            assertEquals("minecraft:iron_ingot", aggregate.getAttributes().get("inventory.0.registryName"));
            assertEquals("7", aggregate.getAttributes().get("inventory.0.amount"));

            Map<UUID, QuestInventorySnapshotLease> active = fixture.transaction.inTransaction(() ->
                fixture.snapshots.findActive(Arrays.asList(fixture.owner, fixture.peer, fixture.staleMember)));
            assertEquals("only fresh member sessions should be represented by active leases", 2, active.size());
            assertEquals(ownerSession, active.get(fixture.owner).getSessionId());
            assertEquals(peerSession, active.get(fixture.peer).getSessionId());
            assertFalse("expired member inventory must not contribute to the party aggregate",
                active.containsKey(fixture.staleMember));
        } finally {
            fixture.close();
        }
    }

    @Test
    public void importedNonConsumingFluidUsesFreshPartyLeasesAndReplacesIncompleteProgress() throws Exception {
        Fixture fixture = Fixture.open();
        try {
            Map<String, String> parameters = new LinkedHashMap<String, String>();
            parameters.put("factType", "galaxy:fluid_inventory_observed");
            parameters.put("consume", "false");
            parameters.put("groupDetect", "false");
            parameters.put("ignoreNBT", "true");
            parameters.put("progressMode", "fluid-vector");
            parameters.put("target", "1000");
            parameters.put("fluid.count", "1");
            parameters.put("fluid.0.name", "water");
            parameters.put("fluid.0.amount", "1000");
            parameters.put("bq.raw", "{}");
            TaskDefinition fluid = new TaskDefinition("fluid", "bq_standard:fluid", false, parameters);
            QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "BQ PARTY Fluid Integration", "",
                QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(fluid),
                Collections.<RewardDefinition>emptyList(), RepeatPolicy.never(),
                QuestBehavior.builder().participantScope(com.jsirgalaxybase.quest.core.QuestParticipantScope.PLAYER)
                    .build());
            JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(fixture.manager);
            StoredQuestDefinition draft = fixture.transaction.inTransaction(() -> definitions.createDraft(quest));
            assertTrue(fixture.transaction.inTransaction(() -> definitions.publish(quest.getId(), quest.getVersion(),
                draft.getContentHash(), 1000L)));

            JdbcParticipantMembershipResolver memberships = new JdbcParticipantMembershipResolver(fixture.manager);
            QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions::findAllPublished,
                new JdbcQuestCompletionQuery(fixture.manager), memberships, new BqHuntPartyAssignmentPolicy(),
                (definition, fact, partyMembership) -> true);
            JdbcQuestRuntimeRepository runtimeRepository = new JdbcQuestRuntimeRepository(fixture.manager);
            QuestRuntimeService runtime = new QuestRuntimeService(runtimeRepository, fixture.transaction,
                new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
            QuestFactSink runtimeSink = fact -> runtime.apply(fact, planner, 4000L);
            BqPartyInventorySnapshotFactSink producer = fixture.sink(runtimeSink);

            UUID staleSession = UUID.randomUUID();
            fixture.transaction.inTransaction(() -> {
                fixture.snapshots.claimSession(fixture.staleMember, "s2", staleSession,
                    fluidAttributes(2000L), BqPartyInventorySnapshotFactSink.INVENTORY_LEASE_MILLIS);
                return null;
            });
            fixture.expireLease(fixture.staleMember);

            UUID peerSession = UUID.randomUUID();
            UUID ownerSession = UUID.randomUUID();
            assertTrue(producer.beginInventorySession(fixture.fluidInventory(fixture.peer, peerSession, 400L)));
            assertEquals(Arrays.asList(400L), progress(fixture, runtimeRepository, quest, fixture.owner)
                .getTasks().get("fluid").getValues());
            assertEquals(Arrays.asList(400L), progress(fixture, runtimeRepository, quest, fixture.peer)
                .getTasks().get("fluid").getValues());
            assertEquals(Arrays.asList(400L), progress(fixture, runtimeRepository, quest, fixture.staleMember)
                .getTasks().get("fluid").getValues());

            assertTrue(producer.beginInventorySession(fixture.fluidInventory(fixture.owner, ownerSession, 500L)));
            assertEquals(Arrays.asList(900L), progress(fixture, runtimeRepository, quest, fixture.owner)
                .getTasks().get("fluid").getValues());
            assertEquals(QuestStatus.IN_PROGRESS,
                progress(fixture, runtimeRepository, quest, fixture.peer).getStatus());

            producer.accept(fixture.fluidInventory(fixture.owner, ownerSession, 300L));
            assertEquals("a fresh snapshot replaces incomplete fluid progress from current active inventories",
                Arrays.asList(700L), progress(fixture, runtimeRepository, quest, fixture.owner)
                    .getTasks().get("fluid").getValues());
            assertEquals(Arrays.asList(700L), progress(fixture, runtimeRepository, quest, fixture.peer)
                .getTasks().get("fluid").getValues());
            assertEquals("expired party members must not contribute their prior fluid lease",
                QuestStatus.IN_PROGRESS, progress(fixture, runtimeRepository, quest, fixture.staleMember).getStatus());

            producer.accept(fixture.fluidInventory(fixture.peer, peerSession, 700L));
            for (UUID member : Arrays.asList(fixture.owner, fixture.peer, fixture.staleMember)) {
                QuestProgressSnapshot completed = progress(fixture, runtimeRepository, quest, member);
                assertEquals(Arrays.asList(1000L), completed.getTasks().get("fluid").getValues());
                assertEquals(QuestStatus.COMPLETED, completed.getStatus());
            }
        } finally {
            fixture.close();
        }
    }

    private static Map<String, String> fluidAttributes(long amount) {
        return new LinkedHashMap<String, String>(new FluidObservationAttributeCodec().encode(
            Collections.singletonList(new FluidObservationEntry("water", amount, "", "", false))));
    }

    @Test
    public void failedFactAcceptanceRollsBackTheSessionLeaseInTheSameDatabaseTransaction() throws Exception {
        final Fixture fixture = Fixture.open();
        try {
            QuestFactSink failingSink = new QuestFactSink() {
                @Override public void accept(GameplayFact fact) {
                    throw new IllegalStateException("simulated downstream fact failure");
                }
            };
            try {
                fixture.sink(failingSink).beginInventorySession(fixture.inventory(fixture.owner,
                    UUID.randomUUID(), 5L));
                fail("expected the downstream failure to abort the inventory transaction");
            } catch (IllegalStateException expected) {
                assertEquals("simulated downstream fact failure", expected.getMessage());
            }

            Map<UUID, QuestInventorySnapshotLease> active = fixture.transaction.inTransaction(() ->
                fixture.snapshots.findActive(Collections.singletonList(fixture.owner)));
            assertFalse("a rolled-back login must not leave a phantom online inventory", active.containsKey(fixture.owner));
        } finally {
            fixture.close();
        }
    }

    @Test
    public void freshPartySnapshotsCompletePersonalRetrievalAndDeliverPersonalBqItemAndChoiceRewards()
        throws Exception {
        Fixture fixture = Fixture.open();
        try {
            Map<String, String> parameters = new LinkedHashMap<String, String>();
            parameters.put("factType", "galaxy:inventory_observed");
            parameters.put("consume", "false");
            parameters.put("groupDetect", "false");
            parameters.put("progressMode", "item-vector");
            parameters.put("item.count", "1");
            parameters.put("item.0.registryName", "minecraft:iron_ingot");
            parameters.put("item.0.amount", "7");
            parameters.put("bq.raw", "{}");
            TaskDefinition retrieval = new TaskDefinition("retrieve", "bq_standard:retrieval", false, parameters);
            Map<String, String> optionalParameters = new LinkedHashMap<String, String>(parameters);
            optionalParameters.put("item.0.registryName", "minecraft:gold_ingot");
            optionalParameters.put("item.0.amount", "5");
            TaskDefinition optionalRetrieval = new TaskDefinition("bonus", "bq_standard:optional_retrieval",
                true, optionalParameters);
            UUID questId = UUID.randomUUID();
            List<RewardDefinition> importedRewards = importedItemAndChoiceRewards(questId);
            RewardDefinition itemReward = importedRewards.get(0);
            RewardDefinition choiceReward = importedRewards.get(1);
            QuestDefinition quest = new QuestDefinition(questId, 1, "BQ PARTY Retrieval Integration", "",
                QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(),
                Arrays.asList(retrieval, optionalRetrieval),
                Arrays.asList(itemReward, choiceReward), RepeatPolicy.never(),
                QuestBehavior.builder().participantScope(com.jsirgalaxybase.quest.core.QuestParticipantScope.PLAYER)
                    .build());
            JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(fixture.manager);
            StoredQuestDefinition draft = fixture.transaction.inTransaction(() -> definitions.createDraft(quest));
            assertTrue(fixture.transaction.inTransaction(() -> definitions.publish(quest.getId(), quest.getVersion(),
                draft.getContentHash(), 1000L)));
            JdbcParticipantMembershipResolver memberships = new JdbcParticipantMembershipResolver(fixture.manager);
            QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions::findAllPublished,
                new JdbcQuestCompletionQuery(fixture.manager), memberships, new BqHuntPartyAssignmentPolicy(),
                (definition, fact, partyMembership) -> true);
            JdbcQuestRuntimeRepository runtimeRepository = new JdbcQuestRuntimeRepository(fixture.manager);
            QuestRuntimeService runtime = new QuestRuntimeService(runtimeRepository, fixture.transaction,
                new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
            final List<FactProcessingStatus> outcomes = new ArrayList<FactProcessingStatus>();
            QuestFactSink runtimeSink = new QuestFactSink() {
                @Override public void accept(GameplayFact fact) {
                    outcomes.add(runtime.apply(fact, planner, 4000L).getStatus());
                }
            };
            BqPartyInventorySnapshotFactSink producer = fixture.sink(runtimeSink);

            UUID peerSession = UUID.randomUUID();
            assertTrue(producer.beginInventorySession(fixture.inventory(fixture.peer, peerSession, 3L)));
            QuestProgressSnapshot partialOwnerProgress = progress(fixture, runtimeRepository, quest, fixture.owner);
            assertEquals("the first live inventory observation should persist partial progress for all members",
                Arrays.asList(3L), partialOwnerProgress.getTasks().get("retrieve").getValues());
            assertEquals("the optional BQ retrieval remains visible as zero progress when its item is absent",
                Arrays.asList(0L), partialOwnerProgress.getTasks().get("bonus").getValues());
            assertFalse(partialOwnerProgress.getTasks().get("bonus").isComplete());
            assertEquals(QuestStatus.IN_PROGRESS,
                progress(fixture, runtimeRepository, quest, fixture.peer).getStatus());
            assertEquals(QuestStatus.IN_PROGRESS,
                progress(fixture, runtimeRepository, quest, fixture.staleMember).getStatus());

            UUID ownerSession = UUID.randomUUID();
            GameplayFact ownerSnapshot = fixture.inventory(fixture.owner, ownerSession, 4L);
            assertTrue(producer.beginInventorySession(ownerSnapshot));

            for (UUID player : Arrays.asList(fixture.owner, fixture.peer, fixture.staleMember)) {
                QuestProgressSnapshot progress = progress(fixture, runtimeRepository, quest, player);
                assertEquals(QuestStatus.COMPLETED, progress.getStatus());
                assertEquals(Arrays.asList(7L), progress.getTasks().get("retrieve").getValues());
                assertEquals("BQ optional retrieval progress is retained without blocking quest completion",
                    Arrays.asList(0L), progress.getTasks().get("bonus").getValues());
                assertFalse(progress.getTasks().get("bonus").isComplete());
            }
            assertEquals("the producer must persist two independent aggregate facts",
                2, count(outcomes, FactProcessingStatus.ACCEPTED));

            GameplayFact postCompletionSnapshot = fixture.inventory(fixture.owner, ownerSession,
                ownerSession + "-post-completion", Arrays.asList(
                    new InventoryObservationEntry("minecraft:iron_ingot", 0, 4L, "ingotIron", "", "", false),
                    new InventoryObservationEntry("minecraft:gold_ingot", 0, 5L, "ingotGold", "", "", false)));
            producer.accept(postCompletionSnapshot);
            assertEquals("a later PARTY snapshot is accepted but must not advance the completed Quest cycle",
                FactProcessingStatus.ACCEPTED, outcomes.get(outcomes.size() - 1));
            for (UUID player : Arrays.asList(fixture.owner, fixture.peer, fixture.staleMember)) {
                QuestProgressSnapshot progress = progress(fixture, runtimeRepository, quest, player);
                assertEquals(QuestStatus.COMPLETED, progress.getStatus());
                assertEquals("optional Retrieval stays frozen after the required objective completes",
                    Arrays.asList(0L), progress.getTasks().get("bonus").getValues());
                assertFalse(progress.getTasks().get("bonus").isComplete());
            }
            Set<UUID> expectedRecipients = new LinkedHashSet<UUID>(Arrays.asList(
                fixture.owner, fixture.peer, fixture.staleMember));
            assertItemRewardEntitlements(fixture, quest, expectedRecipients);
            assertChoiceRewardEntitlements(fixture, quest, expectedRecipients);

            JdbcRewardChoiceSelectionRepository choiceSelections =
                new JdbcRewardChoiceSelectionRepository(fixture.manager);
            AuthenticatedRewardChoiceService choiceService = new AuthenticatedRewardChoiceService(definitions,
                runtimeRepository, choiceSelections, fixture.transaction);
            Map<UUID, Integer> selectedChoices = new LinkedHashMap<UUID, Integer>();
            selectedChoices.put(fixture.owner, 0);
            selectedChoices.put(fixture.peer, 1);
            selectedChoices.put(fixture.staleMember, 2);
            for (Map.Entry<UUID, Integer> selection : selectedChoices.entrySet()) {
                assertEquals("choice selection is scoped to the authenticated player's entitlement",
                    RewardChoiceSelectionStatus.SELECTED, choiceService.select(selection.getKey(), quest.getId(),
                        "reward-1", selection.getValue(), 4900L));
                assertEquals("replaying the same selection is idempotent", RewardChoiceSelectionStatus.ALREADY_SELECTED,
                    choiceService.select(selection.getKey(), quest.getId(), "reward-1", selection.getValue(), 4901L));
            }

            AuthenticatedQuestClaimService claims = new AuthenticatedQuestClaimService(definitions,
                runtimeRepository, new JdbcRewardClaimRepository(fixture.manager), fixture.transaction,
                new com.jsirgalaxybase.quest.core.RewardSuppressionPolicy(false), choiceSelections);
            for (UUID player : expectedRecipients) {
                assertEquals("only the authenticated reward recipient may claim their personal entitlement",
                    RewardClaimStatus.CLAIMED, claims.claim(player, quest.getId(), 5000L));
                assertEquals("claim replay must not queue the same entitlement again", RewardClaimStatus.ALREADY_CLAIMED,
                    claims.claim(player, quest.getId(), 5001L));
            }

            final Map<UUID, FakePlayerState> playerStates = new LinkedHashMap<UUID, FakePlayerState>();
            for (UUID player : expectedRecipients) playerStates.put(player, new FakePlayerState("quest-test", 4));
            final Item diamond = new Item().setHasSubtypes(true);
            final Item emerald = new Item();
            final Item goldIngot = new Item();
            final Map<String, Item> items = new LinkedHashMap<String, Item>();
            items.put("minecraft:diamond", diamond);
            items.put("minecraft:emerald", emerald);
            items.put("minecraft:gold_ingot", goldIngot);
            MinecraftItemRewardDeliveryHandler.ItemLookup itemLookup = items::get;
            MinecraftItemRewardDeliveryHandler.PlayerLookup playerLookup = playerStates::get;
            MinecraftItemRewardDeliveryHandler itemDelivery = MinecraftItemRewardDeliveryHandler.forTest(
                "bq_standard:item", playerLookup, itemLookup);
            MinecraftItemRewardDeliveryHandler choiceDelivery = MinecraftItemRewardDeliveryHandler.forTest(
                "bq_standard:choice", playerLookup, itemLookup);
            RewardDeliveryWorker deliveryWorker = new RewardDeliveryWorker(
                new JdbcRewardDeliveryRepository(fixture.manager), fixture.transaction,
                new RewardDeliveryRouter(Arrays.asList(itemDelivery, choiceDelivery)),
                new QuestClock() {
                    @Override public long currentTimeMillis() { return 6000L; }
                }, 30000L, 1000L, 3);
            RewardDeliveryBatchResult firstDelivery = deliveryWorker.runOnce("party-item-reward-it", 10);
            assertEquals(6, firstDelivery.getLeased());
            assertEquals(6, firstDelivery.getDelivered());
            assertEquals(0, firstDelivery.getRetryScheduled());
            assertEquals(0, firstDelivery.getStaleConfirmations());
            for (Map.Entry<UUID, FakePlayerState> recipient : playerStates.entrySet()) {
                FakePlayerState player = recipient.getValue();
                Item selectedItem = selectedChoices.get(recipient.getKey()) == 0 ? emerald
                    : selectedChoices.get(recipient.getKey()) == 1 ? diamond : goldIngot;
                int expectedChoiceAmount = selectedChoices.get(recipient.getKey()) == 0 ? 5
                    : selectedChoices.get(recipient.getKey()) == 1 ? 3 : 4;
                assertEquals("each player's claimed BQ item stack reaches the Minecraft delivery adapter",
                    2 + (selectedItem == diamond ? expectedChoiceAmount : 0), countItem(player.inventory, diamond));
                assertEquals("the selected choice only is materialized for this recipient", expectedChoiceAmount,
                    countItem(player.inventory, selectedItem, selectedItem == diamond ? 4 : 0));
                if (selectedItem == diamond) {
                    boolean foundBoundChoiceStack = false;
                    for (ItemStack stack : player.inventory) {
                        if (stack == null || stack.getItem() != diamond || stack.getItemDamage() != 4) continue;
                        assertEquals(recipient.getKey().toString(), stack.getTagCompound().getString("boundTo"));
                        foundBoundChoiceStack = true;
                    }
                    assertTrue("the imported choice NBT must survive JDBC publication and delivery",
                        foundBoundChoiceStack);
                }
                assertEquals(2, player.flushCount);
            }
            assertMinecraftRewardMarkers(fixture, quest, playerStates);
            assertDeliveredItemEntitlements(fixture, quest);
            assertDeliveredChoiceEntitlements(fixture, quest, selectedChoices);

            RewardDeliveryBatchResult replayDelivery = deliveryWorker.runOnce("party-item-reward-it-replay", 10);
            assertEquals(0, replayDelivery.getLeased());
            for (Map.Entry<UUID, FakePlayerState> recipient : playerStates.entrySet()) {
                FakePlayerState player = recipient.getValue();
                int expectedDiamonds = selectedChoices.get(recipient.getKey()) == 1 ? 5 : 2;
                assertEquals(expectedDiamonds, countItem(player.inventory, diamond));
                assertEquals("a delivered entitlement must not flush or insert the reward twice", 2,
                    player.flushCount);
            }

            producer.accept(ownerSnapshot);
            assertEquals("replayed inventory session must not apply the same party fact twice",
                FactProcessingStatus.DUPLICATE, outcomes.get(outcomes.size() - 1));
            assertDeliveredItemEntitlements(fixture, quest);
            assertDeliveredChoiceEntitlements(fixture, quest, selectedChoices);
        } finally {
            fixture.close();
        }
    }

    @Test
    public void importedBqQuestCompletionRewardDecodesTargetAndCompletesItOnceInJdbc() throws Exception {
        Fixture fixture = Fixture.open();
        try {
            UUID recipient = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            Map<String, String> taskParameters = new LinkedHashMap<String, String>();
            taskParameters.put("target", "3");
            taskParameters.put("factType", "minecraft:kill");
            QuestDefinition target = new QuestDefinition(targetId, 1, "Quest completion target", "",
                QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(),
                Collections.singletonList(new TaskDefinition("target-task", "count", false, taskParameters)),
                Collections.singletonList(new RewardDefinition("target-reward", "bq_standard:item",
                    Collections.<String, String>emptyMap())), RepeatPolicy.never(),
                QuestBehavior.builder().participantScope(com.jsirgalaxybase.quest.core.QuestParticipantScope.PLAYER)
                    .build());
            JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(fixture.manager);
            StoredQuestDefinition draft = fixture.transaction.inTransaction(() -> definitions.createDraft(target));
            assertTrue(fixture.transaction.inTransaction(() -> definitions.publish(targetId, target.getVersion(),
                draft.getContentHash(), 1000L)));

            RewardDefinition importedReward = importedQuestCompletionReward(targetId);
            assertEquals("bq_standard:questcompletion", importedReward.getTypeId());
            assertEquals(targetId.toString(), importedReward.getParameters().get("questUuid"));
            assertEquals("true", importedReward.getParameters().get("ignoreDisabled"));
            JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(fixture.manager);
            QuestCompletionRewardDeliveryHandler handler = new QuestCompletionRewardDeliveryHandler(
                new JdbcQuestCompletionRewardPort(definitions, runtime, fixture.transaction), () -> 2400L);
            RewardDeliveryLease lease = new RewardDeliveryLease("imported-questcompletion-entitlement",
                ParticipantId.player(recipient), importedReward, 1, "test-worker", 10000L);

            assertTrue("imported compact BQ questID must complete its published target",
                handler.deliver(lease).isDelivered());
            assertTrue("completion replay must be an idempotent delivery success", handler.deliver(lease).isDelivered());

            QuestProgressSnapshot progress = runtime.findProgress(ParticipantId.player(recipient), targetId, 1).get();
            assertEquals(QuestStatus.COMPLETED, progress.getStatus());
            assertEquals(2400L, progress.getCompletedAt());
            assertTrue(progress.getTasks().get("target-task").isComplete());
            try (Connection connection = fixture.scoped.getConnection(); PreparedStatement statement = connection
                .prepareStatement("SELECT COUNT(*) FROM galaxy_quest_reward_entitlement WHERE quest_id=? "
                    + "AND definition_version=1 AND reward_key='target-reward'")) {
                statement.setObject(1, targetId);
                try (java.sql.ResultSet row = statement.executeQuery()) {
                    assertTrue(row.next());
                    assertEquals("the target reward entitlement must be created only once", 1L, row.getLong(1));
                }
            }
        } finally {
            fixture.close();
        }
    }

    @Test
    public void importedBqQuestCompletionRewardFlowsFromSourceClaimThroughJdbcWorker() throws Exception {
        Fixture fixture = Fixture.open();
        Path sourceFile = null;
        Path targetFile = null;
        try {
            UUID recipient = fixture.owner;
            UUID sourceId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            sourceFile = Files.createTempFile("bq-questcompletion-source-full-import", ".json");
            targetFile = Files.createTempFile("bq-questcompletion-target-full-import", ".json");
            byte[] targetBytes = ByteBuffer.allocate(16).putLong(targetId.getMostSignificantBits())
                .putLong(targetId.getLeastSignificantBits()).array();
            String compactTargetId = Base64.getUrlEncoder().withoutPadding().encodeToString(targetBytes);
            String targetJson = "{\"questIDHigh:4\":" + targetId.getMostSignificantBits()
                + ",\"questIDLow:4\":" + targetId.getLeastSignificantBits()
                + ",\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Imported target\"}},"
                + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"}},"
                + "\"rewards:9\":{\"0:10\":{\"index:3\":0,\"rewardID:8\":\"bq_standard:item\","
                + "\"rewards:9\":{\"0:10\":{\"id:8\":\"minecraft:diamond\",\"Damage:2\":0,"
                + "\"Count:3\":2}}}}}";
            String sourceJson = "{\"questIDHigh:4\":" + sourceId.getMostSignificantBits()
                + ",\"questIDLow:4\":" + sourceId.getLeastSignificantBits()
                + ",\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Imported source\"}},"
                + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:retrieval\","
                + "\"consume:1\":0,\"autoConsume:1\":0,\"ignoreNBT:1\":1,\"partialMatch:1\":1,"
                + "\"requiredItems:9\":{\"0:10\":{\"id:8\":\"minecraft:iron_ingot\","
                + "\"Damage:2\":0,\"Count:3\":1}}}},\"rewards:9\":{\"0:10\":{\"index:3\":0,"
                + "\"rewardID:8\":\"bq_standard:questcompletion\",\"questID:8\":\""
                + compactTargetId + "\"}}}";
            Files.write(targetFile, targetJson.getBytes(StandardCharsets.UTF_8));
            Files.write(sourceFile, sourceJson.getBytes(StandardCharsets.UTF_8));
            BqQuestDefinitionImporter importer = new BqQuestDefinitionImporter();
            QuestDefinition target = importer.read(targetFile).getDefinition();
            QuestDefinition source = importer.read(sourceFile).getDefinition();
            assertEquals(targetId, target.getId());
            assertEquals("bq_standard:checkbox", target.getTasks().get(0).getTypeId());
            assertEquals("bq_standard:item", target.getRewards().get(0).getTypeId());
            assertEquals("bq_standard:retrieval", source.getTasks().get(0).getTypeId());
            assertEquals("minecraft:iron_ingot", source.getTasks().get(0).getParameters()
                .get("item.0.registryName"));
            assertEquals("1", source.getTasks().get(0).getParameters().get("item.0.amount"));
            assertEquals("bq_standard:questcompletion", source.getRewards().get(0).getTypeId());
            assertEquals(targetId.toString(), source.getRewards().get(0).getParameters().get("questUuid"));
            assertEquals("true", source.getRewards().get(0).getParameters().get("ignoreDisabled"));
            assertFalse(source.getBehavior().isAutoClaim());

            JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(fixture.manager);
            publish(fixture, definitions, target);
            publish(fixture, definitions, source);
            JdbcQuestRuntimeRepository runtimeRepository = new JdbcQuestRuntimeRepository(fixture.manager);
            QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions::findAllPublished,
                new JdbcQuestCompletionQuery(fixture.manager), new JdbcParticipantMembershipResolver(fixture.manager),
                new BqHuntPartyAssignmentPolicy(), (definition, fact, partyMembership) -> true);
            QuestRuntimeService runtime = new QuestRuntimeService(runtimeRepository, fixture.transaction,
                new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
            List<FactProcessingStatus> sourceOutcomes = new ArrayList<FactProcessingStatus>();
            BqPartyInventorySnapshotFactSink producer = fixture.sink(fact ->
                sourceOutcomes.add(runtime.apply(fact, planner, 1500L).getStatus()));
            assertTrue(producer.beginInventorySession(fixture.inventory(recipient, UUID.randomUUID(), 1L)));
            assertEquals(1, count(sourceOutcomes, FactProcessingStatus.ACCEPTED));
            QuestProgressSnapshot sourceProgress = runtimeRepository.findProgress(
                ParticipantId.player(recipient), sourceId, 1).get();
            assertEquals("imported retrieval progress=" + sourceProgress.getTasks().get("task-0").getValues()
                    + " target=" + sourceProgress.getTasks().get("task-0").getTargets(),
                QuestStatus.COMPLETED, sourceProgress.getStatus());

            JdbcRewardClaimRepository claims = new JdbcRewardClaimRepository(fixture.manager);
            assertEquals(RewardClaimStatus.CLAIMED, fixture.transaction.inTransaction(() ->
                claims.claim(sourceId, 1, 0, recipient, 2000L)));

            QuestClock clock = () -> 2400L;
            QuestCompletionRewardDeliveryHandler completion = new QuestCompletionRewardDeliveryHandler(
                new JdbcQuestCompletionRewardPort(definitions, runtimeRepository, fixture.transaction), clock);
            RewardDeliveryWorker worker = new RewardDeliveryWorker(
                new JdbcRewardDeliveryRepository(fixture.manager), fixture.transaction,
                new RewardDeliveryRouter(Collections.<com.jsirgalaxybase.quest.core.RewardDeliveryHandler>singletonList(
                    completion)), clock, 10000L, 1000L, 3);
            RewardDeliveryBatchResult first = worker.runOnce("test-questcompletion-worker", 10);
            assertEquals(1, first.getLeased());
            assertEquals(1, first.getDelivered());
            assertEquals(0, first.getRetryScheduled());

            QuestProgressSnapshot targetProgress = runtimeRepository.findProgress(
                ParticipantId.player(recipient), targetId, 1).get();
            assertEquals(QuestStatus.COMPLETED, targetProgress.getStatus());
            assertEquals(2400L, targetProgress.getCompletedAt());
            assertTrue(targetProgress.getTasks().get("task-0").isComplete());
            assertEquals("a delivered source entitlement must not be leased again",
                0, worker.runOnce("test-questcompletion-worker", 10).getLeased());
            assertEquals(1L, countRewardEntitlements(fixture, targetId, "reward-0"));
        } finally {
            try {
                if (sourceFile != null) Files.deleteIfExists(sourceFile);
                if (targetFile != null) Files.deleteIfExists(targetFile);
            } finally {
                fixture.close();
            }
        }
    }

    @Test
    public void importedBqRetrievalRemainsPersonalThroughPartyInventoryObservation() throws Exception {
        Fixture fixture = Fixture.open();
        Path source = null;
        try {
            source = Files.createTempFile("bq-retrieval-jdbc-it", ".json");
            String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":18,"
                + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Imported Retrieval\","
                + "\"questLogic:8\":\"AND\",\"taskLogic:8\":\"AND\"}},"
                + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:retrieval\","
                + "\"consume:1\":0,\"autoConsume:1\":0,\"ignoreNBT:1\":1,\"partialMatch:1\":1,"
                + "\"requiredItems:9\":{\"0:10\":{\"id:8\":\"minecraft:iron_ingot\","
                + "\"Damage:2\":0,\"Count:3\":7}}}},\"rewards:9\":{}}";
            Files.write(source, json.getBytes(StandardCharsets.UTF_8));
            QuestDefinition quest = new BqQuestDefinitionImporter().read(source).getDefinition();
            assertEquals(com.jsirgalaxybase.quest.core.QuestParticipantScope.PLAYER,
                quest.getBehavior().getParticipantScope());
            assertEquals("bq_standard:retrieval", quest.getTasks().get(0).getTypeId());
            assertEquals("false", quest.getTasks().get(0).getParameters().get("consume"));

            JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(fixture.manager);
            StoredQuestDefinition draft = fixture.transaction.inTransaction(() -> definitions.createDraft(quest));
            assertTrue(fixture.transaction.inTransaction(() -> definitions.publish(quest.getId(), quest.getVersion(),
                draft.getContentHash(), 1000L)));
            JdbcParticipantMembershipResolver memberships = new JdbcParticipantMembershipResolver(fixture.manager);
            QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions::findAllPublished,
                new JdbcQuestCompletionQuery(fixture.manager), memberships, new BqHuntPartyAssignmentPolicy(),
                (definition, fact, partyMembership) -> true);
            JdbcQuestRuntimeRepository runtimeRepository = new JdbcQuestRuntimeRepository(fixture.manager);
            QuestRuntimeService runtime = new QuestRuntimeService(runtimeRepository, fixture.transaction,
                new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
            final List<FactProcessingStatus> outcomes = new ArrayList<FactProcessingStatus>();
            BqPartyInventorySnapshotFactSink producer = fixture.sink(fact ->
                outcomes.add(runtime.apply(fact, planner, 4000L).getStatus()));

            UUID peerSession = UUID.randomUUID();
            assertTrue(producer.beginInventorySession(fixture.inventory(fixture.peer, peerSession, 3L)));
            QuestProgressSnapshot peerProgress = progress(fixture, runtimeRepository, quest, fixture.peer);
            assertEquals(QuestStatus.IN_PROGRESS, peerProgress.getStatus());
            assertEquals(Arrays.asList(3L), peerProgress.getTasks().get("task-0").getValues());
            assertFalse(runtimeRepository.findProgress(ParticipantId.player(fixture.owner), quest.getId(), 1).isPresent());
            assertFalse(runtimeRepository.findProgress(ParticipantId.player(fixture.staleMember), quest.getId(), 1).isPresent());

            UUID ownerSession = UUID.randomUUID();
            assertTrue(producer.beginInventorySession(fixture.inventory(fixture.owner, ownerSession, 4L)));
            QuestProgressSnapshot ownerProgress = progress(fixture, runtimeRepository, quest, fixture.owner);
            assertEquals(QuestStatus.IN_PROGRESS, ownerProgress.getStatus());
            assertEquals(Arrays.asList(4L), ownerProgress.getTasks().get("task-0").getValues());
            assertEquals(Arrays.asList(3L), progress(fixture, runtimeRepository, quest, fixture.peer)
                .getTasks().get("task-0").getValues());
            assertFalse(runtimeRepository.findProgress(ParticipantId.player(fixture.staleMember), quest.getId(), 1).isPresent());
            assertEquals(2, count(outcomes, FactProcessingStatus.ACCEPTED));
        } finally {
            if (source != null) Files.deleteIfExists(source);
            fixture.close();
        }
    }

    @Test
    public void importedCraftingOutputRefreshesSameTaskStatisticsAndPersistsAbsoluteProgress() throws Exception {
        Fixture fixture = Fixture.open();
        Path source = null;
        try {
            source = Files.createTempFile("bq-crafting-statistics-jdbc-it", ".json");
            String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":26,"
                + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Imported Crafting Statistics\","
                + "\"questLogic:8\":\"AND\",\"taskLogic:8\":\"AND\"}},"
                + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:crafting\","
                + "\"allowCraft:1\":1,\"allowSmelt:1\":1,\"allowAnvil:1\":1,"
                + "\"allowCraftedFromStatistics:1\":1,\"ignoreNBT:1\":1,\"partialMatch:1\":1,"
                + "\"requiredItems:9\":{"
                + "\"0:10\":{\"id:8\":\"minecraft:iron_ingot\",\"Damage:2\":0,\"Count:3\":4},"
                + "\"1:10\":{\"id:8\":\"minecraft:gold_ingot\",\"Damage:2\":0,\"Count:3\":5}}}},"
                + "\"rewards:9\":{}}";
            Files.write(source, json.getBytes(StandardCharsets.UTF_8));
            QuestDefinition quest = new BqQuestDefinitionImporter().read(source).getDefinition();
            TaskDefinition task = quest.getTasks().get(0);
            assertEquals("bq_standard:crafting", task.getTypeId());
            assertEquals("true", task.getParameters().get("allowCraftedFromStatistics"));
            assertEquals("2", task.getParameters().get("item.count"));

            JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(fixture.manager);
            StoredQuestDefinition draft = fixture.transaction.inTransaction(() -> definitions.createDraft(quest));
            assertTrue(fixture.transaction.inTransaction(() -> definitions.publish(quest.getId(), quest.getVersion(),
                draft.getContentHash(), 1000L)));
            List<StoredQuestDefinition> published = definitions.findAllPublished();
            QuestObservationPlan observations = QuestObservationPlan.fromPublished(published);
            Map<String, String> triggerAttributes = new LinkedHashMap<String, String>();
            triggerAttributes.put("registryName", "minecraft:iron_ingot");
            triggerAttributes.put("meta", "0");
            triggerAttributes.put("damageable", "false");
            triggerAttributes.put("amount", "1");
            triggerAttributes.put("channel", "craft");
            triggerAttributes.put("oreDictionary", "");
            List<com.jsirgalaxybase.quest.core.QuestCraftingStatisticObservation> statisticTargets =
                observations.craftingStatisticsForOutput(triggerAttributes);
            assertEquals("matching one target refreshes every statistic target in that Crafting task", 2,
                statisticTargets.size());

            ParticipantMembershipResolver soloMemberships = playerId ->
                Collections.singletonList(ParticipantMembership.player(playerId));
            QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions::findAllPublished,
                new JdbcQuestCompletionQuery(fixture.manager), soloMemberships, new BqHuntPartyAssignmentPolicy());
            JdbcQuestRuntimeRepository runtimeRepository = new JdbcQuestRuntimeRepository(fixture.manager);
            QuestRuntimeService runtime = new QuestRuntimeService(runtimeRepository, fixture.transaction,
                new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
            final List<FactProcessingStatus> outcomes = new ArrayList<FactProcessingStatus>();
            BqPartyInventorySnapshotFactSink producer = fixture.sink(fact ->
                outcomes.add(runtime.apply(fact, planner, 4000L).getStatus()));

            producer.accept(new GameplayFact("s2", "crafting-output-iron", fixture.owner,
                "galaxy:item_produced", 1000L, triggerAttributes));
            assertEquals(Arrays.asList(1L, 0L), progress(fixture, runtimeRepository, quest, fixture.owner)
                .getTasks().get(task.getKey()).getValues());

            GameplayFact goldStatistics = null;
            for (com.jsirgalaxybase.quest.core.QuestCraftingStatisticObservation target : statisticTargets) {
                Map<String, String> attributes = new LinkedHashMap<String, String>();
                attributes.put("registryName", target.getRegistryName());
                attributes.put("meta", Integer.toString(target.getMeta()));
                attributes.put("damageable", "false");
                attributes.put("amount", "minecraft:iron_ingot".equals(target.getRegistryName()) ? "4" : "3");
                attributes.put("channel", "craft-statistics");
                attributes.put("trigger.channel", "craft");
                attributes.put("trigger.registryName", "minecraft:iron_ingot");
                attributes.put("trigger.meta", "0");
                attributes.put("trigger.damageable", "false");
                attributes.put("trigger.oreDictionary", "");
                GameplayFact statistic = new GameplayFact("s2", "crafting-statistics-" + target.getRegistryName(),
                    fixture.owner, "galaxy:item_produced", 1001L, attributes);
                if ("minecraft:gold_ingot".equals(target.getRegistryName())) goldStatistics = statistic;
                producer.accept(statistic);
            }

            QuestProgressSnapshot partial = progress(fixture, runtimeRepository, quest, fixture.owner);
            assertEquals(QuestStatus.IN_PROGRESS, partial.getStatus());
            assertEquals("statistics facts are absolute lifetime counts", Arrays.asList(4L, 3L),
                partial.getTasks().get(task.getKey()).getValues());
            assertNotNull(goldStatistics);
            producer.accept(goldStatistics);
            assertEquals("replayed statistics keep the same source event idempotent", FactProcessingStatus.DUPLICATE,
                outcomes.get(outcomes.size() - 1));

            Map<String, String> completedGoldAttributes = new LinkedHashMap<String, String>(
                goldStatistics.getAttributes());
            completedGoldAttributes.put("amount", "5");
            producer.accept(new GameplayFact("s2", "crafting-statistics-gold-refresh", fixture.owner,
                "galaxy:item_produced", 1002L, completedGoldAttributes));
            QuestProgressSnapshot completed = progress(fixture, runtimeRepository, quest, fixture.owner);
            assertEquals(QuestStatus.COMPLETED, completed.getStatus());
            assertEquals(Arrays.asList(4L, 5L), completed.getTasks().get(task.getKey()).getValues());
            assertEquals(4, count(outcomes, FactProcessingStatus.ACCEPTED));
            assertEquals(1, count(outcomes, FactProcessingStatus.DUPLICATE));
        } finally {
            if (source != null) Files.deleteIfExists(source);
            fixture.close();
        }
    }

    @Test
    public void automaticBqCraftingStatisticsKeepPersonalCountersAndSumPerServerMaxima() throws Exception {
        Fixture fixture = Fixture.open();
        try {
            Map<String, String> parameters = new LinkedHashMap<String, String>();
            parameters.put("factType", "galaxy:item_produced");
            parameters.put("progressMode", "item-vector");
            parameters.put("item.count", "1");
            parameters.put("item.0.registryName", "minecraft:iron_ingot");
            parameters.put("item.0.meta", "0");
            parameters.put("item.0.amount", "20");
            parameters.put("allowCraft", "true");
            parameters.put("allowSmelt", "true");
            parameters.put("allowAnvil", "false");
            parameters.put("allowCraftedFromStatistics", "true");
            parameters.put("bq.raw", "{}");
            TaskDefinition task = new TaskDefinition("craft", "bq_standard:crafting", false, parameters);
            QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Automatic BQ Crafting", "",
                QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task),
                Collections.<RewardDefinition>emptyList(), RepeatPolicy.never(),
                QuestBehavior.builder().participantScope(com.jsirgalaxybase.quest.core.QuestParticipantScope.PLAYER)
                    .build());

            JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(fixture.manager);
            StoredQuestDefinition draft = fixture.transaction.inTransaction(() -> definitions.createDraft(quest));
            assertTrue(fixture.transaction.inTransaction(() -> definitions.publish(quest.getId(), quest.getVersion(),
                draft.getContentHash(), 1000L)));
            JdbcQuestRuntimeRepository progress = new JdbcQuestRuntimeRepository(fixture.manager);
            ParticipantMembershipResolver memberships = new JdbcParticipantMembershipResolver(fixture.manager);
            QuestOnlinePlayerDirectory onlinePlayers = playerId -> fixture.owner.equals(playerId)
                || fixture.peer.equals(playerId);
            JdbcQuestCompletionQuery completions = new JdbcQuestCompletionQuery(fixture.manager);
            BqPartyActiveQuestQuery partyActivity = new BqPartyActiveQuestQuery("s2", completions, progress,
                onlinePlayers);
            QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions::findAllPublished,
                new JdbcQuestCompletionQuery(fixture.manager), memberships, new BqHuntPartyAssignmentPolicy(),
                partyActivity);
            QuestRuntimeService runtime = new QuestRuntimeService(progress, fixture.transaction,
                new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
            AuthenticatedCraftingStatisticsDetectionService detector =
                new AuthenticatedCraftingStatisticsDetectionService(definitions, progress, runtime, planner,
                    (playerId, registryName) -> java.util.OptionalLong.of(
                        fixture.owner.equals(playerId) ? 5L : 2L), "s2");

            List<com.jsirgalaxybase.quest.core.FactProcessingResult> ownerResults = detector.detectAutomatically(
                fixture.owner, quest.getId(), task.getKey(), 4000L);
            assertEquals(1, ownerResults.size());
            assertEquals(FactProcessingStatus.ACCEPTED, ownerResults.get(0).getStatus());
            assertEquals(Arrays.asList(5L), progress(fixture, progress, quest, fixture.owner)
                .getTasks().get("craft").getValues());
            assertFalse("an automatic snapshot is personal, not a PARTY progress fan-out",
                progress.findProgress(ParticipantId.player(fixture.peer), quest.getId(), quest.getVersion())
                    .isPresent());

            List<com.jsirgalaxybase.quest.core.FactProcessingResult> replay = detector.detectAutomatically(
                fixture.owner, quest.getId(), task.getKey(), 4060L);
            assertEquals(FactProcessingStatus.DUPLICATE, replay.get(0).getStatus());

            // Each source keeps its own absolute lifetime maximum; the player's per-source totals are summed.
            long[] secondServerCount = new long[] { 2L };
            BqPartyActiveQuestQuery secondServerActivity = new BqPartyActiveQuestQuery("lobby", completions,
                progress, onlinePlayers);
            QuestEvaluationPlanner secondServerPlanner = new QuestEvaluationPlanner(
                definitions::findAllPublished, completions, memberships, new BqHuntPartyAssignmentPolicy(),
                secondServerActivity);
            AuthenticatedCraftingStatisticsDetectionService secondServerDetector =
                new AuthenticatedCraftingStatisticsDetectionService(definitions, progress, runtime,
                    secondServerPlanner, (playerId, registryName) -> java.util.OptionalLong.of(secondServerCount[0]),
                    "lobby");
            List<com.jsirgalaxybase.quest.core.FactProcessingResult> lowerServerSnapshot =
                secondServerDetector.detectAutomatically(fixture.owner, quest.getId(), task.getKey(), 4070L);
            assertEquals(1, lowerServerSnapshot.size());
            assertEquals(FactProcessingStatus.ACCEPTED, lowerServerSnapshot.get(0).getStatus());
            assertEquals("s2=5 plus lobby=2 is 7; stale values are maxed only within their source",
                Arrays.asList(7L), progress(fixture, progress, quest, fixture.owner)
                    .getTasks().get("craft").getValues());

            secondServerCount[0] = 7L;
            List<com.jsirgalaxybase.quest.core.FactProcessingResult> higherServerSnapshot =
                secondServerDetector.detectAutomatically(fixture.owner, quest.getId(), task.getKey(), 4080L);
            assertEquals(FactProcessingStatus.ACCEPTED, higherServerSnapshot.get(0).getStatus());
            assertEquals(Arrays.asList(12L), progress(fixture, progress, quest, fixture.owner)
                .getTasks().get("craft").getValues());
            assertEquals(FactProcessingStatus.DUPLICATE, secondServerDetector.detectAutomatically(fixture.owner,
                quest.getId(), task.getKey(), 4090L).get(0).getStatus());

            List<com.jsirgalaxybase.quest.core.FactProcessingResult> peerResults = detector.detectAutomatically(
                fixture.peer, quest.getId(), task.getKey(), 4060L);
            assertEquals(FactProcessingStatus.ACCEPTED, peerResults.get(0).getStatus());
            assertEquals("different players' personal counters remain independent",
                Arrays.asList(2L), progress(fixture, progress, quest, fixture.peer)
                    .getTasks().get("craft").getValues());
        } finally {
            fixture.close();
        }
    }

    @Test
    public void lockedPartyCraftingStatisticsUnlockThroughPersonalStableFactInJdbc() throws Exception {
        Fixture fixture = Fixture.open();
        try {
            UUID prerequisite = UUID.randomUUID();
            Map<String, String> parameters = new LinkedHashMap<String, String>();
            parameters.put("factType", "galaxy:item_produced");
            parameters.put("progressMode", "item-vector");
            parameters.put("item.count", "1");
            parameters.put("item.0.registryName", "minecraft:iron_ingot");
            parameters.put("item.0.meta", "0");
            parameters.put("item.0.amount", "1");
            parameters.put("allowCraft", "true");
            parameters.put("allowSmelt", "true");
            parameters.put("allowAnvil", "false");
            parameters.put("allowCraftedFromStatistics", "true");
            parameters.put("bq.raw", "{}");
            TaskDefinition task = new TaskDefinition("craft", "bq_standard:crafting", false, parameters);
            QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Locked Party Crafting", "",
                QuestLogic.AND, QuestLogic.AND, Collections.singleton(prerequisite), Collections.singletonList(task),
                Collections.singletonList(new RewardDefinition("reward-0", "bq_standard:item",
                    Collections.<String, String>emptyMap())), RepeatPolicy.never(),
                QuestBehavior.builder().participantScope(com.jsirgalaxybase.quest.core.QuestParticipantScope.PLAYER)
                    .build());

            JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(fixture.manager);
            StoredQuestDefinition draft = fixture.transaction.inTransaction(() -> definitions.createDraft(quest));
            assertTrue(fixture.transaction.inTransaction(() -> definitions.publish(quest.getId(), quest.getVersion(),
                draft.getContentHash(), 1000L)));
            JdbcQuestRuntimeRepository progress = new JdbcQuestRuntimeRepository(fixture.manager);
            // The Party's online owner has unlocked the Quest, so its online active-Quest union admits
            // the peer's own statistics snapshot while the peer's personal progress remains LOCKED.
            fixture.transaction.inTransaction(() -> {
                progress.saveProgress(new QuestProgressSnapshot(ParticipantId.player(fixture.owner), prerequisite, 1,
                    QuestStatus.COMPLETED, Collections.<String, com.jsirgalaxybase.quest.core.TaskProgress>emptyMap(),
                    900L, 0));
                return null;
            });
            ParticipantMembershipResolver memberships = new JdbcParticipantMembershipResolver(fixture.manager);
            JdbcQuestCompletionQuery completions = new JdbcQuestCompletionQuery(fixture.manager);
            QuestOnlinePlayerDirectory onlinePlayers = playerId -> fixture.owner.equals(playerId)
                || fixture.peer.equals(playerId);
            BqPartyActiveQuestQuery partyActivity = new BqPartyActiveQuestQuery("s2", completions, progress,
                onlinePlayers);
            QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions::findAllPublished, completions,
                memberships, new BqHuntPartyAssignmentPolicy(), partyActivity);
            QuestRuntimeService runtime = new QuestRuntimeService(progress, fixture.transaction,
                new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
            AuthenticatedCraftingStatisticsDetectionService detector =
                new AuthenticatedCraftingStatisticsDetectionService(definitions, progress, runtime, planner,
                    (playerId, registryName) -> java.util.OptionalLong.of(5L), "s2");

            List<com.jsirgalaxybase.quest.core.FactProcessingResult> locked = detector.detectAutomatically(
                fixture.peer, quest.getId(), task.getKey(), 1100L);
            assertEquals(1, locked.size());
            assertEquals(FactProcessingStatus.ACCEPTED, locked.get(0).getStatus());
            QuestProgressSnapshot lockedProgress = progress(fixture, progress, quest, fixture.peer);
            assertEquals("the peer's personal task vector can fill while its prerequisite remains locked",
                QuestStatus.LOCKED, lockedProgress.getStatus());
            assertTrue(lockedProgress.getTasks().get(task.getKey()).isComplete());
            assertEquals("locked statistics must not create a reward entitlement", 0L,
                countRewardEntitlements(fixture, quest.getId(), "reward-0"));

            fixture.transaction.inTransaction(() -> {
                progress.saveProgress(new QuestProgressSnapshot(ParticipantId.player(fixture.peer), prerequisite, 1,
                    QuestStatus.COMPLETED, Collections.<String, com.jsirgalaxybase.quest.core.TaskProgress>emptyMap(),
                    1200L, 0));
                return null;
            });
            List<com.jsirgalaxybase.quest.core.FactProcessingResult> unlocked = detector.detectAutomatically(
                fixture.peer, quest.getId(), task.getKey(), 1300L);
            assertEquals("the unlock pass must use a new fact key rather than replaying the locked snapshot", 1,
                unlocked.size());
            assertEquals(FactProcessingStatus.ACCEPTED, unlocked.get(0).getStatus());
            assertEquals(QuestStatus.COMPLETED,
                progress(fixture, progress, quest, fixture.peer).getStatus());
            assertEquals("unlocking the already-complete personal Quest creates exactly one reward", 1L,
                countRewardEntitlements(fixture, quest.getId(), "reward-0"));
            assertTrue(detector.detectAutomatically(fixture.peer, quest.getId(), task.getKey(), 1360L).isEmpty());
            assertEquals(1L, countRewardEntitlements(fixture, quest.getId(), "reward-0"));
        } finally {
            fixture.close();
        }
    }

    @Test
    public void importedBlockBreakPreservesFirstMatchingRequirementOrderInJdbcProgress() throws Exception {
        Fixture fixture = Fixture.open();
        Path source = null;
        try {
            source = Files.createTempFile("bq-block-break-jdbc-it", ".json");
            String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":28,"
                + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Imported Block Break\","
                + "\"questLogic:8\":\"AND\",\"taskLogic:8\":\"AND\"}},"
                + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:block_break\","
                + "\"blocks:9\":{"
                + "\"0:10\":{\"blockID:8\":\"minecraft:stone\",\"meta:3\":-1,\"amount:3\":2,"
                + "\"oreDict:8\":\"\",\"nbt:10\":{}},"
                + "\"1:10\":{\"blockID:8\":\"minecraft:gold_ore\",\"meta:3\":0,\"amount:3\":1,"
                + "\"oreDict:8\":\"\",\"nbt:10\":{}}}}},\"rewards:9\":{}}";
            Files.write(source, json.getBytes(StandardCharsets.UTF_8));
            QuestDefinition quest = new BqQuestDefinitionImporter().read(source).getDefinition();
            TaskDefinition task = quest.getTasks().get(0);
            assertEquals("bq_standard:block_break", task.getTypeId());
            assertEquals("block-vector", task.getParameters().get("progressMode"));
            assertEquals("2", task.getParameters().get("block.count"));

            JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(fixture.manager);
            StoredQuestDefinition draft = fixture.transaction.inTransaction(() -> definitions.createDraft(quest));
            assertTrue(fixture.transaction.inTransaction(() -> definitions.publish(quest.getId(), quest.getVersion(),
                draft.getContentHash(), 1000L)));
            ParticipantMembershipResolver soloMemberships = playerId ->
                Collections.singletonList(ParticipantMembership.player(playerId));
            QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions::findAllPublished,
                new JdbcQuestCompletionQuery(fixture.manager), soloMemberships, new BqHuntPartyAssignmentPolicy());
            JdbcQuestRuntimeRepository runtimeRepository = new JdbcQuestRuntimeRepository(fixture.manager);
            QuestRuntimeService runtime = new QuestRuntimeService(runtimeRepository, fixture.transaction,
                new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
            final List<FactProcessingStatus> outcomes = new ArrayList<FactProcessingStatus>();
            BqPartyInventorySnapshotFactSink producer = fixture.sink(fact ->
                outcomes.add(runtime.apply(fact, planner, 4000L).getStatus()));

            Map<String, String> stoneAttributes = new LinkedHashMap<String, String>();
            stoneAttributes.put("registryName", "minecraft:stone");
            stoneAttributes.put("meta", "3");
            stoneAttributes.put("oreDictionary", "");
            GameplayFact firstStone = new GameplayFact("s2", "block-break-stone-1", fixture.owner,
                "galaxy:block_broken", 1000L, stoneAttributes);
            producer.accept(firstStone);
            assertEquals(Arrays.asList(1L, 0L), progress(fixture, runtimeRepository, quest, fixture.owner)
                .getTasks().get(task.getKey()).getValues());

            producer.accept(firstStone);
            assertEquals("the original fact key must make a replay idempotent", FactProcessingStatus.DUPLICATE,
                outcomes.get(outcomes.size() - 1));

            producer.accept(new GameplayFact("s2", "block-break-stone-2", fixture.owner, "galaxy:block_broken",
                1001L, stoneAttributes));
            producer.accept(new GameplayFact("s2", "block-break-stone-3", fixture.owner, "galaxy:block_broken",
                1002L, stoneAttributes));
            QuestProgressSnapshot afterStones = progress(fixture, runtimeRepository, quest, fixture.owner);
            assertEquals("BQ breaks after the first matching requirement even after it reaches its target",
                Arrays.asList(2L, 0L), afterStones.getTasks().get(task.getKey()).getValues());
            assertEquals(QuestStatus.IN_PROGRESS, afterStones.getStatus());

            Map<String, String> goldOreAttributes = new LinkedHashMap<String, String>();
            goldOreAttributes.put("registryName", "minecraft:gold_ore");
            goldOreAttributes.put("meta", "0");
            goldOreAttributes.put("oreDictionary", "");
            producer.accept(new GameplayFact("s2", "block-break-gold-ore", fixture.owner,
                "galaxy:block_broken", 1003L, goldOreAttributes));

            QuestProgressSnapshot completed = progress(fixture, runtimeRepository, quest, fixture.owner);
            assertEquals(QuestStatus.COMPLETED, completed.getStatus());
            assertEquals(Arrays.asList(2L, 1L), completed.getTasks().get(task.getKey()).getValues());
            assertEquals(4, count(outcomes, FactProcessingStatus.ACCEPTED));
            assertEquals(1, count(outcomes, FactProcessingStatus.DUPLICATE));
        } finally {
            if (source != null) Files.deleteIfExists(source);
            fixture.close();
        }
    }

    @Test
    public void importedManualRetrievalPersistsAppliedInventoryAndCompletesJdbcProgressOnce() throws Exception {
        Fixture fixture = Fixture.open();
        Path source = null;
        try {
            String registryName = "galaxybase:quest_r22_" + UUID.randomUUID().toString().replace("-", "");
            source = Files.createTempFile("bq-consuming-retrieval-jdbc-it", ".json");
            String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":19,"
                + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Imported Manual Retrieval\","
                + "\"questLogic:8\":\"AND\",\"taskLogic:8\":\"AND\"}},"
                + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:retrieval\","
                + "\"consume:1\":1,\"autoConsume:1\":0,\"ignoreNBT:1\":0,\"partialMatch:1\":1,"
                + "\"requiredItems:9\":{\"0:10\":{\"id:8\":\"" + registryName + "\","
                + "\"Damage:2\":2,\"Count:3\":5,\"tag:10\":{\"grade:3\":2}}}}},\"rewards:9\":{}}";
            Files.write(source, json.getBytes(StandardCharsets.UTF_8));
            QuestDefinition quest = new BqQuestDefinitionImporter().read(source).getDefinition();
            TaskDefinition retrieval = quest.getTasks().get(0);
            assertEquals("true", retrieval.getParameters().get("consume"));
            assertEquals("false", retrieval.getParameters().get("autoConsume"));

            JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(fixture.manager);
            StoredQuestDefinition draft = fixture.transaction.inTransaction(() -> definitions.createDraft(quest));
            assertTrue(fixture.transaction.inTransaction(() -> definitions.publish(quest.getId(), quest.getVersion(),
                draft.getContentHash(), 7000L)));
            JdbcQuestRuntimeRepository runtimeRepository = new JdbcQuestRuntimeRepository(fixture.manager);
            JdbcQuestCompletionQuery completions = new JdbcQuestCompletionQuery(fixture.manager);
            QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions::findAllPublished, completions,
                new JdbcParticipantMembershipResolver(fixture.manager), new BqHuntPartyAssignmentPolicy());
            QuestRuntimeService runtime = new QuestRuntimeService(runtimeRepository, fixture.transaction,
                new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
            ConsumptionRequestAuthorizer authorizer = new ConsumptionRequestAuthorizer(definitions::findAllPublished,
                runtimeRepository, completions, new QuestEngine());

            Item item = registerQuestTestItem(registryName);
            ItemStack held = new ItemStack(item, 5, 2);
            NBTTagCompound actualTag = new NBTTagCompound();
            actualTag.setInteger("grade", 2);
            actualTag.setString("additionalActualData", "allowed-by-partial-match");
            held.setTagCompound(actualTag);
            final List<ItemStack> liveInventory = new ArrayList<ItemStack>(Collections.singletonList(held));
            final MinecraftPlayerConsumptionPort matcher = new MinecraftPlayerConsumptionPort(playerId -> null,
                player -> {});
            final int[] appliedCalls = new int[1];
            ConsumptionPort testPlayerPort = new ConsumptionPort() {
                @Override public ConsumptionApplyResult apply(ConsumptionRequest request) {
                    appliedCalls[0]++;
                    MinecraftPlayerConsumptionPort.ItemPlan plan = matcher.planItems(liveInventory, request);
                    if (plan == null) return ConsumptionApplyResult.insufficient("no-matching-stack");
                    liveInventory.clear();
                    liveInventory.addAll(plan.inventory);
                    return ConsumptionApplyResult.applied("minecraft-item-plan", plan.appliedAmounts);
                }
            };
            final boolean[] allowFactProjection = new boolean[1];
            QuestEvaluationRequestProvider gatedRequests = new QuestEvaluationRequestProvider() {
                @Override public List<com.jsirgalaxybase.quest.core.QuestEvaluationRequest> requestsFor(
                    GameplayFact fact) {
                    return allowFactProjection[0] ? planner.requestsFor(fact)
                        : Collections.<com.jsirgalaxybase.quest.core.QuestEvaluationRequest>emptyList();
                }
            };
            JdbcConsumptionSubmissionRepository submissionRepository =
                new JdbcConsumptionSubmissionRepository(fixture.manager);
            ConsumptionSubmissionService submissions = new ConsumptionSubmissionService(
                submissionRepository, testPlayerPort, runtime, gatedRequests, fixture.transaction);
            UUID playerId = fixture.owner;
            ConsumptionIntent intent = new ConsumptionIntent("r22-manual-retrieval", "s2", playerId,
                quest.getId(), retrieval.getKey(), 7100L);

            try {
                submissions.submit(intent, authorizer, 7100L);
                fail("an applied item submission without an evaluation request must remain resumable");
            } catch (IllegalStateException expected) {
                assertEquals("applied consumption has no matching published task", expected.getMessage());
            }

            com.jsirgalaxybase.quest.core.ConsumptionSubmission applied = fixture.transaction.inTransaction(() ->
                submissionRepository.find(intent.getSubmissionKey()).get());
            assertEquals(ConsumptionSubmissionStatus.APPLIED, applied.getStatus());
            assertEquals("the player-side item plan is applied only once before recovery", 1, appliedCalls[0]);
            assertFalse("progress projection failure must not confirm the submission",
                fixture.transaction.inTransaction(() -> runtimeRepository.findProgress(
                    ParticipantId.player(playerId), quest.getId(), quest.getVersion()).isPresent()));

            allowFactProjection[0] = true;
            com.jsirgalaxybase.quest.core.ConsumptionSubmission result = submissions.resume(
                intent.getSubmissionKey(), 7150L);

            assertEquals(ConsumptionSubmissionStatus.CONFIRMED, result.getStatus());
            assertEquals(Collections.singletonList(5L), result.getAppliedAmounts());
            assertEquals(1, appliedCalls[0]);
            assertNull("the actual item planner must remove the matched stack", liveInventory.get(0));
            QuestProgressSnapshot progress = progress(fixture, runtimeRepository, quest, playerId);
            assertEquals(QuestStatus.COMPLETED, progress.getStatus());
            assertEquals(Collections.singletonList(5L), progress.getTasks().get(retrieval.getKey()).getValues());

            com.jsirgalaxybase.quest.core.ConsumptionSubmission replay = submissions.submit(intent, authorizer, 7200L);
            assertEquals(ConsumptionSubmissionStatus.CONFIRMED, replay.getStatus());
            assertEquals("a durable replay must not consume the player's inventory twice", 1, appliedCalls[0]);
            assertEquals(QuestStatus.COMPLETED, progress(fixture, runtimeRepository, quest, playerId).getStatus());
        } finally {
            if (source != null) Files.deleteIfExists(source);
            fixture.close();
        }
    }

    @Test
    public void importedHeldStackSplitsAcrossOverlappingRetrievalRequirementsAndProjectsActualAmounts()
        throws Exception {
        Fixture fixture = Fixture.open();
        Path source = null;
        try {
            String registryName = "galaxybase:quest_r23_" + UUID.randomUUID().toString().replace("-", "");
            source = Files.createTempFile("bq-held-retrieval-split-it", ".json");
            String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":20,"
                + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Held Stack Split\","
                + "\"questLogic:8\":\"AND\",\"taskLogic:8\":\"AND\"}},"
                + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:retrieval\","
                + "\"consume:1\":1,\"autoConsume:1\":0,\"ignoreNBT:1\":0,\"partialMatch:1\":1,"
                + "\"requiredItems:9\":{"
                + "\"0:10\":{\"id:8\":\"" + registryName + "\",\"Damage:2\":2,\"Count:3\":4,"
                + "\"tag:10\":{\"grade:3\":2}},"
                + "\"1:10\":{\"id:8\":\"" + registryName + "\",\"Damage:2\":2,\"Count:3\":4,"
                + "\"tag:10\":{\"grade:3\":2,\"variant:3\":1}}}}},\"rewards:9\":{}}";
            Files.write(source, json.getBytes(StandardCharsets.UTF_8));
            QuestDefinition quest = new BqQuestDefinitionImporter().read(source).getDefinition();
            TaskDefinition retrieval = quest.getTasks().get(0);
            assertEquals("true", retrieval.getParameters().get("consume"));
            assertEquals("2", retrieval.getParameters().get("item.count"));

            JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(fixture.manager);
            StoredQuestDefinition draft = fixture.transaction.inTransaction(() -> definitions.createDraft(quest));
            assertTrue(fixture.transaction.inTransaction(() -> definitions.publish(quest.getId(), quest.getVersion(),
                draft.getContentHash(), 8000L)));
            JdbcQuestRuntimeRepository runtimeRepository = new JdbcQuestRuntimeRepository(fixture.manager);
            JdbcQuestCompletionQuery completions = new JdbcQuestCompletionQuery(fixture.manager);
            QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions::findAllPublished, completions,
                new JdbcParticipantMembershipResolver(fixture.manager), new BqHuntPartyAssignmentPolicy());
            QuestRuntimeService runtime = new QuestRuntimeService(runtimeRepository, fixture.transaction,
                new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
            ConsumptionRequestAuthorizer authorizer = new ConsumptionRequestAuthorizer(definitions::findAllPublished,
                runtimeRepository, completions, new QuestEngine());

            Item item = registerQuestTestItem(registryName);
            NBTTagCompound actualTag = new NBTTagCompound();
            actualTag.setInteger("grade", 2);
            actualTag.setInteger("variant", 1);
            actualTag.setString("extraActualData", "allowed-by-partial-match");
            ItemStack firstHeldStack = new ItemStack(item, 7, 2);
            firstHeldStack.setTagCompound((NBTTagCompound) actualTag.copy());
            ItemStack laterMatchingStack = new ItemStack(item, 64, 2);
            laterMatchingStack.setTagCompound((NBTTagCompound) actualTag.copy());
            final List<ItemStack> liveInventory = new ArrayList<ItemStack>(
                Arrays.asList(firstHeldStack, laterMatchingStack));
            final MinecraftPlayerConsumptionPort matcher = new MinecraftPlayerConsumptionPort(playerId -> null,
                player -> {});
            final int[] appliedCalls = new int[1];
            ConsumptionPort testPlayerPort = new ConsumptionPort() {
                @Override public ConsumptionApplyResult apply(ConsumptionRequest request) {
                    appliedCalls[0]++;
                    MinecraftPlayerConsumptionPort.ItemPlan plan = matcher.planItems(liveInventory, request);
                    if (plan == null) return ConsumptionApplyResult.insufficient("no-selected-stack-match");
                    liveInventory.clear();
                    liveInventory.addAll(plan.inventory);
                    return ConsumptionApplyResult.applied("minecraft-selected-item-plan", plan.appliedAmounts);
                }
            };
            JdbcConsumptionSubmissionRepository submissionRepository =
                new JdbcConsumptionSubmissionRepository(fixture.manager);
            ConsumptionSubmissionService submissions = new ConsumptionSubmissionService(
                submissionRepository, testPlayerPort, runtime, planner, fixture.transaction);
            UUID playerId = fixture.owner;
            ConsumptionIntent firstIntent = new ConsumptionIntent("r23-held-split-first", "s2", playerId,
                quest.getId(), retrieval.getKey(), 8100L,
                ConsumptionSelection.selectedStack(0, MinecraftPlayerConsumptionPort.fingerprint(firstHeldStack)));

            com.jsirgalaxybase.quest.core.ConsumptionSubmission first = submissions.submit(
                firstIntent, authorizer, 8100L);
            assertEquals(ConsumptionSubmissionStatus.CONFIRMED, first.getStatus());
            assertEquals(Arrays.asList(4L, 3L), first.getAppliedAmounts());
            assertNull("only the server-captured selected stack is consumed", liveInventory.get(0));
            assertEquals(64, liveInventory.get(1).stackSize);
            QuestProgressSnapshot partial = progress(fixture, runtimeRepository, quest, playerId);
            assertEquals(QuestStatus.IN_PROGRESS, partial.getStatus());
            assertEquals(Arrays.asList(4L, 3L), partial.getTasks().get(retrieval.getKey()).getValues());

            ItemStack secondHeldStack = liveInventory.get(1);
            ConsumptionIntent secondIntent = new ConsumptionIntent("r23-held-split-second", "s2", playerId,
                quest.getId(), retrieval.getKey(), 8200L,
                ConsumptionSelection.selectedStack(1, MinecraftPlayerConsumptionPort.fingerprint(secondHeldStack)));
            com.jsirgalaxybase.quest.core.ConsumptionSubmission second = submissions.submit(
                secondIntent, authorizer, 8200L);
            assertEquals(ConsumptionSubmissionStatus.CONFIRMED, second.getStatus());
            assertEquals(Arrays.asList(0L, 1L), second.getAppliedAmounts());
            assertEquals(63, liveInventory.get(1).stackSize);
            QuestProgressSnapshot completed = progress(fixture, runtimeRepository, quest, playerId);
            assertEquals(QuestStatus.COMPLETED, completed.getStatus());
            assertEquals(Arrays.asList(4L, 4L), completed.getTasks().get(retrieval.getKey()).getValues());

            com.jsirgalaxybase.quest.core.ConsumptionSubmission replay = submissions.submit(
                secondIntent, authorizer, 8300L);
            assertEquals(ConsumptionSubmissionStatus.CONFIRMED, replay.getStatus());
            assertEquals("replaying a confirmed hand-in cannot consume another stack", 2, appliedCalls[0]);
            assertEquals(63, liveInventory.get(1).stackSize);
        } finally {
            if (source != null) Files.deleteIfExists(source);
            fixture.close();
        }
    }

    @Test
    public void importedQuestDetectCreditsIndivisibleFluidAndRecoversAfterPlayerApply() throws Exception {
        Fixture fixture = Fixture.open();
        Path source = null;
        try {
            String fluidName = "galaxy_test:steam";
            String registryName = "galaxybase:quest_r24_fluid_"
                + UUID.randomUUID().toString().replace("-", "");
            source = Files.createTempFile("bq-fluid-submit-recovery-it", ".json");
            String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":24,"
                + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Fluid Submit Recovery\","
                + "\"questLogic:8\":\"AND\",\"taskLogic:8\":\"AND\"}},"
                + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:fluid\","
                + "\"consume:1\":1,\"autoConsume:1\":0,\"groupDetect:1\":0,\"ignoreNBT:1\":0,"
                + "\"requiredFluids:9\":{\"0:10\":{\"FluidName:8\":\"" + fluidName
                + "\",\"Amount:3\":1000}}}},\"rewards:9\":{}}";
            Files.write(source, json.getBytes(StandardCharsets.UTF_8));
            QuestDefinition quest = new BqQuestDefinitionImporter().read(source).getDefinition();
            TaskDefinition fluidTask = quest.getTasks().get(0);
            assertEquals("bq_standard:fluid", fluidTask.getTypeId());
            assertEquals("true", fluidTask.getParameters().get("consume"));
            assertEquals("false", fluidTask.getParameters().get("autoConsume"));
            assertEquals("false", fluidTask.getParameters().get("ignoreNBT"));
            assertEquals("false", fluidTask.getParameters().get("fluid.0.nbtPresent"));

            JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(fixture.manager);
            StoredQuestDefinition draft = fixture.transaction.inTransaction(() -> definitions.createDraft(quest));
            assertTrue(fixture.transaction.inTransaction(() -> definitions.publish(quest.getId(), quest.getVersion(),
                draft.getContentHash(), 9000L)));
            JdbcQuestRuntimeRepository runtimeRepository = new JdbcQuestRuntimeRepository(fixture.manager);
            JdbcQuestCompletionQuery completions = new JdbcQuestCompletionQuery(fixture.manager);
            QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions::findAllPublished, completions,
                new JdbcParticipantMembershipResolver(fixture.manager), new BqHuntPartyAssignmentPolicy());
            QuestRuntimeService runtime = new QuestRuntimeService(runtimeRepository, fixture.transaction,
                new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
            ConsumptionRequestAuthorizer authorizer = new ConsumptionRequestAuthorizer(definitions::findAllPublished,
                runtimeRepository, completions, new QuestEngine());

            Item containerItem = registerQuestTestItem(registryName);
            final List<ItemStack> inventory = new ArrayList<ItemStack>(Arrays.asList(
                fluidContainer(containerItem, fluidName, 400, false), null, null));
            TestFluidContainerAdapter fluidAdapter = new TestFluidContainerAdapter();
            TestFluidConsumptionPort playerPort = new TestFluidConsumptionPort(inventory, fluidAdapter);
            JdbcConsumptionSubmissionRepository submissionRepository =
                new JdbcConsumptionSubmissionRepository(fixture.manager);
            ConsumptionSubmissionService submissions = new ConsumptionSubmissionService(
                submissionRepository, playerPort, runtime, planner, fixture.transaction);
            AuthenticatedQuestConsumptionDetectService detect = new AuthenticatedQuestConsumptionDetectService("s2",
                new JdbcConsumptionDetectBatchRepository(fixture.manager), authorizer, submissions, runtime, planner,
                fixture.transaction);
            UUID playerId = fixture.owner;

            ConsumptionIntent firstIntent = new ConsumptionIntent("r24-fluid-recovery-partial", "s2", playerId,
                quest.getId(), fluidTask.getKey(), 9100L,
                ConsumptionSelection.selectedStack(0, MinecraftPlayerConsumptionPort.fingerprint(inventory.get(0))));
            final ConsumptionRequest[] firstRequestHolder = new ConsumptionRequest[1];
            fixture.transaction.inTransaction(() -> {
                firstRequestHolder[0] = authorizer.authorize(firstIntent, 9100L);
                submissionRepository.prepareIfAbsent(firstRequestHolder[0]);
                return null;
            });
            ConsumptionRequest firstRequest = firstRequestHolder[0];
            assertEquals("fluid", firstRequest.getKind());
            assertEquals(Collections.singletonList(1000L), firstRequest.getAmounts());
            assertEquals(fluidName, firstRequest.getResourceParameters().get("fluid.0.name"));
            assertEquals("false", firstRequest.getResourceParameters().get("fluid.0.nbtPresent"));
            ConsumptionApplyResult appliedBeforeCrash = playerPort.apply(firstRequest);
            assertTrue(appliedBeforeCrash.isApplied());
            assertEquals(Collections.singletonList(400L), appliedBeforeCrash.getAppliedAmounts());
            assertEquals(400, fluidAdapter.drainedAmount);
            assertEquals("a returned empty container is retained", 0,
                fluidAdapter.amount(inventory.get(0)));

            // Simulate process loss after the player-side inventory+idempotency marker commit,
            // but before the PostgreSQL submission advances beyond PREPARED.
            com.jsirgalaxybase.quest.core.ConsumptionSubmission recovered = submissions.resume(
                firstIntent.getSubmissionKey(), 9200L);
            assertEquals(ConsumptionSubmissionStatus.CONFIRMED, recovered.getStatus());
            assertEquals(Collections.singletonList(400L), recovered.getAppliedAmounts());
            assertEquals("recovery must replay the player marker, not drain a second time", 1,
                fluidAdapter.drainCalls);
            assertEquals(Collections.singletonList(400L), progress(fixture, runtimeRepository, quest, playerId)
                .getTasks().get(fluidTask.getKey()).getValues());

            ItemStack indivisible = fluidContainer(containerItem, fluidName, 750, true);
            inventory.set(1, indivisible);
            ConsumptionDetectBatchResult detectResult = detect.detect(playerId, quest.getId(), 9300L);
            assertEquals(ConsumptionDetectBatchResult.Status.COMPLETED, detectResult.getStatus());
            assertEquals(1, detectResult.getSubmissions().size());
            com.jsirgalaxybase.quest.core.ConsumptionSubmission second = detectResult.getSubmissions().get(0);
            assertEquals(ConsumptionSubmissionStatus.CONFIRMED, second.getStatus());
            assertEquals("BQ credits the whole indivisible container, including its 150 mB overage",
                Collections.singletonList(750L), second.getAppliedAmounts());
            assertEquals(1150, fluidAdapter.drainedAmount);
            QuestProgressSnapshot completed = progress(fixture, runtimeRepository, quest, playerId);
            assertEquals(QuestStatus.COMPLETED, completed.getStatus());
            assertEquals(Collections.singletonList(1150L), completed.getTasks().get(fluidTask.getKey()).getValues());

            com.jsirgalaxybase.quest.core.ConsumptionSubmission replay = submissions.resume(
                second.getRequest().getSubmissionKey(), 9400L);
            assertEquals(ConsumptionSubmissionStatus.CONFIRMED, replay.getStatus());
            assertEquals("a confirmed fluid hand-in cannot drain again", 2, fluidAdapter.drainCalls);
            assertEquals(1150, fluidAdapter.drainedAmount);
        } finally {
            if (source != null) Files.deleteIfExists(source);
            fixture.close();
        }
    }

    @Test
    public void importedQuestDetectProcessesMultipleConsumingRetrievalTasksInOrderAndResumesAfterInterruption()
        throws Exception {
        Fixture fixture = Fixture.open();
        Path source = null;
        try {
            String itemAName = "galaxybase:quest_detect_a_" + UUID.randomUUID().toString().replace("-", "");
            String itemBName = "galaxybase:quest_detect_b_" + UUID.randomUUID().toString().replace("-", "");
            source = Files.createTempFile("bq-quest-detect-multiple-retrievals-it", ".json");
            String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":25,"
                + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Ordered Quest Detect\","
                + "\"questLogic:8\":\"AND\",\"taskLogic:8\":\"OR\"}},"
                + "\"tasks:9\":{"
                + "\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:retrieval\","
                + "\"consume:1\":1,\"autoConsume:1\":0,\"ignoreNBT:1\":1,\"partialMatch:1\":1,"
                + "\"requiredItems:9\":{\"0:10\":{\"id:8\":\"" + itemAName
                + "\",\"Damage:2\":0,\"Count:3\":2}}},"
                + "\"1:10\":{\"index:3\":1,\"taskID:8\":\"bq_standard:retrieval\","
                + "\"consume:1\":1,\"autoConsume:1\":0,\"ignoreNBT:1\":1,\"partialMatch:1\":1,"
                + "\"requiredItems:9\":{\"0:10\":{\"id:8\":\"" + itemBName
                + "\",\"Damage:2\":0,\"Count:3\":3}}}},\"rewards:9\":{}}";
            Files.write(source, json.getBytes(StandardCharsets.UTF_8));
            QuestDefinition quest = new BqQuestDefinitionImporter().read(source).getDefinition();
            assertEquals(2, quest.getTasks().size());
            TaskDefinition firstTask = quest.getTasks().get(0);
            TaskDefinition secondTask = quest.getTasks().get(1);
            assertEquals("true", firstTask.getParameters().get("consume"));
            assertEquals("true", secondTask.getParameters().get("consume"));

            JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(fixture.manager);
            StoredQuestDefinition draft = fixture.transaction.inTransaction(() -> definitions.createDraft(quest));
            assertTrue(fixture.transaction.inTransaction(() -> definitions.publish(quest.getId(), quest.getVersion(),
                draft.getContentHash(), 9500L)));
            JdbcQuestRuntimeRepository runtimeRepository = new JdbcQuestRuntimeRepository(fixture.manager);
            JdbcQuestCompletionQuery completions = new JdbcQuestCompletionQuery(fixture.manager);
            QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions::findAllPublished, completions,
                new JdbcParticipantMembershipResolver(fixture.manager), new BqHuntPartyAssignmentPolicy());
            QuestRuntimeService runtime = new QuestRuntimeService(runtimeRepository, fixture.transaction,
                new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
            ConsumptionRequestAuthorizer authorizer = new ConsumptionRequestAuthorizer(definitions::findAllPublished,
                runtimeRepository, completions, new QuestEngine());
            Item itemA = registerQuestTestItem(itemAName);
            Item itemB = registerQuestTestItem(itemBName);
            final List<ItemStack> inventory = new ArrayList<ItemStack>(Arrays.asList(
                new ItemStack(itemA, 2, 0), new ItemStack(itemB, 3, 0)));
            final MinecraftPlayerConsumptionPort matcher = new MinecraftPlayerConsumptionPort(playerId -> null,
                player -> {});
            final List<String> appliedTaskKeys = new ArrayList<String>();
            final List<String> submissionKeys = new ArrayList<String>();
            final boolean[] failBeforeSecondApply = new boolean[] { true };
            final int[] applyCalls = new int[1];
            ConsumptionPort playerPort = request -> {
                applyCalls[0]++;
                appliedTaskKeys.add(request.getTaskKey());
                submissionKeys.add(request.getSubmissionKey());
                if (failBeforeSecondApply[0] && applyCalls[0] == 2) {
                    failBeforeSecondApply[0] = false;
                    throw new IllegalStateException("injected interruption between consuming Tasks");
                }
                MinecraftPlayerConsumptionPort.ItemPlan plan = matcher.planItems(inventory, request);
                if (plan == null) return ConsumptionApplyResult.insufficient("no-matching-stack");
                inventory.clear();
                inventory.addAll(plan.inventory);
                return ConsumptionApplyResult.applied("quest-detect-item-plan", plan.appliedAmounts);
            };
            JdbcConsumptionSubmissionRepository submissionRepository =
                new JdbcConsumptionSubmissionRepository(fixture.manager);
            ConsumptionSubmissionService submissions = new ConsumptionSubmissionService(
                submissionRepository, playerPort, runtime, planner, fixture.transaction);
            JdbcConsumptionDetectBatchRepository batches = new JdbcConsumptionDetectBatchRepository(fixture.manager);
            AuthenticatedQuestConsumptionDetectService detect = new AuthenticatedQuestConsumptionDetectService(
                "s2", batches, authorizer, submissions, runtime, planner, fixture.transaction);

            try {
                detect.detect(fixture.owner, quest.getId(), 9600L);
                fail("the injected second Task interruption must leave the Quest Detect batch resumable");
            } catch (IllegalStateException expected) {
                assertEquals("injected interruption between consuming Tasks", expected.getMessage());
            }

            assertEquals(Arrays.asList(firstTask.getKey(), secondTask.getKey()), appliedTaskKeys);
            assertEquals(2, applyCalls[0]);
            assertNull("the first Task inventory change is durable before the next Task starts", inventory.get(0));
            assertEquals(3, inventory.get(1).stackSize);
            com.jsirgalaxybase.quest.core.ConsumptionDetectBatch active = fixture.transaction.inTransaction(() ->
                batches.findActive(fixture.owner, quest.getId(), quest.getVersion()).get());
            assertEquals(1, active.getNextTaskIndex());
            QuestProgressSnapshot partial = progress(fixture, runtimeRepository, quest, fixture.owner);
            assertEquals("OR completion is deferred until the whole Quest Detect pass settles", QuestStatus.IN_PROGRESS,
                partial.getStatus());
            assertTrue(partial.getTasks().get(firstTask.getKey()).isComplete());
            assertFalse(partial.getTasks().get(secondTask.getKey()).isComplete());

            ConsumptionDetectBatchResult resumed = detect.detect(fixture.owner, quest.getId(), 9700L);
            assertEquals(ConsumptionDetectBatchResult.Status.COMPLETED, resumed.getStatus());
            assertEquals(1, resumed.getSubmissions().size());
            assertEquals(3, applyCalls[0]);
            assertEquals(Arrays.asList(firstTask.getKey(), secondTask.getKey(), secondTask.getKey()), appliedTaskKeys);
            assertEquals("retry must reuse the original Task submission key", submissionKeys.get(1),
                submissionKeys.get(2));
            assertEquals(ConsumptionSubmissionStatus.CONFIRMED, resumed.getSubmissions().get(0).getStatus());
            assertNull(inventory.get(0));
            assertNull(inventory.get(1));
            assertFalse(fixture.transaction.inTransaction(() -> batches.findActive(
                fixture.owner, quest.getId(), quest.getVersion()).isPresent()));
            QuestProgressSnapshot completed = progress(fixture, runtimeRepository, quest, fixture.owner);
            assertEquals(QuestStatus.COMPLETED, completed.getStatus());
            assertTrue(completed.getTasks().get(firstTask.getKey()).isComplete());
            assertTrue(completed.getTasks().get(secondTask.getKey()).isComplete());
        } finally {
            if (source != null) Files.deleteIfExists(source);
            fixture.close();
        }
    }

    private static ItemStack fluidContainer(Item item, String fluidName, int amount, boolean indivisible) {
        ItemStack stack = new ItemStack(item, 1);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("fluidName", fluidName);
        tag.setInteger("fluidAmount", amount);
        tag.setBoolean("indivisible", indivisible);
        stack.setTagCompound(tag);
        return stack;
    }

    private static final class TestFluidContainerAdapter implements QuestFluidContainerAdapter {
        private int drainCalls;
        private int drainedAmount;

        @Override public FluidView inspect(ItemStack container) {
            if (container == null || container.getTagCompound() == null) return null;
            NBTTagCompound stackTag = container.getTagCompound();
            String name = stackTag.getString("fluidName");
            int amount = stackTag.getInteger("fluidAmount");
            if (name.isEmpty() || amount <= 0) return null;
            NBTTagCompound fluidTag = stackTag.hasKey("fluidTag", 10)
                ? (NBTTagCompound) stackTag.getCompoundTag("fluidTag").copy() : null;
            return new FluidView(name, amount, fluidTag);
        }

        @Override public FluidDrain drain(ItemStack container, int maximumAmount) {
            FluidView available = inspect(container);
            if (available == null || maximumAmount <= 0) return null;
            drainCalls++;
            boolean indivisible = container.getTagCompound().getBoolean("indivisible");
            int amount = indivisible ? available.getAmount() : Math.min(available.getAmount(), maximumAmount);
            drainedAmount += amount;
            container.getTagCompound().setInteger("fluidAmount", available.getAmount() - amount);
            return new FluidDrain(new FluidView(available.getName(), amount, available.getTag()), container);
        }

        private int amount(ItemStack stack) {
            return stack == null || stack.getTagCompound() == null ? 0
                : stack.getTagCompound().getInteger("fluidAmount");
        }
    }

    private static final class TestFluidConsumptionPort implements ConsumptionPort {
        private final List<ItemStack> inventory;
        private final MinecraftPlayerConsumptionPort planner;
        private final Map<String, List<Long>> persistedMarkers = new LinkedHashMap<String, List<Long>>();

        private TestFluidConsumptionPort(List<ItemStack> inventory, QuestFluidContainerAdapter fluids) {
            this.inventory = inventory;
            this.planner = new MinecraftPlayerConsumptionPort(playerId -> null, player -> {}, fluids);
        }

        @Override public ConsumptionApplyResult apply(ConsumptionRequest request) {
            List<Long> marker = persistedMarkers.get(request.getSubmissionKey());
            if (marker != null) return ConsumptionApplyResult.applied("player-marker-replay", marker);
            MinecraftPlayerConsumptionPort.FluidPlan plan = planner.planFluidsDetailed(inventory, request);
            if (plan == null) return ConsumptionApplyResult.insufficient("insufficient-fluid");
            inventory.clear();
            inventory.addAll(plan.inventory);
            List<Long> actual = Collections.unmodifiableList(new ArrayList<Long>(plan.appliedAmounts));
            persistedMarkers.put(request.getSubmissionKey(), actual);
            return ConsumptionApplyResult.applied("player-inventory-fluid-plan", actual);
        }
    }

    private static Item registerQuestTestItem(String registryName) throws Exception {
        Item item = new Item();
        item.setHasSubtypes(true);
        Field identityMapField = net.minecraft.util.RegistryNamespaced.class.getDeclaredField("underlyingIntegerMap");
        identityMapField.setAccessible(true);
        net.minecraft.util.ObjectIntIdentityMap identityMap =
            (net.minecraft.util.ObjectIntIdentityMap) identityMapField.get(Item.itemRegistry);
        int id = Short.MAX_VALUE;
        while (identityMap.func_148744_b(id)) id--;
        identityMap.func_148746_a(item, id);
        Field registryObjectsField = net.minecraft.util.RegistrySimple.class.getDeclaredField("registryObjects");
        registryObjectsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, Object> registryObjects = (Map<Object, Object>) registryObjectsField.get(Item.itemRegistry);
        registryObjects.put(registryName, item);
        return item;
    }

    @Test
    public void groupDetectPartyRetrievalReplacesProgressFromCurrentPartyInventory() throws Exception {
        Fixture fixture = Fixture.open();
        try {
            Map<String, String> parameters = new LinkedHashMap<String, String>();
            parameters.put("factType", "galaxy:inventory_observed");
            parameters.put("consume", "false");
            parameters.put("groupDetect", "true");
            parameters.put("progressMode", "item-vector");
            parameters.put("item.count", "2");
            parameters.put("item.0.registryName", "minecraft:iron_ingot");
            parameters.put("item.0.meta", "0");
            parameters.put("item.0.amount", "5");
            parameters.put("item.1.registryName", "minecraft:gold_ingot");
            parameters.put("item.1.meta", "0");
            parameters.put("item.1.amount", "5");
            parameters.put("bq.raw", "{}");
            TaskDefinition retrieval = new TaskDefinition("retrieve", "bq_standard:retrieval", false, parameters);
            QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "BQ Group Detect Integration", "",
                QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(retrieval),
                Collections.<RewardDefinition>emptyList(), RepeatPolicy.never(),
                QuestBehavior.builder().participantScope(com.jsirgalaxybase.quest.core.QuestParticipantScope.PLAYER)
                    .build());
            StoredQuestDefinition published = new StoredQuestDefinition(quest, QuestDefinitionLifecycle.PUBLISHED,
                "group-detect-retrieval-hash", 1L);
            JdbcParticipantMembershipResolver memberships = new JdbcParticipantMembershipResolver(fixture.manager);
            QuestEvaluationPlanner planner = new QuestEvaluationPlanner(() -> Collections.singletonList(published),
                new JdbcQuestCompletionQuery(fixture.manager), memberships, new BqHuntPartyAssignmentPolicy(),
                (definition, fact, partyMembership) -> true);
            JdbcQuestRuntimeRepository runtimeRepository = new JdbcQuestRuntimeRepository(fixture.manager);
            QuestRuntimeService runtime = new QuestRuntimeService(runtimeRepository, fixture.transaction,
                new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
            BqPartyInventorySnapshotFactSink producer = fixture.sink(fact -> runtime.apply(fact, planner, 4000L));
            UUID peerSession = UUID.randomUUID();

            assertTrue(producer.beginInventorySession(fixture.inventory(fixture.peer, peerSession, "iron-only",
                Collections.singletonList(new InventoryObservationEntry("minecraft:iron_ingot", 0, 5L,
                    "ingotIron", "", "", false)))));
            for (UUID player : Arrays.asList(fixture.owner, fixture.peer, fixture.staleMember)) {
                QuestProgressSnapshot progress = progress(fixture, runtimeRepository, quest, player);
                assertEquals(QuestStatus.IN_PROGRESS, progress.getStatus());
                assertEquals(Arrays.asList(5L, 0L), progress.getTasks().get("retrieve").getValues());
            }

            producer.accept(fixture.inventory(fixture.peer, peerSession, "gold-only",
                Collections.singletonList(new InventoryObservationEntry("minecraft:gold_ingot", 0, 5L,
                    "ingotGold", "", "", false))));
            for (UUID player : Arrays.asList(fixture.owner, fixture.peer, fixture.staleMember)) {
                QuestProgressSnapshot progress = progress(fixture, runtimeRepository, quest, player);
                assertEquals("BQ groupDetect clears formerly satisfied progress absent from the fresh snapshot",
                    QuestStatus.IN_PROGRESS, progress.getStatus());
                assertEquals(Arrays.asList(0L, 5L), progress.getTasks().get("retrieve").getValues());
            }

            UUID ownerSession = UUID.randomUUID();
            assertTrue(producer.beginInventorySession(fixture.inventory(fixture.owner, ownerSession, "iron-returned",
                Collections.singletonList(new InventoryObservationEntry("minecraft:iron_ingot", 0, 5L,
                    "ingotIron", "", "", false)))));
            for (UUID player : Arrays.asList(fixture.owner, fixture.peer, fixture.staleMember)) {
                QuestProgressSnapshot progress = progress(fixture, runtimeRepository, quest, player);
                assertEquals(QuestStatus.COMPLETED, progress.getStatus());
                assertEquals(Arrays.asList(5L, 5L), progress.getTasks().get("retrieve").getValues());
            }
        } finally {
            fixture.close();
        }
    }

    private static QuestProgressSnapshot progress(Fixture fixture, JdbcQuestRuntimeRepository repository,
        QuestDefinition quest, UUID player) {
        return repository.findProgress(ParticipantId.player(player), quest.getId(), quest.getVersion()).get();
    }

    private static int count(List<FactProcessingStatus> statuses, FactProcessingStatus target) {
        int count = 0;
        for (FactProcessingStatus status : statuses) if (status == target) count++;
        return count;
    }

    private static void assertItemRewardEntitlements(Fixture fixture, QuestDefinition quest,
        Set<UUID> expectedRecipients) throws Exception {
        Set<UUID> recipients = new LinkedHashSet<UUID>();
        int rows = 0;
        try (Connection connection = fixture.scoped.getConnection(); PreparedStatement statement = connection
            .prepareStatement("SELECT participant_type,participant_id,recipient_player_id,reward_type,"
                + "reward_parameters_json->>'item.0.registryName',reward_parameters_json->>'item.0.amount',"
                + "delivery_status FROM galaxy_quest_reward_entitlement "
                + "WHERE quest_id=? AND definition_version=? AND reward_key=?")) {
            statement.setObject(1, quest.getId());
            statement.setInt(2, quest.getVersion());
            statement.setString(3, "reward-0");
            try (java.sql.ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows++;
                    UUID participantId = result.getObject(2, UUID.class);
                    UUID recipientId = result.getObject(3, UUID.class);
                    assertEquals("each BQ party reward is a personal entitlement", "PLAYER", result.getString(1));
                    assertEquals("personal progress and entitlement recipient must match", participantId, recipientId);
                    assertEquals("bq_standard:item", result.getString(4));
                    assertEquals("minecraft:diamond", result.getString(5));
                    assertEquals("2", result.getString(6));
                    assertEquals("manual BQ rewards remain claimable until claimed", "CLAIMABLE", result.getString(7));
                    assertTrue("recipient must be a member captured by the party quest", expectedRecipients.contains(recipientId));
                    assertTrue("one entitlement per player and reward key", recipients.add(recipientId));
                }
            }
        }
        assertEquals("the completed party quest should create one durable reward row per member",
            expectedRecipients.size(), rows);
        assertEquals(expectedRecipients, recipients);
    }

    private static void assertChoiceRewardEntitlements(Fixture fixture, QuestDefinition quest,
        Set<UUID> expectedRecipients) throws Exception {
        Set<UUID> recipients = new LinkedHashSet<UUID>();
        int rows = 0;
        try (Connection connection = fixture.scoped.getConnection(); PreparedStatement statement = connection
            .prepareStatement("SELECT participant_id,recipient_player_id,reward_type,"
                + "reward_parameters_json->>'item.count',reward_parameters_json->>'item.0.registryName',"
                + "reward_parameters_json->>'item.2.registryName',delivery_status "
                + "FROM galaxy_quest_reward_entitlement WHERE quest_id=? AND definition_version=? "
                + "AND reward_key='reward-1'")) {
            statement.setObject(1, quest.getId());
            statement.setInt(2, quest.getVersion());
            try (java.sql.ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows++;
                    UUID participant = result.getObject(1, UUID.class);
                    UUID recipient = result.getObject(2, UUID.class);
                    assertEquals(participant, recipient);
                    assertEquals("bq_standard:choice", result.getString(3));
                    assertEquals("3", result.getString(4));
                    assertEquals("minecraft:emerald", result.getString(5));
                    assertEquals("minecraft:gold_ingot", result.getString(6));
                    assertEquals("CLAIMABLE", result.getString(7));
                    assertTrue(expectedRecipients.contains(recipient));
                    assertTrue(recipients.add(recipient));
                }
            }
        }
        assertEquals("choice rewards are personal, not a shared PARTY selection", expectedRecipients.size(), rows);
        assertEquals(expectedRecipients, recipients);
    }

    private static void assertDeliveredItemEntitlements(Fixture fixture, QuestDefinition quest) throws Exception {
        try (Connection connection = fixture.scoped.getConnection(); PreparedStatement statement = connection
            .prepareStatement("SELECT COUNT(*) FROM galaxy_quest_reward_entitlement WHERE quest_id=? "
                + "AND definition_version=? AND reward_key=? AND reward_type='bq_standard:item' "
                + "AND delivery_status='DELIVERED' AND claimed_at IS NOT NULL AND delivered_at IS NOT NULL")) {
            statement.setObject(1, quest.getId());
            statement.setInt(2, quest.getVersion());
            statement.setString(3, "reward-0");
            try (java.sql.ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals("all PARTY item entitlements must be claimed and marked delivered", 3L,
                    result.getLong(1));
            }
        }
    }

    private static void assertDeliveredChoiceEntitlements(Fixture fixture, QuestDefinition quest,
        Map<UUID, Integer> expectedChoices) throws Exception {
        int rows = 0;
        try (Connection connection = fixture.scoped.getConnection(); PreparedStatement statement = connection
            .prepareStatement("SELECT e.participant_id,e.recipient_player_id,e.delivery_status,s.choice_index "
                + "FROM galaxy_quest_reward_entitlement e JOIN galaxy_quest_reward_choice_selection s "
                + "ON s.entitlement_key=e.entitlement_key WHERE e.quest_id=? AND e.definition_version=? "
                + "AND e.reward_key='reward-1'")) {
            statement.setObject(1, quest.getId());
            statement.setInt(2, quest.getVersion());
            try (java.sql.ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows++;
                    UUID participant = result.getObject(1, UUID.class);
                    UUID recipient = result.getObject(2, UUID.class);
                    assertEquals(participant, recipient);
                    assertEquals("DELIVERED", result.getString(3));
                    assertEquals(expectedChoices.get(recipient).intValue(), result.getInt(4));
                }
            }
        }
        assertEquals("each PARTY member needs an independent, delivered choice", expectedChoices.size(), rows);
    }

    private static void assertMinecraftRewardMarkers(Fixture fixture, QuestDefinition quest,
        Map<UUID, FakePlayerState> players) throws Exception {
        int rows = 0;
        try (Connection connection = fixture.scoped.getConnection(); PreparedStatement statement = connection
            .prepareStatement("SELECT participant_id,entitlement_key FROM galaxy_quest_reward_entitlement "
                + "WHERE quest_id=? AND definition_version=? AND reward_key IN ('reward-0','reward-1') "
                + "AND delivery_status='DELIVERED'")) {
            statement.setObject(1, quest.getId());
            statement.setInt(2, quest.getVersion());
            try (java.sql.ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows++;
                    UUID playerId = result.getObject(1, UUID.class);
                    FakePlayerState player = players.get(playerId);
                    assertNotNull("delivered PLAYER entitlement must map to a simulated online player", player);
                    String marker = QuestRewardMarker.key(result.getString(2));
                    assertTrue("the per-player persistent Minecraft marker must match its durable entitlement",
                        player.persisted.getCompoundTag(QuestRewardMarker.ROOT).getBoolean(marker));
                }
            }
        }
        assertEquals("each delivered item and choice entitlement must have one corresponding player marker",
            players.size() * 2, rows);
    }

    private static List<RewardDefinition> importedItemAndChoiceRewards(UUID questId) throws Exception {
        Path source = Files.createTempFile("bq-party-choice-rewards", ".json");
        String json = "{\"questIDHigh:4\":" + questId.getMostSignificantBits()
            + ",\"questIDLow:4\":" + questId.getLeastSignificantBits()
            + ",\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Imported PARTY reward integration\"}},"
            + "\"tasks:9\":{},\"rewards:9\":{"
            + "\"0:10\":{\"index:3\":0,\"rewardID:8\":\"bq_standard:item\",\"rewards:9\":{"
            + "\"0:10\":{\"id:8\":\"minecraft:diamond\",\"Damage:2\":0,\"Count:3\":2}}},"
            + "\"1:10\":{\"index:3\":1,\"rewardID:8\":\"bq_standard:choice\",\"choices:9\":{"
            + "\"0:10\":{\"id:8\":\"minecraft:emerald\",\"Damage:2\":0,\"Count:3\":5},"
            + "\"1:10\":{\"id:8\":\"minecraft:diamond\",\"Damage:2\":4,\"Count:3\":3,"
            + "\"tag:10\":{\"boundTo:8\":\"VAR_UUID\"}},"
            + "\"2:10\":{\"id:8\":\"minecraft:gold_ingot\",\"Damage:2\":0,\"Count:3\":4}}}}}";
        Files.write(source, json.getBytes(StandardCharsets.UTF_8));
        try {
            return new BqQuestDefinitionImporter().read(source).getDefinition().getRewards();
        } finally {
            Files.deleteIfExists(source);
        }
    }

    private static RewardDefinition importedQuestCompletionReward(UUID targetQuestId) throws Exception {
        Path source = Files.createTempFile("bq-questcompletion-reward", ".json");
        UUID sourceQuestId = UUID.randomUUID();
        byte[] targetBytes = ByteBuffer.allocate(16).putLong(targetQuestId.getMostSignificantBits())
            .putLong(targetQuestId.getLeastSignificantBits()).array();
        String compactTargetId = Base64.getUrlEncoder().withoutPadding().encodeToString(targetBytes);
        String json = "{\"questIDHigh:4\":" + sourceQuestId.getMostSignificantBits()
            + ",\"questIDLow:4\":" + sourceQuestId.getLeastSignificantBits()
            + ",\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Quest completion source\"}},"
            + "\"tasks:9\":{},\"rewards:9\":{\"0:10\":{\"index:3\":0,"
            + "\"rewardID:8\":\"bq_standard:questcompletion\",\"questID:8\":\""
            + compactTargetId + "\"}}}";
        Files.write(source, json.getBytes(StandardCharsets.UTF_8));
        try {
            return new BqQuestDefinitionImporter().read(source).getDefinition().getRewards().get(0);
        } finally {
            Files.deleteIfExists(source);
        }
    }

    private static void publish(Fixture fixture, JdbcQuestDefinitionRepository definitions, QuestDefinition definition) {
        StoredQuestDefinition draft = fixture.transaction.inTransaction(() -> definitions.createDraft(definition));
        assertTrue(fixture.transaction.inTransaction(() -> definitions.publish(definition.getId(),
            definition.getVersion(), draft.getContentHash(), 1000L)));
    }

    private static long countRewardEntitlements(Fixture fixture, UUID questId, String rewardKey) throws Exception {
        try (Connection connection = fixture.scoped.getConnection(); PreparedStatement statement = connection
            .prepareStatement("SELECT COUNT(*) FROM galaxy_quest_reward_entitlement WHERE quest_id=? "
                + "AND definition_version=1 AND reward_key=?")) {
            statement.setObject(1, questId);
            statement.setString(2, rewardKey);
            try (java.sql.ResultSet row = statement.executeQuery()) {
                assertTrue(row.next());
                return row.getLong(1);
            }
        }
    }

    private static int countItem(List<ItemStack> inventory, Item item) {
        int count = 0;
        for (ItemStack stack : inventory) if (stack != null && stack.getItem() == item) count += stack.stackSize;
        return count;
    }

    private static int countItem(List<ItemStack> inventory, Item item, int metadata) {
        int count = 0;
        for (ItemStack stack : inventory) {
            if (stack != null && stack.getItem() == item && stack.getItemDamage() == metadata) count += stack.stackSize;
        }
        return count;
    }

    private static final class FakePlayerState implements MinecraftItemRewardDeliveryHandler.PlayerState {
        private final String name;
        private final List<ItemStack> inventory;
        private final NBTTagCompound persisted = new NBTTagCompound();
        private int flushCount;

        private FakePlayerState(String name, int slots) {
            this.name = name;
            inventory = new ArrayList<ItemStack>(Collections.nCopies(slots, (ItemStack) null));
        }

        @Override public String name() { return name; }
        @Override public NBTTagCompound persisted() { return persisted; }
        @Override public List<ItemStack> snapshotMainInventory() { return copy(inventory); }
        @Override public void restoreMainInventory(List<ItemStack> values) {
            inventory.clear();
            inventory.addAll(copy(values));
        }
        @Override public void markInventoryDirty() {}
        @Override public void dropOverflow(ItemStack stack) { throw new AssertionError("unexpected overflow"); }
        @Override public void flush() { flushCount++; }

        private static List<ItemStack> copy(List<ItemStack> values) {
            List<ItemStack> result = new ArrayList<ItemStack>(values.size());
            for (ItemStack value : values) result.add(value == null ? null : value.copy());
            return result;
        }
    }

    private static final class Fixture {
        private final DataSource root;
        private final String schema;
        private final DataSource scoped;
        private final JdbcQuestConnectionManager manager;
        private final QuestTransaction transaction;
        private final JdbcBqPartyInventorySnapshotRepository snapshots;
        private final UUID owner = UUID.randomUUID();
        private final UUID peer = UUID.randomUUID();
        private final UUID staleMember = UUID.randomUUID();
        private final ParticipantId party = ParticipantId.party(UUID.randomUUID());
        private final RecordingSink recordingSink = new RecordingSink();

        private Fixture(DataSource root, String schema, DataSource scoped) {
            this.root = root;
            this.schema = schema;
            this.scoped = scoped;
            manager = new JdbcQuestConnectionManager(scoped);
            transaction = new JdbcQuestTransaction(manager);
            snapshots = new JdbcBqPartyInventorySnapshotRepository(manager);
        }

        private static Fixture open() throws Exception {
            String user = System.getenv("POSTGRES_USER");
            String password = System.getenv("POSTGRES_PASSWORD");
            String url = System.getenv("QUEST_TEST_POSTGRES_URL");
            Assume.assumeTrue("isolated PostgreSQL integration environment is not configured",
                user != null && password != null && url != null && !url.trim().isEmpty());
            if (!QuestTestDatabaseUrl.isExplicitlyIsolated(url, System.getenv("QUEST_TEST_POSTGRES_ISOLATED"))) {
                throw new IllegalStateException("Quest snapshot integration tests require an explicitly acknowledged *_test or *_it database URL");
            }
            Class.forName("org.postgresql.Driver");
            DriverManagerDataSource root = new DriverManagerDataSource(url, user, password);
            String schema = "bq_party_snapshot_it_" + UUID.randomUUID().toString().replace("-", "");
            try (Connection connection = root.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA \"" + schema + "\"");
            }
            String scopedUrl = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
            DataSource scoped = new DriverManagerDataSource(scopedUrl, user, password);
            Fixture fixture = new Fixture(root, schema, scoped);
            try {
                fixture.installSchema();
                fixture.createParty();
                return fixture;
            } catch (Exception failure) {
                fixture.close();
                throw failure;
            }
        }

        private void installSchema() throws Exception {
            InputStream input = getClass().getResourceAsStream(
                "/com/jsirgalaxybase/quest/postgres/quest-postgresql-ddl.sql");
            if (input == null) throw new IllegalStateException("quest PostgreSQL DDL resource is missing");
            try (InputStream source = input; ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = source.read(buffer)) >= 0) bytes.write(buffer, 0, count);
                try (Connection connection = scoped.getConnection(); Statement statement = connection.createStatement()) {
                    statement.execute(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                }
            }
        }

        private void createParty() {
            JdbcParticipantAdministrationRepository participants = new JdbcParticipantAdministrationRepository(manager);
            assertEquals(ParticipantMutationStatus.APPLIED, transaction.inTransaction(() ->
                participants.create(party, "Isolated inventory test", owner, 1000L)));
            assertEquals(ParticipantMutationStatus.APPLIED, transaction.inTransaction(() ->
                participants.addMember(party, owner, peer, ParticipantMemberRole.MEMBER, 0L, 2000L)));
            assertEquals(ParticipantMutationStatus.APPLIED, transaction.inTransaction(() ->
                participants.addMember(party, owner, staleMember, ParticipantMemberRole.MEMBER, 1L, 3000L)));
        }

        private BqPartyInventorySnapshotFactSink sink(QuestFactSink delegate) {
            ParticipantMembershipResolver memberships = new JdbcParticipantMembershipResolver(manager);
            QuestObservationPlanProvider plans = new QuestObservationPlanProvider() {
                @Override public QuestObservationPlan current() {
                    return new QuestObservationPlan(true, true, true, false, -1, false,
                        Collections.emptyList(), Collections.emptyList());
                }
            };
            QuestClock clock = new QuestClock() {
                @Override public long currentTimeMillis() { return 1000L; }
            };
            return new BqPartyInventorySnapshotFactSink(delegate, transaction, snapshots, memberships,
                new MinecraftQuestFactFactory("s2"), plans, clock);
        }

        private GameplayFact inventory(UUID player, UUID session, long amount) {
            return inventory(player, session, session.toString(), Collections.singletonList(
                new InventoryObservationEntry("minecraft:iron_ingot", 0, amount, "ingotIron", "", "", false)));
        }

        private GameplayFact inventory(UUID player, UUID session, String eventSuffix,
            List<InventoryObservationEntry> entries) {
            Map<String, String> attributes = new LinkedHashMap<String, String>(new InventoryObservationAttributeCodec()
                .encode(entries));
            attributes.put("inventory.sessionId", session.toString());
            return new GameplayFact("s2", "inventory-" + eventSuffix, player, "galaxy:inventory_observed",
                1000L, attributes);
        }

        private GameplayFact fluidInventory(UUID player, UUID session, long amount) {
            Map<String, String> attributes = fluidAttributes(amount);
            attributes.put("inventory.sessionId", session.toString());
            return new GameplayFact("s2", "fluid-inventory-" + UUID.randomUUID(), player,
                "galaxy:inventory_observed", 1000L, attributes);
        }

        private void expireLease(UUID playerId) throws Exception {
            try (Connection connection = scoped.getConnection(); PreparedStatement statement = connection
                .prepareStatement("UPDATE galaxy_quest_active_inventory_snapshot "
                    + "SET observed_at=clock_timestamp() - INTERVAL '2 seconds', "
                    + "expires_at=clock_timestamp() - INTERVAL '1 second' WHERE player_id=?")) {
                statement.setObject(1, playerId);
                assertEquals(1, statement.executeUpdate());
            }
        }

        private void close() throws Exception {
            try (Connection connection = root.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE");
            }
        }
    }

    private static final class RecordingSink implements QuestFactSink {
        private final List<GameplayFact> facts = new ArrayList<GameplayFact>();
        @Override public void accept(GameplayFact fact) { facts.add(fact); }
    }

    private static final class DriverManagerDataSource implements DataSource {
        private final String url;
        private final String user;
        private final String password;
        private DriverManagerDataSource(String url, String user, String password) {
            this.url = url;
            this.user = user;
            this.password = password;
        }
        @Override public Connection getConnection() throws java.sql.SQLException {
            return DriverManager.getConnection(url, user, password);
        }
        @Override public Connection getConnection(String username, String suppliedPassword) throws java.sql.SQLException {
            return DriverManager.getConnection(url, username, suppliedPassword);
        }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() { return Logger.getGlobal(); }
        @Override public <T> T unwrap(Class<T> iface) throws java.sql.SQLException {
            if (iface.isInstance(this)) return iface.cast(this);
            throw new java.sql.SQLException("not a wrapper for " + iface.getName());
        }
        @Override public boolean isWrapperFor(Class<?> iface) { return iface.isInstance(this); }
    }
}
