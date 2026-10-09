package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

public class QuestEvaluationPlannerTest {
    @Test
    public void selectsOnlyRelevantPublishedPlayerQuestAndLoadsPrerequisites() {
        UUID player = UUID.randomUUID();
        UUID completed = UUID.randomUUID();
        QuestDefinition relevant = quest("galaxy:entity_killed");
        QuestDefinition unrelated = quest("galaxy:item_produced");
        PublishedQuestCatalog catalog = () -> Arrays.asList(stored(relevant), stored(unrelated));
        QuestCompletionQuery completions = participant -> Collections.singleton(completed);
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(catalog, completions,
            new PlayerOnlyMembershipResolver(), new PlayerQuestAssignmentPolicy());

        List<QuestEvaluationRequest> result = planner.requestsFor(new GameplayFact("s2", "event", player,
            "galaxy:entity_killed", 1L, Collections.<String, String>emptyMap()));

        assertEquals(1, result.size());
        assertEquals(relevant.getId(), result.get(0).getDefinition().getId());
        assertEquals(ParticipantId.player(player), result.get(0).getMembership().getParticipantId());
        assertTrue(result.get(0).getCompletedPrerequisites().contains(completed));
    }

    @Test
    public void newlyUnlockedLockedProgressRequestsRequireAllPrerequisitesAndPlayerScope() {
        UUID playerId = UUID.randomUUID();
        UUID completed = UUID.randomUUID();
        UUID pending = UUID.randomUUID();
        QuestBehavior lockedProgress = QuestBehavior.builder().progressWhileLocked(true).build();
        QuestDefinition eligible = new QuestDefinition(UUID.randomUUID(), 1, "Eligible", "", QuestLogic.AND,
            QuestLogic.AND, Collections.singleton(completed), Collections.<TaskDefinition>emptyList(),
            Collections.<RewardDefinition>emptyList(), RepeatPolicy.never(), lockedProgress);
        QuestDefinition stillLocked = new QuestDefinition(UUID.randomUUID(), 1, "Still locked", "", QuestLogic.AND,
            QuestLogic.AND, new LinkedHashSet<UUID>(Arrays.asList(completed, pending)),
            Collections.<TaskDefinition>emptyList(), Collections.<RewardDefinition>emptyList(), RepeatPolicy.never(),
            lockedProgress);
        QuestDefinition notConfigured = new QuestDefinition(UUID.randomUUID(), 1, "Not configured", "",
            QuestLogic.AND, QuestLogic.AND, Collections.singleton(completed), Collections.<TaskDefinition>emptyList(),
            Collections.<RewardDefinition>emptyList());
        QuestCompletionQuery completions = participant -> Collections.singleton(completed);
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(
            () -> Arrays.asList(stored(eligible), stored(stillLocked), stored(notConfigured)), completions,
            new PlayerOnlyMembershipResolver(), new PlayerQuestAssignmentPolicy());

        List<QuestEvaluationRequest> result = planner.requestsForNewlyUnlockedLockedProgress(
            ParticipantId.player(playerId), completed);

        assertEquals(1, result.size());
        assertEquals(eligible.getId(), result.get(0).getDefinition().getId());
        assertEquals(ParticipantId.player(playerId), result.get(0).getMembership().getParticipantId());
        assertTrue(result.get(0).getCompletedPrerequisites().contains(completed));
    }

    @Test
    public void lockedProgressReconciliationOnlyPlansPersistedPlayerRowsThatAreNowUnlocked() {
        UUID playerId = UUID.randomUUID();
        UUID prerequisite = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        QuestBehavior playerLocked = QuestBehavior.builder().progressWhileLocked(true).build();
        QuestDefinition eligible = new QuestDefinition(UUID.randomUUID(), 1, "Eligible", "", QuestLogic.AND,
            QuestLogic.AND, Collections.singleton(prerequisite), Collections.<TaskDefinition>emptyList(),
            Collections.<RewardDefinition>emptyList(), RepeatPolicy.never(), playerLocked);
        QuestDefinition stillLocked = new QuestDefinition(UUID.randomUUID(), 1, "Still locked", "", QuestLogic.AND,
            QuestLogic.AND, Collections.singleton(missing), Collections.<TaskDefinition>emptyList(),
            Collections.<RewardDefinition>emptyList(), RepeatPolicy.never(), playerLocked);
        QuestDefinition partyScope = new QuestDefinition(UUID.randomUUID(), 1, "Party", "", QuestLogic.AND,
            QuestLogic.AND, Collections.singleton(prerequisite), Collections.<TaskDefinition>emptyList(),
            Collections.<RewardDefinition>emptyList(), RepeatPolicy.never(), QuestBehavior.builder()
                .progressWhileLocked(true).participantScope(QuestParticipantScope.PARTY).build());
        QuestCompletionQuery completions = participant -> Collections.singleton(prerequisite);
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(() -> Arrays.asList(stored(eligible),
            stored(stillLocked), stored(partyScope)), completions, new PlayerOnlyMembershipResolver(),
            new PlayerQuestAssignmentPolicy());

        List<QuestEvaluationRequest> result = planner.requestsForLockedProgressReconciliation(
            ParticipantId.player(playerId), new HashSet<UUID>(Arrays.asList(eligible.getId(), stillLocked.getId(),
                partyScope.getId())));

        assertEquals(1, result.size());
        assertEquals(eligible.getId(), result.get(0).getDefinition().getId());
        assertEquals(ParticipantId.player(playerId), result.get(0).getMembership().getParticipantId());
    }

