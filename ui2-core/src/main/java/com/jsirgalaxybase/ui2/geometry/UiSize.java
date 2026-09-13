package com.jsirgalaxybase.ui2.geometry;

import java.util.Objects;

public final class UiSize {
    public static final UiSize ZERO = new UiSize(0, 0);
    private final int width;
    private final int height;

    public UiSize(int width, int height) {
        if (width < 0 || height < 0) throw new IllegalArgumentException("size must be non-negative");
        this.width = width;
        this.height = height;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    @Override public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof UiSize)) return false;
        UiSize other = (UiSize) value;
        return width == other.width && height == other.height;
    }

    @Override public int hashCode() { return Objects.hash(width, height); }
    @Override public String toString() { return width + "x" + height; }
}
