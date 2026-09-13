package com.jsirgalaxybase.quest.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TaskDefinition {
    private final String key;
    private final String typeId;
    private final boolean optional;
    private final Map<String, String> parameters;

    public TaskDefinition(String key, String typeId, boolean optional, Map<String, String> parameters) {
        this.key = requireText(key, "key");
        this.typeId = requireText(typeId, "typeId");
        this.optional = optional;
        this.parameters = Collections.unmodifiableMap(new LinkedHashMap<String, String>(
            parameters == null ? Collections.<String, String>emptyMap() : parameters));
    }

    public String getKey() { return key; }
    public String getTypeId() { return typeId; }
    public boolean isOptional() { return optional; }
    public Map<String, String> getParameters() { return parameters; }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}
