package com.jsirgalaxybase.ui2.core;

import java.util.Objects;

public final class UiKey {
    private final String value;

    private UiKey(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("key is required");
        this.value = value;
    }

    public static UiKey of(String value) { return new UiKey(value); }
    public String getValue() { return value; }
    @Override public boolean equals(Object object) { return object instanceof UiKey && value.equals(((UiKey) object).value); }
    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return value; }
}
