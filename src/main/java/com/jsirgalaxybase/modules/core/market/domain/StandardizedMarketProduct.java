package com.jsirgalaxybase.modules.core.market.domain;

public class StandardizedMarketProduct {

    private final String registryName;
    private final int meta;

    public StandardizedMarketProduct(String registryName, int meta) {
        this.registryName = registryName;
        this.meta = meta;
    }

    public String getRegistryName() {
        return registryName;
    }

    public int getMeta() {
        return meta;
    }

    public String getProductKey() {
        return registryName + ":" + meta;
    }
}