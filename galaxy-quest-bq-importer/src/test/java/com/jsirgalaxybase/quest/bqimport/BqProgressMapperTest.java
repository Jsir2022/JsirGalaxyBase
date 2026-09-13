package com.jsirgalaxybase.quest.bqimport;

import static org.junit.Assert.*;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestLogic;
import com.jsirgalaxybase.quest.core.RepeatPolicy;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.quest.core.TaskProgress;

public class BqProgressMapperTest {
    @Test
    public void mapsVectorScalarAndBooleanProgressWithoutFlattening() {
        UUID questId = UUID.randomUUID();
        TaskDefinition items = task("task-0", "bq_standard:retrieval", "item-vector", "item", 4L, 8L);
        TaskDefinition hunt = task("task-1", "bq_standard:hunt", "scalar", null, 10L);
        TaskDefinition checkbox = task("task-2", "bq_standard:checkbox", "boolean", null, 1L);
        QuestDefinition definition = new QuestDefinition(questId, 1, "Q", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Arrays.asList(items, hunt, checkbox), Collections.emptyList(),
            RepeatPolicy.never());
        BqImportedQuestProgress imported = new BqImportedQuestProgress(questId, false, false, 0L, Arrays.asList(
            new BqImportedTaskProgress("task-0", "bq_standard:retrieval", false, Arrays.asList(4L, 3L), "{}"),
            new BqImportedTaskProgress("task-1", "bq_standard:hunt", true, Arrays.asList(10L), "{}"),
            new BqImportedTaskProgress("task-2", "bq_standard:checkbox", true, Collections.<Long>emptyList(), "{}")),
            "{}");
        Map<String, TaskProgress> mapped = new BqProgressMapper().toCore(definition, imported);
        assertEquals(Arrays.asList(4L, 3L), mapped.get("task-0").getValues());
        assertFalse(mapped.get("task-0").isComplete());
        assertTrue(mapped.get("task-1").isComplete());
        assertTrue(mapped.get("task-2").isComplete());
    }

    @Test(expected = BqProgressShapeException.class)
    public void rejectsChangedVectorWidthInsteadOfSilentlyDroppingProgress() {
        UUID questId = UUID.randomUUID();
        TaskDefinition items = task("task-0", "bq_standard:retrieval", "item-vector", "item", 4L, 8L);
        QuestDefinition definition = new QuestDefinition(questId, 1, "Q", "", QuestLogic.AND, QuestLogic.AND,
            Collections.<UUID>emptySet(), Collections.singletonList(items), Collections.emptyList(), RepeatPolicy.never());
        BqImportedQuestProgress imported = new BqImportedQuestProgress(questId, false, false, 0L,
            Collections.singletonList(new BqImportedTaskProgress("task-0", "bq_standard:retrieval", false,
                Collections.singletonList(4L), "{}")), "{}");
        new BqProgressMapper().toCore(definition, imported);
    }

    private static TaskDefinition task(String key, String type, String mode, String prefix, long... targets) {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("progressMode", mode);
        if (prefix == null) {
            parameters.put("target", Long.toString(targets[0]));
        } else {
            parameters.put(prefix + ".count", Integer.toString(targets.length));
            for (int i = 0; i < targets.length; i++) {
                parameters.put(prefix + "." + i + ".amount", Long.toString(targets[i]));
            }
        }
        return new TaskDefinition(key, type, false, parameters);
    }
}
