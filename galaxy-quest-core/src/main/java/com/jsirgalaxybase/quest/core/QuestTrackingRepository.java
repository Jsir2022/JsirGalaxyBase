package com.jsirgalaxybase.quest.core;

import java.util.UUID;

/** Cross-server player preference; implementations must run inside the caller's transaction. */
public interface QuestTrackingRepository {
    boolean toggle(UUID authenticatedPlayerId, UUID questId, long updatedAt);
}
