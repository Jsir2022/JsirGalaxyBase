package com.jsirgalaxybase.modules.servertools.domain;

import java.time.Instant;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;

public class PlayerHome {

    private final String playerUuid;
    private final String homeName;
    private final TeleportTarget target;
    private final Instant createdAt;
    private final Instant updatedAt;

    public PlayerHome(String playerUuid, String homeName, TeleportTarget target, Instant createdAt, Instant updatedAt) {
        this.playerUuid = playerUuid;
        this.homeName = homeName;
        this.target = target;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getHomeName() {
        return homeName;
    }

    public TeleportTarget getTarget() {
        return target;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}