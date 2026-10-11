package com.jsirgalaxybase.quest.core;

import java.util.Map;
import java.util.UUID;

/** Explicit server-authored scope for BQ-compatible active-party item/fluid inventory aggregates. */
final class BqPartyInventorySnapshotScope {
    static final String PARTY_SNAPSHOT = "inventory.bqPartySnapshot";
    static final String PARTY_ID = "inventory.bqPartyId";

    private BqPartyInventorySnapshotScope() { }

    static boolean isPartySnapshot(GameplayFact fact) {
        return QuestBooleanParameter.parse(fact.getAttributes().get(PARTY_SNAPSHOT), false);
    }

    static boolean hasMetadata(GameplayFact fact) {
        Map<String, String> attributes = fact.getAttributes();
        return attributes.containsKey(PARTY_SNAPSHOT) || attributes.containsKey(PARTY_ID);
    }

    static ParticipantId party(GameplayFact fact) {
        if (!isPartySnapshot(fact)) throw new IllegalArgumentException("fact is not a BQ party inventory snapshot");
        String id = fact.getAttributes().get(PARTY_ID);
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException(
            "BQ party inventory snapshot is missing its party id");
        try {
            return ParticipantId.party(UUID.fromString(id));
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("invalid BQ party inventory snapshot party id", invalid);
        }
    }

    static boolean matches(GameplayFact fact, ParticipantMembership membership) {
        return isPartySnapshot(fact) && membership != null
            && party(fact).equals(membership.getParticipantId());
    }

    static boolean matchesBqPartySnapshot(QuestDefinition definition, GameplayFact fact) {
        return matchesBqPartyRetrievalSnapshot(definition, fact)
            || matchesBqPartyFluidSnapshot(definition, fact);
    }

    static boolean matchesBqPartyRetrievalSnapshot(QuestDefinition definition, GameplayFact fact) {
        if (!isPartySnapshot(fact) || !"galaxy:inventory_observed".equals(fact.getTypeId())) return false;
        for (TaskDefinition task : definition.getTasks()) {
            if (("bq_standard:retrieval".equals(task.getTypeId())
                || "bq_standard:optional_retrieval".equals(task.getTypeId()))
                && "galaxy:inventory_observed".equals(task.getParameters().get("factType"))
                && BqImportedTaskScope.isImported(task) && BqImportedTaskScope.sharesParty(task)
                && !QuestBooleanParameter.parse(task.getParameters().get("consume"), false)) return true;
        }
        return false;
    }

    static boolean matchesBqPartyFluidSnapshot(QuestDefinition definition, GameplayFact fact) {
        if (definition == null || !isPartySnapshot(fact)
            || !"galaxy:fluid_inventory_observed".equals(fact.getTypeId())) return false;
        for (TaskDefinition task : definition.getTasks()) {
            if (matchesBqPartyFluidTask(task, fact)) return true;
        }
        return false;
    }

    static boolean matchesBqPartyFluidTask(TaskDefinition task, GameplayFact fact) {
        return task != null && isPartySnapshot(fact)
            && "galaxy:fluid_inventory_observed".equals(fact.getTypeId())
            && "bq_standard:fluid".equals(task.getTypeId())
            && "galaxy:fluid_inventory_observed".equals(task.getParameters().get("factType"))
            && BqImportedTaskScope.isImported(task) && BqImportedTaskScope.sharesParty(task)
            && !QuestBooleanParameter.parse(task.getParameters().get("consume"), false);
    }

    static boolean isSupportedObservationType(String typeId) {
        return "galaxy:inventory_observed".equals(typeId)
            || "galaxy:fluid_inventory_observed".equals(typeId);
    }
}
