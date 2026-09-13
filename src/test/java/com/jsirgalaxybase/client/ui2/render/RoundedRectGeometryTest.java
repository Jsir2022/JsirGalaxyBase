package com.jsirgalaxybase.client.ui2.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RoundedRectGeometryTest {
    @Test public void radiusIsClampedAndSegmentsTrackPhysicalDensity() {
        assertEquals(5F, RoundedRectGeometry.clampRadius(10F, 40F, 20F), 0F);
        assertEquals(0F, RoundedRectGeometry.clampRadius(-1F, 10F, 3F), 0F);
        assertTrue(RoundedRectGeometry.segments(8F, 4F)
            > RoundedRectGeometry.segments(8F, 1F));
        assertTrue(RoundedRectGeometry.segments(100F, 4F) <= 18);
    }

    @Test public void outlineStaysInsideItsRectangle() {
        RoundedRectGeometry.Point[] points = RoundedRectGeometry.outline(10F, 20F, 110F, 70F, 9F, 8);
        assertEquals(36, points.length);
        for (RoundedRectGeometry.Point point : points) {
            assertTrue(point.getX() >= 10F && point.getX() <= 110F);
            assertTrue(point.getY() >= 20F && point.getY() <= 70F);
        }
    }

    @Test public void independentCornerRadiiAreProportionallyClamped() {
        RoundedRectGeometry.CornerRadii clamped = RoundedRectGeometry.clampRadii(20F, 10F,
            new RoundedRectGeometry.CornerRadii(9F, 8F, 7F, 6F));
        assertTrue(clamped.getTopLeft() + clamped.getBottomLeft() <= 10.001F);
        assertTrue(clamped.getTopRight() + clamped.getBottomRight() <= 10.001F);
        assertTrue(clamped.getTopLeft() + clamped.getTopRight() <= 20.001F);
        assertTrue(clamped.getBottomLeft() + clamped.getBottomRight() <= 20.001F);
    }

    @Test public void coverageAndHairlineStayOnePhysicalPixelAtEveryGuiScale() {
        for (float density : new float[] { 1F, 2F, 4F }) {
            assertEquals(0.5F, RoundedRectGeometry.coverageInset(density) * density, 0F);
            assertEquals(1F, RoundedRectGeometry.hairline(density) * density, 0F);
        }
    }
}
