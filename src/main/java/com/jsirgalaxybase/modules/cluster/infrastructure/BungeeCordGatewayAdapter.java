package com.jsirgalaxybase.modules.cluster.infrastructure;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.jsirgalaxybase.GalaxyBase;
import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.cluster.domain.ServerDescriptor;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicket;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicketStatus;
import com.jsirgalaxybase.modules.cluster.port.GatewayAdapter;

public class BungeeCordGatewayAdapter implements GatewayAdapter {

    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final String CONNECT_SUBCHANNEL = "Connect";

    @Override
    public GatewayDispatchResult dispatchRemote(EntityPlayerMP livePlayer, TransferTicket ticket,
        ServerDescriptor targetServer) {
        if (livePlayer == null || livePlayer.playerNetServerHandler == null) {
            TransferTicket failed = ticket.withStatus(TransferTicketStatus.FAILED,
                "Remote gateway dispatch requires the source player to stay online", java.time.Instant.now());
            GalaxyBase.LOG.warn(
                "Cluster gateway dispatch rejected because player handle is unavailable. requestId={}, ticketId={}, playerUuid={}, sourceServerId={}, targetServerId={}",
                ticket.getRequestId(),
                ticket.getTicketId(),
                ticket.getPlayerUuid(),
                ticket.getSourceServerId(),
                ticket.getTarget().getServerId());
            return GatewayDispatchResult.failed(failed.getStatusMessage(), failed);
        }

        String gatewayEndpoint = resolveGatewayEndpoint(targetServer);
        try {
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF(CONNECT_SUBCHANNEL);
            output.writeUTF(gatewayEndpoint);
            livePlayer.playerNetServerHandler.sendPacket(new S3FPacketCustomPayload(BUNGEE_CHANNEL, output.toByteArray()));
            TransferTicket dispatched = ticket.withStatus(TransferTicketStatus.DISPATCHED,
                "Proxy connect requested to " + gatewayEndpoint + "; waiting for target restore",
                java.time.Instant.now());
            GalaxyBase.LOG.info(
                "Cluster gateway dispatch succeeded. requestId={}, ticketId={}, playerUuid={}, sourceServerId={}, targetServerId={}, gatewayEndpoint={}",
                dispatched.getRequestId(),
                dispatched.getTicketId(),
                dispatched.getPlayerUuid(),
                dispatched.getSourceServerId(),
                dispatched.getTarget().getServerId(),
                gatewayEndpoint);
            return GatewayDispatchResult.pendingRemote(dispatched.getStatusMessage(), dispatched);
        } catch (RuntimeException exception) {
            TransferTicket failed = ticket.withStatus(TransferTicketStatus.FAILED,
                "Gateway dispatch failed: " + sanitizeMessage(exception), java.time.Instant.now());
            GalaxyBase.LOG.error(
                "Cluster gateway dispatch failed. requestId={}, ticketId={}, playerUuid={}, sourceServerId={}, targetServerId={}, gatewayEndpoint={}",
                ticket.getRequestId(),
                ticket.getTicketId(),
                ticket.getPlayerUuid(),
                ticket.getSourceServerId(),
                ticket.getTarget().getServerId(),
                gatewayEndpoint,
                exception);
            return GatewayDispatchResult.failed(failed.getStatusMessage(), failed);
        }
    }

    private String resolveGatewayEndpoint(ServerDescriptor targetServer) {
        if (targetServer == null) {
            throw new IllegalArgumentException("targetServer must not be null");
        }
        String gatewayEndpoint = targetServer.getGatewayEndpoint();
        if (gatewayEndpoint == null || gatewayEndpoint.trim().isEmpty()) {
            return targetServer.getServerId();
        }
        return gatewayEndpoint.trim();
    }

    private String sanitizeMessage(RuntimeException exception) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().trim().isEmpty()) {
            return exception == null ? "unknown gateway failure" : exception.getClass().getSimpleName();
        }
        return exception.getMessage().trim();
    }
}