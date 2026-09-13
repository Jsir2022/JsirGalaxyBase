package com.jsirgalaxybase.client.ui2.font;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FontMeasurementBoundsTest {
    @Test public void clampsBrokenNegativeMeasurements() {
        assertEquals(0, FontMeasurementBounds.width(-17, 2F, 100));
        assertEquals(0F, FontMeasurementBounds.drawWidth(-17, 2F), 0F);
    }

    @Test public void clampsInvalidScaleAndWidthLimit() {
        assertEquals(0, FontMeasurementBounds.width(12, Float.NaN, 100));
        assertEquals(0, FontMeasurementBounds.width(12, 1F, -1));
    }

    @Test public void preservesNormalCeilingAndSaturatesLargeValues() {
        assertEquals(19, FontMeasurementBounds.width(15, 1.25F, 100));
        assertEquals(100, FontMeasurementBounds.width(Integer.MAX_VALUE, Float.MAX_VALUE, 100));
        assertEquals(Float.MAX_VALUE,
            FontMeasurementBounds.drawWidth(Integer.MAX_VALUE, Float.MAX_VALUE), 0F);
    }
}
