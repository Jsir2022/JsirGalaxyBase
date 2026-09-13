package com.jsirgalaxybase.quest.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.UUID;

/** Pure graph algorithm used by administration previews before destructive definition changes. */
public final class QuestDefinitionImpactAnalyzer {
    private static final int MAX_RESULTS = 4096;
    private static final String QUEST_REFERENCE_KEY = "questUuid";

    private static final class Reference {
        private final QuestDefinition definition;
        private final QuestDefinitionImpactReport.Dependent.ReferenceKind kind;
        private Reference(QuestDefinition definition, QuestDefinitionImpactReport.Dependent.ReferenceKind kind) {
            this.definition = definition; this.kind = kind;
        }
    }

    public QuestDefinitionImpactReport analyze(UUID targetId, List<StoredQuestDefinition> definitions,
        List<StoredQuestChapter> chapters) {
        if (targetId == null || definitions == null || chapters == null) {
            throw new IllegalArgumentException("target and catalogs are required");
        }
        Map<UUID, QuestDefinition> published = new LinkedHashMap<UUID, QuestDefinition>();
        Map<UUID, List<Reference>> reverse = new HashMap<UUID, List<Reference>>();
        List<StoredQuestDefinition> orderedDefinitions = new ArrayList<StoredQuestDefinition>();
        for (StoredQuestDefinition stored : definitions) if (stored != null) orderedDefinitions.add(stored);
        Collections.sort(orderedDefinitions, new Comparator<StoredQuestDefinition>() {
            @Override public int compare(StoredQuestDefinition left, StoredQuestDefinition right) {
                return left.getDefinition().getId().toString().compareTo(right.getDefinition().getId().toString());
            }
        });
        for (StoredQuestDefinition stored : orderedDefinitions) {
            if (stored == null || stored.getLifecycle() != QuestDefinitionLifecycle.PUBLISHED) continue;
            QuestDefinition definition = stored.getDefinition();
            published.put(definition.getId(), definition);
            for (UUID prerequisite : definition.getPrerequisites()) {
                addReference(reverse, prerequisite, definition,
                    QuestDefinitionImpactReport.Dependent.ReferenceKind.PREREQUISITE);
            }
            for (TaskDefinition task : definition.getTasks()) addParameterReference(reverse, task.getParameters(),
                definition, QuestDefinitionImpactReport.Dependent.ReferenceKind.TASK_REFERENCE);
            for (RewardDefinition reward : definition.getRewards()) addParameterReference(reverse,
                reward.getParameters(), definition, QuestDefinitionImpactReport.Dependent.ReferenceKind.REWARD_REFERENCE);
        }
        Comparator<Reference> byId = new Comparator<Reference>() {
            @Override public int compare(Reference left, Reference right) {
                int id = left.definition.getId().toString().compareTo(right.definition.getId().toString());
                return id != 0 ? id : left.kind.compareTo(right.kind);
            }
        };
        for (List<Reference> values : reverse.values()) Collections.sort(values, byId);

        List<QuestDefinitionImpactReport.Dependent> dependents = new ArrayList<QuestDefinitionImpactReport.Dependent>();
        Map<UUID, Integer> distance = new HashMap<UUID, Integer>();
        ArrayDeque<UUID> pending = new ArrayDeque<UUID>();
        distance.put(targetId, Integer.valueOf(0)); pending.add(targetId);
        boolean truncated = false;
        while (!pending.isEmpty()) {
            UUID current = pending.removeFirst();
            List<Reference> next = reverse.get(current);
            if (next == null) continue;
            for (Reference reference : next) {
                QuestDefinition value = reference.definition;
                if (distance.containsKey(value.getId())) continue;
                int nextDistance = distance.get(current).intValue() + 1;
                distance.put(value.getId(), Integer.valueOf(nextDistance));
                dependents.add(new QuestDefinitionImpactReport.Dependent(value.getId(), value.getName(), nextDistance,
                    reference.kind));
                if (dependents.size() >= MAX_RESULTS) { truncated = true; pending.clear(); break; }
                pending.addLast(value.getId());
            }
        }

        List<QuestDefinitionImpactReport.Placement> placements = new ArrayList<QuestDefinitionImpactReport.Placement>();
        List<StoredQuestChapter> orderedChapters = new ArrayList<StoredQuestChapter>();
        for (StoredQuestChapter stored : chapters) if (stored != null) orderedChapters.add(stored);
        Collections.sort(orderedChapters, new Comparator<StoredQuestChapter>() {
            @Override public int compare(StoredQuestChapter left, StoredQuestChapter right) {
                return left.getDefinition().getId().toString().compareTo(right.getDefinition().getId().toString());
            }
        });
        for (StoredQuestChapter stored : orderedChapters) {
            if (stored.getLifecycle() != QuestDefinitionLifecycle.PUBLISHED) continue;
            for (QuestChapterEntry entry : stored.getDefinition().getEntries()) {
                if (targetId.equals(entry.getQuestId())) {
                    placements.add(new QuestDefinitionImpactReport.Placement(stored.getDefinition().getId(),
                        stored.getDefinition().getName()));
                    break;
                }
            }
            if (placements.size() >= MAX_RESULTS) { truncated = true; break; }
        }
        return new QuestDefinitionImpactReport(targetId, published.containsKey(targetId), dependents, placements,
            truncated);
    }

