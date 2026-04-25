package com.jsirgalaxybase.modules.servertools.infrastructure;

import com.jsirgalaxybase.modules.servertools.port.PlayerPermissionPolicy;
import com.jsirgalaxybase.modules.servertools.port.PlayerTeleportRepository;

public class ServerToolsInfrastructure {

    private final PlayerTeleportRepository playerTeleportRepository;
    private final PlayerPermissionPolicy playerPermissionPolicy;

    public ServerToolsInfrastructure(PlayerTeleportRepository playerTeleportRepository,
        PlayerPermissionPolicy playerPermissionPolicy) {
        this.playerTeleportRepository = playerTeleportRepository;
        this.playerPermissionPolicy = playerPermissionPolicy;
    }

    public PlayerTeleportRepository getPlayerTeleportRepository() {
        return playerTeleportRepository;
    }

    public PlayerPermissionPolicy getPlayerPermissionPolicy() {
        return playerPermissionPolicy;
    }
}