package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TaskEvaluatorRegistry {
    private final Map<String, TaskEvaluator> evaluators = new LinkedHashMap<String, TaskEvaluator>();

    public synchronized void register(TaskEvaluator evaluator) {
        Objects.requireNonNull(evaluator, "evaluator");
        String typeId = evaluator.getTypeId();
        if (typeId == null || typeId.trim().isEmpty()) throw new IllegalArgumentException("typeId must not be blank");
        if (evaluators.containsKey(typeId)) throw new IllegalArgumentException("duplicate task evaluator: " + typeId);
        evaluators.put(typeId, evaluator);
    }

    public synchronized TaskEvaluator require(String typeId) {
        TaskEvaluator evaluator = evaluators.get(typeId);
        if (evaluator == null) throw new IllegalArgumentException("unknown task evaluator: " + typeId);
        return evaluator;
    }

    public synchronized Map<String, TaskEvaluator> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<String, TaskEvaluator>(evaluators));
    }
}
