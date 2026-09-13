package com.jsirgalaxybase.quest.core;

import java.util.List;

public interface PublishedQuestCatalog {
    List<StoredQuestDefinition> findAllPublished();
}
