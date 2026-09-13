package com.jsirgalaxybase.quest.core;

import java.util.Objects;

/** Scoreboard objective that a published task requires the Minecraft adapter to observe. */
public final class QuestScoreObservation {
    private final String name;
    private final String displayName;
    private final String criteria;

    public QuestScoreObservation(String name, String displayName, String criteria) {
        this.name = require(name, "name");
        this.displayName = displayName == null || displayName.isEmpty() ? this.name : displayName;
        this.criteria = criteria == null || criteria.isEmpty() ? "dummy" : criteria;
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getCriteria() { return criteria; }

    private static String require(String value, String key) {
        String normalized = Objects.requireNonNull(value, key).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(key + " must not be blank");
        return normalized;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof QuestScoreObservation)) return false;
        QuestScoreObservation that = (QuestScoreObservation) other;
        return name.equals(that.name) && displayName.equals(that.displayName) && criteria.equals(that.criteria);
    }

    @Override public int hashCode() { return 31 * (31 * name.hashCode() + displayName.hashCode()) + criteria.hashCode(); }
}
