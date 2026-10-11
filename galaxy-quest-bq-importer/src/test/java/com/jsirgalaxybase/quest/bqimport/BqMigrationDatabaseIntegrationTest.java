package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.junit.*;
import com.jsirgalaxybase.quest.core.AuthenticatedCheckboxConfirmationService;
import com.jsirgalaxybase.quest.core.BqCompatibleTaskEvaluatorRegistry;
import com.jsirgalaxybase.quest.core.BqHuntPartyAssignmentPolicy;
import com.jsirgalaxybase.quest.core.BqPartyActiveQuestQuery;
import com.jsirgalaxybase.quest.core.FactProcessingResult;
import com.jsirgalaxybase.quest.core.FactProcessingStatus;
import com.jsirgalaxybase.quest.core.GameplayFact;
import com.jsirgalaxybase.quest.core.ParticipantId;
import com.jsirgalaxybase.quest.core.ParticipantMemberRole;
import com.jsirgalaxybase.quest.core.ParticipantMutationStatus;
import com.jsirgalaxybase.quest.core.RewardClaimStatus;
import com.jsirgalaxybase.quest.core.QuestCompletionQuery;
import com.jsirgalaxybase.quest.core.QuestEvaluationPlanner;
import com.jsirgalaxybase.quest.core.QuestFactProjector;
import com.jsirgalaxybase.quest.core.QuestObservationPlan;
import com.jsirgalaxybase.quest.core.QuestRuntimeService;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.StoredQuestDefinition;
import com.jsirgalaxybase.quest.postgres.*;

