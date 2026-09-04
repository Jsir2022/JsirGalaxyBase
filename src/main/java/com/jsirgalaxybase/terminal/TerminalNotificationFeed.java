package com.jsirgalaxybase.terminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure, bounded notification feed shared by terminal snapshot producers. */
public final class TerminalNotificationFeed {

    public static final int MAX_ENTRIES = 40;
    public static final int MAX_PAGE_SIZE = 12;
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<String, Entry>();

    public synchronized void record(Entry entry) {
        if (entry == null) return;
        Entry existing = entries.remove(entry.getDeduplicationKey());
        entries.put(entry.getDeduplicationKey(), existing == null ? entry : existing.merge(entry));
        while (entries.size() > MAX_ENTRIES) entries.remove(entries.keySet().iterator().next());
    }

    public synchronized Page page(int requestedPage, int requestedPageSize) {
        int pageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, requestedPageSize));
        List<Entry> newestFirst = new ArrayList<Entry>(entries.values());
        Collections.reverse(newestFirst);
        int total = newestFirst.size();
        int pages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
        int page = pages == 0 ? 0 : Math.min(Math.max(0, requestedPage), pages - 1);
        int start = Math.min(total, page * pageSize);
        int end = Math.min(total, start + pageSize);
        return new Page(newestFirst.subList(start, end), page, pages, total, pageSize);
    }

    public synchronized List<Entry> entriesNewestFirst() {
        List<Entry> newestFirst = new ArrayList<Entry>(entries.values());
        Collections.reverse(newestFirst);
        return Collections.unmodifiableList(newestFirst);
    }

    public static final class Entry {
        private final String deduplicationKey;
        private final String sourceId;
        private final String targetPageId;
        private final String targetRecordId;
        private final String title;
        private final String body;
        private final String severityName;
        private final int occurrences;

        public Entry(String deduplicationKey, String sourceId, String targetPageId, String targetRecordId,
            String title, String body, String severityName) {
            this(deduplicationKey, sourceId, targetPageId, targetRecordId, title, body, severityName, 1);
        }

        private Entry(String key, String source, String page, String record, String title, String body,
            String severity, int occurrences) {
            this.deduplicationKey = text(key, "terminal"); this.sourceId = text(source, "terminal");
            this.targetPageId = text(page, ""); this.targetRecordId = text(record, "");
            this.title = text(title, "终端通知"); this.body = text(body, "当前没有通知内容。");
            this.severityName = text(severity, "INFO"); this.occurrences = Math.max(1, occurrences);
        }
        private Entry merge(Entry latest) {
            return new Entry(latest.deduplicationKey, latest.sourceId, latest.targetPageId, latest.targetRecordId,
                latest.title, latest.body, latest.severityName, occurrences + 1);
        }
        public String getDeduplicationKey() { return deduplicationKey; }
        public String getSourceId() { return sourceId; }
        public String getTargetPageId() { return targetPageId; }
        public String getTargetRecordId() { return targetRecordId; }
        public String getTitle() { return title; }
        public String getBody() { return body; }
        public String getSeverityName() { return severityName; }
        public int getOccurrences() { return occurrences; }
        private static String text(String value, String fallback) { return value == null || value.trim().isEmpty() ? fallback : value.trim(); }
    }

    public static final class Page {
        private final List<Entry> entries; private final int pageIndex; private final int totalPages; private final int totalEntries; private final int pageSize;
        private Page(List<Entry> entries, int pageIndex, int totalPages, int totalEntries, int pageSize) {
            this.entries = Collections.unmodifiableList(new ArrayList<Entry>(entries)); this.pageIndex = pageIndex;
            this.totalPages = totalPages; this.totalEntries = totalEntries; this.pageSize = pageSize;
        }
        public List<Entry> getEntries() { return entries; }
        public int getPageIndex() { return pageIndex; }
        public int getTotalPages() { return totalPages; }
        public int getTotalEntries() { return totalEntries; }
        public int getPageSize() { return pageSize; }
    }
}
