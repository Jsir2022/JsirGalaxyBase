package com.jsirgalaxybase.modules.warehouse.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bounded page of one installed Cell. Uninstalled Cells have no content panel. */
public final class TerminalCellContentSnapshot {
    public static final int MAX_PAGE_SIZE = 35;

    private final boolean cellPresent;
    private final long bayVersion;
    private final long totalBytes;
    private final long usedBytes;
    private final long storedItems;
    private final long storedTypes;
    private final long totalTypes;
    private final int pageIndex;
    private final int totalRows;
    private final int pageSize;
    private final String feedback;
    private final List<TerminalCellContentEntry> entries;

    public TerminalCellContentSnapshot(boolean cellPresent, long bayVersion, long totalBytes, long usedBytes,
        long storedItems, long storedTypes, long totalTypes, int pageIndex, int totalRows, int pageSize,
        String feedback, List<TerminalCellContentEntry> entries) {
        this.cellPresent = cellPresent;
        this.bayVersion = Math.max(0L, bayVersion);
        this.totalBytes = Math.max(0L, totalBytes);
        this.usedBytes = Math.max(0L, usedBytes);
        this.storedItems = Math.max(0L, storedItems);
        this.storedTypes = Math.max(0L, storedTypes);
        this.totalTypes = Math.max(0L, totalTypes);
        this.pageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, pageSize));
        this.totalRows = Math.max(0, totalRows);
        this.pageIndex = Math.max(0, Math.min(pageIndex, Math.max(0, getTotalPagesInternal() - 1)));
        this.feedback = safe(feedback, 64);
        List<TerminalCellContentEntry> copy = entries == null
            ? Collections.<TerminalCellContentEntry>emptyList() : new ArrayList<TerminalCellContentEntry>(entries);
        this.entries = Collections.unmodifiableList(copy.subList(0, Math.min(this.pageSize, copy.size())));
    }

    public static TerminalCellContentSnapshot empty() {
        return new TerminalCellContentSnapshot(false, 0L, 0L, 0L, 0L, 0L, 0L, 0, 0,
            MAX_PAGE_SIZE, "", Collections.<TerminalCellContentEntry>emptyList());
    }

    public TerminalCellContentSnapshot withFeedback(String value) {
        return new TerminalCellContentSnapshot(cellPresent, bayVersion, totalBytes, usedBytes, storedItems,
            storedTypes, totalTypes, pageIndex, totalRows, pageSize, value, entries);
    }

    public boolean isCellPresent() { return cellPresent; }
    public long getBayVersion() { return bayVersion; }
    public long getTotalBytes() { return totalBytes; }
    public long getUsedBytes() { return usedBytes; }
    public long getStoredItems() { return storedItems; }
    public long getStoredTypes() { return storedTypes; }
    public long getTotalTypes() { return totalTypes; }
    public int getPageIndex() { return pageIndex; }
    public int getTotalRows() { return totalRows; }
    public int getPageSize() { return pageSize; }
    public int getTotalPages() { return getTotalPagesInternal(); }
    public String getFeedback() { return feedback; }
    public List<TerminalCellContentEntry> getEntries() { return entries; }

    private int getTotalPagesInternal() { return Math.max(1, (totalRows + pageSize - 1) / pageSize); }
    private static String safe(String value, int limit) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() > limit ? normalized.substring(0, limit) : normalized;
    }
}
