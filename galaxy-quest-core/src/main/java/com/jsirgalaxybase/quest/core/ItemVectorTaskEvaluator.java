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
        if (BqPartyCraftingStatisticsScope.isPersonalOnlineSnapshot(fact)
            && !definition.getParameters().containsKey("bq.raw")) return current;
        String targetTaskKey = fact.getAttributes().get("targetTaskKey");
        if (targetTaskKey != null && !targetTaskKey.equals(definition.getKey())) return current;
        if ("craft-statistics".equals(fact.getAttributes().get("channel"))
            && !statisticsDetectionAllowed(definition.getParameters(), fact.getAttributes())) return current;
        int count = integer(definition.getParameters().get("item.count"), 0);
        if (current.getValues().size() != count) throw new IllegalArgumentException(
            "item task vector width does not match definition");
        long amount = positive(fact.getAttributes().get("amount"), 1L);
        boolean crafting = "bq_standard:crafting".equals(typeId);
        boolean statistics = "craft-statistics".equals(fact.getAttributes().get("channel"));
        if (crafting) {
            List<Long> observed = new ArrayList<Long>(count);
            List<Long> deltas = new ArrayList<Long>(count);
            TaskProgress.SourceContribution source = current.getSourceContributions().get(fact.getSourceServer());
            for (int i = 0; i < count; i++) {
                observed.add(Long.valueOf(source == null ? 0L : source.getObservedValues().get(i).longValue()));
                deltas.add(Long.valueOf(0L));
            }
            boolean matched = false;
            for (int i = 0; i < count; i++) {
                if (!matches(definition.getParameters(), i, fact.getAttributes())) continue;
                matched = true;
                if (statistics) {
                    long target = current.getTargets().get(i).longValue();
                    long sourceValue = source == null ? 0L : source.getValue(i, target);
                    // BQ TaskCrafting.detect only reads objectCraftStats while this requirement's
                    // source-local progress is still below its target. In particular, a craft event
                    // that reaches the target is capped before detect runs, so its absolute statistic
                    // must not inflate that same requirement afterward. Other requirements and other
                    // server sources remain independently eligible for detection.
                    if (sourceValue < target) {
                        observed.set(i, Long.valueOf(Math.max(observed.get(i).longValue(), amount)));
                    }
                } else deltas.set(i, Long.valueOf(amount));
            }
            if (!matched) return current;
            return statistics ? current.observeFromSource(fact.getSourceServer(), observed)
                : current.advanceFromSource(fact.getSourceServer(), deltas);
        }
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

    private boolean statisticsDetectionAllowed(Map<String, String> parameters, Map<String, String> attributes) {
        String triggerChannel = attributes.get("trigger.channel");
        if (triggerChannel == null || triggerChannel.isEmpty()) return true; // explicit Detect request
        Map<String, String> trigger = new java.util.LinkedHashMap<String, String>();
        trigger.put("channel", triggerChannel);
        copy(attributes, trigger, "trigger.registryName", "registryName");
        copy(attributes, trigger, "trigger.meta", "meta");
        copy(attributes, trigger, "trigger.damageable", "damageable");
        copy(attributes, trigger, "trigger.oreDictionary", "oreDictionary");
        copy(attributes, trigger, "trigger.nbtPortable", "nbtPortable");
        if (!channelAllowed(parameters, trigger)) return false;
        int count = integer(parameters.get("item.count"), 0);
        for (int i = 0; i < count; i++) if (matches(parameters, i, trigger)) return true;
        return false;
    }

    /**
     * Checks whether a real output event would cause this Crafting task to run its automatic statistics Detect.
     * The same channel and item matching rules are used by normal progress evaluation.
     */
    public boolean matchesCraftingStatisticsTrigger(TaskDefinition definition, Map<String, String> output) {
        if (!"bq_standard:crafting".equals(typeId) || definition == null || output == null
            || !typeId.equals(definition.getTypeId())
            || !bool(definition.getParameters().get("allowCraftedFromStatistics"), false)
            || !channelAllowed(definition.getParameters(), output)) return false;
        int count = integer(definition.getParameters().get("item.count"), 0);
        for (int i = 0; i < count; i++) if (matches(definition.getParameters(), i, output)) return true;
        return false;
    }

    /** Checks whether a craft/smelt/anvil output matches a BQ Crafting requirement. */
    public boolean matchesCraftingProgressEvent(TaskDefinition definition, Map<String, String> output) {
        if (!"bq_standard:crafting".equals(typeId) || definition == null || output == null
            || !typeId.equals(definition.getTypeId()) || !channelAllowed(definition.getParameters(), output)) {
            return false;
        }
        String channel = output.get("channel");
        if (!"craft".equals(channel) && !"smelt".equals(channel) && !"anvil".equals(channel)) return false;
        int count = integer(definition.getParameters().get("item.count"), 0);
        for (int i = 0; i < count; i++) if (matches(definition.getParameters(), i, output)) return true;
        return false;
    }

    private void copy(Map<String, String> source, Map<String, String> target, String sourceKey, String targetKey) {
        String value = source.get(sourceKey);
        if (value != null) target.put(targetKey, value);
    }

    private boolean channelAllowed(Map<String, String> parameters, GameplayFact fact) {
        return channelAllowed(parameters, fact.getAttributes());
    }

    private boolean channelAllowed(Map<String, String> parameters, Map<String, String> fact) {
        String channel = fact.get("channel");
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
        if ("craft-statistics".equals(fact.get("channel"))) {
            // BQ's statistics fallback reads objectCraftStats by Item ID only; it does
            // not re-check metadata, OreDictionary membership, or NBT for this branch.
            return requiredName != null && requiredName.equals(fact.get("registryName"));
        }
        boolean direct = requiredName != null && requiredName.equals(fact.get("registryName"));
        if (direct) {
            int requiredMeta = integer(parameters.get(prefix + "meta"), 0);
            int actualMeta = integer(fact.get("meta"), 0);
            // BQ ItemComparison.StackMatch treats every damage value as compatible when
            // the required item's Item.isDamageable() is true. The adapter records that
            // property on produced-item facts so this remains platform independent.
            direct = requiredMeta == 32767 || requiredMeta == actualMeta || bool(fact.get("damageable"), false);
        }
        String ore = parameters.get(prefix + "oreDictionary");
        boolean oreMatch = ore != null && !ore.isEmpty() && delimitedContains(fact.get("oreDictionary"), ore);
        if (!direct && !oreMatch) return false;
        if (bool(parameters.get("ignoreNBT"), true)) return true;
        String requiredNbt = parameters.get(prefix + "nbtPortable");
        String actualNbt = fact.get("nbtPortable");
        boolean partial = bool(parameters.get("partialMatch"), false);
        // Keep BQ's two APIs distinct: StackMatch uses (required, actual), while
        // OreDictionaryMatch calls CompareNBTTagCompound(actual, required).
        return (direct && PortableNbtMatcher.matches(requiredNbt, actualNbt, partial))
            || (oreMatch && PortableNbtMatcher.matches(actualNbt, requiredNbt, partial));
    }

    private boolean delimitedContains(String values, String expected) {
        if (values == null) return false;
        for (String value : values.split("\\|", -1)) if (expected.equals(value)) return true;
        return false;
    }

    private boolean bool(String raw, boolean fallback) { return QuestBooleanParameter.parse(raw, fallback); }
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
