package com.jsirgalaxybase.terminal.client.ui2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MarketRefreshClockTest {
    @Test public void refreshesEveryFiveSecondsOnlyWhenEligible() {
        MarketRefreshClock clock = new MarketRefreshClock();
        clock.received();
        for (int i = 0; i < 99; i++) assertFalse(clock.tick(true));
        assertTrue(clock.tick(true));
        assertTrue(clock.label().contains("刷新中"));
        clock.received();
        for (int i = 0; i < 120; i++) assertFalse(clock.tick(false));
        assertFalse(clock.label().contains("刷新中"));
    }

    @Test public void pendingRequestEventuallyReportsDelayAndTimeout() {
        MarketRefreshClock clock = new MarketRefreshClock();
        clock.received();
        clock.requested();
        for (int i = 0; i < 101; i++) clock.tick(true);
        assertTrue(clock.label().contains("延迟"));
        for (int i = 0; i < 139; i++) clock.tick(true);
        assertTrue(clock.label().contains("过期"));
    }
}
