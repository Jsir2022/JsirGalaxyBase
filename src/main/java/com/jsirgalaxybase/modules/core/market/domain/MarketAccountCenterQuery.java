package com.jsirgalaxybase.modules.core.market.domain;

import java.time.Instant;

/**
 * Bounded, player-independent request contract for the order and asset center.
 * The authenticated player is deliberately not part of this value; callers must
 * take it from the server session before querying a repository.
 */
public final class MarketAccountCenterQuery {

    public static final int MAX_SEARCH_LENGTH = 96;
    public static final int MAX_PAGE_SIZE = 50;
    public static final int MAX_PAGE_INDEX = 100;

    public enum Tab { OPEN_ORDERS, FILLS, ASSETS_AND_DELIVERY, HISTORY }
    public enum StatusGroup { ALL, ACTIVE, FILLED, CLOSED, EXCEPTION, RECOVERY_REQUIRED }

    private final Tab tab;
    private final String searchText;
    private final String productKey;
    private final MarketOrderSide side;
    private final StatusGroup statusGroup;
    private final Instant createdAfter;
    private final int pageIndex;
    private final int pageSize;
    private final String focusedRecordId;

    public MarketAccountCenterQuery(Tab tab, String searchText, String productKey, MarketOrderSide side,
        StatusGroup statusGroup, Instant createdAfter, int pageIndex, int pageSize, String focusedRecordId) {
        this.tab = tab == null ? Tab.OPEN_ORDERS : tab;
        this.searchText = bounded(searchText, MAX_SEARCH_LENGTH);
        this.productKey = bounded(productKey, MAX_SEARCH_LENGTH);
        this.side = side;
        this.statusGroup = statusGroup == null ? StatusGroup.ALL : statusGroup;
        this.createdAfter = createdAfter;
        this.pageIndex = Math.max(0, Math.min(MAX_PAGE_INDEX, pageIndex));
        this.pageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, pageSize));
        this.focusedRecordId = bounded(focusedRecordId, MAX_SEARCH_LENGTH);
    }

    public Tab getTab() { return tab; }
    public String getSearchText() { return searchText; }
    public String getProductKey() { return productKey; }
    public MarketOrderSide getSide() { return side; }
    public StatusGroup getStatusGroup() { return statusGroup; }
    public Instant getCreatedAfter() { return createdAfter; }
    public int getPageIndex() { return pageIndex; }
    public int getPageSize() { return pageSize; }
    public String getFocusedRecordId() { return focusedRecordId; }

    private static String bounded(String value, int maximum) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() <= maximum ? normalized : normalized.substring(0, maximum);
    }
}
