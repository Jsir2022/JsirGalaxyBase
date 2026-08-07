package com.jsirgalaxybase.modules.core.market.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A stable, database-backed page of formally admitted standard products. */
public final class StandardizedMarketCatalogPage {

    private final String query;
    private final int pageIndex;
    private final int pageSize;
    private final int totalEntries;
    private final List<StandardizedMarketCatalogEntry> entries;

    public StandardizedMarketCatalogPage(String query, int pageIndex, int pageSize, int totalEntries,
        List<StandardizedMarketCatalogEntry> entries) {
        this.query = query == null ? "" : query.trim();
        this.pageIndex = Math.max(0, pageIndex);
        this.pageSize = Math.max(1, pageSize);
        this.totalEntries = Math.max(0, totalEntries);
        this.entries = entries == null
            ? Collections.<StandardizedMarketCatalogEntry>emptyList()
            : Collections.unmodifiableList(new ArrayList<StandardizedMarketCatalogEntry>(entries));
    }

    public String getQuery() { return query; }
    public int getPageIndex() { return pageIndex; }
    public int getPageSize() { return pageSize; }
    public int getTotalEntries() { return totalEntries; }
    public List<StandardizedMarketCatalogEntry> getEntries() { return entries; }
    public boolean hasPreviousPage() { return pageIndex > 0; }
    public boolean hasNextPage() { return (pageIndex + 1) * pageSize < totalEntries; }
}
