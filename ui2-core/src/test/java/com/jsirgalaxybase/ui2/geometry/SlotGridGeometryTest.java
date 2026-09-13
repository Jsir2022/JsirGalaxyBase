package com.jsirgalaxybase.ui2.geometry;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class SlotGridGeometryTest {
    @Test public void centersFixedNativeSizedCells() {
        List<UiRect> cells = SlotGridGeometry.cells(new UiRect(10, 20, 36, 20), 2, 3);
        assertEquals(2, cells.size());
        assertEquals(new UiRect(11, 22, 16, 16), cells.get(0));
        assertEquals(new UiRect(29, 22, 16, 16), cells.get(1));
    }

    @Test public void rejectsInvalidOrOverflowCells() {
        assertEquals(0, SlotGridGeometry.cells(null, 2, 2).size());
        assertEquals(0, SlotGridGeometry.cells(new UiRect(0, 0, 15, 15), 1, 1).size());
    }
}
