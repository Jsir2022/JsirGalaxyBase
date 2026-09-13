package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QuestChapterOrderingResult {
    public enum Status { SUCCESS, FORBIDDEN, INVALID }
    private final Status status;
    private final List<QuestChapterCatalogEntry> order;
    private final String message;
    private QuestChapterOrderingResult(Status status, List<QuestChapterCatalogEntry> order, String message) {
        this.status = status; this.order = order == null ? Collections.<QuestChapterCatalogEntry>emptyList()
            : Collections.unmodifiableList(new ArrayList<QuestChapterCatalogEntry>(order)); this.message = message == null ? "" : message;
    }
    public static QuestChapterOrderingResult success(List<QuestChapterCatalogEntry> order) { return new QuestChapterOrderingResult(Status.SUCCESS, order, ""); }
    public static QuestChapterOrderingResult status(Status status, String message) { return new QuestChapterOrderingResult(status, null, message); }
    public Status getStatus() { return status; }
    public List<QuestChapterCatalogEntry> getOrder() { return order; }
    public String getMessage() { return message; }
}
