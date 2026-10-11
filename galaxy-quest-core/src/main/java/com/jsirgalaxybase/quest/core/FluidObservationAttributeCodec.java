package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Encodes the bounded, platform-neutral shape used by fluid-container observation facts. */
public final class FluidObservationAttributeCodec {
    public static final String COUNT = "fluidInventory.count";
    private static final int MAX_ENTRIES = 4096;

    public Map<String, String> encode(List<FluidObservationEntry> entries) {
        List<FluidObservationEntry> canonical = new FluidObservationCanonicalizer().canonicalize(entries);
        if (canonical.size() > MAX_ENTRIES) throw new IllegalArgumentException("fluid snapshot has too many entries");
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        for (int index = 0; index < canonical.size(); index++) {
            FluidObservationEntry entry = canonical.get(index);
            String prefix = "fluidInventory." + index + ".";
            attributes.put(prefix + "name", entry.getName());
            attributes.put(prefix + "amount", Long.toString(entry.getAmount()));
            attributes.put(prefix + "nbtPresent", Boolean.toString(entry.isNbtPresent()));
            if (!entry.getNbt().isEmpty()) attributes.put(prefix + "nbt", entry.getNbt());
            if (!entry.getNbtPortable().isEmpty()) attributes.put(prefix + "nbtPortable", entry.getNbtPortable());
        }
        attributes.put(COUNT, Integer.toString(canonical.size()));
        return Collections.unmodifiableMap(attributes);
    }

    public List<FluidObservationEntry> decode(Map<String, String> attributes) {
        if (attributes == null) throw new IllegalArgumentException("fluid attributes are required");
        int count = parseInt(attributes.get(COUNT), COUNT);
        if (count < 0 || count > MAX_ENTRIES) throw new IllegalArgumentException("fluid snapshot entry count is out of range");
        List<FluidObservationEntry> entries = new ArrayList<FluidObservationEntry>(count);
        for (int index = 0; index < count; index++) {
            String prefix = "fluidInventory." + index + ".";
            String name = required(attributes, prefix + "name");
            long amount = parsePositiveLong(required(attributes, prefix + "amount"), prefix + "amount");
            String nbt = value(attributes, prefix + "nbt");
            String portable = value(attributes, prefix + "nbtPortable");
            String present = attributes.get(prefix + "nbtPresent");
            boolean nbtPresent = present == null
                ? (!portable.isEmpty() || (!nbt.trim().isEmpty() && !"{}".equals(nbt.trim())))
                : parseBoolean(present, prefix + "nbtPresent");
            entries.add(new FluidObservationEntry(name, amount, nbt, portable, nbtPresent));
        }
        return Collections.unmodifiableList(entries);
    }

    private static String required(Map<String, String> attributes, String key) {
        String value = attributes.get(key);
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("missing fluid attribute " + key);
        return value;
    }

    private static String value(Map<String, String> attributes, String key) {
        String value = attributes.get(key);
        return value == null ? "" : value;
    }

    private static int parseInt(String value, String key) {
        try { return Integer.parseInt(value); }
        catch (RuntimeException invalid) { throw new IllegalArgumentException("invalid fluid attribute " + key, invalid); }
    }

    private static long parsePositiveLong(String value, String key) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed > 0L) return parsed;
        } catch (RuntimeException ignored) { }
        throw new IllegalArgumentException("invalid fluid attribute " + key);
    }

    private static boolean parseBoolean(String value, String key) {
        if ("true".equalsIgnoreCase(value) || "1".equals(value)) return true;
        if ("false".equalsIgnoreCase(value) || "0".equals(value)) return false;
        throw new IllegalArgumentException("invalid fluid attribute " + key);
    }
}
