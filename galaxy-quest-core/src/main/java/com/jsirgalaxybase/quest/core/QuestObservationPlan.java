package com.jsirgalaxybase.quest.core;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable aggregate of periodic observations required by the currently published quest definitions. */
public final class QuestObservationPlan {
    private static final ItemVectorTaskEvaluator CRAFTING_STATISTIC_MATCHER = new ItemVectorTaskEvaluator(
        "bq_standard:crafting", "galaxy:item_produced");

    private final boolean inventory;
    private final boolean bqPartyInventory;
    private final boolean fluidInventory;
    private final boolean location;
    private final int meetingRadius;
    private final boolean xp;
    private final boolean playerLockedProgress;
    private final List<QuestScoreObservation> scores;
    private final List<QuestCraftingStatisticObservation> craftingStatistics;
    private final List<TaskDefinition> craftingStatisticTasks;
    private final List<CraftingStatisticsDetectionTarget> automaticCraftingStatisticsTargets;

    public QuestObservationPlan(boolean inventory, boolean fluidInventory, boolean location, int meetingRadius) {
        this(inventory, false, fluidInventory, location, meetingRadius, false,
            Collections.<QuestScoreObservation>emptyList(), Collections.<QuestCraftingStatisticObservation>emptyList());
    }

    public QuestObservationPlan(boolean inventory, boolean fluidInventory, boolean location, int meetingRadius,
        boolean xp, List<QuestScoreObservation> scores) {
        this(inventory, false, fluidInventory, location, meetingRadius, xp, scores,
            Collections.<QuestCraftingStatisticObservation>emptyList());
    }

    public QuestObservationPlan(boolean inventory, boolean fluidInventory, boolean location, int meetingRadius,
        boolean xp, List<QuestScoreObservation> scores,
        List<QuestCraftingStatisticObservation> craftingStatistics) {
        this(inventory, false, fluidInventory, location, meetingRadius, xp, scores, craftingStatistics);
    }

    public QuestObservationPlan(boolean inventory, boolean bqPartyInventory, boolean fluidInventory, boolean location,
        int meetingRadius, boolean xp, List<QuestScoreObservation> scores,
        List<QuestCraftingStatisticObservation> craftingStatistics) {
        this(inventory, bqPartyInventory, fluidInventory, location, meetingRadius, xp, false, scores,
            craftingStatistics);
    }

    public QuestObservationPlan(boolean inventory, boolean bqPartyInventory, boolean fluidInventory, boolean location,
        int meetingRadius, boolean xp, boolean playerLockedProgress, List<QuestScoreObservation> scores,
        List<QuestCraftingStatisticObservation> craftingStatistics) {
        this(inventory, bqPartyInventory, fluidInventory, location, meetingRadius, xp, playerLockedProgress, scores,
            craftingStatistics, Collections.<TaskDefinition>emptyList(),
            Collections.<CraftingStatisticsDetectionTarget>emptyList());
    }

    private QuestObservationPlan(boolean inventory, boolean bqPartyInventory, boolean fluidInventory,
        boolean location, int meetingRadius, boolean xp, boolean playerLockedProgress,
        List<QuestScoreObservation> scores, List<QuestCraftingStatisticObservation> craftingStatistics,
        List<TaskDefinition> craftingStatisticTasks,
        List<CraftingStatisticsDetectionTarget> automaticCraftingStatisticsTargets) {
        this.inventory = inventory;
        this.bqPartyInventory = bqPartyInventory;
        this.fluidInventory = fluidInventory;
        this.location = location;
        this.meetingRadius = Math.max(-1, meetingRadius);
        this.xp = xp;
        this.playerLockedProgress = playerLockedProgress;
        this.scores = Collections.unmodifiableList(new ArrayList<QuestScoreObservation>(scores == null
            ? Collections.<QuestScoreObservation>emptyList() : scores));
        this.craftingStatistics = Collections.unmodifiableList(new ArrayList<QuestCraftingStatisticObservation>(
            craftingStatistics == null ? Collections.<QuestCraftingStatisticObservation>emptyList() : craftingStatistics));
        this.craftingStatisticTasks = Collections.unmodifiableList(new ArrayList<TaskDefinition>(
            craftingStatisticTasks == null ? Collections.<TaskDefinition>emptyList() : craftingStatisticTasks));
        this.automaticCraftingStatisticsTargets = Collections.unmodifiableList(
            new ArrayList<CraftingStatisticsDetectionTarget>(automaticCraftingStatisticsTargets == null
                ? Collections.<CraftingStatisticsDetectionTarget>emptyList() : automaticCraftingStatisticsTargets));
    }

