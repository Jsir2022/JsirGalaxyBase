package com.jsirgalaxybase.quest.core;

/** Identifies BQ event facts whose TaskCrafting/Hunt progress is copied to every party member's player row. */
final class BqPartyTaskProgressScope {
    private static final String CRAFTING = "bq_standard:crafting";
    private static final String LOCATION = "bq_standard:location";
    private static final String MEETING = "bq_standard:meeting";
    private static final String ITEM_PRODUCED = "galaxy:item_produced";
    private static final String NEARBY_ENTITIES_OBSERVED = "galaxy:nearby_entities_observed";

    private BqPartyTaskProgressScope() { }

    static boolean supports(QuestDefinition definition, GameplayFact fact) {
        if (definition == null || fact == null) return false;
        for (TaskDefinition task : definition.getTasks()) {
            if ("bq_standard:hunt".equals(task.getTypeId())
                && fact.getTypeId().equals(task.getParameters().get("factType"))) return true;
            if (isImportedLocationTask(task) && fact.getTypeId().equals(task.getParameters().get("factType"))) {
                return true;
            }
            if (isImportedMeetingTask(task) && fact.getTypeId().equals(task.getParameters().get("factType"))) {
                return true;
            }
        }
        return isCraftingEvent(definition, fact);
    }

    static boolean requiresUnlockedPartyMember(QuestDefinition definition, GameplayFact fact) {
        if (definition == null || fact == null) return false;
        return isCraftingEvent(definition, fact) || isLocationEvent(definition, fact)
            || isMeetingEvent(definition, fact);
    }

    static boolean isLocationEvent(QuestDefinition definition, GameplayFact fact) {
        if (definition == null || fact == null) return false;
        for (TaskDefinition task : definition.getTasks()) {
            if (isImportedLocationTask(task) && fact.getTypeId().equals(task.getParameters().get("factType"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMeetingEvent(QuestDefinition definition, GameplayFact fact) {
        if (definition == null || fact == null) return false;
        for (TaskDefinition task : definition.getTasks()) {
            if (isImportedMeetingTask(task) && fact.getTypeId().equals(task.getParameters().get("factType"))) {
                return true;
            }
        }
        return false;
    }

    static boolean isCraftingEvent(QuestDefinition definition, GameplayFact fact) {
        if (definition == null || fact == null || !ITEM_PRODUCED.equals(fact.getTypeId())) return false;
        String channel = fact.getAttributes().get("channel");
        if (!"craft".equals(channel) && !"smelt".equals(channel) && !"anvil".equals(channel)) return false;
        for (TaskDefinition task : definition.getTasks()) {
            if (!CRAFTING.equals(task.getTypeId()) || !task.getParameters().containsKey("bq.raw")
                || !ITEM_PRODUCED.equals(task.getParameters().get("factType"))) continue;
            boolean enabled = "craft".equals(channel)
                ? QuestBooleanParameter.parse(task.getParameters().get("allowCraft"), true)
                : "smelt".equals(channel)
                    ? QuestBooleanParameter.parse(task.getParameters().get("allowSmelt"), true)
                    : QuestBooleanParameter.parse(task.getParameters().get("allowAnvil"), false);
            if (enabled) return true;
        }
        return false;
    }

    private static boolean isImportedLocationTask(TaskDefinition task) {
        return LOCATION.equals(task.getTypeId()) && task.getParameters().containsKey("bq.raw");
    }

    private static boolean isImportedMeetingTask(TaskDefinition task) {
        return MEETING.equals(task.getTypeId()) && NEARBY_ENTITIES_OBSERVED.equals(
            task.getParameters().get("factType")) && task.getParameters().containsKey("bq.raw");
    }

    static boolean authorizesPlayerRequest(QuestEvaluationRequest request, GameplayFact fact) {
        if (request == null || !supports(request.getDefinition(), fact)) return false;
        ParticipantMembership progress = request.getMembership();
        ParticipantMembership source = request.getFactMembership();
        return progress.getParticipantId().getType() == ParticipantType.PLAYER
            && source.getParticipantId().getType() == ParticipantType.PARTY
            && source.contains(fact.getPlayerId())
            && source.contains(progress.getParticipantId().getId());
    }
}
