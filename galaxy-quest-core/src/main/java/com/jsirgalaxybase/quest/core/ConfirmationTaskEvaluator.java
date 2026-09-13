package com.jsirgalaxybase.quest.core;

public final class ConfirmationTaskEvaluator implements TaskEvaluator {
    private final String typeId;
    private final String factType;

    public ConfirmationTaskEvaluator(String typeId, String factType) {
        this.typeId = typeId;
        this.factType = factType;
    }

    @Override
    public String getTypeId() { return typeId; }

    @Override
    public TaskProgress evaluate(TaskDefinition definition, TaskProgress current, GameplayFact fact) {
        if (!factType.equals(fact.getTypeId())) return current;
        String taskKey = fact.getAttributes().get("taskKey");
        if (!definition.getKey().equals(taskKey)) return current;
        return current.merge(current.getTargets(), true);
    }
}
