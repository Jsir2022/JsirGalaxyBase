package com.jsirgalaxybase.ui2.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.motion.Motion;
import com.jsirgalaxybase.ui2.theme.DarkIndustrialTheme;
import com.jsirgalaxybase.ui2.theme.UiTheme;

public class ThemeMotionDrawListTest {
    @Test public void themeContainsRequiredSemanticTokens() {
        UiTheme theme = DarkIndustrialTheme.create();
        assertTrue(theme.color("background") != theme.color("surface"));
        assertTrue(theme.color("danger") != theme.color("success"));
        assertTrue(theme.spacing("md") > theme.spacing("sm"));
    }

    @Test public void fixedClockAndReducedMotionAreDeterministic() {
        FixedUiClock clock = new FixedUiClock(100, false);
        assertEquals(0F, Motion.progress(clock, 100, 200), 0F);
        clock.advance(100);
        assertEquals(0.5F, Motion.progress(clock, 100, 200), 0F);
        assertEquals(1F, Motion.progress(new FixedUiClock(100, true), 100, 200), 0F);
    }

    @Test public void drawListRequiresBalancedStacksAndStableLayers() {
        DrawList list = new DrawList();
        list.add(DrawCommand.surface(0, new UiRect(0, 0, 20, 20), 0xFF000000, 2));
        list.add(DrawCommand.clipPush(1, new UiRect(0, 0, 10, 10)));
        list.add(DrawCommand.text(1, new UiRect(1, 1, 8, 8), 0xFFFFFFFF, "OK"));
        list.add(DrawCommand.clipPop(1));
        list.validateBalanced();
        assertEquals(4, list.size());
        assertTrue(list.snapshot().contains("TEXT|1"));
    }

    @Test(expected = IllegalStateException.class)
    public void unmatchedClipIsRejected() {
        new DrawList().add(DrawCommand.clipPush(0, new UiRect(0, 0, 1, 1))).validateBalanced();
    }

    @Test public void platformCommandsPreserveStableIdsAndLayers() {
        DrawList list = new DrawList()
            .add(DrawCommand.gradientSurface(0, new UiRect(0, 0, 20, 10), 0xFF000000, 0xFFFFFFFF, 3))
            .add(DrawCommand.externalRegion(200, new UiRect(2, 2, 16, 16), "slot:0"));
        assertTrue(list.snapshot().contains("slot:0"));
        assertEquals(1, list.layers(200, 300).size());
    }
}
