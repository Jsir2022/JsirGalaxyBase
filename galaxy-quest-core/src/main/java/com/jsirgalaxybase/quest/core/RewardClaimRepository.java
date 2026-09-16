package com.jsirgalaxybase.quest.core;

import java.util.UUID;
import java.util.OptionalInt;
import java.util.Optional;

/** Server-authenticated transition from a claimable quest cycle to queued reward delivery. */
public interface RewardClaimRepository {
    RewardClaimStatus claim(UUID questId, int definitionVersion, int cycle, UUID currentPlayerId, long claimedAt);
    default RewardClaimStatus claim(RewardClaimTarget target, UUID questId, int definitionVersion,
        UUID currentPlayerId, long claimedAt) {
        return claim(questId, definitionVersion, target.getCycle(), currentPlayerId, claimedAt);
    }
    default Optional<RewardClaimTarget> findNextRecipientTarget(UUID questId, int definitionVersion,
        UUID currentPlayerId) { return Optional.empty(); }
    default OptionalInt findLatestRecipientCycle(UUID questId, int definitionVersion, UUID currentPlayerId) {
        return OptionalInt.empty();
    }
}
