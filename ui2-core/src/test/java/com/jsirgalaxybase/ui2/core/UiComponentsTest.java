package com.jsirgalaxybase.ui2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class UiComponentsTest {
    @Test public void firstComponentCatalogUsesStableTypedElements() {
        List<UiElement> elements = Arrays.asList(
            UiComponents.surface("surface"), UiComponents.card("card"), UiComponents.divider("divider"),
            UiComponents.label("label", "名称"), UiComponents.badge("badge", "成功"),
            UiComponents.button("button", "确认", true), UiComponents.iconButton("icon", "search"),
            UiComponents.tabs("tabs"), UiComponents.textField("field", "value"), UiComponents.select("select", "all"),
            UiComponents.list("list"), UiComponents.dataTable("table"), UiComponents.scrollView("scroll"),
            UiComponents.tooltip("tooltip", "help"), UiComponents.contextMenu("menu"), UiComponents.dialog("dialog"),
            UiComponents.toast("toast", "saved"), UiComponents.loading("loading"), UiComponents.empty("empty"),
            UiComponents.error("error", "failed"));
        assertEquals(20, elements.size());
        for (UiElement element : elements) assertFalse(element.getType().isEmpty());
        assertEquals(Boolean.TRUE, elements.get(5).getProp("enabled"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void elementPropsAreImmutable() {
        UiComponents.label("label", "value").getProps().put("text", "changed");
    }
}
