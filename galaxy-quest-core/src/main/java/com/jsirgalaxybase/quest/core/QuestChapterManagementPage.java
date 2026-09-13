package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QuestChapterManagementPage {
    private final List<StoredQuestChapter> chapters;
    private final int page;
    private final int pageSize;
    private final long total;

    public QuestChapterManagementPage(List<StoredQuestChapter> chapters, int page, int pageSize, long total) {
        this.chapters = Collections.unmodifiableList(new ArrayList<StoredQuestChapter>(chapters));
        this.page = Math.max(0, page);
        this.pageSize = Math.max(1, pageSize);
        this.total = Math.max(0L, total);
    }

    public List<StoredQuestChapter> getChapters() { return chapters; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public long getTotal() { return total; }
    public boolean hasPrevious() { return page > 0; }
    public boolean hasNext() { return ((long) page + 1L) * pageSize < total; }
}
