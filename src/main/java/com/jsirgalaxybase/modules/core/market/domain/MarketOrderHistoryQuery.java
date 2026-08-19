package com.jsirgalaxybase.modules.core.market.domain;

import java.time.Instant;

/** Authenticated, server-side filters for the personal standardized-market order history. */
public final class MarketOrderHistoryQuery {

    public static final int MAX_SEARCH_LENGTH = 96;

    public enum StatusGroup {
        ALL,
        OPEN,
        FILLED,
        CLOSED,
        HISTORICAL
    }

    private final String productKey;
    private final MarketOrderSide side;
    private final StatusGroup status;
    private final Instant createdAfter;
    private final String searchText;
    private final int pageIndex;
    private final int pageSize;

    public MarketOrderHistoryQuery(String productKey, MarketOrderSide side, StatusGroup status,
        Instant createdAfter, int pageIndex, int pageSize) {
        this(productKey, side, status, createdAfter, "", pageIndex, pageSize);
    }

    public MarketOrderHistoryQuery(String productKey, MarketOrderSide side, StatusGroup status,
        Instant createdAfter, String searchText, int pageIndex, int pageSize) {
        this.productKey = productKey == null ? "" : productKey.trim();
        this.side = side;
        this.status = status == null ? StatusGroup.ALL : status;
        this.createdAfter = createdAfter;
        String normalizedSearch = searchText == null ? "" : searchText.trim();
        this.searchText = normalizedSearch.length() <= MAX_SEARCH_LENGTH ? normalizedSearch
            : normalizedSearch.substring(0, MAX_SEARCH_LENGTH);
        this.pageIndex = Math.max(0, pageIndex);
        this.pageSize = Math.max(1, Math.min(50, pageSize));
    }

    public String getProductKey() { return productKey; }

    public MarketOrderSide getSide() { return side; }

    public StatusGroup getStatus() { return status; }

    public Instant getCreatedAfter() { return createdAfter; }

    public String getSearchText() { return searchText; }

    public int getPageIndex() { return pageIndex; }

    public int getPageSize() { return pageSize; }
}
