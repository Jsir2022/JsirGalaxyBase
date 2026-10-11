package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

public class BqPartyActiveQuestQueryTest {
    @Test
    public void offlineUnlockedMemberDoesNotActivateTheLocalPartyUnion() {
        UUID actor = id(1), unlockedOffline = id(2), prerequisite = id(3);
        Fixture fixture = new Fixture(actor, unlockedOffline,
            definition(id(4), Collections.singleton(prerequisite), RepeatPolicy.never(), QuestBehavior.DEFAULT));
        fixture.completions.put(ParticipantId.player(unlockedOffline), Collections.singleton(prerequisite));
        fixture.online.add(actor);

        assertFalse(fixture.query.hasActiveMember(fixture.definition, fixture.fact(actor, 10L), fixture.party));

        fixture.online.add(unlockedOffline);
        assertTrue(fixture.query.hasActiveMember(fixture.definition, fixture.fact(actor, 10L), fixture.party));
    }

    @Test
    public void plannerDoesNotFanOutCraftingWhenOnlyAnOfflinePartyMemberHasTheQuestUnlocked() {
        UUID actor = id(51), offlineMember = id(52), prerequisite = id(53);
        QuestDefinition crafting = partyCraftingDefinition(id(54), prerequisite);
        Fixture fixture = new Fixture(actor, offlineMember, crafting);
        fixture.completions.put(ParticipantId.player(offlineMember), Collections.singleton(prerequisite));
        fixture.online.add(actor);
        QuestCompletionQuery completions = participant -> {
            Set<UUID> found = fixture.completions.get(participant);
            return found == null ? Collections.<UUID>emptySet() : found;
        };
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(
            () -> Collections.singletonList(new StoredQuestDefinition(crafting,
                QuestDefinitionLifecycle.PUBLISHED, "hash", 1L)),
            completions,
            ignored -> java.util.Arrays.asList(ParticipantMembership.player(actor), fixture.party),
            new BqHuntPartyAssignmentPolicy(), fixture.query);
        GameplayFact fact = new GameplayFact("s2", "party-craft-online-eligibility", actor,
            "galaxy:item_produced", 10L, map("channel", "craft", "registryName", "minecraft:iron_ingot",
                "meta", "0", "amount", "1"));

        assertTrue(planner.requestsFor(fact).isEmpty());

        fixture.online.add(offlineMember);
        assertEquals(2, planner.requestsFor(fact).size());
    }

    @Test
    public void oneOnlineEligiblePartyMemberActivatesCraftingAndFansOutToTheEntireRoster() {
        UUID actor = id(55), eligiblePeer = id(56), offlinePeer = id(57), prerequisite = id(58);
        QuestDefinition crafting = partyCraftingDefinition(id(59), prerequisite);
        Fixture fixture = new Fixture(actor, new HashSet<UUID>(Arrays.asList(eligiblePeer, offlinePeer)), crafting);
        fixture.completions.put(ParticipantId.player(eligiblePeer), Collections.singleton(prerequisite));
        fixture.online.add(actor);
        fixture.online.add(eligiblePeer);
        QuestCompletionQuery completions = participant -> {
            Set<UUID> found = fixture.completions.get(participant);
            return found == null ? Collections.<UUID>emptySet() : found;
        };
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(
            () -> Collections.singletonList(new StoredQuestDefinition(crafting,
                QuestDefinitionLifecycle.PUBLISHED, "hash", 1L)),
            completions,
            ignored -> Arrays.asList(ParticipantMembership.player(actor), fixture.party),
            new BqHuntPartyAssignmentPolicy(), fixture.query);
        GameplayFact fact = new GameplayFact("s2", "party-crafting-fanout", actor,
            "galaxy:item_produced", 10L, map("channel", "craft", "registryName", "minecraft:iron_ingot",
                "meta", "0", "amount", "1"));

        List<QuestEvaluationRequest> requests = planner.requestsFor(fact);
        Set<ParticipantId> owners = new HashSet<ParticipantId>();
        for (QuestEvaluationRequest request : requests) owners.add(request.getMembership().getParticipantId());
        assertEquals(3, requests.size());
        assertEquals("the online unlocked peer activates the Quest, then BQ fan-out keeps all PARTY owners",
            new HashSet<ParticipantId>(Arrays.asList(ParticipantId.player(actor),
                ParticipantId.player(eligiblePeer), ParticipantId.player(offlinePeer))), owners);

        fixture.online.remove(eligiblePeer);
        assertTrue("an offline-only unlocked member cannot activate this server's event",
            planner.requestsFor(fact).isEmpty());
    }

