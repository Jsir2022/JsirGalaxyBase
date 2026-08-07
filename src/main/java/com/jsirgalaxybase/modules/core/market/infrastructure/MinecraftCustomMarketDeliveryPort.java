package com.jsirgalaxybase.modules.core.market.infrastructure;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

import com.jsirgalaxybase.modules.core.market.application.MarketClaimDeliveryException;
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.domain.CustomMarketItemSnapshot;
import com.jsirgalaxybase.modules.core.market.port.CustomMarketDeliveryPort;

/** Minecraft-server adapter for custom listing escrow delivery. */
public final class MinecraftCustomMarketDeliveryPort implements CustomMarketDeliveryPort {

    @Override
    public void deliver(String deliveryRequestId, String playerRef, String sourceServerId, CustomMarketItemSnapshot snapshot) {
        if (snapshot == null) {
            throw new MarketClaimDeliveryException("custom market delivery snapshot is required", true);
        }
        EntityPlayerMP player = resolveOnlinePlayer(playerRef);
        ItemStack stack = snapshot.toItemStack();
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
            throw new MarketClaimDeliveryException("custom market snapshot cannot restore a deliverable item", true);
        }
        if (!hasInventoryCapacity(player, stack)) {
            throw new MarketClaimDeliveryException("player inventory does not have enough space for custom market delivery", true);
        }
        ItemStack delivery = stack.copy();
        if (!player.inventory.addItemStackToInventory(delivery) || delivery.stackSize > 0) {
            throw new MarketOperationException("custom market delivery partially applied unexpectedly");
        }
        player.inventory.markDirty();
        if (player.openContainer != null) {
            player.openContainer.detectAndSendChanges();
        }
    }

    private EntityPlayerMP resolveOnlinePlayer(String playerRef) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) {
            throw new MarketClaimDeliveryException("minecraft server is not available for custom market delivery", true);
        }
        UUID targetUuid;
        try {
            targetUuid = UUID.fromString(playerRef);
        } catch (IllegalArgumentException exception) {
            throw new MarketClaimDeliveryException("playerRef is not a valid UUID for custom market delivery", true);
        }
        List<EntityPlayerMP> players = server.getConfigurationManager().playerEntityList;
        for (EntityPlayerMP player : players) {
            if (player != null && targetUuid.equals(player.getUniqueID())) {
                return player;
            }
        }
        throw new MarketClaimDeliveryException("player must be online to receive custom market delivery", true);
    }

    private boolean hasInventoryCapacity(EntityPlayerMP player, ItemStack delivery) {
        int remaining = delivery.stackSize;
        int maxStack = Math.max(1, delivery.getMaxStackSize());
        for (ItemStack existing : player.inventory.mainInventory) {
            if (existing == null) {
                remaining -= maxStack;
            } else if (existing.isItemEqual(delivery) && ItemStack.areItemStackTagsEqual(existing, delivery)
                && existing.stackSize < maxStack) {
                remaining -= maxStack - existing.stackSize;
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }
}
