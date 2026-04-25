package com.jsirgalaxybase.modules.cluster.port;

import java.util.List;
import java.util.Optional;

import com.jsirgalaxybase.modules.cluster.domain.ServerDescriptor;

public interface ServerDirectory {

    Optional<ServerDescriptor> findById(String serverId);

    List<ServerDescriptor> listAll();

    ServerDescriptor upsertLocalServer(String serverId, String displayName);
}