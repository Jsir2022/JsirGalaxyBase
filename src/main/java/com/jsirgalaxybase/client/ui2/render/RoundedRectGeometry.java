// SPDX-License-Identifier: LGPL-3.0-or-later
// Derived from Qz-UILib 4.8.0
// src/main/java/club/heiqi/uilib/ui/render/UiRoundedRectGeometry.java at
// 7937cd042910c8182d899d41aa5692d1c8b4a98a; adapted into a platform-neutral vertex generator.
package com.jsirgalaxybase.client.ui2.render;

/** Deterministic rounded-rectangle geometry shared by the GL renderer and unit tests. */
public final class RoundedRectGeometry {
    public static final class CornerRadii {
        private final float topLeft, topRight, bottomRight, bottomLeft;
        public CornerRadii(float topLeft, float topRight, float bottomRight, float bottomLeft) {
            this.topLeft = Math.max(0F, topLeft); this.topRight = Math.max(0F, topRight);
            this.bottomRight = Math.max(0F, bottomRight); this.bottomLeft = Math.max(0F, bottomLeft);
        }
        public float getTopLeft() { return topLeft; }
        public float getTopRight() { return topRight; }
        public float getBottomRight() { return bottomRight; }
        public float getBottomLeft() { return bottomLeft; }
    }
    public static final class Point {
        private final float x;
        private final float y;
        Point(float x, float y) { this.x = x; this.y = y; }
        public float getX() { return x; }
        public float getY() { return y; }
    }

    private RoundedRectGeometry() {}

    public static float clampRadius(float width, float height, float radius) {
        return Math.max(0F, Math.min(radius,
            Math.min(Math.max(0F, width), Math.max(0F, height)) / 2F));
    }

    public static int segments(float radius, float density) {
        float physicalRadius = radius * Math.max(1F, density);
        return Math.max(4, Math.min(18, Math.round(physicalRadius * 0.75F)));
    }

    /** Half of the one-physical-pixel coverage band in logical GUI coordinates. */
    public static float coverageInset(float density) {
        return 0.5F / Math.max(1F, density);
    }

    /** A one-physical-pixel stroke in logical GUI coordinates. */
    public static float hairline(float density) {
        return 1F / Math.max(1F, density);
    }

    /** Proportionally limits independent corners so adjacent radii never overlap. */
    public static CornerRadii clampRadii(float width, float height, CornerRadii radii) {
        if (radii == null) return new CornerRadii(0F, 0F, 0F, 0F);
        float safeWidth = Math.max(0F, width), safeHeight = Math.max(0F, height);
        float scale = 1F;
        scale = limit(scale, safeWidth, radii.topLeft + radii.topRight);
        scale = limit(scale, safeWidth, radii.bottomLeft + radii.bottomRight);
        scale = limit(scale, safeHeight, radii.topLeft + radii.bottomLeft);
        scale = limit(scale, safeHeight, radii.topRight + radii.bottomRight);
        return new CornerRadii(radii.topLeft * scale, radii.topRight * scale,
            radii.bottomRight * scale, radii.bottomLeft * scale);
    }

    private static float limit(float current, float extent, float sum) {
        return sum <= 0F ? current : Math.min(current, extent / sum);
    }

    public static Point[] outline(float left, float top, float right, float bottom, float radius,
        int segments) {
        float safeRadius = clampRadius(right - left, bottom - top, radius);
        int safeSegments = Math.max(1, segments);
        if (safeRadius <= 0F) {
            return new Point[] { new Point(left, top), new Point(right, top),
                new Point(right, bottom), new Point(left, bottom) };
        }
        Point[] points = new Point[(safeSegments + 1) * 4];
        int cursor = 0;
        cursor = corner(points, cursor, right - safeRadius, top + safeRadius, safeRadius,
            270D, 360D, safeSegments);
        cursor = corner(points, cursor, right - safeRadius, bottom - safeRadius, safeRadius,
            0D, 90D, safeSegments);
        cursor = corner(points, cursor, left + safeRadius, bottom - safeRadius, safeRadius,
            90D, 180D, safeSegments);
        corner(points, cursor, left + safeRadius, top + safeRadius, safeRadius,
            180D, 270D, safeSegments);
        return points;
    }

    private static int corner(Point[] output, int cursor, float centerX, float centerY,
        float radius, double start, double end, int segments) {
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(start + (end - start) * i / (double) segments);
            output[cursor++] = new Point(centerX + (float) Math.cos(angle) * radius,
                centerY + (float) Math.sin(angle) * radius);
        }
        return cursor;
    }
}
