package com.jsirgalaxybase.modules.cluster.port;

import java.time.Instant;
import java.util.Optional;

import com.jsirgalaxybase.modules.cluster.domain.TransferTicket;

public interface TeleportTicketRepository {

    TransferTicket save(TransferTicket ticket);

    TransferTicket update(TransferTicket ticket);

    Optional<TransferTicket> findByRequestId(String requestId);

    Optional<TransferTicket> findActiveForTargetPlayer(String targetServerId, String playerUuid, Instant now);

    int expireActiveTickets(Instant now);
}