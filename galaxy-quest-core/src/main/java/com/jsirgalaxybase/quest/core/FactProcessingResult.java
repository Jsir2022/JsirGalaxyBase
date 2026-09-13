package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FactProcessingResult {
    private final FactProcessingStatus status;
    private final List<QuestEvaluation> evaluations;

    private FactProcessingResult(FactProcessingStatus status, List<QuestEvaluation> evaluations) {
        this.status = status;
        this.evaluations = Collections.unmodifiableList(new ArrayList<QuestEvaluation>(evaluations));
    }

    public static FactProcessingResult duplicate() {
        return new FactProcessingResult(FactProcessingStatus.DUPLICATE, Collections.<QuestEvaluation>emptyList());
    }

    public static FactProcessingResult ignored() {
        return new FactProcessingResult(FactProcessingStatus.IGNORED, Collections.<QuestEvaluation>emptyList());
    }

    public static FactProcessingResult accepted(List<QuestEvaluation> evaluations) {
        return new FactProcessingResult(FactProcessingStatus.ACCEPTED, evaluations);
    }

    public boolean isAccepted() { return status == FactProcessingStatus.ACCEPTED; }
    public FactProcessingStatus getStatus() { return status; }
    public List<QuestEvaluation> getEvaluations() { return evaluations; }
}
