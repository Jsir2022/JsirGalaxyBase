package com.jsirgalaxybase.quest.postgres;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.jsirgalaxybase.quest.core.GameplayFact;
import com.jsirgalaxybase.quest.core.ConsumptionRequest;
import com.jsirgalaxybase.quest.core.ConsumptionSubmission;
import com.jsirgalaxybase.quest.core.ConsumptionSubmissionStatus;
import com.jsirgalaxybase.quest.core.AuthenticatedQuestClaimService;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestCompletionRewardPort;
import com.jsirgalaxybase.quest.core.QuestCompletionRewardDeliveryHandler;
import com.jsirgalaxybase.quest.core.QuestDefinitionConflictException;
import com.jsirgalaxybase.quest.core.QuestDefinitionLifecycle;
import com.jsirgalaxybase.quest.core.QuestLogic;
import com.jsirgalaxybase.quest.core.QuestProgressSnapshot;
import com.jsirgalaxybase.quest.core.QuestEvaluationRequest;
import com.jsirgalaxybase.quest.core.QuestEvaluationPlanner;
import com.jsirgalaxybase.quest.core.QuestEngine;
import com.jsirgalaxybase.quest.core.QuestFactProjector;
import com.jsirgalaxybase.quest.core.BqHuntPartyAssignmentPolicy;
import com.jsirgalaxybase.quest.core.BqCompatibleTaskEvaluatorRegistry;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.QuestRuntimeService;
import com.jsirgalaxybase.quest.core.FactCountTaskEvaluator;
import com.jsirgalaxybase.quest.core.FactProcessingResult;
import com.jsirgalaxybase.quest.core.FactProcessingStatus;
import com.jsirgalaxybase.quest.core.ParticipantMembership;
import com.jsirgalaxybase.quest.core.ParticipantMemberRole;
import com.jsirgalaxybase.quest.core.ParticipantMutationStatus;
import com.jsirgalaxybase.quest.core.ParticipantType;
import com.jsirgalaxybase.quest.core.QuestBehavior;
import com.jsirgalaxybase.quest.core.QuestParticipantScope;
import com.jsirgalaxybase.quest.core.RepeatPolicy;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.RewardDeliveryLease;
import com.jsirgalaxybase.quest.core.RewardDeliveryBatchResult;
import com.jsirgalaxybase.quest.core.RewardDeliveryWorker;
import com.jsirgalaxybase.quest.core.RewardChoiceSelectionStatus;
import com.jsirgalaxybase.quest.core.RewardClaimStatus;
import com.jsirgalaxybase.quest.core.RewardEntitlement;
import com.jsirgalaxybase.quest.core.RewardSuppressionPolicy;
import com.jsirgalaxybase.quest.core.TaskProgress;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.quest.core.TaskEvaluatorRegistry;
import com.jsirgalaxybase.quest.core.QuestChapterCatalogEntry;
import com.jsirgalaxybase.quest.core.StoredQuestDefinition;

public class QuestPostgresIntegrationTest {
    private DriverManagerQuestDataSource root;
    private DriverManagerQuestDataSource scoped;
    private String schema;

