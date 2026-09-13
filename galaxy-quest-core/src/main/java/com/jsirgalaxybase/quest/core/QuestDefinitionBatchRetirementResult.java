package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QuestDefinitionBatchRetirementResult {
    public enum Status { SUCCESS, FORBIDDEN, INVALID, NOT_FOUND, UNSAFE, CONFLICT }
    private final Status status;
    private final QuestDefinitionBatchImpactReport impact;
    private final List<StoredQuestDefinition> retired;

    private QuestDefinitionBatchRetirementResult(Status status, QuestDefinitionBatchImpactReport impact,
        List<StoredQuestDefinition> retired) {
        this.status = status; this.impact = impact;
        this.retired = retired == null ? Collections.<StoredQuestDefinition>emptyList()
            : Collections.unmodifiableList(new ArrayList<StoredQuestDefinition>(retired));
    }
    public static QuestDefinitionBatchRetirementResult status(Status status) {
        return new QuestDefinitionBatchRetirementResult(status, null, null);
    }
    public static QuestDefinitionBatchRetirementResult unsafe(QuestDefinitionBatchImpactReport impact) {
        return new QuestDefinitionBatchRetirementResult(Status.UNSAFE, impact, null);
    }
    public static QuestDefinitionBatchRetirementResult success(List<StoredQuestDefinition> retired) {
        return new QuestDefinitionBatchRetirementResult(Status.SUCCESS, null, retired);
    }
    public Status getStatus() { return status; }
    public QuestDefinitionBatchImpactReport getImpact() { return impact; }
    public List<StoredQuestDefinition> getRetired() { return retired; }
}
