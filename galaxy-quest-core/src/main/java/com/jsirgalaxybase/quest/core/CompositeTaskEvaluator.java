package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Chains independent fact handlers for one task type; non-matching delegates leave progress unchanged. */
public final class CompositeTaskEvaluator implements TaskEvaluator {
    private final String typeId;
    private final List<TaskEvaluator> delegates;

    public CompositeTaskEvaluator(String typeId, List<TaskEvaluator> delegates) {
        if (typeId == null || typeId.trim().isEmpty()) throw new IllegalArgumentException("typeId must not be blank");
        if (delegates == null || delegates.isEmpty()) throw new IllegalArgumentException("delegates must not be empty");
        this.typeId = typeId;
        List<TaskEvaluator> copy = new ArrayList<TaskEvaluator>(delegates.size());
        for (TaskEvaluator delegate : delegates) {
            if (delegate == null || !typeId.equals(delegate.getTypeId())) throw new IllegalArgumentException(
                "delegate task type must match composite type");
            copy.add(delegate);
        }
        this.delegates = Collections.unmodifiableList(copy);
    }

    @Override public String getTypeId() { return typeId; }

    @Override
    public TaskProgress evaluate(TaskDefinition definition, TaskProgress current, GameplayFact fact) {
        TaskProgress result = current;
        for (TaskEvaluator delegate : delegates) result = delegate.evaluate(definition, result, fact);
        return result;
    }
}
