package com.jsirgalaxybase.quest.core;

import java.util.UUID;

/** Server-authenticated transition from a claimable quest cycle to queued reward delivery. */
public interface RewardClaimRepository {
    RewardClaimStatus claim(UUID questId, int definitionVersion, int cycle, UUID currentPlayerId, long claimedAt);
}
