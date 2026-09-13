package com.jsirgalaxybase.ui2.input;

import java.util.ArrayList;
import java.util.List;

public final class InputDispatcher {
    private UiInputNode root;
    private UiInputNode modal;
    private UiInputNode pointerCapture;
    private UiInputNode focused;

    public InputDispatcher(UiInputNode root) { if (root == null) throw new IllegalArgumentException("root is required"); this.root = root; }
    public void setRoot(UiInputNode root) {
        if (root == null) throw new IllegalArgumentException("root is required");
        String focusedKey = focused == null ? null : focused.getKey();
        String captureKey = pointerCapture == null ? null : pointerCapture.getKey();
        this.root = root;
        if (modal == null) {
            focused = findByKey(root, focusedKey);
            pointerCapture = findByKey(root, captureKey);
        }
    }
    public void openModal(UiInputNode value) { modal = value; pointerCapture = null; focused = firstFocusable(value); }
    /** Reconciles a rebuilt modal tree without resetting focus on every dirty frame. */
    public void syncModal(UiInputNode value) {
        if (value == null) { if (modal != null) closeModal(); return; }
        if (modal == null || !modal.getKey().equals(value.getKey())) { openModal(value); return; }
        String focusedKey = focused == null ? null : focused.getKey();
        String captureKey = pointerCapture == null ? null : pointerCapture.getKey();
        modal = value;
        focused = findByKey(value, focusedKey);
        if (focused == null) focused = firstFocusable(value);
        pointerCapture = findByKey(value, captureKey);
    }
    public void closeModal() { modal = null; pointerCapture = null; focused = null; }
    public boolean hasModal() { return modal != null; }
    public UiInputNode getFocused() { return focused; }
    public UiInputNode getPointerCapture() { return pointerCapture; }

    public boolean dispatch(UiEvent event) {
        if (event == null) return false;
        if (event.getType() == UiEvent.Type.KEY_DOWN && event.getKeyCode() == UiKeyCode.ESCAPE && modal != null) {
            if (!dispatchToPath(pathTo(modal, focused == null ? modal : focused), event)) closeModal();
            return true;
        }
        if (event.getType() == UiEvent.Type.KEY_DOWN && event.getKeyCode() == UiKeyCode.TAB) {
            focusNext();
            return true;
        }
        if (event.getType() == UiEvent.Type.KEY_DOWN || event.getType() == UiEvent.Type.TEXT_INPUT) {
            return focused != null && dispatchToPath(pathTo(activeRoot(), focused), event);
        }
        if (pointerCapture != null && (event.getType() == UiEvent.Type.POINTER_MOVE || event.getType() == UiEvent.Type.POINTER_UP)) {
            boolean consumed = dispatchToPath(pathTo(activeRoot(), pointerCapture), event);
            if (event.getType() == UiEvent.Type.POINTER_UP) pointerCapture = null;
            return consumed;
        }
        List<UiInputNode> path = hitPath(activeRoot(), event.getX(), event.getY());
        if (path.isEmpty()) return modal != null;
        UiInputNode target = path.get(path.size() - 1);
        if (event.getType() == UiEvent.Type.POINTER_DOWN && target.isFocusable()) focused = target;
        return dispatchToPath(path, event);
    }

    private boolean dispatchToPath(List<UiInputNode> path, UiEvent event) {
        if (path.isEmpty()) return false;
        for (int i = 0; i < path.size() - 1; i++) {
            event.setPhase(UiEvent.Phase.CAPTURE);
            if (invoke(path.get(i), event)) return true;
        }
        event.setPhase(UiEvent.Phase.TARGET);
        if (invoke(path.get(path.size() - 1), event)) return true;
        for (int i = path.size() - 2; i >= 0; i--) {
            event.setPhase(UiEvent.Phase.BUBBLE);
            if (invoke(path.get(i), event)) return true;
        }
        return false;
    }

    private boolean invoke(UiInputNode node, UiEvent event) {
        if (node.getHandler() == null) return false;
        InputResult result = node.getHandler().handle(node, event);
        if (result == null) result = InputResult.PASS;
        if (result.shouldCapture()) pointerCapture = node;
        if (result.shouldRelease()) pointerCapture = null;
        return result.isConsumed();
    }

    private UiInputNode activeRoot() { return modal == null ? root : modal; }
    private void focusNext() {
        List<UiInputNode> candidates = new ArrayList<UiInputNode>();
        collectFocusable(activeRoot(), candidates);
        if (candidates.isEmpty()) { focused = null; return; }
        int index = focused == null ? -1 : candidates.indexOf(focused);
        focused = candidates.get((index + 1) % candidates.size());
    }

    private static void collectFocusable(UiInputNode node, List<UiInputNode> output) {
        if (node.isFocusable()) output.add(node);
        for (UiInputNode child : node.getChildren()) collectFocusable(child, output);
    }
    private static UiInputNode firstFocusable(UiInputNode root) {
        if (root == null) return null;
        List<UiInputNode> values = new ArrayList<UiInputNode>(); collectFocusable(root, values);
        return values.isEmpty() ? null : values.get(0);
    }

    private static List<UiInputNode> hitPath(UiInputNode node, int x, int y) {
        List<UiInputNode> empty = new ArrayList<UiInputNode>();
        if (node == null || !node.getBounds().contains(x, y)) return empty;
        List<UiInputNode> result = new ArrayList<UiInputNode>(); result.add(node);
        List<UiInputNode> children = node.getChildren();
        for (int i = children.size() - 1; i >= 0; i--) {
            List<UiInputNode> childPath = hitPath(children.get(i), x, y);
            if (!childPath.isEmpty()) { result.addAll(childPath); break; }
        }
        return result;
    }

    private static List<UiInputNode> pathTo(UiInputNode root, UiInputNode target) {
        List<UiInputNode> result = new ArrayList<UiInputNode>();
        if (findPath(root, target, result)) return result;
        result.clear();
        return result;
    }
    private static boolean findPath(UiInputNode node, UiInputNode target, List<UiInputNode> path) {
        if (node == null) return false;
        path.add(node);
        if (node == target) return true;
        for (UiInputNode child : node.getChildren()) if (findPath(child, target, path)) return true;
        path.remove(path.size() - 1);
        return false;
    }

    private static UiInputNode findByKey(UiInputNode node, String key) {
        if (node == null || key == null) return null;
        if (key.equals(node.getKey())) return node;
        for (UiInputNode child : node.getChildren()) {
            UiInputNode match = findByKey(child, key);
            if (match != null) return match;
        }
        return null;
    }
}
