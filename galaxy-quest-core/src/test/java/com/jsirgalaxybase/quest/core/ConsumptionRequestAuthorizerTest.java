package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

public class ConsumptionRequestAuthorizerTest {
    @Test public void supportsManualPlayerOwnedItemRetrievalIncludingRepeatableQuests() {
        assertTrue(ConsumptionRequestAuthorizer.supports(definition(QuestBehavior.DEFAULT, RepeatPolicy.never()),
            task("bq_standard:retrieval", "true", "false")));
        assertTrue(ConsumptionRequestAuthorizer.supports(definition(QuestBehavior.DEFAULT, RepeatPolicy.never()),
            task("bq_standard:optional_retrieval", "true", "false")));
        assertFalse(ConsumptionRequestAuthorizer.supports(definition(QuestBehavior.DEFAULT, RepeatPolicy.never()),
            task("bq_standard:retrieval", "false", "false")));
        assertFalse(ConsumptionRequestAuthorizer.supports(definition(QuestBehavior.DEFAULT, RepeatPolicy.never()),
            task("bq_standard:retrieval", "true", "true")));
        assertTrue(ConsumptionRequestAuthorizer.supports(definition(QuestBehavior.DEFAULT, RepeatPolicy.after(1000L, true)),
            task("bq_standard:retrieval", "true", "false")));
        assertFalse(ConsumptionRequestAuthorizer.supports(definition(QuestBehavior.builder()
            .participantScope(QuestParticipantScope.PARTY).build(), RepeatPolicy.never()),
            task("bq_standard:retrieval", "true", "false")));
        assertTrue(ConsumptionRequestAuthorizer.supports(definition(QuestBehavior.DEFAULT, RepeatPolicy.never()),
            task("bq_standard:fluid", "true", "false")));
        assertFalse(ConsumptionRequestAuthorizer.supports(definition(QuestBehavior.DEFAULT, RepeatPolicy.never()),
            task("bq_standard:fluid", "true", "true")));
        assertFalse(ConsumptionRequestAuthorizer.supports(definition(QuestBehavior.DEFAULT, RepeatPolicy.never()),
            task("bq_standard:fluid", "false", "false")));
        assertFalse(ConsumptionRequestAuthorizer.supports(definition(QuestBehavior.builder()
            .participantScope(QuestParticipantScope.PARTY).build(), RepeatPolicy.never()),
            task("bq_standard:fluid", "true", "false")));
    }

    private static QuestDefinition definition(QuestBehavior behavior, RepeatPolicy repeatPolicy) {
        return new QuestDefinition(UUID.randomUUID(), 1, "test", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.<TaskDefinition>emptyList(),
            Collections.<RewardDefinition>emptyList(), repeatPolicy, behavior);
    }

    private static TaskDefinition task(String type, String consume, String autoConsume) {
        java.util.Map<String, String> parameters = new java.util.LinkedHashMap<String, String>();
        parameters.put("consume", consume);
        parameters.put("autoConsume", autoConsume);
        return new TaskDefinition("task-0", type, false, parameters);
    }
}
