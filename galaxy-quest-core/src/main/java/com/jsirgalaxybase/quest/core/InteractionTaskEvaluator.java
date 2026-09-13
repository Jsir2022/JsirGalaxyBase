package com.jsirgalaxybase.quest.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Shared BQ interaction counter with canonical, platform-neutral NBT matching. */
public final class InteractionTaskEvaluator implements TaskEvaluator {
    private final String typeId;
    private final String factType;
    private final boolean entity;

    public InteractionTaskEvaluator(String typeId, String factType, boolean entity) {
        this.typeId = typeId;
        this.factType = factType;
        this.entity = entity;
    }

    @Override public String getTypeId() { return typeId; }

    @Override public TaskProgress evaluate(TaskDefinition definition, TaskProgress current, GameplayFact fact) {
        if (current.isComplete() || !factType.equals(fact.getTypeId())) return current;
        Map<String, String> parameters = definition.getParameters();
        Map<String, String> attributes = fact.getAttributes();
        boolean hit = Boolean.parseBoolean(attributes.get("hit"));
        if (hit && !bool(parameters.get("onHit"), false)
            || !hit && !bool(parameters.get("onInteract"), true)) return current;
        if (entity && !entityMatches(parameters, attributes)) return current;
        if (!entity && !blockMatches(parameters, attributes)) return current;
        if (!itemMatches(parameters, attributes)) return current;
        if (!nbtMatches(parameters, attributes)) return current;
        return current.merge(Math.min(current.getTarget(), saturatingAdd(current.getValue(), 1L)), false);
    }

    private boolean entityMatches(Map<String, String> required, Map<String, String> actual) {
        String subject = required.get("subject");
        if (subject == null || subject.isEmpty()) return true;
        if (subject.equals(actual.get("subject"))) return true;
        return bool(required.get("targetSubtypes"), true) && aliases(actual).contains(subject);
    }

    private boolean blockMatches(Map<String, String> required, Map<String, String> actual) {
        String name = required.get("block.registryName");
        if (name == null || name.isEmpty()) return true;
        boolean direct = name.equals(actual.get("block.registryName"));
        int meta = integer(required.get("block.meta"), 0);
        direct &= meta < 0 || meta == 32767 || meta == integer(actual.get("block.meta"), 0);
        return direct || contains(actual.get("block.oreDictionary"), required.get("block.oreDictionary"));
    }

    private boolean itemMatches(Map<String, String> required, Map<String, String> actual) {
        String name = required.get("heldItem.registryName");
        if (name == null || name.isEmpty()) return true;
        boolean direct = name.equals(actual.get("heldItem.registryName"));
        int meta = integer(required.get("heldItem.meta"), 0);
        direct &= meta == 32767 || meta == integer(actual.get("heldItem.meta"), 0);
        return direct || contains(actual.get("heldItem.oreDictionary"), required.get("heldItem.oreDictionary"));
    }

    private boolean nbtMatches(Map<String, String> required, Map<String, String> actual) {
        if (entity) {
            if (!bool(required.get("ignoreTargetNBT"), true)
                && !portableMatches(required.get("targetNBT.portable"), actual.get("entity.nbtPortable"), true)) return false;
            return bool(required.get("ignoreItemNBT"), false)
                || portableMatches(required.get("heldItem.nbtPortable"), actual.get("heldItem.nbtPortable"),
                    bool(required.get("partialItemMatch"), false));
        }
        if (!portableMatches(required.get("block.nbtPortable"), actual.get("block.nbtPortable"), true)) return false;
        return bool(required.get("ignoreNbt"), true)
            || portableMatches(required.get("heldItem.nbtPortable"), actual.get("heldItem.nbtPortable"),
                bool(required.get("partialMatch"), false));
    }

    private static boolean portableMatches(String required, String actual, boolean partial) {
        return required == null || required.isEmpty() || PortableNbt.decode(required).isEmpty()
            || PortableNbtMatcher.matches(required, actual, partial);
    }

    private static Set<String> aliases(Map<String, String> attributes) {
        String raw = attributes.get("subjectAliases");
        return raw == null || raw.isEmpty() ? Collections.<String>emptySet()
            : new HashSet<String>(Arrays.asList(raw.split("\\|", -1)));
    }

    private static boolean contains(String values, String expected) {
        if (values == null || expected == null || expected.isEmpty()) return false;
        return new HashSet<String>(Arrays.asList(values.split("\\|", -1))).contains(expected);
    }

    private static int integer(String value, int fallback) {
        return value == null || value.isEmpty() ? fallback : Integer.parseInt(value);
    }

    private static boolean bool(String value, boolean fallback) {
        return value == null ? fallback : "1".equals(value) || Boolean.parseBoolean(value);
    }

    private static long saturatingAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
