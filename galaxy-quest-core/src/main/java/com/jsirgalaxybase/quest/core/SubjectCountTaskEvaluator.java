package com.jsirgalaxybase.quest.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Count evaluator for facts that expose an exact subject plus server-derived compatible aliases. */
public final class SubjectCountTaskEvaluator implements TaskEvaluator {
    private final String typeId;
    private final String factType;

    public SubjectCountTaskEvaluator(String typeId, String factType) {
        this.typeId = typeId;
        this.factType = factType;
    }

    @Override
    public String getTypeId() { return typeId; }

    @Override
    public TaskProgress evaluate(TaskDefinition definition, TaskProgress current, GameplayFact fact) {
        if (!factType.equals(fact.getTypeId())) return current;
        String required = definition.getParameters().get("subject");
        String exact = fact.getAttributes().get("subject");
        boolean subtypes = parseBoolean(definition.getParameters().get("subtypes"), true);
        if (required != null && !(required.equals(exact) || (subtypes && aliases(fact).contains(required)))) return current;
        String damageType = definition.getParameters().get("damageType");
        if (damageType != null && !damageType.isEmpty()
            && !damageType.equalsIgnoreCase(fact.getAttributes().get("damageType"))) return current;
        if (!parseBoolean(definition.getParameters().get("ignoreNBT"), true)) {
            String requiredNbt = definition.getParameters().get("targetNBT.portable");
            if (requiredNbt != null && !requiredNbt.isEmpty() && !PortableNbt.decode(requiredNbt).isEmpty()
                && !PortableNbtMatcher.matches(requiredNbt, fact.getAttributes().get("entity.nbtPortable"), true)) {
                return current;
            }
        }
        long amount = positive(fact.getAttributes().get("amount"), 1L);
        return current.merge(saturatingAdd(current.getValue(), amount), false);
    }

    private Set<String> aliases(GameplayFact fact) {
        String raw = fact.getAttributes().get("subjectAliases");
        if (raw == null || raw.isEmpty()) return Collections.emptySet();
        return new HashSet<String>(Arrays.asList(raw.split("\\|", -1)));
    }

    private boolean parseBoolean(String raw, boolean fallback) {
        return raw == null ? fallback : Boolean.parseBoolean(raw);
    }

    private long positive(String raw, long fallback) {
        long value = raw == null ? fallback : Long.parseLong(raw);
        if (value < 0) throw new IllegalArgumentException("fact amount must not be negative");
        return value;
    }

    private long saturatingAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