    @Test
    public void bqHuntFansOutPersonalProgressToEveryTrustedPartyMember() {
        UUID hunter = UUID.randomUUID();
        UUID partyMember = UUID.randomUUID();
        ParticipantId partyId = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership party = new ParticipantMembership(partyId,
            new LinkedHashSet<UUID>(Arrays.asList(hunter, partyMember)));
        QuestDefinition hunt = bqHunt(QuestParticipantScope.PLAYER);
        ParticipantMembershipResolver resolver = new ParticipantMembershipResolver() {
            @Override public List<ParticipantMembership> resolve(UUID ignored) {
                return Arrays.asList(ParticipantMembership.player(hunter), party);
            }
        };
        QuestCompletionQuery completions = participant -> Collections.singleton(participant.getId());
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(() -> Collections.singletonList(stored(hunt)),
            completions, resolver, new BqHuntPartyAssignmentPolicy(), bqPartyActiveIfUnlocked(completions));

        List<QuestEvaluationRequest> requests = planner.requestsFor(new GameplayFact("s2", "hunt-party",
            hunter, "galaxy:entity_killed", 10L, Collections.<String, String>singletonMap("subject", "Zombie")));

        assertEquals(2, requests.size());
        assertEquals(ParticipantId.player(hunter), requests.get(0).getMembership().getParticipantId());
        assertEquals(ParticipantId.player(partyMember), requests.get(1).getMembership().getParticipantId());
        for (QuestEvaluationRequest request : requests) {
            assertEquals(partyId, request.getFactMembership().getParticipantId());
            assertTrue(request.getFactMembership().contains(hunter));
            assertTrue(request.getCompletedPrerequisites().contains(request.getMembership().getParticipantId().getId()));
        }
    }

    @Test
    public void bqHuntPartyFanoutDoesNotChangeAggregateScopeOrNonHuntTasks() {
        UUID hunter = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        ParticipantId partyId = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership party = new ParticipantMembership(partyId,
            new LinkedHashSet<UUID>(Arrays.asList(hunter, member)));
        List<ParticipantMembership> memberships = Arrays.asList(ParticipantMembership.player(hunter), party);
        BqHuntPartyAssignmentPolicy policy = new BqHuntPartyAssignmentPolicy();
        GameplayFact fact = new GameplayFact("s2", "hunt-party", hunter, "galaxy:entity_killed", 10L,
            Collections.<String, String>emptyMap());

        assertEquals(Collections.singletonList(party), policy.select(bqHunt(QuestParticipantScope.PARTY),
            memberships, fact));
        List<ParticipantMembership> nonHunt = policy.select(quest("galaxy:entity_killed"), memberships, fact);
        assertEquals(1, nonHunt.size());
        assertEquals(ParticipantId.player(hunter), nonHunt.get(0).getParticipantId());
        List<ParticipantMembership> noParty = policy.select(bqHunt(QuestParticipantScope.PLAYER),
            Collections.singletonList(ParticipantMembership.player(hunter)), fact);
        assertEquals(1, noParty.size());
        assertEquals(ParticipantId.player(hunter), noParty.get(0).getParticipantId());
    }

    @Test
    public void bqCraftingEventFansOutToPartyWhenAnyMemberHasUnlockedTheQuest() {
        UUID source = UUID.randomUUID();
        UUID unlockedMember = UUID.randomUUID();
        UUID prerequisite = UUID.randomUUID();
        ParticipantId partyId = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership party = new ParticipantMembership(partyId,
            new LinkedHashSet<UUID>(Arrays.asList(source, unlockedMember)));
        QuestDefinition crafting = bqCrafting(QuestParticipantScope.PLAYER, true, true, false, prerequisite);
        QuestCompletionQuery completions = participant -> participant.getId().equals(unlockedMember)
            ? Collections.singleton(prerequisite) : Collections.<UUID>emptySet();
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(() -> Collections.singletonList(stored(crafting)),
            completions, ignored -> Arrays.asList(ParticipantMembership.player(source), party),
            new BqHuntPartyAssignmentPolicy(), bqPartyActiveIfUnlocked(completions));

        List<QuestEvaluationRequest> requests = planner.requestsFor(craftingFact("craft-party", source, "craft"));

        assertEquals(2, requests.size());
        assertEquals(ParticipantId.player(source), requests.get(0).getMembership().getParticipantId());
        assertEquals(ParticipantId.player(unlockedMember), requests.get(1).getMembership().getParticipantId());
        for (QuestEvaluationRequest request : requests) assertEquals(partyId,
            request.getFactMembership().getParticipantId());
        assertTrue(requests.get(0).getCompletedPrerequisites().isEmpty());
        assertTrue(requests.get(1).getCompletedPrerequisites().contains(prerequisite));
    }

