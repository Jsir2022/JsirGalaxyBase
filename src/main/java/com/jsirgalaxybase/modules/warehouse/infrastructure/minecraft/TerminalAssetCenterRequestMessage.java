package com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft;

import com.jsirgalaxybase.terminal.network.TerminalNetwork;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

/** C2S tab selection and actor-scoped audit refresh for an open asset center. */
public final class TerminalAssetCenterRequestMessage implements IMessage {
    private int tabCode;
    private int cellPage;
    private int cellPageSize = 20;

    public TerminalAssetCenterRequestMessage() { }
    public TerminalAssetCenterRequestMessage(TerminalAssetCenterTab tab) { this(tab, 0); }
    public TerminalAssetCenterRequestMessage(TerminalAssetCenterTab tab, int cellPage) { this(tab, cellPage, 20); }
    public TerminalAssetCenterRequestMessage(TerminalAssetCenterTab tab, int cellPage, int cellPageSize) {
        tabCode = (tab == null ? TerminalAssetCenterTab.STORAGE : tab).getCode();
        this.cellPage = Math.max(0, Math.min(8, cellPage));
        this.cellPageSize = Math.max(1, Math.min(35, cellPageSize));
    }
    @Override public void fromBytes(ByteBuf buffer) {
        tabCode = buffer.readUnsignedByte();
        cellPage = buffer.readableBytes() >= 1 ? buffer.readUnsignedByte() : 0;
        cellPageSize = buffer.readableBytes() >= 1 ? buffer.readUnsignedByte() : 20;
    }
    @Override public void toBytes(ByteBuf buffer) {
        buffer.writeByte(TerminalAssetCenterTab.fromCode(tabCode).getCode());
        buffer.writeByte(Math.max(0, Math.min(8, cellPage)));
        buffer.writeByte(Math.max(1, Math.min(35, cellPageSize)));
    }

    public static final class Handler implements IMessageHandler<TerminalAssetCenterRequestMessage, IMessage> {
        @Override public IMessage onMessage(TerminalAssetCenterRequestMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            if (player == null || !(player.openContainer instanceof TerminalAssetCenterContainer)) return null;
            TerminalAssetCenterContainer container = (TerminalAssetCenterContainer) player.openContainer;
            container.setActiveTab(TerminalAssetCenterTab.fromCode(message.tabCode));
            container.setCellViewport(message.cellPage, message.cellPageSize);
            TerminalNetwork.CHANNEL.sendTo(new TerminalAssetCenterSnapshotMessage(container.createActivitySnapshot(),
                container.createCellContentSnapshot()), player);
            return null;
        }
    }
}
