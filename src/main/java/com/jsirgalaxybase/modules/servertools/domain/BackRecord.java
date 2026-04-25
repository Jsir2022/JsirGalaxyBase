package com.jsirgalaxybase.modules.servertools.domain;

import java.time.Instant;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;

public class BackRecord {

    private final String playerUuid;
    private final TeleportKind teleportKind;
    private final TeleportTarget target;
    private final Instant recordedAt;

    public BackRecord(String playerUuid, TeleportKind teleportKind, TeleportTarget target, Instant recordedAt) {
        this.playerUuid = playerUuid;
        this.teleportKind = teleportKind;
        this.target = target;
        this.recordedAt = recordedAt;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public TeleportKind getTeleportKind() {
        return teleportKind;
    }

    public TeleportTarget getTarget() {
        return target;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}