package com.jsirgalaxybase.modules.core.market.application;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MarketRecoveryMetadata {

    private static final String NULL_TOKEN = "~";

    private final Map<String, String> values;

    private MarketRecoveryMetadata(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<String, String>(values));
    }

    public static MarketRecoveryMetadata empty() {
        return new MarketRecoveryMetadata(Collections.<String, String>emptyMap());
    }

    public static MarketRecoveryMetadata parse(String key) {
        if (key == null || key.trim().isEmpty()) {
            return empty();
        }
        Map<String, String> parsed = new LinkedHashMap<String, String>();
        String[] parts = key.split("\\|");
        for (String part : parts) {
            int separatorIndex = part.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }
            String name = part.substring(0, separatorIndex);
            String value = part.substring(separatorIndex + 1);
            parsed.put(name, NULL_TOKEN.equals(value) ? null : value);
        }
        return new MarketRecoveryMetadata(parsed);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String get(String key) {
        return values.get(key);
    }

    public long getLong(String key, long defaultValue) {
        String value = values.get(key);
        return value == null || value.trim().isEmpty() ? defaultValue : Long.parseLong(value);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = values.get(key);
        return value == null || value.trim().isEmpty() ? defaultValue : Boolean.parseBoolean(value);
    }

    public String toKey() {
        if (values.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue() == null ? NULL_TOKEN : entry.getValue());
        }
        return builder.toString();
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            builder.put(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    public static final class Builder {

        private final Map<String, String> values = new LinkedHashMap<String, String>();

        public Builder put(String key, String value) {
            values.put(key, value);
            return this;
        }

        public Builder putLong(String key, long value) {
            values.put(key, String.valueOf(value));
            return this;
        }

        public Builder putBoolean(String key, boolean value) {
            values.put(key, String.valueOf(value));
            return this;
        }

        public MarketRecoveryMetadata build() {
            return new MarketRecoveryMetadata(values);
        }
    }
}