    @Test
    public void bqCraftingPartyEventRequiresAnUnlockedMemberAndEnabledDirectChannel() {
        UUID source = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        UUID prerequisite = UUID.randomUUID();
        ParticipantId partyId = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership party = new ParticipantMembership(partyId,
            new LinkedHashSet<UUID>(Arrays.asList(source, member)));
        QuestDefinition locked = bqCrafting(QuestParticipantScope.PLAYER, true, true, false, prerequisite);
        ParticipantMembershipResolver resolver = ignored -> Arrays.asList(ParticipantMembership.player(source), party);
        QuestCompletionQuery noCompletions = ignored -> Collections.<UUID>emptySet();
        QuestEvaluationPlanner allLocked = new QuestEvaluationPlanner(() -> Collections.singletonList(stored(locked)),
            noCompletions, resolver, new BqHuntPartyAssignmentPolicy(), bqPartyActiveIfUnlocked(noCompletions));

        assertTrue(allLocked.requestsFor(craftingFact("craft-locked", source, "craft")).isEmpty());

        QuestDefinition noAnvil = bqCrafting(QuestParticipantScope.PLAYER, true, true, false);
        QuestEvaluationPlanner enabled = new QuestEvaluationPlanner(() -> Collections.singletonList(stored(noAnvil)),
            noCompletions, resolver, new BqHuntPartyAssignmentPolicy(), bqPartyActiveIfUnlocked(noCompletions));
        assertEquals(1, enabled.requestsFor(craftingFact("craft-stat", source, "craft-statistics")).size());
        assertEquals(1, enabled.requestsFor(craftingFact("craft-anvil", source, "anvil")).size());
        assertEquals(2, enabled.requestsFor(craftingFact("craft-smelt", source, "smelt")).size());
    }

    @Test
    public void bqCraftingPartyEventAllowsLockedProgressWhenEveryMemberIsStillLocked() {
        UUID source = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        UUID prerequisite = UUID.randomUUID();
        ParticipantId partyId = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership party = new ParticipantMembership(partyId,
            new LinkedHashSet<UUID>(Arrays.asList(source, member)));
        QuestDefinition base = bqCrafting(QuestParticipantScope.PLAYER, true, true, false, prerequisite);
        QuestDefinition lockedProgress = new QuestDefinition(base.getId(), base.getVersion(), base.getName(),
            base.getDescription(), base.getPrerequisiteLogic(), base.getTaskLogic(), base.getPrerequisites(),
            base.getTasks(), base.getRewards(), base.getRepeatPolicy(),
            base.getBehavior().toBuilder().progressWhileLocked(true).build());
        QuestCompletionQuery noCompletions = ignored -> Collections.<UUID>emptySet();
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(
            () -> Collections.singletonList(stored(lockedProgress)), noCompletions,
            ignored -> Arrays.asList(ParticipantMembership.player(source), party),
            new BqHuntPartyAssignmentPolicy(), bqPartyActiveIfUnlocked(noCompletions));

        List<QuestEvaluationRequest> requests = planner.requestsFor(craftingFact("craft-locked-progress", source,
            "craft"));

        assertEquals(2, requests.size());
        for (QuestEvaluationRequest request : requests) {
            assertEquals(partyId, request.getFactMembership().getParticipantId());
            assertTrue(request.getCompletedPrerequisites().isEmpty());
        }
    }

    @Test
    public void bqLocationCompletesPersonalPartyProgressWhenAnyMemberHasTheQuestActive() {
        UUID source = UUID.randomUUID();
        UUID unlockedMember = UUID.randomUUID();
        UUID prerequisite = UUID.randomUUID();
        ParticipantId partyId = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership party = new ParticipantMembership(partyId,
            new LinkedHashSet<UUID>(Arrays.asList(source, unlockedMember)));
        QuestDefinition location = bqLocation(QuestParticipantScope.PLAYER, prerequisite);
        ParticipantMembershipResolver resolver = ignored -> Arrays.asList(ParticipantMembership.player(source), party);
        QuestCompletionQuery noCompletions = ignored -> Collections.<UUID>emptySet();
        QuestEvaluationPlanner allLocked = new QuestEvaluationPlanner(() -> Collections.singletonList(stored(location)),
            noCompletions, resolver, new BqHuntPartyAssignmentPolicy(), bqPartyActiveIfUnlocked(noCompletions));

        assertTrue(allLocked.requestsFor(locationFact("location-locked", source, 55)).isEmpty());

        QuestCompletionQuery oneUnlockedCompletion = participant -> participant.getId().equals(unlockedMember)
            ? Collections.singleton(prerequisite) : Collections.<UUID>emptySet();
        QuestEvaluationPlanner oneUnlocked = new QuestEvaluationPlanner(() -> Collections.singletonList(stored(location)),
            oneUnlockedCompletion, resolver, new BqHuntPartyAssignmentPolicy(),
            bqPartyActiveIfUnlocked(oneUnlockedCompletion));
        List<QuestEvaluationRequest> requests = oneUnlocked.requestsFor(locationFact("location-active", source, 55));

        assertEquals(2, requests.size());
        assertEquals(ParticipantId.player(source), requests.get(0).getMembership().getParticipantId());
        assertEquals(ParticipantId.player(unlockedMember), requests.get(1).getMembership().getParticipantId());
        for (QuestEvaluationRequest request : requests) assertEquals(partyId,
            request.getFactMembership().getParticipantId());
    }

