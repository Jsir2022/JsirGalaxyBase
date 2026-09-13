package com.jsirgalaxybase.quest.bqimport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jsirgalaxybase.quest.core.PortableNbt;

/** Converts BetterQuesting's typed JSON NBT representation into the shared canonical tree. */
final class BqPortableNbtCodec {
    private BqPortableNbtCodec() {}

    static String encodeJson(String json) {
        if (json == null || json.trim().isEmpty()) return PortableNbt.compound(
            java.util.Collections.<String, PortableNbt>emptyMap()).encode();
        return encode(com.google.gson.JsonParser.parseString(json), 10).encode();
    }

    static String encodeCompound(JsonObject value) { return encode(value, 10).encode(); }

    private static PortableNbt encode(JsonElement value, int declaredType) {
        if (declaredType == 10) {
            Map<String, PortableNbt> fields = new LinkedHashMap<String, PortableNbt>();
            if (value != null && value.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
                    fields.put(logicalName(entry.getKey()), encode(entry.getValue(), type(entry.getKey(), 8)));
                }
            }
            return PortableNbt.compound(fields);
        }
        if (declaredType == 9) {
            List<PortableNbt> values = new ArrayList<PortableNbt>();
            if (value != null && value.isJsonArray()) {
                for (JsonElement entry : value.getAsJsonArray()) values.add(encode(entry, infer(entry)));
            } else if (value != null && value.isJsonObject()) {
                JsonObject object = value.getAsJsonObject();
                for (String key : sortedKeys(object)) {
                    values.add(encode(object.get(key), type(key, infer(object.get(key)))));
                }
            }
            return PortableNbt.list(values);
        }
        if (declaredType == 7 || declaredType == 11) {
            List<PortableNbt> values = new ArrayList<PortableNbt>();
            if (value != null && value.isJsonArray()) {
                for (JsonElement entry : value.getAsJsonArray()) values.add(PortableNbt.number(entry.getAsString()));
            } else if (value != null && value.isJsonObject()) {
                for (String key : sortedKeys(value.getAsJsonObject())) {
                    values.add(PortableNbt.number(value.getAsJsonObject().get(key).getAsString()));
                }
            }
            return declaredType == 7 ? PortableNbt.byteArray(values) : PortableNbt.intArray(values);
        }
        if (declaredType >= 1 && declaredType <= 6) return PortableNbt.number(value.getAsString());
        return PortableNbt.string(value == null || value.isJsonNull() ? "" : value.getAsString());
    }

    private static int infer(JsonElement value) {
        if (value != null && value.isJsonObject()) return 10;
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) return 6;
        return 8;
    }

    private static int type(String key, int fallback) {
        int separator = key.lastIndexOf(':');
        if (separator < 0) return fallback;
        try { return Integer.parseInt(key.substring(separator + 1)); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static String logicalName(String key) {
        int separator = key.lastIndexOf(':');
        return separator < 0 ? key : key.substring(0, separator);
    }

    private static List<String> sortedKeys(JsonObject object) {
        List<String> keys = new ArrayList<String>(object.keySet());
        java.util.Collections.sort(keys, new java.util.Comparator<String>() {
            @Override public int compare(String left, String right) {
                try { return Integer.compare(Integer.parseInt(logicalName(left)), Integer.parseInt(logicalName(right))); }
                catch (NumberFormatException ignored) { return left.compareTo(right); }
            }
        });
        return keys;
    }
}
