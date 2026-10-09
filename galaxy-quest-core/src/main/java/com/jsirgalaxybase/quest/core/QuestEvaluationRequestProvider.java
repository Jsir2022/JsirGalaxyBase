package com.jsirgalaxybase.quest.core;

import java.util.List;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/** Called inside the quest transaction so membership, prerequisites and definitions form one consistent plan. */
public interface QuestEvaluationRequestProvider {
    List<QuestEvaluationRequest> requestsFor(GameplayFact fact);

    /**
     * Finds BQ locked-progress quests for this player that may become active after a prerequisite completes.
     * The caller invokes this inside the same runtime transaction after persisting that completion.
     */
    default List<QuestEvaluationRequest> requestsForNewlyUnlockedLockedProgress(ParticipantId player,
        UUID completedQuestId) {
        return Collections.emptyList();
    }

    /** Builds unlock-eligible definitions only for identities that already have persisted locked progress. */
    default List<QuestEvaluationRequest> requestsForLockedProgressReconciliation(ParticipantId player,
        Set<UUID> lockedQuestIds) {
        return Collections.emptyList();
    }
}
