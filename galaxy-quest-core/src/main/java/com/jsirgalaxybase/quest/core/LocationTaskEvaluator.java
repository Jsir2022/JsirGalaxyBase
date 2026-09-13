package com.jsirgalaxybase.quest.core;

import java.util.Map;

/** Evaluates an authoritative player-position observation using BetterQuesting location semantics. */
public final class LocationTaskEvaluator implements TaskEvaluator {
    private final String typeId;
    private final String factType;

    public LocationTaskEvaluator(String typeId, String factType) {
        this.typeId = typeId;
        this.factType = factType;
    }

    @Override public String getTypeId() { return typeId; }

    @Override
    public TaskProgress evaluate(TaskDefinition definition, TaskProgress current, GameplayFact fact) {
        if (!factType.equals(fact.getTypeId()) || current.isComplete()) return current;
        Map<String, String> parameters = definition.getParameters();
        Map<String, String> attributes = fact.getAttributes();
        String structure = value(parameters, "structure");
        boolean visible = bool(parameters.get("visible"), false);
        String structureEvidence = structure.isEmpty() ? null : attributes.get(structureEvidenceKey(structure));
        String visibilityEvidence = visible ? attributes.get(visibilityEvidenceKey(parameters)) : null;

        // Structure lookup and line-of-sight are platform-derived. Missing evidence is unknown, not false;
        // in particular an inverted task must never complete merely because an adapter omitted evidence.
        if ((!structure.isEmpty() && structureEvidence == null) || (visible && visibilityEvidence == null)) {
            return current;
        }

        boolean matches = integer(attributes.get("dimension"), Integer.MIN_VALUE)
            == integer(parameters.get("dimension"), 0);
        int range = integer(parameters.get("range"), -1);
        if (matches && range > 0) matches = distance(parameters, attributes) <= range;
        int biome = integer(parameters.get("biome"), -1);
        if (matches && biome >= 0) matches = integer(attributes.get("biome"), Integer.MIN_VALUE) == biome;
        if (matches && !structure.isEmpty()) matches = Boolean.parseBoolean(structureEvidence);
        if (matches && visible) matches = Boolean.parseBoolean(visibilityEvidence);
        if (matches == bool(parameters.get("invert"), false)) return current;
        return current.merge(current.getTargets(), true);
    }

    public static String structureEvidenceKey(String structure) {
        return "structurePresent." + structure;
    }

    public static String visibilityEvidenceKey(Map<String, String> parameters) {
        return "visibleTo." + integer(parameters.get("posX"), 0) + ","
            + integer(parameters.get("posY"), 0) + "," + integer(parameters.get("posZ"), 0);
    }

    private static double distance(Map<String, String> parameters, Map<String, String> attributes) {
        double dx = decimal(attributes.get("x"), 0D) - integer(parameters.get("posX"), 0);
        double dy = decimal(attributes.get("y"), 0D) - integer(parameters.get("posY"), 0);
        double dz = decimal(attributes.get("z"), 0D) - integer(parameters.get("posZ"), 0);
        return bool(parameters.get("taxiCabDist"), false)
            ? Math.abs(dx) + Math.abs(dy) + Math.abs(dz)
            : Math.sqrt(dx * dx + dy * dy + dz * dz);
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
