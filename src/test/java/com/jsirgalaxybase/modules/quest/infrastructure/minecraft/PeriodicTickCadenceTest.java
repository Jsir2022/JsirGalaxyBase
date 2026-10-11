package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PeriodicTickCadenceTest {

    @Test
    public void sixtyTickCraftingPassRunsOnlyOnBoundary() {
        assertTrue(PeriodicTickCadence.isDue(0, 60));
        assertFalse(PeriodicTickCadence.isDue(59, 60));
        assertTrue(PeriodicTickCadence.isDue(60, 60));
        assertFalse(PeriodicTickCadence.isDue(61, 60));
    }

    @Test
    public void invalidIntervalsNeverSchedule() {
        assertFalse(PeriodicTickCadence.isDue(60, 0));
        assertFalse(PeriodicTickCadence.isDue(60, -1));
    }
}
