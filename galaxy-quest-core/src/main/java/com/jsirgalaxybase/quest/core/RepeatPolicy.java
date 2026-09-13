package com.jsirgalaxybase.quest.core;

public final class RepeatPolicy {
    private static final RepeatPolicy NEVER = new RepeatPolicy(-1L, true);
    private final long cooldownMillis;
    private final boolean relative;

    private RepeatPolicy(long cooldownMillis, boolean relative) {
        if (cooldownMillis < -1L) throw new IllegalArgumentException("cooldownMillis must be -1 or non-negative");
        this.cooldownMillis = cooldownMillis;
        this.relative = relative;
    }

    public static RepeatPolicy never() { return NEVER; }
    public static RepeatPolicy after(long cooldownMillis) { return new RepeatPolicy(cooldownMillis, true); }
    public static RepeatPolicy after(long cooldownMillis, boolean relative) {
        return new RepeatPolicy(cooldownMillis, relative);
    }
    public boolean isRepeatable() { return cooldownMillis >= 0L; }
    public long getCooldownMillis() { return cooldownMillis; }
    public boolean isRelative() { return relative; }

    public boolean isAvailable(long completedAt, long now) {
        if (!isRepeatable() || now < completedAt) return false;
        if (relative || cooldownMillis <= 1L) return now - completedAt >= cooldownMillis;
        long boundary = completedAt - completedAt % cooldownMillis;
        if (Long.MAX_VALUE - boundary < cooldownMillis) return false;
        return now >= boundary + cooldownMillis;
    }
}
