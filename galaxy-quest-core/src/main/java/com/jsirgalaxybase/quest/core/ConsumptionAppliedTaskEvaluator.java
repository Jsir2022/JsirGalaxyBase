package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Advances a consuming vector task only after its external deduction has durable evidence. */
public final class ConsumptionAppliedTaskEvaluator implements TaskEvaluator {
    private final String typeId;
    private final String kind;

    public ConsumptionAppliedTaskEvaluator(String typeId, String kind) {
        this.typeId = typeId;
        this.kind = kind;
    }

    @Override public String getTypeId() { return typeId; }

    @Override
    public TaskProgress evaluate(TaskDefinition definition, TaskProgress current, GameplayFact fact) {
        if (!"galaxy:consumption_applied".equals(fact.getTypeId())
            || !bool(definition.getParameters().get("consume"), false)
            || !definition.getKey().equals(fact.getAttributes().get("taskKey"))
            || !kind.equals(fact.getAttributes().get("kind"))) return current;
        int count = integer(fact.getAttributes().get("value.count"), -1);
        if (count != current.getValues().size()) throw new IllegalArgumentException("consumption vector width mismatch");
        List<Long> deltas = new ArrayList<Long>(count);
        for (int i = 0; i < count; i++) deltas.add(positive(fact.getAttributes().get("value." + i)));
        return current.advance(deltas);
    }

    private static boolean bool(String raw, boolean fallback) {
        return raw == null ? fallback : Boolean.parseBoolean(raw);
    }
    private static int integer(String raw, int fallback) { return raw == null ? fallback : Integer.parseInt(raw); }
    private static long positive(String raw) {
        long value = raw == null ? 0L : Long.parseLong(raw);
        if (value < 0) throw new IllegalArgumentException("consumption delta must not be negative");
        return value;
    }
}
