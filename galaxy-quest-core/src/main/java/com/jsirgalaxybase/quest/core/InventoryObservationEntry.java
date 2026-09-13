package com.jsirgalaxybase.quest.core;

import java.util.Objects;

/** Immutable, platform-neutral inventory observation before canonicalization. */
public final class InventoryObservationEntry {
    private final String registryName;
    private final int meta;
    private final long amount;
    private final String oreDictionary;
    private final String nbt;
    private final String nbtPortable;

    public InventoryObservationEntry(String registryName, int meta, long amount, String oreDictionary,
        String nbt, String nbtPortable) {
        if (registryName == null || registryName.trim().isEmpty()) throw new IllegalArgumentException(
            "registryName must not be blank");
        if (amount <= 0L) throw new IllegalArgumentException("amount must be positive");
        this.registryName = registryName.trim();
        this.meta = meta;
        this.amount = amount;
        this.oreDictionary = safe(oreDictionary);
        this.nbt = safe(nbt);
        this.nbtPortable = safe(nbtPortable);
    }

    public String getRegistryName() { return registryName; }
    public int getMeta() { return meta; }
    public long getAmount() { return amount; }
    public String getOreDictionary() { return oreDictionary; }
    public String getNbt() { return nbt; }
    public String getNbtPortable() { return nbtPortable; }

    private static String safe(String value) { return value == null ? "" : value; }
}
