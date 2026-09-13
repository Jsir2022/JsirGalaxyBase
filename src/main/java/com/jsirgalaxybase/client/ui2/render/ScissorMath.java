package com.jsirgalaxybase.client.ui2.render;

import com.jsirgalaxybase.ui2.geometry.UiRect;

public final class ScissorMath {
    private ScissorMath() {}
    public static UiRect toFramebuffer(UiRect logical, int scale, int displayHeight) {
        if (logical == null || scale <= 0 || displayHeight < 0) throw new IllegalArgumentException("invalid scissor input");
        long x = (long) logical.getX() * scale;
        long y = (long) displayHeight - (long) logical.getBottom() * scale;
        long width = (long) logical.getWidth() * scale;
        long height = (long) logical.getHeight() * scale;
        return new UiRect(safe(x), safe(y), nonNegative(width), nonNegative(height));
    }
    public static UiRect intersect(UiRect left, UiRect right) {
        int x = Math.max(left.getX(), right.getX()), y = Math.max(left.getY(), right.getY());
        int edgeX = Math.min(left.getRight(), right.getRight()), edgeY = Math.min(left.getBottom(), right.getBottom());
        return new UiRect(x, y, Math.max(0, edgeX - x), Math.max(0, edgeY - y));
    }
    private static int safe(long value) { return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) value; }
    private static int nonNegative(long value) { return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0, value); }
}
