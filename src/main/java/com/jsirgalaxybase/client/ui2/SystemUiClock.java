package com.jsirgalaxybase.client.ui2;

import com.jsirgalaxybase.ui2.motion.UiClock;

public final class SystemUiClock implements UiClock {
    private final boolean reducedMotion;
    public SystemUiClock(boolean reducedMotion) { this.reducedMotion = reducedMotion; }
    @Override public long nowMillis() { return System.nanoTime() / 1_000_000L; }
    @Override public boolean isReducedMotion() { return reducedMotion; }
}
