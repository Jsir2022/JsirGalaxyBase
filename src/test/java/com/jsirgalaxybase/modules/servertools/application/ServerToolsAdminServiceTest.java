package com.jsirgalaxybase.modules.servertools.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.jsirgalaxybase.modules.cluster.domain.ServerDescriptor;
import com.jsirgalaxybase.modules.cluster.domain.TeleportTarget;
import com.jsirgalaxybase.modules.servertools.domain.ServerWarp;
import com.jsirgalaxybase.modules.servertools.domain.TeleportActor;
import com.jsirgalaxybase.modules.servertools.port.ServerToolsAdminRepository;

public class ServerToolsAdminServiceTest {

    @Test
    public void localWarpUsesInjectedActorLocationAndNormalizesKey() {
        FakeRepository repository = new FakeRepository();
        ServerToolsAdminService service = new ServerToolsAdminService(repository);
        ServerWarp saved = service.setLocalWarp(actor(), "admin-warp-1", "Hub_Main", "", "cluster entry");

        assertEquals("hub_main", saved.getWarpName());
        assertEquals("hub_main", saved.getDisplayName());
        assertEquals(42D, saved.getTarget().getX(), 0D);
        assertEquals("admin-warp-1", repository.lastRequestId);
    }

    @Test
    public void currentServerCannotBeDisabled() {
        ServerToolsAdminService service = new ServerToolsAdminService(new FakeRepository());
        try {
            service.setServerEnabled(actor(), "admin-server-1", "lobby", false);
        } catch (ServerToolsException expected) {
            assertTrue(expected.getMessage().contains("current server"));
            return;
        }
        throw new AssertionError("Expected local-server disable refusal");
    }

    private static TeleportActor actor() {
        return new TeleportActor("player-uuid", "Maintainer", "lobby",
            new TeleportTarget("lobby", 0, 42, 80, -3, 30F, 0F));
    }

    private static final class FakeRepository implements ServerToolsAdminRepository {
        private final Map<String, ServerWarp> warps = new HashMap<String, ServerWarp>();
        private String lastRequestId;
        @Override public ServerWarp upsertLocalWarp(TeleportActor actor, String requestId, String name, String display, String description) {
            lastRequestId = requestId;
            ServerWarp value = new ServerWarp(name, display, description, actor.getCurrentLocation(), true, Instant.now(), Instant.now());
            warps.put(name, value); return value;
        }
        @Override public boolean deleteWarp(TeleportActor actor, String requestId, String name) { lastRequestId = requestId; return warps.remove(name) != null; }
        @Override public ServerWarp setWarpEnabled(TeleportActor actor, String requestId, String name, boolean enabled) { ServerWarp old = warps.get(name); if (old == null) throw new ServerToolsException("missing"); ServerWarp value = new ServerWarp(old.getWarpName(), old.getDisplayName(), old.getDescription(), old.getTarget(), enabled, old.getCreatedAt(), Instant.now()); warps.put(name, value); return value; }
        @Override public ServerDescriptor setServerEnabled(TeleportActor actor, String requestId, String id, boolean enabled) { return new ServerDescriptor(id, id, null, false, enabled, Instant.now(), Instant.now()); }
        @Override public Optional<ServerWarp> findWarp(String name) { return Optional.ofNullable(warps.get(name)); }
    }
}
