package com.jsirgalaxybase.modules.land.domain;

import java.util.Objects;

public final class LandProtectionDecision {

    private final LandProtectionAction action;
    private final LandChunkKey chunkKey;
    private final boolean allowed;
    private final LandProtectionReason reason;
    private final PersonalLandTitle title;

    public LandProtectionDecision(LandProtectionAction action, LandChunkKey chunkKey, boolean allowed,
        LandProtectionReason reason, PersonalLandTitle title) {
        this.action = Objects.requireNonNull(action, "action");
        this.chunkKey = Objects.requireNonNull(chunkKey, "chunkKey");
        this.allowed = allowed;
        this.reason = Objects.requireNonNull(reason, "reason");
        this.title = title;
    }

    public LandProtectionAction getAction() {
        return action;
    }

    public LandChunkKey getChunkKey() {
        return chunkKey;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public LandProtectionReason getReason() {
        return reason;
    }

    public PersonalLandTitle getTitle() {
        return title;
    }
}
