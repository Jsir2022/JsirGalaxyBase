package com.jsirgalaxybase.terminal.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.terminal.TerminalOpenApproval;
import com.jsirgalaxybase.terminal.TerminalService;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class OpenTerminalRequestMessage implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<OpenTerminalRequestMessage, IMessage> {

        @Override
        public IMessage onMessage(OpenTerminalRequestMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            TerminalOpenApproval approval = TerminalService.approveTerminalClientScreen(player);
            if (approval != null) {
                TerminalNetwork.CHANNEL.sendTo(new OpenTerminalApprovedMessage(approval), player);
            }
            return null;
        }
    }
}