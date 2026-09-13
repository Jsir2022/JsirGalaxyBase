package com.jsirgalaxybase.ui2.terminal;

import java.util.Objects;

/** Platform-neutral item identity used by terminal documents and visual previews. */
public final class VisualItem {
    private final String id;
    private final String registryName;
    private final int metadata;
    private final String displayName;
    private final long quantity;

    public VisualItem(String id, String registryName, int metadata, String displayName, long quantity) {
        this.id = required(id, "id");
        this.registryName = required(registryName, "registryName");
        this.metadata = metadata;
        this.displayName = required(displayName, "displayName");
        this.quantity = Math.max(0L, quantity);
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }

    public String getId() { return id; }
    public String getRegistryName() { return registryName; }
    public int getMetadata() { return metadata; }
    public String getDisplayName() { return displayName; }
    public long getQuantity() { return quantity; }

    @Override public boolean equals(Object value) {
        if (!(value instanceof VisualItem)) return false;
        VisualItem other = (VisualItem) value;
        return metadata == other.metadata && quantity == other.quantity && id.equals(other.id)
            && registryName.equals(other.registryName) && displayName.equals(other.displayName);
    }
    @Override public int hashCode() { return Objects.hash(id, registryName, metadata, displayName, quantity); }
}
