package com.jsirgalaxybase.ui2.component;

import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.input.UiInputNode;
import com.jsirgalaxybase.ui2.render.DrawList;

public interface UiWidgetAdapter {
    String type();
    void paint(UiNode node, UiContext context, DrawList target);
    UiInputNode input(UiNode node, UiContext context);
}
