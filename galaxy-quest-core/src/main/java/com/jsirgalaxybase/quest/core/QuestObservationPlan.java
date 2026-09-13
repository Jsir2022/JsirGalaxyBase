package com.jsirgalaxybase.quest.core;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable aggregate of periodic observations required by the currently published quest definitions. */
public final class QuestObservationPlan {
    private final boolean inventory;
    private final boolean fluidInventory;
    private final boolean location;
    private final int meetingRadius;
    private final boolean xp;
    private final List<QuestScoreObservation> scores;
    private final List<QuestCraftingStatisticObservation> craftingStatistics;

    public QuestObservationPlan(boolean inventory, boolean fluidInventory, boolean location, int meetingRadius) {
        this(inventory, fluidInventory, location, meetingRadius, false,
            Collections.<QuestScoreObservation>emptyList());
    }

    public QuestObservationPlan(boolean inventory, boolean fluidInventory, boolean location, int meetingRadius,
        boolean xp, List<QuestScoreObservation> scores) {
        this(inventory, fluidInventory, location, meetingRadius, xp, scores,
            Collections.<QuestCraftingStatisticObservation>emptyList());
    }

    public QuestObservationPlan(boolean inventory, boolean fluidInventory, boolean location, int meetingRadius,
        boolean xp, List<QuestScoreObservation> scores,
        List<QuestCraftingStatisticObservation> craftingStatistics) {
        this.inventory = inventory;
        this.fluidInventory = fluidInventory;
        this.location = location;
        this.meetingRadius = Math.max(-1, meetingRadius);
        this.xp = xp;
        this.scores = Collections.unmodifiableList(new ArrayList<QuestScoreObservation>(scores == null
            ? Collections.<QuestScoreObservation>emptyList() : scores));
        this.craftingStatistics = Collections.unmodifiableList(new ArrayList<QuestCraftingStatisticObservation>(
            craftingStatistics == null ? Collections.<QuestCraftingStatisticObservation>emptyList() : craftingStatistics));
    }

    public static QuestObservationPlan empty() { return new QuestObservationPlan(false, false, false, -1); }

    public static QuestObservationPlan fromPublished(List<StoredQuestDefinition> definitions) {
        boolean inventory = false;
        boolean fluid = false;
        boolean location = false;
        int radius = -1;
        boolean xp = false;
        Map<String, QuestScoreObservation> scores = new LinkedHashMap<String, QuestScoreObservation>();
        java.util.LinkedHashMap<String, QuestCraftingStatisticObservation> crafting =
            new java.util.LinkedHashMap<String, QuestCraftingStatisticObservation>();
        if (definitions != null) for (StoredQuestDefinition stored : definitions) {
            if (stored == null || stored.getLifecycle() != QuestDefinitionLifecycle.PUBLISHED) continue;
            for (TaskDefinition task : stored.getDefinition().getTasks()) {
                String factType = task.getParameters().get("factType");
                boolean consume = Boolean.parseBoolean(task.getParameters().get("consume"));
                if ("bq_standard:crafting".equals(task.getTypeId())
                    && Boolean.parseBoolean(task.getParameters().get("allowCraftedFromStatistics"))) {
                    int count = count(task.getParameters().get("item.count"));
                    for (int i = 0; i < count; i++) {
                        String name = task.getParameters().get("item." + i + ".registryName");
                        if (name == null || name.trim().isEmpty()) continue;
                        int meta = integer(task.getParameters().get("item." + i + ".meta"), 0);
                        QuestCraftingStatisticObservation observation =
                            new QuestCraftingStatisticObservation(name, meta);
                        crafting.put(name + "#" + meta, observation);
                    }
                }
                if ("galaxy:inventory_observed".equals(factType) && !consume) inventory = true;
                else if ("galaxy:fluid_inventory_observed".equals(factType) && !consume) fluid = true;
                else if ("galaxy:location_observed".equals(factType)) location = true;
                else if ("galaxy:nearby_entities_observed".equals(factType)) {
                    radius = Math.max(radius, nonNegative(task.getParameters().get("range"), 4));
                } else if ("galaxy:xp_observed".equals(factType)
                    && !Boolean.parseBoolean(task.getParameters().get("consume"))) {
                    xp = true;
                } else if ("galaxy:score_observed".equals(factType)) {
                    String name = value(task.getParameters(), "subject", "Score");
                    scores.put(name, new QuestScoreObservation(name,
                        value(task.getParameters(), "scoreDisp", name),
                        value(task.getParameters(), "type", "dummy")));
                }
            }
        }
        return new QuestObservationPlan(inventory, fluid, location, radius, xp,
            new ArrayList<QuestScoreObservation>(scores.values()),
            new ArrayList<QuestCraftingStatisticObservation>(crafting.values()));
    }

    public boolean observesInventory() { return inventory; }
    public boolean observesFluidInventory() { return fluidInventory; }
    public boolean observesLocation() { return location; }
    public boolean observesMeeting() { return meetingRadius >= 0; }
    public int getMeetingRadius() { return meetingRadius; }
    public boolean observesXp() { return xp; }
    public List<QuestScoreObservation> getScores() { return scores; }
    public boolean observesCraftingStatistics() { return !craftingStatistics.isEmpty(); }
    public List<QuestCraftingStatisticObservation> getCraftingStatistics() { return craftingStatistics; }

    private static int count(String raw) {
        if (raw == null || raw.isEmpty()) return 0;
        int value = Integer.parseInt(raw);
        return Math.max(0, value);
    }

    private static int integer(String raw, int fallback) {
        if (raw == null || raw.isEmpty()) return fallback;
        return Integer.parseInt(raw);
    }

    private static int nonNegative(String raw, int fallback) {
        int value = raw == null || raw.isEmpty() ? fallback : Integer.parseInt(raw);
        return Math.max(0, value);
    }

    private static String value(Map<String, String> values, String key, String fallback) {
        String value = values.get(key);
        return value == null || value.isEmpty() ? fallback : value;
    }
}
