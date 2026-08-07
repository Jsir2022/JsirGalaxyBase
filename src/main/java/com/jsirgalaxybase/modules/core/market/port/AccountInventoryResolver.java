package com.jsirgalaxybase.modules.core.market.port;

import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;

/** Resolves the account inventory used by market settlement. */
public interface AccountInventoryResolver {

    long countSellable(String playerRef, StandardizedMarketProduct product);

    ItemStack reserveForSell(String requestId, String playerRef, StandardizedMarketProduct product, int quantity);

    void deliver(String requestId, String playerRef, ItemStack stack, String sourceDomain);
}
