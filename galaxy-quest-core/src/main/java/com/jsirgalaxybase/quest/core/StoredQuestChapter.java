package com.jsirgalaxybase.quest.core;

import java.util.Objects;

public final class StoredQuestChapter {
    private final QuestChapterDefinition definition;
    private final QuestDefinitionLifecycle lifecycle;
    private final String contentHash;
    private final long publishedAt;

    public StoredQuestChapter(QuestChapterDefinition definition, QuestDefinitionLifecycle lifecycle,
        String contentHash, long publishedAt) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.contentHash = Objects.requireNonNull(contentHash, "contentHash");
        this.publishedAt = publishedAt;
    }

    public QuestChapterDefinition getDefinition() { return definition; }
    public QuestDefinitionLifecycle getLifecycle() { return lifecycle; }
    public String getContentHash() { return contentHash; }
    public long getPublishedAt() { return publishedAt; }
}
