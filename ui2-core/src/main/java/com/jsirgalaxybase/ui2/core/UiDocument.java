package com.jsirgalaxybase.ui2.core;

import com.jsirgalaxybase.ui2.geometry.UiSize;
import com.jsirgalaxybase.ui2.input.UiInputNode;
import com.jsirgalaxybase.ui2.layout.LayoutSpec;
import com.jsirgalaxybase.ui2.render.DrawList;

/** A platform-neutral screen description consumed by UiRuntime. */
public interface UiDocument {
    UiElement build(UiContext context);
    LayoutSpec layout(UiElement root, UiSize viewport, UiContext context);
    DrawList paint(UiNode root, UiContext context);
    UiInputNode input(UiNode root, UiContext context);
    default UiInputNode modalInput(UiNode root, UiContext context) { return null; }
}