    @Test
    public void bqMeetingEventFansOutToPartyAfterAnyMemberUnlocksTheQuest() {
        UUID source = UUID.randomUUID();
        UUID unlockedMember = UUID.randomUUID();
        UUID prerequisite = UUID.randomUUID();
        ParticipantId partyId = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership party = new ParticipantMembership(partyId,
            new LinkedHashSet<UUID>(Arrays.asList(source, unlockedMember)));
        QuestDefinition meeting = bqMeeting(QuestParticipantScope.PLAYER, prerequisite);
        ParticipantMembershipResolver resolver = ignored -> Arrays.asList(ParticipantMembership.player(source), party);
        GameplayFact fact = meetingFact("meeting-party", source);
        QuestCompletionQuery noCompletions = ignored -> Collections.<UUID>emptySet();
        QuestEvaluationPlanner allLocked = new QuestEvaluationPlanner(() -> Collections.singletonList(stored(meeting)),
            noCompletions, resolver, new BqHuntPartyAssignmentPolicy(), bqPartyActiveIfUnlocked(noCompletions));

        assertTrue(allLocked.requestsFor(fact).isEmpty());

        QuestCompletionQuery oneUnlockedCompletion = participant -> participant.getId().equals(unlockedMember)
            ? Collections.singleton(prerequisite) : Collections.<UUID>emptySet();
        QuestEvaluationPlanner oneUnlocked = new QuestEvaluationPlanner(() -> Collections.singletonList(stored(meeting)),
            oneUnlockedCompletion, resolver, new BqHuntPartyAssignmentPolicy(),
            bqPartyActiveIfUnlocked(oneUnlockedCompletion));
        List<QuestEvaluationRequest> requests = oneUnlocked.requestsFor(fact);

        assertEquals(2, requests.size());
        assertEquals(ParticipantId.player(source), requests.get(0).getMembership().getParticipantId());
        assertEquals(ParticipantId.player(unlockedMember), requests.get(1).getMembership().getParticipantId());
        for (QuestEvaluationRequest request : requests) {
            assertEquals(partyId, request.getFactMembership().getParticipantId());
        }
    }

    @Test
    public void bqPartyRetrievalSnapshotFansOutAllImportedNonConsumingRetrievalsToExactTrustedParty() {
        UUID source = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        ParticipantId partyId = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership party = new ParticipantMembership(partyId,
            new LinkedHashSet<UUID>(Arrays.asList(source, member)));
        QuestDefinition personal = retrievalQuest(QuestParticipantScope.PLAYER, false, true);
        QuestDefinition nonGroup = retrievalQuest(QuestParticipantScope.PLAYER, false);
        QuestDefinition consuming = retrievalQuest(QuestParticipantScope.PLAYER, true);
        QuestDefinition aggregate = retrievalQuest(QuestParticipantScope.PARTY, false);
        ParticipantMembershipResolver resolver = new ParticipantMembershipResolver() {
            @Override public List<ParticipantMembership> resolve(UUID ignored) {
                return Arrays.asList(ParticipantMembership.player(source), party);
            }
        };
        QuestCompletionQuery noCompletions = ignored -> Collections.<UUID>emptySet();
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(
            () -> Arrays.asList(stored(personal), stored(nonGroup), stored(consuming), stored(aggregate)),
            noCompletions, resolver, new BqHuntPartyAssignmentPolicy(), bqPartyActiveIfUnlocked(noCompletions));
        GameplayFact fact = bqPartyInventoryFact("party-snapshot-1", source, partyId,
            map("inventory.count", "0"));

        List<QuestEvaluationRequest> requests = planner.requestsFor(fact);

        assertEquals(4, requests.size());
        Set<UUID> requestedQuests = new HashSet<UUID>();
        for (QuestEvaluationRequest request : requests) {
            requestedQuests.add(request.getDefinition().getId());
            assertEquals(partyId, request.getFactMembership().getParticipantId());
            assertTrue(request.getFactMembership().contains(source));
            assertTrue(request.getFactMembership().contains(request.getMembership().getParticipantId().getId()));
        }
        assertEquals(new HashSet<UUID>(Arrays.asList(personal.getId(), nonGroup.getId())), requestedQuests);
        assertTrue(requests.stream().anyMatch(request -> request.getDefinition().getId().equals(personal.getId())
            && request.getMembership().getParticipantId().equals(ParticipantId.player(source))));
        assertTrue(requests.stream().anyMatch(request -> request.getDefinition().getId().equals(personal.getId())
            && request.getMembership().getParticipantId().equals(ParticipantId.player(member))));
        assertTrue(requests.stream().anyMatch(request -> request.getDefinition().getId().equals(nonGroup.getId())
            && request.getMembership().getParticipantId().equals(ParticipantId.player(source))));
        assertTrue(requests.stream().anyMatch(request -> request.getDefinition().getId().equals(nonGroup.getId())
            && request.getMembership().getParticipantId().equals(ParticipantId.player(member))));
    }

