package com.jsirgalaxybase.terminal.client.viewmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Client presentation model for the current player's bounded notification index. */
public final class TerminalNotificationCenterModel {

    private final String serviceState;
    private final List<EntryModel> entries;
    private final int retainedEntries;

    public TerminalNotificationCenterModel(String serviceState, List<EntryModel> entries, int retainedEntries) {
        this.serviceState = text(serviceState, "通知中心已就绪");
        this.entries = Collections.unmodifiableList(new ArrayList<EntryModel>(entries == null
            ? Collections.<EntryModel>emptyList() : entries));
        this.retainedEntries = Math.max(this.entries.size(), retainedEntries);
    }

    public static TerminalNotificationCenterModel empty() {
        return new TerminalNotificationCenterModel("当前没有可显示的重要通知。", Collections.<EntryModel>emptyList(), 0);
    }
    public String getServiceState() { return serviceState; }
    public List<EntryModel> getEntries() { return entries; }
    public int getRetainedEntries() { return retainedEntries; }

    public static final class EntryModel {
        private final String sourceId, targetPageId, targetRecordId, title, body, severityName;
        private final int occurrences;
        public EntryModel(String sourceId, String targetPageId, String targetRecordId, String title, String body,
            String severityName, int occurrences) {
            this.sourceId = text(sourceId, "terminal"); this.targetPageId = text(targetPageId, "");
            this.targetRecordId = text(targetRecordId, ""); this.title = text(title, "终端通知");
            this.body = text(body, "当前没有通知内容。"); this.severityName = text(severityName, "INFO");
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
    private static String text(String value, String fallback) { return value == null || value.trim().isEmpty() ? fallback : value.trim(); }
}
