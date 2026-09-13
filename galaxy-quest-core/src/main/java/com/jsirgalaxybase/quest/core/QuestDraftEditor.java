package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Platform-neutral editing session for one quest definition.
 *
 * <p>The editor owns mutable working copies, while every value crossing its public boundary remains immutable.
 * UI screens and network handlers must submit these semantic operations rather than mutate a live quest instance.</p>
 */
public final class QuestDraftEditor {
    private final UUID id;
    private int version;
    private String name;
    private String description;
    private QuestLogic prerequisiteLogic;
    private QuestLogic taskLogic;
    private final Set<UUID> prerequisites;
    private final List<TaskDefinition> tasks;
    private final List<RewardDefinition> rewards;
    private RepeatPolicy repeatPolicy;
    private QuestBehavior behavior;

    private QuestDraftEditor(QuestDefinition source, int version) {
        this.id = source.getId();
        this.version = positive(version, "version");
        this.name = source.getName();
        this.description = source.getDescription();
        this.prerequisiteLogic = source.getPrerequisiteLogic();
        this.taskLogic = source.getTaskLogic();
        this.prerequisites = new LinkedHashSet<UUID>(source.getPrerequisites());
        this.tasks = new ArrayList<TaskDefinition>(source.getTasks());
        this.rewards = new ArrayList<RewardDefinition>(source.getRewards());
        this.repeatPolicy = source.getRepeatPolicy();
        this.behavior = source.getBehavior();
    }

    public static QuestDraftEditor edit(QuestDefinition source) {
        return new QuestDraftEditor(Objects.requireNonNull(source, "source"), source.getVersion());
    }

    public static QuestDraftEditor nextVersion(QuestDefinition source) {
        Objects.requireNonNull(source, "source");
        if (source.getVersion() == Integer.MAX_VALUE) throw new IllegalArgumentException("definition version overflow");
        return new QuestDraftEditor(source, source.getVersion() + 1);
    }

    public QuestDraftEditor version(int value) {
        version = positive(value, "version");
        return this;
    }

    public QuestDraftEditor name(String value) {
        name = requireText(value, "name");
        return this;
    }

    public QuestDraftEditor description(String value) {
        description = value == null ? "" : value;
        return this;
    }

    public QuestDraftEditor prerequisiteLogic(QuestLogic value) {
        prerequisiteLogic = Objects.requireNonNull(value, "prerequisiteLogic");
        return this;
    }

    public QuestDraftEditor taskLogic(QuestLogic value) {
        taskLogic = Objects.requireNonNull(value, "taskLogic");
        return this;
    }

    public QuestDraftEditor repeatPolicy(RepeatPolicy value) {
        repeatPolicy = Objects.requireNonNull(value, "repeatPolicy");
        return this;
    }

    public QuestDraftEditor behavior(QuestBehavior value) {
        behavior = Objects.requireNonNull(value, "behavior");
        return this;
    }

    public RepeatPolicy getRepeatPolicy(){return repeatPolicy;}
    public QuestBehavior getBehavior(){return behavior;}

    public QuestDraftEditor addPrerequisite(UUID questId) {
        UUID required = Objects.requireNonNull(questId, "questId");
        if (id.equals(required)) throw new IllegalArgumentException("quest cannot require itself");
        prerequisites.add(required);
        return this;
    }

    public QuestDraftEditor removePrerequisite(UUID questId) {
        prerequisites.remove(Objects.requireNonNull(questId, "questId"));
        return this;
    }

    public QuestDraftEditor addTask(TaskDefinition task) {
        return insertTask(tasks.size(), task);
    }

    public QuestDraftEditor insertTask(int index, TaskDefinition task) {
        TaskDefinition value = Objects.requireNonNull(task, "task");
        requireUniqueTaskKey(value.getKey(), null);
        tasks.add(position(index, tasks.size(), true), value);
        return this;
    }

    public QuestDraftEditor replaceTask(String key, TaskDefinition replacement) {
        int index = taskIndex(key);
        TaskDefinition value = Objects.requireNonNull(replacement, "replacement");
        requireUniqueTaskKey(value.getKey(), key);
        tasks.set(index, value);
        return this;
    }

    public QuestDraftEditor removeTask(String key) {
        tasks.remove(taskIndex(key));
        return this;
    }

    public QuestDraftEditor moveTask(String key, int targetIndex) {
        int source = taskIndex(key);
        TaskDefinition value = tasks.remove(source);
        tasks.add(position(targetIndex, tasks.size(), true), value);
        return this;
    }

    public QuestDraftEditor addReward(RewardDefinition reward) {
        return insertReward(rewards.size(), reward);
    }

    public QuestDraftEditor insertReward(int index, RewardDefinition reward) {
        RewardDefinition value = Objects.requireNonNull(reward, "reward");
        requireUniqueRewardKey(value.getKey(), null);
        rewards.add(position(index, rewards.size(), true), value);
        return this;
    }

    public QuestDraftEditor replaceReward(String key, RewardDefinition replacement) {
        int index = rewardIndex(key);
        RewardDefinition value = Objects.requireNonNull(replacement, "replacement");
        requireUniqueRewardKey(value.getKey(), key);
        rewards.set(index, value);
        return this;
    }

    public QuestDraftEditor removeReward(String key) {
        rewards.remove(rewardIndex(key));
        return this;
    }

    public QuestDraftEditor moveReward(String key, int targetIndex) {
        int source = rewardIndex(key);
        RewardDefinition value = rewards.remove(source);
        rewards.add(position(targetIndex, rewards.size(), true), value);
        return this;
    }

    public QuestDefinition build() {
        return new QuestDefinition(id, version, name, description, prerequisiteLogic, taskLogic, prerequisites, tasks,
            rewards, repeatPolicy, behavior);
    }

    private int taskIndex(String key) {
        String wanted = requireText(key, "key");
        for (int i = 0; i < tasks.size(); i++) if (wanted.equals(tasks.get(i).getKey())) return i;
        throw new IllegalArgumentException("unknown task key: " + wanted);
    }

    private int rewardIndex(String key) {
        String wanted = requireText(key, "key");
        for (int i = 0; i < rewards.size(); i++) if (wanted.equals(rewards.get(i).getKey())) return i;
        throw new IllegalArgumentException("unknown reward key: " + wanted);
    }

    private void requireUniqueTaskKey(String candidate, String replacedKey) {
        for (TaskDefinition task : tasks) {
            if (task.getKey().equals(candidate) && !task.getKey().equals(replacedKey)) {
                throw new IllegalArgumentException("duplicate task key: " + candidate);
            }
        }
    }

    private void requireUniqueRewardKey(String candidate, String replacedKey) {
        for (RewardDefinition reward : rewards) {
            if (reward.getKey().equals(candidate) && !reward.getKey().equals(replacedKey)) {
                throw new IllegalArgumentException("duplicate reward key: " + candidate);
            }
        }
    }

    private static int position(int value, int size, boolean allowEnd) {
        int maximum = allowEnd ? size : size - 1;
        if (value < 0 || value > maximum) throw new IndexOutOfBoundsException("index " + value + " outside 0.." + maximum);
        return value;
    }

    private static int positive(int value, String name) {
        if (value < 1) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
