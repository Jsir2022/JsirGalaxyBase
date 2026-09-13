package com.jsirgalaxybase.ui2.layout;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.jsirgalaxybase.ui2.geometry.UiRect;

public final class LayoutResult {
    private final Map<String, UiRect> bounds;
    LayoutResult(Map<String, UiRect> bounds) { this.bounds = Collections.unmodifiableMap(new LinkedHashMap<String, UiRect>(bounds)); }
    public UiRect get(String key) {
        UiRect value = bounds.get(key);
        if (value == null) throw new IllegalArgumentException("unknown layout key: " + key);
        return value;
    }
    public Map<String, UiRect> asMap() { return bounds; }
}
