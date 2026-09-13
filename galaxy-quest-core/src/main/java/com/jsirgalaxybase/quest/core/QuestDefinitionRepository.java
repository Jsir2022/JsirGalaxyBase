package com.jsirgalaxybase.quest.core;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface QuestDefinitionRepository extends PublishedQuestCatalog {
    StoredQuestDefinition createDraft(QuestDefinition definition);
    StoredQuestDefinition updateDraft(QuestDefinition definition, String expectedContentHash);
    boolean publish(UUID questId, int version, String expectedContentHash, long publishedAt);
    boolean retire(UUID questId, int version);
    Optional<StoredQuestDefinition> find(UUID questId, int version);
    Optional<StoredQuestDefinition> findPublished(UUID questId);
    @Override List<StoredQuestDefinition> findAllPublished();
}
