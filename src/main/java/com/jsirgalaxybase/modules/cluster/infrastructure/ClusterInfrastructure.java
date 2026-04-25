package com.jsirgalaxybase.modules.cluster.infrastructure;

import com.jsirgalaxybase.modules.cluster.application.ClusterTeleportService;
import com.jsirgalaxybase.modules.cluster.application.PlayerArrivalRestoreService;
import com.jsirgalaxybase.modules.cluster.port.GatewayAdapter;
import com.jsirgalaxybase.modules.cluster.port.ServerDirectory;
import com.jsirgalaxybase.modules.cluster.port.TeleportTicketRepository;
import com.jsirgalaxybase.modules.core.banking.infrastructure.jdbc.JdbcConnectionManager;

public class ClusterInfrastructure {

    private final ServerDirectory serverDirectory;
    private final TeleportTicketRepository teleportTicketRepository;
    private final GatewayAdapter gatewayAdapter;
    private final ClusterTeleportService clusterTeleportService;
    private final PlayerArrivalRestoreService playerArrivalRestoreService;
    private final JdbcConnectionManager sharedConnectionManager;

    public ClusterInfrastructure(ServerDirectory serverDirectory, TeleportTicketRepository teleportTicketRepository,
        GatewayAdapter gatewayAdapter, ClusterTeleportService clusterTeleportService,
        PlayerArrivalRestoreService playerArrivalRestoreService,
        JdbcConnectionManager sharedConnectionManager) {
        this.serverDirectory = serverDirectory;
        this.teleportTicketRepository = teleportTicketRepository;
        this.gatewayAdapter = gatewayAdapter;
        this.clusterTeleportService = clusterTeleportService;
        this.playerArrivalRestoreService = playerArrivalRestoreService;
        this.sharedConnectionManager = sharedConnectionManager;
    }

    public ServerDirectory getServerDirectory() {
        return serverDirectory;
    }

    public TeleportTicketRepository getTeleportTicketRepository() {
        return teleportTicketRepository;
    }

    public GatewayAdapter getGatewayAdapter() {
        return gatewayAdapter;
    }

    public ClusterTeleportService getClusterTeleportService() {
        return clusterTeleportService;
    }

    public PlayerArrivalRestoreService getPlayerArrivalRestoreService() {
        return playerArrivalRestoreService;
    }

    public JdbcConnectionManager getSharedConnectionManager() {
        return sharedConnectionManager;
    }
}