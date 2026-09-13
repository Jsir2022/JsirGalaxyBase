package com.jsirgalaxybase.ui2.core;

import java.util.ArrayList;
import java.util.List;

public final class UiReconciler {
    public UiNode reconcile(UiNode previous, UiElement next) {
        if (next == null) throw new IllegalArgumentException("next element is required");
        UiNode node;
        if (previous != null && sameIdentity(previous.getElement(), next)) {
            node = previous;
            node.setElement(next);
        } else {
            if (previous != null) previous.unmount();
            node = new UiNode(next);
        }
        reconcileChildren(node, next.getChildren());
        return node;
    }

    private void reconcileChildren(UiNode parent, List<UiElement> nextElements) {
        List<UiNode> old = new ArrayList<UiNode>(parent.getChildren());
        boolean[] used = new boolean[old.size()];

        List<UiNode> next = new ArrayList<UiNode>();
        for (UiElement element : nextElements) {
            UiNode match = null;
            for (int i = 0; i < old.size(); i++) {
                if (!used[i] && sameIdentity(old.get(i).getElement(), element)) {
                    used[i] = true;
                    match = old.get(i);
                    break;
                }
            }
            next.add(reconcile(match, element));
        }
        for (int i = 0; i < old.size(); i++) if (!used[i]) old.get(i).unmount();
        parent.getChildren().clear();
        parent.getChildren().addAll(next);
    }

    private static boolean sameIdentity(UiElement left, UiElement right) { return identity(left).equals(identity(right)); }
    private static String identity(UiElement element) {
        return element.getType() + "\u0000" + (element.getKey() == null ? "" : element.getKey().getValue());
    }
}
