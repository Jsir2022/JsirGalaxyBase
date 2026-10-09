package com.jsirgalaxybase.quest.core;

import java.util.Objects;

/** Server-captured inventory scope for a consumption request; never construct this from client item data. */
public final class ConsumptionSelection {
    private final int inventorySlot;
    private final String stackFingerprint;

    private ConsumptionSelection(int inventorySlot, String stackFingerprint) {
        if (inventorySlot < 0) throw new IllegalArgumentException("inventorySlot must not be negative");
        this.inventorySlot = inventorySlot;
        this.stackFingerprint = text(stackFingerprint, "stackFingerprint");
    }

    public static ConsumptionSelection selectedStack(int inventorySlot, String stackFingerprint) {
        return new ConsumptionSelection(inventorySlot, stackFingerprint);
    }

    public int getInventorySlot() { return inventorySlot; }
    public String getStackFingerprint() { return stackFingerprint; }

    private static String text(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
