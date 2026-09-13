package com.jsirgalaxybase.quest.core;

/** Supplies a cached plan; implementations decide when published definitions need refreshing. */
public interface QuestObservationPlanProvider {
    QuestObservationPlan current();
}
