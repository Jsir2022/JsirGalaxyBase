package com.jsirgalaxybase.modules.cluster.port;

import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.modules.cluster.domain.GatewayDispatchResult;
import com.jsirgalaxybase.modules.cluster.domain.ServerDescriptor;
import com.jsirgalaxybase.modules.cluster.domain.TransferTicket;

public interface GatewayAdapter {

    GatewayDispatchResult dispatchRemote(EntityPlayerMP livePlayer, TransferTicket ticket, ServerDescriptor targetServer);
}