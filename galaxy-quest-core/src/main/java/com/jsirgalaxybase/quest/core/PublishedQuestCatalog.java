package com.jsirgalaxybase.quest.core;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublishedQuestCatalog {
    List<StoredQuestDefinition> findAllPublished();

    /** Exact historical published version for durable operations pinned before a newer publication. */
    default Optional<StoredQuestDefinition> findPublished(UUID questId, int definitionVersion) {
        if (questId == null || definitionVersion < 1) return Optional.empty();
        List<StoredQuestDefinition> values = findAllPublished();
        if (values == null) throw new IllegalStateException("published quest catalog returned null");
        for (StoredQuestDefinition stored : values) {
            if (stored != null && stored.getLifecycle() == QuestDefinitionLifecycle.PUBLISHED
                && questId.equals(stored.getDefinition().getId())
                && definitionVersion == stored.getDefinition().getVersion()) return Optional.of(stored);
        }
        return Optional.empty();
    }
}
