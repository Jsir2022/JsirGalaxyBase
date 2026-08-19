package com.jsirgalaxybase.modules.core.market.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MarketOperationHistoryPage {
    private final List<MarketOperationLog> operations;
    private final int totalEntries;
    private final int pageIndex;
    private final int pageSize;

    public MarketOperationHistoryPage(List<MarketOperationLog> operations, int totalEntries, int pageIndex,
        int pageSize) {
        this.operations = Collections.unmodifiableList(new ArrayList<MarketOperationLog>(
            operations == null ? Collections.<MarketOperationLog>emptyList() : operations));
        this.totalEntries = Math.max(0, totalEntries);
        this.pageIndex = Math.max(0, pageIndex);
        this.pageSize = Math.max(1, pageSize);
    }
    public List<MarketOperationLog> getOperations() { return operations; }
    public int getTotalEntries() { return totalEntries; }
    public int getPageIndex() { return pageIndex; }
    public int getPageSize() { return pageSize; }
}
