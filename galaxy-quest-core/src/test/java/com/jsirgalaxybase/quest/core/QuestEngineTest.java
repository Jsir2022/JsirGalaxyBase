package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class QuestEngineTest {
    @Test
    public void completesAndIssuesStableEntitlementOnlyOnTransition() {
        UUID player = UUID.randomUUID();
        QuestDefinition quest = quest(QuestLogic.AND);
        Map<String, TaskProgress> observed = new LinkedHashMap<String, TaskProgress>();
        observed.put("collect", new TaskProgress("collect", 8, 8, false));
        observed.put("visit", new TaskProgress("visit", 1, 1, true));

        QuestEvaluation first = new QuestEngine().evaluate(player, quest, null, observed,
            Collections.<UUID>emptySet(), 1234L);
        assertEquals(QuestStatus.COMPLETED, first.getProgress().getStatus());
        assertEquals(1, first.getEntitlements().size());
        assertEquals(RewardDeliveryStatus.CLAIMABLE, first.getEntitlements().get(0).getInitialDeliveryStatus());
        assertEquals("player:" + player + ":" + quest.getId() + ":1:0:coins",
            first.getEntitlements().get(0).getEntitlementKey());

        QuestEvaluation replay = new QuestEngine().evaluate(player, quest, first.getProgress(), observed,
            Collections.<UUID>emptySet(), 9999L);
        assertTrue(replay.getEntitlements().isEmpty());
        assertEquals(1234L, replay.getProgress().getCompletedAt());
    }

    @Test
    public void bqOptionalRetrievalDoesNotBlockQuestCompletion() {
        UUID player = UUID.randomUUID();
        TaskDefinition required = new TaskDefinition("required", "bq_standard:checkbox", false,
            Collections.<String, String>emptyMap());
        TaskDefinition optionalRetrieval = new TaskDefinition("bonus", "bq_standard:optional_retrieval", true,
            map("progressMode", "item-vector", "item.count", "1", "item.0.amount", "5", "consume", "false"));
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Optional retrieval", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Arrays.asList(required, optionalRetrieval),
            Collections.<RewardDefinition>emptyList());

        Map<String, TaskProgress> observed = new LinkedHashMap<String, TaskProgress>();
        observed.put("required", new TaskProgress("required", 1L, 1L, true));
        observed.put("bonus", new TaskProgress("bonus", 0L, 5L, false));
        QuestEvaluation result = new QuestEngine().evaluate(player, quest, null, observed,
            Collections.<UUID>emptySet(), 100L);

        assertEquals(QuestStatus.COMPLETED, result.getProgress().getStatus());
        assertFalse(result.getProgress().getTasks().get("bonus").isComplete());
    }

    @Test
    public void completedBqQuestFreezesOptionalRetrievalUntilANewCycleBegins() {
        UUID player = UUID.randomUUID();
        TaskDefinition required = new TaskDefinition("required", "bq_standard:checkbox", false,
            Collections.<String, String>emptyMap());
        TaskDefinition optionalRetrieval = new TaskDefinition("bonus", "bq_standard:optional_retrieval", true,
            map("progressMode", "item-vector", "item.count", "1", "item.0.amount", "5", "consume", "false"));
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Optional retrieval freeze", "",
            QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(), Arrays.asList(required, optionalRetrieval),
            Collections.<RewardDefinition>emptyList(), RepeatPolicy.after(100L));
        Map<String, TaskProgress> beforeCompletion = new LinkedHashMap<String, TaskProgress>();
        beforeCompletion.put("required", new TaskProgress("required", 1L, 1L, true));
        beforeCompletion.put("bonus", new TaskProgress("bonus", 0L, 5L, false));
        QuestEvaluation completed = new QuestEngine().evaluate(player, quest, null, beforeCompletion,
            Collections.<UUID>emptySet(), 100L);
        assertEquals(QuestStatus.COMPLETED, completed.getProgress().getStatus());

        Map<String, TaskProgress> laterObservation = new LinkedHashMap<String, TaskProgress>();
        laterObservation.put("required", new TaskProgress("required", 1L, 1L, true));
        laterObservation.put("bonus", new TaskProgress("bonus", 5L, 5L, true));
        QuestEvaluation later = new QuestEngine().evaluate(player, quest, completed.getProgress(), laterObservation,
            Collections.<UUID>emptySet(), 101L);

        assertEquals("BQ stops detecting a completed quest before the next repeat cycle",
            QuestStatus.COMPLETED, later.getProgress().getStatus());
        assertEquals(Arrays.asList(0L), later.getProgress().getTasks().get("bonus").getValues());
        assertFalse(later.getProgress().getTasks().get("bonus").isComplete());
        assertTrue(later.getEntitlements().isEmpty());

        QuestEvaluation nextCycle = new QuestEngine().evaluate(player, quest, later.getProgress(), laterObservation,
            Collections.<UUID>emptySet(), 200L);
        assertEquals(1, nextCycle.getProgress().getCycle());
        assertEquals(QuestStatus.COMPLETED, nextCycle.getProgress().getStatus());
        assertEquals(Arrays.asList(5L), nextCycle.getProgress().getTasks().get("bonus").getValues());
        assertTrue(nextCycle.getProgress().getTasks().get("bonus").isComplete());
    }

    @Test
    public void repeatableCraftingStartsNextCycleWithoutPriorSourceEventLedger() {
        UUID player = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        TaskDefinition crafting = new TaskDefinition("craft", "bq_standard:crafting", false,
            map("progressMode", "item-vector", "item.count", "1", "item.0.amount", "3"));
        QuestDefinition quest = new QuestDefinition(questId, 1, "Repeat crafting", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(crafting),
            Collections.<RewardDefinition>emptyList(), RepeatPolicy.after(100L));
        TaskProgress firstCycle = TaskProgress.empty(crafting).advanceFromSource("s2", Collections.singletonList(3L));
        QuestProgressSnapshot completed = new QuestProgressSnapshot(ParticipantId.player(player), questId, 1,
            QuestStatus.COMPLETED, Collections.singletonMap("craft", firstCycle), 100L, 0);
        TaskProgress nextSourceEvent = TaskProgress.empty(crafting).advanceFromSource("lobby",
            Collections.singletonList(1L));

        QuestEvaluation nextCycle = new QuestEngine().evaluate(player, quest, completed,
            Collections.singletonMap("craft", nextSourceEvent), Collections.<UUID>emptySet(), 200L);

        TaskProgress progress = nextCycle.getProgress().getTasks().get("craft");
        assertEquals(1, nextCycle.getProgress().getCycle());
        assertEquals(Arrays.asList(1L), progress.getValues());
        assertTrue("source event ledger from the completed cycle must not leak into the new cycle",
            progress.getSourceContributions().containsKey("lobby"));
        assertFalse(progress.getSourceContributions().containsKey("s2"));
        assertEquals(Arrays.asList(1L), progress.getSourceContributions().get("lobby").getEventValues());

        TaskProgress recapturedLifetimeStatistics = TaskProgress.empty(crafting).observeFromSource("s2",
            Collections.singletonList(3L));
        QuestEvaluation afterStatisticsRefresh = new QuestEngine().evaluate(player, quest, nextCycle.getProgress(),
            Collections.singletonMap("craft", recapturedLifetimeStatistics), Collections.<UUID>emptySet(), 201L);
        TaskProgress refreshedProgress = afterStatisticsRefresh.getProgress().getTasks().get("craft");
        assertEquals("a newly observed absolute counter is still counted in the new cycle", Arrays.asList(4L),
            refreshedProgress.getValues());
        assertEquals(Arrays.asList(0L), refreshedProgress.getSourceContributions().get("s2").getEventValues());
        assertEquals(Arrays.asList(3L), refreshedProgress.getSourceContributions().get("s2").getObservedValues());
    }

    @Test
    public void autoClaimBehaviorRequestsImmediateDelivery() {
        UUID player = UUID.randomUUID();
        QuestDefinition base = quest(QuestLogic.AND);
        QuestDefinition quest = new QuestDefinition(base.getId(), base.getVersion(), base.getName(),
            base.getDescription(), base.getPrerequisiteLogic(), base.getTaskLogic(), base.getPrerequisites(),
            base.getTasks(), base.getRewards(), base.getRepeatPolicy(), QuestBehavior.builder().autoClaim(true).build());
        Map<String, TaskProgress> observed = new LinkedHashMap<String, TaskProgress>();
        observed.put("collect", new TaskProgress("collect", 8, 8, true));
        observed.put("visit", new TaskProgress("visit", 1, 1, true));

        QuestEvaluation result = new QuestEngine().evaluate(player, quest, null, observed,
            Collections.<UUID>emptySet(), 1234L);

        assertEquals(RewardDeliveryStatus.PENDING, result.getEntitlements().get(0).getInitialDeliveryStatus());
    }

    @Test
    public void incompleteBqRetrievalProgressTracksCurrentInventoryWhileCompletedProgressStaysLatched() {
        UUID player = UUID.randomUUID();
        TaskDefinition retrieval = new TaskDefinition("items", "bq_standard:retrieval", false,
            map("progressMode", "item-vector", "item.count", "2", "item.0.amount", "3",
                "item.1.amount", "10",
                "consume", "false", "groupDetect", "false"));
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Retrieval", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(retrieval),
            Collections.<RewardDefinition>emptyList());
        QuestProgressSnapshot previous = progress(quest, player, QuestStatus.IN_PROGRESS,
            new TaskProgress("items", Arrays.asList(3L, 4L), Arrays.asList(3L, 10L), false));

        QuestEvaluation lower = new QuestEngine().evaluate(player, quest, previous,
            Collections.singletonMap("items", new TaskProgress("items", Arrays.asList(0L, 1L),
                Arrays.asList(3L, 10L), false)),
            Collections.<UUID>emptySet(), 10L);
        assertEquals(Arrays.asList(3L, 1L), lower.getProgress().getTasks().get("items").getValues());

        QuestProgressSnapshot completed = progress(quest, player, QuestStatus.IN_PROGRESS,
            new TaskProgress("items", Arrays.asList(3L, 10L), Arrays.asList(3L, 10L), true));
        QuestEvaluation latched = new QuestEngine().evaluate(player, quest, completed,
            Collections.singletonMap("items", new TaskProgress("items", Arrays.asList(0L, 0L),
                Arrays.asList(3L, 10L), false)),
            Collections.<UUID>emptySet(), 11L);
        assertTrue(latched.getProgress().getTasks().get("items").isComplete());
    }

    @Test
    public void nonConsumingBqFluidProgressRecomputesLikeRetrievalAndHonorsGroupDetect() {
        UUID player = UUID.randomUUID();
        TaskDefinition fluid = new TaskDefinition("fluids", "bq_standard:fluid", false,
            map("factType", "galaxy:fluid_inventory_observed", "progressMode", "fluid-vector",
                "fluid.count", "2", "fluid.0.amount", "1000", "fluid.1.amount", "2000",
                "consume", "false", "groupDetect", "false"));
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Fluid snapshot", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(fluid),
            Collections.<RewardDefinition>emptyList());
        QuestProgressSnapshot previous = progress(quest, player, QuestStatus.IN_PROGRESS,
            new TaskProgress("fluids", Arrays.asList(1000L, 1500L), Arrays.asList(1000L, 2000L), false));

        QuestEvaluation currentInventory = new QuestEngine().evaluate(player, quest, previous,
            Collections.singletonMap("fluids", new TaskProgress("fluids", Arrays.asList(0L, 700L),
                Arrays.asList(1000L, 2000L), false)), Collections.<UUID>emptySet(), 10L);
        assertEquals("satisfied requirements stay latched while incomplete fluid totals follow inventory",
            Arrays.asList(1000L, 700L), currentInventory.getProgress().getTasks().get("fluids").getValues());

        Map<String, String> groupParameters = new LinkedHashMap<String, String>(fluid.getParameters());
        groupParameters.put("groupDetect", "true");
        TaskDefinition groupFluid = new TaskDefinition("fluids", "bq_standard:fluid", false, groupParameters);
        QuestDefinition groupQuest = new QuestDefinition(quest.getId(), quest.getVersion(), quest.getName(), "",
            QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(groupFluid),
            Collections.<RewardDefinition>emptyList());
        QuestProgressSnapshot groupPrevious = progress(groupQuest, player, QuestStatus.IN_PROGRESS,
            new TaskProgress("fluids", Arrays.asList(1000L, 1500L), Arrays.asList(1000L, 2000L), false));
        QuestEvaluation groupInventory = new QuestEngine().evaluate(player, groupQuest, groupPrevious,
            Collections.singletonMap("fluids", new TaskProgress("fluids", Arrays.asList(0L, 700L),
                Arrays.asList(1000L, 2000L), false)), Collections.<UUID>emptySet(), 11L);
        assertEquals("groupDetect resets every fluid requirement before evaluating the current aggregate",
            Arrays.asList(0L, 700L), groupInventory.getProgress().getTasks().get("fluids").getValues());
    }

    @Test
    public void consumingRetrievalProgressRemainsMonotonicAfterCommittedConsumption() {
        UUID player = UUID.randomUUID();
        TaskDefinition retrieval = new TaskDefinition("items", "bq_standard:retrieval", false,
            map("progressMode", "item-vector", "item.count", "1", "item.0.amount", "10",
                "consume", "true", "groupDetect", "false"));
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Retrieval", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(retrieval),
            Collections.<RewardDefinition>emptyList());
        QuestProgressSnapshot previous = progress(quest, player, QuestStatus.IN_PROGRESS,
            new TaskProgress("items", Arrays.asList(4L), Arrays.asList(10L), false));

        QuestEvaluation replay = new QuestEngine().evaluate(player, quest, previous,
            Collections.singletonMap("items", new TaskProgress("items", Arrays.asList(1L), Arrays.asList(10L), false)),
            Collections.<UUID>emptySet(), 10L);
        assertEquals(Arrays.asList(4L), replay.getProgress().getTasks().get("items").getValues());
    }

    @Test
    public void groupDetectRetrievalReplacesIncompleteProgressFromAggregateSnapshot() {
        UUID player = UUID.randomUUID();
        TaskDefinition retrieval = new TaskDefinition("items", "bq_standard:retrieval", false,
            map("progressMode", "item-vector", "item.count", "2", "item.0.amount", "3",
                "item.1.amount", "10",
                "consume", "false", "groupDetect", "true"));
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Group retrieval", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(retrieval),
            Collections.<RewardDefinition>emptyList());
        QuestProgressSnapshot previous = progress(quest, player, QuestStatus.IN_PROGRESS,
            new TaskProgress("items", Arrays.asList(3L, 8L), Arrays.asList(3L, 10L), false));

        QuestEvaluation lower = new QuestEngine().evaluate(player, quest, previous,
            Collections.singletonMap("items", new TaskProgress("items", Arrays.asList(0L, 3L),
                Arrays.asList(3L, 10L), false)),
            Collections.<UUID>emptySet(), 10L);
        assertEquals(Arrays.asList(0L, 3L), lower.getProgress().getTasks().get("items").getValues());

        QuestProgressSnapshot completed = progress(quest, player, QuestStatus.COMPLETED,
            new TaskProgress("items", Arrays.asList(3L, 10L), Arrays.asList(3L, 10L), true));
        QuestEvaluation latched = new QuestEngine().evaluate(player, quest, completed,
            Collections.singletonMap("items", new TaskProgress("items", Arrays.asList(0L, 0L),
                Arrays.asList(3L, 10L), false)),
            Collections.<UUID>emptySet(), 11L);
        assertTrue(latched.getProgress().getTasks().get("items").isComplete());
    }

    @Test
    public void groupCompletionFreezesOneIndependentEntitlementPerCurrentMember() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID party = UUID.randomUUID();
        QuestDefinition quest = quest(QuestLogic.OR);
        ParticipantMembership membership = new ParticipantMembership(ParticipantId.party(party),
            new java.util.LinkedHashSet<UUID>(Arrays.asList(first, second)));
        QuestEvaluation result = new QuestEngine().evaluate(membership, quest, null,
            Collections.singletonMap("collect", new TaskProgress("collect", 8, 8, true)),
            Collections.<UUID>emptySet(), 1234L);

        assertEquals(ParticipantId.party(party), result.getProgress().getParticipantId());
        assertEquals(2, result.getEntitlements().size());
        assertEquals(first, result.getEntitlements().get(0).getRecipientPlayerId());
        assertEquals(second, result.getEntitlements().get(1).getRecipientPlayerId());
        assertNotEquals(result.getEntitlements().get(0).getEntitlementKey(),
            result.getEntitlements().get(1).getEntitlementKey());
        assertTrue(result.getEntitlements().get(0).getEntitlementKey().startsWith("party:" + party + ":"));
    }

    @Test
    public void repeatableQuestStartsNewCycleAndGetsDistinctEntitlements() {
        UUID player = UUID.randomUUID();
        QuestDefinition base = quest(QuestLogic.OR);
        QuestDefinition repeatable = new QuestDefinition(base.getId(), 1, base.getName(), "", QuestLogic.AND,
            QuestLogic.OR, Collections.<UUID>emptySet(), base.getTasks(), base.getRewards(), RepeatPolicy.after(100L));
        Map<String, TaskProgress> observed = Collections.singletonMap("collect",
            new TaskProgress("collect", 8, 8, true));
        QuestEvaluation first = new QuestEngine().evaluate(player, repeatable, null, observed,
            Collections.<UUID>emptySet(), 1000L);
        QuestEvaluation tooEarly = new QuestEngine().evaluate(player, repeatable, first.getProgress(),
            Collections.<String, TaskProgress>emptyMap(), Collections.<UUID>emptySet(), 1050L);
        assertEquals(QuestStatus.COMPLETED, tooEarly.getProgress().getStatus());
        assertEquals(0, tooEarly.getProgress().getCycle());
        QuestEvaluation second = new QuestEngine().evaluate(player, repeatable, first.getProgress(), observed,
            Collections.<UUID>emptySet(), 1100L);
        assertEquals(1, second.getProgress().getCycle());
        assertTrue(second.getEntitlements().get(0).getEntitlementKey().contains(":1:coins"));
    }

    @Test
    public void repeatableCraftingTaskResetsVectorAndReappliesAbsoluteCraftStatistic() {
        UUID player = UUID.randomUUID();
        TaskDefinition crafting = new TaskDefinition("craft", "bq_standard:crafting", false,
            map("progressMode", "item-vector", "item.count", "1", "item.0.registryName", "minecraft:iron_ingot",
                "item.0.meta", "0", "item.0.amount", "3", "allowCraftedFromStatistics", "true"));
        QuestDefinition repeatable = new QuestDefinition(UUID.randomUUID(), 1, "Craft again", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(crafting),
            Collections.singletonList(new RewardDefinition("reward", "bq_standard:item",
                Collections.<String, String>emptyMap())), RepeatPolicy.after(100L));
        Map<String, TaskProgress> absoluteStatistics = Collections.singletonMap("craft",
            new TaskProgress("craft", Arrays.asList(3L), Arrays.asList(3L), false));

        QuestEvaluation first = new QuestEngine().evaluate(player, repeatable, null, absoluteStatistics,
            Collections.<UUID>emptySet(), 1000L);
        QuestEvaluation nextCycle = new QuestEngine().evaluate(player, repeatable, first.getProgress(),
            absoluteStatistics, Collections.<UUID>emptySet(), 1100L);

        assertEquals(0, first.getProgress().getCycle());
        assertEquals(1, nextCycle.getProgress().getCycle());
        assertTrue(nextCycle.getProgress().getTasks().get("craft").isComplete());
        assertEquals(1, nextCycle.getEntitlements().size());
        assertTrue(nextCycle.getEntitlements().get(0).getEntitlementKey().contains(":1:reward"));
    }

    @Test
    public void repeatCycleFreezesRecipientsAgainFromCurrentMembership() {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), late = UUID.randomUUID();
        ParticipantId team = new ParticipantId(ParticipantType.TEAM, UUID.randomUUID());
        QuestDefinition base = quest(QuestLogic.OR);
        QuestDefinition repeatable = new QuestDefinition(base.getId(), 1, base.getName(), "", QuestLogic.AND,
            QuestLogic.OR, Collections.<UUID>emptySet(), base.getTasks(), base.getRewards(), RepeatPolicy.after(10L),
            QuestBehavior.builder().participantScope(QuestParticipantScope.TEAM).build());
        Map<String, TaskProgress> observed = Collections.singletonMap("collect",
            new TaskProgress("collect", 8, 8, true));
        QuestEvaluation firstCycle = new QuestEngine().evaluate(new ParticipantMembership(team,
            new java.util.LinkedHashSet<UUID>(Arrays.asList(first, second))), repeatable, null, observed,
            Collections.<UUID>emptySet(), 100L);
        QuestEvaluation secondCycle = new QuestEngine().evaluate(new ParticipantMembership(team,
            new java.util.LinkedHashSet<UUID>(Arrays.asList(second, late))), repeatable, firstCycle.getProgress(),
            observed, Collections.<UUID>emptySet(), 110L);
        assertEquals(1, secondCycle.getProgress().getCycle());
        assertEquals(2, secondCycle.getEntitlements().size());
        assertEquals(second, secondCycle.getEntitlements().get(0).getRecipientPlayerId());
        assertEquals(late, secondCycle.getEntitlements().get(1).getRecipientPlayerId());
    }

    @Test(expected=IllegalArgumentException.class)
    public void groupEntitlementCannotImplicitlyTreatTheGroupUuidAsAPlayerRecipient() {
        QuestDefinition definition=quest(QuestLogic.OR);
        new RewardEntitlement(ParticipantId.publicGroup(UUID.randomUUID()),definition,
            definition.getRewards().get(0),0);
    }

    @Test
    public void orLogicAndPrerequisiteLockArePreserved() {
        UUID prerequisite = UUID.randomUUID();
        QuestDefinition base = quest(QuestLogic.OR);
        QuestDefinition gated = new QuestDefinition(base.getId(), 1, "Quest", "", QuestLogic.AND, QuestLogic.OR,
            Collections.singleton(prerequisite), base.getTasks(), base.getRewards());
        Map<String, TaskProgress> observed = Collections.singletonMap("collect",
            new TaskProgress("collect", 8, 8, true));
        QuestEvaluation locked = new QuestEngine().evaluate(UUID.randomUUID(), gated, null, observed,
            Collections.<UUID>emptySet(), 1L);
        assertEquals(QuestStatus.LOCKED, locked.getProgress().getStatus());
        assertEquals(0L, locked.getProgress().getTasks().get("collect").getValue());
        QuestEvaluation complete = new QuestEngine().evaluate(UUID.randomUUID(), gated, null, observed,
            Collections.singleton(prerequisite), 2L);
        assertEquals(QuestStatus.COMPLETED, complete.getProgress().getStatus());
    }

    @Test
    public void exclusivePrerequisitesUnlockFromOneCompletedAlternative() {
        UUID seed = UUID.randomUUID();
        UUID alternative = UUID.randomUUID();
        QuestDefinition base = quest(QuestLogic.AND);
        QuestDefinition choice = new QuestDefinition(base.getId(), 1, "Choice", "", QuestLogic.XOR,
            QuestLogic.AND, new java.util.LinkedHashSet<UUID>(Arrays.asList(seed, alternative)),
            base.getTasks(), base.getRewards());
        Map<String, TaskProgress> observations = new LinkedHashMap<String, TaskProgress>();
        observations.put("collect", new TaskProgress("collect", 8, 8, true));
        observations.put("visit", new TaskProgress("visit", 1, 1, true));
        QuestEngine engine = new QuestEngine();
        UUID player = UUID.randomUUID();
        assertEquals(QuestStatus.LOCKED, engine.evaluate(player, choice, null, observations,
            Collections.<UUID>emptySet(), 1L).getProgress().getStatus());
        assertEquals(QuestStatus.COMPLETED, engine.evaluate(player, choice, null, observations,
            Collections.singleton(seed), 2L).getProgress().getStatus());
        assertEquals(QuestStatus.LOCKED, engine.evaluate(player, choice, null, observations,
            new java.util.LinkedHashSet<UUID>(Arrays.asList(seed, alternative)), 3L).getProgress().getStatus());
    }

    @Test
    public void lockedProgressAndSimultaneousFollowExplicitBehaviorInsteadOfGlobalDefaults() {
        UUID prerequisite = UUID.randomUUID();
        QuestDefinition base = quest(QuestLogic.AND);
        QuestBehavior behavior = QuestBehavior.builder().progressWhileLocked(true).simultaneous(true).build();
        QuestDefinition gated = new QuestDefinition(base.getId(), 1, "Quest", "", QuestLogic.AND, QuestLogic.AND,
            Collections.singleton(prerequisite), base.getTasks(), base.getRewards(), RepeatPolicy.never(), behavior);
        Map<String, TaskProgress> oneTask = Collections.singletonMap("collect",
            new TaskProgress("collect", 8, 8, true));

        QuestEvaluation partial = new QuestEngine().evaluate(UUID.randomUUID(), gated, null, oneTask,
            Collections.<UUID>emptySet(), 1L);
        assertEquals(QuestStatus.LOCKED, partial.getProgress().getStatus());
        assertEquals(0L, partial.getProgress().getTasks().get("collect").getValue());

        Map<String, TaskProgress> both = new LinkedHashMap<String, TaskProgress>();
        both.put("collect", new TaskProgress("collect", 8, 8, true));
        both.put("visit", new TaskProgress("visit", 1, 1, true));
        QuestEvaluation completedWhileLocked = new QuestEngine().evaluate(UUID.randomUUID(), gated, null, both,
            Collections.<UUID>emptySet(), 2L);
        assertEquals(QuestStatus.COMPLETED, completedWhileLocked.getProgress().getStatus());
    }

    @Test
    public void simultaneousQuestDoesNotResetPreviouslyCompletedTaskWithoutNewCompletion() {
        UUID player = UUID.randomUUID();
        QuestDefinition base = quest(QuestLogic.XOR);
        QuestBehavior behavior = QuestBehavior.builder().simultaneous(true).build();
        QuestDefinition simultaneous = new QuestDefinition(base.getId(), base.getVersion(), base.getName(),
            base.getDescription(), base.getPrerequisiteLogic(), base.getTaskLogic(), base.getPrerequisites(),
            base.getTasks(), base.getRewards(), base.getRepeatPolicy(), behavior);
        Map<String, TaskProgress> previousTasks = new LinkedHashMap<String, TaskProgress>();
        previousTasks.put("collect", new TaskProgress("collect", 8, 8, true));
        previousTasks.put("visit", TaskProgress.empty(base.getTasks().get(1)));
        QuestProgressSnapshot previous = new QuestProgressSnapshot(ParticipantId.player(player),
            simultaneous.getId(), simultaneous.getVersion(), QuestStatus.IN_PROGRESS, previousTasks, 0L, 0);

        QuestEvaluation unrelatedObservation = new QuestEngine().evaluate(player, simultaneous, previous,
            Collections.<String, TaskProgress>emptyMap(), Collections.<UUID>emptySet(), 10L);

        assertTrue(unrelatedObservation.getProgress().getTasks().get("collect").isComplete());
        assertEquals(0L, unrelatedObservation.getProgress().getTasks().get("visit").getValue());
    }

    @Test
    public void simultaneousQuestResetsWhenDetectCompletesTaskButQuestLogicRemainsUnsatisfied() {
        UUID player = UUID.randomUUID();
        QuestDefinition base = quest(QuestLogic.XOR);
        QuestBehavior behavior = QuestBehavior.builder().simultaneous(true).build();
        QuestDefinition simultaneous = new QuestDefinition(base.getId(), base.getVersion(), base.getName(),
            base.getDescription(), base.getPrerequisiteLogic(), base.getTaskLogic(), base.getPrerequisites(),
            base.getTasks(), base.getRewards(), base.getRepeatPolicy(), behavior);
        Map<String, TaskProgress> previousTasks = new LinkedHashMap<String, TaskProgress>();
        previousTasks.put("collect", new TaskProgress("collect", 8, 8, true));
        previousTasks.put("visit", TaskProgress.empty(base.getTasks().get(1)));
        QuestProgressSnapshot previous = new QuestProgressSnapshot(ParticipantId.player(player),
            simultaneous.getId(), simultaneous.getVersion(), QuestStatus.IN_PROGRESS, previousTasks, 0L, 0);
        Map<String, TaskProgress> observations = Collections.singletonMap("visit",
            new TaskProgress("visit", 8, 8, true));

        QuestEvaluation detected = new QuestEngine().evaluate(player, simultaneous, previous, observations,
            Collections.<UUID>emptySet(), 10L);

        assertEquals(QuestStatus.AVAILABLE, detected.getProgress().getStatus());
        assertFalse(detected.getProgress().getTasks().get("collect").isComplete());
        assertEquals(0L, detected.getProgress().getTasks().get("collect").getValue());
        assertFalse(detected.getProgress().getTasks().get("visit").isComplete());
        assertEquals(0L, detected.getProgress().getTasks().get("visit").getValue());
    }

    @Test
    public void fixedRepeatUsesEpochAlignedBoundary() {
        RepeatPolicy fixed = RepeatPolicy.after(100L, false);
        assertFalse(fixed.isAvailable(151L, 199L));
        assertTrue(fixed.isAvailable(151L, 200L));
        assertTrue(RepeatPolicy.after(0L, false).isAvailable(151L, 151L));
    }

    private static QuestDefinition quest(QuestLogic logic) {
        Map<String, String> target = Collections.singletonMap("target", "8");
        return new QuestDefinition(UUID.randomUUID(), 1, "Quest", "", QuestLogic.AND, logic,
            Collections.<UUID>emptySet(), Arrays.asList(
                new TaskDefinition("collect", "item", false, target),
                new TaskDefinition("visit", "location", false, Collections.singletonMap("target", "1"))),
            Collections.singletonList(new RewardDefinition("coins", "bank", Collections.<String, String>emptyMap())));
    }

    private static QuestProgressSnapshot progress(QuestDefinition quest, UUID player, QuestStatus status,
        TaskProgress taskProgress) {
        return new QuestProgressSnapshot(ParticipantId.player(player), quest.getId(), quest.getVersion(), status,
            Collections.singletonMap(taskProgress.getTaskKey(), taskProgress), 0L, 0);
    }

    private static Map<String, String> map(String... values) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int i = 0; i < values.length; i += 2) result.put(values[i], values[i + 1]);
        return result;
    }
}
