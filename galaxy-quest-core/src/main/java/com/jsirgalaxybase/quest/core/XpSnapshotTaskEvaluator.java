package com.jsirgalaxybase.quest.core;

import java.util.Collections;

/** BQ non-consuming XP task; the absolute player XP snapshot may rise or fall until completion. */
public final class XpSnapshotTaskEvaluator implements TaskEvaluator {
    private final String typeId;
    private final String factType;

    public XpSnapshotTaskEvaluator(String typeId, String factType) {
        this.typeId = typeId;
        this.factType = factType;
    }

    @Override public String getTypeId() { return typeId; }

    @Override public TaskProgress evaluate(TaskDefinition definition, TaskProgress current, GameplayFact fact) {
        if (!factType.equals(fact.getTypeId()) || Boolean.parseBoolean(definition.getParameters().get("consume"))) {
            return current;
        }
        String raw = fact.getAttributes().get("value");
        if (raw == null) throw new IllegalArgumentException("XP fact value is required");
        long value = Long.parseLong(raw);
        if (value < 0L) throw new IllegalArgumentException("XP fact value must not be negative");
        return current.replaceIncomplete(Collections.singletonList(value));
    }
}
