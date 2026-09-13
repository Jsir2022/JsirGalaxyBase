package com.jsirgalaxybase.quest.bqimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** One bounded, explainable BQ/Base progress comparison row. */
public final class BqShadowDifference {
    private final UUID playerId;
    private final UUID questId;
    private final BqShadowRelation relation;
    private final List<String> reasons;

    BqShadowDifference(UUID playerId, UUID questId, BqShadowRelation relation, List<String> reasons) {
        this.playerId = playerId;
        this.questId = questId;
        this.relation = relation;
        this.reasons = Collections.unmodifiableList(new ArrayList<String>(reasons));
    }

    public UUID getPlayerId() { return playerId; }
    public UUID getQuestId() { return questId; }
    public BqShadowRelation getRelation() { return relation; }
    public List<String> getReasons() { return reasons; }
}
