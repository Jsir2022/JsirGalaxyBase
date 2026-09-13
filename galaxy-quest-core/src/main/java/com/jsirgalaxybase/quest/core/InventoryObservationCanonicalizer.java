package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges equivalent inventory stacks while preserving first-seen order.
 *
 * <p>Stack identity includes all matching dimensions used by BQ retrieval:
 * registry name, meta, OreDict and portable NBT.  Therefore merging reduces
 * payload size without changing allocation or NBT matching semantics.</p>
 */
public final class InventoryObservationCanonicalizer {
    public List<InventoryObservationEntry> canonicalize(List<InventoryObservationEntry> entries) {
        Map<String, InventoryObservationEntry> unique = new LinkedHashMap<String, InventoryObservationEntry>();
        if (entries != null) for (InventoryObservationEntry entry : entries) {
            if (entry == null) throw new IllegalArgumentException("inventory observation must not be null");
            String key = key(entry);
            InventoryObservationEntry previous = unique.get(key);
            if (previous == null) unique.put(key, entry);
            else unique.put(key, new InventoryObservationEntry(previous.getRegistryName(), previous.getMeta(),
                saturatingAdd(previous.getAmount(), entry.getAmount()), previous.getOreDictionary(),
                previous.getNbt(), previous.getNbtPortable()));
        }
        return Collections.unmodifiableList(new ArrayList<InventoryObservationEntry>(unique.values()));
    }

    private String key(InventoryObservationEntry value) {
        return value.getRegistryName() + "\u0000" + value.getMeta() + "\u0000" + value.getOreDictionary()
            + "\u0000" + value.getNbtPortable();
    }

    private long saturatingAdd(long left, long right) {
        return right > 0L && Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
