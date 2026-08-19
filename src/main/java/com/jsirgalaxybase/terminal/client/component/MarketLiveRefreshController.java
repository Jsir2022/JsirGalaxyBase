package com.jsirgalaxybase.terminal.client.component;

/** Keeps periodic market refreshes bounded while a terminal snapshot is in flight. */
public final class MarketLiveRefreshController {

    public enum Freshness {
        WAITING,
        FRESH,
        REFRESHING,
        DELAYED,
        STALE
    }

    public static final int DEFAULT_INTERVAL_TICKS = 100;
    public static final int DEFAULT_RESPONSE_TIMEOUT_TICKS = 240;

    private final int intervalTicks;
    private final int responseTimeoutTicks;
    private int elapsedTicks;
    private int pendingTicks;
    private int snapshotAgeTicks;
    private boolean hasSnapshot;
    private boolean responseTimedOut;

    public MarketLiveRefreshController() {
        this(DEFAULT_INTERVAL_TICKS, DEFAULT_RESPONSE_TIMEOUT_TICKS);
    }

    MarketLiveRefreshController(int intervalTicks, int responseTimeoutTicks) {
        this.intervalTicks = Math.max(1, intervalTicks);
        this.responseTimeoutTicks = Math.max(this.intervalTicks, responseTimeoutTicks);
    }

    /** Returns true once per interval when the caller should request a new snapshot. */
    public boolean tick(boolean eligible) {
        if (hasSnapshot) {
            snapshotAgeTicks++;
        }
        if (!eligible) {
            elapsedTicks = 0;
            pendingTicks = 0;
            return false;
        }
        if (pendingTicks > 0) {
            pendingTicks++;
            if (pendingTicks >= responseTimeoutTicks) {
                pendingTicks = 0;
                elapsedTicks = 0;
                responseTimedOut = true;
            }
            return false;
        }
        elapsedTicks++;
        if (elapsedTicks < intervalTicks) {
            return false;
        }
        elapsedTicks = 0;
        pendingTicks = 1;
        responseTimedOut = false;
        return true;
    }

    public void onSnapshotReceived() {
        pendingTicks = 0;
        elapsedTicks = 0;
        snapshotAgeTicks = 0;
        hasSnapshot = true;
        responseTimedOut = false;
    }

    public void reset() {
        elapsedTicks = 0;
        pendingTicks = 0;
        snapshotAgeTicks = 0;
        hasSnapshot = false;
        responseTimedOut = false;
    }

    public boolean isPending() {
        return pendingTicks > 0;
    }

    public int getSnapshotAgeSeconds() {
        return snapshotAgeTicks / 20;
    }

    public Freshness getFreshness() {
        if (!hasSnapshot) return Freshness.WAITING;
        if (pendingTicks > 0) return pendingTicks > intervalTicks ? Freshness.DELAYED : Freshness.REFRESHING;
        if (responseTimedOut || snapshotAgeTicks >= responseTimeoutTicks) return Freshness.STALE;
        return Freshness.FRESH;
    }

    public String getStatusLabel() {
        Freshness freshness = getFreshness();
        if (freshness == Freshness.WAITING) return "行情等待中";
        if (freshness == Freshness.REFRESHING) return "行情刷新中";
        if (freshness == Freshness.DELAYED) return "行情响应延迟";
        if (freshness == Freshness.STALE) return "行情已过期 " + getSnapshotAgeSeconds() + "s";
        return getSnapshotAgeSeconds() <= 1 ? "行情实时" : "行情 " + getSnapshotAgeSeconds() + "s 前";
    }
}
