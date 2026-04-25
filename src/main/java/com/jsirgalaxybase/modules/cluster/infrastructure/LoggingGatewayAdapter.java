package com.jsirgalaxybase.modules.cluster.infrastructure;

import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.cluster.domain.ServerDescriptor;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicket;
import com.jsirgalaxybase.modules.cluster.port.GatewayAdapter;

public class LoggingGatewayAdapter implements GatewayAdapter {

    @Override
    public GatewayDispatchResult dispatchRemote(EntityPlayerMP livePlayer, TransferTicket ticket,
        ServerDescriptor targetServer) {
        GalaxyBase.LOG.warn(
            "Remote gateway dispatch is not yet wired. ticketId={}, player={}, sourceServer={}, targetServer={}",
            ticket.getTicketId(),
            ticket.getPlayerName(),
            ticket.getSourceServerId(),
            ticket.getTarget().getServerId());
        return GatewayDispatchResult.pendingRemote(
            "Remote gateway adapter is reserved but not wired; a transfer ticket was persisted for follow-up integration",
            ticket);
    }
}