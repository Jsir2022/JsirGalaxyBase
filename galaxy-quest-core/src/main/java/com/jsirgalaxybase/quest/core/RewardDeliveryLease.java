package com.jsirgalaxybase.quest.core;

import java.util.Objects;

public final class RewardDeliveryLease {
    private final String entitlementKey;
    private final ParticipantId participantId;
    private final ParticipantId progressParticipantId;
    private final RewardDefinition reward;
    private final int attempt;
    private final int failureCount;
    private final String workerId;
    private final long leaseUntil;

    public RewardDeliveryLease(String entitlementKey, ParticipantId participantId, RewardDefinition reward,
        int attempt, String workerId, long leaseUntil) {
        this(entitlementKey, participantId, personalRecipient(participantId), reward, attempt, 0, workerId, leaseUntil);
    }

    public RewardDeliveryLease(String entitlementKey, ParticipantId progressParticipantId, java.util.UUID recipient,
        RewardDefinition reward, int attempt, String workerId, long leaseUntil) {
        this(entitlementKey, progressParticipantId, recipient, reward, attempt, 0, workerId, leaseUntil);
    }

    public RewardDeliveryLease(String entitlementKey, ParticipantId progressParticipantId, java.util.UUID recipient,
        RewardDefinition reward, int attempt, int failureCount, String workerId, long leaseUntil) {
        this.entitlementKey = Objects.requireNonNull(entitlementKey, "entitlementKey");
        this.progressParticipantId = Objects.requireNonNull(progressParticipantId, "progressParticipantId");
        this.participantId = ParticipantId.player(Objects.requireNonNull(recipient, "recipient"));
        this.reward = Objects.requireNonNull(reward, "reward");
        if (attempt < 1) throw new IllegalArgumentException("attempt must be positive");
        if (failureCount < 0) throw new IllegalArgumentException("failureCount must not be negative");
        this.attempt = attempt;
        this.failureCount = failureCount;
        this.workerId = Objects.requireNonNull(workerId, "workerId");
        this.leaseUntil = leaseUntil;
    }

    public String getEntitlementKey() { return entitlementKey; }
    public ParticipantId getParticipantId() { return participantId; }
    public ParticipantId getProgressParticipantId() { return progressParticipantId; }
    public RewardDefinition getReward() { return reward; }
    public int getAttempt() { return attempt; }
    public int getFailureCount() { return failureCount; }
    public String getWorkerId() { return workerId; }
    public long getLeaseUntil() { return leaseUntil; }

    private static java.util.UUID personalRecipient(ParticipantId participantId) {
        Objects.requireNonNull(participantId, "participantId");
        if (participantId.getType() != ParticipantType.PLAYER) throw new IllegalArgumentException(
            "group reward delivery leases require an explicit recipient player");
        return participantId.getId();
    }
}
