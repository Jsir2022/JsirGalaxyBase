package com.jsirgalaxybase.quest.core;

import java.util.Objects;

public final class RewardDeliveryLease {
    private final String entitlementKey;
    private final ParticipantId participantId;
    private final RewardDefinition reward;
    private final int attempt;
    private final String workerId;
    private final long leaseUntil;

    public RewardDeliveryLease(String entitlementKey, ParticipantId participantId, RewardDefinition reward,
        int attempt, String workerId, long leaseUntil) {
        this.entitlementKey = Objects.requireNonNull(entitlementKey, "entitlementKey");
        this.participantId = Objects.requireNonNull(participantId, "participantId");
        this.reward = Objects.requireNonNull(reward, "reward");
        if (attempt < 1) throw new IllegalArgumentException("attempt must be positive");
        this.attempt = attempt;
        this.workerId = Objects.requireNonNull(workerId, "workerId");
        this.leaseUntil = leaseUntil;
    }

    public String getEntitlementKey() { return entitlementKey; }
    public ParticipantId getParticipantId() { return participantId; }
    public RewardDefinition getReward() { return reward; }
    public int getAttempt() { return attempt; }
    public String getWorkerId() { return workerId; }
    public long getLeaseUntil() { return leaseUntil; }
}
