package com.jsirgalaxybase.modules.core.market.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MarketAccountCenterContractTest {

    @Test
    public void queryBoundsUntrustedSearchAndPagination() {
        StringBuilder oversized = new StringBuilder();
        for (int index = 0; index < 200; index++) oversized.append('x');
        MarketAccountCenterQuery query = new MarketAccountCenterQuery(null, oversized.toString(), oversized.toString(),
            null, null, null, Integer.MAX_VALUE, Integer.MAX_VALUE, oversized.toString());

        assertEquals(MarketAccountCenterQuery.MAX_SEARCH_LENGTH, query.getSearchText().length());
        assertEquals(MarketAccountCenterQuery.MAX_SEARCH_LENGTH, query.getProductKey().length());
        assertEquals(MarketAccountCenterQuery.MAX_SEARCH_LENGTH, query.getFocusedRecordId().length());
        assertEquals(MarketAccountCenterQuery.MAX_PAGE_INDEX, query.getPageIndex());
        assertEquals(MarketAccountCenterQuery.MAX_PAGE_SIZE, query.getPageSize());
    }

    @Test
    public void serverCountedPaginationHandlesEveryRequiredBoundary() {
        int[] totals = { 0, 1, 4, 5, 8, 9, 11 };
        int[] pages = { 1, 1, 1, 2, 2, 3, 3 };
        for (int index = 0; index < totals.length; index++) {
            MarketAccountCenterSnapshot.PageMetadata page =
                new MarketAccountCenterSnapshot.PageMetadata(totals[index], 0, 4);
            assertEquals("total=" + totals[index], pages[index], page.getTotalPages());
        }
    }
}
