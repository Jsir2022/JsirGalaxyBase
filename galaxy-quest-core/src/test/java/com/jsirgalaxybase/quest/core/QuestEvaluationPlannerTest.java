package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
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

    private static StoredQuestDefinition stored(QuestDefinition definition) {
        return new StoredQuestDefinition(definition, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L);
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

    private static QuestDefinition consumingQuest(QuestParticipantScope scope) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("target", "1");
        parameters.put("consume", "true");
        return new QuestDefinition(UUID.randomUUID(), 1, "Consume", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.singletonList(new TaskDefinition("consume",
                "bq_standard:retrieval", false, parameters)), Collections.<RewardDefinition>emptyList(),
            RepeatPolicy.never(), QuestBehavior.builder().participantScope(scope).build());
    }
}
