package com.jsirgalaxybase.modules.servertools.domain;

import java.time.Instant;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;

public class RandomTeleportRecord {

    private final long recordId;
    private final String playerUuid;
    private final String sourceServerId;
    private final TeleportTarget target;
    private final Instant createdAt;

    public RandomTeleportRecord(long recordId, String playerUuid, String sourceServerId, TeleportTarget target,
        Instant createdAt) {
        this.recordId = recordId;
        this.playerUuid = playerUuid;
        this.sourceServerId = sourceServerId;
        this.target = target;
        this.createdAt = createdAt;
    }

    public long getRecordId() {
        return recordId;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getSourceServerId() {
        return sourceServerId;
    }

    public TeleportTarget getTarget() {
        return target;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}