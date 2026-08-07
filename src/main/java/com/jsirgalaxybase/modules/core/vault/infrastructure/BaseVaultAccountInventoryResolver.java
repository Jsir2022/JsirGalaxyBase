package com.jsirgalaxybase.modules.core.vault.infrastructure;

import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;
import com.jsirgalaxybase.modules.core.market.port.AccountInventoryResolver;
import com.jsirgalaxybase.modules.core.vault.application.BaseVaultService;

/** Current account-inventory policy: the personal Base Vault is authoritative. */
public final class BaseVaultAccountInventoryResolver implements AccountInventoryResolver {

    private final BaseVaultService vaultService;

    public BaseVaultAccountInventoryResolver(BaseVaultService vaultService) {
        if (vaultService == null) {
            throw new IllegalArgumentException("vaultService must not be null");
        }
        this.vaultService = vaultService;
    }

    @Override
    public long countSellable(String playerRef, StandardizedMarketProduct product) {
        requireProduct(product);
        return vaultService.countPersonalProduct(playerRef, product.getRegistryName(), product.getMeta());
    }

    @Override
    public ItemStack reserveForSell(String requestId, String playerRef, StandardizedMarketProduct product, int quantity) {
        requireProduct(product);
        return vaultService.takeStandardizedProduct(requestId, playerRef, product.getRegistryName(), product.getMeta(),
            quantity, "STANDARDIZED_MARKET_ESCROW");
    }

    @Override
    public void deliver(String requestId, String playerRef, ItemStack stack, String sourceDomain) {
        vaultService.deliverToPersonalVault(requestId, playerRef, sourceDomain, stack);
    }

    private static void requireProduct(StandardizedMarketProduct product) {
        if (product == null) {
            throw new IllegalArgumentException("product must not be null");
        }
    }
}
