package com.jsirgalaxybase.modules.servertools.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.servertools.domain.BackRecord;
import com.jsirgalaxybase.modules.servertools.domain.PlayerHome;
import com.jsirgalaxybase.modules.servertools.domain.RandomTeleportRecord;
import com.jsirgalaxybase.modules.servertools.domain.ServerWarp;
import com.jsirgalaxybase.modules.servertools.domain.TpaRequest;

public interface PlayerTeleportRepository {

    List<PlayerHome> listHomes(String playerUuid);

    Optional<PlayerHome> findHome(String playerUuid, String homeName);

    PlayerHome saveHome(PlayerHome playerHome);

    boolean deleteHome(String playerUuid, String homeName);

    Optional<BackRecord> findBackRecord(String playerUuid);

    BackRecord saveBackRecord(BackRecord backRecord);

    List<ServerWarp> listWarps();

    Optional<ServerWarp> findWarp(String warpName);

    ServerWarp saveWarp(ServerWarp serverWarp);

    TpaRequest saveTpaRequest(TpaRequest tpaRequest);

    TpaRequest updateTpaRequest(TpaRequest tpaRequest);

    Optional<TpaRequest> findPendingTpaRequest(String requesterPlayerName, String targetPlayerName,
        String targetServerId, Instant now);

    int expirePendingTpaRequests(Instant now);

    RandomTeleportRecord saveRandomTeleportRecord(RandomTeleportRecord randomTeleportRecord);
}