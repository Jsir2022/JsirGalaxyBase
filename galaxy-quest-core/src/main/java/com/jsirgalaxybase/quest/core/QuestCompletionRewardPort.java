package com.jsirgalaxybase.quest.core;

import java.util.UUID;

/** Atomically forces a published target quest complete and creates its reward entitlements. */
public interface QuestCompletionRewardPort {
    Result complete(String sourceEntitlementKey, UUID recipientPlayerId, UUID targetQuestId, long completedAt);

    enum Result {
        COMPLETED,
        ALREADY_COMPLETED,
        TARGET_NOT_FOUND,
        TARGET_SCOPE_UNAVAILABLE
    }
}
