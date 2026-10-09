package com.jsirgalaxybase.quest.core;

/** Parses boolean values from BQ's NBT-derived numeric flags as well as editor string values. */
public final class QuestBooleanParameter {
    private QuestBooleanParameter() {}

    public static boolean parse(String value, boolean fallback) {
        if (value == null) return fallback;
        String normalized = value.trim();
        if ("1".equals(normalized) || "true".equalsIgnoreCase(normalized)) return true;
        if ("0".equals(normalized) || "false".equalsIgnoreCase(normalized)) return false;
        return fallback;
    }
}
