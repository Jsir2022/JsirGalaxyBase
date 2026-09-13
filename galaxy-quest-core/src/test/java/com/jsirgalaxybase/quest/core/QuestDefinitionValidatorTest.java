package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import org.junit.Test;

public class QuestDefinitionValidatorTest {
    @Test
    public void editorRejectsUnknownTypesDuplicateKeysAndSelfDependency() {
        UUID id = UUID.randomUUID();
        QuestDefinition definition = new QuestDefinition(id, 1, "Bad", "", QuestLogic.AND, QuestLogic.AND,
            Collections.singleton(id), Arrays.asList(
                new TaskDefinition("same", "known", false, null),
                new TaskDefinition("same", "missing", false, null)),
            Collections.singletonList(new RewardDefinition("reward", "missing-reward", null)));
        assertEquals(4, new QuestDefinitionValidator().validate(definition,
            new HashSet<String>(Collections.singletonList("known")), Collections.<String>emptySet()).size());
    }
}
