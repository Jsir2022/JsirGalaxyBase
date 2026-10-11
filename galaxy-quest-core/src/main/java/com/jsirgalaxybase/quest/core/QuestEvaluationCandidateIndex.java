package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Immutable lookup plan built once for one published-catalog snapshot. */
final class QuestEvaluationCandidateIndex {
    private static final Comparator<Entry> CATALOG_ORDER = new Comparator<Entry>() {
        @Override public int compare(Entry left, Entry right) {
            return Integer.compare(left.ordinal, right.ordinal);
        }
    };

    private final Map<String, List<Entry>> byFactType;
    private final Map<UUID, Entry> byQuestId;
    private final Map<UUID, List<Entry>> byPrerequisite;
    private final int definitionCount;

    QuestEvaluationCandidateIndex(List<StoredQuestDefinition> definitions) {
        if (definitions == null) throw new IllegalArgumentException("definitions are required");
        Map<String, List<Entry>> facts = new HashMap<String, List<Entry>>();
        Map<UUID, Entry> quests = new HashMap<UUID, Entry>();
        Map<UUID, List<Entry>> prerequisites = new HashMap<UUID, List<Entry>>();
        int ordinal = 0;
        for (StoredQuestDefinition stored : definitions) {
            if (stored == null || stored.getDefinition() == null) {
                throw new IllegalStateException("published quest catalog contains a null definition");
            }
            QuestDefinition definition = stored.getDefinition();
            Entry entry = new Entry(ordinal++, stored);
            Entry duplicate = quests.put(definition.getId(), entry);
            if (duplicate != null) throw new IllegalStateException(
                "published quest catalog contains duplicate quest id " + definition.getId());
            for (TaskDefinition task : definition.getTasks()) {
                String factType = task.getParameters().get("factType");
                if (factType != null && !factType.isEmpty()) add(facts, factType, entry);
            }
            for (UUID prerequisite : definition.getPrerequisites()) add(prerequisites, prerequisite, entry);
        }
        this.byFactType = freeze(facts);
        this.byQuestId = Collections.unmodifiableMap(quests);
        this.byPrerequisite = freeze(prerequisites);
        this.definitionCount = ordinal;
    }

    List<StoredQuestDefinition> forFact(GameplayFact fact) {
        if (fact == null) throw new IllegalArgumentException("fact is required");
        String targetedQuest = fact.getAttributes().get("targetQuestId");
        if (targetedQuest == null && ("galaxy:task_confirmed".equals(fact.getTypeId())
            || "galaxy:consumption_applied".equals(fact.getTypeId()))) {
            targetedQuest = fact.getAttributes().get("questId");
        }
        if (targetedQuest != null) {
            UUID id;
            try { id = UUID.fromString(targetedQuest); }
            catch (IllegalArgumentException invalid) { return Collections.emptyList(); }
            Entry target = byQuestId.get(id);
            return target == null ? Collections.<StoredQuestDefinition>emptyList()
                : Collections.singletonList(target.definition);
        }
        return stored(byFactType.get(fact.getTypeId()));
    }

    List<StoredQuestDefinition> newlyUnlockedBy(UUID completedQuestId) {
        return stored(byPrerequisite.get(completedQuestId));
    }

    List<StoredQuestDefinition> forIds(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        Map<Integer, Entry> selected = new LinkedHashMap<Integer, Entry>();
        for (UUID id : ids) {
            Entry entry = byQuestId.get(id);
            if (entry != null) selected.put(entry.ordinal, entry);
        }
        if (selected.isEmpty()) return Collections.emptyList();
        List<Entry> ordered = new ArrayList<Entry>(selected.values());
        Collections.sort(ordered, CATALOG_ORDER);
        List<StoredQuestDefinition> result = new ArrayList<StoredQuestDefinition>(ordered.size());
        for (Entry entry : ordered) result.add(entry.definition);
        return Collections.unmodifiableList(result);
    }

    int definitionCount() { return definitionCount; }

    private static <K> void add(Map<K, List<Entry>> index, K key, Entry entry) {
        List<Entry> entries = index.get(key);
        if (entries == null) {
            entries = new ArrayList<Entry>();
            index.put(key, entries);
        }
        if (entries.isEmpty() || entries.get(entries.size() - 1) != entry) entries.add(entry);
    }

    private static <K> Map<K, List<Entry>> freeze(Map<K, List<Entry>> source) {
        Map<K, List<Entry>> result = new HashMap<K, List<Entry>>();
        for (Map.Entry<K, List<Entry>> entry : source.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<Entry>(entry.getValue())));
        }
        return Collections.unmodifiableMap(result);
    }

    private static List<StoredQuestDefinition> stored(List<Entry> entries) {
        if (entries == null || entries.isEmpty()) return Collections.emptyList();
        List<StoredQuestDefinition> result = new ArrayList<StoredQuestDefinition>(entries.size());
        for (Entry entry : entries) result.add(entry.definition);
        return Collections.unmodifiableList(result);
    }

    private static final class Entry {
        private final int ordinal;
        private final StoredQuestDefinition definition;

        private Entry(int ordinal, StoredQuestDefinition definition) {
            this.ordinal = ordinal;
            this.definition = definition;
        }
    }
}
