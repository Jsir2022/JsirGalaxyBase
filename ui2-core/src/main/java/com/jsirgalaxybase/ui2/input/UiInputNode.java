package com.jsirgalaxybase.ui2.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.ui2.geometry.UiRect;

public final class UiInputNode {
    private final String key;
    private final UiRect bounds;
    private final boolean focusable;
    private final UiInputHandler handler;
    private final List<UiInputNode> children = new ArrayList<UiInputNode>();

    public UiInputNode(String key, UiRect bounds, boolean focusable, UiInputHandler handler) {
        if (key == null || bounds == null) throw new IllegalArgumentException("key and bounds are required");
        this.key = key; this.bounds = bounds; this.focusable = focusable; this.handler = handler;
    }
    public UiInputNode add(UiInputNode child) { if (child != null) children.add(child); return this; }
    public String getKey() { return key; }
    public UiRect getBounds() { return bounds; }
    public boolean isFocusable() { return focusable; }
    public UiInputHandler getHandler() { return handler; }
    public List<UiInputNode> getChildren() { return Collections.unmodifiableList(children); }
}
