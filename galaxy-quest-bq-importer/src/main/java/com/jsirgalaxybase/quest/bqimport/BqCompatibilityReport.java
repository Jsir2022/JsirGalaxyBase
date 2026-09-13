package com.jsirgalaxybase.quest.bqimport;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;

public final class BqCompatibilityReport {
    private final int questCount;
    private final Map<String, Integer> taskTypes;
    private final Map<String, Integer> rewardTypes;
    private final Set<String> unknownTaskTypes;
    private final Set<String> unknownRewardTypes;
    private final Map<String, BqCompatibilityStatus> taskCompatibility;
    private final Map<String, BqCompatibilityStatus> rewardCompatibility;

    private BqCompatibilityReport(int questCount, Map<String, Integer> taskTypes, Map<String, Integer> rewardTypes,
        Set<String> unknownTaskTypes, Set<String> unknownRewardTypes,
        Map<String, BqCompatibilityStatus> taskCompatibility,
        Map<String, BqCompatibilityStatus> rewardCompatibility) {
        this.questCount = questCount;
        this.taskTypes = Collections.unmodifiableMap(taskTypes);
        this.rewardTypes = Collections.unmodifiableMap(rewardTypes);
        this.unknownTaskTypes = Collections.unmodifiableSet(unknownTaskTypes);
        this.unknownRewardTypes = Collections.unmodifiableSet(unknownRewardTypes);
        this.taskCompatibility = Collections.unmodifiableMap(taskCompatibility);
        this.rewardCompatibility = Collections.unmodifiableMap(rewardCompatibility);
    }

    public static BqCompatibilityReport create(Iterable<BqImportedQuest> quests, Set<String> supportedTasks,
        Set<String> supportedRewards) {
        int count = 0;
        Map<String, Integer> taskTypes = new TreeMap<String, Integer>();
        Map<String, Integer> rewardTypes = new TreeMap<String, Integer>();
        for (BqImportedQuest quest : quests) {
            count++;
            for (TaskDefinition task : quest.getDefinition().getTasks()) increment(taskTypes, task.getTypeId());
            for (RewardDefinition reward : quest.getDefinition().getRewards()) increment(rewardTypes, reward.getTypeId());
        }
        Set<String> unknownTasks = new TreeSet<String>(taskTypes.keySet());
        unknownTasks.removeAll(supportedTasks);
        Set<String> unknownRewards = new TreeSet<String>(rewardTypes.keySet());
        unknownRewards.removeAll(supportedRewards);
        return new BqCompatibilityReport(count, taskTypes, rewardTypes, unknownTasks, unknownRewards,
            compatibility(taskTypes.keySet(), supportedTasks), compatibility(rewardTypes.keySet(), supportedRewards));
    }

    private static Map<String, BqCompatibilityStatus> compatibility(Set<String> present, Set<String> adapted) {
        Map<String, BqCompatibilityStatus> result = new TreeMap<String, BqCompatibilityStatus>();
        for (String type : present) {
            result.put(type, adapted.contains(type) ? BqCompatibilityStatus.ADAPTED : BqCompatibilityStatus.UNKNOWN);
        }
        return result;
    }

    private static void increment(Map<String, Integer> counts, String key) {
        Integer old = counts.get(key);
        counts.put(key, old == null ? 1 : old + 1);
    }

    public int getQuestCount() { return questCount; }
    public Map<String, Integer> getTaskTypes() { return taskTypes; }
    public Map<String, Integer> getRewardTypes() { return rewardTypes; }
    public Set<String> getUnknownTaskTypes() { return unknownTaskTypes; }
    public Set<String> getUnknownRewardTypes() { return unknownRewardTypes; }
    public Map<String, BqCompatibilityStatus> getTaskCompatibility() { return taskCompatibility; }
    public Map<String, BqCompatibilityStatus> getRewardCompatibility() { return rewardCompatibility; }

    public String toText() {
        return "quests=" + questCount + "\ntaskTypes=" + taskTypes + "\nrewardTypes=" + rewardTypes
            + "\ntaskCompatibility=" + taskCompatibility + "\nrewardCompatibility=" + rewardCompatibility
            + "\nunknownTaskTypes=" + unknownTaskTypes + "\nunknownRewardTypes=" + unknownRewardTypes + "\n";
    }
}
