package com.jsirgalaxybase.quest.bqimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.QuestDefinition;
import com.jsirgalaxybase.quest.core.QuestProgressSnapshot;
import com.jsirgalaxybase.quest.core.QuestStatus;
import com.jsirgalaxybase.quest.core.TaskDefinition;
import com.jsirgalaxybase.quest.core.TaskProgress;

/**
 * Compares read-only BQ import records with Base PostgreSQL snapshots.
 *
 * <p>This is intentionally not a merge and never changes progress or reward
 * state.  It is the deterministic evidence layer used before a future
 * production cutover.</p>
 */
public final class BqShadowProgressAnalyzer {
    public static final int MAX_ENTRIES = 4096;

    public BqShadowDifferenceReport compare(List<BqImportedPlayerProgress> bqPlayers,
        List<QuestProgressSnapshot> baseProgress, Map<UUID, QuestDefinition> definitions) {
        Map<UUID, BqImportedPlayerProgress> bq = bqPlayers(bqPlayers);
        Map<String, QuestProgressSnapshot> base = baseProgress(baseProgress);
        Set<String> keys = new TreeSet<String>();
        for (Map.Entry<UUID, BqImportedPlayerProgress> player : bq.entrySet()) {
            for (BqImportedQuestProgress quest : player.getValue().getQuests()) keys.add(key(player.getKey(), quest.getQuestId()));
        }
        for (String key : base.keySet()) keys.add(key);

        List<BqShadowDifference> result = new ArrayList<BqShadowDifference>();
        boolean truncated = false;
        for (String key : keys) {
            if (result.size() >= MAX_ENTRIES) { truncated = true; break; }
            int separator = key.indexOf('\u0000');
            UUID playerId = UUID.fromString(key.substring(0, separator));
            UUID questId = UUID.fromString(key.substring(separator + 1));
            BqImportedQuestProgress bqQuest = findBq(bq.get(playerId), questId);
            QuestProgressSnapshot baseQuest = base.get(key);
            result.add(compareOne(playerId, questId, bqQuest, baseQuest,
                definitions == null ? null : definitions.get(questId)));
        }
        return new BqShadowDifferenceReport(result, truncated);
    }

    private BqShadowDifference compareOne(UUID playerId, UUID questId, BqImportedQuestProgress bq,
        QuestProgressSnapshot base, QuestDefinition definition) {
        if (bq == null) return new BqShadowDifference(playerId, questId, BqShadowRelation.BASE_ONLY,
            Collections.singletonList("base-progress-only"));
        if (base == null) return new BqShadowDifference(playerId, questId, BqShadowRelation.BQ_ONLY,
            Collections.singletonList("bq-progress-only"));
        if (definition == null) return new BqShadowDifference(playerId, questId, BqShadowRelation.INCOMPARABLE,
            Collections.singletonList("definition-missing"));

        List<String> reasons = new ArrayList<String>();
        boolean bqCompleted = bq.isCompleted();
        boolean baseCompleted = base.getStatus() == QuestStatus.COMPLETED;
        if (bqCompleted != baseCompleted) reasons.add("completed:" + bqCompleted + "->" + baseCompleted);
        Map<String, BqImportedTaskProgress> bqTasks = bqTasks(bq);
        Map<String, TaskDefinition> definitions = definitions(definition);
        Set<String> taskKeys = new TreeSet<String>();
        taskKeys.addAll(bqTasks.keySet());
        taskKeys.addAll(base.getTasks().keySet());
        boolean incomparable = false;
        for (String taskKey : taskKeys) incomparable |= compareTask(taskKey, bqTasks.get(taskKey),
            base.getTasks().get(taskKey), definitions.get(taskKey), reasons);

        if (reasons.isEmpty()) return new BqShadowDifference(playerId, questId, BqShadowRelation.IDENTICAL, reasons);
        if (incomparable) return new BqShadowDifference(playerId, questId, BqShadowRelation.INCOMPARABLE, reasons);
        boolean baseDominates = baseCompleted || !bqCompleted;
        boolean bqDominates = bqCompleted || !baseCompleted;
        for (String taskKey : taskKeys) {
            BqImportedTaskProgress left = bqTasks.get(taskKey);
            TaskProgress right = base.getTasks().get(taskKey);
            if (left == null || right == null) { baseDominates = false; bqDominates = false; continue; }
            if (!vectorAtLeast(right.getValues(), left.getValues())) baseDominates = false;
            if (!vectorAtLeast(left.getValues(), right.getValues())) bqDominates = false;
        }
        BqShadowRelation relation = baseDominates && !bqDominates ? BqShadowRelation.BASE_AHEAD
            : bqDominates && !baseDominates ? BqShadowRelation.BQ_AHEAD : BqShadowRelation.DIVERGENT;
        return new BqShadowDifference(playerId, questId, relation, reasons);
    }

