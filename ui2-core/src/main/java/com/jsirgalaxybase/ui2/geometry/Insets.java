package com.jsirgalaxybase.ui2.geometry;

public final class Insets {
    public static final Insets ZERO = new Insets(0, 0, 0, 0);
    private final int top;
    private final int right;
    private final int bottom;
    private final int left;

    public Insets(int top, int right, int bottom, int left) {
        if (top < 0 || right < 0 || bottom < 0 || left < 0) {
            throw new IllegalArgumentException("insets must be non-negative");
        }
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    public static Insets all(int value) { return new Insets(value, value, value, value); }
    public int getTop() { return top; }
    public int getRight() { return right; }
    public int getBottom() { return bottom; }
    public int getLeft() { return left; }
    public int horizontal() { return left + right; }
    public int vertical() { return top + bottom; }
}
