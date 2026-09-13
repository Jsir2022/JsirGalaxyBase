package com.jsirgalaxybase.ui2.debug;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.text.TextStyle;

public class UiDebugSnapshotTest {
    @Test public void reportsViewportAndStructuralProblems() {
        DrawList list = new DrawList()
            .add(DrawCommand.surface(0, new UiRect(0, 0, 100, 60), 0xFF000000, 0))
            .add(DrawCommand.text(1, new UiRect(90, 4, 20, 5), 0xFFFFFFFF, "overflow", TextStyle.BODY))
            .add(DrawCommand.externalRegion(2, new UiRect(4, 20, 16, 16), "slot"))
            .add(DrawCommand.externalRegion(2, new UiRect(24, 20, 16, 16), "slot"));
        String value = UiDebugSnapshot.capture(null, list, 100, 60);
        assertTrue(value.contains("WARN|OUTSIDE_VIEWPORT"));
        assertTrue(value.contains("WARN|TEXT_HEIGHT"));
        assertTrue(value.contains("WARN|DUPLICATE_EXTERNAL_ID|slot"));
        assertTrue(value.contains("SUMMARY|issues=3"));
    }
}
