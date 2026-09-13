package com.jsirgalaxybase.quest.core;

public interface QuestDefinitionManagementQuery {
    QuestDefinitionManagementPage load(QuestDefinitionManagementRequest request);
    java.util.Optional<StoredQuestDefinition> find(java.util.UUID questId, int version);
}
