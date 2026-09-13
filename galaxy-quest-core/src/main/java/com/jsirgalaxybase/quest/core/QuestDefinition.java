package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class QuestDefinition {
    private final UUID id;
    private final int version;
    private final String name;
    private final String description;
    private final QuestLogic prerequisiteLogic;
    private final QuestLogic taskLogic;
    private final Set<UUID> prerequisites;
    private final List<TaskDefinition> tasks;
    private final List<RewardDefinition> rewards;
    private final RepeatPolicy repeatPolicy;
    private final QuestBehavior behavior;

    public QuestDefinition(UUID id, int version, String name, String description, QuestLogic prerequisiteLogic,
        QuestLogic taskLogic, Set<UUID> prerequisites, List<TaskDefinition> tasks, List<RewardDefinition> rewards) {
        this(id, version, name, description, prerequisiteLogic, taskLogic, prerequisites, tasks, rewards,
            RepeatPolicy.never(), QuestBehavior.DEFAULT);
    }

    public QuestDefinition(UUID id, int version, String name, String description, QuestLogic prerequisiteLogic,
        QuestLogic taskLogic, Set<UUID> prerequisites, List<TaskDefinition> tasks, List<RewardDefinition> rewards,
        RepeatPolicy repeatPolicy) {
        this(id, version, name, description, prerequisiteLogic, taskLogic, prerequisites, tasks, rewards, repeatPolicy,
            QuestBehavior.DEFAULT);
    }

    public QuestDefinition(UUID id, int version, String name, String description, QuestLogic prerequisiteLogic,
        QuestLogic taskLogic, Set<UUID> prerequisites, List<TaskDefinition> tasks, List<RewardDefinition> rewards,
        RepeatPolicy repeatPolicy, QuestBehavior behavior) {
        this.id = Objects.requireNonNull(id, "id");
        if (version < 1) throw new IllegalArgumentException("version must be positive");
        this.version = version;
        this.name = requireText(name, "name");
        this.description = description == null ? "" : description;
        this.prerequisiteLogic = Objects.requireNonNull(prerequisiteLogic, "prerequisiteLogic");
        this.taskLogic = Objects.requireNonNull(taskLogic, "taskLogic");
        this.prerequisites = Collections.unmodifiableSet(new LinkedHashSet<UUID>(prerequisites == null
            ? Collections.<UUID>emptySet() : prerequisites));
        this.tasks = Collections.unmodifiableList(new ArrayList<TaskDefinition>(tasks == null
            ? Collections.<TaskDefinition>emptyList() : tasks));
        this.rewards = Collections.unmodifiableList(new ArrayList<RewardDefinition>(rewards == null
            ? Collections.<RewardDefinition>emptyList() : rewards));
        this.repeatPolicy = Objects.requireNonNull(repeatPolicy, "repeatPolicy");
        this.behavior = Objects.requireNonNull(behavior, "behavior");
    }

    public UUID getId() { return id; }
    public int getVersion() { return version; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public QuestLogic getPrerequisiteLogic() { return prerequisiteLogic; }
    public QuestLogic getTaskLogic() { return taskLogic; }
    public Set<UUID> getPrerequisites() { return prerequisites; }
    public List<TaskDefinition> getTasks() { return tasks; }
    public List<RewardDefinition> getRewards() { return rewards; }
    public RepeatPolicy getRepeatPolicy() { return repeatPolicy; }
    public QuestBehavior getBehavior() { return behavior; }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
