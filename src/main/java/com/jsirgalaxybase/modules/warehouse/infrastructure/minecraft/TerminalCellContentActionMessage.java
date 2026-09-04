package com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft;

import java.util.UUID;

import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentAction;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/** Actor identity and cursor state always come from the open server Container. */
public final class TerminalCellContentActionMessage implements IMessage {
    private String requestId = "";
    private long expectedVersion;
    private TerminalCellContentAction action = TerminalCellContentAction.INJECT_CURSOR;
    private ItemStack target;

    public TerminalCellContentActionMessage() { }
    public TerminalCellContentActionMessage(String requestId, long expectedVersion,
        TerminalCellContentAction action, ItemStack target) {
        this.requestId = safeRequestId(requestId);
        this.expectedVersion = Math.max(0L, expectedVersion);
        this.action = action == null ? TerminalCellContentAction.INJECT_CURSOR : action;
        this.target = target == null ? null : target.copy();
        if (this.target != null) this.target.stackSize = 1;
    }

    @Override public void fromBytes(ByteBuf buffer) {
        requestId = safeRequestId(ByteBufUtils.readUTF8String(buffer));
        expectedVersion = Math.max(0L, buffer.readLong());
        action = TerminalCellContentAction.fromCode(buffer.readUnsignedByte());
        target = ByteBufUtils.readItemStack(buffer);
        if (target != null) target.stackSize = 1;
    }

    @Override public void toBytes(ByteBuf buffer) {
        ByteBufUtils.writeUTF8String(buffer, safeRequestId(requestId));
        buffer.writeLong(Math.max(0L, expectedVersion));
        buffer.writeByte(action.ordinal());
        ByteBufUtils.writeItemStack(buffer, target);
    }

    public static String nextRequestId() { return "asset-cell:" + UUID.randomUUID().toString(); }

    public static final class Handler implements IMessageHandler<TerminalCellContentActionMessage, IMessage> {
        @Override public IMessage onMessage(TerminalCellContentActionMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            if (player == null || !(player.openContainer instanceof TerminalAssetCenterContainer)) return null;
            ((TerminalAssetCenterContainer) player.openContainer).handleCellContentAction(message.requestId,
                message.expectedVersion, message.action, message.target);
            return null;
        }
    }

    private static String safeRequestId(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) normalized = "invalid-request";
        return normalized.length() > 160 ? normalized.substring(0, 160) : normalized;
    }
}