    /**
     * Computes the impact of retiring a selection together. References between selected quests are internal and do
     * not block the operation; references from outside the selection and every published chapter placement do.
     */
    public QuestDefinitionBatchImpactReport analyzeBatch(List<UUID> targetIds,
        List<StoredQuestDefinition> definitions, List<StoredQuestChapter> chapters) {
        if (targetIds == null || definitions == null || chapters == null || targetIds.isEmpty()
            || targetIds.size() > 128) throw new IllegalArgumentException("1 to 128 targets and catalogs are required");
        Set<UUID> targets = new LinkedHashSet<UUID>();
        for (UUID target : targetIds) {
            if (target == null || !targets.add(target)) throw new IllegalArgumentException("targets must be unique");
        }
        List<UUID> orderedTargets = new ArrayList<UUID>(targets);
        Collections.sort(orderedTargets, new Comparator<UUID>() {
            @Override public int compare(UUID left, UUID right) { return left.toString().compareTo(right.toString()); }
        });

        Map<UUID, QuestDefinition> published = new LinkedHashMap<UUID, QuestDefinition>();
        Map<UUID, List<Reference>> reverse = new HashMap<UUID, List<Reference>>();
        List<StoredQuestDefinition> orderedDefinitions = new ArrayList<StoredQuestDefinition>();
        for (StoredQuestDefinition stored : definitions) if (stored != null) orderedDefinitions.add(stored);
        Collections.sort(orderedDefinitions, new Comparator<StoredQuestDefinition>() {
            @Override public int compare(StoredQuestDefinition left, StoredQuestDefinition right) {
                return left.getDefinition().getId().toString().compareTo(right.getDefinition().getId().toString());
            }
        });
        for (StoredQuestDefinition stored : orderedDefinitions) {
            if (stored.getLifecycle() != QuestDefinitionLifecycle.PUBLISHED) continue;
            QuestDefinition definition = stored.getDefinition();
            published.put(definition.getId(), definition);
            for (UUID prerequisite : definition.getPrerequisites()) addReference(reverse, prerequisite, definition,
                QuestDefinitionImpactReport.Dependent.ReferenceKind.PREREQUISITE);
            for (TaskDefinition task : definition.getTasks()) addParameterReference(reverse, task.getParameters(),
                definition, QuestDefinitionImpactReport.Dependent.ReferenceKind.TASK_REFERENCE);
            for (RewardDefinition reward : definition.getRewards()) addParameterReference(reverse,
                reward.getParameters(), definition, QuestDefinitionImpactReport.Dependent.ReferenceKind.REWARD_REFERENCE);
        }
        Comparator<Reference> byId = new Comparator<Reference>() {
            @Override public int compare(Reference left, Reference right) {
                int id = left.definition.getId().toString().compareTo(right.definition.getId().toString());
                return id != 0 ? id : left.kind.compareTo(right.kind);
            }
        };
        for (List<Reference> values : reverse.values()) Collections.sort(values, byId);

        List<UUID> unpublished = new ArrayList<UUID>();
        for (UUID target : orderedTargets) if (!published.containsKey(target)) unpublished.add(target);
        Map<UUID, Integer> distance = new HashMap<UUID, Integer>();
        ArrayDeque<UUID> pending = new ArrayDeque<UUID>();
        for (UUID target : orderedTargets) { distance.put(target, Integer.valueOf(0)); pending.addLast(target); }
        List<QuestDefinitionImpactReport.Dependent> dependents =
            new ArrayList<QuestDefinitionImpactReport.Dependent>();
        boolean truncated = false;
        while (!pending.isEmpty()) {
            UUID current = pending.removeFirst();
            List<Reference> next = reverse.get(current);
            if (next == null) continue;
            for (Reference reference : next) {
                UUID dependentId = reference.definition.getId();
                if (distance.containsKey(dependentId)) continue;
                int nextDistance = distance.get(current).intValue() + 1;
                distance.put(dependentId, Integer.valueOf(nextDistance));
                dependents.add(new QuestDefinitionImpactReport.Dependent(dependentId,
                    reference.definition.getName(), nextDistance, reference.kind));
                if (dependents.size() >= MAX_RESULTS) { truncated = true; pending.clear(); break; }
                pending.addLast(dependentId);
            }
        }

        List<QuestDefinitionBatchImpactReport.Placement> placements =
            new ArrayList<QuestDefinitionBatchImpactReport.Placement>();
        List<StoredQuestChapter> orderedChapters = new ArrayList<StoredQuestChapter>();
        for (StoredQuestChapter stored : chapters) if (stored != null) orderedChapters.add(stored);
        Collections.sort(orderedChapters, new Comparator<StoredQuestChapter>() {
            @Override public int compare(StoredQuestChapter left, StoredQuestChapter right) {
                return left.getDefinition().getId().toString().compareTo(right.getDefinition().getId().toString());
            }
        });
        for (StoredQuestChapter stored : orderedChapters) {
            if (stored.getLifecycle() != QuestDefinitionLifecycle.PUBLISHED) continue;
            for (QuestChapterEntry entry : stored.getDefinition().getEntries()) {
                if (targets.contains(entry.getQuestId())) placements.add(new QuestDefinitionBatchImpactReport.Placement(
                    entry.getQuestId(), stored.getDefinition().getId(), stored.getDefinition().getName()));
                if (placements.size() >= MAX_RESULTS) { truncated = true; break; }
            }
            if (truncated) break;
        }
        return new QuestDefinitionBatchImpactReport(orderedTargets, unpublished, dependents, placements, truncated);
    }

    private static void addParameterReference(Map<UUID, List<Reference>> reverse, Map<String, String> parameters,
        QuestDefinition definition, QuestDefinitionImpactReport.Dependent.ReferenceKind kind) {
        if (parameters == null) return;
        String raw = parameters.get(QUEST_REFERENCE_KEY);
        if (raw == null || raw.trim().isEmpty()) return;
        try { addReference(reverse, UUID.fromString(raw.trim()), definition, kind); }
        catch (IllegalArgumentException ignored) { /* Definition validation reports malformed UUIDs separately. */ }
    }

    private static void addReference(Map<UUID, List<Reference>> reverse, UUID target, QuestDefinition definition,
        QuestDefinitionImpactReport.Dependent.ReferenceKind kind) {
        List<Reference> values = reverse.get(target);
        if (values == null) { values = new ArrayList<Reference>(); reverse.put(target, values); }
        values.add(new Reference(definition, kind));
    }
}
