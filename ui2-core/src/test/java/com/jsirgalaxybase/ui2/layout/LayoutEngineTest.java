package com.jsirgalaxybase.ui2.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.ui2.geometry.Constraints;
import com.jsirgalaxybase.ui2.geometry.Insets;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.geometry.UiSize;

public class LayoutEngineTest {
    @Test public void rowAllocatesFixedAndFlexibleChildren() {
        LayoutSpec root = LayoutSpec.of("root", LayoutKind.ROW).gap(4).padding(Insets.all(2))
            .child(LayoutSpec.of("nav", LayoutKind.LEAF).preferred(60, 20).build())
            .child(LayoutSpec.of("body", LayoutKind.LEAF).flex(1).build()).build();
        LayoutResult result = new LayoutEngine().layout(root, new UiRect(0, 0, 350, 193));
        assertEquals(new UiRect(2, 2, 60, 189), result.get("nav"));
        assertEquals(new UiRect(66, 2, 282, 189), result.get("body"));
    }

    @Test public void gridAndCompactViewStayInsideBounds() {
        LayoutSpec.Builder grid = LayoutSpec.of("grid", LayoutKind.GRID).columns(3).gap(3).padding(Insets.all(4));
        for (int i = 0; i < 8; i++) grid.child(LayoutSpec.of("cell-" + i, LayoutKind.LEAF).build());
        UiRect viewport = new UiRect(0, 0, 350, 193);
        LayoutResult result = new LayoutEngine().layout(grid.build(), viewport);
        for (UiRect rect : result.asMap().values()) assertTrue(viewport.contains(rect));
    }

    @Test public void measureHonorsTightConstraintsAndLargeInputs() {
        LayoutSpec spec = LayoutSpec.of("root", LayoutKind.COLUMN).gap(2).padding(Insets.all(3))
            .child(LayoutSpec.of("a", LayoutKind.LEAF).preferred(Integer.MAX_VALUE, 20).build()).build();
        UiSize measured = new LayoutEngine().measure(spec, Constraints.tight(620, 340));
        assertEquals(new UiSize(620, 340), measured);
    }
}
