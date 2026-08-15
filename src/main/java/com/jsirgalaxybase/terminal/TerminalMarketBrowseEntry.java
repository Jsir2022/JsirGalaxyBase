package com.jsirgalaxybase.terminal;

/**
 * Server-authoritative browse row shared by the three market front ends.
 * Values are presentation summaries only; every action is revalidated against
 * its respective market service.
 */
public final class TerminalMarketBrowseEntry {

    private final String key;
    private final String itemIdentity;
    private final String title;
    private final String subtitle;
    private final String primaryValue;
    private final String status;

    public TerminalMarketBrowseEntry(String key, String itemIdentity, String title, String subtitle,
        String primaryValue, String status) {
        this.key = text(key);
        this.itemIdentity = text(itemIdentity);
        this.title = text(title);
        this.subtitle = text(subtitle);
        this.primaryValue = text(primaryValue);
        this.status = text(status);
    }

    public String getKey() { return key; }
    public String getItemIdentity() { return itemIdentity; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getPrimaryValue() { return primaryValue; }
    public String getStatus() { return status; }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