    private boolean compareTask(String key, BqImportedTaskProgress bq, TaskProgress base, TaskDefinition definition,
        List<String> reasons) {
        if (bq == null) { reasons.add("task-missing-in-bq:" + key); return true; }
        if (base == null) { reasons.add("task-missing-in-base:" + key); return true; }
        boolean incomparable = false;
        if (definition == null) {
            reasons.add("task-definition-missing:" + key);
            incomparable = true;
        } else if (!definition.getTypeId().equals(bq.getTypeId())) {
            reasons.add("task-type:" + key + ":" + bq.getTypeId() + "->" + definition.getTypeId());
            incomparable = true;
        }
        if (bq.getValues().size() != base.getValues().size()) {
            reasons.add("task-shape:" + key + ":" + bq.getValues().size() + "->" + base.getValues().size());
            incomparable = true;
        }
        if (!bq.getValues().equals(base.getValues())) reasons.add("task-values:" + key + ":"
            + bq.getValues() + "->" + base.getValues());
        if (bq.isComplete() != base.isComplete()) reasons.add("task-complete:" + key + ":"
            + bq.isComplete() + "->" + base.isComplete());
        return incomparable;
    }

    private boolean vectorAtLeast(List<Long> candidate, List<Long> baseline) {
        if (candidate.size() != baseline.size()) return false;
        for (int i = 0; i < baseline.size(); i++) if (candidate.get(i) < baseline.get(i)) return false;
        return true;
    }

    private Map<UUID, BqImportedPlayerProgress> bqPlayers(List<BqImportedPlayerProgress> values) {
        Map<UUID, BqImportedPlayerProgress> result = new LinkedHashMap<UUID, BqImportedPlayerProgress>();
        if (values != null) for (BqImportedPlayerProgress value : values) {
            if (value == null) throw new IllegalArgumentException("BQ player progress must not be null");
            if (result.put(value.getPlayerId(), value) != null) throw new IllegalArgumentException(
                "duplicate BQ player progress: " + value.getPlayerId());
        }
        return result;
    }

    private Map<String, QuestProgressSnapshot> baseProgress(List<QuestProgressSnapshot> values) {
        Map<String, QuestProgressSnapshot> result = new LinkedHashMap<String, QuestProgressSnapshot>();
        if (values != null) for (QuestProgressSnapshot value : values) {
            if (value == null) throw new IllegalArgumentException("Base progress must not be null");
            if (value.getParticipantId().getType() != com.jsirgalaxybase.quest.core.ParticipantType.PLAYER) {
                throw new IllegalArgumentException("Base shadow progress must use PLAYER participants: "
                    + value.getParticipantId());
            }
            String key = key(value.getPlayerId(), value.getQuestId());
            if (result.put(key, value) != null) throw new IllegalArgumentException("duplicate Base progress: " + key);
        }
        return result;
    }

    private BqImportedQuestProgress findBq(BqImportedPlayerProgress player, UUID questId) {
        if (player == null) return null;
        BqImportedQuestProgress result = null;
        for (BqImportedQuestProgress value : player.getQuests()) if (questId.equals(value.getQuestId())) {
            if (result != null) throw new IllegalArgumentException("duplicate BQ quest progress: " + questId);
            result = value;
        }
        return result;
    }

    private Map<String, BqImportedTaskProgress> bqTasks(BqImportedQuestProgress quest) {
        Map<String, BqImportedTaskProgress> result = new LinkedHashMap<String, BqImportedTaskProgress>();
        for (BqImportedTaskProgress value : quest.getTasks()) if (result.put(value.getKey(), value) != null) {
            throw new IllegalArgumentException("duplicate BQ task progress: " + value.getKey());
        }
        return result;
    }

    private Map<String, TaskDefinition> definitions(QuestDefinition definition) {
        Map<String, TaskDefinition> result = new LinkedHashMap<String, TaskDefinition>();
        for (TaskDefinition value : definition.getTasks()) if (result.put(value.getKey(), value) != null) {
            throw new IllegalArgumentException("duplicate Base task definition: " + value.getKey());
        }
        return result;
    }

    private String key(UUID player, UUID quest) { return player.toString() + '\u0000' + quest.toString(); }
}
