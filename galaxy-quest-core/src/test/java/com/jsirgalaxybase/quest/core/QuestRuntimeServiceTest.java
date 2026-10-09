package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

public class QuestRuntimeServiceTest {
    private static final QuestPartyActivityQuery ACTIVE_PARTY_FOR_RUNTIME_TEST =
        (definition, fact, party) -> true;

    @Test
    public void appliesFactProgressAndEntitlementOnceInsideTransaction() {
        UUID player = UUID.randomUUID();
        Map<String, String> parameters = map("target", "1", "factType", "minecraft:kill", "subject", "Zombie");
        TaskDefinition task = new TaskDefinition("hunt", "count", false, parameters);
        RewardDefinition reward = new RewardDefinition("reward", "item", Collections.<String, String>emptyMap());
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Hunt", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task),
            Collections.singletonList(reward), RepeatPolicy.never());
        TaskEvaluatorRegistry registry = new TaskEvaluatorRegistry();
        registry.register(new FactCountTaskEvaluator("count"));
        FakeRepository repository = new FakeRepository();
        CountingTransaction transaction = new CountingTransaction();
        QuestRuntimeService service = new QuestRuntimeService(repository, transaction,
            new QuestFactProjector(registry), new QuestEngine());
        GameplayFact fact = new GameplayFact("s2", "kill-1", player, "minecraft:kill", 10L,
            map("subject", "Zombie", "amount", "1"));
        List<QuestEvaluationRequest> requests = Collections.singletonList(new QuestEvaluationRequest(quest,
            ParticipantMembership.player(player), Collections.<UUID>emptySet()));

        FactProcessingResult first = service.apply(fact, requests, 10L);
        FactProcessingResult duplicate = service.apply(fact, requests, 11L);

