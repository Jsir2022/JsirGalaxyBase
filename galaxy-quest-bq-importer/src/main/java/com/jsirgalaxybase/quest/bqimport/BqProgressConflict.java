package com.jsirgalaxybase.quest.bqimport;

import java.util.UUID;

public final class BqProgressConflict {
    private final UUID playerId;
    private final UUID questId;
    private final BqProgressRelation relation;

    BqProgressConflict(UUID playerId, UUID questId, BqProgressRelation relation) {
        this.playerId = playerId;
        this.questId = questId;
        this.relation = relation;
    }

    public UUID getPlayerId() { return playerId; }
    public UUID getQuestId() { return questId; }
    public BqProgressRelation getRelation() { return relation; }
}
