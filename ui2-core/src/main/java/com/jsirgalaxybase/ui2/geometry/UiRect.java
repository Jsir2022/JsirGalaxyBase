package com.jsirgalaxybase.ui2.geometry;

import java.util.Objects;

public final class UiRect {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public UiRect(int x, int y, int width, int height) {
        if (width < 0 || height < 0) throw new IllegalArgumentException("rect size must be non-negative");
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getRight() { return saturatedAdd(x, width); }
    public int getBottom() { return saturatedAdd(y, height); }
    public boolean contains(int px, int py) { return px >= x && py >= y && px < getRight() && py < getBottom(); }
    public boolean contains(UiRect other) {
        return other.x >= x && other.y >= y && other.getRight() <= getRight() && other.getBottom() <= getBottom();
    }

    private static int saturatedAdd(int left, int right) {
        long value = (long) left + right;
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) value;
    }

    @Override public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof UiRect)) return false;
        UiRect other = (UiRect) value;
        return x == other.x && y == other.y && width == other.width && height == other.height;
    }

    @Override public int hashCode() { return Objects.hash(x, y, width, height); }
    @Override public String toString() { return x + "," + y + " " + width + "x" + height; }
}
