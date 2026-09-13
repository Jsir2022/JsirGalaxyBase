package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Absolute inventory observation for non-consuming retrieval tasks; it never converts observations into deltas. */
public final class InventorySnapshotTaskEvaluator implements TaskEvaluator {
    private final String typeId;
    private final String factType;

    public InventorySnapshotTaskEvaluator(String typeId, String factType) {
        this.typeId = typeId;
        this.factType = factType;
    }

    @Override public String getTypeId() { return typeId; }

    @Override
    public TaskProgress evaluate(TaskDefinition definition, TaskProgress current, GameplayFact fact) {
        if (!factType.equals(fact.getTypeId()) || bool(definition.getParameters().get("consume"), false)) return current;
        int requirements = integer(definition.getParameters().get("item.count"), 0);
        if (current.getValues().size() != requirements) throw new IllegalArgumentException(
            "retrieval task vector width does not match definition");
        int observed = integer(fact.getAttributes().get("inventory.count"), 0);
        long[] remaining = new long[observed];
        for (int item = 0; item < observed; item++) remaining[item] = positive(
            fact.getAttributes().get("inventory." + item + ".amount"), 0L);
        List<Long> values = new ArrayList<Long>(requirements);
        for (int requirement = 0; requirement < requirements; requirement++) {
            long total = 0L;
            for (int item = 0; item < observed; item++) {
                if (!matches(definition.getParameters(), requirement, fact.getAttributes(), item)) continue;
                long needed = Math.max(0L, current.getTargets().get(requirement) - total);
                long allocated = Math.min(needed, remaining[item]);
                total = saturatingAdd(total, allocated);
                remaining[item] -= allocated;
            }
            values.add(Math.min(current.getTargets().get(requirement), total));
        }
        return current.replaceIncomplete(values);
    }

    private boolean matches(Map<String, String> parameters, int requirement, Map<String, String> fact, int item) {
        String required = "item." + requirement + ".";
        String observed = "inventory." + item + ".";
        String requiredName = parameters.get(required + "registryName");
        int requiredMeta = integer(parameters.get(required + "meta"), 0);
        int actualMeta = integer(fact.get(observed + "meta"), 0);
        boolean direct = requiredName != null && requiredName.equals(fact.get(observed + "registryName"))
            && (requiredMeta == 32767 || requiredMeta == actualMeta);
        String ore = parameters.get(required + "oreDictionary");
        boolean oreMatch = ore != null && !ore.isEmpty()
            && delimitedContains(fact.get(observed + "oreDictionary"), ore);
        if (!direct && !oreMatch) return false;
        if (bool(parameters.get("ignoreNBT"), true)) return true;
        String requiredNbt = parameters.get(required + "nbtPortable");
        return requiredNbt == null || PortableNbt.decode(requiredNbt).isEmpty()
            || PortableNbtMatcher.matches(requiredNbt, fact.get(observed + "nbtPortable"),
                bool(parameters.get("partialMatch"), false));
    }

    private boolean delimitedContains(String values, String expected) {
        if (values == null) return false;
        for (String value : values.split("\\|", -1)) if (expected.equals(value)) return true;
        return false;
    }
    private boolean bool(String raw, boolean fallback) { return raw == null ? fallback : Boolean.parseBoolean(raw); }
    private int integer(String raw, int fallback) { return raw == null ? fallback : Integer.parseInt(raw); }
    private long positive(String raw, long fallback) {
        long value = raw == null ? fallback : Long.parseLong(raw);
        if (value < 0) throw new IllegalArgumentException("inventory amount must not be negative");
        return value;
    }
    private long saturatingAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
