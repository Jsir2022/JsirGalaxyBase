package com.jsirgalaxybase.terminal.ui;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MarketRequestIdFactoryTest {

    @Test
    public void rootLeavesRoomForLongestKnownDerivedSuffix() {
        String root = MarketRequestIdFactory.buildRoot(
            "terminal-market-sell-now-cancel",
            "123e4567-e89b-12d3-a456-426614174000");

        assertTrue(root.length() <= MarketRequestIdFactory.ROOT_LIMIT);
        assertTrue((root + ":recovery-release").length() <= MarketRequestIdFactory.DATABASE_LIMIT);
        assertTrue((root + ":custody").length() <= MarketRequestIdFactory.DATABASE_LIMIT);
        assertTrue((root + ":order").length() <= MarketRequestIdFactory.DATABASE_LIMIT);
    }
}
