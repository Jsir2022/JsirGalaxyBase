package com.jsirgalaxybase.terminal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TerminalNotificationFeedTest {

    @Test
    public void deduplicatesAndPaginatesNewestFirstWithBoundedPageSize() {
        TerminalNotificationFeed feed = new TerminalNotificationFeed();
        feed.record(new TerminalNotificationFeed.Entry("land:1", "land-shadow", "property", "", "观察", "一", "WARNING"));
        feed.record(new TerminalNotificationFeed.Entry("land:1", "land-shadow", "property", "", "观察", "二", "WARNING"));
        for (int i = 0; i < 13; i++) feed.record(new TerminalNotificationFeed.Entry("market:" + i, "market-recovery", "market_account_center", "C" + i, "恢复", String.valueOf(i), "WARNING"));

        TerminalNotificationFeed.Page first = feed.page(-3, 99);
        TerminalNotificationFeed.Page second = feed.page(1, 12);
        assertEquals(14, first.getTotalEntries());
        assertEquals(2, first.getTotalPages());
        assertEquals(12, first.getEntries().size());
        assertEquals(2, second.getEntries().size());
        assertEquals(2, second.getEntries().get(1).getOccurrences());
    }
}
