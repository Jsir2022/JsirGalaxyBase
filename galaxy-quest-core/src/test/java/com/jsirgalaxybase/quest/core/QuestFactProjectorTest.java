package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

public class QuestFactProjectorTest {
    @Test
    public void projectsMatchingFactsAndExposesStableDeduplicationKey() {
        UUID player = UUID.randomUUID();
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("target", "64");
        parameters.put("factType", "minecraft:item_obtained");
        parameters.put("subject", "minecraft:iron_ingot:0");
        TaskDefinition task = new TaskDefinition("iron", "count", false, parameters);
        QuestDefinition definition = new QuestDefinition(UUID.randomUUID(), 1, "Iron", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task),
            Collections.<RewardDefinition>emptyList());
        QuestProgressSnapshot initial = new QuestEngine().evaluate(player, definition, null,
            Collections.<String, TaskProgress>emptyMap(), Collections.<UUID>emptySet(), 1L).getProgress();
        TaskEvaluatorRegistry registry = new TaskEvaluatorRegistry();
        registry.register(new FactCountTaskEvaluator("count"));
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        attributes.put("subject", "minecraft:iron_ingot:0");
        attributes.put("amount", "16");
        GameplayFact fact = new GameplayFact("s2", "craft:42", player, "minecraft:item_obtained", 2L, attributes);
        Map<String, TaskProgress> observed = new QuestFactProjector(registry).project(definition, initial, fact);

        assertEquals("s2:craft:42", fact.getFactKey());
        assertEquals(16L, observed.get("iron").getValue());
    }

    @Test
    public void partyProgressAcceptsFactsFromAnySnapshottedMember() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ParticipantId party = ParticipantId.party(UUID.randomUUID());
        TaskDefinition task = new TaskDefinition("meeting", "count", false,
            map("target", "2", "factType", "minecraft:meeting", "subject", "station"));
        QuestDefinition definition = new QuestDefinition(UUID.randomUUID(), 1, "Meeting", "", QuestLogic.AND,
            QuestLogic.AND, Collections.<UUID>emptySet(), Collections.singletonList(task),
            Collections.<RewardDefinition>emptyList());
        ParticipantMembership membership = new ParticipantMembership(party,
            new HashSet<UUID>(Arrays.asList(first, second)));
        QuestProgressSnapshot initial = new QuestEngine().evaluate(membership, definition, null,
            Collections.<String, TaskProgress>emptyMap(), Collections.<UUID>emptySet(), 1L).getProgress();
        TaskEvaluatorRegistry registry = new TaskEvaluatorRegistry();
        registry.register(new FactCountTaskEvaluator("count"));
        GameplayFact fact = new GameplayFact("s1", "meeting:1", second, "minecraft:meeting", 2L,
            map("subject", "station", "amount", "1"));
        assertEquals(1L, new QuestFactProjector(registry).project(definition, initial, membership, fact)
            .get("meeting").getValue());
    }

    private static Map<String, String> map(String... values) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int i = 0; i < values.length; i += 2) result.put(values[i], values[i + 1]);
        return result;
    }
}
