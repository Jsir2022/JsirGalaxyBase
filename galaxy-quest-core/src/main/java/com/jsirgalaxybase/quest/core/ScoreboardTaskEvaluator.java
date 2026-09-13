package com.jsirgalaxybase.quest.core;

import java.util.Map;

/** Latching BQ scoreboard comparison over a server-authoritative score observation. */
public final class ScoreboardTaskEvaluator implements TaskEvaluator {
    private final String typeId;
    private final String factType;

    public ScoreboardTaskEvaluator(String typeId, String factType) {
        this.typeId = typeId;
        this.factType = factType;
    }

    @Override public String getTypeId() { return typeId; }

    @Override public TaskProgress evaluate(TaskDefinition definition, TaskProgress current, GameplayFact fact) {
        if (current.isComplete() || !factType.equals(fact.getTypeId())) return current;
        Map<String, String> parameters = definition.getParameters();
        if (!value(parameters, "subject", "Score").equals(fact.getAttributes().get("subject"))) return current;
        long actual = number(fact.getAttributes().get("value"), "fact value");
        long target = number(value(parameters, "scoreTarget", "1"), "score target");
        String operation = value(parameters, "operation", "MORE_OR_EQUAL");
        if (!matches(actual, target, operation)) return current;
        return current.merge(current.getTarget(), true);
    }

    static boolean matches(long actual, long target, String operation) {
        if ("EQUAL".equals(operation)) return actual == target;
        if ("LESS_THAN".equals(operation)) return actual < target;
        if ("MORE_THAN".equals(operation)) return actual > target;
        if ("LESS_OR_EQUAL".equals(operation)) return actual <= target;
        if ("NOT".equals(operation)) return actual != target;
        return actual >= target;
    }

    private static long number(String raw, String name) {
        if (raw == null) throw new IllegalArgumentException(name + " is required");
        return Long.parseLong(raw);
    }

    private static String value(Map<String, String> values, String key, String fallback) {
        String value = values.get(key);
        return value == null || value.isEmpty() ? fallback : value;
    }
}
