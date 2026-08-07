package com.jsirgalaxybase.modules.core.market.application;

/** Optional browse capability. Admission-only compatibility catalogs need not implement it. */
public interface StandardizedMarketCatalogBrowser {

    StandardizedMarketCatalogPage browse(String query, int pageIndex, int pageSize);
}
