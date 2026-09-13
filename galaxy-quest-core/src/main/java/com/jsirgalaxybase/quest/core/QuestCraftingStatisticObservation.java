package com.jsirgalaxybase.quest.core;

import java.util.Objects;

/** One item whose vanilla crafting statistic is required by a published quest. */
public final class QuestCraftingStatisticObservation {
    private final String registryName;
    private final int meta;

    public QuestCraftingStatisticObservation(String registryName, int meta) {
        if (registryName == null || registryName.trim().isEmpty()) {
            throw new IllegalArgumentException("registryName must not be blank");
        }
        this.registryName = registryName.trim();
        this.meta = meta;
    }

    public String getRegistryName() { return registryName; }
    public int getMeta() { return meta; }

    @Override public boolean equals(Object other) {
        if (!(other instanceof QuestCraftingStatisticObservation)) return false;
        QuestCraftingStatisticObservation value = (QuestCraftingStatisticObservation) other;
        return meta == value.meta && registryName.equals(value.registryName);
    }

    @Override public int hashCode() { return Objects.hash(registryName, meta); }
}
