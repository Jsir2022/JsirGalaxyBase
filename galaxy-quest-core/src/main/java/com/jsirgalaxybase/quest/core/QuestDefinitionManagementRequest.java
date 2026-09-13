package com.jsirgalaxybase.quest.core;

public final class QuestDefinitionManagementRequest {
    public static final int MAX_PAGE_SIZE = 50;
    private final String query;
    private final QuestDefinitionLifecycle lifecycle;
    private final int page;
    private final int pageSize;

    public QuestDefinitionManagementRequest(String query, QuestDefinitionLifecycle lifecycle, int page, int pageSize) {
        String normalized = query == null ? "" : query.trim();
        this.query = normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
        this.lifecycle = lifecycle;
        this.page = Math.max(0, page);
        this.pageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, pageSize));
    }

    public String getQuery() { return query; }
    public QuestDefinitionLifecycle getLifecycle() { return lifecycle; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() {
        long value = (long) page * (long) pageSize;
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
