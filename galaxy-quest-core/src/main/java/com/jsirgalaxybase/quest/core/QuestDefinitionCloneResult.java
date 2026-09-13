package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class QuestDefinitionCloneResult {
    public enum Status { SUCCESS, FORBIDDEN, NOT_FOUND, INVALID, CONFLICT }
    private final Status status;
    private final Map<UUID, UUID> remappedIds;
    private final List<StoredQuestDefinition> definitions;
    private QuestDefinitionCloneResult(Status status, Map<UUID, UUID> remappedIds,
        List<StoredQuestDefinition> definitions) {
        this.status = status;
        this.remappedIds = remappedIds == null ? Collections.<UUID, UUID>emptyMap()
            : Collections.unmodifiableMap(new java.util.LinkedHashMap<UUID, UUID>(remappedIds));
        this.definitions = definitions == null ? Collections.<StoredQuestDefinition>emptyList()
            : Collections.unmodifiableList(new ArrayList<StoredQuestDefinition>(definitions));
    }
    public static QuestDefinitionCloneResult success(Map<UUID, UUID> ids, List<StoredQuestDefinition> definitions) {
        return new QuestDefinitionCloneResult(Status.SUCCESS, ids, definitions);
    }
    public static QuestDefinitionCloneResult status(Status status) {
        return new QuestDefinitionCloneResult(status, null, null);
    }
    public Status getStatus() { return status; }
    public Map<UUID, UUID> getRemappedIds() { return remappedIds; }
    public List<StoredQuestDefinition> getDefinitions() { return definitions; }
    public boolean isSuccess() { return status == Status.SUCCESS; }
}
