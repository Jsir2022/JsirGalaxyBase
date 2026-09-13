package com.jsirgalaxybase.ui2.core;

import java.util.Arrays;

public final class UiComponents {
    private UiComponents() {}

    public static UiElement element(String type, String key, UiElement... children) {
        return UiElement.type(type).key(key).children(Arrays.asList(children)).build();
    }
    public static UiElement surface(String key, UiElement... children) { return element("Surface", key, children); }
    public static UiElement card(String key, UiElement... children) { return element("Card", key, children); }
    public static UiElement divider(String key) { return element("Divider", key); }
    public static UiElement label(String key, String text) { return UiElement.type("Label").key(key).prop("text", text).build(); }
    public static UiElement badge(String key, String text) { return UiElement.type("Badge").key(key).prop("text", text).build(); }
    public static UiElement button(String key, String text, boolean enabled) {
        return UiElement.type("Button").key(key).prop("text", text).prop("enabled", enabled).build();
    }
    public static UiElement iconButton(String key, String icon) { return UiElement.type("IconButton").key(key).prop("icon", icon).build(); }
    public static UiElement tabs(String key, UiElement... children) { return element("Tabs", key, children); }
    public static UiElement textField(String key, String value) { return UiElement.type("TextField").key(key).prop("value", value).build(); }
    public static UiElement select(String key, String value) { return UiElement.type("Select").key(key).prop("value", value).build(); }
    public static UiElement list(String key, UiElement... children) { return element("List", key, children); }
    public static UiElement dataTable(String key, UiElement... children) { return element("DataTable", key, children); }
    public static UiElement scrollView(String key, UiElement... children) { return element("ScrollView", key, children); }
    public static UiElement tooltip(String key, String text) { return UiElement.type("Tooltip").key(key).prop("text", text).build(); }
    public static UiElement contextMenu(String key, UiElement... children) { return element("ContextMenu", key, children); }
    public static UiElement dialog(String key, UiElement... children) { return element("Dialog", key, children); }
    public static UiElement toast(String key, String text) { return UiElement.type("Toast").key(key).prop("text", text).build(); }
    public static UiElement loading(String key) { return element("LoadingState", key); }
    public static UiElement empty(String key) { return element("EmptyState", key); }
    public static UiElement error(String key, String text) { return UiElement.type("ErrorState").key(key).prop("text", text).build(); }
}
