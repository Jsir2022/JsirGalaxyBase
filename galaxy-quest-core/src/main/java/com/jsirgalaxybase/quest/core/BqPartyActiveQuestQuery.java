package com.jsirgalaxybase.quest.core;

import java.util.Set;
import java.util.UUID;

/**
 * Reproduces the eligibility portion of BQ's online Party active-quest union for one local-server event.
 * Progress fan-out remains a separate assignment/runtime concern.
 */
public final class BqPartyActiveQuestQuery implements QuestPartyActivityQuery {
    private final String localServerId;
    private final QuestCompletionQuery completions;
    private final QuestRuntimeRepository progress;
    private final QuestOnlinePlayerDirectory onlinePlayers;

    public BqPartyActiveQuestQuery(String localServerId, QuestCompletionQuery completions,
        QuestRuntimeRepository progress, QuestOnlinePlayerDirectory onlinePlayers) {
        if (localServerId == null || localServerId.trim().isEmpty() || completions == null || progress == null
            || onlinePlayers == null) throw new IllegalArgumentException(
                "local server, completion, progress and online-player queries are required");
        this.localServerId = localServerId.trim();
        this.completions = completions;
        this.progress = progress;
        this.onlinePlayers = onlinePlayers;
    }

    @Override
    public boolean hasActiveMember(QuestDefinition definition, GameplayFact fact, ParticipantMembership party) {
        if (definition == null || fact == null || party == null || party.getParticipantId().getType()
            != ParticipantType.PARTY || !party.contains(fact.getPlayerId())
            || !localServerId.equals(fact.getSourceServer())) return false;
        for (UUID member : party.getPlayerIds()) {
            if (onlinePlayers.isOnline(member) && isActiveFor(member, definition, fact.getOccurredAt())) return true;
        }
        return false;
    }

    private boolean isActiveFor(UUID playerId, QuestDefinition definition, long now) {
        ParticipantId player = ParticipantId.player(playerId);
        boolean unlocked = definition.getPrerequisites().isEmpty();
        boolean progressWhileLocked = definition.getBehavior().isProgressWhileLocked();
        if (!unlocked && !progressWhileLocked) {
            Set<UUID> completed = completions.findCompletedQuestIds(player);
            if (completed == null) return false;
            int completedPrerequisites = 0;
            for (UUID prerequisite : definition.getPrerequisites()) if (completed.contains(prerequisite)) {
                completedPrerequisites++;
            }
            unlocked = definition.getPrerequisiteLogic().satisfied(completedPrerequisites,
                definition.getPrerequisites().size());
        }
        // BQ's active cache admits a quest when either branch is true; LOCKED_PROGRESS
        // is not conditional on the member having already completed prerequisites.
        if (!unlocked && !progressWhileLocked) return false;

        QuestProgressSnapshot previous = progress.findProgress(player, definition.getId(), definition.getVersion())
            .orElse(null);
        if (previous == null || previous.getStatus() != QuestStatus.COMPLETED) return true;
        if (!definition.getRepeatPolicy().isRepeatable()) return false;
        int cycle = previous.getCycle();
        if (progress.hasPendingConsumptionSubmission(player, definition.getId(), definition.getVersion(), cycle)
            || progress.hasClaimableRewardEntitlement(player, definition.getId(), definition.getVersion(), cycle)) {
            return false;
        }
        long anchor = progress.findRepeatCycleAnchorMillis(player, definition.getId(), definition.getVersion(), cycle,
            previous.getCompletedAt());
        return definition.getRepeatPolicy().isAvailable(anchor, now);
    }
}
