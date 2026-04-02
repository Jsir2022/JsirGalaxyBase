package com.jsirgalaxybase.modules.core.market.application;

import com.jsirgalaxybase.modules.core.market.domain.StandardizedMarketProduct;

public class StandardizedMarketProductParser {

    public StandardizedMarketProduct parse(String productKey) {
        if (productKey == null) {
            throw new MarketOperationException("productKey must not be blank");
        }

        String normalized = productKey.trim();
        if (normalized.isEmpty()) {
            throw new MarketOperationException("productKey must not be blank");
        }

        int firstColon = normalized.indexOf(':');
        int lastColon = normalized.lastIndexOf(':');
        if (firstColon <= 0 || lastColon <= firstColon + 1 || lastColon >= normalized.length() - 1) {
            throw new MarketOperationException("productKey must use modid:itemid:meta");
        }

        String registryName = normalized.substring(0, lastColon);
        String metaText = normalized.substring(lastColon + 1);
        int meta;
        try {
            meta = Integer.parseInt(metaText);
        } catch (NumberFormatException exception) {
            throw new MarketOperationException("meta must be a non-negative integer");
        }
        if (meta < 0) {
            throw new MarketOperationException("meta must be a non-negative integer");
        }
        if (registryName.indexOf(':') <= 0 || registryName.endsWith(":")) {
            throw new MarketOperationException("productKey must use modid:itemid:meta");
        }
        return new StandardizedMarketProduct(registryName, meta);
    }
}