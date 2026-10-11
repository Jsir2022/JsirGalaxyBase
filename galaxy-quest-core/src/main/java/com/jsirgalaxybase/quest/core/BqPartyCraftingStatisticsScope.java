package com.jsirgalaxybase.quest.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** Separates BQ's online-member personal-stat refresh from its party-wide event progress fan-out. */
public final class BqPartyCraftingStatisticsScope {
    public static final String PERSONAL_ONLINE_SNAPSHOT = "craftStatistics.bqPartyOnlineSnapshot";
    public static final String AUTOMATIC_TASK_SNAPSHOT = "craftStatistics.automaticTaskSnapshot";

    private static final ItemVectorTaskEvaluator CRAFTING_MATCHER = new ItemVectorTaskEvaluator(
        "bq_standard:crafting", "galaxy:item_produced");

    private BqPartyCraftingStatisticsScope() { }

    public static boolean isPersonalOnlineSnapshot(GameplayFact fact) {
        return fact != null && QuestBooleanParameter.parse(fact.getAttributes().get(PERSONAL_ONLINE_SNAPSHOT), false);
    }

    public static boolean isAutomaticTaskSnapshot(GameplayFact fact) {
        return fact != null && QuestBooleanParameter.parse(
            fact.getAttributes().get(AUTOMATIC_TASK_SNAPSHOT), false);
    }

    /** True only for automatic stats facts caused by an output accepted by an imported BQ Crafting task. */
    public static boolean matchesAutomaticPartyActivity(QuestDefinition definition, GameplayFact fact) {
        if (definition == null || fact == null || !"galaxy:item_produced".equals(fact.getTypeId())
            || !"craft-statistics".equals(fact.getAttributes().get("channel"))) return false;

        if (isAutomaticTaskSnapshot(fact)) {
            if (!definition.getId().toString().equals(fact.getAttributes().get("targetQuestId"))
                || !Integer.toString(definition.getVersion()).equals(
                    fact.getAttributes().get("targetQuestVersion"))) return false;
            String targetTaskKey = fact.getAttributes().get("targetTaskKey");
            String registryName = fact.getAttributes().get("registryName");
            if (targetTaskKey == null || registryName == null) return false;
            for (TaskDefinition task : definition.getTasks()) {
                if (!targetTaskKey.equals(task.getKey()) || !"bq_standard:crafting".equals(task.getTypeId())
                    || !BqImportedTaskScope.isImported(task) || !BqImportedTaskScope.sharesParty(task)
                    || !QuestBooleanParameter.parse(task.getParameters().get("allowCraftedFromStatistics"), false)) {
                    continue;
                }
                int count = integer(task.getParameters().get("item.count"));
                for (int index = 0; index < count; index++) {
                    if (registryName.equals(task.getParameters().get("item." + index + ".registryName"))) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (fact.getAttributes().containsKey("targetQuestId")
            || fact.getAttributes().containsKey("targetTaskKey")) return false;

        Map<String, String> output = new LinkedHashMap<String, String>();
        copy(fact.getAttributes(), output, "trigger.channel", "channel");
        copy(fact.getAttributes(), output, "trigger.registryName", "registryName");
        copy(fact.getAttributes(), output, "trigger.meta", "meta");
        copy(fact.getAttributes(), output, "trigger.damageable", "damageable");
        copy(fact.getAttributes(), output, "trigger.oreDictionary", "oreDictionary");
        copy(fact.getAttributes(), output, "trigger.nbtPortable", "nbtPortable");
        if (output.get("channel") == null || output.get("registryName") == null) return false;

        for (TaskDefinition task : definition.getTasks()) {
            if ("bq_standard:crafting".equals(task.getTypeId())
                && BqImportedTaskScope.isImported(task) && BqImportedTaskScope.sharesParty(task)
                && CRAFTING_MATCHER.matchesCraftingStatisticsTrigger(task, output)) return true;
        }
        return false;
    }

    static boolean authorizesLockedPlayerProgress(QuestEvaluationRequest request, GameplayFact fact) {
        if (request == null || !isAutomaticTaskSnapshot(fact)
            || !matchesAutomaticPartyActivity(request.getDefinition(), fact)) return false;
        ParticipantMembership progress = request.getMembership();
        ParticipantMembership source = request.getFactMembership();
        return progress.getParticipantId().getType() == ParticipantType.PLAYER
            && progress.getParticipantId().getId().equals(fact.getPlayerId())
            && source.getParticipantId().getType() == ParticipantType.PARTY
            && source.contains(fact.getPlayerId())
            && source.contains(progress.getParticipantId().getId());
    }

    private static int integer(String raw) {
        if (raw == null || raw.isEmpty()) return 0;
        try { return Math.max(0, Integer.parseInt(raw)); }
        catch (NumberFormatException invalid) { return 0; }
    }

    private static void copy(Map<String, String> source, Map<String, String> target, String sourceKey,
        String targetKey) {
        String value = source.get(sourceKey);
        if (value != null) target.put(targetKey, value);
    }
}
