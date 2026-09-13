package com.jsirgalaxybase.quest.postgres;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
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
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestCompletionRewardPort;
import com.jsirgalaxybase.quest.core.QuestDefinitionConflictException;
import com.jsirgalaxybase.quest.core.QuestDefinitionLifecycle;
import com.jsirgalaxybase.quest.core.QuestLogic;
import com.jsirgalaxybase.quest.core.QuestProgressSnapshot;
import com.jsirgalaxybase.quest.core.QuestEvaluationRequest;
import com.jsirgalaxybase.quest.core.QuestEngine;
import com.jsirgalaxybase.quest.core.QuestFactProjector;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.QuestRuntimeService;
import com.jsirgalaxybase.quest.core.FactCountTaskEvaluator;
import com.jsirgalaxybase.quest.core.FactProcessingResult;
import com.jsirgalaxybase.quest.core.ParticipantMembership;
import com.jsirgalaxybase.quest.core.RepeatPolicy;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.RewardDeliveryLease;
import com.jsirgalaxybase.quest.core.RewardChoiceSelectionStatus;
import com.jsirgalaxybase.quest.core.RewardClaimStatus;
import com.jsirgalaxybase.quest.core.RewardEntitlement;
import com.jsirgalaxybase.quest.core.TaskProgress;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.quest.core.TaskEvaluatorRegistry;
import com.jsirgalaxybase.quest.core.QuestChapterCatalogEntry;

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
        String baseUrl = "jdbc:postgresql://galaxy-base:5432/" + database;
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
            completion.complete("source-entitlement", ParticipantId.player(player), targetId, 20L));
        assertEquals(QuestCompletionRewardPort.Result.ALREADY_COMPLETED,
            completion.complete("source-entitlement", ParticipantId.player(player), targetId, 30L));

        QuestProgressSnapshot progress = runtime.findProgress(ParticipantId.player(player), targetId, 1).get();
        assertEquals(QuestStatus.COMPLETED, progress.getStatus());
        assertEquals(20L, progress.getCompletedAt());
        assertTrue(progress.getTasks().get("task").isComplete());
        assertEquals(Collections.singletonList(3L), progress.getTasks().get("task").getValues());
        assertEquals(1L, count("galaxy_quest_reward_entitlement"));
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
    public void choiceRewardCannotLeaseUntilAuthenticatedImmutableSelection() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestRuntimeRepository runtime = new JdbcQuestRuntimeRepository(manager);
        JdbcRewardDeliveryRepository delivery = new JdbcRewardDeliveryRepository(manager);
        JdbcRewardChoiceSelectionRepository choices = new JdbcRewardChoiceSelectionRepository(manager);
        UUID player = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("item.count", "2");
        RewardDefinition reward = new RewardDefinition("choice", "bq_standard:choice", parameters);
        QuestDefinition quest = new QuestDefinition(questId, 1, "Choice", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.<TaskDefinition>emptyList(), Collections.singletonList(reward),
            RepeatPolicy.never());
        RewardEntitlement entitlement = new RewardEntitlement(ParticipantId.player(player), quest, reward, 0);
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
        assertEquals(RewardChoiceSelectionStatus.CONFLICT, transaction.inTransaction(
            () -> choices.select(entitlement.getEntitlementKey(), player, 0, 3L)));

        RewardDeliveryLease lease = transaction.inTransaction(
            () -> delivery.leaseAvailable("worker", 3L, 100L, 10)).get(0);
        assertEquals("1", lease.getReward().getParameters().get("choice.index"));
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
        assertEquals(RewardClaimStatus.CLAIMED, transaction.inTransaction(
            () -> claims.claim(questId, 1, 0, player, 3L)));
        RewardDeliveryLease lease = transaction.inTransaction(
            () -> delivery.leaseAvailable("worker", 3L, 100L, 10)).get(0);
        assertEquals("1", lease.getReward().getParameters().get("choice.index"));
    }

    @Test
    public void consumptionSubmissionIsDurableIdempotentAndTransitionChecked() throws Exception {
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(manager);
        JdbcConsumptionSubmissionRepository submissions = new JdbcConsumptionSubmissionRepository(manager);
        UUID player = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        QuestDefinition definition = definition(questId, 1, "Consume");
        transaction.inTransaction(() -> definitions.createDraft(definition));
        ConsumptionRequest request = new ConsumptionRequest("submit-1", "s2", player, ParticipantId.player(player),
            questId, 1, "task", "item", Arrays.asList(2L, 3L), Collections.singletonMap("item.count", "2"), 10L);

        ConsumptionSubmission prepared = transaction.inTransaction(() -> submissions.prepareIfAbsent(request));
        assertEquals(ConsumptionSubmissionStatus.PREPARED, prepared.getStatus());
        assertEquals(ConsumptionSubmissionStatus.PREPARED,
            transaction.inTransaction(() -> submissions.prepareIfAbsent(request)).getStatus());
        assertEquals(ConsumptionSubmissionStatus.APPLIED,
            transaction.inTransaction(() -> submissions.markApplied("submit-1", "player-nbt:submit-1",
                java.util.Arrays.asList(2L, 1L), 20L)).getStatus());
        assertEquals(java.util.Arrays.asList(2L, 1L), transaction.inTransaction(() -> submissions.find("submit-1"))
            .get().getAppliedAmounts());
        assertEquals(ConsumptionSubmissionStatus.CONFIRMED,
            transaction.inTransaction(() -> submissions.markConfirmed("submit-1", 30L)).getStatus());
        assertEquals(ConsumptionSubmissionStatus.CONFIRMED,
            transaction.inTransaction(() -> submissions.markConfirmed("submit-1", 31L)).getStatus());
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
        RewardDefinition reward = new RewardDefinition("reward-0", "bq_standard:item",
            Collections.singletonMap("item.0.registryName", "minecraft:diamond"));
        QuestDefinition quest = new QuestDefinition(questId, 1, "Q", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.emptyList(), Collections.singletonList(reward), RepeatPolicy.never());
        return new RewardEntitlement(ParticipantId.player(player), quest, reward, 0);
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
