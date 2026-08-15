package com.jsirgalaxybase.modules.core.market.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Service-facing custom market page with item snapshots attached. */
public final class CustomMarketBrowseResult {

    private final List<CustomMarketService.ListingView> listingViews;
    private final int totalEntries;

    public CustomMarketBrowseResult(List<CustomMarketService.ListingView> listingViews, int totalEntries) {
        this.listingViews = listingViews == null ? Collections.<CustomMarketService.ListingView>emptyList()
            : Collections.unmodifiableList(new ArrayList<CustomMarketService.ListingView>(listingViews));
        this.totalEntries = Math.max(0, totalEntries);
    }

    public List<CustomMarketService.ListingView> getListingViews() { return listingViews; }
    public int getTotalEntries() { return totalEntries; }
}
