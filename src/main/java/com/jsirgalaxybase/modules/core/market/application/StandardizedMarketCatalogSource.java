package com.jsirgalaxybase.modules.core.market.application;

import java.util.Optional;

import net.minecraft.item.ItemStack;

public interface StandardizedMarketCatalogSource {

    String getSourceKey();

    String getSourceDescription();

    Optional<StandardizedMarketCatalogEntry> findEntryByProductKey(String productKey);

    Optional<StandardizedMarketCatalogEntry> findEntryByStack(ItemStack stack);
}