    @Test
    public void craftingStatisticsUsePartyOnlineEligibilityButKeepPersonalProgressOwner() {
        UUID actor = id(61), unlockedPeer = id(62), prerequisite = id(63);
        QuestDefinition crafting = partyCraftingStatisticsDefinition(id(64), prerequisite);
        Fixture fixture = new Fixture(actor, unlockedPeer, crafting);
        fixture.completions.put(ParticipantId.player(unlockedPeer), Collections.singleton(prerequisite));
        fixture.online.add(actor);
        QuestCompletionQuery completions = participant -> {
            Set<UUID> found = fixture.completions.get(participant);
            return found == null ? Collections.<UUID>emptySet() : found;
        };
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(
            () -> Collections.singletonList(new StoredQuestDefinition(crafting,
                QuestDefinitionLifecycle.PUBLISHED, "hash", 1L)),
            completions,
            ignored -> java.util.Arrays.asList(ParticipantMembership.player(actor), fixture.party),
            new BqHuntPartyAssignmentPolicy(), fixture.query);
        Map<String, String> attributes = map("channel", "craft-statistics", "registryName", "minecraft:iron_ingot",
            "meta", "0", "amount", "4", "trigger.channel", "craft",
            "trigger.registryName", "minecraft:iron_ingot", "trigger.meta", "0");
        attributes.put(BqPartyCraftingStatisticsScope.PERSONAL_ONLINE_SNAPSHOT, "true");
        GameplayFact fact = new GameplayFact("s2", "party-crafting-statistics", actor,
            "galaxy:item_produced", 10L, attributes);

        assertTrue("an offline unlocked Party member does not make the Quest active on this server",
            planner.requestsFor(fact).isEmpty());
        fixture.online.add(unlockedPeer);
        List<QuestEvaluationRequest> requests = planner.requestsFor(fact);
        assertEquals("the online peer activates the Party Quest, but this fact remains actor-personal",
            1, requests.size());
        assertEquals(ParticipantId.player(actor), requests.get(0).getMembership().getParticipantId());
        assertEquals(ParticipantId.player(actor), requests.get(0).getFactMembership().getParticipantId());
    }

    @Test
    public void automaticBqCraftingStatisticsUseOnlinePartyEligibilityAndKeepPersonalProgressOwner() {
        UUID actor = id(71), unlockedPeer = id(72), prerequisite = id(73), questId = id(74);
        QuestDefinition crafting = partyCraftingStatisticsDefinition(questId, prerequisite);
        Fixture fixture = new Fixture(actor, unlockedPeer, crafting);
        fixture.completions.put(ParticipantId.player(unlockedPeer), Collections.singleton(prerequisite));
        fixture.online.add(actor);
        QuestCompletionQuery completions = participant -> {
            Set<UUID> found = fixture.completions.get(participant);
            return found == null ? Collections.<UUID>emptySet() : found;
        };
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(
            () -> Collections.singletonList(new StoredQuestDefinition(crafting,
                QuestDefinitionLifecycle.PUBLISHED, "hash", 1L)),
            completions,
            ignored -> java.util.Arrays.asList(ParticipantMembership.player(actor), fixture.party),
            new BqHuntPartyAssignmentPolicy(), fixture.query);
        Map<String, String> attributes = map("channel", "craft-statistics", "registryName", "minecraft:iron_ingot",
            "meta", "0", "amount", "4", "targetQuestId", questId.toString(), "targetQuestVersion", "1",
            "targetTaskKey", "craft");
        attributes.put(BqPartyCraftingStatisticsScope.PERSONAL_ONLINE_SNAPSHOT, "true");
        attributes.put(BqPartyCraftingStatisticsScope.AUTOMATIC_TASK_SNAPSHOT, "true");
        GameplayFact fact = new GameplayFact("s2", "automatic-party-crafting-statistics", actor,
            "galaxy:item_produced", 10L, attributes);

        assertTrue("an offline unlocked Party member cannot activate this server's automatic Detect",
            planner.requestsFor(fact).isEmpty());
        fixture.online.add(unlockedPeer);
        List<QuestEvaluationRequest> requests = planner.requestsFor(fact);
        assertEquals(1, requests.size());
        assertEquals("each online member is detected into their own player progress row",
            ParticipantId.player(actor), requests.get(0).getMembership().getParticipantId());
        assertEquals("the trusted Party identity is used only for active-quest/locked-progress authorization",
            fixture.party.getParticipantId(), requests.get(0).getFactMembership().getParticipantId());
    }

