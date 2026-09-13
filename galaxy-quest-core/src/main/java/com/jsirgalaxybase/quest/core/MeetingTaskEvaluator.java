package com.jsirgalaxybase.quest.core;

import java.util.Map;

/** Evaluates a complete nearby-entity snapshot using BetterQuesting meeting semantics. */
public final class MeetingTaskEvaluator implements TaskEvaluator {
    private final String typeId;
    private final String factType;

    public MeetingTaskEvaluator(String typeId, String factType) {
        this.typeId = typeId;
        this.factType = factType;
    }

    @Override public String getTypeId() { return typeId; }

    @Override
    public TaskProgress evaluate(TaskDefinition definition, TaskProgress current, GameplayFact fact) {
        if (!factType.equals(fact.getTypeId()) || current.isComplete()) return current;
        Map<String, String> parameters = definition.getParameters();
        Map<String, String> attributes = fact.getAttributes();
        int requiredRange = Math.max(0, integer(parameters.get("range"), 4));
        if (decimal(attributes.get("observedRadius"), -1D) < requiredRange) return current;
        int required = Math.max(1, integer(parameters.get("amount"), 1));
        int count = integer(attributes.get("nearbyEntity.count"), 0);
        int matches = 0;
        for (int i = 0; i < count; i++) {
            if (matches(parameters, attributes, i) && ++matches >= required) {
                return current.merge(current.getTargets(), true);
            }
        }
        return current;
    }

    private boolean matches(Map<String, String> parameters, Map<String, String> attributes, int index) {
        String prefix = "nearbyEntity." + index + ".";
        String subject = value(parameters, "subject");
        boolean subtype = bool(parameters.get("subtypes"), true);
        if (subtype) {
            if (!subject.equals(value(attributes, prefix + "subject"))
                && !contains(value(attributes, prefix + "subjectAliases"), subject)) return false;
        } else if (!subject.equals(value(attributes, prefix + "subject"))) return false;
        if (bool(parameters.get("ignoreNBT"), true)) return true;
        String requiredNbt = value(parameters, "targetNBT.portable");
        return requiredNbt.isEmpty() || PortableNbt.decode(requiredNbt).isEmpty()
            || PortableNbtMatcher.matches(requiredNbt, value(attributes, prefix + "nbtPortable"), true);
    }

    private static boolean contains(String values, String expected) {
        if (expected.isEmpty()) return false;
        for (String value : values.split("\\|")) if (expected.equals(value)) return true;
        return false;
    }

    private static String value(Map<String, String> values, String key) {
        String value = values.get(key);
        return value == null ? "" : value;
    }

    private static boolean bool(String raw, boolean fallback) {
        return raw == null ? fallback : Boolean.parseBoolean(raw);
    }

    private static int integer(String raw, int fallback) {
        return raw == null || raw.isEmpty() ? fallback : Integer.parseInt(raw);
    }

    private static double decimal(String raw, double fallback) {
        return raw == null || raw.isEmpty() ? fallback : Double.parseDouble(raw);
    }
}
