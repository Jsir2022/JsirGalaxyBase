package com.jsirgalaxybase.modules.core.market.domain;

import java.time.Instant;

/** Authenticated, server-side filters for the personal standardized-market order history. */
public final class MarketOrderHistoryQuery {

    public enum StatusGroup {
        ALL,
        OPEN,
        FILLED,
        CLOSED
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
        this.searchText = searchText == null ? "" : searchText.trim();
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
