package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class QuestVisibilityEvaluatorTest {
    @Test
    public void appliesHiddenSecretCompletedAndEditorPolicies() {
        QuestDefinition hidden = quest(QuestVisibility.HIDDEN, Collections.<UUID>emptySet());
        QuestDefinition secret = quest(QuestVisibility.SECRET, Collections.<UUID>emptySet());
        QuestDefinition completed = quest(QuestVisibility.COMPLETED, Collections.<UUID>emptySet());
        Map<UUID, QuestStatus> states = new LinkedHashMap<UUID, QuestStatus>();
        states.put(completed.getId(), QuestStatus.COMPLETED);
        QuestVisibilityEvaluator evaluator = new QuestVisibilityEvaluator();

        assertFalse(evaluator.isVisible(hidden, definitions(hidden), states, false, false));
        assertTrue(evaluator.isVisible(hidden, definitions(hidden), states, true, false));
        assertTrue(evaluator.isVisible(secret, definitions(secret), states, false, false));
        assertTrue(evaluator.isVisible(completed, definitions(completed), states, false, false));
    }

    @Test
    public void normalForeshadowsUnlockedParentsAndChainRecursesWithoutLoopingForever() {
        QuestDefinition parent = quest(QuestVisibility.NORMAL, Collections.<UUID>emptySet());
        QuestDefinition child = quest(QuestVisibility.NORMAL, Collections.singleton(parent.getId()));
        QuestDefinition chain = quest(QuestVisibility.CHAIN, Collections.singleton(child.getId()));
        Map<UUID, QuestDefinition> definitions = definitions(parent, child, chain);
        Map<UUID, QuestStatus> states = new LinkedHashMap<UUID, QuestStatus>();
        states.put(parent.getId(), QuestStatus.AVAILABLE);
        states.put(child.getId(), QuestStatus.LOCKED);
        states.put(chain.getId(), QuestStatus.LOCKED);
        QuestVisibilityEvaluator evaluator = new QuestVisibilityEvaluator();

        assertTrue(evaluator.isVisible(child, definitions, states, false, false));
        assertTrue(evaluator.isVisible(chain, definitions, states, false, false));
    }

    private static QuestDefinition quest(QuestVisibility visibility, java.util.Set<UUID> prerequisites) {
        return new QuestDefinition(UUID.randomUUID(), 1, "Quest", "", QuestLogic.AND, QuestLogic.AND, prerequisites,
            Collections.emptyList(), Collections.emptyList(), RepeatPolicy.never(),
            QuestBehavior.builder().visibility(visibility).build());
    }

    private static Map<UUID, QuestDefinition> definitions(QuestDefinition... values) {
        Map<UUID, QuestDefinition> result = new LinkedHashMap<UUID, QuestDefinition>();
        for (QuestDefinition value : values) result.put(value.getId(), value);
        return result;
    }
}
