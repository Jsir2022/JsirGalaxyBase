package com.jsirgalaxybase.ui2.motion;

public final class Motion {
    private Motion() {}
    public static float progress(UiClock clock, long startedAt, long durationMillis) {
        if (clock.isReducedMotion() || durationMillis <= 0) return 1F;
        float value = (float) (clock.nowMillis() - startedAt) / (float) durationMillis;
        return Math.max(0F, Math.min(1F, value));
    }
}
