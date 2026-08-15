package com.jsirgalaxybase.modules.core.market.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.modules.core.market.domain.CustomMarketListing;

/** A database-backed page of custom listings; the total is evaluated before LIMIT/OFFSET. */
public final class CustomMarketBrowsePage {

    private final List<CustomMarketListing> listings;
    private final int totalEntries;

    public CustomMarketBrowsePage(List<CustomMarketListing> listings, int totalEntries) {
        this.listings = listings == null ? Collections.<CustomMarketListing>emptyList()
            : Collections.unmodifiableList(new ArrayList<CustomMarketListing>(listings));
        this.totalEntries = Math.max(0, totalEntries);
    }

    public List<CustomMarketListing> getListings() {
        return listings;
    }

    public int getTotalEntries() {
        return totalEntries;
    }
}
