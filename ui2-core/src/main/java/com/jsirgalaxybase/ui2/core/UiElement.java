package com.jsirgalaxybase.ui2.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UiElement {
    private final String type;
    private final UiKey key;
    private final Map<String, Object> props;
    private final List<UiElement> children;

    private UiElement(Builder builder) {
        this.type = builder.type;
        this.key = builder.key;
        this.props = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.props));
        this.children = Collections.unmodifiableList(new ArrayList<UiElement>(builder.children));
    }

    public static Builder type(String type) { return new Builder(type); }
    public String getType() { return type; }
    public UiKey getKey() { return key; }
    public Map<String, Object> getProps() { return props; }
    public Object getProp(String name) { return props.get(name); }
    public List<UiElement> getChildren() { return children; }

    public static final class Builder {
        private final String type;
        private UiKey key;
        private final Map<String, Object> props = new LinkedHashMap<String, Object>();
        private final List<UiElement> children = new ArrayList<UiElement>();

        private Builder(String type) {
            if (type == null || type.trim().isEmpty()) throw new IllegalArgumentException("type is required");
            this.type = type;
        }

        public Builder key(String value) { this.key = UiKey.of(value); return this; }
        public Builder key(UiKey value) { this.key = value; return this; }
        public Builder prop(String name, Object value) {
            if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("prop name is required");
            props.put(name, value);
            return this;
        }
        public Builder child(UiElement value) { if (value != null) children.add(value); return this; }
        public Builder children(List<UiElement> values) { if (values != null) children.addAll(values); return this; }
        public UiElement build() { return new UiElement(this); }
    }
}
