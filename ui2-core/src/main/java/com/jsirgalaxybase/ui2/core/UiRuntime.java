package com.jsirgalaxybase.ui2.core;

import java.util.Map;

import com.jsirgalaxybase.ui2.geometry.Constraints;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.geometry.UiSize;
import com.jsirgalaxybase.ui2.input.InputDispatcher;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.input.UiInputNode;
import com.jsirgalaxybase.ui2.layout.LayoutEngine;
import com.jsirgalaxybase.ui2.layout.LayoutResult;
import com.jsirgalaxybase.ui2.layout.LayoutSpec;
import com.jsirgalaxybase.ui2.render.DrawList;

/** Owns one component tree and batches state invalidation into the next frame. */
public final class UiRuntime implements AutoCloseable {
    private final UiDocument document;
    private final UiContext context;
    private final UiReconciler reconciler = new UiReconciler();
    private final LayoutEngine layoutEngine = new LayoutEngine();
    private UiNode root;
    private InputDispatcher input;
    private DrawList drawList = new DrawList();
    private UiSize viewport = UiSize.ZERO;
    private boolean dirty = true;
    private boolean closed;

    public UiRuntime(UiDocument document, UiContext context) {
        if (document == null || context == null) throw new IllegalArgumentException("document and context are required");
        this.document = document;
        this.context = context;
    }

    public void setViewport(int width, int height) {
        ensureOpen();
        UiSize next = new UiSize(Math.max(0, width), Math.max(0, height));
        if (!next.equals(viewport)) { viewport = next; dirty = true; }
    }

    public void invalidate() { ensureOpen(); dirty = true; }

    public DrawList frame() {
        ensureOpen();
        if (!dirty) return drawList;
        UiElement element = document.build(context);
        root = reconciler.reconcile(root, element);
        LayoutSpec spec = document.layout(element, viewport, context);
        layoutEngine.measure(spec, Constraints.tight(viewport.getWidth(), viewport.getHeight()));
        LayoutResult result = layoutEngine.layout(spec, new UiRect(0, 0, viewport.getWidth(), viewport.getHeight()));
        applyBounds(root, result.asMap());
        UiInputNode inputRoot = document.input(root, context);
        if (inputRoot == null) inputRoot = new UiInputNode("root", root.getBounds(), false, null);
        if (input == null) input = new InputDispatcher(inputRoot); else input.setRoot(inputRoot);
        input.syncModal(document.modalInput(root, context));
        drawList = document.paint(root, context);
        if (drawList == null) throw new IllegalStateException("document returned no draw list");
        drawList.validateBalanced();
        dirty = false;
        return drawList;
    }

    public boolean dispatch(UiEvent event) {
        ensureOpen();
        frame();
        boolean consumed = input.dispatch(event);
        // Input handlers are allowed to mutate document/store state. A consumed event is
        // therefore an invalidation boundary; otherwise a closed modal can remain cached
        // forever and keep intercepting Escape and pointer input.
        if (consumed) dirty = true;
        return consumed;
    }
    public boolean hasModal() { return input != null && input.hasModal(); }
    public UiNode getRoot() { return root; }
    public UiSize getViewport() { return viewport; }

    private static void applyBounds(UiNode node, Map<String, UiRect> bounds) {
        if (node == null) return;
        if (node.getElement().getKey() != null) {
            UiRect value = bounds.get(node.getElement().getKey().getValue());
            if (value != null) node.setBounds(value);
        }
        for (UiNode child : node.getChildren()) applyBounds(child, bounds);
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        if (root != null) root.unmount();
        root = null; input = null; drawList = new DrawList();
    }

    private void ensureOpen() { if (closed) throw new IllegalStateException("runtime is closed"); }
}
