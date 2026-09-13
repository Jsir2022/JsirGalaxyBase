package com.jsirgalaxybase.ui2.text;

import java.util.Objects;

public final class TextStyle {
    public enum Align { LEFT, CENTER, RIGHT }

    public static final TextStyle BODY = new TextStyle("body", 11, false, 14, Align.LEFT);

    private final String token;
    private final int size;
    private final boolean bold;
    private final int lineHeight;
    private final Align align;

    public TextStyle(String token, int size, boolean bold, int lineHeight, Align align) {
        if (token == null || token.trim().isEmpty() || size <= 0 || lineHeight < size || align == null) {
            throw new IllegalArgumentException("invalid text style");
        }
        this.token = token;
        this.size = size;
        this.bold = bold;
        this.lineHeight = lineHeight;
        this.align = align;
    }

    public String getToken() { return token; }
    public int getSize() { return size; }
    public boolean isBold() { return bold; }
    public int getLineHeight() { return lineHeight; }
    public Align getAlign() { return align; }

    @Override public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof TextStyle)) return false;
        TextStyle other = (TextStyle) value;
        return size == other.size && bold == other.bold && lineHeight == other.lineHeight
            && token.equals(other.token) && align == other.align;
    }

    @Override public int hashCode() { return Objects.hash(token, size, bold, lineHeight, align); }
}
