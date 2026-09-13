package com.jsirgalaxybase.client.ui2.font;

/** Saturating arithmetic for measurements supplied by resource-pack font renderers. */
final class FontMeasurementBounds {
    private FontMeasurementBounds() {}

    static int width(int rawWidth, float scale, int maxWidth) {
        int limit = Math.max(0, maxWidth);
        if (rawWidth <= 0 || scale <= 0F || Float.isNaN(scale)) return 0;
        double scaled = rawWidth * (double) scale;
        if (Double.isNaN(scaled) || scaled <= 0D) return 0;
        if (Double.isInfinite(scaled) || scaled >= limit) return limit;
        return Math.max(0, Math.min(limit, (int) Math.ceil(scaled)));
    }

    static float drawWidth(int rawWidth, float scale) {
        if (rawWidth <= 0 || scale <= 0F || Float.isNaN(scale)) return 0F;
        double scaled = rawWidth * (double) scale;
        if (Double.isNaN(scaled) || scaled <= 0D) return 0F;
        return scaled >= Float.MAX_VALUE ? Float.MAX_VALUE : (float) scaled;
    }
}
