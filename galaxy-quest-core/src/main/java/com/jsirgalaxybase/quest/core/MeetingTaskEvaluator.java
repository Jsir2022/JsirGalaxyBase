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
        int observedRadius = integer(attributes.get("observedRadius"), -1);
        if (observedRadius < requiredRange) return current;
        int required = Math.max(1, integer(parameters.get("amount"), 1));
        int count = integer(attributes.get("nearbyEntity.count"), 0);
        int matches = 0;
        for (int i = 0; i < count; i++) {
            if (withinTaskBounds(attributes, i, requiredRange, observedRadius)
                && matches(parameters, attributes, i) && ++matches >= required) {
                return current.merge(current.getTargets(), true);
            }
        }
        return current;
    }

    /**
     * The platform collects one snapshot at the largest published Meeting range. BQ instead performs a
     * separate AABB query for each task, so an oversized shared snapshot must retain and reapply each
     * task's own bounding-box filter. Older exact-radius snapshots remain self-authenticating because
     * every included entity already came from that same AABB query.
     */
    private static boolean withinTaskBounds(Map<String, String> attributes, int index, int range,
        int observedRadius) {
        if (observedRadius == range) return true;
        String prefix = "nearbyEntity." + index + ".";
        double playerMinX = coordinate(attributes, "observer.minX");
        double playerMinY = coordinate(attributes, "observer.minY");
        double playerMinZ = coordinate(attributes, "observer.minZ");
        double playerMaxX = coordinate(attributes, "observer.maxX");
        double playerMaxY = coordinate(attributes, "observer.maxY");
        double playerMaxZ = coordinate(attributes, "observer.maxZ");
        double entityMinX = coordinate(attributes, prefix + "minX");
        double entityMinY = coordinate(attributes, prefix + "minY");
        double entityMinZ = coordinate(attributes, prefix + "minZ");
        double entityMaxX = coordinate(attributes, prefix + "maxX");
        double entityMaxY = coordinate(attributes, prefix + "maxY");
        double entityMaxZ = coordinate(attributes, prefix + "maxZ");
        if (!validBounds(playerMinX, playerMinY, playerMinZ, playerMaxX, playerMaxY, playerMaxZ)
            || !validBounds(entityMinX, entityMinY, entityMinZ, entityMaxX, entityMaxY, entityMaxZ)) return false;
        double expansion = range;
        return entityMinX < playerMaxX + expansion && entityMaxX > playerMinX - expansion
            && entityMinY < playerMaxY + expansion && entityMaxY > playerMinY - expansion
            && entityMinZ < playerMaxZ + expansion && entityMaxZ > playerMinZ - expansion;
    }

    private static double coordinate(Map<String, String> attributes, String key) {
        String raw = attributes.get(key);
        if (raw == null || raw.isEmpty()) return Double.NaN;
        try {
            return decimal(raw, Double.NaN);
        } catch (NumberFormatException invalid) {
            return Double.NaN;
        }
    }

    private static boolean validBounds(double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ) {
        return finite(minX) && finite(minY) && finite(minZ) && finite(maxX) && finite(maxY) && finite(maxZ)
            && minX <= maxX && minY <= maxY && minZ <= maxZ;
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
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
        return QuestBooleanParameter.parse(raw, fallback);
    }

    private static int integer(String raw, int fallback) {
        return raw == null || raw.isEmpty() ? fallback : Integer.parseInt(raw);
    }

    private static double decimal(String raw, double fallback) {
        return raw == null || raw.isEmpty() ? fallback : Double.parseDouble(raw);
    }
}
