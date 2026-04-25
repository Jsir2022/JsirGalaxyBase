package com.jsirgalaxybase.modules.core.market.application;

public final class StandardizedMarketCatalogFactory {

    public static final StandardizedMarketCatalogVersion DEFAULT_VERSION = new StandardizedMarketCatalogVersion(
        "standardized-spot-catalog-v1", "标准商品市场目录 v1");

    private StandardizedMarketCatalogFactory() {}

    public static StandardizedMarketProductCatalog createDefaultCatalog() {
        return createDefaultCatalog(new StandardizedMarketProductParser());
    }

    public static StandardizedMarketProductCatalog createDefaultCatalog(StandardizedMarketProductParser parser) {
        return new StandardizedMarketCatalogService(DEFAULT_VERSION, new GregTechStandardizedMetalCatalog(parser));
    }
}