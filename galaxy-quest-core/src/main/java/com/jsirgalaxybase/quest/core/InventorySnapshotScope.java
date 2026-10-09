package com.jsirgalaxybase.quest.core;

import java.util.Map;
import java.util.UUID;

/** Server-authored scope metadata for an aggregate active-member inventory observation. */
final class InventorySnapshotScope {
    static final String GROUP_SNAPSHOT = "inventory.groupSnapshot";
    static final String PARTICIPANT_TYPE = "inventory.participantType";
    static final String PARTICIPANT_ID = "inventory.participantId";

    private InventorySnapshotScope() { }

    static boolean isGroupSnapshot(GameplayFact fact) {
        return QuestBooleanParameter.parse(fact.getAttributes().get(GROUP_SNAPSHOT), false);
    }

    static boolean hasScopeMetadata(GameplayFact fact) {
        Map<String, String> attributes = fact.getAttributes();
        return attributes.containsKey(GROUP_SNAPSHOT) || attributes.containsKey(PARTICIPANT_TYPE)
            || attributes.containsKey(PARTICIPANT_ID);
    }

    static ParticipantId participant(GameplayFact fact) {
        Map<String, String> attributes = fact.getAttributes();
        if (!isGroupSnapshot(fact) || !attributes.containsKey(PARTICIPANT_TYPE)
            || !attributes.containsKey(PARTICIPANT_ID)) {
            throw new IllegalArgumentException("group inventory snapshot is missing participant scope");
        }
        try {
            ParticipantType type = ParticipantType.valueOf(attributes.get(PARTICIPANT_TYPE));
            if (type == ParticipantType.PLAYER) throw new IllegalArgumentException(
                "group inventory snapshot cannot target a player participant");
            return new ParticipantId(type, UUID.fromString(attributes.get(PARTICIPANT_ID)));
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("invalid group inventory snapshot participant", invalid);
        }
    }

    static boolean matches(GameplayFact fact, ParticipantId participantId) {
        return isGroupSnapshot(fact) && participantId.equals(participant(fact));
    }
}