    @Test
    public void lockedProgressMakesAnOnlineMemberActiveWithoutPrerequisites() {
        UUID actor = id(11), other = id(12), prerequisite = id(13);
        Fixture fixture = new Fixture(actor, other, definition(id(14), Collections.singleton(prerequisite),
            RepeatPolicy.never(), QuestBehavior.builder().progressWhileLocked(true).build()));
        fixture.online.add(actor);

        assertTrue(fixture.query.hasActiveMember(fixture.definition, fixture.fact(actor, 10L), fixture.party));
    }

    @Test
    public void sourceServerMustMatchTheAuthoritativeOnlineDirectory() {
        UUID actor = id(21), other = id(22);
        Fixture fixture = new Fixture(actor, other, definition(id(23), Collections.<UUID>emptySet(),
            RepeatPolicy.never(), QuestBehavior.DEFAULT));
        fixture.online.add(actor);

        assertFalse(fixture.query.hasActiveMember(fixture.definition,
            new GameplayFact("lobby", "event", actor, "galaxy:item_produced", 10L,
                Collections.<String, String>emptyMap()), fixture.party));
    }

    @Test
    public void completedNonRepeatableQuestIsNotInTheActiveUnion() {
        UUID actor = id(31), other = id(32), quest = id(33);
        Fixture fixture = new Fixture(actor, other, definition(quest, Collections.<UUID>emptySet(),
            RepeatPolicy.never(), QuestBehavior.DEFAULT));
        fixture.online.add(actor);
        fixture.progress.set(ParticipantId.player(actor), fixture.definition,
            new QuestProgressSnapshot(ParticipantId.player(actor), quest, 1, QuestStatus.COMPLETED,
                Collections.<String, TaskProgress>emptyMap(), 5L, 0));

        assertFalse(fixture.query.hasActiveMember(fixture.definition, fixture.fact(actor, 10L), fixture.party));
    }

    @Test
    public void repeatQuestWaitsForRewardAndCooldownThenBecomesActive() {
        UUID actor = id(41), other = id(42), quest = id(43);
        Fixture fixture = new Fixture(actor, other, definition(quest, Collections.<UUID>emptySet(),
            RepeatPolicy.after(10L), QuestBehavior.DEFAULT));
        fixture.online.add(actor);
        ParticipantId owner = ParticipantId.player(actor);
        fixture.progress.set(owner, fixture.definition, new QuestProgressSnapshot(owner, quest, 1,
            QuestStatus.COMPLETED, Collections.<String, TaskProgress>emptyMap(), 100L, 0));
        fixture.progress.anchor = 120L;

        assertFalse(fixture.query.hasActiveMember(fixture.definition, fixture.fact(actor, 129L), fixture.party));
        fixture.progress.claimable = true;
        assertFalse(fixture.query.hasActiveMember(fixture.definition, fixture.fact(actor, 130L), fixture.party));
        fixture.progress.claimable = false;
        fixture.progress.pendingConsumption = true;
        assertFalse(fixture.query.hasActiveMember(fixture.definition, fixture.fact(actor, 130L), fixture.party));
        fixture.progress.pendingConsumption = false;
        assertTrue(fixture.query.hasActiveMember(fixture.definition, fixture.fact(actor, 130L), fixture.party));
    }

    private static QuestDefinition definition(UUID id, Set<UUID> prerequisites, RepeatPolicy repeat,
        QuestBehavior behavior) {
        return new QuestDefinition(id, 1, "Quest", "", QuestLogic.AND, QuestLogic.AND, prerequisites,
            Collections.<TaskDefinition>emptyList(), Collections.<RewardDefinition>emptyList(), repeat, behavior);
    }

    private static QuestDefinition partyCraftingDefinition(UUID id, UUID prerequisite) {
        Map<String, String> parameters = map("factType", "galaxy:item_produced", "progressMode", "item-vector",
            "item.count", "1", "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0",
            "item.0.amount", "1", "allowCraft", "true", "allowSmelt", "true", "allowAnvil", "false",
            "bq.raw", "{}");
        TaskDefinition task = new TaskDefinition("craft", "bq_standard:crafting", false, parameters);
        return new QuestDefinition(id, 1, "Party crafting", "", QuestLogic.AND, QuestLogic.AND,
            Collections.singleton(prerequisite), Collections.singletonList(task),
            Collections.<RewardDefinition>emptyList(), RepeatPolicy.never(),
            QuestBehavior.builder().participantScope(QuestParticipantScope.PLAYER).build());
    }

