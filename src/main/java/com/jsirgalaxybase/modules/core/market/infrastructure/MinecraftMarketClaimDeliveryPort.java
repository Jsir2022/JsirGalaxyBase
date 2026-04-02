package com.jsirgalaxybase.modules.core.market.infrastructure;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

import com.jsirgalaxybase.modules.core.market.application.MarketClaimDeliveryException;
import com.jsirgalaxybase.modules.core.market.application.MarketOperationException;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.port.MarketClaimDeliveryPort;

import cpw.mods.fml.common.registry.GameRegistry;

public class MinecraftMarketClaimDeliveryPort implements MarketClaimDeliveryPort {

    @Override
    public void deliver(String deliveryRequestId, String playerRef, String sourceServerId, StandardizedMarketProduct product,
        boolean stackable, long quantity) {
        if (quantity <= 0L) {
            throw new MarketClaimDeliveryException("claim quantity must be positive", true);
        }

        EntityPlayerMP player = resolveOnlinePlayer(playerRef);
        Item item = resolveItem(product);
        int maxStackSize = new ItemStack(item, 1, product.getMeta()).getMaxStackSize();
        if (!stackable) {
            maxStackSize = 1;
        }
        if (!hasInventoryCapacity(player, item, product.getMeta(), maxStackSize, quantity)) {
            throw new MarketClaimDeliveryException("player inventory does not have enough space for claim", true);
        }

        long remaining = quantity;
        while (remaining > 0L) {
            int chunk = (int) Math.min((long) maxStackSize, remaining);
            ItemStack stack = new ItemStack(item, chunk, product.getMeta());
            if (!player.inventory.addItemStackToInventory(stack) || stack.stackSize > 0) {
                throw new MarketOperationException("claim delivery partially applied unexpectedly");
            }
            remaining -= chunk;
        }

        player.inventory.markDirty();
        if (player.openContainer != null) {
            player.openContainer.detectAndSendChanges();
        }
    }

    private EntityPlayerMP resolveOnlinePlayer(String playerRef) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) {
            throw new MarketClaimDeliveryException("minecraft server is not available for claim delivery", true);
        }

        List<EntityPlayerMP> players = server.getConfigurationManager().playerEntityList;
        UUID targetUuid;
        try {
            targetUuid = UUID.fromString(playerRef);
        } catch (IllegalArgumentException exception) {
            throw new MarketClaimDeliveryException("playerRef is not a valid UUID for claim delivery", true);
        }
        for (EntityPlayerMP player : players) {
            if (player != null && targetUuid.equals(player.getUniqueID())) {
                return player;
            }
        }
        throw new MarketClaimDeliveryException("player must be online to receive claim delivery", true);
    }

    private Item resolveItem(StandardizedMarketProduct product) {
        String registryName = product.getRegistryName();
        int separatorIndex = registryName.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex == registryName.length() - 1) {
            throw new MarketClaimDeliveryException("invalid standardized product registry name", true);
        }
        Item item = GameRegistry.findItem(registryName.substring(0, separatorIndex),
            registryName.substring(separatorIndex + 1));
        if (item == null) {
            throw new MarketClaimDeliveryException("standardized product item is not registered", true);
        }
        return item;
    }

    private boolean hasInventoryCapacity(EntityPlayerMP player, Item item, int meta, int maxStackSize, long quantity) {
        long remaining = quantity;
        ItemStack[] mainInventory = player.inventory.mainInventory;
        for (ItemStack stack : mainInventory) {
            if (stack == null) {
                remaining -= maxStackSize;
            } else if (stack.getItem() == item && stack.getItemDamage() == meta && stack.stackSize < maxStackSize) {
                remaining -= (maxStackSize - stack.stackSize);
            }
            if (remaining <= 0L) {
                return true;
            }
        }
        return remaining <= 0L;
    }
}