    @Test(expected = IllegalArgumentException.class)
    public void bqPartyRetrievalSnapshotRejectsPartyThatDoesNotContainSource() {
        UUID source = UUID.randomUUID();
        ParticipantId partyId = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership party = new ParticipantMembership(partyId, Collections.singleton(UUID.randomUUID()));
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(Collections::emptyList,
            participant -> Collections.<UUID>emptySet(), ignored -> Arrays.asList(
                ParticipantMembership.player(source), party), new BqHuntPartyAssignmentPolicy());

        planner.requestsFor(bqPartyInventoryFact("party-snapshot-invalid", source, partyId,
            Collections.<String, String>emptyMap()));
    }

    @Test
    public void allImportedNonConsumingRetrievalsRequireTrustedPartyInventoryFacts() {
        UUID source = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        ParticipantId partyId = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership party = new ParticipantMembership(partyId,
            new LinkedHashSet<UUID>(Arrays.asList(source, member)));
        QuestDefinition groupDetect = retrievalQuest(QuestParticipantScope.PLAYER, false, true);
        QuestDefinition ordinaryDetect = retrievalQuest(QuestParticipantScope.PLAYER, false, false);
        QuestCompletionQuery noCompletions = ignored -> Collections.<UUID>emptySet();
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(
            () -> Arrays.asList(stored(groupDetect), stored(ordinaryDetect)),
            noCompletions, ignored -> Arrays.asList(ParticipantMembership.player(source), party),
            new BqHuntPartyAssignmentPolicy(), bqPartyActiveIfUnlocked(noCompletions));
        GameplayFact singlePlayer = new GameplayFact("s2", "single-inventory", source,
            "galaxy:inventory_observed", 10L, map("inventory.count", "0"));

        assertTrue(planner.requestsFor(singlePlayer).isEmpty());
        assertEquals(4, planner.requestsFor(bqPartyInventoryFact("party-inventory", source, partyId,
            map("inventory.count", "0"))).size());
    }

    @Test
    public void importedBqRetrievalUsesPersonalInventoryWhenPlayerHasNoParty() {
        UUID source = UUID.randomUUID();
        QuestDefinition groupDetect = retrievalQuest(QuestParticipantScope.PLAYER, false, true);
        QuestDefinition optionalRetrieval = retrievalQuest("bq_standard:optional_retrieval",
            QuestParticipantScope.PLAYER, false, false);
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(
            () -> Arrays.asList(stored(groupDetect), stored(optionalRetrieval)),
            participant -> Collections.<UUID>emptySet(),
            ignored -> Collections.singletonList(ParticipantMembership.player(source)),
            new BqHuntPartyAssignmentPolicy());
        GameplayFact personalInventory = new GameplayFact("s2", "solo-inventory", source,
            "galaxy:inventory_observed", 10L, map("inventory.count", "0"));

        assertEquals("BQ scans the player's own inventory when ParticipantInfo has no party",
            2, planner.requestsFor(personalInventory).size());
    }

