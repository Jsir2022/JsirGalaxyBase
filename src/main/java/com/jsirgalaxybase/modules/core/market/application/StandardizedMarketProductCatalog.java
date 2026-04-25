package com.jsirgalaxybase.modules.core.market.application;

import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;

public interface StandardizedMarketProductCatalog {

    StandardizedMarketCatalogVersion getCatalogVersion();

    String getCatalogSourceKey();

    String getCatalogSourceDescription();

    StandardizedMarketAdmissionDecision evaluateProduct(String productKey);

    StandardizedMarketAdmissionDecision evaluateStack(ItemStack stack);

    default StandardizedMarketProduct requireTradableProduct(String productKey) {
        return evaluateProduct(productKey).requireProduct();
    }

    default StandardizedMarketProduct requireTradableStack(ItemStack stack) {
        return evaluateStack(stack).requireProduct();
    }
}