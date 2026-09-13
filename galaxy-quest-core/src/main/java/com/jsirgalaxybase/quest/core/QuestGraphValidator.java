package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class QuestGraphValidator {
    public List<String> validate(Collection<QuestDefinition> definitions) {
        List<String> errors = new ArrayList<String>();
        Map<UUID, QuestDefinition> byId = new HashMap<UUID, QuestDefinition>();
        for (QuestDefinition definition : definitions) {
            if (byId.put(definition.getId(), definition) != null) errors.add("duplicate quest id: " + definition.getId());
        }
        for (QuestDefinition definition : definitions) {
            for (UUID prerequisite : definition.getPrerequisites()) {
                if (!byId.containsKey(prerequisite)) errors.add("missing prerequisite " + prerequisite + " for " + definition.getId());
            }
        }
        Set<UUID> visiting = new HashSet<UUID>();
        Set<UUID> visited = new HashSet<UUID>();
        for (UUID id : byId.keySet()) detectCycle(id, byId, visiting, visited, errors);
        return errors;
    }

    private static void detectCycle(UUID id, Map<UUID, QuestDefinition> definitions, Set<UUID> visiting,
        Set<UUID> visited, List<String> errors) {
        if (visited.contains(id)) return;
        if (!visiting.add(id)) {
            errors.add("quest prerequisite cycle at " + id);
            return;
        }
        QuestDefinition definition = definitions.get(id);
        if (definition != null) {
            for (UUID prerequisite : definition.getPrerequisites()) {
                if (definitions.containsKey(prerequisite)) detectCycle(prerequisite, definitions, visiting, visited, errors);
            }
        }
        visiting.remove(id);
        visited.add(id);
    }
}
