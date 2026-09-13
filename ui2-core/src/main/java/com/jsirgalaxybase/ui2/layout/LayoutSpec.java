package com.jsirgalaxybase.ui2.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.ui2.geometry.Insets;
import com.jsirgalaxybase.ui2.geometry.UiSize;

public final class LayoutSpec {
    private final String key;
    private final LayoutKind kind;
    private final UiSize preferredSize;
    private final float flex;
    private final int gap;
    private final int columns;
    private final Insets padding;
    private final List<LayoutSpec> children;

    private LayoutSpec(Builder builder) {
        this.key = builder.key;
        this.kind = builder.kind;
        this.preferredSize = builder.preferredSize;
        this.flex = builder.flex;
        this.gap = builder.gap;
        this.columns = builder.columns;
        this.padding = builder.padding;
        this.children = Collections.unmodifiableList(new ArrayList<LayoutSpec>(builder.children));
    }

    public static Builder of(String key, LayoutKind kind) { return new Builder(key, kind); }
    public String getKey() { return key; }
    public LayoutKind getKind() { return kind; }
    public UiSize getPreferredSize() { return preferredSize; }
    public float getFlex() { return flex; }
    public int getGap() { return gap; }
    public int getColumns() { return columns; }
    public Insets getPadding() { return padding; }
    public List<LayoutSpec> getChildren() { return children; }

    public static final class Builder {
        private final String key;
        private final LayoutKind kind;
        private UiSize preferredSize = UiSize.ZERO;
        private float flex;
        private int gap;
        private int columns = 1;
        private Insets padding = Insets.ZERO;
        private final List<LayoutSpec> children = new ArrayList<LayoutSpec>();

        private Builder(String key, LayoutKind kind) {
            if (key == null || key.trim().isEmpty() || kind == null) throw new IllegalArgumentException("key and kind are required");
            this.key = key;
            this.kind = kind;
        }
        public Builder preferred(int width, int height) { preferredSize = new UiSize(width, height); return this; }
        public Builder flex(float value) { if (value < 0F) throw new IllegalArgumentException("flex must be non-negative"); flex = value; return this; }
        public Builder gap(int value) { if (value < 0) throw new IllegalArgumentException("gap must be non-negative"); gap = value; return this; }
        public Builder columns(int value) { if (value < 1) throw new IllegalArgumentException("columns must be positive"); columns = value; return this; }
        public Builder padding(Insets value) { padding = value == null ? Insets.ZERO : value; return this; }
        public Builder child(LayoutSpec value) { if (value != null) children.add(value); return this; }
        public LayoutSpec build() { return new LayoutSpec(this); }
    }
}