    private static QuestDefinition partyCraftingStatisticsDefinition(UUID id, UUID prerequisite) {
        Map<String, String> parameters = map("factType", "galaxy:item_produced", "progressMode", "item-vector",
            "item.count", "1", "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0",
            "item.0.amount", "1", "allowCraft", "true", "allowSmelt", "true", "allowAnvil", "false",
            "allowCraftedFromStatistics", "true", "bq.raw", "{}");
        TaskDefinition task = new TaskDefinition("craft", "bq_standard:crafting", false, parameters);
        return new QuestDefinition(id, 1, "Party crafting statistics", "", QuestLogic.AND, QuestLogic.AND,
            Collections.singleton(prerequisite), Collections.singletonList(task),
            Collections.<RewardDefinition>emptyList(), RepeatPolicy.never(),
            QuestBehavior.builder().participantScope(QuestParticipantScope.PLAYER).build());
    }

    private static UUID id(long value) { return new UUID(0L, value); }

    private static Map<String, String> map(String... pairs) {
        Map<String, String> values = new HashMap<String, String>();
        for (int i = 0; i < pairs.length; i += 2) values.put(pairs[i], pairs[i + 1]);
        return values;
    }

    private static final class Fixture {
        private final UUID actor;
        private final QuestDefinition definition;
        private final ParticipantMembership party;
        private final Set<UUID> online = new HashSet<UUID>();
        private final Map<ParticipantId, Set<UUID>> completions = new HashMap<ParticipantId, Set<UUID>>();
        private final Runtime progress = new Runtime();
        private final BqPartyActiveQuestQuery query;

        private Fixture(UUID actor, UUID other, QuestDefinition definition) {
            this(actor, Collections.singleton(other), definition);
        }

        private Fixture(UUID actor, Set<UUID> otherMembers, QuestDefinition definition) {
            this.actor = actor;
            this.definition = definition;
            Set<UUID> members = new HashSet<UUID>();
            members.add(actor);
            members.addAll(otherMembers);
            party = new ParticipantMembership(new ParticipantId(ParticipantType.PARTY, id(999L)), members);
            query = new BqPartyActiveQuestQuery("s2", participant -> {
                Set<UUID> found = completions.get(participant);
                return found == null ? Collections.<UUID>emptySet() : found;
            }, progress, online::contains);
        }

        private GameplayFact fact(UUID playerId, long now) {
            return new GameplayFact("s2", "event-" + now, playerId, "galaxy:item_produced", now,
                Collections.<String, String>emptyMap());
        }

        private static Map<String, String> map(String... pairs) {
            Map<String, String> values = new HashMap<String, String>();
            for (int i = 0; i < pairs.length; i += 2) values.put(pairs[i], pairs[i + 1]);
            return values;
        }
    }

    private static final class Runtime implements QuestRuntimeRepository {
        private final Map<String, QuestProgressSnapshot> snapshots = new HashMap<String, QuestProgressSnapshot>();
        private boolean pendingConsumption;
        private boolean claimable;
        private long anchor;

        private void set(ParticipantId participant, QuestDefinition definition, QuestProgressSnapshot snapshot) {
            snapshots.put(key(participant, definition.getId(), definition.getVersion()), snapshot);
        }

        @Override public Optional<QuestProgressSnapshot> findProgress(ParticipantId participantId, UUID questId,
            int version) {
            return Optional.ofNullable(snapshots.get(key(participantId, questId, version)));
        }
        @Override public boolean wasEverCompleted(ParticipantId participantId, UUID questId, int version) {
            return findProgress(participantId, questId, version).isPresent();
        }
        @Override public boolean hasPendingConsumptionSubmission(ParticipantId participantId, UUID questId,
            int version, int cycle) { return pendingConsumption; }
        @Override public boolean hasClaimableRewardEntitlement(ParticipantId participantId, UUID questId,
            int version, int cycle) { return claimable; }
        @Override public long findRepeatCycleAnchorMillis(ParticipantId participantId, UUID questId, int version,
            int cycle, long fallback) { return anchor == 0L ? fallback : anchor; }
        @Override public boolean recordFactIfAbsent(GameplayFact fact) { throw new UnsupportedOperationException(); }
        @Override public void saveProgress(QuestProgressSnapshot progress) { throw new UnsupportedOperationException(); }
        @Override public void insertEntitlementsIfAbsent(Set<RewardEntitlement> entitlements) {
            throw new UnsupportedOperationException();
        }

        private static String key(ParticipantId participant, UUID questId, int version) {
            return participant.toString() + ':' + questId + ':' + version;
        }
    }
}