public class BqMigrationDatabaseIntegrationTest {
    private DriverManagerQuestDataSource root,scoped; private String schema;
    @Before public void setUp() throws Exception {
        String user=System.getenv("POSTGRES_USER"), password=System.getenv("POSTGRES_PASSWORD");
        String url=System.getenv("QUEST_TEST_POSTGRES_URL");
        if(url==null||url.trim().isEmpty())
            Assume.assumeTrue("QUEST_TEST_POSTGRES_URL is required for schema-writing integration tests",false);
        if(!QuestImporterTestDatabaseUrl.isExplicitlyIsolated(url,System.getenv("QUEST_TEST_POSTGRES_ISOLATED")))
            throw new IllegalStateException("Quest importer tests require an explicitly acknowledged *_test or *_it database URL");
        if(user==null||password==null)
            throw new IllegalStateException("POSTGRES_USER and POSTGRES_PASSWORD are required when QUEST_TEST_POSTGRES_URL is configured");
        Class.forName("org.postgresql.Driver");
        root=new DriverManagerQuestDataSource(url,user,password);
        schema="bq_migration_it_"+UUID.randomUUID().toString().replace("-","");
        try(Connection connection=root.getConnection();Statement statement=connection.createStatement()){
            statement.execute("CREATE SCHEMA \""+schema+"\"");
        }catch(SQLException unavailable){
            schema=null;
            throw new IllegalStateException("configured isolated Quest importer PostgreSQL is unavailable",unavailable);
        }
        String scopedUrl=url+(url.contains("?")?"&":"?")+"currentSchema="+schema;
        scoped=new DriverManagerQuestDataSource(scopedUrl,user,password);
        try(Connection connection=scoped.getConnection();Statement statement=connection.createStatement()){
            statement.execute(ddl());
        }
    }
    @After public void tearDown() throws Exception {if(root!=null&&schema!=null)try(Connection connection=root.getConnection();Statement statement=connection.createStatement()){statement.execute("DROP SCHEMA IF EXISTS \""+schema+"\" CASCADE");}}
    @Test public void dryRunWritesNothingApplyIsIdempotentAndRollbackRetiresBatch() throws Exception {BqMigrationManifest manifest=new BqMigrationPlanner().plan(fixture());JdbcQuestConnectionManager manager=new JdbcQuestConnectionManager(scoped);BqMigrationDatabaseService service=new BqMigrationDatabaseService(new JdbcQuestTransaction(manager),new JdbcQuestDefinitionRepository(manager),new JdbcQuestChapterRepository(manager),new JdbcQuestContentMigrationRepository(manager));
        BqMigrationDryRun preview=service.dryRun(manifest);assertTrue(preview.isApplicable());assertEquals(2,preview.count(QuestContentMigrationItem.Action.CREATED));assertEquals(0,count("galaxy_quest_content_migration_batch"));QuestContentMigrationBatch staged=service.stage(manifest);assertEquals(QuestContentMigrationBatch.Status.PREVIEW,staged.getStatus());assertEquals(1,count("galaxy_quest_content_migration_batch"));assertEquals(0,countPublished("galaxy_quest_definition"));assertEquals(staged.getBatchId(),service.stage(manifest).getBatchId());
        QuestContentMigrationBatch first=service.apply(manifest,1000L);assertEquals(QuestContentMigrationBatch.Status.APPLIED,first.getStatus());assertEquals(1,count("galaxy_quest_content_migration_batch"));assertEquals(1,countPublished("galaxy_quest_definition"));assertEquals(1,countPublished("galaxy_quest_chapter_definition"));
        com.jsirgalaxybase.quest.core.QuestCenterPage center=new JdbcQuestCenterQuery(manager).loadPageForPlayer(UUID.randomUUID(),com.jsirgalaxybase.quest.core.QuestCenterPageRequest.initial());assertEquals(1,center.getChapters().size());assertEquals(1,center.getQuests().size());assertEquals("Q",center.getQuests().get(0).getDefinition().getName());
        QuestContentMigrationBatch second=service.apply(manifest,2000L);assertEquals(first.getBatchId(),second.getBatchId());assertEquals(1,count("galaxy_quest_content_migration_batch"));assertEquals(1,count("galaxy_quest_definition"));
        service.rollback(first.getBatchId());assertEquals(0,countPublished("galaxy_quest_definition"));assertEquals(0,countPublished("galaxy_quest_chapter_definition"));assertEquals(0,count("galaxy_quest_chapter_catalog"));assertEquals("ROLLED_BACK",status(first.getBatchId()));
        service.rollback(first.getBatchId());assertEquals("ROLLED_BACK",status(first.getBatchId()));
    }
    @Test public void failedApplyRollsBackEveryMigrationWrite() throws Exception {BqMigrationManifest manifest=new BqMigrationPlanner().plan(fixture());JdbcQuestConnectionManager manager=new JdbcQuestConnectionManager(scoped);JdbcQuestTransaction transaction=new JdbcQuestTransaction(manager);JdbcQuestDefinitionRepository definitions=new JdbcQuestDefinitionRepository(manager);BqMigrationDatabaseService service=new BqMigrationDatabaseService(transaction,definitions,new JdbcQuestChapterRepository(manager),new JdbcQuestContentMigrationRepository(manager));transaction.inTransaction(()->{definitions.createDraft(manifest.getQuests().get(0).getDefinition());return null;});try{service.apply(manifest,1L);fail("expected version conflict");}catch(com.jsirgalaxybase.quest.core.QuestDefinitionConflictException expected){}assertEquals(0,count("galaxy_quest_content_migration_batch"));assertEquals(1,count("galaxy_quest_definition"));assertEquals(0,count("galaxy_quest_chapter_definition"));}
    @Test public void changedContentCreatesControlledVersionAndProtectsEarlierRollback() throws Exception {Path firstSource=fixture();BqMigrationManifest first=new BqMigrationPlanner().plan(firstSource);JdbcQuestConnectionManager manager=new JdbcQuestConnectionManager(scoped);BqMigrationDatabaseService service=new BqMigrationDatabaseService(new JdbcQuestTransaction(manager),new JdbcQuestDefinitionRepository(manager),new JdbcQuestChapterRepository(manager),new JdbcQuestContentMigrationRepository(manager));service.apply(first,1L);Path quest=firstSource.resolve("Quests/line/q.json");String changed=new String(Files.readAllBytes(quest),StandardCharsets.UTF_8).replace("\"Q\"","\"Q2\"");Files.write(quest,changed.getBytes(StandardCharsets.UTF_8));BqMigrationManifest second=new BqMigrationPlanner().plan(firstSource);QuestContentMigrationBatch applied=service.apply(second,2L);QuestContentMigrationItem questItem=null;for(QuestContentMigrationItem item:applied.getItems())if(item.getKind()==QuestContentMigrationItem.Kind.QUEST)questItem=item;assertNotNull(questItem);assertEquals(2,questItem.getVersion());assertEquals(QuestContentMigrationItem.Action.VERSIONED,questItem.getAction());try{service.rollback(first.getBatchId());fail("later version must protect earlier batch");}catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("later quest version"));}service.rollback(second.getBatchId());assertEquals("Q",new JdbcQuestDefinitionRepository(manager).findPublished(questItem.getId()).get().getDefinition().getName());service.rollback(first.getBatchId());assertEquals(0,countPublished("galaxy_quest_definition"));}
    @Test public void blockedPreviewIsVisibleButCannotApplyOrRollback() throws Exception {Path source=fixture();Path quest=source.resolve("Quests/line/q.json");String invalid=new String(Files.readAllBytes(quest),StandardCharsets.UTF_8).replace("bq_standard:checkbox","addon:unknown");Files.write(quest,invalid.getBytes(StandardCharsets.UTF_8));BqMigrationManifest manifest=new BqMigrationPlanner().plan(source);assertFalse(manifest.isApplicable());JdbcQuestConnectionManager manager=new JdbcQuestConnectionManager(scoped);BqMigrationDatabaseService service=new BqMigrationDatabaseService(new JdbcQuestTransaction(manager),new JdbcQuestDefinitionRepository(manager),new JdbcQuestChapterRepository(manager),new JdbcQuestContentMigrationRepository(manager));QuestContentMigrationBatch staged=service.stage(manifest);assertEquals(QuestContentMigrationBatch.Status.BLOCKED,staged.getStatus());assertTrue(staged.getErrorCount()>0);assertEquals(0,countPublished("galaxy_quest_definition"));try{service.apply(manifest,1L);fail("blocked manifest must not apply");}catch(IllegalArgumentException expected){}try{service.rollback(staged.getBatchId());fail("blocked preview must not roll back");}catch(IllegalStateException expected){}}
    @Test public void importedContentPreservesChapterPrerequisiteGoalRewardAndPlayerBrowse() throws Exception {
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(richFixture());
        assertTrue(manifest.toText(), manifest.isApplicable());
        assertTrue(manifest.toText().contains("WARNING|UNPLACED_QUEST"));

        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(manager);
        JdbcQuestChapterRepository chapters = new JdbcQuestChapterRepository(manager);
        BqMigrationDatabaseService service = new BqMigrationDatabaseService(new JdbcQuestTransaction(manager),
            definitions, chapters, new JdbcQuestContentMigrationRepository(manager));
        service.apply(manifest, 1L);

        com.jsirgalaxybase.quest.core.StoredQuestDefinition second = definitions.findPublished(new UUID(0L, 8L)).get();
        assertEquals("进阶钢材", second.getDefinition().getName());
        assertTrue(second.getDefinition().getPrerequisites().contains(new UUID(0L, 7L)));
        assertEquals("bq_standard:retrieval", second.getDefinition().getTasks().get(0).getTypeId());
        assertEquals("64", second.getDefinition().getTasks().get(0).getParameters().get("target"));
        assertEquals("bq_standard:item", second.getDefinition().getRewards().get(0).getTypeId());
        assertTrue(definitions.findPublished(new UUID(0L, 9L)).isPresent());
        assertEquals(3, definitions.findAllPublished().size());

        com.jsirgalaxybase.quest.core.StoredQuestChapter chapter = chapters.findPublished(new UUID(0L, 1L)).get();
        assertEquals(2, chapter.getDefinition().getEntries().size());
        for (com.jsirgalaxybase.quest.core.QuestChapterEntry entry : chapter.getDefinition().getEntries()) {
            assertFalse(entry.getQuestId().equals(new UUID(0L, 9L)));
        }
        assertTrue(chapter.getDefinition().getIconReference().contains("minecraft:book"));
        assertEquals("base:textures/gui/quests.png", chapter.getDefinition().getBackgroundReference());

        com.jsirgalaxybase.quest.core.QuestCenterPage center = new JdbcQuestCenterQuery(manager)
            .loadPageForPlayer(UUID.randomUUID(), com.jsirgalaxybase.quest.core.QuestCenterPageRequest.initial());
        assertEquals(2, center.getChapters().size());
        com.jsirgalaxybase.quest.core.QuestCenterChapterSummary unplaced = center.getChapters().get(1);
        assertEquals(com.jsirgalaxybase.quest.core.QuestCenterPaginator.UNPLACED_CHAPTER_ID, unplaced.getId());
        assertEquals(1, unplaced.getTotal());
        assertEquals(2, center.getQuests().size());
    }

    @Test
    public void importedRepeatableLocationFlowsThroughPartyProgressAndJdbcClaimCycles() throws Exception {
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(locationRepeatFixture());
        assertTrue(manifest.toText(), manifest.isApplicable());

        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(manager);
        JdbcQuestRuntimeRepository progress = new JdbcQuestRuntimeRepository(manager);
        BqMigrationDatabaseService migrations = new BqMigrationDatabaseService(transaction, definitions,
            new JdbcQuestChapterRepository(manager), new JdbcQuestContentMigrationRepository(manager));
        migrations.apply(manifest, 1000L);

        UUID questId = new UUID(0L, 7L);
        StoredQuestDefinition imported = definitions.findPublished(questId).get();
        assertEquals(100L, imported.getDefinition().getRepeatPolicy().getCooldownMillis());
        assertTrue(QuestObservationPlan.fromPublished(Collections.singletonList(imported)).observesLocation());
        Map<String, String> importedLocation = imported.getDefinition().getTasks().get(0).getParameters();
        assertEquals("galaxy:location_observed", importedLocation.get("factType"));
        assertEquals("55", importedLocation.get("dimension"));
        assertEquals("-1", importedLocation.get("range"));
        assertEquals("false", importedLocation.get("invert"));

        UUID owner = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        ParticipantId party = ParticipantId.party(UUID.randomUUID());
        JdbcParticipantAdministrationRepository administration = new JdbcParticipantAdministrationRepository(manager);
        assertEquals(ParticipantMutationStatus.APPLIED, transaction.inTransaction(() -> administration.create(
            party, "Location test party", owner, 900L)));
        assertEquals(ParticipantMutationStatus.APPLIED, transaction.inTransaction(() -> administration.addMember(
            party, owner, member, ParticipantMemberRole.MEMBER, 0L, 950L)));

        JdbcQuestCompletionQuery completions = new JdbcQuestCompletionQuery(manager);
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions::findAllPublished, completions,
            new JdbcParticipantMembershipResolver(manager), new BqHuntPartyAssignmentPolicy(),
            new BqPartyActiveQuestQuery("s2", completions, progress,
                player -> owner.equals(player) || member.equals(player)));
        QuestRuntimeService runtime = new QuestRuntimeService(progress, transaction,
            new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()),
            new com.jsirgalaxybase.quest.core.QuestEngine());

        GameplayFact firstLocation = locationFact("location-import-cycle-0", owner, 1000L);
        FactProcessingResult firstCycle = runtime.apply(firstLocation, planner, 1000L);
        assertEquals(FactProcessingStatus.ACCEPTED, firstCycle.getStatus());
        assertEquals("the imported BQ location task fans out to each Party member's PLAYER row", 2,
            firstCycle.getEvaluations().size());
        assertFalse("replaying the exact imported location fact must preserve its original idempotency key",
            transaction.inTransaction(() -> progress.recordFactIfAbsent(firstLocation)));
        for (UUID player : Arrays.asList(owner, member)) {
            assertEquals(QuestStatus.COMPLETED, progress.findProgress(ParticipantId.player(player), questId, 1)
                .get().getStatus());
            assertTrue(transaction.inTransaction(() -> progress.hasClaimableRewardEntitlement(
                ParticipantId.player(player), questId, 1, 0)));
        }

        assertEquals(FactProcessingStatus.IGNORED,
            runtime.apply(locationFact("location-before-claim", owner, 1100L), planner, 1100L).getStatus());
        JdbcRewardClaimRepository claims = new JdbcRewardClaimRepository(manager);
        for (UUID player : Arrays.asList(owner, member)) {
            assertEquals(RewardClaimStatus.CLAIMED, transaction.inTransaction(() -> claims.claim(
                questId, 1, 0, player, 2000L)));
        }
        assertEquals(FactProcessingStatus.IGNORED,
            runtime.apply(locationFact("location-before-cooldown", owner, 2099L), planner, 2099L).getStatus());

        FactProcessingResult nextCycle = runtime.apply(locationFact("location-after-cooldown", owner, 2100L),
            planner, 2100L);
        assertEquals(FactProcessingStatus.ACCEPTED, nextCycle.getStatus());
        assertEquals(2, nextCycle.getEvaluations().size());
        for (UUID player : Arrays.asList(owner, member)) {
            com.jsirgalaxybase.quest.core.QuestProgressSnapshot saved = progress.findProgress(
                ParticipantId.player(player), questId, 1).get();
            assertEquals(1, saved.getCycle());
            assertEquals(QuestStatus.COMPLETED, saved.getStatus());
            assertTrue(transaction.inTransaction(() -> progress.hasClaimableRewardEntitlement(
                ParticipantId.player(player), questId, 1, 1)));
        }
    }

    @Test
    public void importedBqCheckboxIsAnAuthenticatedManualPlayerAction() throws Exception {
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(fixture());
        assertTrue(manifest.toText(), manifest.isApplicable());

        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(manager);
        JdbcQuestRuntimeRepository progress = new JdbcQuestRuntimeRepository(manager);
        BqMigrationDatabaseService migrations = new BqMigrationDatabaseService(transaction, definitions,
            new JdbcQuestChapterRepository(manager), new JdbcQuestContentMigrationRepository(manager));
        migrations.apply(manifest, 1000L);

        UUID questId = new UUID(0L, 7L);
        StoredQuestDefinition imported = definitions.findPublished(questId).get();
        assertEquals(1, imported.getDefinition().getTasks().size());
        com.jsirgalaxybase.quest.core.TaskDefinition checkbox = imported.getDefinition().getTasks().get(0);
        assertEquals("bq_standard:checkbox", checkbox.getTypeId());
        assertEquals("galaxy:task_confirmed", checkbox.getParameters().get("factType"));
        QuestObservationPlan observations = QuestObservationPlan.fromPublished(Collections.singletonList(imported));
        assertFalse("a checkbox is an explicit action, not an inventory polling task", observations.observesInventory());
        assertFalse(observations.observesFluidInventory());

        UUID clickingPlayer = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions::findAllPublished,
            new JdbcQuestCompletionQuery(manager), new JdbcParticipantMembershipResolver(manager),
            new BqHuntPartyAssignmentPolicy());
        QuestRuntimeService runtime = new QuestRuntimeService(progress, transaction,
            new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()),
            new com.jsirgalaxybase.quest.core.QuestEngine());
        AuthenticatedCheckboxConfirmationService confirmation = new AuthenticatedCheckboxConfirmationService(
            definitions, progress, runtime, planner, "s2");

        assertEquals(FactProcessingStatus.ACCEPTED,
            confirmation.confirm(clickingPlayer, questId, checkbox.getKey(), 2000L).getStatus());
        assertEquals(QuestStatus.COMPLETED,
            progress.findProgress(ParticipantId.player(clickingPlayer), questId, 1).get().getStatus());
        assertFalse("the default BQ checkbox click must not create progress for a different player",
            progress.findProgress(ParticipantId.player(otherPlayer), questId, 1).isPresent());
        assertEquals(FactProcessingStatus.IGNORED,
            confirmation.confirm(clickingPlayer, questId, checkbox.getKey(), 3000L).getStatus());
    }

    @Test
    public void importedRepeatingCheckboxPreservesFactKeyAcrossClaimCooldownAndServerChange() throws Exception {
        Path source = fixture();
        String json = "{\"questIDHigh:4\":0,\"questIDLow:4\":7,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Repeat check\","
            + "\"repeatTime:3\":2,\"repeat_relative:1\":1,\"autoClaim:1\":0}},"
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"}},"
            + "\"rewards:9\":{\"0:10\":{\"index:3\":0,\"rewardID:8\":\"bq_standard:item\","
            + "\"rewards:9\":{\"0:10\":{\"id:8\":\"minecraft:diamond\",\"Damage:2\":0,\"Count:3\":1}}}}}";
        Files.write(source.resolve("Quests/line/q.json"), json.getBytes(StandardCharsets.UTF_8));
        BqMigrationManifest manifest = new BqMigrationPlanner().plan(source);
        assertTrue(manifest.toText(), manifest.isApplicable());
        JdbcQuestConnectionManager manager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestTransaction transaction = new JdbcQuestTransaction(manager);
        JdbcQuestDefinitionRepository definitions = new JdbcQuestDefinitionRepository(manager);
        JdbcQuestRuntimeRepository progress = new JdbcQuestRuntimeRepository(manager);
        new BqMigrationDatabaseService(transaction, definitions, new JdbcQuestChapterRepository(manager),
            new JdbcQuestContentMigrationRepository(manager)).apply(manifest, 1000L);
        UUID questId = new UUID(0L, 7L);
        UUID player = UUID.randomUUID();
        String taskKey = definitions.findPublished(questId).get().getDefinition().getTasks().get(0).getKey();
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(definitions::findAllPublished,
            new JdbcQuestCompletionQuery(manager), new JdbcParticipantMembershipResolver(manager),
            new BqHuntPartyAssignmentPolicy());
        QuestRuntimeService runtime = new QuestRuntimeService(progress, transaction,
            new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()),
            new com.jsirgalaxybase.quest.core.QuestEngine());
        AuthenticatedCheckboxConfirmationService firstServer = new AuthenticatedCheckboxConfirmationService(
            definitions, progress, runtime, planner, "s2");
        assertEquals(FactProcessingStatus.ACCEPTED, firstServer.confirm(player, questId, taskKey, 2000L).getStatus());
        assertEquals(FactProcessingStatus.IGNORED, firstServer.confirm(player, questId, taskKey, 2100L).getStatus());
        assertEquals(1L, count("galaxy_quest_fact"));
        JdbcRewardClaimRepository claims = new JdbcRewardClaimRepository(manager);
        assertEquals(RewardClaimStatus.CLAIMED,
            transaction.inTransaction(() -> claims.claim(questId, 1, 0, player, 3000L)));
        assertEquals(FactProcessingStatus.IGNORED, firstServer.confirm(player, questId, taskKey, 3099L).getStatus());
        assertEquals(1L, count("galaxy_quest_fact"));

        // Rebuild service/connection objects as a different server, using only persisted state.
        JdbcQuestConnectionManager nextManager = new JdbcQuestConnectionManager(scoped);
        JdbcQuestRuntimeRepository nextProgress = new JdbcQuestRuntimeRepository(nextManager);
        JdbcQuestDefinitionRepository nextDefinitions = new JdbcQuestDefinitionRepository(nextManager);
        QuestEvaluationPlanner nextPlanner = new QuestEvaluationPlanner(nextDefinitions::findAllPublished,
            new JdbcQuestCompletionQuery(nextManager), new JdbcParticipantMembershipResolver(nextManager),
            new BqHuntPartyAssignmentPolicy());
        QuestRuntimeService nextRuntime = new QuestRuntimeService(nextProgress, new JdbcQuestTransaction(nextManager),
            new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()),
            new com.jsirgalaxybase.quest.core.QuestEngine());
        AuthenticatedCheckboxConfirmationService nextServer = new AuthenticatedCheckboxConfirmationService(
            nextDefinitions, nextProgress, nextRuntime, nextPlanner, "lobby");
        assertEquals(FactProcessingStatus.ACCEPTED, nextServer.confirm(player, questId, taskKey, 3100L).getStatus());
        assertEquals(1, nextProgress.findProgress(ParticipantId.player(player), questId, 1).get().getCycle());
        assertEquals(FactProcessingStatus.IGNORED, firstServer.confirm(player, questId, taskKey, 3101L).getStatus());
        assertEquals(2L, count("galaxy_quest_fact"));
        assertEquals(2L, count("galaxy_quest_reward_entitlement"));
        assertFalse(nextProgress.findProgress(ParticipantId.player(UUID.randomUUID()), questId, 1).isPresent());
    }

    private static GameplayFact locationFact(String eventId, UUID player, long time) {
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("dimension", "55");
        attributes.put("x", "0.5");
        attributes.put("y", "64.0");
        attributes.put("z", "0.5");
        attributes.put("biome", "1");
        return new GameplayFact("s2", eventId, player, "galaxy:location_observed", time, attributes);
    }

    private Path locationRepeatFixture() throws Exception {
        Path root = Files.createTempDirectory("bq-location-repeat");
        Path quests = Files.createDirectories(root.resolve("Quests/location"));
        String questJson = "{\"questIDHigh:4\":0,\"questIDLow:4\":7,"
            + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Repeating location\","
            + "\"repeatTime:3\":2,\"repeat_relative:1\":1,\"autoClaim:1\":0}},"
            + "\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:location\","
            + "\"dimension:3\":55,\"posX:3\":0,\"posY:3\":64,\"posZ:3\":0,\"range:3\":-1,"
            + "\"biome:3\":-1,\"invert:1\":0,\"taxiCabDist:1\":0,\"visible:1\":0,\"structure:8\":\"\"}},"
            + "\"rewards:9\":{\"0:10\":{\"index:3\":0,\"rewardID:8\":\"bq_standard:item\","
            + "\"rewards:9\":{\"0:10\":{\"id:8\":\"minecraft:diamond\",\"Damage:2\":0,\"Count:3\":1}}}}}";
        Files.write(quests.resolve("location.json"), questJson.getBytes(StandardCharsets.UTF_8));
        Files.write(root.resolve("QuestLinesOrder.txt"),
            "AAAAAAAAAAAAAAAAAAAAAQ==: Location\n".getBytes(StandardCharsets.UTF_8));
        Path chapter = Files.createDirectories(root.resolve("QuestLines/Location-AAAAAAAAAAAAAAAAAAAAAQ=="));
        Files.write(chapter.resolve("QuestLine.json"),
            ("{\"questLineIDHigh:4\":0,\"questLineIDLow:4\":1,"
                + "\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Location\"}}}")
                .getBytes(StandardCharsets.UTF_8));
        Files.write(chapter.resolve("location.json"),
            "{\"questIDHigh:4\":0,\"questIDLow:4\":7,\"sizeX:3\":24,\"sizeY:3\":24,\"x:3\":0,\"y:3\":0}"
                .getBytes(StandardCharsets.UTF_8));
        return root;
    }
    private Path fixture() throws Exception {Path root=Files.createTempDirectory("bq-db-plan");Path quests=Files.createDirectories(root.resolve("Quests/line"));Files.write(quests.resolve("q.json"),("{\"questIDHigh:4\":0,\"questIDLow:4\":7,\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"Q\"}},\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"}},\"rewards:9\":{}}").getBytes(StandardCharsets.UTF_8));Files.write(root.resolve("QuestLinesOrder.txt"),"AAAAAAAAAAAAAAAAAAAAAQ==: Main\n".getBytes(StandardCharsets.UTF_8));Path line=Files.createDirectories(root.resolve("QuestLines/Main-AAAAAAAAAAAAAAAAAAAAAQ=="));Files.write(line.resolve("q.json"),"{\"questIDHigh:4\":0,\"questIDLow:4\":7,\"sizeX:3\":24,\"sizeY:3\":24,\"x:3\":0,\"y:3\":0}".getBytes(StandardCharsets.UTF_8));return root;}
    private Path richFixture()throws Exception{Path root=Files.createTempDirectory("bq-rich-plan");Path quests=Files.createDirectories(root.resolve("Quests/line"));Files.write(quests.resolve("first.json"),"{\"questIDHigh:4\":0,\"questIDLow:4\":7,\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"基础钢材\"}},\"preRequisites:9\":{},\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"}},\"rewards:9\":{}}".getBytes(StandardCharsets.UTF_8));Files.write(quests.resolve("second.json"),"{\"questIDHigh:4\":0,\"questIDLow:4\":8,\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"进阶钢材\",\"desc:8\":\"提交一组钢锭\"}},\"preRequisites:9\":{\"0:10\":{\"questIDHigh:4\":0,\"questIDLow:4\":7}},\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:retrieval\",\"requiredItems:9\":{\"0:10\":{\"id:8\":\"minecraft:iron_ingot\",\"Damage:2\":0,\"Count:3\":64}}}},\"rewards:9\":{\"0:10\":{\"index:3\":0,\"rewardID:8\":\"bq_standard:item\",\"rewards:9\":{\"0:10\":{\"id:8\":\"minecraft:gold_ingot\",\"Damage:2\":0,\"Count:3\":2}}}}}".getBytes(StandardCharsets.UTF_8));Files.write(quests.resolve("unplaced.json"),"{\"questIDHigh:4\":0,\"questIDLow:4\":9,\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"未放置测试任务\"}},\"preRequisites:9\":{},\"tasks:9\":{\"0:10\":{\"index:3\":0,\"taskID:8\":\"bq_standard:checkbox\"}},\"rewards:9\":{}}".getBytes(StandardCharsets.UTF_8));Files.write(root.resolve("QuestLinesOrder.txt"),"AAAAAAAAAAAAAAAAAAAAAQ==: Main\n".getBytes(StandardCharsets.UTF_8));Path line=Files.createDirectories(root.resolve("QuestLines/Main-AAAAAAAAAAAAAAAAAAAAAQ=="));Files.write(line.resolve("QuestLine.json"),"{\"questLineIDHigh:4\":0,\"questLineIDLow:4\":1,\"properties:10\":{\"betterquesting:10\":{\"name:8\":\"钢铁时代\",\"icon:10\":{\"id:8\":\"minecraft:book\"},\"bg_image:8\":\"base:textures/gui/quests.png\"}}}".getBytes(StandardCharsets.UTF_8));Files.write(line.resolve("first.json"),"{\"questIDHigh:4\":0,\"questIDLow:4\":7,\"sizeX:3\":24,\"sizeY:3\":24,\"x:3\":0,\"y:3\":0}".getBytes(StandardCharsets.UTF_8));Files.write(line.resolve("second.json"),"{\"questIDHigh:4\":0,\"questIDLow:4\":8,\"sizeX:3\":24,\"sizeY:3\":24,\"x:3\":32,\"y:3\":0}".getBytes(StandardCharsets.UTF_8));return root;}
    private long count(String table)throws Exception{try(Connection c=scoped.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getLong(1);}}
    private long countPublished(String table)throws Exception{try(Connection c=scoped.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM "+table+" WHERE lifecycle='PUBLISHED'")){r.next();return r.getLong(1);}}
    private String status(String id)throws Exception{try(Connection c=scoped.getConnection();PreparedStatement s=c.prepareStatement("SELECT status FROM galaxy_quest_content_migration_batch WHERE batch_id=?")){s.setString(1,id);try(ResultSet r=s.executeQuery()){r.next();return r.getString(1);}}}
    private static String ddl()throws Exception{InputStream input=BqMigrationDatabaseIntegrationTest.class.getResourceAsStream("/com/jsirgalaxybase/quest/postgres/quest-postgresql-ddl.sql");assertNotNull(input);try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];for(int read;(read=in.read(buffer))>=0;)out.write(buffer,0,read);return new String(out.toByteArray(),StandardCharsets.UTF_8);}}
}
