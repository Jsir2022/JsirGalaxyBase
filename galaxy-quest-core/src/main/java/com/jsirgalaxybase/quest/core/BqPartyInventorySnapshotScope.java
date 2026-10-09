package com.jsirgalaxybase.quest.core;

import java.util.Map;
import java.util.UUID;

/** Explicit server-authored scope for a BQ-compatible aggregate of active party inventories. */
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
}
