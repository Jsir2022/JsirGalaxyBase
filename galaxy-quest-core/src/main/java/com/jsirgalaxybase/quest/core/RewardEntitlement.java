package com.jsirgalaxybase.quest.core;

import java.util.Objects;
import java.util.UUID;

public final class RewardEntitlement {
    private final String entitlementKey;
    private final ParticipantId participantId;
    private final UUID questId;
    private final int questVersion;
    private final RewardDefinition reward;
    private final int cycle;
    private final boolean deliveryRequested;

    public RewardEntitlement(UUID playerId, QuestDefinition quest, RewardDefinition reward) {
        this(ParticipantId.player(playerId), quest, reward, 0);
    }

    public RewardEntitlement(ParticipantId participantId, QuestDefinition quest, RewardDefinition reward, int cycle) {
        this(participantId, quest, reward, cycle, true);
    }

    public RewardEntitlement(ParticipantId participantId, QuestDefinition quest, RewardDefinition reward, int cycle,
        boolean deliveryRequested) {
        this.participantId = Objects.requireNonNull(participantId, "participantId");
        this.questId = quest.getId();
        this.questVersion = quest.getVersion();
        this.reward = Objects.requireNonNull(reward, "reward");
        this.cycle = cycle;
        this.deliveryRequested = deliveryRequested;
        this.entitlementKey = participantId.asStableKey() + ":" + questId + ":" + questVersion + ":" + cycle
            + ":" + reward.getKey();
    }

    public String getEntitlementKey() { return entitlementKey; }
    public ParticipantId getParticipantId() { return participantId; }
    public UUID getPlayerId() { return participantId.getId(); }
    public UUID getQuestId() { return questId; }
    public int getQuestVersion() { return questVersion; }
    public RewardDefinition getReward() { return reward; }
    public int getCycle() { return cycle; }
    public boolean isDeliveryRequested() { return deliveryRequested; }
    public RewardDeliveryStatus getInitialDeliveryStatus() {
        return deliveryRequested ? RewardDeliveryStatus.PENDING : RewardDeliveryStatus.CLAIMABLE;
    }
}
