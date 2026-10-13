package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.jsirgalaxybase.quest.core.AuthenticatedQuestConsumptionDetectService;
import com.jsirgalaxybase.quest.core.BqCompatibleTaskEvaluatorRegistry;
import com.jsirgalaxybase.quest.core.ConsumptionApplyResult;
import com.jsirgalaxybase.quest.core.ConsumptionDetectBatchResult;
import com.jsirgalaxybase.quest.core.ConsumptionPort;
import com.jsirgalaxybase.quest.core.ConsumptionRequestAuthorizer;
import com.jsirgalaxybase.quest.core.ConsumptionSubmission;
import com.jsirgalaxybase.quest.core.ConsumptionSubmissionService;
import com.jsirgalaxybase.quest.core.ConsumptionSubmissionStatus;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.PlayerOnlyMembershipResolver;
import com.jsirgalaxybase.quest.core.QuestAssignmentPolicy;
import com.jsirgalaxybase.quest.core.QuestCompletionQuery;
import com.jsirgalaxybase.quest.core.QuestEngine;
import com.jsirgalaxybase.quest.core.QuestEvaluationPlanner;
import com.jsirgalaxybase.quest.core.QuestProgressSnapshot;
import com.jsirgalaxybase.quest.core.QuestRuntimeService;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.ScopedQuestAssignmentPolicy;
import com.jsirgalaxybase.quest.core.StoredQuestDefinition;
import com.jsirgalaxybase.quest.postgres.DriverManagerQuestDataSource;
import com.jsirgalaxybase.quest.postgres.JdbcConsumptionDetectBatchRepository;
import com.jsirgalaxybase.quest.postgres.JdbcConsumptionSubmissionRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestChapterRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestConnectionManager;
import com.jsirgalaxybase.quest.postgres.JdbcQuestContentMigrationRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestDefinitionRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestRuntimeRepository;
import com.jsirgalaxybase.quest.postgres.JdbcQuestTransaction;

/** Runs fixed-source consuming item/fluid profiles through import, PostgreSQL publication and Quest-level Detect. */
public class BqSourceProfileQuestDetectPostgresTest {
    private DriverManagerQuestDataSource root;
    private DriverManagerQuestDataSource scoped;
    private String schema;

