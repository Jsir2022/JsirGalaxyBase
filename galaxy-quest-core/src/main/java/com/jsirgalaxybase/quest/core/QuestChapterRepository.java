package com.jsirgalaxybase.quest.core;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestChapterRepository {
    StoredQuestChapter createDraft(QuestChapterDefinition definition);
    StoredQuestChapter updateDraft(QuestChapterDefinition definition, String expectedContentHash);
    boolean publish(UUID chapterId, int version, String expectedContentHash, long publishedAt);
    boolean retire(UUID chapterId, int version);
    Optional<StoredQuestChapter> find(UUID chapterId, int version);
    Optional<StoredQuestChapter> findPublished(UUID chapterId);
    List<StoredQuestChapter> findAllPublished();
}
