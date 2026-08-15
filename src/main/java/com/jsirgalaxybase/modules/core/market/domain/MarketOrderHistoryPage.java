package com.jsirgalaxybase.modules.core.market.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One stable page of personal orders plus the server-side total. */
public final class MarketOrderHistoryPage {

    private final List<MarketOrder> orders;
    private final int totalEntries;
    private final int pageIndex;
    private final int pageSize;

    public MarketOrderHistoryPage(List<MarketOrder> orders, int totalEntries, int pageIndex, int pageSize) {
        this.orders = Collections.unmodifiableList(new ArrayList<MarketOrder>(
            orders == null ? Collections.<MarketOrder>emptyList() : orders));
        this.totalEntries = Math.max(0, totalEntries);
        this.pageIndex = Math.max(0, pageIndex);
        this.pageSize = Math.max(1, pageSize);
    }

    public List<MarketOrder> getOrders() { return orders; }

    public int getTotalEntries() { return totalEntries; }

    public int getPageIndex() { return pageIndex; }

    public int getPageSize() { return pageSize; }

    public int getTotalPages() {
        return Math.max(1, (totalEntries + pageSize - 1) / pageSize);
    }
}
