package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

public class QuestGraphValidatorTest {
    @Test
    public void rejectsMissingPrerequisitesAndCycles() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        QuestDefinition first = definition(a, b);
        QuestDefinition second = definition(b, a);
        assertTrue(new QuestGraphValidator().validate(Arrays.asList(first, second)).get(0).contains("cycle"));
        assertTrue(new QuestGraphValidator().validate(Collections.singletonList(first)).get(0).contains("missing"));
    }

    private static QuestDefinition definition(UUID id, UUID prerequisite) {
        return new QuestDefinition(id, 1, id.toString(), "", QuestLogic.AND, QuestLogic.AND,
            Collections.singleton(prerequisite), Collections.<TaskDefinition>emptyList(),
            Collections.<RewardDefinition>emptyList());
    }
}
