package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Incremental item evaluator. Ore dictionary and NBT matching are resolved by the platform fact adapter. */
public final class ItemVectorTaskEvaluator implements TaskEvaluator {
    private final String typeId;
    private final String factType;

    public ItemVectorTaskEvaluator(String typeId, String factType) {
        this.typeId = typeId;
        this.factType = factType;
    }

    @Override
    public String getTypeId() { return typeId; }

    @Override
    public TaskProgress evaluate(TaskDefinition definition, TaskProgress current, GameplayFact fact) {
        if (!factType.equals(fact.getTypeId()) || !channelAllowed(definition.getParameters(), fact)) return current;
        int count = integer(definition.getParameters().get("item.count"), 0);
        if (current.getValues().size() != count) throw new IllegalArgumentException(
            "item task vector width does not match definition");
        long amount = positive(fact.getAttributes().get("amount"), 1L);
        List<Long> next = new ArrayList<Long>(current.getValues());
        for (int i = 0; i < count; i++) {
            if (!matches(definition.getParameters(), i, fact.getAttributes())) continue;
            long target = current.getTargets().get(i);
            long observed = "craft-statistics".equals(fact.getAttributes().get("channel"))
                ? Math.min(target, amount) : Math.min(target, saturatingAdd(next.get(i), amount));
            next.set(i, Math.max(next.get(i), observed));
        }
        return current.merge(next, false);
    }

    private boolean channelAllowed(Map<String, String> parameters, GameplayFact fact) {
        String channel = fact.getAttributes().get("channel");
        if (channel == null) return true;
        if ("craft".equals(channel)) return bool(parameters.get("allowCraft"), true);
        if ("smelt".equals(channel)) return bool(parameters.get("allowSmelt"), true);
        if ("anvil".equals(channel)) return bool(parameters.get("allowAnvil"), false);
        if ("craft-statistics".equals(channel)) return bool(parameters.get("allowCraftedFromStatistics"), false);
        return false;
    }

    private boolean matches(Map<String, String> parameters, int index, Map<String, String> fact) {
        String prefix = "item." + index + ".";
        String requiredName = parameters.get(prefix + "registryName");
        if (requiredName != null && requiredName.equals(fact.get("registryName"))) {
            int requiredMeta = integer(parameters.get(prefix + "meta"), 0);
            int actualMeta = integer(fact.get("meta"), 0);
            if (requiredMeta == 32767 || requiredMeta == actualMeta) return nbtMatches(parameters, prefix, fact);
        }
        String ore = parameters.get(prefix + "oreDictionary");
        return ore != null && !ore.isEmpty() && delimitedContains(fact.get("oreDictionary"), ore)
            && nbtMatches(parameters, prefix, fact);
    }

    private boolean nbtMatches(Map<String, String> parameters, String prefix, Map<String, String> fact) {
        if (bool(parameters.get("ignoreNBT"), true)) return true;
        String required = parameters.get(prefix + "nbtPortable");
        return required == null || PortableNbt.decode(required).isEmpty()
            || PortableNbtMatcher.matches(required, fact.get("nbtPortable"), bool(parameters.get("partialMatch"), false));
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
        if (value < 0) throw new IllegalArgumentException("fact amount must not be negative");
        return value;
    }
    private long saturatingAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
