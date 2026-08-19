package com.jsirgalaxybase.modules.core.market.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MarketTradeHistoryPage {
    private final List<MarketTradeRecord> trades;
    private final int totalEntries;
    private final int pageIndex;
    private final int pageSize;

    public MarketTradeHistoryPage(List<MarketTradeRecord> trades, int totalEntries, int pageIndex, int pageSize) {
        this.trades = Collections.unmodifiableList(new ArrayList<MarketTradeRecord>(
            trades == null ? Collections.<MarketTradeRecord>emptyList() : trades));
        this.totalEntries = Math.max(0, totalEntries);
        this.pageIndex = Math.max(0, pageIndex);
        this.pageSize = Math.max(1, pageSize);
    }
    public List<MarketTradeRecord> getTrades() { return trades; }
    public int getTotalEntries() { return totalEntries; }
    public int getPageIndex() { return pageIndex; }
    public int getPageSize() { return pageSize; }
}
