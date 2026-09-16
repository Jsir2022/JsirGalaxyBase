package com.jsirgalaxybase.quest.core;

import java.util.Optional;
import java.util.UUID;

/** Resolves the authoritative reward entitlement before recording a player's immutable choice. */
public final class AuthenticatedRewardChoiceService {
    private static final String CHOICE_TYPE = "bq_standard:choice";

    private final QuestDefinitionRepository definitions;
    private final QuestRuntimeRepository runtime;
    private final RewardChoiceSelectionRepository choices;
    private final QuestTransaction transaction;

    public AuthenticatedRewardChoiceService(QuestDefinitionRepository definitions, QuestRuntimeRepository runtime,
        RewardChoiceSelectionRepository choices, QuestTransaction transaction) {
        if (definitions == null || runtime == null || choices == null || transaction == null) {
            throw new IllegalArgumentException("dependencies are required");
        }
        this.definitions = definitions;
        this.runtime = runtime;
        this.choices = choices;
        this.transaction = transaction;
    }

    public RewardChoiceSelectionStatus select(final UUID authenticatedPlayerId, final UUID questId,
        final String rewardKey, final int choiceIndex, final long selectedAt) {
        if (authenticatedPlayerId == null || questId == null || rewardKey == null || rewardKey.trim().isEmpty()
            || choiceIndex < 0 || selectedAt < 0L) {
            throw new IllegalArgumentException("authenticated player, quest, reward, choice, and time are required");
        }
        return transaction.inTransaction(() -> {
            Optional<StoredQuestDefinition> stored = definitions.findPublished(questId);
            if (!stored.isPresent()) return RewardChoiceSelectionStatus.NOT_FOUND;
            QuestDefinition quest = stored.get().getDefinition();
            RewardDefinition reward = findChoiceReward(quest, rewardKey);
            if (reward == null) return RewardChoiceSelectionStatus.INVALID;
            RewardChoiceSelectionStatus latest = choices.selectLatest(questId, quest.getVersion(), rewardKey,
                authenticatedPlayerId, choiceIndex, selectedAt);
            if (latest != RewardChoiceSelectionStatus.NOT_FOUND) return latest;
            // Compatibility for entitlement rows created before recipient_player_id existed.
            Optional<QuestProgressSnapshot> progress = runtime.findProgress(
                ParticipantId.player(authenticatedPlayerId), questId, quest.getVersion());
            if (!progress.isPresent()) return RewardChoiceSelectionStatus.NOT_FOUND;
            RewardEntitlement entitlement = new RewardEntitlement(ParticipantId.player(authenticatedPlayerId),
                quest, reward, progress.get().getCycle());
            return choices.select(entitlement.getEntitlementKey(), authenticatedPlayerId, choiceIndex, selectedAt);
        });
    }

    private static RewardDefinition findChoiceReward(QuestDefinition quest, String rewardKey) {
        for (RewardDefinition reward : quest.getRewards()) {
            if (reward.getKey().equals(rewardKey) && CHOICE_TYPE.equals(reward.getTypeId())) return reward;
        }
        return null;
    }
}
