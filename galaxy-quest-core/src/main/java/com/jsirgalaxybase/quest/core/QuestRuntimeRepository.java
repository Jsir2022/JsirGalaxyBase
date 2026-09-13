package com.jsirgalaxybase.quest.core;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** PostgreSQL implementations must apply a fact, progress transition and entitlement inserts atomically. */
public interface QuestRuntimeRepository {
    Optional<QuestProgressSnapshot> findProgress(ParticipantId participantId, UUID questId, int definitionVersion);

    /** @return false when the stable fact key was already consumed. */
    boolean recordFactIfAbsent(GameplayFact fact);

    void saveProgress(QuestProgressSnapshot progress);

    void insertEntitlementsIfAbsent(Set<RewardEntitlement> entitlements);
}
