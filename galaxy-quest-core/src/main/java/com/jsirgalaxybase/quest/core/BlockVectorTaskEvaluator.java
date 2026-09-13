package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Mirrors BQ block-break semantics: one physical break advances only the first matching requirement. */
public final class BlockVectorTaskEvaluator implements TaskEvaluator {
    private final String typeId;
    private final String factType;

    public BlockVectorTaskEvaluator(String typeId, String factType) {
        this.typeId = typeId;
        this.factType = factType;
    }

    @Override public String getTypeId() { return typeId; }

    @Override
    public TaskProgress evaluate(TaskDefinition definition, TaskProgress current, GameplayFact fact) {
        if (!factType.equals(fact.getTypeId())) return current;
        int count = integer(definition.getParameters().get("block.count"), 0);
        if (current.getValues().size() != count) throw new IllegalArgumentException(
            "block task vector width does not match definition");
        List<Long> next = new ArrayList<Long>(current.getValues());
        for (int i = 0; i < count; i++) {
            if (!matches(definition.getParameters(), i, fact.getAttributes())) continue;
            next.set(i, Math.min(current.getTargets().get(i), saturatingAdd(next.get(i), 1L)));
            break;
        }
        return current.merge(next, false);
    }

    private boolean matches(Map<String, String> parameters, int index, Map<String, String> fact) {
        String prefix = "block." + index + ".";
        String name = parameters.get(prefix + "registryName");
        int requiredMeta = integer(parameters.get(prefix + "meta"), 0);
        int actualMeta = integer(fact.get("meta"), 0);
        boolean direct = name != null && name.equals(fact.get("registryName"))
            && (requiredMeta < 0 || requiredMeta == 32767 || requiredMeta == actualMeta);
        String ore = parameters.get(prefix + "oreDictionary");
        boolean oreMatch = ore != null && !ore.isEmpty() && delimitedContains(fact.get("oreDictionary"), ore);
        if (!direct && !oreMatch) return false;
        String requiredNbt = parameters.get(prefix + "nbtPortable");
        return requiredNbt == null || PortableNbt.decode(requiredNbt).isEmpty()
            || PortableNbtMatcher.matches(requiredNbt, fact.get("nbtPortable"), true);
    }

    private boolean delimitedContains(String values, String expected) {
        if (values == null) return false;
        for (String value : values.split("\\|", -1)) if (expected.equals(value)) return true;
        return false;
    }
    private int integer(String raw, int fallback) { return raw == null ? fallback : Integer.parseInt(raw); }
    private long saturatingAdd(long left, long right) { return left == Long.MAX_VALUE ? left : left + right; }
}
