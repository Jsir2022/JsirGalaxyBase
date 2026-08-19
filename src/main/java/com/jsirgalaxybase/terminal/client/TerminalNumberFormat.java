package com.jsirgalaxybase.terminal.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Semantic number formats shared by dense terminal views. */
public final class TerminalNumberFormat {

    private static final long[] COMPACT_UNITS = {
        1_000L,
        1_000_000L,
        1_000_000_000L,
        1_000_000_000_000L,
        1_000_000_000_000_000L,
        1_000_000_000_000_000_000L
    };
    private static final String[] COMPACT_SUFFIXES = { "K", "M", "G", "T", "P", "E" };

    private TerminalNumberFormat() {}

    /** Exact, grouped form for balances, prices, confirmations, and actionable records. */
    public static String exact(long value) {
        return String.format(Locale.ROOT, "%,d", Long.valueOf(value));
    }

    /**
     * Compact quantity for dense, read-only surfaces. Values below 1,000 remain
     * exact; larger values use at most three significant digits and are truncated
     * toward zero so displayed market depth never overstates available liquidity.
     */
    public static String compactQuantity(long value) {
        BigDecimal absolute = BigDecimal.valueOf(value).abs();
        int unitIndex = -1;
        for (int index = COMPACT_UNITS.length - 1; index >= 0; index--) {
            if (absolute.compareTo(BigDecimal.valueOf(COMPACT_UNITS[index])) >= 0) {
                unitIndex = index;
                break;
            }
        }
        if (unitIndex < 0) {
            return Long.toString(value);
        }

        BigDecimal unit = BigDecimal.valueOf(COMPACT_UNITS[unitIndex]);
        BigDecimal whole = absolute.divide(unit, 0, RoundingMode.DOWN);
        int decimals = whole.precision() >= 3 ? 0 : 3 - whole.precision();
        BigDecimal scaled = absolute.divide(unit, decimals, RoundingMode.DOWN).stripTrailingZeros();
        return (value < 0L ? "-" : "") + scaled.toPlainString() + COMPACT_SUFFIXES[unitIndex];
    }
}
