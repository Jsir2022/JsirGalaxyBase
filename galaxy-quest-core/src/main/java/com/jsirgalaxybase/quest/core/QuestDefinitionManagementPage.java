package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QuestDefinitionManagementPage {
    private final List<StoredQuestDefinition> definitions;
    private final int page;
    private final int pageSize;
    private final long total;

    public QuestDefinitionManagementPage(List<StoredQuestDefinition> definitions, int page, int pageSize, long total) {
        this.definitions = Collections.unmodifiableList(new ArrayList<StoredQuestDefinition>(definitions));
        this.page = Math.max(0, page);
        this.pageSize = Math.max(1, pageSize);
        this.total = Math.max(0L, total);
    }

    public List<StoredQuestDefinition> getDefinitions() { return definitions; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public long getTotal() { return total; }
    public boolean hasPrevious() { return page > 0; }
    public boolean hasNext() { return ((long) page + 1L) * pageSize < total; }
}
