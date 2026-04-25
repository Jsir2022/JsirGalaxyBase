package com.jsirgalaxybase.modules.core.market.application;

public class StandardizedMarketCatalogVersion {

    private final String versionKey;
    private final String displayName;

    public StandardizedMarketCatalogVersion(String versionKey, String displayName) {
        this.versionKey = versionKey;
        this.displayName = displayName;
    }

    public String getVersionKey() {
        return versionKey;
    }

    public String getDisplayName() {
        return displayName;
    }
}