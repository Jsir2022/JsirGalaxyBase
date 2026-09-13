package com.jsirgalaxybase.ui2.component;

import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiDocument;
import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.input.UiInputNode;
import com.jsirgalaxybase.ui2.render.DrawList;

/** Base document whose component tree owns painting and input through a widget registry. */
public abstract class ComponentUiDocument implements UiDocument {
    private final UiWidgetRegistry widgets;

    protected ComponentUiDocument(UiWidgetRegistry widgets) {
        if (widgets == null) throw new IllegalArgumentException("widget registry is required");
        this.widgets = widgets;
    }

    protected final UiWidgetRegistry widgets() { return widgets; }
    @Override public DrawList paint(UiNode root, UiContext context) { return widgets.paint(root, context); }
    @Override public UiInputNode input(UiNode root, UiContext context) { return widgets.input(root, context); }
}