    @Before
    public void setUp() throws Exception {
        String user = System.getenv("POSTGRES_USER");
        String password = System.getenv("POSTGRES_PASSWORD");
        String database = System.getenv("POSTGRES_DB");
        Assume.assumeTrue(user != null && password != null && database != null);
        Class.forName("org.postgresql.Driver");
        String baseUrl = System.getenv("QUEST_TEST_POSTGRES_URL");
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = "jdbc:postgresql://galaxy-base:5432/" + database;
        }
        root = new DriverManagerQuestDataSource(baseUrl, user, password);
        schema = "quest_it_" + UUID.randomUUID().toString().replace("-", "");
        try {
            try (Connection connection = root.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA \"" + schema + "\"");
            }
        } catch (SQLException unavailable) {
            schema = null;
            Assume.assumeNoException("configured PostgreSQL is unavailable", unavailable);
        }
        scoped = new DriverManagerQuestDataSource(baseUrl + "?currentSchema=" + schema, user, password);
        try (Connection connection = scoped.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(ddl());
        }
    }

    @After
    public void tearDown() throws Exception {
        if (root == null || schema == null) return;
        try (Connection connection = root.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE");
        }
    }

    @Test
    public void currentSchemaSupportsSuppressedRewardStatus() {
        new JdbcQuestCenterQuery(new JdbcQuestConnectionManager(scoped)).validateSuppressedRewardStatus();
    }

    @Test
    public void completionQueryPreservesBqPrerequisiteUnlockAcrossRepeatCycleReset() {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcQuestCompletionQuery completions = new JdbcQuestCompletionQuery(manager);
        ParticipantId player = ParticipantId.player(UUID.randomUUID());
        UUID questId = UUID.randomUUID();

        transaction.inTransaction(() -> {
            runtime.saveProgress(new QuestProgressSnapshot(player, questId, 1, QuestStatus.COMPLETED,
                Collections.<String, TaskProgress>emptyMap(), 100L, 0));
            return null;
        });
        assertTrue(completions.findCompletedQuestIds(player).contains(questId));

        transaction.inTransaction(() -> {
            runtime.saveProgress(new QuestProgressSnapshot(player, questId, 1, QuestStatus.IN_PROGRESS,
                Collections.<String, TaskProgress>emptyMap(), 0L, 1));
            return null;
        });
        assertTrue("BQ isComplete remains true after a repeat-cycle reset",
            completions.findCompletedQuestIds(player).contains(questId));
    }

    @Test
    public void lockedProgressQueryLoadsOnlyCurrentPlayersLockedVectorsUnderTransaction() {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        ParticipantId player = ParticipantId.player(UUID.randomUUID());
        ParticipantId other = ParticipantId.player(UUID.randomUUID());
        UUID lockedQuest = UUID.randomUUID();
        UUID availableQuest = UUID.randomUUID();
        TaskProgress completedTask = new TaskProgress("craft", Arrays.asList(1L, 3L), Arrays.asList(1L, 3L), true);
        transaction.inTransaction(() -> {
            runtime.saveProgress(new QuestProgressSnapshot(player, lockedQuest, 2, QuestStatus.LOCKED,
                Collections.singletonMap("craft", completedTask), 0L, 0));
            runtime.saveProgress(new QuestProgressSnapshot(player, availableQuest, 1, QuestStatus.AVAILABLE,
                Collections.<String, TaskProgress>emptyMap(), 0L, 0));
            runtime.saveProgress(new QuestProgressSnapshot(other, UUID.randomUUID(), 1, QuestStatus.LOCKED,
                Collections.<String, TaskProgress>emptyMap(), 0L, 0));
            return null;
        });

        List<QuestProgressSnapshot> locked = transaction.inTransaction(() -> runtime.findLockedProgress(player));

        assertEquals(1, locked.size());
        assertEquals(lockedQuest, locked.get(0).getQuestId());
        assertEquals(2, locked.get(0).getDefinitionVersion());
        assertEquals(Arrays.asList(1L, 3L), locked.get(0).getTasks().get("craft").getValues());
        assertTrue(locked.get(0).getTasks().get("craft").isComplete());
    }

    @Test
    public void suppressedRewardStatusGuardRejectsSchemaBeforeMigration() throws Exception {
        try (Connection connection = scoped.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE galaxy_quest_reward_entitlement "
                + "DROP CONSTRAINT galaxy_quest_reward_entitlement_delivery_status_check");
        }
        try {
            new JdbcQuestCenterQuery(new JdbcQuestConnectionManager(scoped)).validateSuppressedRewardStatus();
            fail("expected a missing-migration diagnostic");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("20260929_001_add_quest_reward_suppressed_status.sql"));
        }
    }

    @Test
    public void scopedMembershipFreezesRecipientsAndSurvivesLeaveAndLaterJoin() throws Exception {
        JdbcQuestConnectionManager manager=new JdbcQuestConnectionManager(scoped);JdbcQuestTransaction transaction=new JdbcQuestTransaction(manager);
        JdbcParticipantAdministrationRepository administration=new JdbcParticipantAdministrationRepository(manager);
        JdbcParticipantMembershipResolver memberships=new JdbcParticipantMembershipResolver(manager);
        JdbcQuestDefinitionRepository definitions=new JdbcQuestDefinitionRepository(manager);JdbcQuestRuntimeRepository runtime=new JdbcQuestRuntimeRepository(manager);
        JdbcRewardClaimRepository claims=new JdbcRewardClaimRepository(manager);JdbcRewardDeliveryRepository delivery=new JdbcRewardDeliveryRepository(manager);
        UUID owner=UUID.randomUUID(),member=UUID.randomUUID(),late=UUID.randomUUID(),teamId=UUID.randomUUID(),questId=UUID.randomUUID();
        ParticipantId team=new ParticipantId(ParticipantType.TEAM,teamId);
        assertEquals(ParticipantMutationStatus.APPLIED,transaction.inTransaction(()->administration.create(team,"Builders",owner,1000L)));
        assertEquals(ParticipantMutationStatus.APPLIED,transaction.inTransaction(()->administration.addMember(team,owner,member,ParticipantMemberRole.MEMBER,0L,2000L)));
        assertEquals(2,memberships.resolve(owner).get(1).getPlayerIds().size());
        com.jsirgalaxybase.quest.core.ParticipantDirectoryEntry ownerDirectory=
            new JdbcParticipantDirectoryQuery(manager).findActiveForPlayer(owner).get(0);
        assertEquals(team,ownerDirectory.getParticipantId());assertEquals(ParticipantMemberRole.OWNER,ownerDirectory.getRole());
        assertEquals(1L,ownerDirectory.getRevision());assertEquals(2,ownerDirectory.getMemberCount());
        RewardDefinition reward=new RewardDefinition("item","bq_standard:item",Collections.singletonMap("item.count","2"));
        QuestDefinition quest=new QuestDefinition(questId,1,"Team quest","",QuestLogic.AND,QuestLogic.AND,Collections.<UUID>emptySet(),Collections.<TaskDefinition>emptyList(),Collections.singletonList(reward),RepeatPolicy.never(),QuestBehavior.builder().participantScope(QuestParticipantScope.TEAM).build());
        transaction.inTransaction(()->{com.jsirgalaxybase.quest.core.StoredQuestDefinition draft=definitions.createDraft(quest);assertTrue(definitions.publish(questId,1,draft.getContentHash(),1L));
            runtime.saveProgress(new QuestProgressSnapshot(team,questId,1,QuestStatus.COMPLETED,Collections.<String,TaskProgress>emptyMap(),3L,0));
            runtime.insertEntitlementsIfAbsent(new java.util.LinkedHashSet<RewardEntitlement>(Arrays.asList(new RewardEntitlement(team,owner,quest,reward,0,false),new RewardEntitlement(team,member,quest,reward,0,false))));return null;});
        assertEquals(ParticipantMutationStatus.APPLIED,transaction.inTransaction(()->administration.removeMember(team,member,member,1L,3000L)));
        assertEquals(ParticipantMutationStatus.APPLIED,transaction.inTransaction(()->administration.addMember(team,owner,late,ParticipantMemberRole.MEMBER,2L,4000L)));
        assertTrue(memberships.resolve(member).size()==1);assertEquals(2,memberships.resolve(late).get(1).getPlayerIds().size());
        com.jsirgalaxybase.quest.core.QuestCenterSnapshot departedCenter=new JdbcQuestCenterQuery(manager).loadForPlayer(member);
        assertNull(departedCenter.getQuests().get(0).getProgress());
        assertEquals(1,departedCenter.getQuests().get(0).getRewards().getClaimable());
        assertEquals(team,departedCenter.getQuests().get(0).getRewards().getSourceParticipant());
        assertEquals(0,departedCenter.getQuests().get(0).getRewards().getCycle());
        assertEquals(RewardClaimStatus.CLAIMED,transaction.inTransaction(()->claims.claim(questId,1,0,member,5000L)));
        assertEquals(RewardClaimStatus.NOT_OWNED,transaction.inTransaction(()->claims.claim(questId,1,0,late,5000L)));
        RewardDeliveryLease lease=transaction.inTransaction(()->delivery.leaseAvailable("worker",5001L,6000L,10)).get(0);
        assertEquals(ParticipantId.player(member),lease.getParticipantId());assertEquals(team,lease.getProgressParticipantId());
        com.jsirgalaxybase.quest.core.QuestCenterSnapshot ownerCenter=new JdbcQuestCenterQuery(manager).loadForPlayer(owner);
        assertEquals(team,ownerCenter.getQuests().get(0).getProgress().getParticipantId());assertEquals(1,ownerCenter.getQuests().get(0).getRewards().getClaimable());
        com.jsirgalaxybase.quest.core.QuestCenterSnapshot lateCenter=new JdbcQuestCenterQuery(manager).loadForPlayer(late);
        assertEquals(team,lateCenter.getQuests().get(0).getProgress().getParticipantId());assertEquals(0,lateCenter.getQuests().get(0).getRewards().getTotal());
    }

    @Test
    public void concurrentBqHuntFactsUseDatabasePartyMembershipAndCompletePersonalProgress() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcParticipantAdministrationRepository administration = new JdbcParticipantAdministrationRepository(manager);
        JdbcParticipantMembershipResolver memberships = new JdbcParticipantMembershipResolver(manager);
        JdbcQuestRuntimeRepository runtimeRepository = new JdbcQuestRuntimeRepository(manager);
        UUID hunter = UUID.randomUUID(), partyMember = UUID.randomUUID(), partyUuid = UUID.randomUUID();
        ParticipantId partyId = ParticipantId.party(partyUuid);
        assertEquals(ParticipantMutationStatus.APPLIED, transaction.inTransaction(() -> administration.create(
            partyId, "Hunt party", hunter, 1000L)));
        assertEquals(ParticipantMutationStatus.APPLIED, transaction.inTransaction(() -> administration.addMember(
            partyId, hunter, partyMember, ParticipantMemberRole.MEMBER, 0L, 1100L)));

        Map<String, String> taskParameters = new LinkedHashMap<String, String>();
        taskParameters.put("target", "2");
        taskParameters.put("factType", "galaxy:entity_killed");
        taskParameters.put("subject", "Zombie");
        taskParameters.put("subtypes", "true");
        TaskDefinition hunt = new TaskDefinition("hunt", "bq_standard:hunt", false, taskParameters);
        RewardDefinition reward = new RewardDefinition("reward", "bq_standard:item",
            Collections.singletonMap("item.0.registryName", "minecraft:diamond"));
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "BQ Hunt", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(hunt),
            Collections.singletonList(reward), RepeatPolicy.never());
        StoredQuestDefinition published = new StoredQuestDefinition(quest, QuestDefinitionLifecycle.PUBLISHED,
            "hunt-hash", 1L);
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(() -> Collections.singletonList(published),
            new JdbcQuestCompletionQuery(manager), memberships, new BqHuntPartyAssignmentPolicy(),
            (definition, fact, partyMembership) -> true);
        QuestRuntimeService runtime = new QuestRuntimeService(runtimeRepository, transaction,
            new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
        GameplayFact hunterKill = new GameplayFact("s2", "bq-hunt-party:" + UUID.randomUUID(), hunter,
            "galaxy:entity_killed", 1200L, attributes("Zombie", "1"));
        GameplayFact memberKill = new GameplayFact("lobby", "bq-hunt-party:" + UUID.randomUUID(), partyMember,
            "galaxy:entity_killed", 1201L, attributes("Zombie", "1"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        FactProcessingResult hunterResult;
        FactProcessingResult memberResult;
        try {
            Future<FactProcessingResult> hunterFuture = executor.submit(() -> runtime.apply(hunterKill, planner, 1200L));
            Future<FactProcessingResult> memberFuture = executor.submit(() -> runtime.apply(memberKill, planner, 1201L));
            hunterResult = hunterFuture.get();
            memberResult = memberFuture.get();
        } finally {
            executor.shutdownNow();
        }

        assertEquals(FactProcessingStatus.ACCEPTED, hunterResult.getStatus());
        assertEquals(FactProcessingStatus.ACCEPTED, memberResult.getStatus());
        assertEquals(2, hunterResult.getEvaluations().size());
        assertEquals(2, memberResult.getEvaluations().size());
        for (UUID player : Arrays.asList(hunter, partyMember)) {
            QuestProgressSnapshot progress = runtimeRepository.findProgress(ParticipantId.player(player),
                quest.getId(), quest.getVersion()).get();
            assertEquals(QuestStatus.COMPLETED, progress.getStatus());
        }
        assertEquals(2L, scalar("SELECT COUNT(*) FROM galaxy_quest_reward_entitlement WHERE quest_id='"
            + quest.getId() + "'"));
        assertEquals(FactProcessingStatus.DUPLICATE, runtime.apply(hunterKill, planner, 1300L).getStatus());
        assertEquals(2L, scalar("SELECT COUNT(*) FROM galaxy_quest_reward_entitlement WHERE quest_id='"
            + quest.getId() + "'"));
    }

    @Test
    public void bqMeetingObservationUsesDatabasePartyMembershipForPersonalProgress() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcParticipantAdministrationRepository administration = new JdbcParticipantAdministrationRepository(manager);
        JdbcParticipantMembershipResolver memberships = new JdbcParticipantMembershipResolver(manager);
        JdbcQuestRuntimeRepository runtimeRepository = new JdbcQuestRuntimeRepository(manager);
        UUID observer = UUID.randomUUID(), partyMember = UUID.randomUUID(), partyUuid = UUID.randomUUID();
        ParticipantId partyId = ParticipantId.party(partyUuid);
        assertEquals(ParticipantMutationStatus.APPLIED, transaction.inTransaction(() -> administration.create(
            partyId, "Meeting party", observer, 1000L)));
        assertEquals(ParticipantMutationStatus.APPLIED, transaction.inTransaction(() -> administration.addMember(
            partyId, observer, partyMember, ParticipantMemberRole.MEMBER, 0L, 1100L)));

        Map<String, String> taskParameters = new LinkedHashMap<String, String>();
        taskParameters.put("factType", "galaxy:nearby_entities_observed");
        taskParameters.put("subject", "Villager");
        taskParameters.put("range", "4");
        taskParameters.put("amount", "1");
        taskParameters.put("subtypes", "true");
        taskParameters.put("ignoreNBT", "true");
        taskParameters.put("bq.raw", "{}");
        TaskDefinition meeting = new TaskDefinition("meeting", "bq_standard:meeting", false, taskParameters);
        RewardDefinition reward = new RewardDefinition("reward", "bq_standard:item",
            Collections.<String, String>emptyMap());
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "BQ Meeting", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(meeting),
            Collections.singletonList(reward), RepeatPolicy.never(),
            QuestBehavior.builder().participantScope(QuestParticipantScope.PLAYER).build());
        StoredQuestDefinition published = new StoredQuestDefinition(quest, QuestDefinitionLifecycle.PUBLISHED,
            "meeting-hash", 1L);
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(() -> Collections.singletonList(published),
            new JdbcQuestCompletionQuery(manager), memberships, new BqHuntPartyAssignmentPolicy(),
            (definition, fact, partyMembership) -> true);
        QuestRuntimeService runtime = new QuestRuntimeService(runtimeRepository, transaction,
            new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
        Map<String, String> observationAttributes = new LinkedHashMap<String, String>();
        observationAttributes.put("observedRadius", "4");
        observationAttributes.put("nearbyEntity.count", "1");
        observationAttributes.put("nearbyEntity.0.subject", "Villager");
        GameplayFact observation = new GameplayFact("s2", "bq-meeting-party:" + UUID.randomUUID(), observer,
            "galaxy:nearby_entities_observed", 1200L, observationAttributes);

        FactProcessingResult result = runtime.apply(observation, planner, 1200L);

        assertEquals(FactProcessingStatus.ACCEPTED, result.getStatus());
        assertEquals(2, result.getEvaluations().size());
        for (UUID player : Arrays.asList(observer, partyMember)) {
            QuestProgressSnapshot progress = runtimeRepository.findProgress(ParticipantId.player(player),
                quest.getId(), quest.getVersion()).get();
            assertEquals(QuestStatus.COMPLETED, progress.getStatus());
            assertTrue(progress.getTasks().get("meeting").isComplete());
        }
        assertEquals(2L, scalar("SELECT COUNT(*) FROM galaxy_quest_reward_entitlement WHERE quest_id='"
            + quest.getId() + "'"));
        assertEquals(FactProcessingStatus.DUPLICATE, runtime.apply(observation, planner, 1300L).getStatus());
        assertEquals(2L, scalar("SELECT COUNT(*) FROM galaxy_quest_reward_entitlement WHERE quest_id='"
            + quest.getId() + "'"));
    }

    @Test
    public void bqNonGroupDetectRetrievalUsesCurrentDatabasePartySnapshot() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcParticipantAdministrationRepository administration = new JdbcParticipantAdministrationRepository(manager);
        JdbcParticipantMembershipResolver memberships = new JdbcParticipantMembershipResolver(manager);
        JdbcQuestRuntimeRepository runtimeRepository = new JdbcQuestRuntimeRepository(manager);
        UUID owner = UUID.randomUUID(), member = UUID.randomUUID(), partyUuid = UUID.randomUUID();
        ParticipantId party = ParticipantId.party(partyUuid);
        assertEquals(ParticipantMutationStatus.APPLIED, transaction.inTransaction(() -> administration.create(
            party, "Retrieval party", owner, 1000L)));
        assertEquals(ParticipantMutationStatus.APPLIED, transaction.inTransaction(() -> administration.addMember(
            party, owner, member, ParticipantMemberRole.MEMBER, 0L, 1100L)));

        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("factType", "galaxy:inventory_observed");
        parameters.put("consume", "false");
        parameters.put("groupDetect", "false");
        parameters.put("progressMode", "item-vector");
        parameters.put("item.count", "2");
        parameters.put("item.0.registryName", "minecraft:iron_ingot");
        parameters.put("item.0.amount", "2");
        parameters.put("item.1.registryName", "minecraft:gold_ingot");
        parameters.put("item.1.amount", "2");
        parameters.put("bq.raw", "{}");
        TaskDefinition retrieval = new TaskDefinition("retrieve", "bq_standard:retrieval", false, parameters);
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "BQ Retrieval", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(retrieval),
            Collections.<RewardDefinition>emptyList(), RepeatPolicy.never(),
            QuestBehavior.builder().participantScope(QuestParticipantScope.PLAYER).build());
        StoredQuestDefinition published = new StoredQuestDefinition(quest, QuestDefinitionLifecycle.PUBLISHED,
            "retrieval-hash", 1L);
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(() -> Collections.singletonList(published),
            new JdbcQuestCompletionQuery(manager), memberships, new BqHuntPartyAssignmentPolicy(),
            (definition, fact, partyMembership) -> true);
        QuestRuntimeService runtime = new QuestRuntimeService(runtimeRepository, transaction,
            new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());

        transaction.inTransaction(() -> {
            runtimeRepository.saveProgress(new QuestProgressSnapshot(ParticipantId.player(owner), quest.getId(), 1,
                QuestStatus.IN_PROGRESS, Collections.singletonMap("retrieve", new TaskProgress("retrieve",
                    Arrays.asList(2L, 0L), Arrays.asList(2L, 2L), false)), 0L, 0));
            runtimeRepository.saveProgress(new QuestProgressSnapshot(ParticipantId.player(member), quest.getId(), 1,
                QuestStatus.IN_PROGRESS, Collections.singletonMap("retrieve", new TaskProgress("retrieve",
                    Arrays.asList(0L, 2L), Arrays.asList(2L, 2L), false)), 0L, 0));
            return null;
        });

        Map<String, String> inventory = new LinkedHashMap<String, String>();
        inventory.put("inventory.bqPartySnapshot", "true");
        inventory.put("inventory.bqPartyId", partyUuid.toString());
        inventory.put("inventory.count", "2");
        inventory.put("inventory.0.registryName", "minecraft:iron_ingot");
        inventory.put("inventory.0.meta", "0");
        inventory.put("inventory.0.amount", "2");
        inventory.put("inventory.1.registryName", "minecraft:gold_ingot");
        inventory.put("inventory.1.meta", "0");
        inventory.put("inventory.1.amount", "2");
        GameplayFact snapshot = new GameplayFact("s2", "bq-retrieval-party:" + UUID.randomUUID(), owner,
            "galaxy:inventory_observed", 1200L, inventory);

        FactProcessingResult result = runtime.apply(snapshot, planner, 1200L);

        assertEquals(FactProcessingStatus.ACCEPTED, result.getStatus());
        assertEquals(2, result.getEvaluations().size());
        for (UUID player : Arrays.asList(owner, member)) {
            QuestProgressSnapshot progress = runtimeRepository.findProgress(ParticipantId.player(player),
                quest.getId(), quest.getVersion()).get();
            assertEquals(QuestStatus.COMPLETED, progress.getStatus());
            assertEquals(Arrays.asList(2L, 2L), progress.getTasks().get("retrieve").getValues());
        }
        assertEquals(FactProcessingStatus.DUPLICATE, runtime.apply(snapshot, planner, 1300L).getStatus());
    }

    @Test
    public void exactRewardTargetDoesNotClaimAnotherGroupWithTheSameCycle() throws Exception {
        JdbcQuestConnectionManager manager=new JdbcQuestConnectionManager(scoped);JdbcQuestTransaction transaction=new JdbcQuestTransaction(manager);
        JdbcQuestDefinitionRepository definitions=new JdbcQuestDefinitionRepository(manager);JdbcQuestRuntimeRepository runtime=new JdbcQuestRuntimeRepository(manager);
        JdbcRewardClaimRepository claims=new JdbcRewardClaimRepository(manager);UUID player=UUID.randomUUID(),questId=UUID.randomUUID();
        ParticipantId first=new ParticipantId(ParticipantType.TEAM,UUID.fromString("00000000-0000-0000-0000-000000000001"));
        ParticipantId second=new ParticipantId(ParticipantType.TEAM,UUID.fromString("00000000-0000-0000-0000-000000000002"));
        RewardDefinition reward=new RewardDefinition("item","bq_standard:item",Collections.singletonMap("item.count","1"));
        QuestDefinition quest=new QuestDefinition(questId,1,"Shared reward","",QuestLogic.AND,QuestLogic.AND,
            Collections.<UUID>emptySet(),Collections.<TaskDefinition>emptyList(),Collections.singletonList(reward),RepeatPolicy.never(),
            QuestBehavior.builder().participantScope(QuestParticipantScope.TEAM).build());
        transaction.inTransaction(()->{com.jsirgalaxybase.quest.core.StoredQuestDefinition draft=definitions.createDraft(quest);
            assertTrue(definitions.publish(questId,1,draft.getContentHash(),1L));runtime.insertEntitlementsIfAbsent(
                new java.util.LinkedHashSet<RewardEntitlement>(Arrays.asList(new RewardEntitlement(first,player,quest,reward,0,false),
                    new RewardEntitlement(second,player,quest,reward,0,false))));return null;});
        com.jsirgalaxybase.quest.core.RewardClaimTarget target=claims.findNextRecipientTarget(questId,1,player).get();
        assertEquals(first,target.getParticipantId());
        assertEquals(RewardClaimStatus.CLAIMED,transaction.inTransaction(()->claims.claim(target,questId,1,player,10L)));
        try(Connection connection=scoped.getConnection();PreparedStatement statement=connection.prepareStatement(
            "SELECT participant_id,delivery_status FROM galaxy_quest_reward_entitlement WHERE recipient_player_id=? ORDER BY participant_id")){
            statement.setObject(1,player);try(ResultSet rows=statement.executeQuery()){assertTrue(rows.next());assertEquals(first.getId(),rows.getObject(1));assertEquals("PENDING",rows.getString(2));
                assertTrue(rows.next());assertEquals(second.getId(),rows.getObject(1));assertEquals("CLAIMABLE",rows.getString(2));assertFalse(rows.next());}}
    }

    @Test
    public void ownershipTransferIsAtomicAndOldOwnerCannotAdministerAfterward() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcParticipantAdministrationRepository administration = new JdbcParticipantAdministrationRepository(manager);
        JdbcParticipantDirectoryQuery directory = new JdbcParticipantDirectoryQuery(manager);
        UUID oldOwner = UUID.randomUUID(), newOwner = UUID.randomUUID();
        ParticipantId party = new ParticipantId(ParticipantType.PARTY, UUID.randomUUID());
        assertEquals(ParticipantMutationStatus.APPLIED, transaction.inTransaction(
            () -> administration.create(party, "Explorers", oldOwner, 1000L)));
        assertEquals(ParticipantMutationStatus.APPLIED, transaction.inTransaction(() -> administration.addMember(
            party, oldOwner, newOwner, ParticipantMemberRole.MEMBER, 0L, 1100L)));
        assertEquals(ParticipantMutationStatus.APPLIED, transaction.inTransaction(() -> administration.transferOwnership(
            party, oldOwner, newOwner, 1L, 1200L)));
        assertEquals(ParticipantMemberRole.ADMIN, directory.findActiveForPlayer(oldOwner).get(0).getRole());
        assertEquals(ParticipantMemberRole.OWNER, directory.findActiveForPlayer(newOwner).get(0).getRole());
        assertEquals(ParticipantMutationStatus.NOT_AUTHORIZED, transaction.inTransaction(() ->
            administration.transferOwnership(party, oldOwner, UUID.randomUUID(), 2L, 1300L)));
    }

    @Test
    public void chapterCatalogOrderIsSeparateFromDefinitionVersionAndReplacesAtomically() {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestChapterRepository chapters = new JdbcQuestChapterRepository(manager);
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        com.jsirgalaxybase.quest.core.QuestChapterDefinition firstDefinition = new com.jsirgalaxybase.quest.core.QuestChapterDefinition(
            first, 1, "First", "", "", "", Collections.<com.jsirgalaxybase.quest.core.QuestChapterEntry>emptyList());
        com.jsirgalaxybase.quest.core.QuestChapterDefinition secondDefinition = new com.jsirgalaxybase.quest.core.QuestChapterDefinition(
            second, 1, "Second", "", "", "", Collections.<com.jsirgalaxybase.quest.core.QuestChapterEntry>emptyList());
        transaction.inTransaction(() -> { chapters.createDraft(firstDefinition); chapters.createDraft(secondDefinition); return null; });
        assertEquals(Arrays.asList(first, second), catalogIds(chapters.listOrdered()));
        transaction.inTransaction(() -> { chapters.replaceOrder(Arrays.asList(second, first)); return null; });
        assertEquals(Arrays.asList(second, first), catalogIds(chapters.listOrdered()));
        assertEquals(0L, chapters.listOrdered().get(0).getOrder());
        assertEquals(1L, chapters.listOrdered().get(1).getOrder());
    }

    private static List<UUID> catalogIds(List<QuestChapterCatalogEntry> values) {
        List<UUID> result = new java.util.ArrayList<UUID>(); for (QuestChapterCatalogEntry value : values) result.add(value.getChapterId()); return result;
    }

    @Test
    public void questCompletionRewardAtomicallyCompletesTargetAndCreatesRewardsOnce() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcQuestCompletionRewardPort completion = new JdbcQuestCompletionRewardPort(definitions, runtime,
            transaction);
        UUID player = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        QuestDefinition target = definition(targetId, 1, "Reward target");
        com.jsirgalaxybase.quest.core.StoredQuestDefinition draft = transaction.inTransaction(
            () -> definitions.createDraft(target));
        assertTrue(transaction.inTransaction(() -> definitions.publish(targetId, 1, draft.getContentHash(), 10L)));

        assertEquals(QuestCompletionRewardPort.Result.COMPLETED,
            completion.complete("source-entitlement", player, targetId, 20L));
        assertEquals(QuestCompletionRewardPort.Result.ALREADY_COMPLETED,
            completion.complete("source-entitlement", player, targetId, 30L));

        QuestProgressSnapshot progress = runtime.findProgress(ParticipantId.player(player), targetId, 1).get();
        assertEquals(QuestStatus.COMPLETED, progress.getStatus());
        assertEquals(20L, progress.getCompletedAt());
        assertTrue(progress.getTasks().get("task").isComplete());
        assertEquals(Collections.singletonList(3L), progress.getTasks().get("task").getValues());
        assertEquals(1L, count("galaxy_quest_reward_entitlement"));
    }

    @Test
    public void questCompletionRewardCompletesOnlyRecipientForPersonalNonRepeatableTarget() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcQuestCompletionRewardPort completion = new JdbcQuestCompletionRewardPort(definitions, runtime,
            transaction);
        UUID recipient = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Map<String, String> taskParameters = new LinkedHashMap<String, String>();
        taskParameters.put("target", "1");
        taskParameters.put("factType", "minecraft:kill");
        QuestDefinition target = new QuestDefinition(targetId, 1, "Personal target", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(),
            Collections.singletonList(new TaskDefinition("task", "count", false, taskParameters)),
            Collections.singletonList(new RewardDefinition("reward", "item", Collections.<String, String>emptyMap())),
            RepeatPolicy.never(), QuestBehavior.builder().participantScope(QuestParticipantScope.PLAYER).build());
        StoredQuestDefinition draft = transaction.inTransaction(() -> definitions.createDraft(target));
        assertTrue(transaction.inTransaction(() -> definitions.publish(targetId, 1, draft.getContentHash(), 10L)));

        assertEquals(QuestCompletionRewardPort.Result.COMPLETED,
            completion.complete("source-entitlement", recipient, targetId, 20L));

        QuestProgressSnapshot recipientProgress = runtime.findProgress(ParticipantId.player(recipient), targetId, 1)
            .get();
        assertEquals(QuestStatus.COMPLETED, recipientProgress.getStatus());
        assertFalse(runtime.findProgress(ParticipantId.player(otherPlayer), targetId, 1).isPresent());
        assertEquals(1L, count("galaxy_quest_reward_entitlement"));
    }

    @Test
    public void autoClaimedSourceCompletionRewardCompletesTargetAndLeavesTargetRewardClaimable() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(manager);
        JdbcQuestRuntimeRepository runtimeRepository = new JdbcQuestRuntimeRepository(manager);
        JdbcRewardDeliveryRepository deliveryRepository = new JdbcRewardDeliveryRepository(manager);
        UUID player = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Map<String, String> taskParameters = new LinkedHashMap<String, String>();
        taskParameters.put("target", "1");
        taskParameters.put("factType", "test:bq_source_complete");
        TaskDefinition task = new TaskDefinition("confirm", "count", false, taskParameters);
        RewardDefinition completionReward = new RewardDefinition("unlock-target", "bq_standard:questcompletion",
            Collections.singletonMap("questUuid", targetId.toString()));
        QuestDefinition source = new QuestDefinition(sourceId, 1, "Auto-claim source", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task),
            Collections.singletonList(completionReward), RepeatPolicy.never(),
            QuestBehavior.builder().autoClaim(true).build());
        RewardDefinition targetReward = new RewardDefinition("target-item", "bq_standard:item",
            Collections.singletonMap("item.0.registryName", "minecraft:diamond"));
        QuestDefinition target = new QuestDefinition(targetId, 1, "Personal target", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.<TaskDefinition>emptyList(),
            Collections.singletonList(targetReward), RepeatPolicy.never(),
            QuestBehavior.builder().autoClaim(false).participantScope(QuestParticipantScope.PLAYER).build());
        StoredQuestDefinition sourceDraft = transaction.inTransaction(() -> definitions.createDraft(source));
        StoredQuestDefinition targetDraft = transaction.inTransaction(() -> definitions.createDraft(target));
        assertTrue(transaction.inTransaction(() -> definitions.publish(sourceId, 1, sourceDraft.getContentHash(), 10L)));
        assertTrue(transaction.inTransaction(() -> definitions.publish(targetId, 1, targetDraft.getContentHash(), 11L)));

        TaskEvaluatorRegistry evaluators = new TaskEvaluatorRegistry();
        evaluators.register(new FactCountTaskEvaluator("count"));
        QuestRuntimeService runtime = new QuestRuntimeService(runtimeRepository, transaction,
            new QuestFactProjector(evaluators), new QuestEngine());
        GameplayFact completionFact = new GameplayFact("s2", "source-complete-once", player,
            "test:bq_source_complete", 100L, Collections.<String, String>emptyMap());
        QuestEvaluationRequest request = new QuestEvaluationRequest(source, ParticipantMembership.player(player),
            Collections.<UUID>emptySet());
        assertTrue(runtime.apply(completionFact, Collections.singletonList(request), 100L).isAccepted());

        JdbcQuestCompletionRewardPort completionPort = new JdbcQuestCompletionRewardPort(definitions, runtimeRepository,
            transaction);
        QuestCompletionRewardDeliveryHandler completionHandler = new QuestCompletionRewardDeliveryHandler(
            completionPort, () -> 200L);
        RewardDeliveryWorker worker = new RewardDeliveryWorker(deliveryRepository, transaction,
            completionHandler::deliver, () -> 200L, 1_000L, 100L, 3);
        RewardDeliveryBatchResult sourceDelivery = worker.runOnce("completion-worker", 10);
        assertEquals(1, sourceDelivery.getLeased());
        assertEquals(1, sourceDelivery.getDelivered());

        QuestProgressSnapshot targetProgress = runtimeRepository
            .findProgress(ParticipantId.player(player), targetId, 1).get();
        assertEquals(QuestStatus.COMPLETED, targetProgress.getStatus());
        assertEquals(2L, count("galaxy_quest_reward_entitlement"));
        assertEquals(1L, scalar("SELECT COUNT(*) FROM galaxy_quest_reward_entitlement WHERE quest_id='"
            + targetId + "' AND reward_key='target-item' AND delivery_status='CLAIMABLE'"));
        assertEquals(0, worker.runOnce("completion-worker", 10).getLeased());
    }

    @Test
    public void questCompletionRewardDoesNotRecompleteTargetAfterRepeatCycleReset() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcQuestCompletionRewardPort completion = new JdbcQuestCompletionRewardPort(definitions, runtime,
            transaction);
        UUID player = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        QuestDefinition target = definition(targetId, 1, "Repeat target");
        StoredQuestDefinition draft = transaction.inTransaction(() -> definitions.createDraft(target));
        assertTrue(transaction.inTransaction(() -> definitions.publish(targetId, 1, draft.getContentHash(), 10L)));

        assertEquals(QuestCompletionRewardPort.Result.COMPLETED,
            completion.complete("source-cycle-0", player, targetId, 20L));
        assertTrue(transaction.inTransaction(() -> runtime.wasEverCompleted(ParticipantId.player(player), targetId, 1)));

        Map<String, TaskProgress> resetTasks = new LinkedHashMap<String, TaskProgress>();
        resetTasks.put("task", new TaskProgress("task", 0L, 3L, false));
        transaction.inTransaction(() -> {
            runtime.saveProgress(new QuestProgressSnapshot(ParticipantId.player(player), targetId, 1,
                QuestStatus.IN_PROGRESS, resetTasks, 0L, 1));
            return null;
        });

        assertEquals(QuestCompletionRewardPort.Result.ALREADY_COMPLETED,
            completion.complete("source-cycle-1", player, targetId, 40L));
        QuestProgressSnapshot current = runtime.findProgress(ParticipantId.player(player), targetId, 1).get();
        assertEquals(1, current.getCycle());
        assertEquals(QuestStatus.IN_PROGRESS, current.getStatus());
        assertTrue(transaction.inTransaction(() -> runtime.wasEverCompleted(ParticipantId.player(player), targetId, 1)));
        assertEquals(1L, count("galaxy_quest_reward_entitlement"));
    }

    @Test
    public void questCompletionRewardSelectsTargetScopeAndFreezesItsCurrentMembers() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcParticipantAdministrationRepository administration = new JdbcParticipantAdministrationRepository(manager);
        JdbcParticipantMembershipResolver memberships = new JdbcParticipantMembershipResolver(manager);
        UUID owner = UUID.randomUUID(), member = UUID.randomUUID();
        ParticipantId team = new ParticipantId(ParticipantType.TEAM, UUID.randomUUID());
        assertEquals(ParticipantMutationStatus.APPLIED, transaction.inTransaction(
            () -> administration.create(team, "Builders", owner, 1L)));
        assertEquals(ParticipantMutationStatus.APPLIED, transaction.inTransaction(() -> administration.addMember(
            team, owner, member, ParticipantMemberRole.MEMBER, 0L, 2L)));
        UUID targetId = UUID.randomUUID();
        RewardDefinition reward = new RewardDefinition("xp", "bq_standard:xp",
            Collections.singletonMap("amount", "5"));
        QuestDefinition target = new QuestDefinition(targetId, 1, "Team target", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.<TaskDefinition>emptyList(),
            Collections.singletonList(reward), RepeatPolicy.never(),
            QuestBehavior.builder().participantScope(QuestParticipantScope.TEAM).build());
        transaction.inTransaction(() -> { com.jsirgalaxybase.quest.core.StoredQuestDefinition draft = definitions.createDraft(target);
            assertTrue(definitions.publish(targetId, 1, draft.getContentHash(), 3L)); return null; });
        JdbcQuestCompletionRewardPort completion = new JdbcQuestCompletionRewardPort(definitions, runtime,
            transaction, memberships);
        assertEquals(QuestCompletionRewardPort.Result.COMPLETED,
            completion.complete("source", owner, targetId, 4L));
        assertTrue(runtime.findProgress(team, targetId, 1).isPresent());
        assertEquals(2L, count("galaxy_quest_reward_entitlement"));
    }

    @Test
    public void transactionPersistsVectorProgressAndEntitlementIdempotently() throws Exception {
        final JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        final JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        final JdbcQuestRuntimeRepository repository = new JdbcQuestRuntimeRepository(manager);
        final UUID player = UUID.randomUUID();
        final UUID quest = UUID.randomUUID();
        final UUID completedQuest = UUID.randomUUID();
        final QuestProgressSnapshot progress = progress(player, quest);
        final Set<RewardEntitlement> rewards = Collections.singleton(entitlement(player, quest));
        final GameplayFact fact = new GameplayFact("s2", "event-1", player, "item-observed", 10L,
            Collections.singletonMap("subject", "minecraft:iron_ingot"));

        assertTrue(transaction.inTransaction(() -> {
            boolean inserted = repository.recordFactIfAbsent(fact);
            repository.saveProgress(progress);
            repository.saveProgress(completedProgress(player, completedQuest));
            repository.insertEntitlementsIfAbsent(rewards);
            repository.insertEntitlementsIfAbsent(rewards);
            return inserted;
        }));
        assertFalse(transaction.inTransaction(() -> repository.recordFactIfAbsent(fact)));

        Optional<QuestProgressSnapshot> loaded = repository.findProgress(ParticipantId.player(player), quest, 1);
        assertTrue(loaded.isPresent());
        assertEquals(Arrays.asList(4L, 3L), loaded.get().getTasks().get("items").getValues());
        Set<UUID> completed = new JdbcQuestCompletionQuery(manager)
            .findCompletedQuestIds(ParticipantId.player(player));
        assertTrue(completed.contains(completedQuest));
        assertFalse(completed.contains(quest));
        assertEquals(1L, count("galaxy_quest_reward_entitlement"));
    }

    @Test
    public void repeatCycleClaimGateClearsOnlyAfterRewardClaimIsRecorded() {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcRewardClaimRepository claims = new JdbcRewardClaimRepository(manager);
        UUID player = UUID.randomUUID();
        UUID questId = UUID.randomUUID();

        transaction.inTransaction(() -> {
            runtime.insertEntitlementsIfAbsent(Collections.singleton(claimableEntitlement(player, questId)));
            return null;
        });
        assertTrue(transaction.inTransaction(() -> runtime.hasClaimableRewardEntitlement(
            ParticipantId.player(player), questId, 1, 0)));
        assertEquals(Long.valueOf(50L), transaction.inTransaction(() -> runtime.findRepeatCycleAnchorMillis(
            ParticipantId.player(player), questId, 1, 0, 50L)));
        assertFalse(transaction.inTransaction(() -> runtime.hasClaimableRewardEntitlement(
            ParticipantId.player(UUID.randomUUID()), questId, 1, 0)));

        assertEquals(RewardClaimStatus.CLAIMED,
            transaction.inTransaction(() -> claims.claim(questId, 1, 0, player, 100L)));
        assertFalse(transaction.inTransaction(() -> runtime.hasClaimableRewardEntitlement(
            ParticipantId.player(player), questId, 1, 0)));
        assertEquals(Long.valueOf(100L), transaction.inTransaction(() -> runtime.findRepeatCycleAnchorMillis(
            ParticipantId.player(player), questId, 1, 0, 50L)));
    }

    @Test
    public void concurrentDuplicateFactCanAdvanceAndCreateRewardsOnlyOnce() throws Exception {
        final JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        final JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        final JdbcQuestRuntimeRepository repository = new JdbcQuestRuntimeRepository(manager);
        final UUID player = UUID.randomUUID();
        final UUID quest = UUID.randomUUID();
        final GameplayFact fact = new GameplayFact("s2", "same-event", player, "kill", 20L,
            Collections.<String, String>emptyMap());
        final QuestProgressSnapshot progress = progress(player, quest);
        final Set<RewardEntitlement> rewards = Collections.singleton(entitlement(player, quest));
        Callable<Boolean> work = () -> transaction.inTransaction(() -> {
            if (!repository.recordFactIfAbsent(fact)) return false;
            repository.saveProgress(progress);
            repository.insertEntitlementsIfAbsent(rewards);
            return true;
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(work);
            Future<Boolean> second = executor.submit(work);
            int accepted = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
            assertEquals(1, accepted);
        } finally {
            executor.shutdownNow();
        }
        assertEquals(1L, count("galaxy_quest_fact"));
        assertEquals(1L, count("galaxy_quest_reward_entitlement"));
    }

    @Test
    public void bqPartyInventoryLeasesExpireAndFenceStaleLoginSessions() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcBqPartyInventorySnapshotRepository leases = new JdbcBqPartyInventorySnapshotRepository(manager);
        JdbcParticipantAdministrationRepository administration = new JdbcParticipantAdministrationRepository(manager);
        UUID owner = UUID.randomUUID();
        ParticipantId party = ParticipantId.party(UUID.randomUUID());
        assertEquals(ParticipantMutationStatus.APPLIED, transaction.inTransaction(() -> administration.create(
            party, "Snapshot party", owner, 1000L)));
        UUID firstSession = UUID.randomUUID();
        UUID currentSession = UUID.randomUUID();
        Map<String, String> inventory = new LinkedHashMap<String, String>();
        inventory.put("inventory.count", "1");
        inventory.put("inventory.0.registryName", "minecraft:iron_ingot");

        transaction.inTransaction(() -> {
            assertTrue(leases.lockActiveParty(party));
            leases.claimSession(owner, "s2", firstSession, inventory, 15000L);
            return null;
        });
        transaction.inTransaction(() -> {
            assertTrue(leases.lockActiveParty(party));
            leases.claimSession(owner, "lobby", currentSession, inventory, 15000L);
            return null;
        });

        transaction.inTransaction(() -> {
            assertFalse(leases.publish(owner, "s2", firstSession, inventory, 15000L));
            assertEquals(0, leases.deleteSession(owner, "s2", firstSession));
            QuestInventorySnapshotLease active = leases.findActive(Collections.singletonList(owner)).get(owner);
            assertNotNull(active);
            assertEquals("lobby", active.getSourceServer());
            assertEquals(currentSession, active.getSessionId());
            assertEquals(1, leases.deleteSession(owner, "lobby", currentSession));
            return null;
        });

        UUID expiringSession = UUID.randomUUID();
        transaction.inTransaction(() -> {
            leases.claimSession(owner, "s2", expiringSession, inventory, 5L);
            return null;
        });
        Thread.sleep(25L);
        transaction.inTransaction(() -> {
            assertTrue(leases.findActive(Collections.singletonList(owner)).isEmpty());
            assertEquals(1, leases.deleteExpired());
            return null;
        });
    }

    @Test
    public void inventoryFactRowsRedactStackAndSessionAttributes() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestRuntimeRepository repository = new JdbcQuestRuntimeRepository(manager);
        UUID player = UUID.randomUUID();
        UUID party = UUID.randomUUID();
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("inventory.count", "1");
        attributes.put("inventory.0.registryName", "example:private_inventory_item");
        attributes.put("inventory.sessionId", UUID.randomUUID().toString());
        attributes.put("inventory.bqPartySnapshot", "true");
        attributes.put("inventory.bqPartyId", party.toString());
        GameplayFact fact = new GameplayFact("s2", "inventory-redaction:" + UUID.randomUUID(), player,
            "galaxy:inventory_observed", 1000L, attributes);

        assertTrue(transaction.inTransaction(() -> repository.recordFactIfAbsent(fact)));
        try (Connection connection = scoped.getConnection(); PreparedStatement statement = connection.prepareStatement(
            "SELECT attributes_json::text FROM galaxy_quest_fact WHERE source_server=? AND event_id=?")) {
            statement.setString(1, "s2");
            statement.setString(2, fact.getEventId());
            try (ResultSet row = statement.executeQuery()) {
                assertTrue(row.next());
                String stored = row.getString(1);
                assertTrue(stored.contains("inventory.snapshotRedacted"));
                assertTrue(stored.contains("inventory.count"));
                assertTrue(stored.contains(party.toString()));
                assertFalse(stored.contains("private_inventory_item"));
                assertFalse(stored.contains("sessionId"));
            }
        }
    }

    @Test
    public void rollbackRemovesFactProgressAndRewardTogether() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestRuntimeRepository repository = new JdbcQuestRuntimeRepository(manager);
        UUID player = UUID.randomUUID();
        UUID quest = UUID.randomUUID();
        try {
            transaction.inTransaction(() -> {
                repository.recordFactIfAbsent(new GameplayFact("s2", "rollback", player, "kill", 1L,
                    Collections.<String, String>emptyMap()));
                repository.saveProgress(progress(player, quest));
                repository.insertEntitlementsIfAbsent(Collections.singleton(entitlement(player, quest)));
                throw new IllegalStateException("forced");
            });
            fail("expected rollback");
        } catch (IllegalStateException expected) {
            assertEquals("forced", expected.getMessage());
        }
        assertEquals(0L, count("galaxy_quest_fact"));
        assertEquals(0L, count("galaxy_quest_progress"));
        assertEquals(0L, count("galaxy_quest_reward_entitlement"));
    }

    @Test
    public void differentConcurrentFactsSerializeAbsentProgressWithoutLostUpdate() throws Exception {
        final JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        final JdbcQuestRuntimeRepository repository = new JdbcQuestRuntimeRepository(manager);
        TaskEvaluatorRegistry evaluators = new TaskEvaluatorRegistry();
        evaluators.register(new FactCountTaskEvaluator("count"));
        final QuestRuntimeService service = new QuestRuntimeService(repository, new JdbcQuestTransaction(manager),
            new QuestFactProjector(evaluators), new QuestEngine());
        final UUID player = UUID.randomUUID();
        Map<String, String> taskParameters = new LinkedHashMap<String, String>();
        taskParameters.put("target", "2");
        taskParameters.put("factType", "minecraft:kill");
        taskParameters.put("subject", "Zombie");
        final QuestDefinition definition = new QuestDefinition(UUID.randomUUID(), 1, "Two kills", "",
            QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(),
            Collections.singletonList(new TaskDefinition("hunt", "count", false, taskParameters)),
            Collections.<RewardDefinition>emptyList(), RepeatPolicy.never());
        final List<QuestEvaluationRequest> requests = Collections.singletonList(new QuestEvaluationRequest(definition,
            ParticipantMembership.player(player), Collections.<UUID>emptySet()));
        Callable<FactProcessingResult> firstWork = () -> service.apply(new GameplayFact("s2", "kill-a", player,
            "minecraft:kill", 1L, attributes("Zombie", "1")), requests, 1L);
        Callable<FactProcessingResult> secondWork = () -> service.apply(new GameplayFact("s2", "kill-b", player,
            "minecraft:kill", 2L, attributes("Zombie", "1")), requests, 2L);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<FactProcessingResult> first = executor.submit(firstWork);
            Future<FactProcessingResult> second = executor.submit(secondWork);
            assertTrue(first.get().isAccepted());
            assertTrue(second.get().isAccepted());
        } finally {
            executor.shutdownNow();
        }
        QuestProgressSnapshot loaded = repository.findProgress(ParticipantId.player(player), definition.getId(), 1).get();
        assertEquals(2L, loaded.getTasks().get("hunt").getValue());
        assertEquals(QuestStatus.COMPLETED, loaded.getStatus());
    }

    @Test
    public void draftUsesOptimisticHashAndPublishedDefinitionCannotBeMutated() {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(manager);
        UUID id = UUID.randomUUID();
        QuestDefinition first = definition(id, 1, "Draft A");
        com.jsirgalaxybase.quest.core.StoredQuestDefinition created = transaction.inTransaction(
            () -> definitions.createDraft(first));
        QuestDefinition edited = definition(id, 1, "Draft B");
        com.jsirgalaxybase.quest.core.StoredQuestDefinition updated = transaction.inTransaction(
            () -> definitions.updateDraft(edited, created.getContentHash()));
        try {
            transaction.inTransaction(() -> definitions.updateDraft(first, created.getContentHash()));
            fail("expected stale draft conflict");
        } catch (QuestDefinitionConflictException expected) {
            assertTrue(expected.getMessage().contains("changed"));
        }
        assertTrue(transaction.inTransaction(() -> definitions.publish(id, 1, updated.getContentHash(), 1234000L)));
        assertFalse(transaction.inTransaction(() -> definitions.publish(id, 1, updated.getContentHash(), 1234001L)));
        assertEquals(QuestDefinitionLifecycle.PUBLISHED, definitions.findPublished(id).get().getLifecycle());
        assertEquals("Draft B", definitions.find(id, 1).get().getDefinition().getName());
        try {
            transaction.inTransaction(() -> definitions.updateDraft(first, updated.getContentHash()));
            fail("published version must be immutable");
        } catch (QuestDefinitionConflictException expected) {
            assertTrue(expected.getMessage().contains("published"));
        }

        UUID secondId = UUID.randomUUID();
        com.jsirgalaxybase.quest.core.StoredQuestDefinition second = transaction.inTransaction(
            () -> definitions.createDraft(definition(secondId, 1, "Second")));
        assertTrue(transaction.inTransaction(() -> definitions.publish(secondId, 1, second.getContentHash(), 1235000L)));
        assertEquals(2, definitions.findAllPublished().size());

        com.jsirgalaxybase.quest.core.QuestDefinitionManagementPage management = definitions.load(
            new com.jsirgalaxybase.quest.core.QuestDefinitionManagementRequest("draft b",
                QuestDefinitionLifecycle.PUBLISHED, 0, 20));
        assertEquals(1L, management.getTotal());
        assertEquals(id, management.getDefinitions().get(0).getDefinition().getId());
        assertFalse(management.hasNext());
    }

    @Test
    public void questCenterQueryJoinsPublishedCatalogProgressAndClaimStateWithPlayerIsolation() {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(manager);
        JdbcQuestChapterRepository chapters = new JdbcQuestChapterRepository(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcQuestCenterQuery center = new JdbcQuestCenterQuery(manager);
        center.validateSchema();
        UUID player = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        UUID hiddenId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        QuestDefinition definition = definition(questId, 1, "Terminal quest");
        QuestDefinition hidden = new QuestDefinition(hiddenId, 1, "Hidden", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.<TaskDefinition>emptyList(), Collections.<RewardDefinition>emptyList(),
            RepeatPolicy.never(), com.jsirgalaxybase.quest.core.QuestBehavior.builder()
                .visibility(com.jsirgalaxybase.quest.core.QuestVisibility.HIDDEN).build());
        com.jsirgalaxybase.quest.core.QuestChapterDefinition chapter =
            new com.jsirgalaxybase.quest.core.QuestChapterDefinition(chapterId, 1, "Chapter", "Description", "", "",
                Collections.singletonList(new com.jsirgalaxybase.quest.core.QuestChapterEntry(questId, 3, 4, 24, 24)));
        transaction.inTransaction(() -> {
            com.jsirgalaxybase.quest.core.StoredQuestDefinition questDraft = definitions.createDraft(definition);
            assertTrue(definitions.publish(questId, 1, questDraft.getContentHash(), 10L));
            com.jsirgalaxybase.quest.core.StoredQuestDefinition hiddenDraft = definitions.createDraft(hidden);
            assertTrue(definitions.publish(hiddenId, 1, hiddenDraft.getContentHash(), 10L));
            com.jsirgalaxybase.quest.core.StoredQuestChapter chapterDraft = chapters.createDraft(chapter);
            assertTrue(chapters.publish(chapterId, 1, chapterDraft.getContentHash(), 10L));
            runtime.saveProgress(completedProgress(player, questId));
            RewardDefinition reward = definition.getRewards().get(0);
            runtime.insertEntitlementsIfAbsent(Collections.singleton(
                new RewardEntitlement(ParticipantId.player(player), definition, reward, 0, false)));
            return null;
        });

        com.jsirgalaxybase.quest.core.QuestChapterManagementPage chapterPage = chapters.load(
            new com.jsirgalaxybase.quest.core.QuestDefinitionManagementRequest("chapter",
                QuestDefinitionLifecycle.PUBLISHED, 0, 20));
        assertEquals(1L, chapterPage.getTotal());
        assertEquals(chapterId, chapterPage.getChapters().get(0).getDefinition().getId());
        assertFalse(chapterPage.hasNext());

        com.jsirgalaxybase.quest.core.QuestCenterSnapshot mine = center.load(ParticipantId.player(player));
        assertEquals(1, mine.getChapters().size());
        assertEquals(1, mine.getQuests().size());
        assertEquals(QuestStatus.COMPLETED, mine.getQuests().get(0).getStatus());
        assertEquals(1, mine.getQuests().get(0).getRewards().getClaimable());
        com.jsirgalaxybase.quest.core.QuestCenterSnapshot other = center.load(ParticipantId.player(UUID.randomUUID()));
        assertNull(other.getQuests().get(0).getProgress());
        assertEquals(0, other.getQuests().get(0).getRewards().getTotal());
    }

    @Test
    public void rewardLeaseRetriesAndRejectsStaleConfirmation() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcRewardDeliveryRepository delivery = new JdbcRewardDeliveryRepository(manager);
        UUID player = UUID.randomUUID();
        UUID quest = UUID.randomUUID();
        RewardEntitlement entitlement = entitlement(player, quest);
        transaction.inTransaction(() -> {
            runtime.insertEntitlementsIfAbsent(Collections.singleton(entitlement));
            return null;
        });

        List<RewardDeliveryLease> first = transaction.inTransaction(() -> delivery.leaseAvailable("worker-a", 100L, 200L, 10));
        assertEquals(1, first.size());
        assertTrue(transaction.inTransaction(() -> delivery.markFailed(entitlement.getEntitlementKey(), "worker-a", 1,
            "inventory unavailable", 150L, 300L, true)));
        assertTrue(transaction.inTransaction(() -> delivery.leaseAvailable("worker-b", 299L, 400L, 10)).isEmpty());
        List<RewardDeliveryLease> second = transaction.inTransaction(() -> delivery.leaseAvailable("worker-b", 300L, 500L, 10));
        assertEquals(2, second.get(0).getAttempt());
        assertFalse(transaction.inTransaction(() -> delivery.markDelivered(entitlement.getEntitlementKey(),
            "worker-a", 1, 350L)));
        assertTrue(transaction.inTransaction(() -> delivery.markDelivered(entitlement.getEntitlementKey(),
            "worker-b", 2, 350L)));
        assertEquals(2L, count("galaxy_quest_reward_delivery_attempt"));
        assertEquals(1L, scalar("SELECT COUNT(*) FROM galaxy_quest_reward_delivery_attempt WHERE status='DELIVERED'"));
    }

    @Test public void deferredDeliveryDoesNotConsumeFailureBudget() throws Exception {
        JdbcQuestConnectionManager manager=new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction=new JdbcQuestTransaction(manager);
        JdbcQuestRuntimeRepository runtime=new JdbcQuestRuntimeRepository(manager);
        JdbcRewardDeliveryRepository delivery=new JdbcRewardDeliveryRepository(manager);
        RewardEntitlement entitlement=entitlement(UUID.randomUUID(),UUID.randomUUID());
        transaction.inTransaction(()->{runtime.insertEntitlementsIfAbsent(Collections.singleton(entitlement));return null;});
        RewardDeliveryLease first=transaction.inTransaction(()->delivery.leaseAvailable("worker-a",100L,200L,1)).get(0);
        assertEquals(0,first.getFailureCount());
        assertTrue(transaction.inTransaction(()->delivery.markDeferred(entitlement.getEntitlementKey(),"worker-a",
            first.getAttempt(),"offline",150L,200L)));
        RewardDeliveryLease second=transaction.inTransaction(()->delivery.leaseAvailable("worker-b",200L,300L,1)).get(0);
        assertEquals(2,second.getAttempt());assertEquals(0,second.getFailureCount());
        assertTrue(transaction.inTransaction(()->delivery.markFailed(entitlement.getEntitlementKey(),"worker-b",
            second.getAttempt(),"inventory-full",250L,300L,true)));
        assertEquals(1L,scalar("SELECT failure_count FROM galaxy_quest_reward_entitlement"));
    }

    @Test
    public void manualRewardsRemainClaimableUntilAuthenticatedPlayerClaimsThem() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcRewardDeliveryRepository delivery = new JdbcRewardDeliveryRepository(manager);
        JdbcRewardClaimRepository claims = new JdbcRewardClaimRepository(manager);
        UUID player = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        RewardDefinition reward = new RewardDefinition("manual", "bq_standard:item", Collections.emptyMap());
        QuestDefinition quest = new QuestDefinition(questId, 1, "Manual", "", QuestLogic.AND, QuestLogic.AND,
            Collections.emptySet(), Collections.emptyList(), Collections.singletonList(reward), RepeatPolicy.never());
        RewardEntitlement entitlement = new RewardEntitlement(ParticipantId.player(player), quest, reward, 0, false);
        transaction.inTransaction(() -> { runtime.insertEntitlementsIfAbsent(Collections.singleton(entitlement)); return null; });

        assertEquals("CLAIMABLE", text("SELECT delivery_status FROM galaxy_quest_reward_entitlement"));
        assertTrue(transaction.inTransaction(() -> delivery.leaseAvailable("worker", 1L, 100L, 10)).isEmpty());
        assertEquals(RewardClaimStatus.NOT_OWNED, transaction.inTransaction(
            () -> claims.claim(questId, 1, 0, UUID.randomUUID(), 2L)));
        assertEquals(RewardClaimStatus.CLAIMED, transaction.inTransaction(
            () -> claims.claim(questId, 1, 0, player, 3L)));
        assertEquals(RewardClaimStatus.ALREADY_CLAIMED, transaction.inTransaction(
            () -> claims.claim(questId, 1, 0, player, 4L)));
        assertEquals(1, transaction.inTransaction(() -> delivery.leaseAvailable("worker", 4L, 100L, 10)).size());
    }

    @Test
    public void globalNoRewardsMarksManualEntitlementSuppressedWithoutLeasingIt() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcRewardDeliveryRepository delivery = new JdbcRewardDeliveryRepository(manager);
        JdbcRewardClaimRepository claims = new JdbcRewardClaimRepository(manager);
        UUID player = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        RewardDefinition reward = new RewardDefinition("manual", "bq_standard:item", Collections.emptyMap());
        QuestDefinition quest = new QuestDefinition(questId, 1, "Suppressed", "", QuestLogic.AND, QuestLogic.AND,
            Collections.emptySet(), Collections.emptyList(), Collections.singletonList(reward), RepeatPolicy.never());
        RewardEntitlement entitlement = new RewardEntitlement(ParticipantId.player(player), quest, reward, 0, false);
        transaction.inTransaction(() -> { runtime.insertEntitlementsIfAbsent(Collections.singleton(entitlement)); return null; });

        assertEquals(RewardClaimStatus.SUPPRESSED, transaction.inTransaction(
            () -> claims.claim(questId, 1, 0, player, 3L, true)));
        assertEquals("SUPPRESSED", text("SELECT delivery_status FROM galaxy_quest_reward_entitlement"));
        assertTrue(transaction.inTransaction(() -> delivery.leaseAvailable("worker", 4L, 100L, 10)).isEmpty());
        assertEquals(RewardClaimStatus.ALREADY_CLAIMED, transaction.inTransaction(
            () -> claims.claim(questId, 1, 0, player, 5L, true)));
    }

    @Test
    public void globalNoRewardsStillAllowsRewardsThatIgnoreDisabled() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcRewardDeliveryRepository delivery = new JdbcRewardDeliveryRepository(manager);
        JdbcRewardClaimRepository claims = new JdbcRewardClaimRepository(manager);
        UUID player = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        RewardDefinition suppressedReward = new RewardDefinition("item", "bq_standard:item", Collections.emptyMap());
        RewardDefinition exemptReward = new RewardDefinition("completion", "bq_standard:questcompletion", Collections.emptyMap());
        QuestDefinition quest = new QuestDefinition(questId, 1, "Mixed", "", QuestLogic.AND, QuestLogic.AND,
            Collections.emptySet(), Collections.emptyList(), Arrays.asList(suppressedReward, exemptReward), RepeatPolicy.never());
        java.util.LinkedHashSet<RewardEntitlement> entitlements = new java.util.LinkedHashSet<RewardEntitlement>();
        entitlements.add(new RewardEntitlement(ParticipantId.player(player), quest, suppressedReward, 0, false));
        entitlements.add(new RewardEntitlement(ParticipantId.player(player), quest, exemptReward, 0, false));
        transaction.inTransaction(() -> { runtime.insertEntitlementsIfAbsent(entitlements); return null; });

        assertEquals(RewardClaimStatus.CLAIMED, transaction.inTransaction(
            () -> claims.claim(questId, 1, 0, player, 3L, true)));
        assertEquals(1L, scalar("SELECT COUNT(*) FROM galaxy_quest_reward_entitlement WHERE delivery_status='SUPPRESSED'"));
        assertEquals(1L, scalar("SELECT COUNT(*) FROM galaxy_quest_reward_entitlement WHERE delivery_status='PENDING'"));
        assertEquals(1, transaction.inTransaction(() -> delivery.leaseAvailable("worker", 4L, 100L, 10)).size());
    }

    @Test
    public void expiredRewardLeaseIsTakenOverAndClosesPriorAttempt() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcRewardDeliveryRepository delivery = new JdbcRewardDeliveryRepository(manager);
        RewardEntitlement entitlement = entitlement(UUID.randomUUID(), UUID.randomUUID());
        transaction.inTransaction(() -> {
            runtime.insertEntitlementsIfAbsent(Collections.singleton(entitlement));
            return null;
        });

        assertEquals(1, transaction.inTransaction(() -> delivery.leaseAvailable("worker-a", 100L, 200L, 1))
            .get(0).getAttempt());
        RewardDeliveryLease takeover = transaction.inTransaction(
            () -> delivery.leaseAvailable("worker-b", 201L, 400L, 1)).get(0);

        assertEquals(2, takeover.getAttempt());
        assertEquals("worker-b", takeover.getWorkerId());
        assertEquals(1L, scalar("SELECT COUNT(*) FROM galaxy_quest_reward_delivery_attempt "
            + "WHERE attempt_no=1 AND status='FAILED' AND error_text='lease expired before confirmation'"));
        assertEquals(1L, scalar("SELECT COUNT(*) FROM galaxy_quest_reward_delivery_attempt "
            + "WHERE attempt_no=2 AND status='STARTED'"));
    }

    @Test
    public void choiceRewardCanBeChangedBeforeClaimAndLocksAtClaim() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcRewardDeliveryRepository delivery = new JdbcRewardDeliveryRepository(manager);
        JdbcRewardClaimRepository claims = new JdbcRewardClaimRepository(manager);
        JdbcRewardChoiceSelectionRepository choices = new JdbcRewardChoiceSelectionRepository(manager);
        UUID player = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("item.count", "2");
        RewardDefinition reward = new RewardDefinition("choice", "bq_standard:choice", parameters);
        QuestDefinition quest = new QuestDefinition(questId, 1, "Choice", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.<TaskDefinition>emptyList(), Collections.singletonList(reward),
            RepeatPolicy.never());
        RewardEntitlement entitlement = new RewardEntitlement(ParticipantId.player(player), quest, reward, 0, false);
        transaction.inTransaction(() -> {
            runtime.insertEntitlementsIfAbsent(Collections.singleton(entitlement));
            return null;
        });

        assertTrue(transaction.inTransaction(() -> delivery.leaseAvailable("worker", 1L, 100L, 10)).isEmpty());
        assertEquals(RewardChoiceSelectionStatus.INVALID, transaction.inTransaction(
            () -> choices.select(entitlement.getEntitlementKey(), UUID.randomUUID(), 1, 2L)));
        assertEquals(RewardChoiceSelectionStatus.INVALID, transaction.inTransaction(
            () -> choices.select(entitlement.getEntitlementKey(), player, 2, 2L)));
        assertEquals(RewardChoiceSelectionStatus.SELECTED, transaction.inTransaction(
            () -> choices.select(entitlement.getEntitlementKey(), player, 1, 2L)));
        assertEquals(RewardChoiceSelectionStatus.ALREADY_SELECTED, transaction.inTransaction(
            () -> choices.select(entitlement.getEntitlementKey(), player, 1, 3L)));
        assertEquals(RewardChoiceSelectionStatus.RESELECTED, transaction.inTransaction(
            () -> choices.select(entitlement.getEntitlementKey(), player, 0, 4L)));
        assertEquals(RewardClaimStatus.CLAIMED, transaction.inTransaction(
            () -> claims.claim(questId, 1, 0, player, 5L)));
        assertEquals(RewardChoiceSelectionStatus.CONFLICT, transaction.inTransaction(
            () -> choices.select(entitlement.getEntitlementKey(), player, 1, 6L)));

        RewardDeliveryLease lease = transaction.inTransaction(
            () -> delivery.leaseAvailable("worker", 7L, 100L, 10)).get(0);
        assertEquals("0", lease.getReward().getParameters().get("choice.index"));
        assertEquals(RewardChoiceSelectionStatus.CONFLICT, transaction.inTransaction(
            () -> choices.select(entitlement.getEntitlementKey(), player, 1, 8L)));
    }

    @Test
    public void queuedChoiceInitialSelectionIsReservedForForceClaim() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcRewardChoiceSelectionRepository choices = new JdbcRewardChoiceSelectionRepository(manager);
        UUID player = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        RewardDefinition reward = new RewardDefinition("auto-choice", "bq_standard:choice",
            Collections.singletonMap("item.count", "2"));
        QuestDefinition quest = new QuestDefinition(questId, 1, "Auto choice", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.<TaskDefinition>emptyList(),
            Collections.singletonList(reward), RepeatPolicy.never());
        RewardEntitlement entitlement = new RewardEntitlement(ParticipantId.player(player), quest, reward, 0);
        transaction.inTransaction(() -> {
            runtime.insertEntitlementsIfAbsent(Collections.singleton(entitlement));
            return null;
        });

        assertEquals(RewardChoiceSelectionStatus.INVALID, transaction.inTransaction(
            () -> choices.select(entitlement.getEntitlementKey(), player, 1, 1L)));
        assertEquals(RewardChoiceSelectionStatus.SELECTED, transaction.inTransaction(
            () -> choices.selectForForcedClaim(entitlement.getEntitlementKey(), player, 1, 2L)));
        assertEquals(RewardChoiceSelectionStatus.CONFLICT, transaction.inTransaction(
            () -> choices.select(entitlement.getEntitlementKey(), player, 0, 3L)));
    }

    @Test
    public void manualChoiceRequiresSelectionBeforeAuthenticatedClaimAndDelivery() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcRewardDeliveryRepository delivery = new JdbcRewardDeliveryRepository(manager);
        JdbcRewardChoiceSelectionRepository choices = new JdbcRewardChoiceSelectionRepository(manager);
        JdbcRewardClaimRepository claims = new JdbcRewardClaimRepository(manager);
        UUID player = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("item.count", "2");
        RewardDefinition reward = new RewardDefinition("choice-manual", "bq_standard:choice", parameters);
        QuestDefinition quest = new QuestDefinition(questId, 1, "Choice", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.<TaskDefinition>emptyList(), Collections.singletonList(reward),
            RepeatPolicy.never());
        RewardEntitlement entitlement = new RewardEntitlement(ParticipantId.player(player), quest, reward, 0, false);
        transaction.inTransaction(() -> {
            runtime.insertEntitlementsIfAbsent(Collections.singleton(entitlement));
            return null;
        });

        assertEquals(RewardClaimStatus.NEEDS_CHOICE, transaction.inTransaction(
            () -> claims.claim(questId, 1, 0, player, 1L)));
        assertTrue(transaction.inTransaction(() -> delivery.leaseAvailable("worker", 1L, 100L, 10)).isEmpty());
        assertEquals(RewardChoiceSelectionStatus.SELECTED, transaction.inTransaction(
            () -> choices.select(entitlement.getEntitlementKey(), player, 1, 2L)));
        assertEquals(RewardChoiceSelectionStatus.RESELECTED, transaction.inTransaction(
            () -> choices.select(entitlement.getEntitlementKey(), player, 0, 3L)));
        assertEquals(RewardClaimStatus.CLAIMED, transaction.inTransaction(
            () -> claims.claim(questId, 1, 0, player, 4L)));
        assertEquals(RewardChoiceSelectionStatus.CONFLICT, transaction.inTransaction(
            () -> choices.select(entitlement.getEntitlementKey(), player, 1, 5L)));
        RewardDeliveryLease lease = transaction.inTransaction(
            () -> delivery.leaseAvailable("worker", 6L, 100L, 10)).get(0);
        assertEquals("0", lease.getReward().getParameters().get("choice.index"));
    }

    @Test
    public void forceClaimWithBqRandomChoicesPersistsSelectionAndQueuesOnlyThatOption() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcRewardChoiceSelectionRepository choices = new JdbcRewardChoiceSelectionRepository(manager);
        JdbcRewardClaimRepository claims = new JdbcRewardClaimRepository(manager);
        JdbcRewardDeliveryRepository delivery = new JdbcRewardDeliveryRepository(manager);
        UUID player = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("item.count", "3");
        parameters.put("item.0.registryName", "minecraft:diamond");
        parameters.put("item.1.registryName", "enhancedlootbags:common");
        parameters.put("item.2.registryName", "minecraft:emerald");
        RewardDefinition reward = new RewardDefinition("choice-force", "bq_standard:choice", parameters);
        QuestDefinition quest = new QuestDefinition(questId, 1, "Force choice", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.<TaskDefinition>emptyList(),
            Collections.singletonList(reward), RepeatPolicy.never());
        RewardEntitlement entitlement = new RewardEntitlement(ParticipantId.player(player), quest, reward, 0, false);
        transaction.inTransaction(() -> {
            StoredQuestDefinition draft = definitions.createDraft(quest);
            assertTrue(definitions.publish(questId, 1, draft.getContentHash(), 1L));
            runtime.insertEntitlementsIfAbsent(Collections.singleton(entitlement));
            return null;
        });

        AuthenticatedQuestClaimService service = new AuthenticatedQuestClaimService(definitions, runtime, claims,
            transaction, new RewardSuppressionPolicy(false), choices);
        assertEquals(RewardClaimStatus.NEEDS_CHOICE, service.claim(player, questId, 2L));
        assertTrue(transaction.inTransaction(() -> delivery.leaseAvailable("worker", 2L, 100L, 10)).isEmpty());

        assertEquals(RewardClaimStatus.CLAIMED, service.forceClaimWithBqRandomChoices(player, questId, 3L));
        RewardDeliveryLease lease = transaction.inTransaction(
            () -> delivery.leaseAvailable("worker", 4L, 100L, 10)).get(0);
        assertEquals(entitlement.getEntitlementKey(), lease.getEntitlementKey());
        assertEquals("1", lease.getReward().getParameters().get("choice.index"));
        assertEquals(RewardClaimStatus.ALREADY_CLAIMED,
            service.forceClaimWithBqRandomChoices(player, questId, 5L));
    }

    @Test
    public void repeatedChoiceRetainsSelectionAndKeepsEachCycleDeliveryIsolated() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcRewardDeliveryRepository delivery = new JdbcRewardDeliveryRepository(manager);
        JdbcRewardChoiceSelectionRepository choices = new JdbcRewardChoiceSelectionRepository(manager);
        JdbcRewardClaimRepository claims = new JdbcRewardClaimRepository(manager);
        UUID player = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("item.count", "2");
        parameters.put("selectionMode", "player-required");
        RewardDefinition reward = new RewardDefinition("choice-repeat", "bq_standard:choice", parameters);
        QuestDefinition quest = new QuestDefinition(questId, 1, "Repeated choice", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.<TaskDefinition>emptyList(),
            Collections.singletonList(reward), RepeatPolicy.after(1000L, true));
        RewardEntitlement first = new RewardEntitlement(ParticipantId.player(player), quest, reward, 0, false);
        RewardEntitlement second = new RewardEntitlement(ParticipantId.player(player), quest, reward, 1, false);

        transaction.inTransaction(() -> { runtime.insertEntitlementsIfAbsent(Collections.singleton(first)); return null; });
        assertEquals(RewardChoiceSelectionStatus.SELECTED, transaction.inTransaction(
            () -> choices.selectLatest(questId, 1, reward.getKey(), player, 1, 2L)));
        assertEquals(RewardClaimStatus.CLAIMED, transaction.inTransaction(
            () -> claims.claim(questId, 1, 0, player, 3L)));
        RewardDeliveryLease firstLease = transaction.inTransaction(
            () -> delivery.leaseAvailable("worker", 4L, 100L, 10)).get(0);
        assertEquals(first.getEntitlementKey(), firstLease.getEntitlementKey());
        assertEquals("1", firstLease.getReward().getParameters().get("choice.index"));
        assertTrue(transaction.inTransaction(() -> delivery.markDelivered(first.getEntitlementKey(),
            "worker", firstLease.getAttempt(), 5L)));

        transaction.inTransaction(() -> { runtime.insertEntitlementsIfAbsent(Collections.singleton(second)); return null; });
        assertEquals(RewardClaimStatus.CLAIMED, transaction.inTransaction(
            () -> claims.claim(questId, 1, 1, player, 6L)));
        RewardDeliveryLease secondLease = transaction.inTransaction(
            () -> delivery.leaseAvailable("worker", 7L, 100L, 10)).get(0);
        assertEquals(second.getEntitlementKey(), secondLease.getEntitlementKey());
        assertEquals("1", secondLease.getReward().getParameters().get("choice.index"));
        assertEquals(RewardClaimStatus.ALREADY_CLAIMED, transaction.inTransaction(
            () -> claims.claim(questId, 1, 0, player, 8L)));

        UUID neverSelectedPlayer = UUID.randomUUID();
        RewardEntitlement unansweredFirst = new RewardEntitlement(ParticipantId.player(neverSelectedPlayer), quest,
            reward, 0, false);
        RewardEntitlement unansweredNext = new RewardEntitlement(ParticipantId.player(neverSelectedPlayer), quest,
            reward, 1, false);
        transaction.inTransaction(() -> {
            runtime.insertEntitlementsIfAbsent(Collections.singleton(unansweredFirst));
            runtime.insertEntitlementsIfAbsent(Collections.singleton(unansweredNext));
            return null;
        });
        assertEquals(RewardClaimStatus.NEEDS_CHOICE, transaction.inTransaction(
            () -> claims.claim(questId, 1, 1, neverSelectedPlayer, 9L)));
    }

    @Test
    public void consumptionSubmissionIsDurableIdempotentAndTransitionChecked() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(manager);
        JdbcConsumptionSubmissionRepository submissions = new JdbcConsumptionSubmissionRepository(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        UUID player = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        QuestDefinition definition = definition(questId, 1, "Consume");
        transaction.inTransaction(() -> definitions.createDraft(definition));
        Map<String, String> requestParameters = new LinkedHashMap<String, String>();
        requestParameters.put("item.count", "2");
        requestParameters.put(ConsumptionRequest.QUEST_CYCLE_PARAMETER, "2");
        requestParameters.put(ConsumptionRequest.SELECTION_SLOT_PARAMETER, "4");
        requestParameters.put(ConsumptionRequest.SELECTION_FINGERPRINT_PARAMETER, "sha256:stable-stack-fingerprint");
        ConsumptionRequest request = new ConsumptionRequest("submit-1", "s2", player, ParticipantId.player(player),
            questId, 1, "task", "item", Arrays.asList(2L, 3L), requestParameters, 10L);

        ConsumptionSubmission prepared = transaction.inTransaction(() -> submissions.prepareIfAbsent(request));
        assertEquals(ConsumptionSubmissionStatus.PREPARED, prepared.getStatus());
        assertEquals("4", prepared.getRequest().getResourceParameters()
            .get(ConsumptionRequest.SELECTION_SLOT_PARAMETER));
        assertEquals("sha256:stable-stack-fingerprint", prepared.getRequest().getResourceParameters()
            .get(ConsumptionRequest.SELECTION_FINGERPRINT_PARAMETER));
        assertEquals(2, prepared.getRequest().getQuestCycle());
        assertEquals("2", prepared.getRequest().confirmedFact().getAttributes().get("questCycle"));
        assertTrue(transaction.inTransaction(() -> runtime.hasPendingConsumptionSubmission(
            ParticipantId.player(player), questId, 1, 2)));
        assertFalse(transaction.inTransaction(() -> runtime.hasPendingConsumptionSubmission(
            ParticipantId.player(player), questId, 1, 1)));
        assertEquals(1, transaction.inTransaction(() -> submissions.findPendingForPlayer(player, "s2", 64)).size());
        assertTrue(transaction.inTransaction(() -> submissions.findPendingForPlayer(UUID.randomUUID(), "s2", 64)).isEmpty());
        assertTrue(transaction.inTransaction(() -> submissions.findPendingForPlayer(player, "lobby", 64)).isEmpty());
        assertEquals(ConsumptionSubmissionStatus.PREPARED,
            transaction.inTransaction(() -> submissions.prepareIfAbsent(request)).getStatus());
        assertEquals(ConsumptionSubmissionStatus.APPLIED,
            transaction.inTransaction(() -> submissions.markApplied("submit-1", "player-nbt:submit-1",
                java.util.Arrays.asList(2L, 1L), 20L)).getStatus());
        ConsumptionSubmission reloaded = transaction.inTransaction(() -> submissions.find("submit-1")).get();
        assertEquals(java.util.Arrays.asList(2L, 1L), reloaded.getAppliedAmounts());
        assertEquals("4", reloaded.getRequest().getResourceParameters()
            .get(ConsumptionRequest.SELECTION_SLOT_PARAMETER));
        assertEquals("sha256:stable-stack-fingerprint", reloaded.getRequest().getResourceParameters()
            .get(ConsumptionRequest.SELECTION_FINGERPRINT_PARAMETER));
        assertEquals(ConsumptionSubmissionStatus.APPLIED,
            transaction.inTransaction(() -> submissions.findPendingForPlayer(player, "s2", 64)).get(0).getStatus());
        assertEquals(ConsumptionSubmissionStatus.CONFIRMED,
            transaction.inTransaction(() -> submissions.markConfirmed("submit-1", 30L)).getStatus());
        assertEquals(ConsumptionSubmissionStatus.CONFIRMED,
            transaction.inTransaction(() -> submissions.markConfirmed("submit-1", 31L)).getStatus());
        assertTrue(transaction.inTransaction(() -> submissions.findPendingForPlayer(player, "s2", 64)).isEmpty());
        assertFalse(transaction.inTransaction(() -> runtime.hasPendingConsumptionSubmission(
            ParticipantId.player(player), questId, 1, 2)));
        assertEquals(1L, count("galaxy_quest_consumption_submission"));

        ConsumptionRequest collision = new ConsumptionRequest("submit-1", "s2", player, ParticipantId.player(player),
            questId, 1, "task", "item", Arrays.asList(9L, 3L), Collections.singletonMap("item.count", "2"), 10L);
        try {
            transaction.inTransaction(() -> submissions.prepareIfAbsent(collision));
            fail("expected stable-key collision rejection");
        } catch (QuestPersistenceException expected) {
            assertTrue(expected.getMessage().contains("different content"));
        }
    }

    private long count(String table) throws Exception {
        try (Connection connection = scoped.getConnection(); Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private long scalar(String sql) throws Exception {
        try (Connection connection = scoped.getConnection(); Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private String text(String sql) throws Exception {
        try (Connection connection = scoped.getConnection(); Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }

    private static QuestProgressSnapshot progress(UUID player, UUID quest) {
        Map<String, TaskProgress> tasks = new LinkedHashMap<String, TaskProgress>();
        tasks.put("items", new TaskProgress("items", Arrays.asList(4L, 3L), Arrays.asList(4L, 8L), false));
        return new QuestProgressSnapshot(ParticipantId.player(player), quest, 1, QuestStatus.IN_PROGRESS, tasks, 0L, 0);
    }

    private static QuestProgressSnapshot completedProgress(UUID player, UUID quest) {
        Map<String, TaskProgress> tasks = new LinkedHashMap<String, TaskProgress>();
        tasks.put("done", new TaskProgress("done", 1L, 1L, true));
        return new QuestProgressSnapshot(ParticipantId.player(player), quest, 1, QuestStatus.COMPLETED, tasks, 5L, 0);
    }

    private static RewardEntitlement entitlement(UUID player, UUID questId) {
        return entitlement(player, questId, true);
    }

    private static RewardEntitlement claimableEntitlement(UUID player, UUID questId) {
        return entitlement(player, questId, false);
    }

    private static RewardEntitlement entitlement(UUID player, UUID questId, boolean deliveryRequested) {
        RewardDefinition reward = new RewardDefinition("reward-0", "bq_standard:item",
            Collections.singletonMap("item.0.registryName", "minecraft:diamond"));
        QuestDefinition quest = new QuestDefinition(questId, 1, "Q", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.emptyList(), Collections.singletonList(reward), RepeatPolicy.never());
        return new RewardEntitlement(ParticipantId.player(player), player, quest, reward, 0, deliveryRequested);
    }

    private static QuestDefinition definition(UUID id, int version, String name) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("target", "3");
        parameters.put("factType", "minecraft:kill");
        return new QuestDefinition(id, version, name, "Description", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.singletonList(new TaskDefinition("task", "count", false, parameters)),
            Collections.singletonList(new RewardDefinition("reward", "item", Collections.<String, String>emptyMap())),
            RepeatPolicy.after(1000L));
    }

    private static Map<String, String> attributes(String subject, String amount) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        values.put("subject", subject);
        values.put("amount", amount);
        return values;
    }

    private static String ddl() throws Exception {
        InputStream input = QuestPostgresIntegrationTest.class.getResourceAsStream(
            "/com/jsirgalaxybase/quest/postgres/quest-postgresql-ddl.sql");
        assertNotNull(input);
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) >= 0) output.write(buffer, 0, read);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
