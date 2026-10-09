package com.jsirgalaxybase.quest.core;

import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** PostgreSQL implementations must apply a fact, progress transition and entitlement inserts atomically. */
public interface QuestRuntimeRepository {
    Optional<QuestProgressSnapshot> findProgress(ParticipantId participantId, UUID questId, int definitionVersion);

    /** Existing locked progress is the bounded input for periodic BQ-compatible unlock reconciliation. */
    default List<QuestProgressSnapshot> findLockedProgress(ParticipantId participantId) {
        return java.util.Collections.emptyList();
    }

    /**
     * BetterQuesting's isComplete(UUID) remains true across repeat-cycle resets until a full reset.
     * Implementations with durable progress history should preserve that lifetime completion latch.
     */
    default boolean wasEverCompleted(ParticipantId participantId, UUID questId, int definitionVersion) {
        Optional<QuestProgressSnapshot> latest = findProgress(participantId, questId, definitionVersion);
        return latest.isPresent() && latest.get().getStatus() == QuestStatus.COMPLETED;
    }

    /** Must be read under the same transaction/identity lock used for progress evaluation. */
    boolean hasPendingConsumptionSubmission(ParticipantId participantId, UUID questId, int definitionVersion,
        int cycle);

    /** True while the completed cycle still has an explicit, unclaimed reward entitlement. */
    boolean hasClaimableRewardEntitlement(ParticipantId participantId, UUID questId, int definitionVersion, int cycle);

    /** Uses the reward-claim time as the BQ repeat-reset anchor, falling back for auto-claim/no-reward cycles. */
    long findRepeatCycleAnchorMillis(ParticipantId participantId, UUID questId, int definitionVersion, int cycle,
        long fallbackCompletedAt);

    /** @return false when the stable fact key was already consumed. */
    boolean recordFactIfAbsent(GameplayFact fact);

    void saveProgress(QuestProgressSnapshot progress);

    void insertEntitlementsIfAbsent(Set<RewardEntitlement> entitlements);
}