    @Before public void setUp() throws Exception {
        String url = System.getenv("QUEST_TEST_POSTGRES_URL");
        Assume.assumeTrue("an explicitly isolated Quest test PostgreSQL URL is required",
            url != null && !url.trim().isEmpty());
        if (!QuestImporterTestDatabaseUrl.isExplicitlyIsolated(url,
            System.getenv("QUEST_TEST_POSTGRES_ISOLATED"))) {
            throw new IllegalStateException("Quest importer tests require an explicitly acknowledged *_test or *_it URL");
        }
        String user = System.getenv("POSTGRES_USER");
        String password = System.getenv("POSTGRES_PASSWORD");
        if (user == null || password == null) throw new IllegalStateException(
            "POSTGRES_USER and POSTGRES_PASSWORD are required for isolated integration tests");
        Class.forName("org.postgresql.Driver");
        root = new DriverManagerQuestDataSource(url, user, password);
        schema = "bq_source_detect_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = root.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA \"" + schema + "\"");
        }
        String scopedUrl = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema;
        scoped = new DriverManagerQuestDataSource(scopedUrl, user, password);
        try (Connection connection = scoped.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(ddl());
        }
    }

    @After public void tearDown() throws Exception {
        if (root != null && schema != null) try (Connection connection = root.getConnection();
            Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE");
        }
    }

    @Test public void fixedSourceConsumingProfilesPersistThroughPlayerQuestDetect() throws Exception {
        String source = System.getenv("BQ_TEST_DEFAULT_QUESTS");
        Assume.assumeTrue("read-only pinned DefaultQuests is required", source != null && !source.trim().isEmpty());
        BqMigrationManifest pinned = new BqMigrationPlanner().plan(Paths.get(source));
        assertEquals("9795d6c7fe71e72e91fed8cdc3dea632ab091ccf040e71707d2dd0d90cd355fc",
            pinned.getSourceHash());

        Map<String, BqImportedQuest> profileQuests = new LinkedHashMap<String, BqImportedQuest>();
        Map<String, String> profileTaskKeys = new LinkedHashMap<String, String>();
        for (BqImportedQuest imported : pinned.getQuests()) {
            for (com.jsirgalaxybase.quest.core.TaskDefinition task : imported.getDefinition().getTasks()) {
                if (!"bq_standard:retrieval".equals(task.getTypeId())
                    && !"bq_standard:fluid".equals(task.getTypeId())) continue;
                Map<String, String> parameters = task.getParameters();
                if (!"true".equals(parameters.get("consume")) || "true".equals(parameters.get("autoConsume"))) {
                    continue;
                }
                String profile = task.getTypeId() + ";ignoreNBT=" + parameters.get("ignoreNBT")
                    + ";partialMatch=" + parameters.get("partialMatch")
                    + ";groupDetect=" + parameters.get("groupDetect");
                if (!profileQuests.containsKey(profile)) {
                    profileQuests.put(profile, imported);
                    profileTaskKeys.put(profile, task.getKey());
                }
            }
        }
        assertEquals(new LinkedHashSet<String>(java.util.Arrays.asList(
            "bq_standard:retrieval;ignoreNBT=false;partialMatch=true;groupDetect=false",
            "bq_standard:retrieval;ignoreNBT=true;partialMatch=true;groupDetect=false",
            "bq_standard:fluid;ignoreNBT=false;partialMatch=null;groupDetect=false")), profileQuests.keySet());

        List<BqImportedQuest> selected = new ArrayList<BqImportedQuest>();
        Set<UUID> selectedIds = new LinkedHashSet<UUID>();
        for (BqImportedQuest quest : profileQuests.values()) {
            if (selectedIds.add(quest.getDefinition().getId())) selected.add(quest);
        }
        BqMigrationManifest subset = new BqMigrationManifest(pinned.getBatchId() + "-n8-detect-profile",
            pinned.getSourceHash(), selected, Collections.<BqImportedChapter>emptyList(),
            Collections.<BqMigrationIssue>emptyList());

        JdbcQuestConnectionManager connections = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(connections);
        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(connections);
        JdbcQuestRuntimeRepository progress = new JdbcQuestRuntimeRepository(connections);
        new BqMigrationDatabaseService(transaction, definitions, new JdbcQuestChapterRepository(connections),
            new JdbcQuestContentMigrationRepository(connections)).apply(subset, 1000L);

        Set<UUID> completedForTestUnlock = new LinkedHashSet<UUID>();
        for (BqImportedQuest quest : pinned.getQuests()) completedForTestUnlock.add(quest.getDefinition().getId());
        QuestCompletionQuery completions = participant -> completedForTestUnlock;
        PlayerOnlyMembershipResolver memberships = new PlayerOnlyMembershipResolver();
        QuestAssignmentPolicy assignments = new ScopedQuestAssignmentPolicy();
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions::findAllPublished, completions,
            memberships, assignments);
        QuestEngine engine = new QuestEngine();
        ConsumptionRequestAuthorizer authorizer = new ConsumptionRequestAuthorizer(definitions::findAllPublished,
            progress, completions, engine, memberships, assignments);
        QuestRuntimeService runtime = new QuestRuntimeService(progress, transaction,
            new com.jsirgalaxybase.quest.core.QuestFactProjector(
                BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), engine);
        JdbcConsumptionSubmissionRepository submissionsRepository =
            new JdbcConsumptionSubmissionRepository(connections);
        List<String> appliedSubmissionKeys = new ArrayList<String>();
        ConsumptionPort testInventory = request -> {
            appliedSubmissionKeys.add(request.getSubmissionKey());
            return ConsumptionApplyResult.applied("isolated-source-profile-test", request.getAmounts());
        };
        ConsumptionSubmissionService submissions = new ConsumptionSubmissionService(submissionsRepository,
            testInventory, runtime, planner, transaction);
        AuthenticatedQuestConsumptionDetectService detect = new AuthenticatedQuestConsumptionDetectService("s2",
            new JdbcConsumptionDetectBatchRepository(connections), authorizer, submissions, runtime, planner,
            transaction);

        long now = 2000L;
        for (Map.Entry<String, BqImportedQuest> entry : profileQuests.entrySet()) {
            UUID player = UUID.randomUUID();
            UUID questId = entry.getValue().getDefinition().getId();
            // Make every source prerequisite available while keeping the target Quest itself incomplete.
            completedForTestUnlock.remove(questId);
            StoredQuestDefinition published = definitions.findPublished(questId).get();
            String taskKey = profileTaskKeys.get(entry.getKey());
            com.jsirgalaxybase.quest.core.TaskDefinition task = null;
            for (com.jsirgalaxybase.quest.core.TaskDefinition candidate : published.getDefinition().getTasks()) {
                if (taskKey.equals(candidate.getKey())) task = candidate;
            }
            assertNotNull(task);
            int previousCalls = appliedSubmissionKeys.size();
            long previousSubmissionRows = count("galaxy_quest_consumption_submission");
            ConsumptionDetectBatchResult result = detect.detect(player, questId, now++);
            assertEquals(ConsumptionDetectBatchResult.Status.COMPLETED, result.getStatus());
            assertTrue("Quest Detect should durably submit the actual source-profile task",
                appliedSubmissionKeys.size() > previousCalls);
            for (ConsumptionSubmission submission : result.getSubmissions()) {
                assertEquals(ConsumptionSubmissionStatus.CONFIRMED, submission.getStatus());
                assertEquals(ParticipantId.player(player), submission.getRequest().getParticipantId());
                com.jsirgalaxybase.quest.core.TaskDefinition submittedTask = null;
                for (com.jsirgalaxybase.quest.core.TaskDefinition candidate : published.getDefinition().getTasks()) {
                    if (candidate.getKey().equals(submission.getRequest().getTaskKey())) submittedTask = candidate;
                }
                assertNotNull(submittedTask);
                assertEquals("bq_standard:fluid".equals(submittedTask.getTypeId()) ? "fluid" : "item",
                    submission.getRequest().getKind());
            }
            QuestProgressSnapshot saved = progress.findProgress(ParticipantId.player(player), questId,
                published.getDefinition().getVersion()).get();
            assertTrue("the source Quest must remain available for its other non-consuming requirements",
                saved.getStatus() == QuestStatus.IN_PROGRESS || saved.getStatus() == QuestStatus.COMPLETED);
            assertTrue("the actual consuming source task must persist complete after Quest Detect",
                saved.getTasks().get(taskKey).isComplete());
            assertEquals(result.getSubmissions().size(),
                count("galaxy_quest_consumption_submission") - previousSubmissionRows);
            assertEquals(0L, count("galaxy_quest_consumption_detect_batch"));
            int completedCalls = appliedSubmissionKeys.size();
            try {
                assertEquals(ConsumptionDetectBatchResult.Status.NO_CONSUMING_TASKS,
                    detect.detect(player, questId, now++).getStatus());
            } catch (IllegalArgumentException completedQuest) {
                assertTrue("a repeated Detect may refuse an already-completed Quest",
                    completedQuest.getMessage().contains("not accepting consumption"));
            }
            assertEquals("a repeated Detect cannot consume the source-profile task twice", completedCalls,
                appliedSubmissionKeys.size());
            assertFalse("PLAYER source progress must not leak to another account", progress.findProgress(
                ParticipantId.player(UUID.randomUUID()), questId, published.getDefinition().getVersion()).isPresent());
            completedForTestUnlock.add(questId);
        }
    }

    private long count(String table) throws Exception {
        try (Connection connection = scoped.getConnection(); Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static String ddl() throws Exception {
        InputStream input = BqSourceProfileQuestDetectPostgresTest.class.getResourceAsStream(
            "/com/jsirgalaxybase/quest/postgres/quest-postgresql-ddl.sql");
        assertNotNull(input);
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            for (int read; (read = in.read(buffer)) >= 0;) out.write(buffer, 0, read);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
