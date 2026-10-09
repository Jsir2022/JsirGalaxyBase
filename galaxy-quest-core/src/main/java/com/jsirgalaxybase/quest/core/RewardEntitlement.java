package com.jsirgalaxybase.quest.core;

import java.util.Objects;
import java.util.UUID;

public final class RewardEntitlement {
    private final String entitlementKey;
    private final ParticipantId participantId;
    private final UUID recipientPlayerId;
    private final UUID questId;
    private final int questVersion;
    private final RewardDefinition reward;
    private final int cycle;
    private final boolean deliveryRequested;
    private final boolean suppressed;

    public RewardEntitlement(UUID playerId, QuestDefinition quest, RewardDefinition reward) {
        this(ParticipantId.player(playerId), quest, reward, 0);
    }

    public RewardEntitlement(ParticipantId participantId, QuestDefinition quest, RewardDefinition reward, int cycle) {
        this(participantId, quest, reward, cycle, true);
    }

    public RewardEntitlement(ParticipantId participantId, QuestDefinition quest, RewardDefinition reward, int cycle,
        boolean deliveryRequested) {
        this(participantId, personalRecipient(participantId), quest, reward, cycle, deliveryRequested);
    }

    public RewardEntitlement(ParticipantId participantId, UUID recipientPlayerId, QuestDefinition quest,
        RewardDefinition reward, int cycle, boolean deliveryRequested) {
        this(participantId, recipientPlayerId, quest, reward, cycle, deliveryRequested, false);
    }

    public RewardEntitlement(ParticipantId participantId, UUID recipientPlayerId, QuestDefinition quest,
        RewardDefinition reward, int cycle, boolean deliveryRequested, boolean suppressed) {
        this.participantId = Objects.requireNonNull(participantId, "participantId");
        this.recipientPlayerId = Objects.requireNonNull(recipientPlayerId, "recipientPlayerId");
        this.questId = quest.getId();
        this.questVersion = quest.getVersion();
        this.reward = Objects.requireNonNull(reward, "reward");
        this.cycle = cycle;
        this.deliveryRequested = deliveryRequested;
        this.suppressed = suppressed;
        String recipientPart = participantId.getType() == ParticipantType.PLAYER
            && participantId.getId().equals(recipientPlayerId) ? "" : recipientPlayerId + ":";
        this.entitlementKey = participantId.asStableKey() + ":" + recipientPart + questId + ":"
            + questVersion + ":" + cycle + ":" + reward.getKey();
    }

    public String getEntitlementKey() { return entitlementKey; }
    public ParticipantId getParticipantId() { return participantId; }
    public UUID getPlayerId() { return recipientPlayerId; }
    public UUID getRecipientPlayerId() { return recipientPlayerId; }
    public UUID getQuestId() { return questId; }
    public int getQuestVersion() { return questVersion; }
    public RewardDefinition getReward() { return reward; }
    public int getCycle() { return cycle; }
    public boolean isDeliveryRequested() { return deliveryRequested; }
    public boolean isSuppressed() { return suppressed; }
    public RewardDeliveryStatus getInitialDeliveryStatus() {
        if (suppressed) return RewardDeliveryStatus.SUPPRESSED;
        return deliveryRequested ? RewardDeliveryStatus.PENDING : RewardDeliveryStatus.CLAIMABLE;
    }

    private RewardEntitlement(RewardEntitlement source, boolean suppressed) {
        this.entitlementKey = source.entitlementKey;
        this.participantId = source.participantId;
        this.recipientPlayerId = source.recipientPlayerId;
        this.questId = source.questId;
        this.questVersion = source.questVersion;
        this.reward = source.reward;
        this.cycle = source.cycle;
        this.deliveryRequested = source.deliveryRequested;
        this.suppressed = suppressed;
    }

    public RewardEntitlement withSuppressedDelivery() {
        return suppressed ? this : new RewardEntitlement(this, true);
    }

    private static UUID personalRecipient(ParticipantId participantId) {
        Objects.requireNonNull(participantId, "participantId");
        if (participantId.getType() != ParticipantType.PLAYER) throw new IllegalArgumentException(
            "group reward entitlements require an explicit recipient player");
        return participantId.getId();
    }
}
