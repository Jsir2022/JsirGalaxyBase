package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Encodes the bounded, platform-neutral shape used by inventory observation facts. */
public final class InventoryObservationAttributeCodec {
    public static final String COUNT = "inventory.count";
    private static final int MAX_ENTRIES = 4096;

    public Map<String, String> encode(List<InventoryObservationEntry> entries) {
        List<InventoryObservationEntry> canonical = new InventoryObservationCanonicalizer().canonicalize(entries);
        if (canonical.size() > MAX_ENTRIES) throw new IllegalArgumentException("inventory snapshot has too many entries");
        Map<String, String> attributes = new LinkedHashMap<String, String>();
        for (int index = 0; index < canonical.size(); index++) {
            InventoryObservationEntry entry = canonical.get(index);
            String prefix = "inventory." + index + ".";
            attributes.put(prefix + "registryName", entry.getRegistryName());
            attributes.put(prefix + "meta", Integer.toString(entry.getMeta()));
            attributes.put(prefix + "damageable", Boolean.toString(entry.isDamageable()));
            attributes.put(prefix + "amount", Long.toString(entry.getAmount()));
            attributes.put(prefix + "oreDictionary", entry.getOreDictionary());
            if (!entry.getNbtPortable().isEmpty()) attributes.put(prefix + "nbtPortable", entry.getNbtPortable());
        }
        attributes.put(COUNT, Integer.toString(canonical.size()));
        return Collections.unmodifiableMap(attributes);
    }

    public List<InventoryObservationEntry> decode(Map<String, String> attributes) {
        if (attributes == null) throw new IllegalArgumentException("inventory attributes are required");
        int count = parseCount(attributes.get(COUNT));
        List<InventoryObservationEntry> entries = new ArrayList<InventoryObservationEntry>(count);
        for (int index = 0; index < count; index++) {
            String prefix = "inventory." + index + ".";
            String registryName = required(attributes, prefix + "registryName");
            int meta = parseInt(required(attributes, prefix + "meta"), prefix + "meta");
            long amount = parsePositiveLong(required(attributes, prefix + "amount"), prefix + "amount");
            boolean damageable = parseBoolean(attributes.get(prefix + "damageable"), prefix + "damageable");
            entries.add(new InventoryObservationEntry(registryName, meta, amount,
                attributes.get(prefix + "oreDictionary"), attributes.get(prefix + "nbt"),
                attributes.get(prefix + "nbtPortable"), damageable));
        }
        return Collections.unmodifiableList(entries);
    }

    private static int parseCount(String value) {
        int count = parseInt(value, COUNT);
        if (count < 0 || count > MAX_ENTRIES) throw new IllegalArgumentException("inventory snapshot entry count is out of range");
        return count;
    }

    private static int parseInt(String value, String key) {
        try { return Integer.parseInt(value); }
        catch (RuntimeException invalid) { throw new IllegalArgumentException("invalid inventory attribute " + key, invalid); }
    }

    private static long parsePositiveLong(String value, String key) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed > 0L) return parsed;
        } catch (RuntimeException ignored) { }
        throw new IllegalArgumentException("invalid inventory attribute " + key);
    }

    private static boolean parseBoolean(String value, String key) {
        if ("true".equalsIgnoreCase(value) || "1".equals(value)) return true;
        if ("false".equalsIgnoreCase(value) || "0".equals(value)) return false;
        throw new IllegalArgumentException("invalid inventory attribute " + key);
    }

    private static String required(Map<String, String> attributes, String key) {
        String value = attributes.get(key);
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("missing inventory attribute " + key);
        return value;
    }
}
