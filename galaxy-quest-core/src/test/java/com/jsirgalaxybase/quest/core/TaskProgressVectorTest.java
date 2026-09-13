package com.jsirgalaxybase.quest.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class TaskProgressVectorTest {
    @Test
    public void vectorRequiresEveryComponentAndMergesMonotonically() {
        TaskProgress current = new TaskProgress("items", Arrays.asList(4L, 1L), Arrays.asList(4L, 8L), false);
        assertFalse(current.isComplete());
        assertEquals(5L, current.getValue());
        assertEquals(12L, current.getTarget());

        TaskProgress merged = current.merge(Arrays.asList(2L, 8L), false);
        assertEquals(Arrays.asList(4L, 8L), merged.getValues());
        assertTrue(merged.isComplete());
    }

    @Test(expected = IllegalStateException.class)
    public void scalarMergeCannotSilentlyFlattenVectorProgress() {
        new TaskProgress("items", Arrays.asList(0L, 0L), Arrays.asList(1L, 1L), false).merge(1L, false);
    }

    @Test
    public void createsVectorShapeFromNormalizedDefinition() {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("progressMode", "item-vector");
        parameters.put("item.count", "2");
        parameters.put("item.0.amount", "4");
        parameters.put("item.1.amount", "8");
        TaskProgress empty = TaskProgress.empty(new TaskDefinition("items", "retrieval", false, parameters));
        assertEquals(Arrays.asList(0L, 0L), empty.getValues());
        assertEquals(Arrays.asList(4L, 8L), empty.getTargets());
    }

    @Test
    public void absoluteSnapshotMayDecreaseUntilCompletionThenRemainsLatched() {
        TaskProgress partial = new TaskProgress("items", Arrays.asList(3L, 2L), Arrays.asList(4L, 4L), false);
        assertEquals(Arrays.asList(1L, 0L), partial.replaceIncomplete(Arrays.asList(1L, 0L)).getValues());
        TaskProgress complete = new TaskProgress("items", Arrays.asList(4L, 4L), Arrays.asList(4L, 4L), true);
        assertSame(complete, complete.replaceIncomplete(Arrays.asList(0L, 0L)));
    }
}
