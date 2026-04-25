package com.jsirgalaxybase.modules.cluster.domain;

import java.time.Instant;

public class ServerDescriptor {

    private final String serverId;
    private final String displayName;
    private final String gatewayEndpoint;
    private final boolean localServer;
    private final boolean enabled;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ServerDescriptor(String serverId, String displayName, String gatewayEndpoint, boolean localServer,
        boolean enabled, Instant createdAt, Instant updatedAt) {
        this.serverId = serverId;
        this.displayName = displayName;
        this.gatewayEndpoint = gatewayEndpoint;
        this.localServer = localServer;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getServerId() {
        return serverId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getGatewayEndpoint() {
        return gatewayEndpoint;
    }

    public boolean isLocalServer() {
        return localServer;
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