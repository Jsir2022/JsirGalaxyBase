package com.jsirgalaxybase.terminal.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.terminal.TerminalService;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class OpenTerminalMessage implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<OpenTerminalMessage, IMessage> {

        @Override
        public IMessage onMessage(OpenTerminalMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            TerminalService.openTerminal(player);
            return null;
        }
    }
}