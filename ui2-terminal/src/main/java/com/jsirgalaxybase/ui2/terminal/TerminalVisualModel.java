package com.jsirgalaxybase.ui2.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable terminal chrome model with no Minecraft or network types. */
public final class TerminalVisualModel {
    private final String title;
    private final String selectedPageId;
    private final List<NavItem> navigation;

    public TerminalVisualModel(String title, String selectedPageId, List<NavItem> navigation) {
        this.title = text(title, "银河终端");
        this.selectedPageId = text(selectedPageId, "home");
        this.navigation = Collections.unmodifiableList(new ArrayList<NavItem>(navigation == null
            ? Collections.<NavItem>emptyList() : navigation));
    }
    private static String text(String value, String fallback) { return value == null || value.trim().isEmpty() ? fallback : value.trim(); }
    public String getTitle() { return title; }
    public String getSelectedPageId() { return selectedPageId; }
    public List<NavItem> getNavigation() { return navigation; }

    public static final class NavItem {
        private final String pageId, label;
        private final boolean enabled, selected;
        public NavItem(String pageId, String label, boolean enabled, boolean selected) {
            this.pageId = text(pageId, "home"); this.label = text(label, pageId);
            this.enabled = enabled; this.selected = selected;
        }
        public String getPageId() { return pageId; }
        public String getLabel() { return label; }
        public boolean isEnabled() { return enabled; }
        public boolean isSelected() { return selected; }
    }
}
