package com.jsirgalaxybase.modules.servertools.port;

import java.util.Optional;

import com.jsirgalaxybase.modules.cluster.domain.ServerDescriptor;
import com.jsirgalaxybase.modules.servertools.domain.ServerWarp;
import com.jsirgalaxybase.modules.servertools.domain.TeleportActor;

/** Mutating system-warp and server-directory operations must append an audit record atomically. */
public interface ServerToolsAdminRepository {
    ServerWarp upsertLocalWarp(TeleportActor actor, String requestId, String warpName, String displayName, String description);
    boolean deleteWarp(TeleportActor actor, String requestId, String warpName);
    ServerWarp setWarpEnabled(TeleportActor actor, String requestId, String warpName, boolean enabled);
    ServerDescriptor setServerEnabled(TeleportActor actor, String requestId, String serverId, boolean enabled);
    Optional<ServerWarp> findWarp(String warpName);
}
