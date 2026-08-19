package com.jsirgalaxybase.terminal.client.component;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MarketLiveRefreshControllerTest {

    @Test
    public void refreshesAtIntervalAndWaitsForTheReturnedSnapshot() {
        MarketLiveRefreshController controller = new MarketLiveRefreshController(3, 8);

        assertFalse(controller.tick(true));
        assertFalse(controller.tick(true));
        assertTrue(controller.tick(true));
        assertTrue(controller.isPending());
        assertFalse(controller.tick(true));
        assertFalse(controller.tick(true));

        controller.onSnapshotReceived();
        assertFalse(controller.isPending());
        assertFalse(controller.tick(true));
        assertFalse(controller.tick(true));
        assertTrue(controller.tick(true));
    }

    @Test
    public void pausesAndResetsWheneverThePlayerIsEditingOrInAnotherRoute() {
        MarketLiveRefreshController controller = new MarketLiveRefreshController(2, 4);

        assertFalse(controller.tick(true));
        assertFalse(controller.tick(false));
        assertFalse(controller.tick(true));
        assertTrue(controller.tick(true));
        assertTrue(controller.isPending());

        assertFalse(controller.tick(false));
        assertFalse(controller.isPending());
    }

    @Test
    public void permitsAnotherRefreshAfterALostResponseTimesOut() {
        MarketLiveRefreshController controller = new MarketLiveRefreshController(2, 4);

        assertFalse(controller.tick(true));
        assertTrue(controller.tick(true));
        assertFalse(controller.tick(true));
        assertFalse(controller.tick(true));
        assertFalse(controller.tick(true));
        assertFalse(controller.isPending());
        assertFalse(controller.tick(true));
        assertTrue(controller.tick(true));
    }

    @Test
    public void reportsFreshRefreshingAndStaleWithoutForgettingPausedSnapshotAge() {
        MarketLiveRefreshController controller = new MarketLiveRefreshController(2, 4);
        assertEquals(MarketLiveRefreshController.Freshness.WAITING, controller.getFreshness());
        controller.onSnapshotReceived();
        assertEquals(MarketLiveRefreshController.Freshness.FRESH, controller.getFreshness());
        assertFalse(controller.tick(false));
        assertEquals(MarketLiveRefreshController.Freshness.FRESH, controller.getFreshness());
        assertFalse(controller.tick(true));
        assertTrue(controller.tick(true));
        assertEquals(MarketLiveRefreshController.Freshness.REFRESHING, controller.getFreshness());
        assertFalse(controller.tick(true));
        assertEquals(MarketLiveRefreshController.Freshness.REFRESHING, controller.getFreshness());
        assertFalse(controller.tick(true));
        assertEquals(MarketLiveRefreshController.Freshness.DELAYED, controller.getFreshness());
        assertFalse(controller.tick(true));
        assertFalse(controller.tick(true));
        assertEquals(MarketLiveRefreshController.Freshness.STALE, controller.getFreshness());
    }
}
