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
    private final Map<String, BqCompatibilityStatus> elementCompatibility;

    private BqCompatibilityReport(int questCount, Map<String, Integer> taskTypes, Map<String, Integer> rewardTypes,
        Set<String> unknownTaskTypes, Set<String> unknownRewardTypes,
        Map<String, BqCompatibilityStatus> taskCompatibility,
        Map<String, BqCompatibilityStatus> rewardCompatibility) {
        this(questCount,taskTypes,rewardTypes,unknownTaskTypes,unknownRewardTypes,taskCompatibility,rewardCompatibility,
            Collections.<String,BqCompatibilityStatus>emptyMap());
    }

    private BqCompatibilityReport(int questCount, Map<String, Integer> taskTypes, Map<String, Integer> rewardTypes,
        Set<String> unknownTaskTypes, Set<String> unknownRewardTypes,
        Map<String, BqCompatibilityStatus> taskCompatibility,
        Map<String, BqCompatibilityStatus> rewardCompatibility,
        Map<String, BqCompatibilityStatus> elementCompatibility) {
        this.questCount = questCount;
        this.taskTypes = Collections.unmodifiableMap(taskTypes);
        this.rewardTypes = Collections.unmodifiableMap(rewardTypes);
        this.unknownTaskTypes = Collections.unmodifiableSet(unknownTaskTypes);
        this.unknownRewardTypes = Collections.unmodifiableSet(unknownRewardTypes);
        this.taskCompatibility = Collections.unmodifiableMap(taskCompatibility);
        this.rewardCompatibility = Collections.unmodifiableMap(rewardCompatibility);
        this.elementCompatibility = Collections.unmodifiableMap(elementCompatibility);
    }

    /** Per-element compatibility matrix used by migration dry-runs. */
    public static BqCompatibilityReport create(BqMigrationManifest manifest) {
        BqCompatibilityReport base=create(manifest.getQuests(),BqStandardCodec.supportedTaskTypes(),
            BqStandardCodec.supportedRewardTypes());
        Set<String> blocked=new TreeSet<String>();
        for(BqMigrationIssue issue:manifest.getIssues())if(issue.getCode().startsWith("INVALID_TASK_")
            ||issue.getCode().startsWith("INVALID_REWARD_"))blocked.add(issue.getSubject());
        Map<String,BqCompatibilityStatus> elements=new TreeMap<String,BqCompatibilityStatus>();
        Map<String,BqCompatibilityStatus> tasks=new TreeMap<String,BqCompatibilityStatus>(base.taskCompatibility);
        Map<String,BqCompatibilityStatus> rewards=new TreeMap<String,BqCompatibilityStatus>(base.rewardCompatibility);
        for(BqImportedQuest quest:manifest.getQuests()){
            String id=quest.getDefinition().getId().toString();
            for(TaskDefinition task:quest.getDefinition().getTasks()){
                BqCompatibilityStatus status=!BqStandardCodec.supportedTaskTypes().contains(task.getTypeId())
                    ?BqCompatibilityStatus.UNKNOWN:blocked.contains(id+"/"+task.getKey())
                    ?BqCompatibilityStatus.BLOCKED:BqCompatibilityStatus.ADAPTED;
                elements.put("TASK|"+id+"|"+task.getKey()+"|"+task.getTypeId(),status);
                tasks.put(task.getTypeId(),worst(tasks.get(task.getTypeId()),status));
            }
            for(RewardDefinition reward:quest.getDefinition().getRewards()){
                BqCompatibilityStatus status=!BqStandardCodec.supportedRewardTypes().contains(reward.getTypeId())
                    ?BqCompatibilityStatus.UNKNOWN:blocked.contains(id+"/"+reward.getKey())
                    ?BqCompatibilityStatus.BLOCKED:BqCompatibilityStatus.ADAPTED;
                elements.put("REWARD|"+id+"|"+reward.getKey()+"|"+reward.getTypeId(),status);
                rewards.put(reward.getTypeId(),worst(rewards.get(reward.getTypeId()),status));
            }
        }
        return new BqCompatibilityReport(base.questCount,new TreeMap<String,Integer>(base.taskTypes),
            new TreeMap<String,Integer>(base.rewardTypes),new TreeSet<String>(base.unknownTaskTypes),
            new TreeSet<String>(base.unknownRewardTypes),tasks,rewards,elements);
    }

    private static BqCompatibilityStatus worst(BqCompatibilityStatus left,BqCompatibilityStatus right){
        if(left==BqCompatibilityStatus.UNKNOWN||right==BqCompatibilityStatus.UNKNOWN)return BqCompatibilityStatus.UNKNOWN;
        if(left==BqCompatibilityStatus.BLOCKED||right==BqCompatibilityStatus.BLOCKED)return BqCompatibilityStatus.BLOCKED;
        return right==null?left:right;
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
    public Map<String, BqCompatibilityStatus> getElementCompatibility() { return elementCompatibility; }

    public String toText() {
        return "quests=" + questCount + "\ntaskTypes=" + taskTypes + "\nrewardTypes=" + rewardTypes
            + "\ntaskCompatibility=" + taskCompatibility + "\nrewardCompatibility=" + rewardCompatibility
            + "\nelementCompatibility=" + elementCompatibility
            + "\nunknownTaskTypes=" + unknownTaskTypes + "\nunknownRewardTypes=" + unknownRewardTypes + "\n";
    }
}
