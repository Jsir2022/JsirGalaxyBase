package com.jsirgalaxybase.modules.core.vault.infrastructure;

import com.jsirgalaxybase.modules.core.market.application.MarketClaimDeliveryException;
import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.port.MarketClaimDeliveryPort;
import com.jsirgalaxybase.modules.core.vault.application.BaseVaultService;
import com.jsirgalaxybase.modules.core.vault.application.VaultCapacityException;
import com.jsirgalaxybase.modules.core.vault.application.VaultException;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Market claim adapter. A full vault leaves the market custody CLAIMABLE. */
public final class VaultMarketClaimDeliveryPort implements MarketClaimDeliveryPort {

    private final BaseVaultService vaultService;

    public VaultMarketClaimDeliveryPort(BaseVaultService vaultService) {
        this.vaultService = vaultService;
    }

    @Override
    public void deliver(String requestId, String playerRef, String sourceServerId, StandardizedMarketProduct product,
        boolean stackable, long quantity) {
        if (quantity <= 0L || quantity > Integer.MAX_VALUE) {
            throw new MarketClaimDeliveryException("claim quantity cannot be stored in Base Vault", true);
        }
        Item item = resolveItem(product);
        int maxStack = stackable ? new ItemStack(item, 1, product.getMeta()).getMaxStackSize() : 1;
        long remaining = quantity;
        int part = 0;
        try {
            ItemStack fullDelivery = new ItemStack(item, (int) quantity, product.getMeta());
            if (!vaultService.canFitPersonalVault(playerRef, fullDelivery)) {
                throw new VaultCapacityException("Base Vault cannot hold the complete market claim");
            }
            while (remaining > 0L) {
                int amount = (int) Math.min((long) maxStack, remaining);
                vaultService.deliverToPersonalVault(requestId + ":" + part++, playerRef, "MARKET_CLAIM",
                    new ItemStack(item, amount, product.getMeta()));
                remaining -= amount;
            }
        } catch (VaultCapacityException exception) {
            throw new MarketClaimDeliveryException("Base Vault has insufficient space; asset remains CLAIMABLE", true);
        } catch (VaultException exception) {
            throw new MarketClaimDeliveryException("Base Vault delivery failed: " + exception.getMessage(), true);
        }
    }

    private Item resolveItem(StandardizedMarketProduct product) {
        String id = product == null ? "" : product.getRegistryName();
        int separator = id.indexOf(':');
        Item item = separator > 0 && separator < id.length() - 1
            ? GameRegistry.findItem(id.substring(0, separator), id.substring(separator + 1)) : null;
        if (item == null) {
            throw new MarketClaimDeliveryException("standardized product item is not registered", true);
        }
        return item;
    }
}
