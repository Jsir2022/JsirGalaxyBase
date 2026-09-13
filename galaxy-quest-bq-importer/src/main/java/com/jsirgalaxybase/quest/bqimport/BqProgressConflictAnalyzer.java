package com.jsirgalaxybase.quest.bqimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/** Read-only comparison. It deliberately does not invent a merge for divergent task vectors. */
public final class BqProgressConflictAnalyzer {
    public BqProgressConflictReport compare(List<BqImportedPlayerProgress> left,
        List<BqImportedPlayerProgress> right) {
        Map<UUID, BqImportedPlayerProgress> leftPlayers = players(left);
        Map<UUID, BqImportedPlayerProgress> rightPlayers = players(right);
        TreeSet<UUID> playerIds = new TreeSet<UUID>(Comparator.comparing(UUID::toString));
        playerIds.addAll(leftPlayers.keySet());
        playerIds.addAll(rightPlayers.keySet());
        List<BqProgressConflict> result = new ArrayList<BqProgressConflict>();
        for (UUID playerId : playerIds) {
            Map<UUID, BqImportedQuestProgress> leftQuests = quests(leftPlayers.get(playerId));
            Map<UUID, BqImportedQuestProgress> rightQuests = quests(rightPlayers.get(playerId));
            TreeSet<UUID> questIds = new TreeSet<UUID>(Comparator.comparing(UUID::toString));
            questIds.addAll(leftQuests.keySet());
            questIds.addAll(rightQuests.keySet());
            for (UUID questId : questIds) result.add(new BqProgressConflict(playerId, questId,
                relation(leftQuests.get(questId), rightQuests.get(questId))));
        }
        return new BqProgressConflictReport(result);
    }

    BqProgressRelation relation(BqImportedQuestProgress left, BqImportedQuestProgress right) {
        if (left == null) return BqProgressRelation.RIGHT_ONLY;
        if (right == null) return BqProgressRelation.LEFT_ONLY;
        boolean leftDominates = dominates(left, right);
        boolean rightDominates = dominates(right, left);
        if (leftDominates && rightDominates) return BqProgressRelation.IDENTICAL;
        if (leftDominates) return BqProgressRelation.LEFT_AHEAD;
        if (rightDominates) return BqProgressRelation.RIGHT_AHEAD;
        return BqProgressRelation.DIVERGENT;
    }

    private boolean dominates(BqImportedQuestProgress candidate, BqImportedQuestProgress baseline) {
        if (baseline.isCompleted() && !candidate.isCompleted()) return false;
        if (baseline.isClaimed() && !candidate.isClaimed()) return false;
        Map<String, BqImportedTaskProgress> candidateTasks = tasks(candidate);
        for (BqImportedTaskProgress task : baseline.getTasks()) {
            BqImportedTaskProgress other = candidateTasks.get(task.getKey());
            if (other == null || !other.getTypeId().equals(task.getTypeId())) return false;
            if (task.isComplete() && !other.isComplete()) return false;
            if (!vectorDominates(other.getValues(), task.getValues())) return false;
        }
        return true;
    }

    private boolean vectorDominates(List<Long> candidate, List<Long> baseline) {
        if (candidate.size() != baseline.size()) return false;
        for (int i = 0; i < baseline.size(); i++) if (candidate.get(i) < baseline.get(i)) return false;
        return true;
    }

    private Map<UUID, BqImportedPlayerProgress> players(List<BqImportedPlayerProgress> values) {
        Map<UUID, BqImportedPlayerProgress> result = new LinkedHashMap<UUID, BqImportedPlayerProgress>();
        for (BqImportedPlayerProgress value : values) {
            if (result.put(value.getPlayerId(), value) != null) throw new IllegalArgumentException(
                "duplicate player progress: " + value.getPlayerId());
        }
        return result;
    }

    private Map<UUID, BqImportedQuestProgress> quests(BqImportedPlayerProgress player) {
        if (player == null) return Collections.emptyMap();
        Map<UUID, BqImportedQuestProgress> result = new LinkedHashMap<UUID, BqImportedQuestProgress>();
        for (BqImportedQuestProgress value : player.getQuests()) {
            if (result.put(value.getQuestId(), value) != null) throw new IllegalArgumentException(
                "duplicate quest progress: " + value.getQuestId());
        }
        return result;
    }

    private Map<String, BqImportedTaskProgress> tasks(BqImportedQuestProgress quest) {
        Map<String, BqImportedTaskProgress> result = new LinkedHashMap<String, BqImportedTaskProgress>();
        for (BqImportedTaskProgress value : quest.getTasks()) {
            if (result.put(value.getKey(), value) != null) throw new IllegalArgumentException(
                "duplicate task progress: " + value.getKey());
        }
        return result;
    }
}
