package com.jsirgalaxybase.modules.servertools.domain;

import java.time.Instant;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;

public class ServerWarp {

    private final String warpName;
    private final String displayName;
    private final String description;
    private final TeleportTarget target;
    private final boolean enabled;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ServerWarp(String warpName, String displayName, String description, TeleportTarget target, boolean enabled,
        Instant createdAt, Instant updatedAt) {
        this.warpName = warpName;
        this.displayName = displayName;
        this.description = description;
        this.target = target;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getWarpName() {
        return warpName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public TeleportTarget getTarget() {
        return target;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}