    @Test
    public void bqPartyRetrievalSnapshotRequiresOriginToMeetItsOwnPrerequisites() {
        UUID source = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        UUID prerequisite = UUID.randomUUID();
        ParticipantId partyId = ParticipantId.party(UUID.randomUUID());
        ParticipantMembership party = new ParticipantMembership(partyId,
            new LinkedHashSet<UUID>(Arrays.asList(source, member)));
        QuestDefinition locked = retrievalQuest(QuestParticipantScope.PLAYER, false, true, prerequisite);
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(() -> Collections.singletonList(stored(locked)),
            participant -> Collections.<UUID>emptySet(), ignored -> Arrays.asList(
                ParticipantMembership.player(source), party), new BqHuntPartyAssignmentPolicy());

        assertTrue(planner.requestsFor(bqPartyInventoryFact("party-snapshot-locked", source, partyId,
            Collections.<String, String>emptyMap())).isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMembershipThatDoesNotContainFactPlayer() {
        UUID player = UUID.randomUUID();
        ParticipantMembershipResolver bad = ignored -> Collections.singletonList(
            new ParticipantMembership(ParticipantId.party(UUID.randomUUID()), Collections.singleton(UUID.randomUUID())));
        new QuestEvaluationPlanner(Collections::emptyList, participant -> Collections.emptySet(), bad,
            new PlayerQuestAssignmentPolicy()).requestsFor(new GameplayFact("s2", "event", player,
                "galaxy:entity_killed", 1L, Collections.<String, String>emptyMap()));
    }

    @Test
    public void consumptionFactIsBoundToExactQuestVersionAndConsumingTask() {
        UUID player = UUID.randomUUID();
        QuestDefinition definition = consumingQuest();
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(() -> Collections.singletonList(stored(definition)),
            participant -> Collections.emptySet(), new PlayerOnlyMembershipResolver(), new PlayerQuestAssignmentPolicy());
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("questId", definition.getId().toString());
        attributes.put("definitionVersion", Integer.toString(definition.getVersion()));
        attributes.put("taskKey", "consume");
        assertEquals(1, planner.requestsFor(new GameplayFact("s2", "consume", player,
            "galaxy:consumption_applied", 1L, attributes)).size());
        attributes.put("definitionVersion", "2");
        assertTrue(planner.requestsFor(new GameplayFact("s2", "wrong", player,
            "galaxy:consumption_applied", 1L, attributes)).isEmpty());
    }

    @Test
    public void checkboxConfirmationIsBoundToExactQuestVersionAndTask() {
        UUID player = UUID.randomUUID();
        QuestDefinition target = checkboxQuest();
        QuestDefinition other = checkboxQuest();
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(
            () -> Arrays.asList(stored(target), stored(other)), participant -> Collections.emptySet(),
            new PlayerOnlyMembershipResolver(), new PlayerQuestAssignmentPolicy());
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("questId", target.getId().toString());
        attributes.put("definitionVersion", Integer.toString(target.getVersion()));
        attributes.put("taskKey", "task-0");
        GameplayFact valid = new GameplayFact("s2", "check-1", player, "galaxy:task_confirmed", 1L, attributes);
        List<QuestEvaluationRequest> selected = planner.requestsFor(valid);
        assertEquals(1, selected.size());
        assertEquals(target.getId(), selected.get(0).getDefinition().getId());

        attributes.put("definitionVersion", "2");
        assertTrue(planner.requestsFor(new GameplayFact("s2", "check-2", player,
            "galaxy:task_confirmed", 2L, attributes)).isEmpty());
        attributes.put("definitionVersion", "1");
        attributes.put("taskKey", "missing");
        assertTrue(planner.requestsFor(new GameplayFact("s2", "check-3", player,
            "galaxy:task_confirmed", 3L, attributes)).isEmpty());
        attributes.remove("questId");
        attributes.put("taskKey", "task-0");
        assertTrue(planner.requestsFor(new GameplayFact("s2", "check-4", player,
            "galaxy:task_confirmed", 4L, attributes)).isEmpty());
    }

    @Test
    public void authorizedConsumptionKeepsOriginalGroupAfterSubmittingPlayerLeaves() {
        UUID player = UUID.randomUUID(), remaining = UUID.randomUUID();
        ParticipantId team = new ParticipantId(ParticipantType.TEAM, UUID.randomUUID());
        QuestDefinition definition = consumingQuest(QuestParticipantScope.TEAM);
        ParticipantMembershipResolver resolver = new ParticipantMembershipResolver() {
            @Override public List<ParticipantMembership> resolve(UUID ignored) { return Collections.emptyList(); }
            @Override public ParticipantMembership resolveParticipant(ParticipantId requested) {
                assertEquals(team, requested);
                return new ParticipantMembership(team, Collections.singleton(remaining));
            }
        };
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(() -> Collections.singletonList(stored(definition)),
            participant -> Collections.emptySet(), resolver, new ScopedQuestAssignmentPolicy());
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("questId", definition.getId().toString());
        attributes.put("definitionVersion", Integer.toString(definition.getVersion()));
        attributes.put("taskKey", "consume");
        attributes.put("progressParticipantType", "TEAM");
        attributes.put("progressParticipantId", team.getId().toString());
        List<QuestEvaluationRequest> result = planner.requestsFor(new GameplayFact("s2", "consume", player,
            "galaxy:consumption_applied", 1L, attributes));
        assertEquals(1, result.size());
        assertEquals(team, result.get(0).getMembership().getParticipantId());
        assertEquals(Collections.singleton(remaining), result.get(0).getMembership().getPlayerIds());
    }

    @Test
    public void aggregateInventorySnapshotTargetsOnlyItsCurrentSharedScope() {
        UUID player = UUID.randomUUID();
        ParticipantId team = ParticipantId.team(UUID.randomUUID());
        QuestDefinition shared = inventoryQuest(QuestParticipantScope.TEAM);
        QuestDefinition personal = inventoryQuest(QuestParticipantScope.PLAYER);
        ParticipantMembership membership = new ParticipantMembership(team, Collections.singleton(player));
        ParticipantMembershipResolver resolver = new ParticipantMembershipResolver() {
            @Override public List<ParticipantMembership> resolve(UUID ignored) {
                fail("aggregate snapshots must resolve the captured participant, not all player memberships");
                return Collections.emptyList();
            }
            @Override public ParticipantMembership resolveParticipant(ParticipantId requested) {
                assertEquals(team, requested);
                return membership;
            }
        };
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(
            () -> Arrays.asList(stored(shared), stored(personal)), participant -> Collections.emptySet(), resolver,
            new ScopedQuestAssignmentPolicy());
        GameplayFact aggregate = new GameplayFact("s2", "team-inventory", player, "galaxy:inventory_observed", 5L,
            inventoryScope(team));

        List<QuestEvaluationRequest> requests = planner.requestsFor(aggregate);
        assertEquals(1, requests.size());
        assertEquals(shared.getId(), requests.get(0).getDefinition().getId());
        assertEquals(team, requests.get(0).getMembership().getParticipantId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void aggregateInventorySnapshotRejectsOriginOutsideCurrentMembership() {
        UUID player = UUID.randomUUID();
        ParticipantId team = ParticipantId.team(UUID.randomUUID());
        ParticipantMembershipResolver resolver = new ParticipantMembershipResolver() {
            @Override public List<ParticipantMembership> resolve(UUID ignored) { return Collections.emptyList(); }
            @Override public ParticipantMembership resolveParticipant(ParticipantId ignored) {
                return new ParticipantMembership(team, Collections.singleton(UUID.randomUUID()));
            }
        };
        QuestEvaluationPlanner planner = new QuestEvaluationPlanner(Collections::emptyList,
            participant -> Collections.emptySet(), resolver, new ScopedQuestAssignmentPolicy());
        planner.requestsFor(new GameplayFact("s2", "team-inventory", player, "galaxy:inventory_observed", 5L,
            inventoryScope(team)));
    }

    private static StoredQuestDefinition stored(QuestDefinition definition) {
        return new StoredQuestDefinition(definition, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L);
    }

    /** These planner tests model every trusted roster member as online; online filtering is tested separately. */
    private static QuestPartyActivityQuery bqPartyActiveIfUnlocked(QuestCompletionQuery completions) {
        return (definition, fact, party) -> {
            if (definition.getPrerequisites().isEmpty() || definition.getBehavior().isProgressWhileLocked()) {
                return true;
            }
            for (UUID member : party.getPlayerIds()) {
                Set<UUID> completed = completions.findCompletedQuestIds(ParticipantId.player(member));
                if (completed == null) continue;
                int count = 0;
                for (UUID prerequisite : definition.getPrerequisites()) if (completed.contains(prerequisite)) count++;
                if (definition.getPrerequisiteLogic().satisfied(count, definition.getPrerequisites().size())) return true;
            }
            return false;
        };
    }

    private static QuestDefinition quest(String factType) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("target", "1");
        parameters.put("factType", factType);
        return new QuestDefinition(UUID.randomUUID(), 1, "Q", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.singletonList(new TaskDefinition("task", "count", false, parameters)),
            Collections.<RewardDefinition>emptyList(), RepeatPolicy.never());
    }

    private static QuestDefinition consumingQuest() {
        return consumingQuest(QuestParticipantScope.PLAYER);
    }

    private static QuestDefinition checkboxQuest() {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("target", "1");
        parameters.put("factType", "galaxy:task_confirmed");
        return new QuestDefinition(UUID.randomUUID(), 1, "Check", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.singletonList(new TaskDefinition("task-0",
                "bq_standard:checkbox", false, parameters)), Collections.<RewardDefinition>emptyList(),
            RepeatPolicy.never());
    }

    private static QuestDefinition consumingQuest(QuestParticipantScope scope) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("target", "1");
        parameters.put("consume", "true");
        return new QuestDefinition(UUID.randomUUID(), 1, "Consume", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.singletonList(new TaskDefinition("consume",
                "bq_standard:retrieval", false, parameters)), Collections.<RewardDefinition>emptyList(),
            RepeatPolicy.never(), QuestBehavior.builder().participantScope(scope).build());
    }

    private static QuestDefinition inventoryQuest(QuestParticipantScope scope) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("factType", "galaxy:inventory_observed");
        parameters.put("consume", "false");
        parameters.put("groupDetect", "true");
        parameters.put("progressMode", "item-vector");
        parameters.put("item.count", "1");
        parameters.put("item.0.amount", "2");
        TaskDefinition task = new TaskDefinition("retrieve", "bq_standard:retrieval", false, parameters);
        return new QuestDefinition(UUID.randomUUID(), 1, "Retrieve", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.singletonList(task), Collections.<RewardDefinition>emptyList(),
            RepeatPolicy.never(), QuestBehavior.builder().participantScope(scope).build());
    }

    private static QuestDefinition retrievalQuest(QuestParticipantScope scope, boolean consume,
        UUID... prerequisites) {
        return retrievalQuest(scope, consume, false, prerequisites);
    }

    private static QuestDefinition retrievalQuest(QuestParticipantScope scope, boolean consume,
        boolean groupDetect, UUID... prerequisites) {
        return retrievalQuest("bq_standard:retrieval", scope, consume, groupDetect, prerequisites);
    }

    private static QuestDefinition retrievalQuest(String typeId, QuestParticipantScope scope, boolean consume,
        boolean groupDetect, UUID... prerequisites) {
        Map<String, String> parameters = map("factType", "galaxy:inventory_observed", "consume",
            Boolean.toString(consume), "groupDetect", Boolean.toString(groupDetect), "progressMode", "item-vector",
            "item.count", "1", "item.0.registryName", "minecraft:iron_ingot", "item.0.amount", "2",
            "bq.raw", "{}");
        TaskDefinition task = new TaskDefinition("retrieve", typeId, false, parameters);
        Set<UUID> required = prerequisites == null || prerequisites.length == 0
            ? Collections.<UUID>emptySet() : new LinkedHashSet<UUID>(Arrays.asList(prerequisites));
        return new QuestDefinition(UUID.randomUUID(), 1, "Retrieve", "", QuestLogic.AND, QuestLogic.AND,
            required, Collections.singletonList(task), Collections.<RewardDefinition>emptyList(),
            RepeatPolicy.never(), QuestBehavior.builder().participantScope(scope).build());
    }

    private static GameplayFact bqPartyInventoryFact(String key, UUID source, ParticipantId party,
        Map<String, String> inventory) {
        Map<String, String> attributes = new LinkedHashMap<String, String>(inventory);
        attributes.put(BqPartyInventorySnapshotScope.PARTY_SNAPSHOT, "true");
        attributes.put(BqPartyInventorySnapshotScope.PARTY_ID, party.getId().toString());
        return new GameplayFact("s2", key, source, "galaxy:inventory_observed", 10L, attributes);
    }

    private static QuestDefinition bqHunt(QuestParticipantScope scope) {
        TaskDefinition task = new TaskDefinition("hunt", "bq_standard:hunt", false,
            map("target", "1", "factType", "galaxy:entity_killed", "subject", "Zombie", "subtypes", "true"));
        return new QuestDefinition(UUID.randomUUID(), 1, "Hunt", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.singletonList(task), Collections.<RewardDefinition>emptyList(),
            RepeatPolicy.never(), QuestBehavior.builder().participantScope(scope).build());
    }

    private static QuestDefinition bqCrafting(QuestParticipantScope scope, boolean allowCraft, boolean allowSmelt,
        boolean allowAnvil, UUID... prerequisites) {
        Map<String, String> parameters = map("factType", "galaxy:item_produced", "progressMode", "item-vector",
            "item.count", "1", "item.0.registryName", "minecraft:iron_ingot", "item.0.meta", "0",
            "item.0.amount", "1", "allowCraft", Boolean.toString(allowCraft), "allowSmelt",
            Boolean.toString(allowSmelt), "allowAnvil", Boolean.toString(allowAnvil), "bq.raw", "{}");
        TaskDefinition task = new TaskDefinition("craft", "bq_standard:crafting", false, parameters);
        Set<UUID> required = prerequisites == null || prerequisites.length == 0
            ? Collections.<UUID>emptySet() : new LinkedHashSet<UUID>(Arrays.asList(prerequisites));
        return new QuestDefinition(UUID.randomUUID(), 1, "Craft", "", QuestLogic.AND, QuestLogic.AND,
            required, Collections.singletonList(task), Collections.<RewardDefinition>emptyList(), RepeatPolicy.never(),
            QuestBehavior.builder().participantScope(scope).build());
    }

    private static GameplayFact craftingFact(String id, UUID source, String channel) {
        return new GameplayFact("s2", id, source, "galaxy:item_produced", 10L,
            map("registryName", "minecraft:iron_ingot", "meta", "0", "damageable", "false", "amount", "1",
                "channel", channel, "oreDictionary", ""));
    }

    private static QuestDefinition bqLocation(QuestParticipantScope scope, UUID... prerequisites) {
        TaskDefinition task = new TaskDefinition("location", "bq_standard:location", false,
            map("factType", "galaxy:location_observed", "dimension", "55", "range", "-1", "biome", "-1",
                "posX", "0", "posY", "0", "posZ", "0", "visible", "false", "invert", "false",
                "taxiCabDist", "false", "structure", "", "bq.raw", "{}"));
        Set<UUID> required = prerequisites == null || prerequisites.length == 0
            ? Collections.<UUID>emptySet() : new LinkedHashSet<UUID>(Arrays.asList(prerequisites));
        return new QuestDefinition(UUID.randomUUID(), 1, "Location", "", QuestLogic.AND, QuestLogic.AND,
            required, Collections.singletonList(task), Collections.<RewardDefinition>emptyList(), RepeatPolicy.never(),
            QuestBehavior.builder().participantScope(scope).build());
    }

    private static QuestDefinition bqMeeting(QuestParticipantScope scope, UUID... prerequisites) {
        TaskDefinition task = new TaskDefinition("meeting", "bq_standard:meeting", false,
            map("factType", "galaxy:nearby_entities_observed", "subject", "Villager", "range", "4",
                "amount", "1", "subtypes", "true", "ignoreNBT", "true", "bq.raw", "{}"));
        Set<UUID> required = prerequisites == null || prerequisites.length == 0
            ? Collections.<UUID>emptySet() : new LinkedHashSet<UUID>(Arrays.asList(prerequisites));
        return new QuestDefinition(UUID.randomUUID(), 1, "Meeting", "", QuestLogic.AND, QuestLogic.AND,
            required, Collections.singletonList(task), Collections.<RewardDefinition>emptyList(), RepeatPolicy.never(),
            QuestBehavior.builder().participantScope(scope).build());
    }

    private static GameplayFact meetingFact(String id, UUID source) {
        return new GameplayFact("s2", id, source, "galaxy:nearby_entities_observed", 10L,
            map("observedRadius", "4", "nearbyEntity.count", "1", "nearbyEntity.0.subject", "Villager"));
    }

    private static GameplayFact locationFact(String id, UUID source, int dimension) {
        return new GameplayFact("s2", id, source, "galaxy:location_observed", 10L,
            map("dimension", Integer.toString(dimension), "x", "0", "y", "64", "z", "0", "biome", "1"));
    }

    private static Map<String, String> inventoryScope(ParticipantId participant) {
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("inventory.groupSnapshot", "true");
        attributes.put("inventory.participantType", participant.getType().name());
        attributes.put("inventory.participantId", participant.getId().toString());
        return attributes;
    }

    private static Map<String, String> map(String... values) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int i = 0; i < values.length; i += 2) result.put(values[i], values[i + 1]);
        return result;
    }
}
