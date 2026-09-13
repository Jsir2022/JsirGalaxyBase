package com.jsirgalaxybase.ui2.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.input.UiInputNode;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.render.DrawList;

/** Renders and wires a retained component tree through registered widget behavior. */
public final class UiWidgetRegistry {
    private final Map<String, UiWidgetAdapter> adapters = new LinkedHashMap<String, UiWidgetAdapter>();

    public UiWidgetRegistry register(UiWidgetAdapter adapter) {
        if (adapter == null || adapter.type() == null || adapter.type().trim().isEmpty()) {
            throw new IllegalArgumentException("widget adapter and type are required");
        }
        adapters.put(adapter.type(), adapter);
        return this;
    }

    public boolean supports(String type) { return adapters.containsKey(type); }

    public DrawList paint(UiNode root, UiContext context) {
        List<DrawCommand> commands = new ArrayList<DrawCommand>();
        paintInto(root, context, commands);
        Collections.sort(commands, new Comparator<DrawCommand>() {
            @Override public int compare(DrawCommand left, DrawCommand right) {
                return Integer.compare(left.getLayer(), right.getLayer());
            }
        });
        DrawList result = new DrawList();
        for (DrawCommand command : commands) result.add(command);
        return result;
    }

    public UiInputNode input(UiNode root, UiContext context) {
        if (root == null) return null;
        UiInputNode result = inputFor(root, context);
        return result == null ? new UiInputNode("root", root.getBounds(), false, null) : result;
    }

    private void paintInto(UiNode node, UiContext context, List<DrawCommand> target) {
        if (node == null) return;
        UiWidgetAdapter adapter = adapters.get(node.getElement().getType());
        if (adapter != null) {
            DrawList local = new DrawList();
            adapter.paint(node, context, local);
            target.addAll(local.ordered());
        }
        for (UiNode child : node.getChildren()) paintInto(child, context, target);
    }

    private UiInputNode inputFor(UiNode node, UiContext context) {
        UiWidgetAdapter adapter = adapters.get(node.getElement().getType());
        UiInputNode result = adapter == null ? null : adapter.input(node, context);
        if (result == null) result = new UiInputNode(key(node), node.getBounds(), false, null);
        for (UiNode child : node.getChildren()) result.add(inputFor(child, context));
        return result;
    }

    private static String key(UiNode node) {
        return node.getElement().getKey() == null ? node.getElement().getType()
            : node.getElement().getKey().getValue();
    }
}
