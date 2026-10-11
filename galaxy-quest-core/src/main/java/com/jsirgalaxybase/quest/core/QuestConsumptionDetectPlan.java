package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Server-derived ordered set of incomplete, currently supported consuming Tasks. */
public final class QuestConsumptionDetectPlan {
    private final QuestEvaluationRequest evaluationRequest;
    private final int cycle;
    private final List<String> taskKeys;

    public QuestConsumptionDetectPlan(QuestEvaluationRequest evaluationRequest, int cycle, List<String> taskKeys) {
        this.evaluationRequest = Objects.requireNonNull(evaluationRequest, "evaluationRequest");
        if (cycle < 0) throw new IllegalArgumentException("cycle must not be negative");
        this.cycle = cycle;
        if (taskKeys == null) throw new IllegalArgumentException("taskKeys are required");
        this.taskKeys = Collections.unmodifiableList(new ArrayList<String>(taskKeys));
    }

    public QuestEvaluationRequest getEvaluationRequest() { return evaluationRequest; }
    public int getCycle() { return cycle; }
    public List<String> getTaskKeys() { return taskKeys; }
}
