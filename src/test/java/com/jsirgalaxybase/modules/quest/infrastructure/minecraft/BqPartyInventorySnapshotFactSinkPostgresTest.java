package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.jsirgalaxybase.quest.core.GameplayFact;
import com.jsirgalaxybase.quest.core.BqCompatibleTaskEvaluatorRegistry;
import com.jsirgalaxybase.quest.core.BqHuntPartyAssignmentPolicy;
import com.jsirgalaxybase.quest.core.InventoryObservationAttributeCodec;
import com.jsirgalaxybase.quest.core.InventoryObservationEntry;
import com.jsirgalaxybase.quest.core.FactProcessingStatus;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.ParticipantMemberRole;
import com.jsirgalaxybase.quest.core.ParticipantMembershipResolver;
import com.jsirgalaxybase.quest.core.ParticipantMutationStatus;
import com.jsirgalaxybase.quest.core.PortableNbt;
import com.jsirgalaxybase.quest.core.QuestBehavior;
import com.jsirgalaxybase.quest.core.QuestClock;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestDefinitionLifecycle;
import com.jsirgalaxybase.quest.core.QuestEngine;
import com.jsirgalaxybase.quest.core.QuestEvaluationPlanner;
import com.jsirgalaxybase.quest.core.QuestFactProjector;
import com.jsirgalaxybase.quest.core.QuestLogic;
import com.jsirgalaxybase.quest.core.QuestObservationPlan;
import com.jsirgalaxybase.quest.core.QuestObservationPlanProvider;
import com.jsirgalaxybase.quest.core.QuestProgressSnapshot;
import com.jsirgalaxybase.quest.core.QuestRuntimeService;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.QuestTransaction;
import com.jsirgalaxybase.quest.core.RewardClaimStatus;
import com.jsirgalaxybase.quest.core.RewardChoiceSelectionStatus;
import com.jsirgalaxybase.quest.core.RewardDeliveryBatchResult;
import com.jsirgalaxybase.quest.core.RewardDeliveryRouter;
import com.jsirgalaxybase.quest.core.RewardDeliveryWorker;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.RepeatPolicy;
import com.jsirgalaxybase.quest.core.StoredQuestDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.quest.postgres.JdbcBqPartyInventorySnapshotRepository;
import com.jsirgalaxybase.quest.postgres.JdbcParticipantAdministrationRepository;
import com.jsirgalaxybase.quest.postgres.JdbcParticipantMembershipResolver;
import com.jsirgalaxybase.quest.postgres.JdbcQuestCompletionQuery;
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
            Map<String, String> rewardParameters = new LinkedHashMap<String, String>();
            rewardParameters.put("ignoreDisabled", "false");
            rewardParameters.put("item.count", "1");
            rewardParameters.put("item.0.registryName", "minecraft:diamond");
            rewardParameters.put("item.0.meta", "0");
            rewardParameters.put("item.0.amount", "2");
            rewardParameters.put("item.0.oreDictionary", "");
            rewardParameters.put("item.0.nbt", "{}");
            rewardParameters.put("item.0.nbtPortable", PortableNbt
                .compound(Collections.<String, PortableNbt>emptyMap()).encode());
            rewardParameters.put("bq.raw", "{}");
            RewardDefinition itemReward = new RewardDefinition("reward-0", "bq_standard:item", rewardParameters);
            Map<String, String> choiceParameters = new LinkedHashMap<String, String>();
            choiceParameters.put("ignoreDisabled", "false");
            choiceParameters.put("selectionMode", "player-required");
            choiceParameters.put("item.count", "3");
            addChoice(choiceParameters, 0, "minecraft:emerald", 5);
            addChoice(choiceParameters, 1, "minecraft:diamond", 3);
            addChoice(choiceParameters, 2, "minecraft:gold_ingot", 4);
            choiceParameters.put("bq.raw", "{}");
            RewardDefinition choiceReward = new RewardDefinition("reward-1", "bq_standard:choice",
                choiceParameters);
            QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "BQ PARTY Retrieval Integration", "",
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
            final Item diamond = new Item();
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
                    countItem(player.inventory, selectedItem) - (selectedItem == diamond ? 2 : 0));
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

    private static void addChoice(Map<String, String> parameters, int index, String registryName, int amount) {
        String prefix = "item." + index + ".";
        parameters.put(prefix + "registryName", registryName);
        parameters.put(prefix + "meta", "0");
        parameters.put(prefix + "amount", Integer.toString(amount));
        parameters.put(prefix + "oreDictionary", "");
        parameters.put(prefix + "nbt", "{}");
        parameters.put(prefix + "nbtPortable", PortableNbt
            .compound(Collections.<String, PortableNbt>emptyMap()).encode());
    }

    private static int countItem(List<ItemStack> inventory, Item item) {
        int count = 0;
        for (ItemStack stack : inventory) if (stack != null && stack.getItem() == item) count += stack.stackSize;
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
            String database = System.getenv("POSTGRES_DB");
            String url = System.getenv("QUEST_TEST_POSTGRES_URL");
            Assume.assumeTrue("isolated PostgreSQL integration environment is not configured",
                user != null && password != null && database != null && url != null && !url.trim().isEmpty());
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
                    return new QuestObservationPlan(true, true, false, false, -1, false,
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
