package com.jsirgalaxybase.modules.servertools.application;

import java.util.Optional;

import com.jsirgalaxybase.modules.cluster.domain.ServerDescriptor;
import com.jsirgalaxybase.modules.servertools.domain.ServerWarp;
import com.jsirgalaxybase.modules.servertools.domain.TeleportActor;
import com.jsirgalaxybase.modules.servertools.port.ServerToolsAdminRepository;

/** Server-side administrative facade. Command permission is checked before this service is called. */
public final class ServerToolsAdminService {

    private final ServerToolsAdminRepository repository;

    public ServerToolsAdminService(ServerToolsAdminRepository repository) {
        this.repository = repository;
    }

    public ServerWarp setLocalWarp(TeleportActor actor, String requestId, String warpName, String displayName,
        String description) {
        return repository.upsertLocalWarp(requireActor(actor), requestId, key(warpName, "warpName"),
            empty(displayName) ? key(warpName, "warpName") : text(displayName, "displayName"), optionalText(description));
    }

    public boolean deleteWarp(TeleportActor actor, String requestId, String warpName) {
        return repository.deleteWarp(requireActor(actor), requestId, key(warpName, "warpName"));
    }

    public ServerWarp setWarpEnabled(TeleportActor actor, String requestId, String warpName, boolean enabled) {
        return repository.setWarpEnabled(requireActor(actor), requestId, key(warpName, "warpName"), enabled);
    }

    public ServerDescriptor setServerEnabled(TeleportActor actor, String requestId, String serverId, boolean enabled) {
        TeleportActor resolved = requireActor(actor);
        String normalized = key(serverId, "serverId");
        if (!enabled && normalized.equals(resolved.getSourceServerId())) {
            throw new ServerToolsException("Cannot disable the current server from its own runtime");
        }
        return repository.setServerEnabled(resolved, requestId, normalized, enabled);
    }

    public Optional<ServerWarp> findWarp(String warpName) { return repository.findWarp(key(warpName, "warpName")); }

    private static TeleportActor requireActor(TeleportActor actor) {
        if (actor == null || empty(actor.getPlayerUuid()) || empty(actor.getPlayerName()) || empty(actor.getSourceServerId())
            || actor.getCurrentLocation() == null) throw new IllegalArgumentException("actor must be a live server player");
        return actor;
    }
    private static String key(String value, String field) {
        String normalized = text(value, field).toLowerCase(java.util.Locale.ROOT);
        if (!normalized.matches("[a-z0-9_-]{1,64}")) throw new IllegalArgumentException(field + " must use [a-z0-9_-] and be <= 64 chars");
        return normalized;
    }
    private static String text(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(fallback + " must not be blank");
        if (normalized.length() > 255) throw new IllegalArgumentException(fallback + " is too long");
        return normalized;
    }
    private static String optionalText(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > 255) throw new IllegalArgumentException("description is too long");
        return normalized.isEmpty() ? null : normalized;
    }
    private static boolean empty(String value) { return value == null || value.trim().isEmpty(); }
}
