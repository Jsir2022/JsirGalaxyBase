package com.jsirgalaxybase.terminal.client.component;

/** Keeps periodic market refreshes bounded while a terminal snapshot is in flight. */
public final class MarketLiveRefreshController {

    public static final int DEFAULT_INTERVAL_TICKS = 100;
    public static final int DEFAULT_RESPONSE_TIMEOUT_TICKS = 240;

    private final int intervalTicks;
    private final int responseTimeoutTicks;
    private int elapsedTicks;
    private int pendingTicks;

    public MarketLiveRefreshController() {
        this(DEFAULT_INTERVAL_TICKS, DEFAULT_RESPONSE_TIMEOUT_TICKS);
    }

    MarketLiveRefreshController(int intervalTicks, int responseTimeoutTicks) {
        this.intervalTicks = Math.max(1, intervalTicks);
        this.responseTimeoutTicks = Math.max(this.intervalTicks, responseTimeoutTicks);
    }

    /** Returns true once per interval when the caller should request a new snapshot. */
    public boolean tick(boolean eligible) {
        if (!eligible) {
            reset();
            return false;
        }
        if (pendingTicks > 0) {
            pendingTicks++;
            if (pendingTicks >= responseTimeoutTicks) {
                pendingTicks = 0;
                elapsedTicks = 0;
            }
            return false;
        }
        elapsedTicks++;
        if (elapsedTicks < intervalTicks) {
            return false;
        }
        elapsedTicks = 0;
        pendingTicks = 1;
        return true;
    }

    public void onSnapshotReceived() {
        pendingTicks = 0;
        elapsedTicks = 0;
    }

    public void reset() {
        elapsedTicks = 0;
        pendingTicks = 0;
    }

    public boolean isPending() {
        return pendingTicks > 0;
    }
}
