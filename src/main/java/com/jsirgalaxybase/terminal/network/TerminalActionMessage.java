package com.jsirgalaxybase.terminal.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import com.jsirgalaxybase.GalaxyBase;
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
    private long requestSequence;

    public TerminalActionMessage() {}

    public TerminalActionMessage(String sessionToken, String pageId, String actionType, String payload) {
        this(sessionToken, pageId, actionType, payload, 0L);
    }

    public TerminalActionMessage(String sessionToken, String pageId, String actionType, String payload,
        long requestSequence) {
        this.sessionToken = sessionToken;
        this.pageId = pageId;
        this.actionType = actionType;
        this.payload = payload;
        this.requestSequence = Math.max(0L, requestSequence);
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

    public long getRequestSequence() {
        return requestSequence;
    }

    public TerminalActionMessage withRequestSequence(long sequence) {
        return new TerminalActionMessage(sessionToken, pageId, actionType, payload, sequence);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        sessionToken = ByteBufUtils.readUTF8String(buf);
        pageId = ByteBufUtils.readUTF8String(buf);
        actionType = ByteBufUtils.readUTF8String(buf);
        payload = ByteBufUtils.readUTF8String(buf);
        requestSequence = buf.readableBytes() >= 8 ? Math.max(0L, buf.readLong()) : 0L;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, safe(sessionToken));
        ByteBufUtils.writeUTF8String(buf, safe(pageId));
        ByteBufUtils.writeUTF8String(buf, safe(actionType));
        ByteBufUtils.writeUTF8String(buf, safe(payload));
        buf.writeLong(requestSequence);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static class Handler implements IMessageHandler<TerminalActionMessage, IMessage> {

        @Override
        public IMessage onMessage(TerminalActionMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            try {
                TerminalOpenApproval approval = TerminalService.handleClientAction(
                    player,
                    message.sessionToken,
                    message.pageId,
                    message.actionType,
                    message.payload);
                if (approval != null) {
                    TerminalNetwork.CHANNEL.sendTo(new TerminalSnapshotMessage(approval, message.requestSequence), player);
                }
            } catch (RuntimeException exception) {
                String playerName = player == null ? "unknown" : player.getCommandSenderName();
                GalaxyBase.LOG.error("Terminal action failed without disconnecting player={} page={} action={}",
                    playerName, safe(message.pageId), safe(message.actionType), exception);
                if (player != null) {
                    player.addChatMessage(new ChatComponentText(
                        "银河终端请求失败，操作未完成。请刷新后重试；若持续失败请联系管理员。"));
                }
            }
            return null;
        }
    }
}
