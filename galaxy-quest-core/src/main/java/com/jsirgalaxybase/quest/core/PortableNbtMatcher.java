package com.jsirgalaxybase.quest.core;

import java.util.List;
import java.util.Map;

/** BetterQuesting-compatible compound/list/array subset matching with cross-numeric equality. */
public final class PortableNbtMatcher {
    private PortableNbtMatcher() {}

    public static boolean matches(String required, String actual, boolean partial) {
        try { return matches(PortableNbt.decode(required), PortableNbt.decode(actual), partial); }
        catch (RuntimeException malformed) { return false; }
    }

    public static boolean matches(PortableNbt required, PortableNbt actual, boolean partial) {
        if (required == null || actual == null) return required == actual;
        if (required.isEmpty() || actual.isEmpty()) return required.isEmpty() == actual.isEmpty();
        if (required.getKind() == PortableNbt.Kind.NUMBER && actual.getKind() == PortableNbt.Kind.NUMBER) {
            return required.getScalar().equals(actual.getScalar());
        }
        if (required.getKind() != actual.getKind()) return false;
        if (required.getKind() == PortableNbt.Kind.STRING) return required.getScalar().equals(actual.getScalar());
        if (required.getKind() == PortableNbt.Kind.COMPOUND) {
            Map<String, PortableNbt> left = required.getFields();
            Map<String, PortableNbt> right = actual.getFields();
            if (!partial && left.size() != right.size()) return false;
            for (Map.Entry<String, PortableNbt> entry : left.entrySet()) {
                PortableNbt value = right.get(entry.getKey());
                if (value == null || !matches(entry.getValue(), value, partial)) return false;
            }
            return true;
        }
        return unordered(required.getValues(), actual.getValues(), partial);
    }

    private static boolean unordered(List<PortableNbt> required, List<PortableNbt> actual, boolean partial) {
        if (required.size() > actual.size() || !partial && required.size() != actual.size()) return false;
        boolean[] used = new boolean[actual.size()];
        for (PortableNbt expected : required) {
            boolean found = false;
            for (int i = 0; i < actual.size(); i++) {
                if (!used[i] && matches(expected, actual.get(i), partial)) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }
}