    public static QuestObservationPlan empty() { return new QuestObservationPlan(false, false, false, -1); }

    public static QuestObservationPlan fromPublished(List<StoredQuestDefinition> definitions) {
        boolean inventory = false;
        boolean fluid = false;
        boolean location = false;
        int radius = -1;
        boolean xp = false;
        boolean playerLockedProgress = false;
        boolean bqPartyInventory = false;
        Map<String, QuestScoreObservation> scores = new LinkedHashMap<String, QuestScoreObservation>();
        java.util.LinkedHashMap<String, QuestCraftingStatisticObservation> crafting =
            new java.util.LinkedHashMap<String, QuestCraftingStatisticObservation>();
        List<TaskDefinition> craftingStatisticTasks = new ArrayList<TaskDefinition>();
        List<CraftingStatisticsDetectionTarget> automaticCraftingStatisticsTargets =
            new ArrayList<CraftingStatisticsDetectionTarget>();
        if (definitions != null) for (StoredQuestDefinition stored : definitions) {
            if (stored == null || stored.getLifecycle() != QuestDefinitionLifecycle.PUBLISHED) continue;
            if (stored.getDefinition().getBehavior().isProgressWhileLocked()
                && stored.getDefinition().getBehavior().getParticipantScope() == QuestParticipantScope.PLAYER) {
                playerLockedProgress = true;
            }
            for (TaskDefinition task : stored.getDefinition().getTasks()) {
                String factType = task.getParameters().get("factType");
                boolean consume = QuestBooleanParameter.parse(task.getParameters().get("consume"), false);
                if ("bq_standard:crafting".equals(task.getTypeId())
                    && QuestBooleanParameter.parse(task.getParameters().get("allowCraftedFromStatistics"), false)) {
                    craftingStatisticTasks.add(task);
                    if (task.getParameters().containsKey("bq.raw")) {
                        automaticCraftingStatisticsTargets.add(new CraftingStatisticsDetectionTarget(
                            stored.getDefinition().getId(), stored.getDefinition().getVersion(), task.getKey()));
                    }
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
                if ("galaxy:inventory_observed".equals(factType) && !consume) {
                    inventory = true;
                    if (("bq_standard:retrieval".equals(task.getTypeId())
                        || "bq_standard:optional_retrieval".equals(task.getTypeId()))
                        && task.getParameters().containsKey("bq.raw")
                        && !consume) {
                        bqPartyInventory = true;
                    }
                } else if ("galaxy:fluid_inventory_observed".equals(factType) && !consume) {
                    fluid = true;
                    if ("bq_standard:fluid".equals(task.getTypeId())
                        && task.getParameters().containsKey("bq.raw")) bqPartyInventory = true;
                }
                else if ("galaxy:location_observed".equals(factType)) location = true;
                else if ("galaxy:nearby_entities_observed".equals(factType)) {
                    radius = Math.max(radius, nonNegative(task.getParameters().get("range"), 4));
                } else if ("galaxy:xp_observed".equals(factType)
                    && !QuestBooleanParameter.parse(task.getParameters().get("consume"), false)) {
                    xp = true;
                } else if ("galaxy:score_observed".equals(factType)) {
                    String name = value(task.getParameters(), "subject", "Score");
                    scores.put(name, new QuestScoreObservation(name,
                        value(task.getParameters(), "scoreDisp", name),
                        value(task.getParameters(), "type", "dummy")));
                }
            }
        }
        return new QuestObservationPlan(inventory, bqPartyInventory, fluid, location, radius, xp,
            playerLockedProgress,
            new ArrayList<QuestScoreObservation>(scores.values()),
            new ArrayList<QuestCraftingStatisticObservation>(crafting.values()), craftingStatisticTasks,
            automaticCraftingStatisticsTargets);
    }

    public boolean observesInventory() { return inventory; }
    public boolean observesBqPartyInventory() { return bqPartyInventory; }
    public boolean observesFluidInventory() { return fluidInventory; }
    public boolean observesLocation() { return location; }
    public boolean observesMeeting() { return meetingRadius >= 0; }
    public int getMeetingRadius() { return meetingRadius; }
    public boolean observesXp() { return xp; }
    public boolean observesPlayerLockedProgress() { return playerLockedProgress; }
    public List<QuestScoreObservation> getScores() { return scores; }
    public boolean observesCraftingStatistics() { return !craftingStatistics.isEmpty(); }
    public List<QuestCraftingStatisticObservation> getCraftingStatistics() { return craftingStatistics; }
    public List<CraftingStatisticsDetectionTarget> getAutomaticCraftingStatisticsTargets() {
        return automaticCraftingStatisticsTargets;
    }

    /** Returns only counters whose owning Crafting task would run BQ's automatic Detect for this output. */
    public List<QuestCraftingStatisticObservation> craftingStatisticsForOutput(Map<String, String> output) {
        return craftingStatisticsForOutput(output, null);
    }

    /** Returns targets owned by imported BQ Crafting tasks; these are sampled for every local online Party member. */
    public List<QuestCraftingStatisticObservation> bqPartyCraftingStatisticsForOutput(Map<String, String> output) {
        return craftingStatisticsForOutput(output, Boolean.TRUE);
    }

    /** Returns targets owned only by non-imported tasks; these retain the ordinary event player's own snapshot. */
    public List<QuestCraftingStatisticObservation> otherCraftingStatisticsForOutput(Map<String, String> output) {
        return craftingStatisticsForOutput(output, Boolean.FALSE);
    }

    private List<QuestCraftingStatisticObservation> craftingStatisticsForOutput(Map<String, String> output,
        Boolean importedBq) {
        if (output == null) return Collections.emptyList();
        if (craftingStatisticTasks.isEmpty()) {
            return importedBq == null || !importedBq.booleanValue() ? craftingStatistics
                : Collections.<QuestCraftingStatisticObservation>emptyList();
        }
        Map<String, QuestCraftingStatisticObservation> result = new LinkedHashMap<String,
            QuestCraftingStatisticObservation>();
        for (TaskDefinition task : craftingStatisticTasks) {
            if (importedBq != null && task.getParameters().containsKey("bq.raw") != importedBq.booleanValue()) {
                continue;
            }
            if (!CRAFTING_STATISTIC_MATCHER.matchesCraftingStatisticsTrigger(task, output)) continue;
            int count = count(task.getParameters().get("item.count"));
            for (int index = 0; index < count; index++) {
                String registryName = task.getParameters().get("item." + index + ".registryName");
                if (registryName == null || registryName.trim().isEmpty()) continue;
                int meta = integer(task.getParameters().get("item." + index + ".meta"), 0);
                QuestCraftingStatisticObservation observation = new QuestCraftingStatisticObservation(
                    registryName, meta);
                result.put(registryName.trim() + "#" + meta, observation);
            }
        }
        return Collections.unmodifiableList(new ArrayList<QuestCraftingStatisticObservation>(result.values()));
    }

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
