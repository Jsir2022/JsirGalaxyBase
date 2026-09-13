package com.jsirgalaxybase.ui2.geometry;

public final class Constraints {
    private final int minWidth;
    private final int maxWidth;
    private final int minHeight;
    private final int maxHeight;

    public Constraints(int minWidth, int maxWidth, int minHeight, int maxHeight) {
        if (minWidth < 0 || minHeight < 0 || maxWidth < minWidth || maxHeight < minHeight) {
            throw new IllegalArgumentException("invalid constraints");
        }
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
    }

    public static Constraints tight(int width, int height) { return new Constraints(width, width, height, height); }
    public int getMinWidth() { return minWidth; }
    public int getMaxWidth() { return maxWidth; }
    public int getMinHeight() { return minHeight; }
    public int getMaxHeight() { return maxHeight; }
    public UiSize constrain(UiSize value) {
        return new UiSize(clamp(value.getWidth(), minWidth, maxWidth), clamp(value.getHeight(), minHeight, maxHeight));
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
