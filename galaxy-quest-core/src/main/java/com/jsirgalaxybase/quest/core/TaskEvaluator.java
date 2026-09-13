package com.jsirgalaxybase.quest.core;

public interface TaskEvaluator {
    String getTypeId();

    TaskProgress evaluate(TaskDefinition definition, TaskProgress current, GameplayFact fact);
}
