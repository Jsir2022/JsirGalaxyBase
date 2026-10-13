package com.jsirgalaxybase.quest.bqimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

final class BqTypedJson {
    private BqTypedJson() {}

    static JsonElement find(JsonObject object, String logicalName) {
        if (object == null) return null;
        for (String key : object.keySet()) {
            int separator = key.lastIndexOf(':');
            String name = separator < 0 ? key : key.substring(0, separator);
            if (logicalName.equals(name)) return object.get(key);
        }
        return null;
    }

    static JsonObject object(JsonObject parent, String name) {
        JsonElement value = find(parent, name);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
    }

    static boolean hasCompound(JsonObject parent, String name) {
        JsonElement value = find(parent, name);
        return value != null && value.isJsonObject();
    }

    static String string(JsonObject parent, String name, String fallback) {
        JsonElement value = find(parent, name);
        return value == null || value.isJsonNull() ? fallback : value.getAsString();
    }

    static long number(JsonObject parent, String name, long fallback) {
        JsonElement value = find(parent, name);
        return value == null || value.isJsonNull() ? fallback : value.getAsLong();
    }

    static int int32(JsonObject parent, String name, int fallback) {
        JsonElement value = find(parent, name);
        if (value == null || value.isJsonNull()) return fallback;
        try {
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                return value.getAsBigDecimal().intValueExact();
            }
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                return Integer.parseInt(value.getAsString().trim());
            }
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("BetterQuesting integer property " + name
                + " must fit a signed 32-bit integer: " + value, invalid);
        }
        throw new IllegalArgumentException("BetterQuesting integer property " + name
            + " must be numeric: " + value);
    }

    static boolean bool(JsonObject parent, String name, boolean fallback) {
        JsonElement value = find(parent, name);
        if (value == null || value.isJsonNull()) return fallback;
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            try {
                return value.getAsBigDecimal().toBigIntegerExact().signum() != 0;
            } catch (ArithmeticException invalid) {
                throw new IllegalArgumentException("Invalid BetterQuesting boolean property " + name
                    + "=" + value.getAsString(), invalid);
            }
        }
        String raw = value.getAsString().trim();
        if ("1".equals(raw) || "true".equalsIgnoreCase(raw)) return true;
        if ("0".equals(raw) || "false".equalsIgnoreCase(raw)) return false;
        throw new IllegalArgumentException("Invalid BetterQuesting boolean property " + name + "=" + raw);
    }

    static List<JsonObject> indexedObjects(JsonObject parent, String name) {
        return indexedObjects(object(parent, name));
    }

    static List<JsonObject> indexedObjects(JsonObject values) {
        List<String> keys = new ArrayList<String>(values.keySet());
        Collections.sort(keys, new Comparator<String>() {
            @Override
            public int compare(String left, String right) {
                return Integer.compare(index(left), index(right));
            }
        });
        List<JsonObject> result = new ArrayList<JsonObject>();
        for (String key : keys) {
            JsonElement value = values.get(key);
            if (value != null && value.isJsonObject()) result.add(value.getAsJsonObject());
        }
        return result;
    }

    private static int index(String key) {
        int separator = key.indexOf(':');
        String raw = separator < 0 ? key : key.substring(0, separator);
        try { return Integer.parseInt(raw); } catch (NumberFormatException ignored) { return Integer.MAX_VALUE; }
    }
}
