package com.jsirgalaxybase.quest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QuestEditorFieldDescriptor {
    private final String key;
    private final String label;
    private final QuestEditorFieldType type;
    private final boolean required;
    private final Long minimum;
    private final Long maximum;
    private final List<String> options;

    private QuestEditorFieldDescriptor(Builder builder) {
        this.key = requireText(builder.key, "key");
        this.label = requireText(builder.label, "label");
        this.type = builder.type == null ? QuestEditorFieldType.TEXT : builder.type;
        this.required = builder.required;
        this.minimum = builder.minimum;
        this.maximum = builder.maximum;
        if (minimum != null && maximum != null && minimum.longValue() > maximum.longValue()) {
            throw new IllegalArgumentException("minimum must not exceed maximum");
        }
        this.options = Collections.unmodifiableList(new ArrayList<String>(builder.options));
        if (type == QuestEditorFieldType.ENUM && options.isEmpty()) {
            throw new IllegalArgumentException("enum field requires options");
        }
    }

    public static Builder builder(String key, String label, QuestEditorFieldType type) {
        return new Builder(key, label, type);
    }

    public String getKey() { return key; }
    public String getLabel() { return label; }
    public QuestEditorFieldType getType() { return type; }
    public boolean isRequired() { return required; }
    public Long getMinimum() { return minimum; }
    public Long getMaximum() { return maximum; }
    public List<String> getOptions() { return options; }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    public static final class Builder {
        private final String key;
        private final String label;
        private final QuestEditorFieldType type;
        private boolean required;
        private Long minimum;
        private Long maximum;
        private final List<String> options = new ArrayList<String>();

        private Builder(String key, String label, QuestEditorFieldType type) {
            this.key = key;
            this.label = label;
            this.type = type;
        }

        public Builder required() { this.required = true; return this; }
        public Builder range(long minimum, long maximum) {
            this.minimum = Long.valueOf(minimum);
            this.maximum = Long.valueOf(maximum);
            return this;
        }
        public Builder minimum(long minimum) { this.minimum = Long.valueOf(minimum); return this; }
        public Builder options(String... values) {
            if (values != null) Collections.addAll(options, values);
            return this;
        }
        public QuestEditorFieldDescriptor build() { return new QuestEditorFieldDescriptor(this); }
    }
}
