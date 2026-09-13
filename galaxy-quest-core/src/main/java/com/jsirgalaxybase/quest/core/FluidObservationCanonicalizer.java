package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges equivalent fluid containers without changing BQ requirement allocation.
 * Identity is the fluid name plus portable NBT; the first raw NBT representation
 * and first-seen order are retained for diagnostics.
 */
public final class FluidObservationCanonicalizer {
    public List<FluidObservationEntry> canonicalize(List<FluidObservationEntry> entries) {
        Map<String, FluidObservationEntry> unique = new LinkedHashMap<String, FluidObservationEntry>();
        if (entries != null) for (FluidObservationEntry entry : entries) {
            if (entry == null) throw new IllegalArgumentException("fluid observation must not be null");
            String key = key(entry);
            FluidObservationEntry previous = unique.get(key);
            if (previous == null) unique.put(key, entry);
            else unique.put(key, new FluidObservationEntry(previous.getName(), saturatingAdd(
                previous.getAmount(), entry.getAmount()), previous.getNbt(), previous.getNbtPortable()));
        }
        return Collections.unmodifiableList(new ArrayList<FluidObservationEntry>(unique.values()));
    }

    private String key(FluidObservationEntry value) {
        return value.getName() + "\u0000" + value.getNbtPortable();
    }

    private long saturatingAdd(long left, long right) {
        return right > 0L && Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
