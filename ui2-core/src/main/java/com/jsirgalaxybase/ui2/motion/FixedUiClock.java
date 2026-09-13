package com.jsirgalaxybase.ui2.motion;

public final class FixedUiClock implements UiClock {
    private long now;
    private final boolean reducedMotion;

    public FixedUiClock(long now, boolean reducedMotion) { this.now = now; this.reducedMotion = reducedMotion; }
    public void advance(long millis) { if (millis < 0) throw new IllegalArgumentException("cannot move backwards"); now += millis; }
    @Override public long nowMillis() { return now; }
    @Override public boolean isReducedMotion() { return reducedMotion; }
}
