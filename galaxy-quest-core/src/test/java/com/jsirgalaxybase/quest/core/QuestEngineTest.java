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
}
