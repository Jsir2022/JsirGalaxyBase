package com.jsirgalaxybase.quest.core;

import java.util.UUID;

/** Stable display-order identity, deliberately separate from immutable chapter definition versions. */
public final class QuestChapterCatalogEntry {
    private final UUID chapterId;
    private final long order;

    public QuestChapterCatalogEntry(UUID chapterId, long order) {
        if (chapterId == null || order < 0L) throw new IllegalArgumentException("valid chapter catalog entry is required");
        this.chapterId = chapterId;
        this.order = order;
    }
    public UUID getChapterId() { return chapterId; }
    public long getOrder() { return order; }
}
