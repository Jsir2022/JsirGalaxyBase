package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class QuestEvaluationCandidateIndexTest {
    @Test
    public void factTypeIndexSelectsOnlyMatchingDefinitionsAtFixedCatalogScale() {
        List<StoredQuestDefinition> definitions = new ArrayList<StoredQuestDefinition>();
        for (int i = 0; i < 2500; i++) definitions.add(stored("galaxy:unrelated_" + i));
        QuestDefinition target = stored("galaxy:inventory_observed").getDefinition();
        definitions.add(stored(target));

        QuestEvaluationCandidateIndex index = new QuestEvaluationCandidateIndex(definitions);
        List<StoredQuestDefinition> candidates = index.forFact(fact("galaxy:inventory_observed",
            Collections.<String, String>emptyMap()));

        assertEquals(2501, index.definitionCount());
        assertEquals(1, candidates.size());
        assertEquals(target.getId(), candidates.get(0).getDefinition().getId());
    }

    @Test
    public void targetedFactsAndPrerequisiteReconciliationUseDirectIndexes() {
        UUID targetId = UUID.randomUUID();
        UUID prerequisite = UUID.randomUUID();
        QuestDefinition target = definition(targetId, "galaxy:item_produced", prerequisite);
        QuestDefinition other = definition(UUID.randomUUID(), "galaxy:task_confirmed");
        QuestEvaluationCandidateIndex index = new QuestEvaluationCandidateIndex(Arrays.asList(stored(other),
            stored(target)));

        assertEquals(Collections.singletonList(stored(target).getDefinition().getId()), ids(index.forFact(
            fact("galaxy:item_produced", singleton("targetQuestId", targetId.toString())))));
        assertEquals(Collections.singletonList(other.getId()), ids(index.forFact(
            fact("galaxy:consumption_applied", singleton("questId", other.getId().toString())))));
        assertEquals(Collections.singletonList(target.getId()), ids(index.newlyUnlockedBy(prerequisite)));
        assertEquals(Arrays.asList(other.getId(), target.getId()), ids(index.forIds(
            new LinkedHashSet<UUID>(Arrays.asList(target.getId(), other.getId())))));
        assertTrue(index.forFact(fact("galaxy:task_confirmed", singleton("questId", "not-a-uuid"))).isEmpty());
    }

    private static GameplayFact fact(String type, Map<String, String> attributes) {
        return new GameplayFact("s2", UUID.randomUUID().toString(), UUID.randomUUID(), type, 1L, attributes);
    }

    private static Map<String, String> singleton(String key, String value) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        result.put(key, value);
        return result;
    }

    private static QuestDefinition definition(UUID id, String factType, UUID... prerequisites) {
        Map<String, String> parameters = singleton("factType", factType);
        TaskDefinition task = new TaskDefinition("task", "bq_standard:checkbox", false, parameters);
        return new QuestDefinition(id, 1, "Q", "", QuestLogic.AND, QuestLogic.AND,
            new LinkedHashSet<UUID>(Arrays.asList(prerequisites)), Collections.singletonList(task),
            Collections.<RewardDefinition>emptyList());
    }

    private static StoredQuestDefinition stored(String factType) {
        return stored(definition(UUID.randomUUID(), factType));
    }

    private static StoredQuestDefinition stored(QuestDefinition definition) {
        return new StoredQuestDefinition(definition, QuestDefinitionLifecycle.PUBLISHED, "hash", 1L);
    }

    private static List<UUID> ids(List<StoredQuestDefinition> definitions) {
        List<UUID> result = new ArrayList<UUID>();
        for (StoredQuestDefinition definition : definitions) result.add(definition.getDefinition().getId());
        return result;
    }
}
