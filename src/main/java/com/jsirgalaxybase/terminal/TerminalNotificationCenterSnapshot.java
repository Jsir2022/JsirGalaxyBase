package com.jsirgalaxybase.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only, player-scoped terminal notification index. Business records remain owned by their source modules.
 */
public final class TerminalNotificationCenterSnapshot {

    private final String serviceState;
    private final List<Entry> entries;
    private final int retainedEntries;

    public TerminalNotificationCenterSnapshot(String serviceState, List<Entry> entries, int retainedEntries) {
        this.serviceState = text(serviceState, "通知中心已就绪");
        this.entries = Collections.unmodifiableList(new ArrayList<Entry>(entries == null
            ? Collections.<Entry>emptyList() : entries));
        this.retainedEntries = Math.max(this.entries.size(), retainedEntries);
    }

    public static TerminalNotificationCenterSnapshot empty() {
        return new TerminalNotificationCenterSnapshot("当前没有可显示的重要通知。", Collections.<Entry>emptyList(), 0);
    }

    public String getServiceState() { return serviceState; }
    public List<Entry> getEntries() { return entries; }
    public int getRetainedEntries() { return retainedEntries; }

    public static final class Entry {
        private final String sourceId;
        private final String targetPageId;
        private final String targetRecordId;
        private final String title;
        private final String body;
        private final String severityName;
        private final int occurrences;

        public Entry(String sourceId, String targetPageId, String targetRecordId, String title, String body,
            String severityName, int occurrences) {
            this.sourceId = text(sourceId, "terminal");
            this.targetPageId = text(targetPageId, "");
            this.targetRecordId = text(targetRecordId, "");
            this.title = text(title, "终端通知");
            this.body = text(body, "当前没有通知内容。");
            this.severityName = text(severityName, "INFO");
            this.occurrences = Math.max(1, occurrences);
        }

        public String getSourceId() { return sourceId; }
        public String getTargetPageId() { return targetPageId; }
        public String getTargetRecordId() { return targetRecordId; }
        public String getTitle() { return title; }
        public String getBody() { return body; }
        public String getSeverityName() { return severityName; }
        public int getOccurrences() { return occurrences; }
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
