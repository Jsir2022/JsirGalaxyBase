package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RewardDefinition {
    private final String key;
    private final String typeId;
    private final Map<String, String> parameters;

    public RewardDefinition(String key, String typeId, Map<String, String> parameters) {
        this.key = requireText(key, "key");
        this.typeId = requireText(typeId, "typeId");
        this.parameters = Collections.unmodifiableMap(new LinkedHashMap<String, String>(
            parameters == null ? Collections.<String, String>emptyMap() : parameters));
    }

    public String getKey() { return key; }
    public String getTypeId() { return typeId; }
    public Map<String, String> getParameters() { return parameters; }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
