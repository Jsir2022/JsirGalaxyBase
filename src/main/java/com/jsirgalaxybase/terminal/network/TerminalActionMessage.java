package com.jsirgalaxybase.terminal.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.jsirgalaxybase.terminal.TerminalOpenApproval;
import com.jsirgalaxybase.terminal.TerminalService;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class TerminalActionMessage implements IMessage {

    private String sessionToken;
    private String pageId;
    private String actionType;
    private String payload;

    public TerminalActionMessage() {}

    public TerminalActionMessage(String sessionToken, String pageId, String actionType, String payload) {
        this.sessionToken = sessionToken;
        this.pageId = pageId;
        this.actionType = actionType;
        this.payload = payload;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public String getPageId() {
        return pageId;
    }

    public String getActionType() {
        return actionType;
    }

    public String getPayload() {
        return payload;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        sessionToken = ByteBufUtils.readUTF8String(buf);
        pageId = ByteBufUtils.readUTF8String(buf);
        actionType = ByteBufUtils.readUTF8String(buf);
        payload = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, safe(sessionToken));
        ByteBufUtils.writeUTF8String(buf, safe(pageId));
        ByteBufUtils.writeUTF8String(buf, safe(actionType));
        ByteBufUtils.writeUTF8String(buf, safe(payload));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static class Handler implements IMessageHandler<TerminalActionMessage, IMessage> {

        @Override
        public IMessage onMessage(TerminalActionMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            TerminalOpenApproval approval = TerminalService.handleClientAction(
                player,
                message.sessionToken,
                message.pageId,
                message.actionType,
                message.payload);
            if (approval != null) {
                TerminalNetwork.CHANNEL.sendTo(new TerminalSnapshotMessage(approval), player);
            }
            return null;
        }
    }
}
