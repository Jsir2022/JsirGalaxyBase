package com.jsirgalaxybase.modules.servertools.domain;

import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;

public class TeleportActor {

    private final String playerUuid;
    private final String playerName;
    private final String sourceServerId;
    private final TeleportTarget currentLocation;

    public TeleportActor(String playerUuid, String playerName, String sourceServerId, TeleportTarget currentLocation) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.sourceServerId = sourceServerId;
        this.currentLocation = currentLocation;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getSourceServerId() {
        return sourceServerId;
    }

    public TeleportTarget getCurrentLocation() {
        return currentLocation;
    }
}