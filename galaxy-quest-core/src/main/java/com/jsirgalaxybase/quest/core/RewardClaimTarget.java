package com.jsirgalaxybase.quest.core;

/** Stable server-resolved identity of one recipient's reward batch. */
public final class RewardClaimTarget {
    private final ParticipantId participantId;
    private final int cycle;

    public RewardClaimTarget(ParticipantId participantId, int cycle) {
        if (participantId == null || cycle < 0) throw new IllegalArgumentException("participant and cycle are required");
        this.participantId = participantId;
        this.cycle = cycle;
    }

    public ParticipantId getParticipantId() { return participantId; }
    public int getCycle() { return cycle; }
}
