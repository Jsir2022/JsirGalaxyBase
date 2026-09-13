package com.jsirgalaxybase.quest.core;

public final class FactCountTaskEvaluator implements TaskEvaluator {
    private final String typeId;

    public FactCountTaskEvaluator(String typeId) {
        this.typeId = typeId;
    }

    @Override
    public String getTypeId() { return typeId; }

    @Override
    public TaskProgress evaluate(TaskDefinition definition, TaskProgress current, GameplayFact fact) {
        String requiredFact = definition.getParameters().get("factType");
        if (requiredFact == null || !requiredFact.equals(fact.getTypeId())) return current;
        String requiredSubject = definition.getParameters().get("subject");
        if (requiredSubject != null && !requiredSubject.equals(fact.getAttributes().get("subject"))) return current;
        long amount = parsePositive(fact.getAttributes().get("amount"), 1L);
        return current.merge(saturatingAdd(current.getValue(), amount), false);
    }

    private static long parsePositive(String raw, long fallback) {
        if (raw == null) return fallback;
        long value = Long.parseLong(raw);
        if (value < 0) throw new IllegalArgumentException("fact amount must not be negative");
        return value;
    }

    private static long saturatingAdd(long left, long right) {
        if (Long.MAX_VALUE - left < right) return Long.MAX_VALUE;
        return left + right;
    }
}
