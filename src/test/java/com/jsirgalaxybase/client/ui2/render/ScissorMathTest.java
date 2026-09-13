package com.jsirgalaxybase.client.ui2.render;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.jsirgalaxybase.ui2.geometry.UiRect;

public class ScissorMathTest {
    @Test public void convertsTopLeftGuiCoordinatesToBottomLeftFramebufferCoordinates() {
        assertEquals(new UiRect(20, 280, 60, 80), ScissorMath.toFramebuffer(new UiRect(10, 20, 30, 40), 2, 400));
    }
    @Test public void intersectionNeverProducesNegativeSize() {
        assertEquals(new UiRect(20, 20, 0, 0), ScissorMath.intersect(new UiRect(0, 0, 10, 10), new UiRect(20, 20, 5, 5)));
    }
}
