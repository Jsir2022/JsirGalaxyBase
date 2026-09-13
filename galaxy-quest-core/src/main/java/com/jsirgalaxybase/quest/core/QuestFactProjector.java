package com.jsirgalaxybase.quest.core;

import java.util.LinkedHashMap;
import java.util.Map;

public final class QuestFactProjector {
    private final TaskEvaluatorRegistry registry;

    public QuestFactProjector(TaskEvaluatorRegistry registry) {
        this.registry = registry;
    }

    public Map<String, TaskProgress> project(QuestDefinition definition, QuestProgressSnapshot current,
        GameplayFact fact) {
        return project(definition, current, ParticipantMembership.player(current.getPlayerId()), fact);
    }

    public Map<String, TaskProgress> project(QuestDefinition definition, QuestProgressSnapshot current,
        ParticipantMembership membership, GameplayFact fact) {
        if (!membership.getParticipantId().equals(current.getParticipantId())) {
            throw new IllegalArgumentException("membership participant does not match progress owner");
        }
        if (!membership.contains(fact.getPlayerId())) {
            throw new IllegalArgumentException("fact player does not match progress owner");
        }
        Map<String, TaskProgress> result = new LinkedHashMap<String, TaskProgress>();
        for (TaskDefinition task : definition.getTasks()) {
            TaskProgress progress = current.getTasks().get(task.getKey());
            if (progress == null) progress = TaskProgress.empty(task);
            result.put(task.getKey(), registry.require(task.getTypeId()).evaluate(task, progress, fact));
        }
        return result;
    }

}
