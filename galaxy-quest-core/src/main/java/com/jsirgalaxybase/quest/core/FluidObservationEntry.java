package com.jsirgalaxybase.quest.core;

import java.util.Objects;

/** Immutable fluid-container observation used by the platform adapters. */
public final class FluidObservationEntry {
    private final String name;
    private final long amount;
    private final String nbt;
    private final String nbtPortable;
    private final boolean nbtPresent;

    public FluidObservationEntry(String name, long amount, String nbt, String nbtPortable) {
        this(name, amount, nbt, nbtPortable, (nbtPortable != null && !nbtPortable.isEmpty())
            || (nbt != null && !nbt.trim().isEmpty() && !"{}".equals(nbt.trim())));
    }

    public FluidObservationEntry(String name, long amount, String nbt, String nbtPortable, boolean nbtPresent) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("fluid name must not be blank");
        if (amount <= 0L) throw new IllegalArgumentException("fluid amount must be positive");
        this.name = name.trim();
        this.amount = amount;
        this.nbt = nbt == null ? "" : nbt;
        this.nbtPortable = nbtPortable == null ? "" : nbtPortable;
        this.nbtPresent = nbtPresent;
    }

    public String getName() { return name; }
    public long getAmount() { return amount; }
    public String getNbt() { return nbt; }
    public String getNbtPortable() { return nbtPortable; }
    public boolean isNbtPresent() { return nbtPresent; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FluidObservationEntry)) return false;
        FluidObservationEntry that = (FluidObservationEntry) other;
        return amount == that.amount && name.equals(that.name) && nbt.equals(that.nbt)
            && nbtPortable.equals(that.nbtPortable) && nbtPresent == that.nbtPresent;
    }

    @Override public int hashCode() { return Objects.hash(name, amount, nbt, nbtPortable, nbtPresent); }
}