        assertTrue(first.isAccepted());
        assertEquals(QuestStatus.COMPLETED, first.getEvaluations().get(0).getProgress().getStatus());
        assertEquals(1, repository.entitlements.size());
        assertFalse(duplicate.isAccepted());
        assertTrue(duplicate.getEvaluations().isEmpty());
        assertEquals(2, transaction.calls);
    }

    @Test
    public void completingPrerequisiteCompletesStoredLockedProgressAndCreatesItsReward() {
        UUID player = UUID.randomUUID();
        QuestDefinition prerequisite = new QuestDefinition(UUID.randomUUID(), 1, "Prerequisite", "",
            QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(),
            Collections.singletonList(new TaskDefinition("confirm", "count", false,
                map("target", "1", "factType", "minecraft:kill", "subject", "Zombie"))),
            Collections.<RewardDefinition>emptyList());
        TaskDefinition childTask = new TaskDefinition("craft", "bq_standard:crafting", false,
            Collections.<String, String>emptyMap());
        RewardDefinition childReward = new RewardDefinition("reward", "bq_standard:item",
            Collections.<String, String>emptyMap());
        QuestDefinition dependent = new QuestDefinition(UUID.randomUUID(), 1, "Locked progress", "",
            QuestLogic.AND, QuestLogic.AND, Collections.singleton(prerequisite.getId()),
            Collections.singletonList(childTask), Collections.singletonList(childReward), RepeatPolicy.never(),
            QuestBehavior.builder().progressWhileLocked(true).autoClaim(true).build());
        ParticipantId owner = ParticipantId.player(player);
        FakeRepository repository = new FakeRepository();
        repository.seed(new QuestProgressSnapshot(owner, dependent.getId(), 1, QuestStatus.LOCKED,
            Collections.singletonMap("craft", new TaskProgress("craft", Arrays.asList(1L), Arrays.asList(1L), true)),
            0L, 0));
        TaskEvaluatorRegistry registry = new TaskEvaluatorRegistry();
        registry.register(new FactCountTaskEvaluator("count"));
        QuestEvaluationRequest parentRequest = new QuestEvaluationRequest(prerequisite,
            ParticipantMembership.player(player), Collections.<UUID>emptySet());
        QuestEvaluationRequest childRequest = new QuestEvaluationRequest(dependent,
            ParticipantMembership.player(player), Collections.singleton(prerequisite.getId()));
        QuestEvaluationRequestProvider provider = new QuestEvaluationRequestProvider() {
            @Override public List<QuestEvaluationRequest> requestsFor(GameplayFact ignored) {
                return Collections.singletonList(parentRequest);
            }
            @Override public List<QuestEvaluationRequest> requestsForNewlyUnlockedLockedProgress(
                ParticipantId participant, UUID completedQuestId) {
                return owner.equals(participant) && prerequisite.getId().equals(completedQuestId)
                    ? Collections.singletonList(childRequest) : Collections.<QuestEvaluationRequest>emptyList();
            }
        };
        QuestRuntimeService service = new QuestRuntimeService(repository, new CountingTransaction(),
            new QuestFactProjector(registry), new QuestEngine(), new RewardSuppressionPolicy(true));

        FactProcessingResult result = service.apply(new GameplayFact("s2", "complete-prerequisite", player,
            "minecraft:kill", 10L, map("subject", "Zombie", "amount", "1")), provider, 10L);

        assertTrue(result.isAccepted());
        assertEquals(2, result.getEvaluations().size());
        assertEquals(QuestStatus.COMPLETED, repository.findProgress(owner, prerequisite.getId(), 1).get().getStatus());
        QuestProgressSnapshot completedDependent = repository.findProgress(owner, dependent.getId(), 1).get();
        assertEquals(QuestStatus.COMPLETED, completedDependent.getStatus());
        assertTrue(completedDependent.getTasks().get("craft").isComplete());
        assertEquals(1, repository.entitlements.size());
        assertEquals(dependent.getId(), repository.entitlements.get(0).getQuestId());
        assertEquals(RewardDeliveryStatus.SUPPRESSED,
            repository.entitlements.get(0).getInitialDeliveryStatus());
    }

    @Test
    public void periodicReconciliationCompletesOldLockedProgressAfterPrerequisiteWasAlreadyCompleted() {
        UUID player = UUID.randomUUID();
        UUID prerequisite = UUID.randomUUID();
        TaskDefinition task = new TaskDefinition("craft", "bq_standard:crafting", false,
            Collections.<String, String>emptyMap());
        QuestDefinition dependent = new QuestDefinition(UUID.randomUUID(), 1, "Legacy locked progress", "",
            QuestLogic.AND, QuestLogic.AND, Collections.singleton(prerequisite), Collections.singletonList(task),
            Collections.singletonList(new RewardDefinition("reward", "bq_standard:item",
                Collections.<String, String>emptyMap())), RepeatPolicy.never(), QuestBehavior.builder()
                    .progressWhileLocked(true).autoClaim(true).build());
        ParticipantId owner = ParticipantId.player(player);
        FakeRepository repository = new FakeRepository();
        repository.seed(new QuestProgressSnapshot(owner, dependent.getId(), 1, QuestStatus.LOCKED,
            Collections.singletonMap("craft", new TaskProgress("craft", Arrays.asList(1L), Arrays.asList(1L), true)),
            0L, 0));
        QuestEvaluationRequest request = new QuestEvaluationRequest(dependent, ParticipantMembership.player(player),
            Collections.singleton(prerequisite));
        QuestEvaluationRequestProvider provider = new QuestEvaluationRequestProvider() {
            @Override public List<QuestEvaluationRequest> requestsFor(GameplayFact ignored) {
                return Collections.<QuestEvaluationRequest>emptyList();
            }
            @Override public List<QuestEvaluationRequest> requestsForLockedProgressReconciliation(
                ParticipantId participant, Set<UUID> lockedQuestIds) {
                return owner.equals(participant) && lockedQuestIds.contains(dependent.getId())
                    ? Collections.singletonList(request) : Collections.<QuestEvaluationRequest>emptyList();
            }
        };
        QuestRuntimeService service = new QuestRuntimeService(repository, new CountingTransaction(),
            new QuestFactProjector(new TaskEvaluatorRegistry()), new QuestEngine());

        FactProcessingResult reconciled = service.reconcileLockedProgress(owner, provider, 20L);
        FactProcessingResult repeated = service.reconcileLockedProgress(owner, provider, 21L);

        assertTrue(reconciled.isAccepted());
        assertEquals(QuestStatus.COMPLETED, reconciled.getEvaluations().get(0).getProgress().getStatus());
        assertFalse(repeated.isAccepted());
        assertEquals(1, repository.entitlements.size());
        assertEquals(RewardDeliveryStatus.PENDING,
            repository.entitlements.get(0).getInitialDeliveryStatus());
    }

    @Test
    public void repeatableBqHuntStartsNextCycleFromFreshKillProgress() {
        UUID player = UUID.randomUUID();
        TaskDefinition hunt = new TaskDefinition("hunt", "bq_standard:hunt", false,
            map("target", "2", "factType", "galaxy:entity_killed", "subject", "Zombie", "subtypes", "true"));
        RewardDefinition reward = new RewardDefinition("reward", "bq_standard:item", Collections.<String, String>emptyMap());
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Hunt", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(hunt),
            Collections.singletonList(reward), RepeatPolicy.after(100L));
        FakeRepository repository = new FakeRepository();
        QuestRuntimeService service = new QuestRuntimeService(repository, new CountingTransaction(),
            new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
        List<QuestEvaluationRequest> requests = Collections.singletonList(new QuestEvaluationRequest(quest,
            ParticipantMembership.player(player), Collections.<UUID>emptySet()));

        FactProcessingResult firstKill = service.apply(new GameplayFact("s2", "hunt-1", player,
            "galaxy:entity_killed", 1000L, map("subject", "Zombie", "amount", "1")), requests, 1000L);
        assertEquals(QuestStatus.IN_PROGRESS, firstKill.getEvaluations().get(0).getProgress().getStatus());
        assertEquals(1L, firstKill.getEvaluations().get(0).getProgress().getTasks().get("hunt").getValue());

        FactProcessingResult secondKill = service.apply(new GameplayFact("s2", "hunt-2", player,
            "galaxy:entity_killed", 1010L, map("subject", "Zombie", "amount", "1")), requests, 1010L);
        QuestProgressSnapshot completedCycle = secondKill.getEvaluations().get(0).getProgress();
        assertEquals(QuestStatus.COMPLETED, completedCycle.getStatus());
        assertEquals(0, completedCycle.getCycle());
        assertEquals(1, repository.entitlements.size());

        FactProcessingResult beforeReset = service.apply(new GameplayFact("s2", "hunt-3", player,
            "galaxy:entity_killed", 1109L, map("subject", "Zombie", "amount", "1")), requests, 1109L);
        assertEquals(0, beforeReset.getEvaluations().get(0).getProgress().getCycle());
        assertEquals(1, repository.entitlements.size());

        FactProcessingResult heldCycle = service.apply(new GameplayFact("s2", "hunt-4", player,
            "galaxy:entity_killed", 1110L, map("subject", "Zombie", "amount", "1")), requests, 1110L);
        assertEquals(0, heldCycle.getEvaluations().get(0).getProgress().getCycle());
        assertEquals(1, repository.entitlements.size());

        repository.markClaimed(ParticipantId.player(player), quest.getId(), 1, 0, 2_000L);
        FactProcessingResult beforeClaimCooldown = service.apply(new GameplayFact("s2", "hunt-5", player,
            "galaxy:entity_killed", 2_099L, map("subject", "Zombie", "amount", "1")), requests, 2_099L);
        assertEquals(0, beforeClaimCooldown.getEvaluations().get(0).getProgress().getCycle());
        FactProcessingResult firstKillOfNextCycle = service.apply(new GameplayFact("s2", "hunt-6", player,
            "galaxy:entity_killed", 2_100L, map("subject", "Zombie", "amount", "1")), requests, 2_100L);
        QuestProgressSnapshot nextCycle = firstKillOfNextCycle.getEvaluations().get(0).getProgress();
        assertEquals(1, nextCycle.getCycle());
        assertEquals(QuestStatus.IN_PROGRESS, nextCycle.getStatus());
        assertEquals(1L, nextCycle.getTasks().get("hunt").getValue());
        assertEquals(1, repository.entitlements.size());

        FactProcessingResult secondKillOfNextCycle = service.apply(new GameplayFact("s2", "hunt-7", player,
            "galaxy:entity_killed", 2_110L, map("subject", "Zombie", "amount", "1")), requests, 2_110L);
        assertEquals(QuestStatus.COMPLETED, secondKillOfNextCycle.getEvaluations().get(0).getProgress().getStatus());
        assertEquals(1, secondKillOfNextCycle.getEvaluations().get(0).getProgress().getCycle());
        assertEquals(2, repository.entitlements.size());
    }

    @Test
    public void bqHuntPartyFactCompletesEachMembersPersonalProgressAndRewardOnce() {
        UUID hunter = UUID.randomUUID();
        UUID partyMember = UUID.randomUUID();
        ParticipantId partyId = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership party = new ParticipantMembership(partyId,
            new java.util.LinkedHashSet<UUID>(java.util.Arrays.asList(hunter, partyMember)));
        TaskDefinition hunt = new TaskDefinition("hunt", "bq_standard:hunt", false,
            map("target", "1", "factType", "galaxy:entity_killed", "subject", "Zombie", "subtypes", "true"));
        RewardDefinition reward = new RewardDefinition("reward", "bq_standard:item",
            Collections.<String, String>emptyMap());
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Party hunt", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(hunt),
            Collections.singletonList(reward), RepeatPolicy.never());
        ParticipantMembershipResolver memberships = player -> java.util.Arrays.asList(
            ParticipantMembership.player(player), party);
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(() -> Collections.singletonList(
            new StoredQuestDefinition(quest, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L)),
            participant -> Collections.<UUID>emptySet(), memberships, new BqHuntPartyAssignmentPolicy(),
            ACTIVE_PARTY_FOR_RUNTIME_TEST);
        FakeRepository repository = new FakeRepository();
        QuestRuntimeService service = new QuestRuntimeService(repository, new CountingTransaction(),
            new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
        GameplayFact kill = new GameplayFact("s2", "party-hunt-1", hunter, "galaxy:entity_killed", 10L,
            map("subject", "Zombie", "amount", "1"));

        FactProcessingResult first = service.apply(kill, planner, 10L);

        assertTrue(first.isAccepted());
        assertEquals(2, first.getEvaluations().size());
        assertEquals(QuestStatus.COMPLETED, repository.findProgress(ParticipantId.player(hunter), quest.getId(), 1)
            .get().getStatus());
        assertEquals(QuestStatus.COMPLETED, repository.findProgress(ParticipantId.player(partyMember), quest.getId(), 1)
            .get().getStatus());
        assertEquals(2, repository.entitlements.size());
        assertTrue(repository.entitlements.stream().anyMatch(value -> value.getPlayerId().equals(hunter)));
        assertTrue(repository.entitlements.stream().anyMatch(value -> value.getPlayerId().equals(partyMember)));

        FactProcessingResult duplicate = service.apply(kill, planner, 11L);
        assertEquals(FactProcessingStatus.DUPLICATE, duplicate.getStatus());
        assertEquals(2, repository.entitlements.size());
    }

    @Test
    public void bqCraftingEventFansOutProgressAndDefersLockedPartyMemberRewardUntilUnlock() {
        UUID crafter = UUID.randomUUID();
        UUID lockedMember = UUID.randomUUID();
        UUID prerequisite = UUID.randomUUID();
        boolean[] memberUnlocked = { false };
        ParticipantId partyId = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership party = new ParticipantMembership(partyId,
            new java.util.LinkedHashSet<UUID>(java.util.Arrays.asList(crafter, lockedMember)));
        TaskDefinition crafting = new TaskDefinition("craft", "bq_standard:crafting", false,
            map("factType", "galaxy:item_produced", "progressMode", "item-vector", "item.count", "1",
                "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0", "item.0.amount", "1",
                "allowCraft", "true", "allowSmelt", "true", "allowAnvil", "false", "bq.raw", "{}"));
        RewardDefinition reward = new RewardDefinition("reward", "bq_standard:item",
            Collections.<String, String>emptyMap());
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Party crafting", "", QuestLogic.AND,
            QuestLogic.AND, Collections.singleton(prerequisite), Collections.singletonList(crafting),
            Collections.singletonList(reward), RepeatPolicy.never(),
            QuestBehavior.builder().participantScope(QuestParticipantScope.PLAYER).build());
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(() -> Collections.singletonList(
            new StoredQuestDefinition(quest, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L)),
            participant -> participant.getId().equals(crafter) || memberUnlocked[0]
                ? Collections.singleton(prerequisite) : Collections.<UUID>emptySet(),
            player -> java.util.Arrays.asList(ParticipantMembership.player(player), party),
            new BqHuntPartyAssignmentPolicy(), ACTIVE_PARTY_FOR_RUNTIME_TEST);
        FakeRepository repository = new FakeRepository();
        QuestRuntimeService service = new QuestRuntimeService(repository, new CountingTransaction(),
            new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
        GameplayFact firstCraft = new GameplayFact("s2", "party-craft-1", crafter, "galaxy:item_produced", 10L,
            map("registryName", "minecraft:iron_ingot", "meta", "0", "damageable", "false", "amount", "1",
                "channel", "craft", "oreDictionary", ""));

        FactProcessingResult first = service.apply(firstCraft, planner, 10L);

        assertEquals(2, first.getEvaluations().size());
        assertEquals(QuestStatus.COMPLETED, repository.findProgress(ParticipantId.player(crafter), quest.getId(), 1)
            .get().getStatus());
        QuestProgressSnapshot deferred = repository.findProgress(ParticipantId.player(lockedMember), quest.getId(), 1)
            .get();
        assertEquals(QuestStatus.LOCKED, deferred.getStatus());
        assertTrue(deferred.getTasks().get("craft").isComplete());
        assertEquals(1, repository.entitlements.size());
        assertEquals(crafter, repository.entitlements.get(0).getPlayerId());

        memberUnlocked[0] = true;
        GameplayFact secondCraft = new GameplayFact("s2", "party-craft-2", crafter, "galaxy:item_produced", 20L,
            map("registryName", "minecraft:iron_ingot", "meta", "0", "damageable", "false", "amount", "1",
                "channel", "craft", "oreDictionary", ""));
        FactProcessingResult afterUnlock = service.apply(secondCraft, planner, 20L);

        assertEquals(QuestStatus.COMPLETED, afterUnlock.getEvaluations().get(1).getProgress().getStatus());
        assertEquals(2, repository.entitlements.size());
        assertEquals(lockedMember, repository.entitlements.get(1).getPlayerId());
    }

    @Test
    public void bqLocationObservationCompletesEachPartyMembersPersonalProgressButDefersLockedReward() {
        UUID visitor = UUID.randomUUID();
        UUID lockedMember = UUID.randomUUID();
        UUID prerequisite = UUID.randomUUID();
        boolean[] memberUnlocked = { false };
        ParticipantId partyId = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership party = new ParticipantMembership(partyId,
            new java.util.LinkedHashSet<UUID>(java.util.Arrays.asList(visitor, lockedMember)));
        TaskDefinition location = new TaskDefinition("visit", "bq_standard:location", false,
            map("factType", "galaxy:location_observed", "dimension", "55", "range", "-1", "biome", "-1",
                "posX", "0", "posY", "0", "posZ", "0", "visible", "false", "invert", "false",
                "taxiCabDist", "false", "structure", "", "bq.raw", "{}"));
        RewardDefinition reward = new RewardDefinition("reward", "bq_standard:item",
            Collections.<String, String>emptyMap());
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Party location", "", QuestLogic.AND,
            QuestLogic.AND, Collections.singleton(prerequisite), Collections.singletonList(location),
            Collections.singletonList(reward), RepeatPolicy.never(),
            QuestBehavior.builder().participantScope(QuestParticipantScope.PLAYER).build());
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(() -> Collections.singletonList(
            new StoredQuestDefinition(quest, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L)),
            participant -> participant.getId().equals(visitor) || memberUnlocked[0]
                ? Collections.singleton(prerequisite) : Collections.<UUID>emptySet(),
            player -> java.util.Arrays.asList(ParticipantMembership.player(player), party),
            new BqHuntPartyAssignmentPolicy(), ACTIVE_PARTY_FOR_RUNTIME_TEST);
        FakeRepository repository = new FakeRepository();
        QuestRuntimeService service = new QuestRuntimeService(repository, new CountingTransaction(),
            new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
        GameplayFact firstVisit = new GameplayFact("s2", "party-location-1", visitor, "galaxy:location_observed",
            10L, map("dimension", "55", "x", "0", "y", "64", "z", "0", "biome", "1"));

        FactProcessingResult first = service.apply(firstVisit, planner, 10L);

        assertEquals(2, first.getEvaluations().size());
        assertEquals(QuestStatus.COMPLETED, repository.findProgress(ParticipantId.player(visitor), quest.getId(), 1)
            .get().getStatus());
        QuestProgressSnapshot deferred = repository.findProgress(ParticipantId.player(lockedMember), quest.getId(), 1)
            .get();
        assertEquals(QuestStatus.LOCKED, deferred.getStatus());
        assertTrue(deferred.getTasks().get("visit").isComplete());
        assertEquals(1, repository.entitlements.size());
        assertEquals(visitor, repository.entitlements.get(0).getPlayerId());

        memberUnlocked[0] = true;
        GameplayFact secondVisit = new GameplayFact("s2", "party-location-2", visitor, "galaxy:location_observed",
            20L, map("dimension", "55", "x", "0", "y", "64", "z", "0", "biome", "1"));
        FactProcessingResult afterUnlock = service.apply(secondVisit, planner, 20L);

        assertEquals(QuestStatus.COMPLETED, afterUnlock.getEvaluations().get(1).getProgress().getStatus());
        assertEquals(2, repository.entitlements.size());
        assertEquals(lockedMember, repository.entitlements.get(1).getPlayerId());
    }

    @Test
    public void bqMeetingObservationCompletesEachPartyMembersPersonalProgressAndDefersLockedReward() {
        UUID observer = UUID.randomUUID();
        UUID lockedMember = UUID.randomUUID();
        UUID prerequisite = UUID.randomUUID();
        boolean[] memberUnlocked = { false };
        ParticipantId partyId = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership party = new ParticipantMembership(partyId,
            new java.util.LinkedHashSet<UUID>(java.util.Arrays.asList(observer, lockedMember)));
        TaskDefinition meeting = new TaskDefinition("meeting", "bq_standard:meeting", false,
            map("factType", "galaxy:nearby_entities_observed", "subject", "Villager", "range", "4",
                "amount", "1", "subtypes", "true", "ignoreNBT", "true", "bq.raw", "{}"));
        RewardDefinition reward = new RewardDefinition("reward", "bq_standard:item",
            Collections.<String, String>emptyMap());
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Party meeting", "", QuestLogic.AND,
            QuestLogic.AND, Collections.singleton(prerequisite), Collections.singletonList(meeting),
            Collections.singletonList(reward), RepeatPolicy.never(),
            QuestBehavior.builder().participantScope(QuestParticipantScope.PLAYER).build());
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(() -> Collections.singletonList(
            new StoredQuestDefinition(quest, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L)),
            participant -> participant.getId().equals(observer) || memberUnlocked[0]
                ? Collections.singleton(prerequisite) : Collections.<UUID>emptySet(),
            player -> java.util.Arrays.asList(ParticipantMembership.player(player), party),
            new BqHuntPartyAssignmentPolicy(), ACTIVE_PARTY_FOR_RUNTIME_TEST);
        FakeRepository repository = new FakeRepository();
        QuestRuntimeService service = new QuestRuntimeService(repository, new CountingTransaction(),
            new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
        GameplayFact firstObservation = new GameplayFact("s2", "party-meeting-1", observer,
            "galaxy:nearby_entities_observed", 10L,
            map("observedRadius", "4", "nearbyEntity.count", "1", "nearbyEntity.0.subject", "Villager"));

        FactProcessingResult first = service.apply(firstObservation, planner, 10L);

        assertEquals(2, first.getEvaluations().size());
        assertEquals(QuestStatus.COMPLETED, repository.findProgress(ParticipantId.player(observer), quest.getId(), 1)
            .get().getStatus());
        QuestProgressSnapshot deferred = repository.findProgress(ParticipantId.player(lockedMember), quest.getId(), 1)
            .get();
        assertEquals(QuestStatus.LOCKED, deferred.getStatus());
        assertTrue(deferred.getTasks().get("meeting").isComplete());
        assertEquals(1, repository.entitlements.size());
        assertEquals(observer, repository.entitlements.get(0).getPlayerId());

        memberUnlocked[0] = true;
        GameplayFact secondObservation = new GameplayFact("s2", "party-meeting-2", observer,
            "galaxy:nearby_entities_observed", 20L,
            map("observedRadius", "4", "nearbyEntity.count", "1", "nearbyEntity.0.subject", "Villager"));
        FactProcessingResult afterUnlock = service.apply(secondObservation, planner, 20L);

        assertEquals(QuestStatus.COMPLETED, afterUnlock.getEvaluations().get(1).getProgress().getStatus());
        assertEquals(2, repository.entitlements.size());
        assertEquals(lockedMember, repository.entitlements.get(1).getPlayerId());
    }

    @Test
    public void repeatableBqLocationStartsFreshPartyCycleAfterCooldown() {
        UUID visitor = UUID.randomUUID();
        UUID partyMember = UUID.randomUUID();
        ParticipantId partyId = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership party = new ParticipantMembership(partyId,
            new java.util.LinkedHashSet<UUID>(java.util.Arrays.asList(visitor, partyMember)));
        TaskDefinition location = new TaskDefinition("visit", "bq_standard:location", false,
            map("factType", "galaxy:location_observed", "dimension", "55", "range", "-1", "biome", "-1",
                "posX", "0", "posY", "0", "posZ", "0", "visible", "false", "invert", "false",
                "taxiCabDist", "false", "structure", "", "bq.raw", "{}"));
        RewardDefinition reward = new RewardDefinition("reward", "bq_standard:item",
            Collections.<String, String>emptyMap());
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Repeatable party location", "",
            QuestLogic.AND, QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(location),
            Collections.singletonList(reward), RepeatPolicy.after(100L),
            QuestBehavior.builder().participantScope(QuestParticipantScope.PLAYER).build());
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(() -> Collections.singletonList(
            new StoredQuestDefinition(quest, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L)),
            participant -> Collections.<UUID>emptySet(),
            player -> java.util.Arrays.asList(ParticipantMembership.player(player), party),
            new BqHuntPartyAssignmentPolicy(), ACTIVE_PARTY_FOR_RUNTIME_TEST);
        FakeRepository repository = new FakeRepository();
        QuestRuntimeService service = new QuestRuntimeService(repository, new CountingTransaction(),
            new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());

        FactProcessingResult firstCycle = service.apply(locationFact("repeat-location-1", visitor, 1_000L),
            planner, 1_000L);
        assertEquals(2, firstCycle.getEvaluations().size());
        assertEquals(2, repository.entitlements.size());
        for (UUID member : java.util.Arrays.asList(visitor, partyMember)) {
            QuestProgressSnapshot progress = repository.findProgress(ParticipantId.player(member), quest.getId(), 1)
                .get();
            assertEquals(0, progress.getCycle());
            assertEquals(QuestStatus.COMPLETED, progress.getStatus());
        }

        FactProcessingResult beforeCooldown = service.apply(locationFact("repeat-location-2", visitor, 1_099L),
            planner, 1_099L);
        for (QuestEvaluation evaluation : beforeCooldown.getEvaluations()) {
            assertEquals(0, evaluation.getProgress().getCycle());
            assertEquals(QuestStatus.COMPLETED, evaluation.getProgress().getStatus());
        }
        assertEquals(2, repository.entitlements.size());

        FactProcessingResult heldCycle = service.apply(locationFact("repeat-location-3", visitor, 1_100L),
            planner, 1_100L);
        for (QuestEvaluation evaluation : heldCycle.getEvaluations()) {
            assertEquals(0, evaluation.getProgress().getCycle());
        }
        assertEquals(2, repository.entitlements.size());
        repository.markClaimed(ParticipantId.player(visitor), quest.getId(), 1, 0, 2_000L);
        repository.markClaimed(ParticipantId.player(partyMember), quest.getId(), 1, 0, 2_000L);
        FactProcessingResult beforeClaimCooldown = service.apply(locationFact("repeat-location-4", visitor, 2_099L),
            planner, 2_099L);
        for (QuestEvaluation evaluation : beforeClaimCooldown.getEvaluations()) {
            assertEquals(0, evaluation.getProgress().getCycle());
        }
        FactProcessingResult nextCycle = service.apply(locationFact("repeat-location-5", visitor, 2_100L),
            planner, 2_100L);
        assertEquals(2, nextCycle.getEvaluations().size());
        for (UUID member : java.util.Arrays.asList(visitor, partyMember)) {
            QuestProgressSnapshot progress = repository.findProgress(ParticipantId.player(member), quest.getId(), 1)
                .get();
            assertEquals(1, progress.getCycle());
            assertEquals(QuestStatus.COMPLETED, progress.getStatus());
        }
        assertEquals(4, repository.entitlements.size());
    }

    private static GameplayFact locationFact(String id, UUID visitor, long time) {
        return new GameplayFact("s2", id, visitor, "galaxy:location_observed", time,
            map("dimension", "55", "x", "0", "y", "64", "z", "0", "biome", "1"));
    }

    @Test
    public void bqPartyNonGroupDetectRetrievalLatchesCompletionPerPlayerButDefersLockedMemberReward() {
        UUID source = UUID.randomUUID();
        UUID lockedMember = UUID.randomUUID();
        UUID prerequisite = UUID.randomUUID();
        ParticipantId partyId = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership party = new ParticipantMembership(partyId,
            new java.util.LinkedHashSet<UUID>(java.util.Arrays.asList(source, lockedMember)));
        TaskDefinition retrieval = new TaskDefinition("retrieve", "bq_standard:retrieval", false,
            map("factType", "galaxy:inventory_observed", "consume", "false", "groupDetect", "false",
                "progressMode", "item-vector", "item.count", "2", "item.0.registryName",
                "minecraft:iron_ingot", "item.0.amount", "2", "item.1.registryName", "minecraft:gold_ingot",
                "item.1.amount", "2", "bq.raw", "{}"));
        RewardDefinition reward = new RewardDefinition("reward", "bq_standard:item",
            Collections.<String, String>emptyMap());
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Party retrieval", "", QuestLogic.AND,
            QuestLogic.AND, Collections.singleton(prerequisite), Collections.singletonList(retrieval),
            Collections.singletonList(reward), RepeatPolicy.never(),
            QuestBehavior.builder().participantScope(QuestParticipantScope.PLAYER).build());
        QuestProgressSnapshot sourceProgress = new QuestProgressSnapshot(ParticipantId.player(source),
            quest.getId(), 1, QuestStatus.IN_PROGRESS, Collections.singletonMap("retrieve",
                new TaskProgress("retrieve", java.util.Arrays.asList(2L, 0L), java.util.Arrays.asList(2L, 2L), false)),
            0L, 0);
        QuestProgressSnapshot lockedProgress = new QuestProgressSnapshot(ParticipantId.player(lockedMember),
            quest.getId(), 1, QuestStatus.LOCKED, Collections.singletonMap("retrieve",
                new TaskProgress("retrieve", java.util.Arrays.asList(0L, 2L), java.util.Arrays.asList(2L, 2L), false)),
            0L, 0);
        FakeRepository repository = new FakeRepository();
        repository.seed(sourceProgress);
        repository.seed(lockedProgress);
        QuestRuntimeService service = new QuestRuntimeService(repository, new CountingTransaction(),
            new QuestFactProjector(BqCompatibleTaskEvaluatorRegistry.firstEventDrivenBatch()), new QuestEngine());
        QuestEvaluationRequest sourceRequest = new QuestEvaluationRequest(quest, ParticipantMembership.player(source),
            party, Collections.singleton(prerequisite));
        QuestEvaluationRequest lockedRequest = new QuestEvaluationRequest(quest,
            ParticipantMembership.player(lockedMember), party, Collections.<UUID>emptySet());
        Map<String, String> aggregate = partyInventoryAttributes(partyId);
        aggregate.put("inventory.count", "2");
        aggregate.put("inventory.0.registryName", "minecraft:iron_ingot");
        aggregate.put("inventory.0.meta", "0");
        aggregate.put("inventory.0.amount", "2");
        aggregate.put("inventory.1.registryName", "minecraft:gold_ingot");
        aggregate.put("inventory.1.meta", "0");
        aggregate.put("inventory.1.amount", "2");

        FactProcessingResult first = service.apply(new GameplayFact("s2", "party-retrieval-1", source,
            "galaxy:inventory_observed", 10L, aggregate), java.util.Arrays.asList(sourceRequest, lockedRequest), 10L);

        QuestProgressSnapshot completedSource = repository.findProgress(ParticipantId.player(source),
            quest.getId(), 1).get();
        QuestProgressSnapshot completedLocked = repository.findProgress(ParticipantId.player(lockedMember),
            quest.getId(), 1).get();
        assertEquals(QuestStatus.COMPLETED, completedSource.getStatus());
        assertEquals(java.util.Arrays.asList(2L, 2L), completedSource.getTasks().get("retrieve").getValues());
        assertTrue(completedSource.getTasks().get("retrieve").isComplete());
        assertEquals(QuestStatus.LOCKED, completedLocked.getStatus());
        assertEquals(java.util.Arrays.asList(2L, 2L), completedLocked.getTasks().get("retrieve").getValues());
        assertTrue(completedLocked.getTasks().get("retrieve").isComplete());
        assertEquals(2, first.getEvaluations().size());
        assertEquals(1, repository.entitlements.size());
        assertEquals(source, repository.entitlements.get(0).getPlayerId());

        Map<String, String> unlockedSnapshot = partyInventoryAttributes(partyId);
        unlockedSnapshot.put("inventory.count", "2");
        unlockedSnapshot.put("inventory.0.registryName", "minecraft:iron_ingot");
        unlockedSnapshot.put("inventory.0.meta", "0");
        unlockedSnapshot.put("inventory.0.amount", "2");
        unlockedSnapshot.put("inventory.1.registryName", "minecraft:gold_ingot");
        unlockedSnapshot.put("inventory.1.meta", "0");
        unlockedSnapshot.put("inventory.1.amount", "2");
        FactProcessingResult unlocked = service.apply(new GameplayFact("s2", "party-retrieval-2", lockedMember,
            "galaxy:inventory_observed", 20L, unlockedSnapshot), Collections.singletonList(
                new QuestEvaluationRequest(quest, ParticipantMembership.player(lockedMember), party,
                    Collections.singleton(prerequisite))), 20L);

        assertEquals(QuestStatus.COMPLETED, unlocked.getEvaluations().get(0).getProgress().getStatus());
        assertEquals(2, repository.entitlements.size());
        assertEquals(lockedMember, repository.entitlements.get(1).getPlayerId());
    }

    @Test
    public void pendingConsumptionHoldsExpiredCompletedRepeatCycleUntilRecoveryFinishes() {
        UUID player = UUID.randomUUID();
        TaskDefinition task = new TaskDefinition("hunt", "count", false,
            map("target", "1", "factType", "minecraft:kill", "subject", "Zombie"));
        RewardDefinition reward = new RewardDefinition("reward", "item", Collections.<String, String>emptyMap());
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Repeat hunt", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task),
            Collections.singletonList(reward), RepeatPolicy.after(100L));
        QuestProgressSnapshot completed = new QuestProgressSnapshot(ParticipantId.player(player), quest.getId(), 1,
            QuestStatus.COMPLETED, Collections.singletonMap("hunt", new TaskProgress("hunt", 1, 1, true)),
            100L, 0);
        FakeRepository repository = new FakeRepository();
        repository.seed(completed);
        repository.pendingConsumptionCycles.add(repository.cycleKey(ParticipantId.player(player), quest.getId(), 1, 0));
        TaskEvaluatorRegistry registry = new TaskEvaluatorRegistry();
        registry.register(new FactCountTaskEvaluator("count"));
        QuestRuntimeService service = new QuestRuntimeService(repository, new CountingTransaction(),
            new QuestFactProjector(registry), new QuestEngine());
        List<QuestEvaluationRequest> requests = Collections.singletonList(new QuestEvaluationRequest(quest,
            ParticipantMembership.player(player), Collections.<UUID>emptySet()));

        FactProcessingResult held = service.apply(new GameplayFact("s2", "pending-cycle", player,
            "minecraft:kill", 200L, map("subject", "Zombie", "amount", "1")), requests, 200L);
        assertEquals(0, held.getEvaluations().get(0).getProgress().getCycle());
        assertEquals(QuestStatus.COMPLETED, held.getEvaluations().get(0).getProgress().getStatus());
        assertTrue(held.getEvaluations().get(0).getEntitlements().isEmpty());

        repository.pendingConsumptionCycles.clear();
        FactProcessingResult next = service.apply(new GameplayFact("s2", "after-recovery", player,
            "minecraft:kill", 201L, map("subject", "Zombie", "amount", "1")), requests, 201L);
        assertEquals(1, next.getEvaluations().get(0).getProgress().getCycle());
        assertEquals(1, next.getEvaluations().get(0).getEntitlements().size());
    }

    @Test
    public void resolvesPlanInsideTransactionAndIgnoresIrrelevantFactWithoutPersistingIt() {
        FakeRepository repository = new FakeRepository();
        CountingTransaction transaction = new CountingTransaction();
        TaskEvaluatorRegistry registry = new TaskEvaluatorRegistry();
        registry.register(new FactCountTaskEvaluator("count"));
        QuestRuntimeService service = new QuestRuntimeService(repository, transaction,
            new QuestFactProjector(registry), new QuestEngine());
        GameplayFact fact = new GameplayFact("s2", "irrelevant", UUID.randomUUID(), "other", 1L,
            Collections.<String, String>emptyMap());

        FactProcessingResult result = service.apply(fact, value -> {
            assertTrue(transaction.active);
            return Collections.emptyList();
        }, 1L);

        assertEquals(FactProcessingStatus.IGNORED, result.getStatus());
        assertTrue(repository.facts.isEmpty());
    }

    @Test
    public void noRewardsSuppressesOnlyAutoClaimRewardsThatDoNotOptOut() {
        UUID player = UUID.randomUUID();
        TaskDefinition task = new TaskDefinition("hunt", "count", false,
            map("target", "1", "factType", "minecraft:kill", "subject", "Zombie"));
        List<RewardDefinition> rewards = new ArrayList<RewardDefinition>();
        rewards.add(new RewardDefinition("item", "bq_standard:item", map("ignoreDisabled", "false")));
        rewards.add(new RewardDefinition("completion", "bq_standard:questcompletion", Collections.<String, String>emptyMap()));
        rewards.add(new RewardDefinition("opt-out", "bq_standard:item", map("ignoreDisabled", "true")));
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Hunt", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task), rewards,
            RepeatPolicy.never(), QuestBehavior.builder().autoClaim(true).build());
        TaskEvaluatorRegistry registry = new TaskEvaluatorRegistry();
        registry.register(new FactCountTaskEvaluator("count"));
        FakeRepository repository = new FakeRepository();
        QuestRuntimeService service = new QuestRuntimeService(repository, new CountingTransaction(),
            new QuestFactProjector(registry), new QuestEngine(), new RewardSuppressionPolicy(true));
        GameplayFact fact = new GameplayFact("s2", "kill-no-rewards", player, "minecraft:kill", 10L,
            map("subject", "Zombie", "amount", "1"));

        service.apply(fact, Collections.singletonList(new QuestEvaluationRequest(quest,
            ParticipantMembership.player(player), Collections.<UUID>emptySet())), 10L);

        assertEquals(3, repository.entitlements.size());
        assertEquals(RewardDeliveryStatus.SUPPRESSED, repository.entitlements.get(0).getInitialDeliveryStatus());
        assertEquals(RewardDeliveryStatus.PENDING, repository.entitlements.get(1).getInitialDeliveryStatus());
        assertEquals(RewardDeliveryStatus.PENDING, repository.entitlements.get(2).getInitialDeliveryStatus());
    }

    @Test
    public void noRewardsDoesNotPrematurelySuppressManualClaimEntitlements() {
        UUID player = UUID.randomUUID();
        TaskDefinition task = new TaskDefinition("hunt", "count", false,
            map("target", "1", "factType", "minecraft:kill", "subject", "Zombie"));
        QuestDefinition quest = new QuestDefinition(UUID.randomUUID(), 1, "Hunt", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task), Collections.singletonList(
                new RewardDefinition("item", "bq_standard:item", Collections.<String, String>emptyMap())),
            RepeatPolicy.never(), QuestBehavior.DEFAULT);
        TaskEvaluatorRegistry registry = new TaskEvaluatorRegistry();
        registry.register(new FactCountTaskEvaluator("count"));
        FakeRepository repository = new FakeRepository();
        QuestRuntimeService service = new QuestRuntimeService(repository, new CountingTransaction(),
            new QuestFactProjector(registry), new QuestEngine(), new RewardSuppressionPolicy(true));
        service.apply(new GameplayFact("s2", "kill-manual-claim", player, "minecraft:kill", 10L,
            map("subject", "Zombie", "amount", "1")), Collections.singletonList(new QuestEvaluationRequest(quest,
                ParticipantMembership.player(player), Collections.<UUID>emptySet())), 10L);

        assertEquals(RewardDeliveryStatus.CLAIMABLE, repository.entitlements.get(0).getInitialDeliveryStatus());
    }

    private static Map<String, String> map(String... values) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int i = 0; i < values.length; i += 2) result.put(values[i], values[i + 1]);
        return result;
    }

    private static Map<String, String> partyInventoryAttributes(ParticipantId party) {
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put(BqPartyInventorySnapshotScope.PARTY_SNAPSHOT, "true");
        attributes.put(BqPartyInventorySnapshotScope.PARTY_ID, party.getId().toString());
        return attributes;
    }

    private static final class CountingTransaction implements QuestTransaction {
        private int calls;
        private boolean active;
        @Override public <T> T inTransaction(Work<T> work) {
            calls++;
            active = true;
            try { return work.execute(); } finally { active = false; }
        }
    }

    private static final class FakeRepository implements QuestRuntimeRepository {
        private final Set<String> facts = new HashSet<String>();
        private final Map<String, QuestProgressSnapshot> progress = new LinkedHashMap<String, QuestProgressSnapshot>();
        private final List<RewardEntitlement> entitlements = new ArrayList<RewardEntitlement>();
        private final Set<String> pendingConsumptionCycles = new HashSet<String>();
        private final Set<String> claimedRewardCycles = new HashSet<String>();
        private final Map<String, Long> rewardClaimTimes = new LinkedHashMap<String, Long>();

        @Override public Optional<QuestProgressSnapshot> findProgress(ParticipantId participant, UUID questId,
            int version) {
            return Optional.ofNullable(progress.get(participant + ":" + questId + ":" + version));
        }
        @Override public List<QuestProgressSnapshot> findLockedProgress(ParticipantId participant) {
            List<QuestProgressSnapshot> result = new ArrayList<QuestProgressSnapshot>();
            for (QuestProgressSnapshot value : progress.values()) {
                if (participant.equals(value.getParticipantId()) && value.getStatus() == QuestStatus.LOCKED) {
                    result.add(value);
                }
            }
            return result;
        }
        @Override public boolean hasPendingConsumptionSubmission(ParticipantId participant, UUID questId,
            int version, int cycle) {
            return pendingConsumptionCycles.contains(cycleKey(participant, questId, version, cycle));
        }
        @Override public boolean hasClaimableRewardEntitlement(ParticipantId participant, UUID questId,
            int version, int cycle) {
            if (claimedRewardCycles.contains(cycleKey(participant, questId, version, cycle))) return false;
            for (RewardEntitlement entitlement : entitlements) {
                if (entitlement.getParticipantId().equals(participant) && entitlement.getQuestId().equals(questId)
                    && entitlement.getQuestVersion() == version && entitlement.getCycle() == cycle
                    && entitlement.getInitialDeliveryStatus() == RewardDeliveryStatus.CLAIMABLE) return true;
            }
            return false;
        }
        @Override public long findRepeatCycleAnchorMillis(ParticipantId participant, UUID questId, int version,
            int cycle, long fallbackCompletedAt) {
            Long claimedAt = rewardClaimTimes.get(cycleKey(participant, questId, version, cycle));
            return claimedAt == null ? fallbackCompletedAt : claimedAt.longValue();
        }
        private void markClaimed(ParticipantId participant, UUID questId, int version, int cycle, long claimedAt) {
            String key = cycleKey(participant, questId, version, cycle);
            claimedRewardCycles.add(key);
            rewardClaimTimes.put(key, Long.valueOf(claimedAt));
        }
        private void seed(QuestProgressSnapshot value) {
            progress.put(value.getParticipantId() + ":" + value.getQuestId() + ":" + value.getDefinitionVersion(), value);
        }
        private String cycleKey(ParticipantId participant, UUID questId, int version, int cycle) {
            return participant + ":" + questId + ":" + version + ":" + cycle;
        }
        @Override public boolean recordFactIfAbsent(GameplayFact fact) { return facts.add(fact.getFactKey()); }
        @Override public void saveProgress(QuestProgressSnapshot value) {
            progress.put(value.getParticipantId() + ":" + value.getQuestId() + ":" + value.getDefinitionVersion(), value);
        }
        @Override public void insertEntitlementsIfAbsent(Set<RewardEntitlement> values) { entitlements.addAll(values); }
    }
}
