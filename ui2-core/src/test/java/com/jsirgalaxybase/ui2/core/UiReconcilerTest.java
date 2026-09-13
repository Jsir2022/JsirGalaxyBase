package com.jsirgalaxybase.ui2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class UiReconcilerTest {
    @Test public void stableTypeAndKeyPreserveNodeAndLocalState() {
        UiReconciler reconciler = new UiReconciler();
        UiNode first = reconciler.reconcile(null, UiElement.type("Button").key("save").prop("text", "Save").build());
        first.getLocalState().put("focused", Boolean.TRUE);
        UiNode second = reconciler.reconcile(first, UiElement.type("Button").key("save").prop("text", "保存").build());
        assertSame(first, second);
        assertEquals(Boolean.TRUE, second.getLocalState().get("focused"));
        assertEquals("保存", second.getElement().getProp("text"));
    }

    @Test public void changedKeyUnmountsOldNode() {
        UiReconciler reconciler = new UiReconciler();
        UiNode first = reconciler.reconcile(null, UiElement.type("Row").key("order-1").build());
        long firstId = first.getInstanceId();
        UiNode second = reconciler.reconcile(first, UiElement.type("Row").key("order-2").build());
        assertFalse(first.isMounted());
        assertNotEquals(firstId, second.getInstanceId());
    }

    @Test public void keyedChildrenKeepIdentityWhenReordered() {
        UiReconciler reconciler = new UiReconciler();
        UiElement firstTree = UiElement.type("List").key("list")
            .child(UiComponents.label("a", "A")).child(UiComponents.label("b", "B")).build();
        UiNode first = reconciler.reconcile(null, firstTree);
        long a = first.getChildren().get(0).getInstanceId();
        long b = first.getChildren().get(1).getInstanceId();
        UiElement secondTree = UiElement.type("List").key("list")
            .child(UiComponents.label("b", "B2")).child(UiComponents.label("a", "A2")).build();
        UiNode second = reconciler.reconcile(first, secondTree);
        assertEquals(b, second.getChildren().get(0).getInstanceId());
        assertEquals(a, second.getChildren().get(1).getInstanceId());
    }
}
