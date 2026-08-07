package com.jsirgalaxybase.modules.core.vault.infrastructure.minecraft;

import com.jsirgalaxybase.GalaxyBase;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

/** C2S request for the audited Vault-only sort operation. */
public final class BaseVaultSortRequestMessage implements IMessage {

    @Override
    public void fromBytes(ByteBuf buffer) { }

    @Override
    public void toBytes(ByteBuf buffer) { }

    public static final class Handler implements IMessageHandler<BaseVaultSortRequestMessage, IMessage> {
        @Override
        public IMessage onMessage(BaseVaultSortRequestMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            if (player != null && player.openContainer instanceof BaseVaultContainer) {
                try {
                    ((BaseVaultContainer) player.openContainer).sortVault();
                } catch (RuntimeException exception) {
                    GalaxyBase.LOG.warn("Base Vault sort rejected for {}", player.getUniqueID(), exception);
                }
            }
            return null;
        }
    }
}
