package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.UUID;

import org.junit.Test;

public class QuestDraftEditorTest {
    @Test
    public void semanticOperationsBuildNewImmutableDefinitionWithoutMutatingSource() {
        UUID prerequisite = UUID.randomUUID();
        TaskDefinition first = task("first");
        QuestDefinition source = definition(4, Collections.singletonList(first), Collections.singletonList(reward("old")));

        QuestDefinition edited = QuestDraftEditor.nextVersion(source)
            .name("Edited")
            .description("description")
            .taskLogic(QuestLogic.OR)
            .addPrerequisite(prerequisite)
            .addTask(task("second"))
            .moveTask("second", 0)
            .replaceReward("old", reward("new"))
            .behavior(QuestBehavior.builder().autoClaim(true).visibility(QuestVisibility.SECRET).build())
            .build();

        assertNotSame(source, edited);
        assertEquals(4, source.getVersion());
        assertEquals(1, source.getTasks().size());
        assertEquals(5, edited.getVersion());
        assertEquals("Edited", edited.getName());
        assertEquals(QuestLogic.OR, edited.getTaskLogic());
        assertEquals(Arrays.asList("second", "first"), Arrays.asList(
            edited.getTasks().get(0).getKey(), edited.getTasks().get(1).getKey()));
        assertEquals("new", edited.getRewards().get(0).getKey());
        assertEquals(QuestVisibility.SECRET, edited.getBehavior().getVisibility());
        assertTrue(edited.getBehavior().isAutoClaim());
        assertEquals(new LinkedHashSet<UUID>(Collections.singletonList(prerequisite)), edited.getPrerequisites());
    }

    @Test
    public void rejectsAmbiguousKeysMissingElementsAndSelfPrerequisiteAtEditBoundary() {
        QuestDefinition source = definition(1, Collections.singletonList(task("task")),
            Collections.singletonList(reward("reward")));
        QuestDraftEditor editor = QuestDraftEditor.edit(source);

        assertThrows(IllegalArgumentException.class, () -> editor.addTask(task("task")));
        assertThrows(IllegalArgumentException.class, () -> editor.addReward(reward("reward")));
        assertThrows(IllegalArgumentException.class, () -> editor.removeTask("missing"));
        assertThrows(IllegalArgumentException.class, () -> editor.addPrerequisite(source.getId()));
        assertThrows(IndexOutOfBoundsException.class, () -> editor.moveReward("reward", 2));
    }

    private static QuestDefinition definition(int version, java.util.List<TaskDefinition> tasks,
        java.util.List<RewardDefinition> rewards) {
        return new QuestDefinition(UUID.randomUUID(), version, "Quest", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), tasks, rewards, RepeatPolicy.never());
    }

    private static TaskDefinition task(String key) {
        return new TaskDefinition(key, "test:task", false, new LinkedHashMap<String, String>());
    }

    private static RewardDefinition reward(String key) {
        return new RewardDefinition(key, "test:reward", new LinkedHashMap<String, String>());
    }
}
