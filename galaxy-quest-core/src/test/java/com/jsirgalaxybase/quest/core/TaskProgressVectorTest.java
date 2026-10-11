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

    @Test
    public void sourceScopedCraftingUsesMaxPerSourceThenSumsSources() {
        TaskProgress progress = new TaskProgress("craft", Arrays.asList(0L, 0L), Arrays.asList(10L, 10L), false);
        progress = progress.advanceFromSource("s1", Arrays.asList(4L, 0L));
        progress = progress.observeFromSource("s1", Arrays.asList(3L, 2L));
        assertEquals(Arrays.asList(4L, 2L), progress.getValues());

        progress = progress.observeFromSource("lobby", Arrays.asList(5L, 1L));
        assertEquals(Arrays.asList(9L, 3L), progress.getValues());
        progress = progress.observeFromSource("s1", Arrays.asList(1L, 1L));
        assertEquals("a stale same-source snapshot is maxed locally, not subtracted", Arrays.asList(9L, 3L),
            progress.getValues());
    }

    @Test
    public void sourceScopedUpgradeKeepsLegacyAggregateAsNonAdditiveFloor() {
        TaskProgress legacy = new TaskProgress("craft", Arrays.asList(5L), Arrays.asList(20L), false);
        TaskProgress upgraded = legacy.observeFromSource("s1", Arrays.asList(2L));
        assertEquals("unknown historical source attribution must not regress or double-add", Arrays.asList(5L),
            upgraded.getValues());
        upgraded = upgraded.observeFromSource("s2", Arrays.asList(4L));
        assertEquals(Arrays.asList(6L), upgraded.getValues());
    }

    @Test
    public void sourceScopedCraftingPreservesBqStatisticsAboveTarget() {
        TaskProgress progress = new TaskProgress("craft", Arrays.asList(0L, 0L), Arrays.asList(3L, 4L), false);
        progress = progress.advanceFromSource("s2", Arrays.asList(2L, 2L));
        progress = progress.observeFromSource("s2", Arrays.asList(6L, 6L));
        progress = progress.observeFromSource("lobby", Arrays.asList(7L, 1L));

        assertEquals("same-source event and absolute statistics use max; source totals then sum",
            Arrays.asList(13L, 7L), progress.getValues());
        assertEquals(Arrays.asList(3L, 4L), progress.getTargets());
        assertTrue("completion still uses the configured thresholds", progress.isComplete());
    }

    @Test
    public void sourceScopedCraftingCapsEachServerEventAtTheTaskTarget() {
        TaskProgress progress = new TaskProgress("craft", Arrays.asList(0L, 0L), Arrays.asList(3L, 4L), false);
        progress = progress.advanceFromSource("s2", Arrays.asList(8L, 2L));

        assertEquals(Arrays.asList(3L, 2L), progress.getValues());
        assertEquals(Arrays.asList(3L, 2L), progress.getSourceContributions().get("s2").getEventValues());
        assertFalse(progress.isComplete());
    }
}
