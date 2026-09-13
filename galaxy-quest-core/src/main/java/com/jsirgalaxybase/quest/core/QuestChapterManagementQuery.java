package com.jsirgalaxybase.quest.core;

import java.util.Optional;
import java.util.UUID;

public interface QuestChapterManagementQuery {
    QuestChapterManagementPage load(QuestDefinitionManagementRequest request);

    Optional<StoredQuestChapter> find(UUID chapterId, int version);
}
