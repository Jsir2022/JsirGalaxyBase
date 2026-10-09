package com.jsirgalaxybase.modules.quest.infrastructure.minecraft;

import com.jsirgalaxybase.quest.core.GameplayFact;

import java.util.UUID;

/** Server-side boundary; implementations resolve applicable published quests and persist the fact transactionally. */
public interface QuestFactSink {
    void accept(GameplayFact fact);

    /** Opens a new online inventory session; implementations may atomically replace stale server ownership. */
    default boolean beginInventorySession(GameplayFact fact) {
        accept(fact);
        return true;
    }

    /** Removes only the matching login session, never a newer session for the same player. */
    default void endInventorySession(UUID playerId, UUID sessionId) { }

    /** Rechecks durable BQ locked-progress state for a server-authenticated player. */
    default void reconcileLockedProgress(UUID playerId, long now) { }
}
