package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Absolute fluid-container observation for non-consuming BQ fluid tasks. */
public final class FluidSnapshotTaskEvaluator implements TaskEvaluator {
    private final String typeId;
    private final String factType;

    public FluidSnapshotTaskEvaluator(String typeId, String factType) {
        this.typeId = typeId;
        this.factType = factType;
    }

    @Override public String getTypeId() { return typeId; }

    @Override
    public TaskProgress evaluate(TaskDefinition definition, TaskProgress current, GameplayFact fact) {
        if (!factType.equals(fact.getTypeId()) || bool(definition.getParameters().get("consume"), true)) return current;
        int requirements = integer(definition.getParameters().get("fluid.count"), 0);
        if (current.getValues().size() != requirements) throw new IllegalArgumentException(
            "fluid task vector width does not match definition");
        int observed = integer(fact.getAttributes().get("fluidInventory.count"), 0);
        long[] remaining = new long[observed];
        for (int item = 0; item < observed; item++) remaining[item] = positive(
            fact.getAttributes().get("fluidInventory." + item + ".amount"), 0L);
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
        String required = "fluid." + requirement + ".";
        String observed = "fluidInventory." + item + ".";
        if (!value(parameters, required + "name").equals(value(fact, observed + "name"))) return false;
        if (bool(parameters.get("ignoreNBT"), true)) return true;
        String requiredNbt = value(parameters, required + "nbtPortable");
        return requiredNbt.isEmpty() || PortableNbt.decode(requiredNbt).isEmpty()
            || PortableNbtMatcher.matches(requiredNbt, value(fact, observed + "nbtPortable"), false);
    }

    private String value(Map<String, String> values, String key) {
        String value = values.get(key);
        return value == null ? "" : value;
    }
    private boolean bool(String raw, boolean fallback) { return raw == null ? fallback : Boolean.parseBoolean(raw); }
    private int integer(String raw, int fallback) { return raw == null ? fallback : Integer.parseInt(raw); }
    private long positive(String raw, long fallback) {
        long value = raw == null ? fallback : Long.parseLong(raw);
        if (value < 0) throw new IllegalArgumentException("fluid amount must not be negative");
        return value;
    }
    private long saturatingAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
