package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Builds a server-owned batch clone plan. References between selected quests are remapped while references to quests
 * outside the selection remain unchanged. Persistence is deliberately left to one surrounding transaction.
 */
public final class QuestDefinitionBatchCloner {
    public interface IdSource { UUID next(); }

    public static final class Result {
        private final Map<UUID, UUID> remappedIds;
        private final List<QuestDefinition> definitions;

        private Result(Map<UUID, UUID> remappedIds, List<QuestDefinition> definitions) {
            this.remappedIds = Collections.unmodifiableMap(new LinkedHashMap<UUID, UUID>(remappedIds));
            this.definitions = Collections.unmodifiableList(new ArrayList<QuestDefinition>(definitions));
        }

        public Map<UUID, UUID> getRemappedIds() { return remappedIds; }
        public List<QuestDefinition> getDefinitions() { return definitions; }
    }

    private final IdSource ids;

    public QuestDefinitionBatchCloner(IdSource ids) {
        this.ids = Objects.requireNonNull(ids, "ids");
    }

    public Result clone(List<QuestDefinition> sources) {
        if (sources == null || sources.isEmpty() || sources.size() > 128) {
            throw new IllegalArgumentException("clone batch requires 1 to 128 quests");
        }
        LinkedHashMap<UUID, QuestDefinition> unique = new LinkedHashMap<UUID, QuestDefinition>();
        for (QuestDefinition source : sources) {
            QuestDefinition value = Objects.requireNonNull(source, "source quest");
            if (unique.put(value.getId(), value) != null) throw new IllegalArgumentException("duplicate source quest");
        }

        LinkedHashMap<UUID, UUID> remap = new LinkedHashMap<UUID, UUID>();
        LinkedHashSet<UUID> reserved = new LinkedHashSet<UUID>(unique.keySet());
        for (UUID oldId : unique.keySet()) {
            UUID newId = nextUnique(reserved);
            reserved.add(newId);
            remap.put(oldId, newId);
        }

        List<QuestDefinition> copies = new ArrayList<QuestDefinition>(unique.size());
        for (QuestDefinition source : unique.values()) {
            Set<UUID> prerequisites = new LinkedHashSet<UUID>();
            for (UUID prerequisite : source.getPrerequisites()) {
                UUID replacement = remap.get(prerequisite);
                prerequisites.add(replacement == null ? prerequisite : replacement);
            }
            copies.add(new QuestDefinition(remap.get(source.getId()), 1, source.getName(), source.getDescription(),
                source.getPrerequisiteLogic(), source.getTaskLogic(), prerequisites, source.getTasks(),
                source.getRewards(), source.getRepeatPolicy(), source.getBehavior()));
        }
        return new Result(remap, copies);
    }

    private UUID nextUnique(Set<UUID> reserved) {
        for (int attempt = 0; attempt < 1024; attempt++) {
            UUID candidate = ids.next();
            if (candidate != null && !reserved.contains(candidate)) return candidate;
        }
        throw new IllegalStateException("unable to allocate a unique quest id");
    }
}
