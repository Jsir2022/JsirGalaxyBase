package com.jsirgalaxybase.quest.core;

import java.util.Objects;

/** Validated command reward passed only to an entitlement-idempotent executor. */
public final class CommandRewardInvocation {
    private final String entitlementKey;
    private final ParticipantId participantId;
    private final String command;
    private final boolean viaPlayer;

    public CommandRewardInvocation(String entitlementKey, ParticipantId participantId, String command,
        boolean viaPlayer) {
        this.entitlementKey = Objects.requireNonNull(entitlementKey, "entitlementKey");
        this.participantId = Objects.requireNonNull(participantId, "participantId");
        this.command = Objects.requireNonNull(command, "command");
        this.viaPlayer = viaPlayer;
    }
    public String getEntitlementKey() { return entitlementKey; }
    public ParticipantId getParticipantId() { return participantId; }
    public String getCommand() { return command; }
    public boolean isViaPlayer() { return viaPlayer; }
}
